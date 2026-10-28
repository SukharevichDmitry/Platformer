package com.cursedarchie.platformer.levels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Integer world coordinates and joined openings for a generated room graph. */
public final class PlacedRoomGraph {
    public static final class PlacedRoom {
        public final int id;
        public final String templateId;
        public final int worldX;
        public final int worldY;
        public final int width;
        public final int height;
        public final List<String> tileRows;

        PlacedRoom(int id, RoomTemplateCatalog.RoomTemplate template, int width, int height,
                   List<String> selectedRows, Map<String, Integer> neighbors, int worldX, int worldY) {
            this.id = id;
            this.templateId = template.id;
            this.worldX = worldX;
            this.worldY = worldY;
            this.width = width;
            this.height = height;
            List<char[]> rows = new ArrayList<>();
            for (String row : selectedRows) rows.add(row.toCharArray());
            for (RoomTemplateCatalog.Door door : template.doors) {
                if (neighbors.containsKey(door.side)) continue;
                for (int i = door.offset; i < door.offset + door.span; i++) {
                    int x = door.side.equals("left") ? 0 : door.side.equals("right") ? width - 1 : i;
                    int y = door.side.equals("bottom") ? 0 : door.side.equals("top") ? height - 1 : i;
                    rows.get(height - 1 - y)[x] = '#';
                }
            }
            List<String> sealed = new ArrayList<>();
            for (char[] row : rows) sealed.add(new String(row));
            this.tileRows = Collections.unmodifiableList(sealed);
        }
    }

    public static final class DoorJoin {
        public final int fromRoomId;
        public final int toRoomId;
        public final String fromSide;
        public final int worldX;
        public final int worldY;
        public final int span;

        DoorJoin(int fromRoomId, int toRoomId, String fromSide, int worldX, int worldY, int span) {
            this.fromRoomId = fromRoomId;
            this.toRoomId = toRoomId;
            this.fromSide = fromSide;
            this.worldX = worldX;
            this.worldY = worldY;
            this.span = span;
        }
    }

    public final String locationId;
    public final long seed;
    public final List<PlacedRoom> rooms;
    public final List<DoorJoin> doorJoins;
    public final int minX;
    public final int minY;
    public final int maxX;
    public final int maxY;

    PlacedRoomGraph(GeneratedRoomGraph graph, List<PlacedRoom> rooms, List<DoorJoin> doorJoins) {
        this.locationId = graph.locationId;
        this.seed = graph.seed;
        this.rooms = Collections.unmodifiableList(new ArrayList<>(rooms));
        this.doorJoins = Collections.unmodifiableList(new ArrayList<>(doorJoins));
        int left = Integer.MAX_VALUE;
        int bottom = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;
        int top = Integer.MIN_VALUE;
        for (PlacedRoom room : rooms) {
            left = Math.min(left, room.worldX);
            bottom = Math.min(bottom, room.worldY);
            right = Math.max(right, room.worldX + room.width);
            top = Math.max(top, room.worldY + room.height);
        }
        this.minX = left;
        this.minY = bottom;
        this.maxX = right;
        this.maxY = top;
    }

    public PlacedRoom room(int id) {
        return rooms.get(id);
    }

    /** Stable text for reproducing a placed seed. */
    public String placementSignature() {
        StringBuilder result = new StringBuilder();
        for (PlacedRoom room : rooms)
            result.append(room.id).append(':').append(room.templateId).append('@')
                .append(room.worldX).append(',').append(room.worldY).append(':')
                .append(room.width).append('x').append(room.height).append(';');
        return result.toString();
    }
}
