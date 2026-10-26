package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.cursedarchie.platformer.physics.PhysicsCategories;
import com.cursedarchie.platformer.physics.OneWaySurface;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.tiles.TileMetrics;
import com.cursedarchie.platformer.tiles.types.BreakableBlock;
import com.cursedarchie.platformer.tiles.types.HalfOfBlock;
import com.cursedarchie.platformer.tiles.types.OneWayPlatform;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.IdentityHashMap;
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
    private final List<VerticalConnection> verticalConnections = new ArrayList<>();
    private final Map<Tile, Fixture> independentFixtures = new IdentityHashMap<>();

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
    public List<VerticalConnection> getVerticalConnections() {
        return Collections.unmodifiableList(verticalConnections);
    }

    void addVerticalConnection(VerticalConnection connection) {
        verticalConnections.add(connection);
    }

    public VerticalConnection climbConnectionAt(float x, float y, float heroWidth) {
        for (VerticalConnection connection : verticalConnections)
            if (connection.contains(x, y, heroWidth)) return connection;
        return null;
    }

    void setGeneration(GeneratedRunSave save, int entryRoomId) {
        this.generatedRun = save;
        this.generationSeed = save.seed;
        this.entryRoomId = entryRoomId;
        for (String id : save.brokenTileIds) {
            String[] parts = id.split(":");
            if (parts.length != 3) continue;
            try {
                RoomInstance room = getRoom(Integer.parseInt(parts[0]));
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                if (room != null && x >= 0 && x < room.getTileColumns() && y >= 0 && y < room.getTileRows() &&
                    room.getTiles()[x][y] instanceof BreakableBlock) room.removeTile(x, y);
            } catch (NumberFormatException ignored) { /* Invalid stale IDs cannot affect terrain. */ }
        }
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
            int roomX = Math.round(room.getWorldX() * TileMetrics.CELLS_PER_UNIT);
            int roomY = Math.round(room.getWorldY() * TileMetrics.CELLS_PER_UNIT);
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
            createRectangleFixture(x * TileMetrics.SIZE, y * TileMetrics.SIZE,
                width * TileMetrics.SIZE, height * TileMetrics.SIZE);
        }

        // Merge adjacent partial ledges too. Separate rectangles leave an end edge
        // at every tile seam that can catch a walking body.
        Set<Long> halfBlocks = new TreeSet<>();
        Set<Long> oneWayPlatforms = new TreeSet<>();
        for (RoomInstance room : rooms.values()) {
            int roomX = Math.round(room.getWorldX() * TileMetrics.CELLS_PER_UNIT);
            int roomY = Math.round(room.getWorldY() * TileMetrics.CELLS_PER_UNIT);
            Tile[][] tiles = room.getTiles();
            for (int x = 0; x < tiles.length; x++) {
                for (int y = 0; y < tiles[x].length; y++) {
                    Tile tile = tiles[x][y];
                    if (tile == null || !tile.isSolid() || tile.isFullCellSolid()) continue;
                    if (tile instanceof HalfOfBlock) {
                        halfBlocks.add(cellKey(roomX + x, roomY + y));
                        continue;
                    }
                    if (tile instanceof OneWayPlatform) {
                        oneWayPlatforms.add(cellKey(roomX + x, roomY + y));
                        continue;
                    }
                    // Breakable tiles retain independent fixtures for removal.
                    Rectangle bounds = tile.getBounds();
                    Fixture fixture = createRectangleFixture(room.getWorldX() + bounds.x,
                        room.getWorldY() + bounds.y, bounds.width, bounds.height);
                    if (tile instanceof BreakableBlock) {
                        fixture.setUserData(tile);
                        independentFixtures.put(tile, fixture);
                    }
                }
            }
        }
        createLedgeRuns(halfBlocks, 0f, HalfOfBlock.HEIGHT, false);
        createLedgeRuns(oneWayPlatforms, OneWayPlatform.BOTTOM_OFFSET,
            OneWayPlatform.HEIGHT, true);
    }

    public void destroyBodies(World world) {
        if (levelBody == null) return;
        world.destroyBody(levelBody);
        levelBody = null;
        independentFixtures.clear();
    }

    /** Called outside world.step after an active hero attack has hit this tile. */
    public boolean breakTile(int roomId, int x, int y) {
        RoomInstance room = getRoom(roomId);
        if (room == null || x < 0 || x >= room.getTileColumns() || y < 0 || y >= room.getTileRows()) return false;
        Tile tile = room.getTiles()[x][y];
        if (!(tile instanceof BreakableBlock)) return false;
        Fixture fixture = independentFixtures.remove(tile);
        if (fixture != null) levelBody.destroyFixture(fixture);
        room.removeTile(x, y);
        if (isGenerated()) {
            String id = roomId + ":" + x + ":" + y;
            if (!generatedRun.brokenTileIds.contains(id, false)) generatedRun.brokenTileIds.add(id);
        }
        return true;
    }

    private static long cellKey(int x, int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }

    private void createLedgeRuns(Set<Long> cells, float bottomOffset, float height,
                                 boolean oneWay) {
        for (long cell : cells) {
            int x = (int) (cell >> 32);
            int y = (int) cell;
            if (cells.contains(cellKey(x - 1, y))) continue;
            int width = 1;
            while (cells.contains(cellKey(x + width, y))) width++;
            float worldX = x * TileMetrics.SIZE;
            float worldY = y * TileMetrics.SIZE;
            Fixture fixture = createRectangleFixture(worldX, worldY + bottomOffset,
                width * TileMetrics.SIZE, height);
            if (oneWay) fixture.setUserData(new OneWaySurface(worldY + bottomOffset + height));
        }
    }

    private Fixture createRectangleFixture(float x, float y, float width, float height) {
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2f, height / 2f,
            new Vector2(x + width / 2f, y + height / 2f), 0);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.friction = 0.8f;
        fixtureDef.filter.categoryBits = PhysicsCategories.WALL;
        fixtureDef.filter.maskBits = PhysicsCategories.HERO | PhysicsCategories.ENEMY;
        Fixture fixture = levelBody.createFixture(fixtureDef);
        fixture.setUserData("level");
        shape.dispose();
        return fixture;
    }
}
