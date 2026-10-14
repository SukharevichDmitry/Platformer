package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/** Runs the complete procedural pipeline and writes tuning statistics for a reproducible seed range. */
public final class GenerationQualityCheck {
    public static final class Report {
        public String profile;
        public long firstSeed;
        public int seedCount;
        public int minRooms = Integer.MAX_VALUE;
        public int maxRooms;
        public double averageRooms;
        public double averageMainRouteRooms;
        public double averageEnemies;
        public Map<String, Integer> templates = new LinkedHashMap<>();
        public Map<String, Integer> enemyTypes = new LinkedHashMap<>();
    }

    public static void main(String[] args) {
        if (args.length != 4)
            throw new IllegalArgumentException("Expected assets directory, report file, first seed, and seed count");
        FileHandle generation = new FileHandle(new File(args[0], "generation"));
        FileHandle reportFile = new FileHandle(args[1]);
        long firstSeed = Long.parseLong(args[2]);
        int count = Integer.parseInt(args[3]);
        if (count < 1) throw new IllegalArgumentException("Seed count must be positive");

        GenerationProfile profile = GenerationProfile.load(generation.child("ruins.json"));
        RoomTemplateCatalog catalog = RoomTemplateCatalog.load(generation.child("ruins.templates.json"), profile);
        Report report = new Report();
        report.profile = profile.locationId;
        report.firstSeed = firstSeed;
        report.seedCount = count;
        long roomTotal = 0;
        long routeTotal = 0;
        long enemyTotal = 0;

        for (long seed = firstSeed; seed < firstSeed + count; seed++) {
            final long currentSeed = seed;
            try {
                GeneratedRoomGraph graph = rule(currentSeed, "schema/connectivity/doorways",
                    () -> new RoomGraphGenerator().generate(profile, catalog, currentSeed));
                PlacedRoomGraph layout = rule(currentSeed, "collision/layout",
                    () -> new RoomLayoutPlacer().place(graph, catalog, profile));
                verifyNoOverlap(layout, currentSeed);
                GeneratedRoomTiles tiles = rule(currentSeed, "jump/reachability",
                    () -> new RoomTileGenerator().generate(layout, catalog));
                rule(currentSeed, "jump/reachability", () -> { RoomRouteReachability.validate(tiles); return tiles; });
                GeneratedRoomEncounters encounters = rule(currentSeed, "spawn",
                    () -> new RoomEncounterGenerator().generate(tiles, catalog, profile));
                rule(currentSeed, "spawn", () -> { RoomEncounterGenerator.validate(encounters, catalog, profile); return encounters; });
                verifyTransition(layout, profile, new FileHandle(args[0]), currentSeed);

                int enemies = 0;
                for (PlacedRoomGraph.PlacedRoom room : layout.rooms) {
                    increment(report.templates, room.templateId);
                    for (GeneratedRoomEncounters.Spawn spawn : encounters.roomSpawns(room.id)) {
                        enemies++;
                        increment(report.enemyTypes, spawn.type);
                    }
                }
                report.minRooms = Math.min(report.minRooms, graph.rooms.size());
                report.maxRooms = Math.max(report.maxRooms, graph.rooms.size());
                roomTotal += graph.rooms.size();
                routeTotal += graph.mainRoute.size();
                enemyTotal += enemies;
            } catch (RuntimeException | AssertionError failure) {
                throw new AssertionError("Generation quality failure: seed=" + seed + ", " + failure.getMessage(), failure);
            }
        }
        report.averageRooms = roomTotal / (double) count;
        report.averageMainRouteRooms = routeTotal / (double) count;
        report.averageEnemies = enemyTotal / (double) count;
        Json json = new Json(JsonWriter.OutputType.json);
        json.setTypeName(null);
        reportFile.parent().mkdirs();
        reportFile.writeString(json.prettyPrint(report), false, "UTF-8");
        System.out.println("GENERATION QUALITY PASSED: " + count + " seeds from " + firstSeed +
            "; rooms " + report.minRooms + ".." + report.maxRooms +
            ", average=" + report.averageRooms + ", enemies=" + report.averageEnemies +
            "; report=" + reportFile.path());
    }

    private static void verifyNoOverlap(PlacedRoomGraph layout, long seed) {
        for (int i = 0; i < layout.rooms.size(); i++) for (int j = i + 1; j < layout.rooms.size(); j++) {
            PlacedRoomGraph.PlacedRoom a = layout.rooms.get(i);
            PlacedRoomGraph.PlacedRoom b = layout.rooms.get(j);
            boolean overlap = a.worldX < b.worldX + b.width && a.worldX + a.width > b.worldX &&
                a.worldY < b.worldY + b.height && a.worldY + a.height > b.worldY;
            if (overlap) throw new AssertionError("rule=collision/layout, rooms " + a.id + " and " + b.id + " overlap at seed " + seed);
        }
    }

    private static void verifyTransition(PlacedRoomGraph layout, GenerationProfile profile, FileHandle assets, long seed) {
        if (!assets.child("levels").child(profile.returnLocation).child(profile.returnRoomId + ".json").exists())
            throw new AssertionError("rule=transition, missing return room at seed " + seed);
        PlacedRoomGraph.PlacedRoom entry = layout.rooms.get(0);
        for (PlacedRoomGraph.PlacedRoom room : layout.rooms)
            if (room.worldX < entry.worldX || room.worldX == entry.worldX && room.id < entry.id) entry = room;
        for (PlacedRoomGraph.PlacedRoom room : layout.rooms)
            if (room.worldX < entry.worldX) throw new AssertionError("rule=transition, entry is not on left boundary at seed " + seed);
    }

    private static void increment(Map<String, Integer> counts, String key) {
        counts.put(key, counts.getOrDefault(key, 0) + 1);
    }

    private static <T> T rule(long seed, String name, CheckedSupplier<T> action) {
        try { return action.get(); }
        catch (RuntimeException | AssertionError failure) {
            throw new AssertionError("rule=" + name + ", seed=" + seed + ": " + failure.getMessage(), failure);
        }
    }

    private interface CheckedSupplier<T> { T get(); }
}
