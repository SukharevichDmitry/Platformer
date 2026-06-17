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
    private String currentLocation;
    private int currentRoomId;
    private RoomInstance roomInstance;
    Map<String, RoomState> roomStates = new HashMap<>();

    private final Array<Enemy> enemies = new Array<>();
    private final EnemyController enemyController;


    public GameWorld() {
        this.world = new World(new Vector2(0, -10), true);
        this.currentLocation = "start";
        this.currentRoomId = 0;

        this.roomInstance = roomManager.getRoom(currentLocation, currentRoomId);

        this.hero = new Hero(new Vector2(5, 5), world);
        world.setContactListener(new GameContactListener());

        this.heroController = new HeroController(this);
        this.enemyController = new EnemyController(this);

        rebuildLevel();
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

        heroController.update(delta);
        enemyController.update(delta);

        hero.getMotionStateMachine().update(delta);
    }

    private void rebuildLevel() {
        clearLevelBodies();

        roomInstance = roomManager.getRoom(
            currentLocation,
            currentRoomId
        );

        roomInstance.buildBodies(world);
        spawnEnemies();
    }

    private void clearLevelBodies() {
        if (roomInstance != null) {
            roomInstance.getRuntime().destroy(world);
        }
    }

    public void goToNextLevel() {
        Integer nextRoom = roomInstance.getRightNeighbor();

        if (nextRoom == null) {
            LocationTransition exit = roomInstance.getLocationExit();

            if (exit == null) {
                return;
            }

            currentLocation = exit.location;
            nextRoom = exit.id;
        }

        currentRoomId = nextRoom;

        rebuildLevel();

        hero.getBody().setTransform(hero.getSize() / 2f, hero.getPosition().y, 0);
    }

    public void goToPreviousLevel() {
        Integer nextRoom = roomInstance.getLeftNeighbor();

        if (nextRoom == null) {
            LocationTransition entry = roomInstance.getLocationEntry();

            if (entry == null) {
                return;
            }

            currentLocation = entry.location;
            nextRoom = entry.id;
        }

        currentRoomId = nextRoom;

        rebuildLevel();

        float width = roomInstance.getWidth();
        hero.getBody().setTransform( width - hero.getSize() / 2f, hero.getPosition().y, 0);
    }

    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }

    public void spawnEnemies() {
        if (roomInstance.isEnemiesSpawned()) {
            return;
        }

        Array<EnemySpawnData> spawnData = roomInstance.getSpawns();
        for (EnemySpawnData spawn: spawnData) {
            enemyController.spawnEnemy(spawn, world);
        }

        roomInstance.setEnemiesSpawned(true);
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
    public RoomInstance getRoomInstance() {
        return roomInstance;
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
