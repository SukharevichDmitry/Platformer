package com.cursedarchie.platformer.levels;

import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.world.GameWorld;

import java.util.List;

/** Verifies a standing, grounded hero can walk from every doorway to every other doorway. */
public final class RoomRouteReachability {
    private RoomRouteReachability() {}

    public static void validate(GeneratedRoomTiles generated) {
        if (generated == null || generated.rooms.size() != generated.layout.rooms.size())
            throw new IllegalArgumentException("Tile rows must match placed rooms");
        if (Hero.WALK_SPEED <= 0 || Hero.BODY_SIZE <= 0 || Hero.BODY_SIZE >= 1 ||
            Hero.JUMP_SPEED <= 0 || GameWorld.GRAVITY <= 0)
            throw new IllegalStateException("Unsupported hero movement configuration");
        for (PlacedRoomGraph.PlacedRoom room : generated.layout.rooms) {
            List<String> rows = generated.roomRows(room.id);
            if (rows.size() != room.height) throw invalid(generated, room.id, "wrong row count");
            for (String row : rows)
                if (row.length() != room.width) throw invalid(generated, room.id, "wrong row width");
            // A two-tile-high clear lane gives the actual 0.5 tile hero clearance.
            // Its continuous floor means the route needs only the hero's walking action.
            for (int x = 0; x < room.width; x++) {
                if (tile(rows, room.height, x, 0) != '#')
                    throw invalid(generated, room.id, "floor missing at x=" + x);
                if (x > 0 && x < room.width - 1 &&
                    (tile(rows, room.height, x, 1) != ' ' ||
                    tile(rows, room.height, x, 2) != ' '))
                    throw invalid(generated, room.id, "walking corridor blocked at x=" + x);
            }
        }
        for (PlacedRoomGraph.DoorJoin join : generated.layout.doorJoins) {
            if (!join.fromSide.equals("left") && !join.fromSide.equals("right"))
                throw invalid(generated, join.fromRoomId,
                    "vertical doorway has no verified route within the hero's jump limits");
            if (join.span < 2 || join.worldY != generated.layout.room(join.fromRoomId).worldY + 1 ||
                join.worldY != generated.layout.room(join.toRoomId).worldY + 1)
                throw invalid(generated, join.fromRoomId, "doorway lacks walking-height clearance");
            PlacedRoomGraph.PlacedRoom from = generated.layout.room(join.fromRoomId);
            PlacedRoomGraph.PlacedRoom to = generated.layout.room(join.toRoomId);
            int fromX = join.fromSide.equals("left") ? 0 : from.width - 1;
            int toX = join.fromSide.equals("left") ? to.width - 1 : 0;
            for (int y = 1; y <= 2; y++)
                if (tile(generated.roomRows(from.id), from.height, fromX, y) != ' ' ||
                    tile(generated.roomRows(to.id), to.height, toX, y) != ' ')
                    throw invalid(generated, from.id, "doorway is blocked");
        }
    }

    private static char tile(List<String> rows, int height, int x, int y) {
        return rows.get(height - 1 - y).charAt(x);
    }

    private static IllegalArgumentException invalid(GeneratedRoomTiles generated, int roomId, String detail) {
        return new IllegalArgumentException("Seed " + generated.layout.seed + " room " + roomId + ": " + detail);
    }
}
