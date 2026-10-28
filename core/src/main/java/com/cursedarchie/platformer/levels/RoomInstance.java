package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.levels.wrappers.LocationTransition;
import com.cursedarchie.platformer.levels.wrappers.RoomData;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.levels.wrappers.EnemySpawnData;
import com.cursedarchie.platformer.levels.wrappers.InteractionData;
import com.cursedarchie.platformer.tiles.TileFactory;
import com.cursedarchie.platformer.tiles.TileMetrics;

public class RoomInstance {

    private final RoomData data;
    private Tile[][] tiles;

    public RoomInstance(RoomData data) {
        this.data = data;
    }

    public Integer getRoomId() { return data.id; }
    public int getWidth() { return data.width; }
    public int getHeight() { return data.height; }
    public Integer getLeftNeighbor() { return data.neighbors.left; }
    public Integer getRightNeighbor() { return data.neighbors.right; }
    public Integer getUpNeighbor() { return data.neighbors.up; }
    public Integer getDownNeighbor() { return data.neighbors.down; }
    public String getLocation() { return data.location; }
    public String getSceneryTheme() { return data.sceneryTheme; }
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
    public Array<InteractionData> getInteractions() { return data.interactions; }
    public Vector2 getWorldSpawnPosition(EnemySpawnData spawn) {
        return new Vector2(
            getWorldX() + spawn.position.x,
            getWorldY() + spawn.position.y
        );
    }
    public Tile[][] getTiles() { return tiles; }
    public int getTileColumns() { return data.width; }
    public int getTileRows() { return data.height; }

    public void removeTile(int x, int y) { tiles[x][y] = null; }


    public void buildTiles() {
        tiles = new Tile[getTileColumns()][getTileRows()];

        for (int y = 0; y < data.height; y++) {

            String row = data.tileRows[y];

            for (int x = 0; x < data.width; x++) {

                char c = row.charAt(x);

                if (c == ' ') continue;
                int cellY = data.height - y - 1;
                tiles[x][cellY] = TileFactory.create(c, new Vector2(x, cellY));
            }
        }
    }

}
