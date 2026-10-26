package com.cursedarchie.platformer.world;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.controllers.EnemyController;
import com.cursedarchie.platformer.controllers.HeroController;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.levels.*;
import com.cursedarchie.platformer.levels.wrappers.LocationTransition;
import com.cursedarchie.platformer.physics.GameContactListener;
import com.cursedarchie.platformer.physics.OneWaySurface;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.tiles.types.BreakableBlock;
import com.cursedarchie.platformer.levels.wrappers.EnemySpawnData;
import com.cursedarchie.platformer.tiles.TileMetrics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Owns one play run: Box2D, fixed-step gameplay, rooms, enemies, and generated-run state. */
public class GameWorld implements com.badlogic.gdx.utils.Disposable {
    public static final float FIXED_STEP = 1f / 60f;
    public static final float GRAVITY = 40f;
    private static final int MAX_STEPS_PER_FRAME = 6;

    private final World world;

    private final Hero hero;
    private final HeroController heroController;

    private final RoomManager roomManager;
    private String currentLocationName;
    private LocationInstance location;
    private int currentRoomId;
    private RoomInstance currentRoom;
    private final Map<Integer, RoomState> roomStates = new HashMap<>();
    private final Map<String, Set<Integer>> visitedRooms = new HashMap<>();
    private final Array<Enemy> enemies = new Array<>();
    private final Array<DefeatEffect> defeatEffects = new Array<>();
    private final Array<DamageIndicator> damageIndicators = new Array<>();
    private final Map<Actor, Float> observedHealth = new IdentityHashMap<>();
    private final EnemyController enemyController;
    private boolean disposed;
    private float accumulator;
    private boolean breakHitThisSwing;


    public GameWorld() { this(new RoomManager()); }

    public GameWorld(long seed) { this(new RoomManager(seed)); }

    public GameWorld(long seed, Preferences preferences) {
        this(new RoomManager(seed, preferences));
    }

    private GameWorld(RoomManager roomManager) {
        this.roomManager = roomManager;
        this.world = new World(new Vector2(0, -GRAVITY), true);
        this.currentLocationName = "start";
        this.currentRoomId = 0;
        location = roomManager.loadLocation(currentLocationName);
        currentRoom = location.getRoom(currentRoomId);
        markCurrentRoomVisited();

        this.hero = new Hero(new Vector2(5, 5), world);
        observedHealth.put(hero, hero.getHealth());
        world.setContactListener(new GameContactListener());

        this.heroController = new HeroController(this);
        this.enemyController = new EnemyController(this);

        buildLocation();
    }

    public void update(float delta) {
        if (delta <= 0f || Float.isNaN(delta)) return;
        // Drop excess accumulated time after a stall instead of simulating an unbounded backlog.
        accumulator = Math.min(accumulator + delta, FIXED_STEP * MAX_STEPS_PER_FRAME);
        int steps = (int) (accumulator / FIXED_STEP);
        accumulator -= steps * FIXED_STEP;
        for (int i = 0; i < steps; i++) simulateStep(FIXED_STEP);
    }

    private void simulateStep(float delta) {
        updateDefeatEffects(delta);
        updateDamageIndicators(delta);
        recordDamage();
        // Body destruction must happen outside Box2D's locked world.step callback.
        removeDeadEnemies();
        heroController.update(delta);
        updateBreakableAttack();
        world.step(delta, 6, 2);
        // Contacts from this step affect input on the next step (including buffered jumps).
        refreshHeroGrounding();

        updateCurrentRoom();
        checkRoomExit();

        enemyController.update(delta);

        recordDamage();
        removeDeadEnemies();
    }

    private void recordDamage() {
        recordDamage(hero);
        for (Enemy enemy : enemies) recordDamage(enemy);
    }

    private void recordDamage(Actor actor) {
        Float previous = observedHealth.put(actor, actor.getHealth());
        if (previous != null && actor.getHealth() < previous)
            damageIndicators.add(new DamageIndicator(actor, previous - actor.getHealth()));
    }

    private void updateDamageIndicators(float delta) {
        for (int i = damageIndicators.size - 1; i >= 0; i--) {
            DamageIndicator indicator = damageIndicators.get(i);
            indicator.elapsed += delta;
            if (indicator.elapsed >= DamageIndicator.DURATION) damageIndicators.removeIndex(i);
        }
    }

    private void refreshHeroGrounding() {
        boolean supported = hero.getFootContacts() > 0;
        if (!supported) {
            for (Contact contact : world.getContactList()) {
                if (isEnemyHeadSupport(contact) || isOneWaySupport(contact)) {
                    supported = true;
                    break;
                }
            }
        }
        hero.setGrounded(supported);
    }

    private boolean isOneWaySupport(Contact contact) {
        if (!contact.isTouching() || !contact.isEnabled()) return false;
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();
        Fixture platform = a.getUserData() instanceof OneWaySurface ? a :
            b.getUserData() instanceof OneWaySurface ? b : null;
        if (platform == null) return false;
        Fixture actor = platform == a ? b : a;
        if (actor.getBody() != hero.getBody() || !"BODY".equals(actor.getUserData())) return false;
        float feet = hero.getPosition().y - Hero.BODY_SIZE / 2f;
        return feet >= ((OneWaySurface) platform.getUserData()).topY - 0.07f &&
            hero.getBody().getLinearVelocity().y <= 0.1f;
    }

    private void updateBreakableAttack() {
        if (!hero.isLeftAttackActive() && !hero.isRightAttackActive()) {
            breakHitThisSwing = false;
            return;
        }
        if (breakHitThisSwing) return;
        float heroX = hero.getPosition().x;
        float heroY = hero.getPosition().y;
        boolean left = hero.isLeftAttackActive();
        float attackMinX = heroX + (left ? -0.7f : 0.3f);
        float attackMaxX = heroX + (left ? -0.3f : 0.7f);
        float attackMinY = heroY - 0.2f;
        float attackMaxY = heroY + 0.2f;
        RoomInstance hitRoom = null;
        int hitX = -1, hitY = -1;
        float nearest = Float.MAX_VALUE;
        for (RoomInstance room : location.getRooms()) {
            Tile[][] tiles = room.getTiles();
            for (int x = 0; x < tiles.length; x++) {
                for (int y = 0; y < tiles[x].length; y++) {
                    if (!(tiles[x][y] instanceof BreakableBlock)) continue;
                    float worldX = room.getWorldX() + x * TileMetrics.SIZE;
                    float worldY = room.getWorldY() + y * TileMetrics.SIZE;
                    if (worldX >= attackMaxX || worldX + TileMetrics.SIZE <= attackMinX ||
                        worldY >= attackMaxY || worldY + TileMetrics.SIZE <= attackMinY) continue;
                    float distance = Math.abs(worldX + TileMetrics.SIZE / 2f - heroX);
                    if (distance < nearest) {
                        nearest = distance;
                        hitRoom = room;
                        hitX = x;
                        hitY = y;
                    }
                }
            }
        }
        if (hitRoom != null && location.breakTile(hitRoom.getRoomId(), hitX, hitY)) {
            breakHitThisSwing = true;
            roomManager.saveGeneratedRun(location);
        }
    }

    private boolean isEnemyHeadSupport(Contact contact) {
        if (!contact.isTouching() || !contact.isEnabled()) return false;
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();
        if (!"BODY".equals(a.getUserData()) || !"BODY".equals(b.getUserData())) return false;
        Object ownerA = a.getBody().getUserData();
        Object ownerB = b.getBody().getUserData();
        boolean heroIsA = ownerA == hero && ownerB instanceof Enemy && ((Enemy) ownerB).isAlive();
        boolean heroIsB = ownerB == hero && ownerA instanceof Enemy && ((Enemy) ownerA).isAlive();
        if (!heroIsA && !heroIsB) return false;

        // Box2D's manifold normal points from fixture A to B; only support from below
        // counts. Side contact must block movement without granting a jump.
        float normalY = contact.getWorldManifold().getNormal().y;
        return heroIsA ? normalY < -0.7f : normalY > 0.7f;
    }

    private void removeDeadEnemies() {
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (!enemy.isAlive()) {
                defeatEffects.add(new DefeatEffect(enemy));
                rememberDefeatedEnemy(enemy);
                forgetEnemy(enemy);
                world.destroyBody(enemy.getBody());
                enemies.removeIndex(i);
            }
        }
    }

    private void updateDefeatEffects(float delta) {
        for (int i = defeatEffects.size - 1; i >= 0; i--) {
            DefeatEffect effect = defeatEffects.get(i);
            effect.elapsed += delta;
            if (effect.elapsed >= DefeatEffect.DURATION) defeatEffects.removeIndex(i);
        }
    }

    public static final class DefeatEffect {
        public static final float DURATION = 0.3f;
        public final float x;
        public final float y;
        public final float size;
        public final boolean facingLeft;
        public final boolean boss;
        private float elapsed;

        private DefeatEffect(Enemy enemy) {
            x = enemy.getPosition().x;
            y = enemy.getPosition().y;
            size = enemy.getSize();
            facingLeft = enemy.isFacingLeft();
            boss = size >= 1f;
        }

        public float getProgress() { return Math.min(1f, elapsed / DURATION); }
    }

    /** Transient proof of actual health loss, including a lethal hit before body cleanup. */
    public static final class DamageIndicator {
        public static final float DURATION = 0.65f;
        public static final float FLASH_DURATION = 0.18f;
        public final Actor target;
        public final float x;
        public final float y;
        public final float amount;
        public final boolean hero;
        public final String label;
        private float elapsed;

        private DamageIndicator(Actor target, float amount) {
            this.target = target;
            this.x = target.getPosition().x;
            this.y = target.getPosition().y + target.getSize() / 2f + 0.2f;
            this.amount = amount;
            this.hero = target instanceof Hero;
            this.label = amount == Math.round(amount) ?
                "-" + Math.round(amount) : "-" + amount;
        }

        public float getProgress() { return Math.min(1f, elapsed / DURATION); }
    }

    private void rememberDefeatedEnemy(Enemy enemy) {
        if (!location.isGenerated() || enemy.getPersistentSpawnId() == null) return;
        GeneratedRunSave run = location.getGeneratedRun();
        if (!run.defeatedSpawnIds.contains(enemy.getPersistentSpawnId(), false)) {
            run.defeatedSpawnIds.add(enemy.getPersistentSpawnId());
            roomManager.saveGeneratedRun(location);
        }
    }

    private void forgetEnemy(Enemy enemy) {
        observedHealth.remove(enemy);
        hero.removeAttackTarget(enemy);
        hero.removeVisibleActor(enemy);
        for (int i = 0; i < enemies.size; i++) {
            Enemy other = enemies.get(i);
            other.removeAttackTarget(enemy);
            other.removeVisibleActor(enemy);
        }
    }


    private void buildLocation() {
        location.buildBodies(world);
        spawnEnemies();
    }

    private void unloadLocation() {
        defeatEffects.clear();
        damageIndicators.clear();
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            forgetEnemy(enemy);
            world.destroyBody(enemy.getBody());
        }
        enemies.clear();
        observedHealth.clear();
        observedHealth.put(hero, hero.getHealth());
        location.destroyBodies(world);
    }

    public void changeLocation(String locationName, int roomId) {
        changeLocation(locationName, roomId, false);
    }

    private void changeLocation(String locationName, int roomId, boolean enterFromRight) {
        if (disposed) throw new IllegalStateException("GameWorld has been disposed");
        if (world.isLocked()) throw new IllegalStateException("Cannot change location during a physics step");
        LocationInstance nextLocation = roomManager.loadLocation(locationName);
        RoomInstance nextRoom = nextLocation.getRoom(nextLocation.isGenerated() && roomId == 0 ?
            nextLocation.getEntryRoomId() : roomId);
        if (nextRoom == null) throw new IllegalArgumentException("Missing room " + roomId + " in " + locationName);

        unloadLocation();
        currentLocationName = locationName;
        location = nextLocation;
        breakHitThisSwing = false;
        roomStates.clear();
        currentRoomId = nextRoom.getRoomId();
        currentRoom = nextRoom;
        markCurrentRoomVisited();

        buildLocation();
        Vector2 spawn = new Vector2(
            currentRoom.getWorldX() + (enterFromRight ? currentRoom.getWidth() - 1.5f : 1f),
            currentRoom.getWorldY() + 2
        );

        hero.getBody().setTransform(spawn, 0);
        hero.getBody().setLinearVelocity(0, 0);
        // The old location's endContact events are not a reliable way to reset support.
        hero.setFootContacts(0);
        hero.setGrounded(false);
    }

    private void updateCurrentRoom() {
        Vector2 heroPos = hero.getBody().getPosition();

        for (RoomInstance room : location.getRooms()) {
            if (room.contains(heroPos)) {
                currentRoomId = room.getRoomId();
                currentRoom = room;
                markCurrentRoomVisited();
                return;
            }
        }
    }

    private void checkRoomExit() {
        if (currentRoom == null) return;

        Vector2 pos = hero.getBody().getPosition();
        float buffer = 0.2f;
        LocationTransition rightExit = currentRoom.getLocationExit();
        if (rightExit != null && pos.x >= currentRoom.getWorldX() + currentRoom.getWidth() - buffer) {
            changeLocation(rightExit.location, rightExit.id, false);
            return;
        }

        LocationTransition leftExit = currentRoom.getLocationEntry();
        if (leftExit != null && pos.x <= currentRoom.getWorldX() + buffer) {
            changeLocation(leftExit.location, leftExit.id, true);
        }
    }

    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
        observedHealth.put(enemy, enemy.getHealth());
    }

    public void spawnEnemies() {
        for (RoomInstance room : location.getRooms()) {
            RoomState state = getRoomState(room.getRoomId());
            if (state.isEnemiesSpawned()) continue;

            for (int spawnIndex = 0; spawnIndex < room.getSpawns().size; spawnIndex++) {
                EnemySpawnData spawn = room.getSpawns().get(spawnIndex);
                // Stable per-room ID lets a generated run omit enemies defeated earlier.
                String persistentId = room.getRoomId() + ":" + spawnIndex;
                if (location.isGenerated() &&
                    location.getGeneratedRun().defeatedSpawnIds.contains(persistentId, false)) continue;

                Vector2 pos = room.localToWorld(spawn.position);
                Enemy enemy = enemyController.spawnEnemy(spawn, pos, world);
                if (enemy != null && location.isGenerated()) enemy.setPersistentSpawnId(persistentId);
            }

            state.setEnemiesSpawned(true);
        }
    }

    private RoomState getRoomState(int roomId) {
        return roomStates.computeIfAbsent(roomId, id -> new RoomState());
    }

    public Array<Enemy> getEnemies() {
        return enemies;
    }
    public Array<DefeatEffect> getDefeatEffects() { return defeatEffects; }
    public Array<DamageIndicator> getDamageIndicators() { return damageIndicators; }
    public boolean hasRecentDamage(Actor actor) {
        for (DamageIndicator indicator : damageIndicators)
            if (indicator.target == actor && indicator.elapsed < DamageIndicator.FLASH_DURATION)
                return true;
        return false;
    }
    public Array<Enemy> getAliveEnemies() {
        Array<Enemy> alive = new Array<>();
        for (Enemy e : enemies) {
            if (e.isAlive()) alive.add(e);
        }
        return alive;
    }
    public Hero getHero() {
        return hero;
    }
    public LocationInstance getLocation() {
        return location;
    }
    public RoomInstance getCurrentRoom() {
        return currentRoom;
    }
    public boolean isRoomVisited(int roomId) {
        Set<Integer> visited = visitedRooms.get(currentLocationName);
        return visited != null && visited.contains(roomId);
    }

    private void markCurrentRoomVisited() {
        // Room discovery belongs to the run, not to a rendered frame. Keep separate
        // sets per location so revisiting start does not reveal unseen ruins rooms.
        visitedRooms.computeIfAbsent(currentLocationName, name -> new HashSet<>())
            .add(currentRoomId);
    }
    public World getWorld() {
        return world;
    }
    public HeroController getHeroController() {
        return heroController;
    }
    public EnemyController getEnemyController() {
        return enemyController;
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        unloadLocation();
        roomStates.clear();
        world.dispose();
    }
}
