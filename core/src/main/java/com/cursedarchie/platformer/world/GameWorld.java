package com.cursedarchie.platformer.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.cursedarchie.platformer.controllers.EnemyController;
import com.cursedarchie.platformer.controllers.HeroController;
import com.cursedarchie.platformer.physics.GameContactListener;
import com.cursedarchie.platformer.physics.PhysicsCategories;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.levels.Level;
import com.cursedarchie.platformer.utils.*;

public class GameWorld {

    private final World world;
    private final Hero hero;
    private final HeroController heroController;
    private final EnemyController enemyController;
    private Level level;
    private int currentLevel;
    private Tile[][] tiles;

    public GameWorld() {
        this.world = new World(new Vector2(0, -10), true);
        this.currentLevel = 0;
        this.level = loadLevel(currentLevel);
        this.hero = new Hero(new Vector2(5, 5), world);
        world.setContactListener(new GameContactListener(hero));

        this.heroController = new HeroController(this);
        this.enemyController = new EnemyController(this);

        rebuildLevel();
    }

    public void update(float delta) {
        delta = Math.min(delta, 1f/30f);

        world.step(delta, 6, 2);

        heroController.update(delta);
        enemyController.update(delta);

        hero.getMotionStateMachine().update(delta);
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
            return json.fromJson(Level.class, file);
        } catch (Exception e) {
            System.err.println("Error while loading level: " + path);
            e.printStackTrace();
            return null;
        }
    }

    private void rebuildLevel() {
        clearLevelBodies();
        level = loadLevel(currentLevel);
        generateTilesAndBodies(level);
        spawnEnemies(level);
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

    private void generateTilesAndBodies(Level level) {
        int[][] map = level.getTileMap();
        int height = level.getHeight();
        int width = level.getWidth();

        tiles = new Tile[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = TileFactory.create(map[y][x], new Vector2(x, height - y - 1));
                tiles[x][height - y - 1] = tile;
            }
        }

        createEdgeBodyForLevel(level);
    }

    private void createEdgeBodyForLevel(Level level) {
        int[][] map = level.getTileMap();
        int width = level.getWidth();
        int height = level.getHeight();
        float tileSize = 1f;

        // -------------------------
        // 1. Горизонтальные верхние грани
        // -------------------------
        for (int y = 0; y < height; y++) {
            int startX = -1;
            for (int x = 0; x <= width; x++) {
                boolean isTile = (x < width) && (map[y][x] != 0);

                if (isTile && startX == -1) startX = x;
                if ((!isTile || x == width) && startX != -1) {
                    int endX = (x == width) ? width - 1 : x - 1;

                    Vector2[] topEdge = new Vector2[]{
                        new Vector2(startX * tileSize, (height - y) * tileSize),
                        new Vector2((endX + 1) * tileSize, (height - y) * tileSize)
                    };
                    createChainShape(topEdge);

                    startX = -1;
                }
            }
        }

        // -------------------------
        // 2. Вертикальные боковые грани
        // -------------------------
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (map[y][x] == 0) continue;

                float leftX = x * tileSize;
                float rightX = (x + 1) * tileSize;
                float topY = (height - y) * tileSize;
                float bottomY = (height - y - 1) * tileSize;

                if (x == 0 || map[y][x - 1] == 0) {
                    createChainShape(new Vector2[]{
                        new Vector2(leftX, bottomY),
                        new Vector2(leftX, topY)
                    });
                }

                if (x == width - 1 || map[y][x + 1] == 0) {
                    createChainShape(new Vector2[]{
                        new Vector2(rightX, bottomY),
                        new Vector2(rightX, topY)
                    });
                }
            }
        }

        // -------------------------
        // 3. Нижние грани
        // -------------------------
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (map[y][x] == 0) continue;

                boolean noTileBelow = (y == height - 1) || (map[y + 1][x] == 0);

                if (noTileBelow) {
                    float leftX = x * tileSize;
                    float rightX = (x + 1) * tileSize;
                    float bottomY = (height - y - 1) * tileSize;

                    Vector2[] bottomEdge = new Vector2[]{
                        new Vector2(leftX, bottomY),
                        new Vector2(rightX, bottomY)
                    };
                    createChainShape(bottomEdge);
                }
            }
        }
    }

    private void createChainShape(Vector2[] vertices) {
        ChainShape chain = new ChainShape();
        chain.createChain(vertices);

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        Body body = world.createBody(bodyDef);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = chain;
        fixtureDef.friction = 0.8f;
        fixtureDef.filter.categoryBits = PhysicsCategories.WALL;
        fixtureDef.filter.maskBits = PhysicsCategories.HERO | PhysicsCategories.ENEMY;

        Fixture fixture = body.createFixture(fixtureDef);
        fixture.setUserData("level");
        body.setUserData("level");

        chain.dispose();
    }

    private void spawnEnemies(Level level) {
        for (EnemySpawnData spawn: level.getEnemySpawns()) {
            enemyController.spawnEnemy(spawn, world);
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
    public Tile getTile(int x, int y) {
        if (x < 0 || y < 0 || x >= tiles.length || y >= tiles[0].length) return null;
        return tiles[x][y];
    }
    public Tile[][] getTiles() {
        return tiles;
    }
}
