package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.levels.wrappers.EnemySpawnData;
import com.cursedarchie.platformer.levels.wrappers.LocationTransition;
import com.cursedarchie.platformer.levels.wrappers.Neighbors;
import com.cursedarchie.platformer.levels.wrappers.RoomData;

import java.util.ArrayList;
import java.util.List;

/** Converts validated procedural data to the same room model as authored locations. */
final class GeneratedLocationFactory {
    LocationInstance load(FileHandle profileFile, FileHandle templateFile, long requestedSeed) {
        GenerationProfile profile = GenerationProfile.load(profileFile);
        RoomTemplateCatalog catalog = RoomTemplateCatalog.load(templateFile, profile);
        FileHandle returnRoom = profileFile.parent().parent().child("levels")
            .child(profile.returnLocation).child(profile.returnRoomId + ".json");
        if (!returnRoom.exists())
            throw new IllegalArgumentException(profileFile.path() + ": returnTransition references missing " +
                profile.returnLocation + "/" + profile.returnRoomId);
        try {
            return generate(profile, catalog, requestedSeed);
        } catch (IllegalArgumentException primary) {
            if (requestedSeed == 0) throw primary;
            try {
                return generate(profile, catalog, 0L);
            } catch (IllegalArgumentException fallback) {
                throw new IllegalArgumentException("Generation failed for seed " + requestedSeed +
                    " and validated fallback seed 0: " + fallback.getMessage(), fallback);
            }
        }
    }

    private LocationInstance generate(GenerationProfile profile, RoomTemplateCatalog catalog, long seed) {
        GeneratedRoomGraph graph = new RoomGraphGenerator().generate(profile, catalog, seed);
        PlacedRoomGraph layout = new RoomLayoutPlacer().place(graph, catalog, profile);
        GeneratedRoomTiles tiles = new RoomTileGenerator().generate(layout, catalog);
        int entryRoomId = 0;
        for (PlacedRoomGraph.PlacedRoom room : layout.rooms) {
            PlacedRoomGraph.PlacedRoom current = layout.room(entryRoomId);
            if (room.worldX < current.worldX ||
                (room.worldX == current.worldX && room.id < current.id)) entryRoomId = room.id;
        }
        tiles = openExteriorEntry(tiles, entryRoomId);
        RoomRouteReachability.validate(tiles);
        GeneratedRoomEncounters encounters = new RoomEncounterGenerator().generate(tiles, catalog, profile);

        LocationInstance location = new LocationInstance(profile.locationId);
        for (PlacedRoomGraph.PlacedRoom placed : layout.rooms) {
            GeneratedRoomGraph.RoomNode node = graph.room(placed.id);
            RoomData data = new RoomData();
            data.location = profile.locationId;
            data.id = placed.id;
            data.width = placed.width;
            data.height = placed.height;
            data.worldX = placed.worldX;
            data.worldY = placed.worldY;
            data.tileRows = tiles.roomRows(placed.id).toArray(new String[0]);
            data.neighbors = new Neighbors();
            data.neighbors.left = node.neighbors.get("left");
            data.neighbors.right = node.neighbors.get("right");
            data.neighbors.up = node.neighbors.get("top");
            data.neighbors.down = node.neighbors.get("bottom");
            for (GeneratedRoomEncounters.Spawn spawn : encounters.roomSpawns(placed.id)) {
                EnemySpawnData enemy = new EnemySpawnData();
                enemy.type = spawn.type;
                enemy.position = new Vector2(spawn.x, spawn.y);
                data.enemySpawns.add(enemy);
            }
            if (placed.id == entryRoomId) {
                LocationTransition transition = new LocationTransition();
                transition.location = profile.returnLocation;
                transition.id = profile.returnRoomId;
                data.locationEntry = transition;
            }
            RoomInstance room = new RoomInstance(data);
            room.buildTiles();
            location.addRoom(room);
        }
        location.setGeneration(seed, entryRoomId);
        return location;
    }

    private GeneratedRoomTiles openExteriorEntry(GeneratedRoomTiles tiles, int entryRoomId) {
        List<List<String>> rooms = new ArrayList<>(tiles.rooms);
        PlacedRoomGraph.PlacedRoom room = tiles.layout.room(entryRoomId);
        List<String> rows = new ArrayList<>(tiles.roomRows(entryRoomId));
        for (int y = 1; y <= 2; y++) {
            int index = room.height - 1 - y;
            char[] row = rows.get(index).toCharArray();
            row[0] = ' ';
            rows.set(index, new String(row));
        }
        rooms.set(entryRoomId, rows);
        return new GeneratedRoomTiles(tiles.layout, rooms);
    }
}
