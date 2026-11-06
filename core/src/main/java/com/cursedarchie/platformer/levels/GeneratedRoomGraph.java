package com.cursedarchie.platformer.levels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Seeded topology before room coordinates, tiles, and encounters are generated. */
public final class GeneratedRoomGraph {
    public static final class RoomNode {
        public final int id;
        public final String templateId;
        public final String patternId;
        public final String role;
        public final Map<String, Integer> neighbors;

        RoomNode(int id, RoomTemplateCatalog.RoomTemplate template, Map<String, Integer> neighbors) {
            this.id = id;
            this.templateId = template.id;
            this.patternId = template.patternId;
            this.role = template.role;
            this.neighbors = Collections.unmodifiableMap(new LinkedHashMap<>(neighbors));
        }
    }

    public final String locationId;
    public final long seed;
    public final List<RoomNode> rooms;
    public final List<Integer> mainRoute;

    GeneratedRoomGraph(String locationId, long seed, List<RoomNode> rooms, List<Integer> mainRoute) {
        this.locationId = locationId;
        this.seed = seed;
        this.rooms = Collections.unmodifiableList(new ArrayList<>(rooms));
        this.mainRoute = Collections.unmodifiableList(new ArrayList<>(mainRoute));
    }

    public RoomNode room(int id) {
        return rooms.get(id);
    }

    /** Stable text for a seed report or regression comparison. */
    public String topologySignature() {
        StringBuilder result = new StringBuilder();
        for (RoomNode room : rooms) {
            result.append(room.id).append(':').append(room.templateId);
            for (String side : new String[]{"left", "right", "top", "bottom"}) {
                Integer neighbor = room.neighbors.get(side);
                if (neighbor != null) result.append(':').append(side).append('=').append(neighbor);
            }
            result.append(';');
        }
        return result.toString();
    }
}
