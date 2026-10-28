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
import com.cursedarchie.platformer.levels.wrappers.InteractionData;
import com.cursedarchie.platformer.tiles.TileMetrics;
import com.cursedarchie.platformer.run.RunCheckpoint;
import com.cursedarchie.platformer.run.ItemCatalog;
import com.cursedarchie.platformer.run.Inventory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Owns one play run: Box2D, fixed-step gameplay, rooms, enemies, and generated-run state. */
public class GameWorld implements com.badlogic.gdx.utils.Disposable {
    public static final String KEEPER_BARGAIN = "keeper_bargain_accepted";
    public static final String KEEPER_REFUSAL = "keeper_bargain_refused";
    public static final String NEIGHBOR_SEARCH = "neighbor_search_accepted";
    public static final String NEIGHBOR_LIST_FOUND = "ruins_missing_list_found";
    public static final String NEIGHBOR_LIST_RETURNED = "neighbor_list_returned";
    public static final String NEIGHBOR_LIST_KEPT = "neighbor_list_kept";
    public static final float FIXED_STEP = 1f / 60f;
    public static final float GRAVITY = 80f;
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
    private final Set<String> consumedInteractionIds = new HashSet<>();
    private final Set<String> runFlags = new HashSet<>();
    private final Inventory inventory = new Inventory();
    private final Array<Enemy> enemies = new Array<>();
    private final Array<DefeatEffect> defeatEffects = new Array<>();
    private final Array<DamageIndicator> damageIndicators = new Array<>();
    private final Map<Actor, Float> observedHealth = new IdentityHashMap<>();
    private final EnemyController enemyController;
    private boolean disposed;
    private float accumulator;
    private boolean breakHitThisSwing;
    private long persistenceRevision;


    public GameWorld() { this(new RoomManager()); }

    public GameWorld(long seed) { this(new RoomManager(seed)); }

    public GameWorld(long seed, Preferences preferences) {
        this(new RoomManager(seed, preferences));
    }

    public GameWorld(RunCheckpoint checkpoint, Preferences preferences) {
        this(new RoomManager(checkpoint.seed, preferences));
        try { restore(checkpoint); }
        catch (RuntimeException invalid) {
            dispose();
            throw invalid;
        }
    }

    private GameWorld(RoomManager roomManager) {
        this.roomManager = roomManager;
        this.world = new World(new Vector2(0, -GRAVITY), true);
        this.currentLocationName = "start";
        this.currentRoomId = 0;
        location = roomManager.loadLocation(currentLocationName);
        currentRoom = location.getRoom(currentRoomId);
        markCurrentRoomVisited();

        this.hero = new Hero(new Vector2(10, 10), world);
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
        if (previous != null && actor.getHealth() < previous) {
            damageIndicators.add(new DamageIndicator(actor, previous - actor.getHealth()));
            if (actor == hero) persistenceRevision++;
        }
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
        return feet >= ((OneWaySurface) platform.getUserData()).topY - 0.14f &&
            hero.getBody().getLinearVelocity().y <= 0.2f;
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
        float attackMinX = heroX + (left ? -1.4f : 0.6f);
        float attackMaxX = heroX + (left ? -0.6f : 1.4f);
        float attackMinY = heroY - 0.4f;
        float attackMaxY = heroY + 0.4f;
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
            persistenceRevision++;
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
            boss = "boss".equals(enemy.getArchetypeId());
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
            this.y = target.getPosition().y + target.getSize() / 2f + 0.4f;
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
            persistenceRevision++;
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
        RoomInstance nextRoom = nextLocation.getRoom(nextLocation.isGenerated() ?
            roomId == 0 ? nextLocation.getEntryRoomId() :
                roomId == -1 ? nextLocation.getExitRoomId() : roomId : roomId);
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
            currentRoom.getWorldX() + (enterFromRight ? currentRoom.getWidth() - 3f : 2f),
            currentRoom.getWorldY() + 4
        );

        hero.getBody().setTransform(spawn, 0);
        hero.getBody().setLinearVelocity(0, 0);
        // The old location's endContact events are not a reliable way to reset support.
        hero.setFootContacts(0);
        hero.setGrounded(false);
        persistenceRevision++;
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
        float buffer = 0.4f;
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
    public long getPersistenceRevision() { return persistenceRevision; }

    public InteractionData getNearbyInteraction() {
        if (currentRoom == null || !hero.isAlive()) return null;
        InteractionData nearest = null;
        float nearestDistance2 = 1.7f * 1.7f;
        for (InteractionData interaction : currentRoom.getInteractions()) {
            if (isInteractionConsumed(currentRoom, interaction)) continue;
            float dx = currentRoom.getWorldX() + interaction.position.x - hero.getPosition().x;
            float dy = currentRoom.getWorldY() + interaction.position.y - hero.getPosition().y;
            float distance2 = dx * dx + dy * dy;
            if (distance2 <= nearestDistance2) {
                nearestDistance2 = distance2;
                nearest = interaction;
            }
        }
        return nearest;
    }

    public boolean interact() {
        InteractionData interaction = getNearbyInteraction();
        if (interaction == null) return false;
        if ("npc".equals(interaction.kind)) return false;
        if ("pickup".equals(interaction.kind)) {
            if (!ItemCatalog.supports(interaction.itemId) || !inventory.add(interaction.itemId)) return false;
        }
        consumedInteractionIds.add(interactionId(currentRoom, interaction));
        runFlags.add(interaction.eventId);
        persistenceRevision++;
        return true;
    }

    public boolean hasRunFlag(String eventId) { return runFlags.contains(eventId); }
    public boolean hasEchoDash() { return hasRunFlag(KEEPER_BARGAIN); }

    private boolean isNearWarden() {
        InteractionData nearby = getNearbyInteraction();
        return nearby != null && "shelter_warden".equals(nearby.id);
    }

    public boolean acceptNeighborSearch() {
        if (!isNearWarden() || hasRunFlag(NEIGHBOR_SEARCH)) return false;
        runFlags.add(NEIGHBOR_SEARCH);
        persistenceRevision++;
        return true;
    }

    public boolean resolveNeighborList(boolean returnToFamilies) {
        if (!isNearWarden() || !hasRunFlag(NEIGHBOR_LIST_FOUND) ||
            hasRunFlag(NEIGHBOR_LIST_RETURNED) || hasRunFlag(NEIGHBOR_LIST_KEPT)) return false;
        runFlags.add(returnToFamilies ? NEIGHBOR_LIST_RETURNED : NEIGHBOR_LIST_KEPT);
        persistenceRevision++;
        return true;
    }

    private boolean isNearKeeper() {
        InteractionData nearby = getNearbyInteraction();
        return nearby != null && "npc".equals(nearby.kind) && "keeper".equals(nearby.id);
    }

    public boolean acceptKeeperOffer() {
        if (!isNearKeeper() || hasEchoDash() || hasRunFlag(KEEPER_REFUSAL) || !hero.isAlive() ||
            hero.getMaxHealth() <= 1f) return false;
        hero.setMaxHealth(hero.getMaxHealth() - 1f);
        hero.setHealth(Math.min(hero.getHealth(), hero.getMaxHealth()));
        observedHealth.put(hero, hero.getHealth());
        runFlags.add(KEEPER_BARGAIN);
        persistenceRevision++;
        return true;
    }

    public boolean refuseKeeperOffer() {
        if (!isNearKeeper() || hasEchoDash() || hasRunFlag(KEEPER_REFUSAL)) return false;
        runFlags.add(KEEPER_REFUSAL);
        persistenceRevision++;
        return true;
    }
    public Inventory getInventory() { return inventory; }
    public int getFieldBandages() { return inventory.count(ItemCatalog.FIELD_BANDAGE); }

    public boolean moveToPocket(int bagSlot, int pocket) {
        if (!inventory.swapWithPocket(bagSlot, pocket)) return false;
        persistenceRevision++;
        return true;
    }

    public boolean useFieldBandage() {
        return useFieldBandage(inventory.firstSlot(ItemCatalog.FIELD_BANDAGE));
    }

    public boolean useFieldBandage(int slot) {
        if (!hero.isAlive() || slot < Inventory.CAPACITY || slot >= Inventory.TOTAL_SLOTS ||
            !ItemCatalog.FIELD_BANDAGE.equals(inventory.itemId(slot)) ||
            hero.getHealth() >= hero.getMaxHealth()) return false;
        hero.setHealth(Math.min(hero.getMaxHealth(), hero.getHealth() + ItemCatalog.bandage().heal));
        observedHealth.put(hero, hero.getHealth());
        inventory.removeOne(slot);
        persistenceRevision++;
        return true;
    }

    public boolean isInteractionConsumed(RoomInstance room, InteractionData interaction) {
        return consumedInteractionIds.contains(interactionId(room, interaction));
    }

    private String interactionId(RoomInstance room, InteractionData interaction) {
        return room.getLocation() + ":" + room.getRoomId() + ":" + interaction.id;
    }

    public RunCheckpoint checkpoint() {
        if (!hero.isAlive()) throw new IllegalStateException("Cannot save a dead run");
        RunCheckpoint checkpoint = new RunCheckpoint();
        checkpoint.worldLayoutVersion = 10;
        checkpoint.seed = roomManager.getRunSeed();
        checkpoint.locationId = currentLocationName;
        checkpoint.roomId = currentRoomId;
        checkpoint.heroX = hero.getPosition().x;
        checkpoint.heroY = hero.getPosition().y;
        checkpoint.heroHealth = hero.getHealth();
        for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
            RunCheckpoint.InventorySlot slot = new RunCheckpoint.InventorySlot();
            slot.itemId = inventory.itemId(i);
            slot.count = inventory.count(i);
            checkpoint.inventory.add(slot);
        }
        for (Map.Entry<String, Set<Integer>> locationVisits : visitedRooms.entrySet()) {
            for (Integer roomId : locationVisits.getValue())
                checkpoint.visitedRooms.add(locationVisits.getKey() + ":" + roomId);
        }
        checkpoint.visitedRooms.sort();
        checkpoint.consumedInteractionIds.addAll(consumedInteractionIds.toArray(new String[0]));
        checkpoint.consumedInteractionIds.sort();
        checkpoint.runFlags.addAll(runFlags.toArray(new String[0]));
        checkpoint.runFlags.sort();
        for (GeneratedRunSave run : roomManager.getGeneratedRuns()) {
            RunCheckpoint.GeneratedLocationRef ref = new RunCheckpoint.GeneratedLocationRef();
            ref.locationId = run.locationId;
            ref.profileVersion = run.profileVersion;
            ref.topologySignature = run.topologySignature;
            ref.placementSignature = run.placementSignature;
            checkpoint.generatedLocations.add(ref);
        }
        return checkpoint;
    }

    private void restore(RunCheckpoint checkpoint) {
        if (checkpoint.worldLayoutVersion != 10)
            throw new IllegalArgumentException("Checkpoint uses an older world layout");
        for (RunCheckpoint.GeneratedLocationRef ref : checkpoint.generatedLocations) {
            if (ref == null || ref.locationId == null || ref.topologySignature == null ||
                ref.placementSignature == null) throw new IllegalArgumentException("Invalid generated run reference");
            if (!roomManager.hasSavedGeneratedReference(ref))
                throw new IllegalArgumentException("Generated location no longer matches checkpoint");
            LocationInstance restored = roomManager.loadLocation(ref.locationId);
            GeneratedRunSave run = restored.getGeneratedRun();
            boolean upgradedRuins = "ruins".equals(ref.locationId) &&
                ref.profileVersion == 7 && run != null && run.profileVersion == 8;
            if (run == null || (run.profileVersion != ref.profileVersion && !upgradedRuins) ||
                !run.topologySignature.equals(ref.topologySignature) ||
                !run.placementSignature.equals(ref.placementSignature))
                throw new IllegalArgumentException("Generated location no longer matches checkpoint");
        }
        changeLocation(checkpoint.locationId, checkpoint.roomId);
        RoomInstance room = location.getRoom(checkpoint.roomId);
        Vector2 savedPosition = new Vector2(checkpoint.heroX, checkpoint.heroY);
        if (checkpoint.runFlags.contains(KEEPER_BARGAIN, false)) hero.setMaxHealth(2f);
        if (room == null || !room.contains(savedPosition) ||
            !isRestoredPositionClear(savedPosition) ||
            checkpoint.heroHealth > hero.getMaxHealth())
            throw new IllegalArgumentException("Invalid saved hero state");
        currentRoomId = checkpoint.roomId;
        currentRoom = room;
        hero.getBody().setTransform(savedPosition, 0);
        hero.getBody().setLinearVelocity(0, 0);
        hero.setHealth(checkpoint.heroHealth);
        if (checkpoint.formatVersion < 4) {
            inventory.setSlot(0, checkpoint.fieldBandages == 0 ? null : ItemCatalog.FIELD_BANDAGE,
                checkpoint.fieldBandages);
        } else {
            if (checkpoint.inventory == null || checkpoint.inventory.size !=
                (checkpoint.formatVersion >= 7 ? Inventory.TOTAL_SLOTS :
                    checkpoint.formatVersion == 6 ? 18 : 16))
                throw new IllegalArgumentException("Invalid saved inventory");
            if (checkpoint.formatVersion >= 7) {
                for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
                    RunCheckpoint.InventorySlot slot = checkpoint.inventory.get(i);
                    if (slot == null) throw new IllegalArgumentException("Invalid saved inventory slot");
                    inventory.setSlot(i, slot.itemId, slot.count);
                }
            } else restoreLegacyInventory(checkpoint);
        }
        observedHealth.put(hero, hero.getHealth());
        visitedRooms.clear();
        consumedInteractionIds.clear();
        for (String id : checkpoint.consumedInteractionIds) consumedInteractionIds.add(id);
        runFlags.clear();
        for (String flag : checkpoint.runFlags) runFlags.add(flag);
        // Earlier checkpoints recorded the note as a consumed event without an item.
        if (checkpoint.formatVersion < 5 && runFlags.contains("intro_note_read") &&
            inventory.count(ItemCatalog.SEALED_NOTE) == 0 &&
            !inventory.add(ItemCatalog.SEALED_NOTE))
            throw new IllegalArgumentException("No room to migrate the collected note");
        for (String visited : checkpoint.visitedRooms) {
            int separator = visited.lastIndexOf(':');
            if (separator <= 0) throw new IllegalArgumentException("Invalid saved room visit");
            int roomId;
            try { roomId = Integer.parseInt(visited.substring(separator + 1)); }
            catch (NumberFormatException invalid) { throw new IllegalArgumentException("Invalid saved room visit", invalid); }
            visitedRooms.computeIfAbsent(visited.substring(0, separator), id -> new HashSet<>()).add(roomId);
        }
        markCurrentRoomVisited();
    }

    private void restoreLegacyInventory(RunCheckpoint checkpoint) {
        // Previous runs contained at most the authored note and bandage. Compact
        // their old 16-cell bag while retaining the two pocket contents.
        int nextBag = 0;
        for (int i = 0; i < 16; i++) {
            RunCheckpoint.InventorySlot slot = checkpoint.inventory.get(i);
            if (slot == null) throw new IllegalArgumentException("Invalid saved inventory slot");
            if (slot.itemId == null) continue;
            if (nextBag == Inventory.CAPACITY)
                throw new IllegalArgumentException("Legacy inventory exceeds the new bag capacity");
            inventory.setSlot(nextBag++, slot.itemId, slot.count);
        }
        if (checkpoint.formatVersion >= 6) {
            for (int pocket = 0; pocket < Inventory.POCKETS; pocket++) {
                RunCheckpoint.InventorySlot slot = checkpoint.inventory.get(16 + pocket);
                if (slot == null) throw new IllegalArgumentException("Invalid saved pocket");
                inventory.setSlot(Inventory.CAPACITY + pocket, slot.itemId, slot.count);
            }
        }
    }

    private boolean isRestoredPositionClear(Vector2 position) {
        float half = Hero.BODY_SIZE / 2f - 0.04f;
        boolean[] blocked = {false};
        world.QueryAABB(fixture -> {
            if (fixture.getBody().getType() != BodyDef.BodyType.StaticBody ||
                fixture.isSensor() || fixture.getUserData() instanceof OneWaySurface) return true;
            for (float dx : new float[]{-half, 0f, half}) {
                for (float dy : new float[]{-half, 0f, half}) {
                    if (fixture.testPoint(position.x + dx, position.y + dy)) {
                        blocked[0] = true;
                        return false;
                    }
                }
            }
            return true;
        }, position.x - half, position.y - half, position.x + half, position.y + half);
        return !blocked[0];
    }

    private void markCurrentRoomVisited() {
        // Room discovery belongs to the run, not to a rendered frame. Keep separate
        // sets per location so revisiting start does not reveal unseen ruins rooms.
        if (visitedRooms.computeIfAbsent(currentLocationName, name -> new HashSet<>())
            .add(currentRoomId)) persistenceRevision++;
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
