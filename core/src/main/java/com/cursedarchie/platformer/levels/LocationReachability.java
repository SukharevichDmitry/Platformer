package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.levels.wrappers.InteractionData;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.tiles.types.OneWayPlatform;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Conservative reachability over every room of an authored location, in world cells, on foot and through the
 * location's own portals (descents into rooms below). The hero (one cell
 * tall) stands on a solid or one-way cell; it walks, steps off ledges, drops through one-way platforms and jumps at
 * most {@link #JUMP_RISE} cells up and {@link #JUMP_REACH} across. A jump may go across, up through a gap and across
 * again, which is how the hero climbs through an opening into the room above. The hero's real jump is higher
 * (about 6.7 cells), so a route that passes here is playable.
 */
public final class LocationReachability {
    static final int JUMP_RISE = 5;
    static final int JUMP_REACH = 4;
    /** Interactions are used from within this distance of the hero's centre (GameWorld's interaction range). */
    static final float USE_RANGE = 1.7f;

    private final LocationInstance location;
    private final Set<Long> solid = new HashSet<>();
    private final Set<Long> oneWay = new HashSet<>();
    private final Set<Long> inside = new HashSet<>();
    private final Set<Long> standing = new HashSet<>();

    private LocationReachability(LocationInstance location) {
        this.location = location;
        for (RoomInstance room : location.getRooms()) {
            int ox = Math.round(room.getWorldX()), oy = Math.round(room.getWorldY());
            Tile[][] tiles = room.getTiles();
            for (int x = 0; x < room.getWidth(); x++)
                for (int y = 0; y < room.getHeight(); y++) {
                    long cell = key(ox + x, oy + y);
                    inside.add(cell);
                    Tile tile = tiles[x][y];
                    if (tile == null) continue;
                    if (tile instanceof OneWayPlatform) oneWay.add(cell);
                    else solid.add(cell);
                }
        }
    }

    /** Throws unless every room, interaction and named spawn can be reached on foot from the start spawn. */
    public static void validate(LocationInstance location, int startRoomId, String startSpawnId) {
        LocationReachability reach = new LocationReachability(location);
        RoomInstance start = location.getRoom(startRoomId);
        Vector2 spawn = start == null ? null : start.namedSpawn(startSpawnId);
        if (spawn == null)
            throw new IllegalArgumentException(location.getLocation() + ": missing start spawn " + startSpawnId);
        reach.flood(reach.landAt(spawn));
        // A portal into another room of this location (stairs, hatch, breach) is a route once its door is reached.
        boolean grew = true;
        while (grew) {
            int before = reach.standing.size();
            for (RoomInstance room : location.getRooms())
                for (InteractionData portal : room.getInteractions()) {
                    if (!"portal".equals(portal.kind) || portal.returnToOrigin ||
                        !location.getLocation().equals(portal.targetLocation)) continue;
                    RoomInstance target = location.getRoom(portal.targetRoomId);
                    Vector2 landing = target == null ? null : target.namedSpawn(portal.targetSpawnId);
                    if (landing != null && reach.reachesPoint(room.localToWorld(portal.position)))
                        reach.flood(reach.landAt(landing));
                }
            grew = reach.standing.size() > before;
        }
        for (RoomInstance room : location.getRooms()) {
            if (!reach.reachesRoom(room))
                throw new IllegalArgumentException(location.getLocation() + ": room " + room.getRoomId() +
                    " cannot be reached on foot from " + startSpawnId);
            for (InteractionData interaction : room.getInteractions())
                if (!reach.reachesPoint(room.localToWorld(interaction.position)))
                    throw new IllegalArgumentException(location.getLocation() + ": " + interaction.id +
                        " in room " + room.getRoomId() + " is out of reach");
        }
    }

    private boolean free(int x, int y) {
        long cell = key(x, y);
        return inside.contains(cell) && !solid.contains(cell);
    }

    private boolean support(int x, int y) {
        long cell = key(x, y);
        return solid.contains(cell) || oneWay.contains(cell);
    }

    private boolean canStand(int x, int y) { return free(x, y) && !oneWay.contains(key(x, y)) && support(x, y - 1); }

    private long[] landAt(Vector2 centre) {
        return land((int) Math.floor(centre.x), (int) Math.floor(centre.y - .5f));
    }

    /** Where a hero released at (x, y) comes to rest, or null when it falls out of the location. */
    private long[] land(int x, int y) {
        for (int fy = y; free(x, fy); fy--)
            if (canStand(x, fy)) return new long[] {x, fy};
        return null;
    }

    private void flood(long[] from) {
        if (from == null) return;
        ArrayDeque<long[]> queue = new ArrayDeque<>();
        visit(from, queue);
        while (!queue.isEmpty()) {
            long[] at = queue.poll();
            int x = (int) at[0], y = (int) at[1];
            for (int dx = -1; dx <= 1; dx += 2)
                if (free(x + dx, y)) visit(land(x + dx, y), queue);
            if (oneWay.contains(key(x, y - 1)) && free(x, y - 2)) visit(land(x, y - 2), queue);
            for (int rise = 1; rise <= JUMP_RISE; rise++)
                for (int tx = x - JUMP_REACH; tx <= x + JUMP_REACH; tx++)
                    if (canStand(tx, y + rise) && jumpClear(x, y, tx, y + rise)) visit(new long[] {tx, y + rise}, queue);
        }
    }

    /**
     * Across at the start height, up a clear column, then across at the landing height. Moving sideways at the
     * start height means hopping, which needs a free cell above the hero along that stretch: with no headroom the
     * hero can only jump straight up.
     */
    private boolean jumpClear(int x, int y, int tx, int ty) {
        int lo = Math.min(x, tx) - 1, hi = Math.max(x, tx) + 1;
        for (int cx = lo; cx <= hi; cx++) {
            if (!rowClear(x, cx, y) || !rowClear(cx, tx, ty)) continue;
            if (cx != x && !rowClear(x, cx, y + 1)) continue;
            boolean column = true;
            for (int cy = y; cy <= ty && column; cy++) column = free(cx, cy);
            if (column) return true;
        }
        return false;
    }

    private boolean rowClear(int a, int b, int y) {
        for (int x = Math.min(a, b); x <= Math.max(a, b); x++) if (!free(x, y)) return false;
        return true;
    }

    private void visit(long[] cell, ArrayDeque<long[]> queue) {
        if (cell != null && standing.add(key((int) cell[0], (int) cell[1]))) queue.add(cell);
    }

    private boolean reachesRoom(RoomInstance room) {
        int ox = Math.round(room.getWorldX()), oy = Math.round(room.getWorldY());
        for (long cell : standing) {
            int x = (int) (cell >> 32), y = (int) cell;
            if (x >= ox && x < ox + room.getWidth() && y >= oy && y < oy + room.getHeight()) return true;
        }
        return false;
    }

    /** A point is usable from a standing cell nearby or from the top of a jump made beneath it. */
    private boolean reachesPoint(Vector2 point) {
        for (long cell : standing) {
            float cx = (int) (cell >> 32) + .5f, cy = (int) cell + .5f;
            if (Math.abs(point.x - cx) > USE_RANGE) continue;
            float above = point.y - cy;
            if (above < -USE_RANGE || above > JUMP_RISE + USE_RANGE) continue;
            if (above <= USE_RANGE) return true;
            boolean clear = true;
            for (int y = (int) cy; y <= (int) point.y && clear; y++) clear = free((int) cx, y);
            if (clear) return true;
        }
        return false;
    }

    private static long key(int x, int y) { return ((long) x << 32) | (y & 0xFFFFFFFFL); }
}
