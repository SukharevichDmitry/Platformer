package com.cursedarchie.platformer.levels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Final tile rows for a placed, seeded location. Rows are stored top to bottom. */
public final class GeneratedRoomTiles {
    public final PlacedRoomGraph layout;
    public final List<List<String>> rooms;

    GeneratedRoomTiles(PlacedRoomGraph layout, List<List<String>> rooms) {
        this.layout = layout;
        List<List<String>> copy = new ArrayList<>();
        for (List<String> rows : rooms)
            copy.add(Collections.unmodifiableList(new ArrayList<>(rows)));
        this.rooms = Collections.unmodifiableList(copy);
    }

    public List<String> roomRows(int roomId) {
        return rooms.get(roomId);
    }

    public String tileSignature() {
        StringBuilder result = new StringBuilder();
        for (List<String> rows : rooms) {
            for (String row : rows) result.append(row).append('/');
            result.append(';');
        }
        return result.toString();
    }
}
