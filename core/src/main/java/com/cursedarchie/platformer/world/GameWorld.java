package com.cursedarchie.platformer.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.controllers.EnemyController;
import com.cursedarchie.platformer.controllers.HeroController;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.levels.Level;
import com.cursedarchie.platformer.physics.GameContactListener;

import java.util.HashMap;
import java.util.Map;

public class GameWorld {

    private final World world;
    private final Hero hero;
    private final HeroController heroController;
    private final EnemyController enemyController;
    private Level level;
    private final Map<Integer, Level> levels = new HashMap<>();
    private int currentLevel;

    public GameWorld() {
        this.world = new World(new Vector2(0, -10), true);
        this.currentLevel = 0;
        this.level = loadLevel(currentLevel);
        this.hero = new Hero(new Vector2(5, 5), world);
        world.setContactListener(new GameContactListener());

        this.heroController = new HeroController(this);
        this.enemyController = new EnemyController(this);

        rebuildLevel();
    }

    public void update(float delta) {
        delta = Math.min(delta, 1f/30f);

        world.step(delta, 6, 2);

        Array<Enemy> enemies = level.getEnemies();

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

    private Level getLevel(int index) {
        if (levels.containsKey(index)) {
            return levels.get(index);
        }

        Level level = loadLevel(index);
        levels.put(index, level);

        return level;
    }

    private Level loadLevel(int levelIndex) {
        String path = "levels/level" + (levelIndex + 1) + ".json";
        FileHandle file = Gdx.files.internal(path);

        if (!file.exists()) {
            System.err.println("File not found: " + path);
            return null;
        }

        Json json = new Json();
        json.setUsePrototypes(false);

        json.addClassTag("Vector2", Vector2.class);
        json.setSerializer(Vector2.class, new Json.Serializer<Vector2>() {
            @Override
            public void write(Json json, Vector2 object, Class knownType) {
                json.writeObjectStart();
                json.writeValue("x", object.x);
                json.writeValue("y", object.y);
                json.writeObjectEnd();
            }

            @Override
            public Vector2 read(Json json, JsonValue jsonData, Class type) {
                float x = jsonData.getFloat("x");
                float y = jsonData.getFloat("y");
                return new Vector2(x, y);
            }
        });

        try {
            Level level = new Level(world);

            JsonValue jsonData = new JsonReader().parse(file);
            json.readFields(level, jsonData);

            return level;
        } catch (Exception e) {
            System.err.println("Error while loading level: " + path);
            e.printStackTrace();
            return null;
        }
    }

    private void rebuildLevel() {

        clearLevelBodies();

        level = getLevel(currentLevel);

        level.generateTilesAndBodies();

        if (!level.isInitialized()) {
            level.spawnEnemies(enemyController);
            level.setInitialized(true);
        }
    }

    private void clearLevelBodies() {
        Array<Body> bodies = new Array<>();
        world.getBodies(bodies);

        for (Body body : bodies) {
            if (body.getUserData() != null &&
                body.getUserData().equals("level")) {
                world.destroyBody(body);
            }
        }
    }

    public void goToNextLevel() {
        if (currentLevel >= 4) return;
        Gdx.app.log("GOTONEXTLEVEL", "Go to next level");
        currentLevel++;
        rebuildLevel();

        hero.getBody().setTransform(hero.getSize() / 2f, hero.getPosition().y, 0);
    }

    public void goToPreviousLevel() {
        if (currentLevel < 0) return;

        currentLevel--;
        rebuildLevel();

        float width = level.getWidth();
        hero.getBody().setTransform( width - hero.getSize() / 2f, hero.getPosition().y, 0);
    }

    public Hero getHero() {
        return hero;
    }
    public Level getLevel() {
        return level;
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
