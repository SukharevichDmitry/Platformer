package com.cursedarchie.platformer.levels;

import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.world.GameWorld;

import java.util.List;

/** Verifies horizontal walking lanes and clear, climbable vertical joins. */
public final class RoomRouteReachability {
    private RoomRouteReachability() {}

    public static void validate(GeneratedRoomTiles generated) {
        if (generated == null || generated.rooms.size() != generated.layout.rooms.size())
            throw new IllegalArgumentException("Tile rows must match placed rooms");
        if (Hero.WALK_SPEED <= 0 || Hero.BODY_SIZE <= 0 || Hero.BODY_SIZE >= 2 ||
            Hero.JUMP_SPEED <= 0 || GameWorld.GRAVITY <= 0)
            throw new IllegalStateException("Unsupported hero movement configuration");
        for (PlacedRoomGraph.PlacedRoom room : generated.layout.rooms) {
            List<String> rows = generated.roomRows(room.id);
            if (rows.size() != room.height) throw invalid(generated, room.id, "wrong row count");
            for (String row : rows)
                if (row.length() != room.width) throw invalid(generated, room.id, "wrong row width");
            // Four clear rows above the two-row floor preserve the old clearance.
            // Bottom doors are deliberate shaft openings; all other floor remains walkable.
            for (int x = 0; x < room.width; x++) {
                boolean bottomDoor = false;
                for (PlacedRoomGraph.DoorJoin join : generated.layout.doorJoins) {
                    if ((join.fromSide.equals("top") && join.toRoomId == room.id ||
                         join.fromSide.equals("bottom") && join.fromRoomId == room.id) &&
                        room.worldX + x >= join.worldX && room.worldX + x < join.worldX + join.span)
                        bottomDoor = true;
                }
                if (tile(rows, room.height, x, 0) != (bottomDoor ? ' ' : '#') ||
                    tile(rows, room.height, x, 1) != (bottomDoor ? ' ' : '#'))
                    throw invalid(generated, room.id, "floor missing at x=" + x);
                if (x > 0 && x < room.width - 1 &&
                    (tile(rows, room.height, x, 2) != ' ' ||
                    tile(rows, room.height, x, 3) != ' ' ||
                    tile(rows, room.height, x, 4) != ' ' ||
                    tile(rows, room.height, x, 5) != ' '))
                    throw invalid(generated, room.id, "walking corridor blocked at x=" + x);
            }
        }
        for (PlacedRoomGraph.DoorJoin join : generated.layout.doorJoins) {
            if (join.fromSide.equals("top") || join.fromSide.equals("bottom")) {
                validateVertical(generated, join);
                continue;
            }
            if (join.span < 4 || join.worldY != generated.layout.room(join.fromRoomId).worldY + 2 ||
                join.worldY != generated.layout.room(join.toRoomId).worldY + 2)
                throw invalid(generated, join.fromRoomId, "doorway lacks walking-height clearance");
            PlacedRoomGraph.PlacedRoom from = generated.layout.room(join.fromRoomId);
            PlacedRoomGraph.PlacedRoom to = generated.layout.room(join.toRoomId);
            int fromX = join.fromSide.equals("left") ? 0 : from.width - 1;
            int toX = join.fromSide.equals("left") ? to.width - 1 : 0;
            for (int y = 2; y <= 5; y++)
                if (tile(generated.roomRows(from.id), from.height, fromX, y) != ' ' ||
                    tile(generated.roomRows(to.id), to.height, toX, y) != ' ')
                    throw invalid(generated, from.id, "doorway is blocked");
        }
    }

    private static void validateVertical(GeneratedRoomTiles generated, PlacedRoomGraph.DoorJoin join) {
        PlacedRoomGraph.PlacedRoom from = generated.layout.room(join.fromRoomId);
        PlacedRoomGraph.PlacedRoom to = generated.layout.room(join.toRoomId);
        PlacedRoomGraph.PlacedRoom lower = join.fromSide.equals("top") ? from : to;
        PlacedRoomGraph.PlacedRoom upper = join.fromSide.equals("top") ? to : from;
        if (join.span < 2 || join.worldY != lower.worldY + lower.height ||
            join.worldY != upper.worldY || join.span <= Hero.BODY_SIZE)
            throw invalid(generated, from.id, "vertical opening is misaligned or too narrow");
        for (int worldX = join.worldX; worldX < join.worldX + join.span; worldX++) {
            int lowerX = worldX - lower.worldX;
            int upperX = worldX - upper.worldX;
            for (int y = 2; y < lower.height; y++)
                if (tile(generated.roomRows(lower.id), lower.height, lowerX, y) != ' ')
                    throw invalid(generated, lower.id, "climb shaft is blocked");
            for (int y = 0; y <= 5; y++)
                if (tile(generated.roomRows(upper.id), upper.height, upperX, y) != ' ')
                    throw invalid(generated, upper.id, "upper shaft opening is blocked");
        }
    }

    private static char tile(List<String> rows, int height, int x, int y) {
        return rows.get(height - 1 - y).charAt(x);
    }

    private static IllegalArgumentException invalid(GeneratedRoomTiles generated, int roomId, String detail) {
        return new IllegalArgumentException("Seed " + generated.layout.seed + " room " + roomId + ": " + detail);
    }
}
