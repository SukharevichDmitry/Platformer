package com.cursedarchie.platformer.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.controllers.EnemyController;
import com.cursedarchie.platformer.controllers.HeroController;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.levels.*;
import com.cursedarchie.platformer.levels.wrappers.LocationTransition;
import com.cursedarchie.platformer.physics.GameContactListener;
import com.cursedarchie.platformer.levels.wrappers.EnemySpawnData;

import java.util.HashMap;
import java.util.Map;

public class GameWorld implements com.badlogic.gdx.utils.Disposable {
    public static final float FIXED_STEP = 1f / 60f;
    private static final int MAX_STEPS_PER_FRAME = 6;

    private final World world;

    private final Hero hero;
    private final HeroController heroController;

    private final RoomManager roomManager = new RoomManager();
    private String currentLocationName;
    private LocationInstance location;
    private int currentRoomId;
    private RoomInstance currentRoom;
    Map<Integer, RoomState> roomStates = new HashMap<>();
    private final Array<Enemy> enemies = new Array<>();
    private final EnemyController enemyController;
    private boolean disposed;
    private float accumulator;


    public GameWorld() {
        this.world = new World(new Vector2(0, -10), true);
        this.currentLocationName = "start";
        this.currentRoomId = 0;
        location = roomManager.loadLocation(currentLocationName);
        currentRoom = location.getRoom(currentRoomId);

        this.hero = new Hero(new Vector2(5, 5), world);
        world.setContactListener(new GameContactListener());

        this.heroController = new HeroController(this);
        this.enemyController = new EnemyController(this);

        buildLocation();
    }

    public void update(float delta) {
        if (delta <= 0f || Float.isNaN(delta)) return;
        accumulator = Math.min(accumulator + delta, FIXED_STEP * MAX_STEPS_PER_FRAME);
        int steps = (int) (accumulator / FIXED_STEP);
        accumulator -= steps * FIXED_STEP;
        for (int i = 0; i < steps; i++) simulateStep(FIXED_STEP);
    }

    private void simulateStep(float delta) {
        removeDeadEnemies();
        world.step(delta, 6, 2);

        updateCurrentRoom();
        checkRoomExit();

        heroController.update(delta);
        enemyController.update(delta);

        removeDeadEnemies();
    }

    private void removeDeadEnemies() {
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (!enemy.isAlive()) {
                forgetEnemy(enemy);
                world.destroyBody(enemy.getBody());
                enemies.removeIndex(i);
            }
        }
    }

    private void forgetEnemy(Enemy enemy) {
        hero.removeAttackTarget(enemy);
        hero.removeVisibleActor(enemy);
        for (int i = 0; i < enemies.size; i++) {
            Enemy other = enemies.get(i);
            other.removeAttackTarget(enemy);
            other.removeVisibleActor(enemy);
        }
    }


    private void buildLocation() {
        for (RoomInstance room : location.getRooms()) {
            room.buildBodies(world);
        }
        spawnEnemies();
    }

    private void unloadLocation() {
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            forgetEnemy(enemy);
            world.destroyBody(enemy.getBody());
        }
        enemies.clear();
        for (RoomInstance room : location.getRooms()) {
            room.getRuntime().destroy(world);
        }
    }

    public void changeLocation(String locationName, int roomId) {
        changeLocation(locationName, roomId, false);
    }

    private void changeLocation(String locationName, int roomId, boolean enterFromRight) {
        if (disposed) throw new IllegalStateException("GameWorld has been disposed");
        if (world.isLocked()) throw new IllegalStateException("Cannot change location during a physics step");
        LocationInstance nextLocation = roomManager.loadLocation(locationName);
        RoomInstance nextRoom = nextLocation.getRoom(roomId);
        if (nextRoom == null) throw new IllegalArgumentException("Missing room " + roomId + " in " + locationName);

        unloadLocation();
        currentLocationName = locationName;
        location = nextLocation;
        roomStates.clear();
        currentRoomId = roomId;
        currentRoom = nextRoom;

        buildLocation();
        Vector2 spawn = new Vector2(
            currentRoom.getWorldX() + (enterFromRight ? currentRoom.getWidth() - 1.5f : 1f),
            currentRoom.getWorldY() + 2
        );

        hero.getBody().setTransform(spawn, 0);
        hero.getBody().setLinearVelocity(0, 0);
    }

    private void updateCurrentRoom() {
        Vector2 heroPos = hero.getBody().getPosition();

        for (RoomInstance room : location.getRooms()) {
            if (room.contains(heroPos)) {
                currentRoomId = room.getRoomId();
                currentRoom = room;
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
    }

    public void spawnEnemies() {

        for (RoomInstance room :
            location.getRooms()) {

            RoomState state =
                getRoomState(
                    room.getRoomId()
                );

            if (state.isEnemiesSpawned()) {
                continue;
            }

            for (EnemySpawnData spawn :
                room.getSpawns()) {

                Vector2 pos =
                    room.localToWorld(
                        spawn.position
                    );

                enemyController.spawnEnemy(
                    spawn,
                    pos,
                    world
                );
            }

            state.setEnemiesSpawned(true);
        }
    }

    private RoomState getRoomState(int roomId) {
        return roomStates.computeIfAbsent(
            roomId,
            id -> new RoomState()
        );
    }

    public Array<Enemy> getEnemies() {
        return enemies;
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
