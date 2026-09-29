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

public class GameWorld {

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

        rebuildLocation();
    }

    public void update(float delta) {
        delta = Math.min(delta, 1f/30f);

        world.step(delta, 6, 2);

        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);

            if (!enemy.isAlive()) {

                if (enemy.getBody() != null) {
                    world.destroyBody(enemy.getBody());
                }

                enemies.removeIndex(i);
            }
        }

        updateCurrentRoom();
        checkRoomExit();

        heroController.update(delta);
        enemyController.update(delta);

        hero.getMotionStateMachine().update(delta);
    }



    private void rebuildLocation() {
        clearLevelBodies();

        for (RoomInstance room : location.getRooms()) {
            room.buildBodies(world);
        }

        spawnEnemies();
    }

    private void clearLevelBodies() {
        for (RoomInstance room : location.getRooms()) {
            if (room != null) {
                room.getRuntime().destroy(world);
            }
        }
    }

    public void changeLocation(String locationName, int roomId) {
        currentLocationName = locationName;
        location = roomManager.loadLocation(locationName);
        roomStates.clear();
        currentRoomId = roomId;
        currentRoom = location.getRoom(roomId);

        rebuildLocation();
        Vector2 spawn = new Vector2(
            currentRoom.getWorldX() + 1,
            currentRoom.getWorldY() + 2
        );

        hero.getBody().setTransform(spawn, 0);
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

        LocationTransition exit = currentRoom.getLocationExit();
        if (exit == null) return;

        Vector2 pos = hero.getBody().getPosition();
        float roomRightEdge = currentRoom.getWorldX() + currentRoom.getWidth();
        float buffer = 0.2f;
        if (pos.x >= roomRightEdge - buffer) {
            changeLocation(exit.location, exit.id);
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
}
