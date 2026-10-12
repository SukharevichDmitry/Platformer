package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.files.FileHandle;

import java.io.File;

/** Checks placed room bounds, joined openings, and closed unused doors. */
public final class RoomLayoutPlacementCheck {
    public static void main(String[] args) {
        if (args.length != 2) throw new IllegalArgumentException("Expected assets and fixture directories");
        FileHandle generation = new FileHandle(new File(args[0], "generation"));
        GenerationProfile profile = GenerationProfile.load(generation.child("ruins.json"));
        FileHandle catalogFile = generation.child("ruins.templates.json");
        RoomTemplateCatalog catalog = RoomTemplateCatalog.load(catalogFile, profile);
        RoomGraphGenerator generator = new RoomGraphGenerator();
        RoomLayoutPlacer placer = new RoomLayoutPlacer();

        boolean sawClosedBranch = false;
        boolean sawOpenBranch = false;
        boolean sawSizeVariation = false;
        for (long seed = 0; seed < 1000; seed++) {
            GeneratedRoomGraph graph = generator.generate(profile, catalog, seed);
            PlacedRoomGraph placed = placer.place(graph, catalog, profile);
            PlacedRoomGraph repeated = placer.place(generator.generate(profile, catalog, seed), catalog, profile);
            if (placed.seed != seed || !placed.placementSignature().equals(repeated.placementSignature()))
                throw new AssertionError("Seed " + seed + " placement was not deterministic");
            checkPlacement(graph, placed, catalog, profile);
            if (graph.rooms.size() == graph.mainRoute.size()) sawClosedBranch = true;
            else sawOpenBranch = true;
            for (PlacedRoomGraph.PlacedRoom room : placed.rooms) {
                RoomTemplateCatalog.RoomTemplate template = catalog.templates.get(room.templateId);
                if (room.width > template.width || room.height > template.height) sawSizeVariation = true;
            }
        }
        if (!sawClosedBranch || !sawOpenBranch || !sawSizeVariation)
            throw new AssertionError("Seeds never exercised door choices and allowed size variation");

        FileHandle fixture = new FileHandle(new File(args[1], "generation/mismatched-doors.json"));
        fixture.parent().mkdirs();
        String source = catalogFile.readString();
        int cacheStart = source.indexOf("\"id\": \"cache\"");
        int ambushStart = source.indexOf("\"id\": \"ambush\"");
        String cache = source.substring(cacheStart, ambushStart);
        int opening = -1;
        for (int i = 0; i < 3; i++) opening = cache.indexOf("\"#       #\"", opening + 1);
        cache = cache.substring(0, opening) + "\"#        \"" +
            cache.substring(opening + "\"#       #\"".length());
        cache = cache.replace("\"side\": \"right\", \"offset\": 1, \"span\": 2",
            "\"side\": \"right\", \"offset\": 1, \"span\": 3");
        String invalid = source.substring(0, cacheStart) + cache + source.substring(ambushStart);
        fixture.writeString(invalid, false);
        RoomTemplateCatalog mismatched = RoomTemplateCatalog.load(fixture, profile);
        boolean rejected = false;
        for (long seed = 0; seed < 1000 && !rejected; seed++) {
            GeneratedRoomGraph graph = generator.generate(profile, mismatched, seed);
            boolean hasCache = false;
            for (GeneratedRoomGraph.RoomNode node : graph.rooms)
                if (node.templateId.equals("cache")) hasCache = true;
            if (!hasCache) continue;
            try {
                placer.place(graph, mismatched, profile);
                throw new AssertionError("Mismatched branch doorway was accepted");
            } catch (IllegalArgumentException expected) {
                if (!expected.getMessage().contains("mismatched doors"))
                    throw new AssertionError("Unclear placement error: " + expected.getMessage());
                rejected = true;
            }
        }
        if (!rejected) throw new AssertionError("No seed exercised the mismatched branch doorway");
        System.out.println("ROOM LAYOUT PASSED: 1000 seeds, aligned doors, continuous floors, no overlap or exposed sockets");
    }

    private static void checkPlacement(GeneratedRoomGraph graph, PlacedRoomGraph placed,
                                       RoomTemplateCatalog catalog, GenerationProfile profile) {
        if (placed.rooms.size() != graph.rooms.size() || placed.room(0).worldX != 0 ||
            placed.room(0).worldY != 0 || placed.minX > 0 || placed.minY > 0)
            throw new AssertionError("Placement lost rooms or origin");
        int links = 0;
        for (int i = 0; i < graph.rooms.size(); i++) {
            PlacedRoomGraph.PlacedRoom room = placed.room(i);
            GeneratedRoomGraph.RoomNode node = graph.room(i);
            RoomTemplateCatalog.RoomTemplate template = catalog.templates.get(room.templateId);
            GenerationProfile.RoomPattern pattern = null;
            for (GenerationProfile.RoomPattern candidate : profile.roomPatterns)
                if (candidate.id.equals(template.patternId)) pattern = candidate;
            if (pattern == null || room.id != i ||
                room.width < template.width || room.width > pattern.width.max ||
                room.height < template.height || room.height > pattern.height.max ||
                (!template.role.equals("optional") &&
                    (room.width != template.width || room.height != template.height)) ||
                room.tileRows.size() != room.height ||
                room.worldX < placed.minX || room.worldY < placed.minY ||
                room.worldX + room.width > placed.maxX || room.worldY + room.height > placed.maxY)
                throw new AssertionError("Room dimensions or bounds changed");
            for (String row : room.tileRows)
                if (row.length() != room.width) throw new AssertionError("Resized tile row has wrong width");
            for (int j = 0; j < i; j++) {
                PlacedRoomGraph.PlacedRoom other = placed.room(j);
                if (room.worldX < other.worldX + other.width && room.worldX + room.width > other.worldX &&
                    room.worldY < other.worldY + other.height && room.worldY + room.height > other.worldY)
                    throw new AssertionError("Rooms " + i + " and " + j + " overlap");
            }
            links += node.neighbors.size();
            for (RoomTemplateCatalog.Door door : template.doors) {
                char expected = node.neighbors.containsKey(door.side) ? ' ' : '#';
                for (int offset = door.offset; offset < door.offset + door.span; offset++) {
                    int x = door.side.equals("left") ? 0 : door.side.equals("right") ? room.width - 1 : offset;
                    int y = door.side.equals("bottom") ? 0 : door.side.equals("top") ? room.height - 1 : offset;
                    if (room.tileRows.get(room.height - 1 - y).charAt(x) != expected)
                        throw new AssertionError("Door was not joined or sealed in room " + i);
                }
            }
        }
        if (placed.doorJoins.size() != links / 2)
            throw new AssertionError("A graph edge is missing its doorway join");
        for (PlacedRoomGraph.DoorJoin join : placed.doorJoins) {
            PlacedRoomGraph.PlacedRoom a = placed.room(join.fromRoomId);
            PlacedRoomGraph.PlacedRoom b = placed.room(join.toRoomId);
            RoomTemplateCatalog.Door aDoor = catalog.templates.get(a.templateId).door(join.fromSide);
            RoomTemplateCatalog.Door bDoor = catalog.templates.get(b.templateId).door(opposite(join.fromSide));
            if (aDoor == null || bDoor == null || aDoor.span != bDoor.span || join.span != aDoor.span)
                throw new AssertionError("Joined door sizes differ");
            if (join.fromSide.equals("left") || join.fromSide.equals("right")) {
                int seamX = join.fromSide.equals("right") ? a.worldX + a.width : a.worldX;
                int otherX = join.fromSide.equals("right") ? b.worldX : b.worldX + b.width;
                if (join.worldX != seamX || seamX != otherX ||
                    join.worldY != a.worldY + aDoor.offset ||
                    join.worldY != b.worldY + bDoor.offset ||
                    a.worldY != b.worldY || join.worldY != a.worldY + 1)
                    throw new AssertionError("Walking-height doorway or floor seam is misaligned");
            } else {
                int seamY = join.fromSide.equals("top") ? a.worldY + a.height : a.worldY;
                int otherY = join.fromSide.equals("top") ? b.worldY : b.worldY + b.height;
                if (join.worldY != seamY || seamY != otherY ||
                    join.worldX != a.worldX + aDoor.offset ||
                    join.worldX != b.worldX + bDoor.offset)
                    throw new AssertionError("Vertical doorway is misaligned");
            }
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
