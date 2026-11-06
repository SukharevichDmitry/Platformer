package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.levels.wrappers.EnemySpawnData;
import com.cursedarchie.platformer.levels.wrappers.LocationTransition;
import com.cursedarchie.platformer.levels.wrappers.Neighbors;
import com.cursedarchie.platformer.levels.wrappers.RoomData;
import com.cursedarchie.platformer.levels.wrappers.InteractionData;
import com.cursedarchie.platformer.levels.wrappers.SpawnPointData;
import com.cursedarchie.platformer.tiles.Tile;

import java.util.ArrayList;
import java.util.List;

/** Converts validated procedural data to the same room model as authored locations. */
final class GeneratedLocationFactory {
    LocationInstance load(FileHandle profileFile, FileHandle templateFile, long requestedSeed,
                          GeneratedRunSave restored) {
        GenerationProfile profile = GenerationProfile.load(profileFile);
        RoomTemplateCatalog catalog = RoomTemplateCatalog.load(templateFile, profile);
        FileHandle returnRoom = profileFile.parent().parent().child("levels")
            .child(profile.returnLocation).child(profile.returnRoomId + ".json");
        if (!returnRoom.exists())
            throw new IllegalArgumentException(profileFile.path() + ": returnTransition references missing " +
                profile.returnLocation + "/" + profile.returnRoomId);
        if (profile.forwardLocation != null) {
            FileHandle forwardRoom = profileFile.parent().parent().child("levels")
                .child(profile.forwardLocation).child(profile.forwardRoomId + ".json");
            if (!forwardRoom.exists())
                throw new IllegalArgumentException(profileFile.path() + ": forwardTransition references missing " +
                    profile.forwardLocation + "/" + profile.forwardRoomId);
        }
        long seed = restored == null ? requestedSeed : restored.seed;
        try {
            return generate(profile, catalog, seed, restored);
        } catch (IllegalArgumentException primary) {
            if (restored != null || seed == 0) throw primary;
            try {
                return generate(profile, catalog, 0L, null);
            } catch (IllegalArgumentException fallback) {
                throw new IllegalArgumentException("Generation failed for seed " + seed +
                    " and validated fallback seed 0: " + fallback.getMessage(), fallback);
            }
        }
    }

    private LocationInstance generate(GenerationProfile profile, RoomTemplateCatalog catalog, long seed,
                                      GeneratedRunSave restored) {
        GeneratedRoomGraph graph = new RoomGraphGenerator().generate(profile, catalog, seed);
        PlacedRoomGraph layout = new RoomLayoutPlacer().place(graph, catalog, profile);
        GeneratedRoomTiles tiles = new RoomTileGenerator().generate(layout, catalog);
        int entryRoomId = entryRoomId(layout);
        tiles = openExteriorEntry(tiles, entryRoomId);
        int exitRoomId = graph.mainRoute.get(graph.mainRoute.size() - 1);
        if (profile.forwardLocation != null) tiles = openExteriorExit(tiles, exitRoomId);
        RoomRouteReachability.validate(tiles);
        GeneratedRoomEncounters encounters = new RoomEncounterGenerator().generate(tiles, catalog, profile);
        int alleyRouteIndex = Math.min(2, graph.mainRoute.size() - 2);
        boolean shooterBeforeAlley = false;
        for (int i = 1; i < graph.mainRoute.size() - 1; i++) {
            boolean metScavenger = false;
            for (GeneratedRoomEncounters.Spawn spawn : encounters.roomSpawns(graph.mainRoute.get(i - 1)))
                if ("scavenger".equals(spawn.type)) metScavenger = true;
            if (metScavenger) { alleyRouteIndex = i; shooterBeforeAlley = true; break; }
        }
        int alleyRoomId = graph.mainRoute.get(alleyRouteIndex);
        int forcedShooterRoomId = shooterBeforeAlley ? -1 : graph.mainRoute.get(alleyRouteIndex - 1);
        GeneratedRunSave run = describeRun(profile, catalog, graph, layout, restored);

        LocationInstance location = new LocationInstance(profile.locationId);
        for (PlacedRoomGraph.PlacedRoom placed : layout.rooms) {
            GeneratedRoomGraph.RoomNode node = graph.room(placed.id);
            RoomData data = new RoomData();
            data.location = profile.locationId;
            data.id = placed.id;
            data.sceneryTheme = sceneryTheme(placed.templateId, placed.id);
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
            if (placed.id == forcedShooterRoomId) {
                Vector2 position = findShooterPosition(tiles, placed.id, placed.width, placed.height);
                EnemySpawnData shooter = new EnemySpawnData();
                shooter.type = "scavenger";
                shooter.position = position;
                data.enemySpawns.add(shooter);
            }
            if (placed.id == entryRoomId) {
                LocationTransition transition = new LocationTransition();
                transition.location = profile.returnLocation;
                transition.id = profile.returnRoomId;
                data.locationEntry = transition;
            }
            if (profile.forwardLocation != null && placed.id == exitRoomId) {
                LocationTransition transition = new LocationTransition();
                transition.location = profile.forwardLocation;
                transition.id = profile.forwardRoomId;
                data.locationExit = transition;
            }
            RoomInstance room = new RoomInstance(data);
            room.buildTiles();
            RoomFineDecorator.decorate(room, catalog.templates.get(placed.templateId));
            if (placed.id == alleyRoomId) addAlleyPortal(room);
            location.addRoom(room);
        }
        for (PlacedRoomGraph.DoorJoin join : layout.doorJoins) {
            if (!join.fromSide.equals("top") && !join.fromSide.equals("bottom")) continue;
            PlacedRoomGraph.PlacedRoom from = layout.room(join.fromRoomId);
            PlacedRoomGraph.PlacedRoom to = layout.room(join.toRoomId);
            location.addVerticalConnection(new VerticalConnection(join,
                join.fromSide.equals("top") ? from : to,
                join.fromSide.equals("top") ? to : from));
        }
        location.setGeneration(run, entryRoomId, exitRoomId);
        return location;
    }

    /** The selected street instance is stable for a generated run and owns its return point. */
    private static void addAlleyPortal(RoomInstance room) {
        Tile[][] tiles = room.getTiles();
        for (int offset = 0; offset < room.getWidth(); offset++) {
            int x = room.getWidth() / 2 + (offset % 2 == 0 ? offset / 2 : -(offset + 1) / 2);
            if (x < 5 || x >= room.getWidth() - 5) continue;
            for (int y = 2; y < Math.min(6, room.getHeight() - 2); y++) {
                if (tiles[x][y - 1] == null || tiles[x][y] != null || tiles[x][y + 1] != null ||
                    tiles[x - 1][y] != null || tiles[x + 1][y] != null) continue;
                InteractionData portal = new InteractionData();
                portal.id = "laundry_alley_entrance";
                portal.kind = "portal";
                portal.visualId = "alley_door";
                portal.eventId = "alley_discovered";
                portal.position = new Vector2(x + .5f, y + .5f);
                portal.targetLocation = "laundry_alley";
                portal.targetRoomId = 0;
                portal.targetSpawnId = "street_entry";
                portal.transition = "alley_enter";
                room.getInteractions().add(portal);
                SpawnPointData returnPoint = new SpawnPointData();
                returnPoint.id = "alley_return";
                returnPoint.position = new Vector2(x + .5f, y + .5f);
                room.getSpawnPoints().add(returnPoint);
                return;
            }
        }
        throw new IllegalArgumentException("Generated entry room has no clear alley portal position");
    }

    private static Vector2 findShooterPosition(GeneratedRoomTiles tiles, int roomId, int width, int height) {
        List<String> rows = tiles.roomRows(roomId);
        for (int x = width / 2; x < width - 5; x++) {
            for (int y = 2; y < Math.min(height - 2, 7); y++) {
                if (rows.get(height - y).charAt(x) != ' ' &&
                    rows.get(height - 1 - y).charAt(x) == ' ' &&
                    rows.get(height - 2 - y).charAt(x) == ' ')
                    return new Vector2(x + .5f, y + .5f);
            }
        }
        throw new IllegalArgumentException("No clear scavenger position before alley");
    }

    private static String sceneryTheme(String templateId, int roomId) {
        switch (templateId) {
            case "red_registry": case "bell_hall": case "guarded_hall": return "red";
            case "story_shrine": case "ink_house": return "ink";
            case "ruined_workshop": return "mechanical";
            case "entry": return "shelter";
            default:
                return new String[]{"ink", "mechanical", "red"}[Math.floorMod(roomId, 3)];
        }
    }

    private GeneratedRunSave describeRun(GenerationProfile profile, RoomTemplateCatalog catalog,
                                         GeneratedRoomGraph graph, PlacedRoomGraph layout,
                                         GeneratedRunSave restored) {
        GeneratedRunSave run = restored == null ? new GeneratedRunSave() : restored;
        Array<String> narratives = new Array<>();
        for (GeneratedRoomGraph.RoomNode node : graph.rooms) {
            RoomTemplateCatalog.RoomTemplate template = catalog.templates.get(node.templateId);
            if (template.narrativeEventId != null)
                narratives.add(node.id + ":" + template.id + ":" + template.narrativeEventId);
        }
        String topology = graph.topologySignature();
        String placement = layout.placementSignature();
        if (restored != null && (!topology.equals(restored.topologySignature) ||
            !placement.equals(restored.placementSignature) || !narratives.equals(restored.narrativeChoices)))
            throw new IllegalArgumentException("Saved generated run does not match profile version " + profile.version);
        run.profileVersion = profile.version;
        run.locationId = profile.locationId;
        run.seed = graph.seed;
        run.topologySignature = topology;
        run.placementSignature = placement;
        run.narrativeChoices = narratives;
        if (run.defeatedSpawnIds == null) run.defeatedSpawnIds = new Array<>();
        if (run.brokenTileIds == null) run.brokenTileIds = new Array<>();
        return run;
    }

    static int entryRoomId(PlacedRoomGraph layout) {
        int entryRoomId = 0;
        for (PlacedRoomGraph.PlacedRoom room : layout.rooms) {
            PlacedRoomGraph.PlacedRoom current = layout.room(entryRoomId);
            if (room.worldX < current.worldX ||
                (room.worldX == current.worldX && room.id < current.id)) entryRoomId = room.id;
        }
        return entryRoomId;
    }

    static GeneratedRoomTiles openExteriorEntry(GeneratedRoomTiles tiles, int entryRoomId) {
        List<List<String>> rooms = new ArrayList<>(tiles.rooms);
        PlacedRoomGraph.PlacedRoom room = tiles.layout.room(entryRoomId);
        List<String> rows = new ArrayList<>(tiles.roomRows(entryRoomId));
        for (int y = 2; y <= 5; y++) {
            int index = room.height - 1 - y;
            char[] row = rows.get(index).toCharArray();
            row[0] = ' ';
            rows.set(index, new String(row));
        }
        rooms.set(entryRoomId, rows);
        return new GeneratedRoomTiles(tiles.layout, rooms);
    }

    static GeneratedRoomTiles openExteriorExit(GeneratedRoomTiles tiles, int exitRoomId) {
        List<List<String>> rooms = new ArrayList<>(tiles.rooms);
        PlacedRoomGraph.PlacedRoom room = tiles.layout.room(exitRoomId);
        List<String> rows = new ArrayList<>(tiles.roomRows(exitRoomId));
        for (int y = 2; y <= 5; y++) {
            int index = room.height - 1 - y;
            char[] row = rows.get(index).toCharArray();
            row[room.width - 1] = ' ';
            rows.set(index, new String(row));
        }
        rooms.set(exitRoomId, rows);
        return new GeneratedRoomTiles(tiles.layout, rooms);
    }
}
