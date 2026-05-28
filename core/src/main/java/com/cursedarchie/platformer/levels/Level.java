package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.controllers.EnemyController;
import com.cursedarchie.platformer.physics.PhysicsCategories;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.utils.EnemySpawnData;
import com.cursedarchie.platformer.utils.TileFactory;

public class Level {

    private final World world;
    private Tile[][] tiles;

    public int width = 10;
    public int height = 7;
    public int[][] tileMap;
    public Array<EnemySpawnData> enemySpawns;
    public Array<Enemy> enemies;

    public Level(World world) {
        tileMap = new int[width][height];
        enemySpawns = new Array<>();
        enemies = new Array<>();
        this.world = world;
    }

    public int[][] getTileMap() {
        return tileMap;
    }

    public Array<EnemySpawnData> getEnemySpawns() {
        return enemySpawns;
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

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }

    public void clearEnemies() {
        enemies.clear();
    }

    public void generateTilesAndBodies() {
        int[][] map = getTileMap();

        tiles = new Tile[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = TileFactory.create(map[y][x], new Vector2(x, height - y - 1));
                tiles[x][height - y - 1] = tile;
            }
        }

        createEdgeBodyForLevel();
    }

    private void createEdgeBodyForLevel() {
        int[][] map = getTileMap();
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

    public void spawnEnemies(EnemyController enemyController) {
        for (EnemySpawnData spawn: getEnemySpawns()) {
            enemyController.spawnEnemy(spawn, world);
        }
    }

    public Tile getTile(int x, int y) {
        if (x < 0 || y < 0 || x >= tiles.length || y >= tiles[0].length) return null;
        return tiles[x][y];
    }

    public Tile[][] getTiles() {
        return tiles;
    }
}
