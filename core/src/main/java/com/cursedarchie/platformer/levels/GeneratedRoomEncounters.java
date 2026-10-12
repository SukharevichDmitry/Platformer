package com.cursedarchie.platformer.levels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Seeded, room-local spawn positions. No physics bodies are created here. */
public final class GeneratedRoomEncounters {
    public static final class Spawn {
        public final String type;
        public final float x;
        public final float y;

        Spawn(String type, float x, float y) {
            this.type = type;
            this.x = x;
            this.y = y;
        }
    }

    public final GeneratedRoomTiles tiles;
    public final List<List<Spawn>> rooms;

    GeneratedRoomEncounters(GeneratedRoomTiles tiles, List<List<Spawn>> rooms) {
        this.tiles = tiles;
        List<List<Spawn>> copy = new ArrayList<>();
        for (List<Spawn> room : rooms)
            copy.add(Collections.unmodifiableList(new ArrayList<>(room)));
        this.rooms = Collections.unmodifiableList(copy);
    }

    public List<Spawn> roomSpawns(int roomId) {
        return rooms.get(roomId);
    }

    public String encounterSignature() {
        StringBuilder result = new StringBuilder();
        for (List<Spawn> room : rooms) {
            for (Spawn spawn : room)
                result.append(spawn.type).append('@').append(spawn.x).append(',').append(spawn.y).append('/');
            result.append(';');
        }
        return result.toString();
    }
}
