package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.levels.wrappers.LocationTransition;
import com.cursedarchie.platformer.levels.wrappers.RoomData;
import com.cursedarchie.platformer.physics.PhysicsCategories;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.levels.wrappers.EnemySpawnData;
import com.cursedarchie.platformer.tiles.TileFactory;

public class RoomInstance {

    private final RoomData data;
    private final RoomRuntime runtime = new RoomRuntime();
    private Tile[][] tiles;

    public RoomInstance(RoomData data) {
        this.data = data;
    }

    public RoomRuntime getRuntime() { return runtime; }
    public Integer getRoomId() { return data.id; }
    public int getWidth() { return data.width; }
    public int getHeight() { return data.height; }
    public Integer getLeftNeighbor() { return data.neighbors.left; }
    public Integer getRightNeighbor() { return data.neighbors.right; }
    public Integer getUpNeighbor() { return data.neighbors.up; }
    public Integer getDownNeighbor() { return data.neighbors.down; }
    public String getLocation() { return data.location; }
    public LocationTransition getLocationExit() { return data.locationExit; }
    public LocationTransition getLocationEntry() { return data.locationEntry; }

    public float getWorldX() {
        return data.worldX;
    }

    public float getWorldY() {
        return data.worldY;
    }

    public void setWorldPosition(
        float worldX,
        float worldY
    ) {
        data.worldX = worldX;
        data.worldY = worldY;
    }

    public boolean contains(
        Vector2 pos
    ) {
        return
            pos.x >= getWorldX()
                &&  pos.x < getWorldX() + getWidth()
                &&  pos.y >= getWorldY()
                &&  pos.y < getWorldY() + getHeight();
    }

    public Vector2 localToWorld(float localX, float localY) {
        return new Vector2(
            getWorldX() + localX,
            getWorldY() + localY
        );
    }

    public Vector2 localToWorld(Vector2 local) {
        return localToWorld(local.x, local.y);
    }


    public Array<EnemySpawnData> getSpawns() { return data.enemySpawns; }
    public Vector2 getWorldSpawnPosition(EnemySpawnData spawn) {
        return new Vector2(
            getWorldX() + spawn.position.x,
            getWorldY() + spawn.position.y
        );
    }
    public Tile[][] getTiles() { return tiles; }


    public void buildTiles() {
        System.out.println(
            "Room " + data.id +
                " worldX=" + data.worldX +
                " data hash=" + System.identityHashCode(data)
        );
        tiles = new Tile[data.width][data.height];

        for (int y = 0; y < data.height; y++) {

            String row = data.tileRows[y];

            for (int x = 0; x < data.width; x++) {

                char c = row.charAt(x);

                Vector2 localPos = new Vector2(
                    x,
                    data.height - y - 1
                );

                tiles[x][data.height - y - 1] =
                    TileFactory.create(c, localPos);
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

                Vector2 worldPos =
                    localToWorld(
                        x,
                        data.height - y - rectHeight
                    );

                createRectangleFixture(
                    worldPos.x,
                    worldPos.y,
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
