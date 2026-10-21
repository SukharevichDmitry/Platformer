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
        public double combatRoomPercent;
        public double optionalBranchPercent;
        public long shortestRouteSeed = -1;
        public long longestRouteSeed = -1;
        public long optionalBranchSeed = -1;
        public long noBranchSeed = -1;
        public int maxConsecutiveSafeRooms;
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
        long combatRoomTotal = 0;
        long optionalBranchTotal = 0;
        int requiredBranchCount = 0;
        for (RoomTemplateCatalog.BranchSlot slot : catalog.optionalBranches)
            if (slot.required) requiredBranchCount++;

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
                verifyTransition(tiles, profile, new FileHandle(args[0]), currentSeed);

                int enemies = 0;
                // Optional-branch tuning excludes guaranteed vertical progression rooms.
                int branches = graph.rooms.size() - graph.mainRoute.size() - requiredBranchCount;
                optionalBranchTotal += branches;
                if (graph.rooms.size() < report.minRooms) report.shortestRouteSeed = currentSeed;
                if (graph.rooms.size() > report.maxRooms) report.longestRouteSeed = currentSeed;
                if (branches > 0 && report.optionalBranchSeed < 0) report.optionalBranchSeed = currentSeed;
                if (branches == 0 && report.noBranchSeed < 0) report.noBranchSeed = currentSeed;
                report.maxConsecutiveSafeRooms = Math.max(report.maxConsecutiveSafeRooms,
                    consecutiveSafeRooms(graph, catalog));
                for (PlacedRoomGraph.PlacedRoom room : layout.rooms) {
                    increment(report.templates, room.templateId);
                    String encounterRole = catalog.templates.get(room.templateId).encounterRole;
                    if (encounterRole.equals("combat") || encounterRole.equals("boss")) combatRoomTotal++;
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
        report.combatRoomPercent = combatRoomTotal * 100.0 / roomTotal;
        report.optionalBranchPercent = optionalBranchTotal * 100.0 / count;
        if (profile.status.equals("production") && count >= 1000) verifyTuningTargets(profile, report);
        Json json = new Json(JsonWriter.OutputType.json);
        json.setTypeName(null);
        reportFile.parent().mkdirs();
        reportFile.writeString(json.prettyPrint(report), false, "UTF-8");
        System.out.println("GENERATION QUALITY PASSED: " + count + " seeds from " + firstSeed +
            "; rooms " + report.minRooms + ".." + report.maxRooms +
            ", average=" + report.averageRooms + ", enemies=" + report.averageEnemies +
            ", combat=" + report.combatRoomPercent + "%" +
            ", branches=" + report.optionalBranchPercent + "%" +
            "; report=" + reportFile.path());
    }

    private static void verifyTuningTargets(GenerationProfile profile, Report report) {
        GenerationProfile.TuningTargets targets = profile.tuningTargets;
        target("averageRooms", report.averageRooms, targets.averageRooms);
        target("averageEnemies", report.averageEnemies, targets.averageEnemies);
        target("combatRoomPercent", report.combatRoomPercent, targets.combatRoomPercent);
        target("optionalBranchPercent", report.optionalBranchPercent, targets.optionalBranchPercent);
        if (report.maxConsecutiveSafeRooms > targets.maxConsecutiveSafeRooms)
            throw new AssertionError("Production tuning target maxConsecutiveSafeRooms expected at most " +
                targets.maxConsecutiveSafeRooms + " but was " + report.maxConsecutiveSafeRooms);
    }

    private static void target(String name, double actual, GenerationProfile.IntRange expected) {
        if (actual < expected.min || actual > expected.max)
            throw new AssertionError("Production tuning target " + name + " expected " +
                expected.min + ".." + expected.max + " but was " + actual);
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

    private static void verifyTransition(GeneratedRoomTiles tiles, GenerationProfile profile,
                                         FileHandle assets, long seed) {
        if (!assets.child("levels").child(profile.returnLocation).child(profile.returnRoomId + ".json").exists())
            throw new AssertionError("rule=transition, missing return room at seed " + seed);
        int entryRoomId = GeneratedLocationFactory.entryRoomId(tiles.layout);
        GeneratedRoomTiles opened = GeneratedLocationFactory.openExteriorEntry(tiles, entryRoomId);
        rule(seed, "transition", () -> { RoomRouteReachability.validate(opened); return opened; });
        java.util.List<String> rows = opened.roomRows(entryRoomId);
        int height = tiles.layout.room(entryRoomId).height;
        if (rows.get(height - 2).charAt(0) != ' ' || rows.get(height - 3).charAt(0) != ' ' ||
            rows.get(height - 1).charAt(0) != '#')
            throw new AssertionError("rule=transition, exterior entry is not walkable at seed " + seed);
    }

    private static int consecutiveSafeRooms(GeneratedRoomGraph graph, RoomTemplateCatalog catalog) {
        int current = 0;
        int maximum = 0;
        for (int roomId : graph.mainRoute) {
            String role = catalog.templates.get(graph.room(roomId).templateId).encounterRole;
            current = role.equals("safe") ? current + 1 : 0;
            maximum = Math.max(maximum, current);
        }
        return maximum;
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
