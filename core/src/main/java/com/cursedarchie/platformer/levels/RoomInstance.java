package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.levels.wrappers.LocationTransition;
import com.cursedarchie.platformer.levels.wrappers.RoomCoord;
import com.cursedarchie.platformer.levels.wrappers.RoomData;
import com.cursedarchie.platformer.physics.PhysicsCategories;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.levels.wrappers.EnemySpawnData;
import com.cursedarchie.platformer.tiles.TileFactory;

public class RoomInstance {

    private final RoomData data;

    private Tile[][] tiles;

    private final RoomRuntime runtime = new RoomRuntime();

    private boolean enemiesSpawned;

    public RoomInstance(RoomData data) {
        this.data = data;
    }

    public int getWidth() { return data.width; }
    public int getHeight() { return data.height; }
    public Integer getLeftNeighbor() { return data.neighbors.left; }
    public Integer getRightNeighbor() { return data.neighbors.right; }
    public Integer getUpNeighbor() { return data.neighbors.up; }
    public Integer getDownNeighbor() { return data.neighbors.down; }
    public String getLocation() { return data.location; }
    public Integer getRoomId() { return data.id; }
    public RoomCoord getRoomCoord() { return data.coord; }
    public LocationTransition getLocationExit() { return data.locationExit; }
    public LocationTransition getLocationEntry() { return data.locationEntry; }

    public RoomRuntime getRuntime() { return runtime; }
    public Array<EnemySpawnData> getSpawns() { return data.enemySpawns; }
    public Tile[][] getTiles() { return tiles; }

    public boolean isEnemiesSpawned() {
        return enemiesSpawned;
    }
    public void setEnemiesSpawned(boolean enemiesSpawned) {
        this.enemiesSpawned = enemiesSpawned;
    }

    public void buildTiles() {

        tiles = new Tile[data.width][data.height];

        for (int y = 0; y < data.height; y++) {

            String row = data.tileRows[y];

            for (int x = 0; x < data.width; x++) {

                char c = row.charAt(x);

                Vector2 pos = new Vector2(
                    x,
                    data.height - y - 1
                );

                tiles[x][data.height - y - 1] =
                    TileFactory.create(c, pos);
            }
        }
    }

    public void buildBodies(World world) {

        if (runtime.getLevelBody() == null) {
            runtime.build(world);
        }

        boolean[][] used = new boolean[data.height][data.width];

        for (int y = 0; y < data.height; y++) {
            for (int x = 0; x < data.width; x++) {

                if (used[y][x]) continue;

                Tile tile = getTileForMapCoordinates(x, y);

                if (tile == null || !tile.isSolid()) {
                    continue;
                }

                int rectWidth = 0;

                while (x + rectWidth < data.width && !used[y][x + rectWidth]) {
                    Tile nextTile = getTileForMapCoordinates(x + rectWidth, y);

                    if (nextTile == null || !nextTile.isSolid()) {
                        break;
                    }

                    rectWidth++;
                }

                int rectHeight = 1;
                boolean canGrow = true;

                while (canGrow && y + rectHeight < data.height) {

                    for (int dx = 0; dx < rectWidth; dx++) {
                        Tile nextTile = getTileForMapCoordinates(x + dx, y + rectHeight);

                        if (nextTile == null || !nextTile.isSolid() || used[y + rectHeight][x + dx]) {
                            canGrow = false;
                            break;
                        }
                    }

                    if (canGrow) {
                        rectHeight++;
                    }
                }

                for (int dy = 0; dy < rectHeight; dy++) {
                    for (int dx = 0; dx < rectWidth; dx++) {
                        used[y + dy][x + dx] = true;
                    }
                }

                float worldX = x;
                float worldY = data.height - y - rectHeight;

                createRectangleFixture(
                    worldX,
                    worldY,
                    rectWidth,
                    rectHeight
                );
            }
        }
    }

    private Tile getTileForMapCoordinates(int x, int y) {
        return tiles[x][data.height - y - 1];
    }
    private void createRectangleFixture(
        float x,
        float y,
        float width,
        float height
    ) {

        PolygonShape shape = new PolygonShape();

        shape.setAsBox(
            width / 2f,
            height / 2f,
            new Vector2(
                x + width / 2f,
                y + height / 2f
            ),
            0
        );

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.friction = 0.8f;

        fixtureDef.filter.categoryBits =
            PhysicsCategories.WALL;

        fixtureDef.filter.maskBits =
            PhysicsCategories.HERO |
                PhysicsCategories.ENEMY;

        Fixture fixture = runtime.getLevelBody().createFixture(fixtureDef);

        fixture.setUserData("level");

        shape.dispose();
    }
}
