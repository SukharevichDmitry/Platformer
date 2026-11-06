package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.files.FileHandle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Checks deterministic encounter budgets, palettes, and terrain clearance. */
public final class RoomEncounterGenerationCheck {
    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("Expected assets directory");
        FileHandle generation = new FileHandle(new File(args[0], "generation"));
        GenerationProfile profile = GenerationProfile.load(generation.child("ruins.json"));
        RoomTemplateCatalog catalog = RoomTemplateCatalog.load(generation.child("ruins.templates.json"), profile);
        RoomGraphGenerator graphs = new RoomGraphGenerator();
        RoomLayoutPlacer placer = new RoomLayoutPlacer();
        RoomTileGenerator tileGenerator = new RoomTileGenerator();
        RoomEncounterGenerator encounterGenerator = new RoomEncounterGenerator();
        String first = null;
        boolean varied = false;
        int combatSpawns = 0;
        for (long seed = 0; seed < 1000; seed++) {
            GeneratedRoomTiles tiles = tileGenerator.generate(
                placer.place(graphs.generate(profile, catalog, seed), catalog, profile), catalog);
            GeneratedRoomEncounters encounters = encounterGenerator.generate(tiles, catalog, profile);
            RoomEncounterGenerator.validate(encounters, catalog, profile);
            if (!encounters.encounterSignature().equals(
                encounterGenerator.generate(tiles, catalog, profile).encounterSignature()))
                throw new AssertionError("Seed " + seed + " changed encounters");
            if (first == null) first = encounters.encounterSignature();
            else if (!first.equals(encounters.encounterSignature())) varied = true;
            for (PlacedRoomGraph.PlacedRoom room : tiles.layout.rooms) {
                RoomTemplateCatalog.RoomTemplate template = catalog.templates.get(room.templateId);
                if (template.encounterRole.equals("safe") && !encounters.roomSpawns(room.id).isEmpty())
                    throw new AssertionError("Seed " + seed + " populated safe room " + room.id);
                if (template.encounterRole.equals("combat"))
                    combatSpawns += encounters.roomSpawns(room.id).size();
            }
            if (seed == 0) {
                List<List<GeneratedRoomEncounters.Spawn>> altered = new ArrayList<>(encounters.rooms);
                int bossRoom = -1;
                for (PlacedRoomGraph.PlacedRoom room : tiles.layout.rooms)
                    if (catalog.templates.get(room.templateId).encounterRole.equals("boss")) bossRoom = room.id;
                if (bossRoom < 0) throw new AssertionError("Missing boss room");
                List<GeneratedRoomEncounters.Spawn> invalid = new ArrayList<>(altered.get(bossRoom));
                invalid.set(0, new GeneratedRoomEncounters.Spawn("enemy2", 3.5f, 2f));
                altered.set(bossRoom, invalid);
                expectError(new GeneratedRoomEncounters(tiles, altered), catalog, profile, "palette");
                invalid.set(0, new GeneratedRoomEncounters.Spawn("boss", 0.5f, 1.8f));
                expectError(new GeneratedRoomEncounters(tiles, altered), catalog, profile, "clearance");
            }
        }
        if (!varied || combatSpawns == 0)
            throw new AssertionError("Encounters did not vary or combat rooms were empty");
        System.out.println("ROOM ENCOUNTERS PASSED: 1000 deterministic seeds, budgets, palettes, terrain and safe-space checks");
    }

    private static void expectError(GeneratedRoomEncounters encounters, RoomTemplateCatalog catalog,
                                    GenerationProfile profile, String detail) {
        try {
            RoomEncounterGenerator.validate(encounters, catalog, profile);
            throw new AssertionError("Expected encounter error: " + detail);
        } catch (IllegalArgumentException expected) {
            if (!expected.getMessage().contains(detail)) throw expected;
        }
    }
}
