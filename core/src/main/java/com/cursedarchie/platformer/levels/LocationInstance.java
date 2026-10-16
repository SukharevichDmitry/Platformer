package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.cursedarchie.platformer.physics.PhysicsCategories;
import com.cursedarchie.platformer.tiles.Tile;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Loaded authored or generated rooms plus their shared terrain body. */
public class LocationInstance {

    private final String location;

    private final Map<Integer, RoomInstance> rooms =
        new HashMap<>();
    private Body levelBody;
    private Long generationSeed;
    private int entryRoomId;
    private GeneratedRunSave generatedRun;

    public LocationInstance(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    public boolean isGenerated() { return generationSeed != null; }
    public Long getGenerationSeed() { return generationSeed; }
    public int getEntryRoomId() { return entryRoomId; }
    public GeneratedRunSave getGeneratedRun() { return generatedRun; }

    void setGeneration(GeneratedRunSave save, int entryRoomId) {
        this.generatedRun = save;
        this.generationSeed = save.seed;
        this.entryRoomId = entryRoomId;
    }

    public void addRoom(RoomInstance room) {
        rooms.put(room.getRoomId(), room);
    }

    public RoomInstance getRoom(int id) {
        return rooms.get(id);
    }

    public Collection<RoomInstance> getRooms() {
        return rooms.values();
    }

    public void buildBodies(World world) {
        if (levelBody != null) return;

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        levelBody = world.createBody(bodyDef);
        levelBody.setUserData("level");

        // Merge full solid cells across room borders so floors have no internal edge.
        // Partial tiles are deliberately excluded: their bounds are not full cells.
        Set<Long> solid = new TreeSet<>();
        for (RoomInstance room : rooms.values()) {
            int roomX = Math.round(room.getWorldX());
            int roomY = Math.round(room.getWorldY());
            Tile[][] tiles = room.getTiles();
            for (int x = 0; x < tiles.length; x++) {
                for (int y = 0; y < tiles[x].length; y++) {
                    Tile tile = tiles[x][y];
                    if (tile != null && tile.isFullCellSolid()) {
                        solid.add(cellKey(roomX + x, roomY + y));
                    }
                }
            }
        }

        Set<Long> used = new HashSet<>();
        for (long cell : solid) {
            if (used.contains(cell)) continue;
            int x = (int) (cell >> 32);
            int y = (int) cell;
            int width = 0;
            while (solid.contains(cellKey(x + width, y)) && !used.contains(cellKey(x + width, y))) width++;

            int height = 1;
            boolean canGrow = true;
            while (canGrow) {
                for (int dx = 0; dx < width; dx++) {
                    long next = cellKey(x + dx, y + height);
                    if (!solid.contains(next) || used.contains(next)) {
                        canGrow = false;
                        break;
                    }
                }
                if (canGrow) height++;
            }

            for (int dy = 0; dy < height; dy++) {
                for (int dx = 0; dx < width; dx++) used.add(cellKey(x + dx, y + dy));
            }
            createRectangleFixture(x, y, width, height);
        }

        // Partial shapes use their Tile bounds for both visible and collision geometry.
        for (RoomInstance room : rooms.values()) {
            Tile[][] tiles = room.getTiles();
            for (int x = 0; x < tiles.length; x++) {
                for (int y = 0; y < tiles[x].length; y++) {
                    Tile tile = tiles[x][y];
                    if (tile == null || !tile.isSolid() || tile.isFullCellSolid()) continue;
                    Rectangle bounds = tile.getBounds();
                    createRectangleFixture(room.getWorldX() + bounds.x,
                        room.getWorldY() + bounds.y, bounds.width, bounds.height);
                }
            }
        }
    }

    public void destroyBodies(World world) {
        if (levelBody == null) return;
        world.destroyBody(levelBody);
        levelBody = null;
    }

    private static long cellKey(int x, int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }

    private void createRectangleFixture(float x, float y, float width, float height) {
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2f, height / 2f,
            new Vector2(x + width / 2f, y + height / 2f), 0);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.friction = 0.8f;
        fixtureDef.filter.categoryBits = PhysicsCategories.WALL;
        fixtureDef.filter.maskBits = PhysicsCategories.HERO | PhysicsCategories.ENEMY;
        levelBody.createFixture(fixtureDef).setUserData("level");
        shape.dispose();
    }
}
