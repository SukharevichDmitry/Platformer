package com.cursedarchie.platformer.levels;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Aligns authored door openings and rejects overlapping room rectangles. */
public final class RoomLayoutPlacer {
    private static final int SIZE_ATTEMPTS = 8;

    private static final class PlacementConflictException extends IllegalArgumentException {
        PlacementConflictException(String message) {
            super(message);
        }
    }

    private static final class RoomShape {
        final RoomTemplateCatalog.RoomTemplate template;
        final int width;
        final int height;
        final List<String> rows;

        RoomShape(RoomTemplateCatalog.RoomTemplate template, int width, int height, List<String> rows) {
            this.template = template;
            this.width = width;
            this.height = height;
            this.rows = rows;
        }
    }

    public PlacedRoomGraph place(GeneratedRoomGraph graph, RoomTemplateCatalog catalog,
                                 GenerationProfile profile) {
        if (graph == null || catalog == null || profile == null ||
            !graph.locationId.equals(catalog.locationId) || !graph.locationId.equals(profile.locationId))
            throw new IllegalArgumentException("Matching room graph, template catalog, and profile are required");
        if (graph.rooms.isEmpty()) throw new IllegalArgumentException("Room graph is empty");

        PlacementConflictException lastConflict = null;
        for (int attempt = 0; attempt <= SIZE_ATTEMPTS; attempt++) {
            RoomShape[] shapes = new RoomShape[graph.rooms.size()];
            Random sizes = new Random(graph.seed ^ (0x5DEECE66DL + attempt * 0x9E3779B97F4A7C15L));
            for (int i = 0; i < shapes.length; i++)
                shapes[i] = selectShape(template(catalog, graph.room(i)), profile, sizes,
                    attempt == SIZE_ATTEMPTS);
            try {
                return placeShapes(graph, shapes);
            } catch (PlacementConflictException conflict) {
                lastConflict = conflict;
            }
        }
        throw lastConflict;
    }

    private static PlacedRoomGraph placeShapes(GeneratedRoomGraph graph, RoomShape[] shapes) {
        PlacedRoomGraph.PlacedRoom[] placed = new PlacedRoomGraph.PlacedRoom[graph.rooms.size()];
        List<PlacedRoomGraph.DoorJoin> joins = new ArrayList<>();
        ArrayDeque<Integer> pending = new ArrayDeque<>();
        RoomShape start = shapes[0];
        placed[0] = new PlacedRoomGraph.PlacedRoom(0, start.template, start.width, start.height,
            start.rows, graph.room(0).neighbors, 0, 0);
        pending.add(0);

        while (!pending.isEmpty()) {
            int roomId = pending.removeFirst();
            GeneratedRoomGraph.RoomNode node = graph.room(roomId);
            PlacedRoomGraph.PlacedRoom current = placed[roomId];
            RoomShape currentShape = shapes[roomId];
            RoomTemplateCatalog.RoomTemplate currentTemplate = currentShape.template;
            for (Map.Entry<String, Integer> link : node.neighbors.entrySet()) {
                String side = link.getKey();
                int neighborId = link.getValue();
                if (neighborId < 0 || neighborId >= placed.length || neighborId == roomId)
                    throw invalid(graph, "room " + roomId + " has invalid neighbor " + neighborId);
                GeneratedRoomGraph.RoomNode neighborNode = graph.room(neighborId);
                RoomShape neighborShape = shapes[neighborId];
                RoomTemplateCatalog.RoomTemplate neighborTemplate = neighborShape.template;
                String opposite = opposite(side);
                if (!Integer.valueOf(roomId).equals(neighborNode.neighbors.get(opposite)))
                    throw invalid(graph, "rooms " + roomId + " and " + neighborId + " lack reciprocal links");
                RoomTemplateCatalog.Door door = currentTemplate.door(side);
                RoomTemplateCatalog.Door otherDoor = neighborTemplate.door(opposite);
                if (door == null || otherDoor == null || door.span != otherDoor.span)
                    throw invalid(graph, "rooms " + roomId + " and " + neighborId + " have mismatched doors");
                if (side.equals("left") || side.equals("right"))
                    checkWalkingFloor(graph, roomId, neighborId, currentShape, neighborShape, door, otherDoor);

                int x;
                int y;
                switch (side) {
                    case "right":
                        x = current.worldX + current.width;
                        y = current.worldY + door.offset - otherDoor.offset;
                        break;
                    case "left":
                        x = current.worldX - neighborShape.width;
                        y = current.worldY + door.offset - otherDoor.offset;
                        break;
                    case "top":
                        x = current.worldX + door.offset - otherDoor.offset;
                        y = current.worldY + current.height;
                        break;
                    case "bottom":
                        x = current.worldX + door.offset - otherDoor.offset;
                        y = current.worldY - neighborShape.height;
                        break;
                    default: throw invalid(graph, "room " + roomId + " has unknown side " + side);
                }

                if (placed[neighborId] == null) {
                    PlacedRoomGraph.PlacedRoom next = new PlacedRoomGraph.PlacedRoom(
                        neighborId, neighborTemplate, neighborShape.width, neighborShape.height,
                        neighborShape.rows, neighborNode.neighbors, x, y);
                    for (PlacedRoomGraph.PlacedRoom existing : placed) {
                        if (existing != null && overlaps(next, existing))
                            throw new PlacementConflictException("Seed " + graph.seed + ": rooms " +
                                neighborId + " and " + existing.id + " overlap");
                    }
                    placed[neighborId] = next;
                    pending.add(neighborId);
                } else if (placed[neighborId].worldX != x || placed[neighborId].worldY != y) {
                    throw invalid(graph, "room " + neighborId + " has inconsistent placement");
                }
                if (roomId < neighborId) {
                    int openingX = side.equals("right") ? current.worldX + current.width :
                        side.equals("left") ? current.worldX : current.worldX + door.offset;
                    int openingY = side.equals("top") ? current.worldY + current.height :
                        side.equals("bottom") ? current.worldY : current.worldY + door.offset;
                    joins.add(new PlacedRoomGraph.DoorJoin(roomId, neighborId, side,
                        openingX, openingY, door.span));
                }
            }
        }
        List<PlacedRoomGraph.PlacedRoom> result = new ArrayList<>();
        for (int i = 0; i < placed.length; i++) {
            if (placed[i] == null) throw invalid(graph, "room " + i + " is disconnected");
            result.add(placed[i]);
        }
        return new PlacedRoomGraph(graph, result, joins);
    }

    private static RoomTemplateCatalog.RoomTemplate template(RoomTemplateCatalog catalog,
                                                              GeneratedRoomGraph.RoomNode node) {
        RoomTemplateCatalog.RoomTemplate template = catalog.templates.get(node.templateId);
        if (template == null) throw new IllegalArgumentException("Missing template " + node.templateId);
        return template;
    }

    private static RoomShape selectShape(RoomTemplateCatalog.RoomTemplate template,
                                         GenerationProfile profile, Random random, boolean useBaseSize) {
        GenerationProfile.RoomPattern pattern = null;
        for (GenerationProfile.RoomPattern candidate : profile.roomPatterns)
            if (candidate.id.equals(template.patternId)) pattern = candidate;
        if (pattern == null) throw new IllegalArgumentException("Missing pattern " + template.patternId);
        int width = template.width;
        int height = template.height;
        if (template.role.equals("optional") && !useBaseSize) {
            width += random.nextInt(pattern.width.max - width + 1);
            height += random.nextInt(pattern.height.max - height + 1);
        }
        List<String> rows = new ArrayList<>(template.tileRows);
        int extraWidth = width - template.width;
        if (extraWidth > 0) {
            int insertAt = template.width - 2;
            for (int i = 0; i < rows.size(); i++) {
                String row = rows.get(i);
                StringBuilder expanded = new StringBuilder(width);
                expanded.append(row, 0, insertAt);
                for (int extra = 0; extra < extraWidth; extra++) expanded.append(row.charAt(insertAt));
                expanded.append(row.substring(insertAt));
                rows.set(i, expanded.toString());
            }
        }
        String upperInterior = rows.get(1);
        for (int extra = template.height; extra < height; extra++) rows.add(1, upperInterior);
        return new RoomShape(template, width, height, rows);
    }

    private static void checkWalkingFloor(GeneratedRoomGraph graph, int aId, int bId,
                                          RoomShape a, RoomShape b,
                                          RoomTemplateCatalog.Door aDoor,
                                          RoomTemplateCatalog.Door bDoor) {
        if (aDoor.offset != 1 || bDoor.offset != 1)
            throw invalid(graph, "rooms " + aId + " and " + bId + " have different walking floor height");
        int aX = aDoor.side.equals("left") ? 0 : a.width - 1;
        int bX = bDoor.side.equals("left") ? 0 : b.width - 1;
        int aInside = aDoor.side.equals("left") ? 1 : a.width - 2;
        int bInside = bDoor.side.equals("left") ? 1 : b.width - 2;
        if (a.rows.get(a.height - 1).charAt(aX) != '#' ||
            b.rows.get(b.height - 1).charAt(bX) != '#' ||
            a.rows.get(a.height - 1).charAt(aInside) != '#' ||
            b.rows.get(b.height - 1).charAt(bInside) != '#')
            throw invalid(graph, "rooms " + aId + " and " + bId + " lack floor blocks at the seam");
    }

    private static boolean overlaps(PlacedRoomGraph.PlacedRoom a, PlacedRoomGraph.PlacedRoom b) {
        return a.worldX < b.worldX + b.width && a.worldX + a.width > b.worldX &&
            a.worldY < b.worldY + b.height && a.worldY + a.height > b.worldY;
    }

    private static String opposite(String side) {
        switch (side) {
            case "left": return "right";
            case "right": return "left";
            case "top": return "bottom";
            case "bottom": return "top";
            default: throw new IllegalArgumentException("Unknown side " + side);
        }
    }

    private static IllegalArgumentException invalid(GeneratedRoomGraph graph, String message) {
        return new IllegalArgumentException("Seed " + graph.seed + ": " + message);
    }
}
