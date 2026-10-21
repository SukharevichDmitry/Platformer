package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.files.FileHandle;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic graph checks before coordinates and tiles are generated. */
public final class RoomGraphGenerationCheck {
    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("Expected assets directory");
        FileHandle generation = new FileHandle(new File(args[0], "generation"));
        GenerationProfile profile = GenerationProfile.load(generation.child("ruins.json"));
        RoomTemplateCatalog catalog = RoomTemplateCatalog.load(generation.child("ruins.templates.json"), profile);
        RoomGraphGenerator generator = new RoomGraphGenerator();

        Set<Integer> roomCounts = new HashSet<>();
        Set<String> layouts = new HashSet<>();
        boolean sawBranch = false;
        boolean sawSideBranch = false;
        for (long seed = 0; seed < 1000; seed++) {
            GeneratedRoomGraph graph = generator.generate(profile, catalog, seed);
            GeneratedRoomGraph repeated = generator.generate(profile, catalog, seed);
            if (graph.seed != seed || !graph.topologySignature().equals(repeated.topologySignature()))
                throw new AssertionError("Seed " + seed + " was not deterministic");
            checkGraph(graph, profile, catalog);
            roomCounts.add(graph.rooms.size());
            layouts.add(graph.topologySignature());
            if (graph.rooms.size() > graph.mainRoute.size()) sawBranch = true;
            if (graph.rooms.size() > graph.mainRoute.size() + 1) sawSideBranch = true;
        }
        for (long seed : new long[]{-1L, Long.MIN_VALUE, Long.MAX_VALUE})
            checkGraph(generator.generate(profile, catalog, seed), profile, catalog);
        if (!roomCounts.equals(new HashSet<>(Arrays.asList(5, 6, 7, 8, 9, 10))) ||
            layouts.size() < 20 || !sawBranch || !sawSideBranch)
            throw new AssertionError("Seeds did not explore the permitted counts, branches, and template choices");
        System.out.println("ROOM GRAPH PASSED: 1000 reproducible seeds, 5-10 rooms, quotas, story order and connectivity");
    }

    private static void checkGraph(GeneratedRoomGraph graph, GenerationProfile profile, RoomTemplateCatalog catalog) {
        if (!graph.locationId.equals(profile.locationId) || graph.rooms.size() < profile.roomCount.min ||
            graph.rooms.size() > profile.roomCount.max || graph.mainRoute.size() < catalog.requiredRoute.size())
            throw new AssertionError("Graph violates location room count");
        Map<String, Integer> patternCounts = new HashMap<>();
        Set<Integer> seen = new HashSet<>();
        ArrayDeque<Integer> pending = new ArrayDeque<>();
        pending.add(0);
        while (!pending.isEmpty()) {
            int id = pending.removeFirst();
            if (!seen.add(id)) continue;
            pending.addAll(graph.room(id).neighbors.values());
        }
        if (seen.size() != graph.rooms.size()) throw new AssertionError("Graph is disconnected");

        for (int i = 0; i < graph.rooms.size(); i++) {
            GeneratedRoomGraph.RoomNode room = graph.room(i);
            if (room.id != i || !catalog.templates.containsKey(room.templateId))
                throw new AssertionError("Room IDs or template references are unstable");
            patternCounts.put(room.patternId, patternCounts.getOrDefault(room.patternId, 0) + 1);
            for (Map.Entry<String, Integer> link : room.neighbors.entrySet()) {
                int otherId = link.getValue();
                if (otherId < 0 || otherId >= graph.rooms.size() ||
                    !Integer.valueOf(i).equals(graph.room(otherId).neighbors.get(opposite(link.getKey()))))
                    throw new AssertionError("Nonreciprocal room link at " + i);
            }
        }
        for (GenerationProfile.RoomPattern pattern : profile.roomPatterns) {
            int count = patternCounts.getOrDefault(pattern.id, 0);
            if (count < pattern.roomQuota.min || count > pattern.roomQuota.max)
                throw new AssertionError("Pattern quota failed for " + pattern.id);
        }

        List<String> anchors = new ArrayList<>();
        for (int i = 0; i < graph.mainRoute.size(); i++) {
            int id = graph.mainRoute.get(i);
            if (id != i) throw new AssertionError("Main route IDs are not stable");
            GeneratedRoomGraph.RoomNode room = graph.room(id);
            if (!room.role.equals("optional")) anchors.add(room.templateId);
            if (i + 1 < graph.mainRoute.size() &&
                !Integer.valueOf(i + 1).equals(room.neighbors.get("right")))
                throw new AssertionError("Main route has a broken forward link");
        }
        if (!anchors.equals(catalog.requiredRoute))
            throw new AssertionError("Required story order changed: " + anchors);
        int bellId = -1;
        for (int id : graph.mainRoute)
            if (graph.room(id).templateId.equals("bell_hall")) bellId = id;
        if (bellId < 0 || !graph.room(graph.room(bellId).neighbors.get("top")).templateId.equals("watchtower"))
            throw new AssertionError("Generated ruins lack the reachable upper room");
        for (int i = graph.mainRoute.size(); i < graph.rooms.size(); i++) {
            GeneratedRoomGraph.RoomNode branch = graph.room(i);
            if (!branch.role.equals("optional") || branch.neighbors.size() != 1)
                throw new AssertionError("Optional branch is not a leaf");
            int anchorId = branch.neighbors.values().iterator().next();
            if (!catalog.requiredRoute.contains(graph.room(anchorId).templateId))
                throw new AssertionError("Optional branch does not attach to a story anchor");
        }
    }

    private static String opposite(String side) {
        switch (side) {
            case "left": return "right";
            case "right": return "left";
            case "top": return "bottom";
            case "bottom": return "top";
            default: throw new AssertionError("Unknown side " + side);
        }
    }
}
