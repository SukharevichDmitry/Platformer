package com.cursedarchie.platformer.levels;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Builds a room-sized silhouette from a small set of deliberate architectural motifs. */
public final class RoomTileGenerator {
    public GeneratedRoomTiles generate(PlacedRoomGraph layout, RoomTemplateCatalog catalog) {
        if (layout == null || catalog == null || !layout.locationId.equals(catalog.locationId))
            throw new IllegalArgumentException("Matching layout and template catalog are required");
        List<List<String>> generated = new ArrayList<>();
        for (PlacedRoomGraph.PlacedRoom room : layout.rooms) {
            RoomTemplateCatalog.RoomTemplate template = catalog.templates.get(room.templateId);
            if (template == null) throw new IllegalArgumentException("Unknown template " + room.templateId);
            char[][] tiles = new char[room.height][];
            for (int y = 0; y < room.height; y++) tiles[y] = room.tileRows.get(y).toCharArray();
            // Doors and the walking corridor are fixed. The seed selects a
            // complete motif, rather than independent random blocks.
            if (template.door("bottom") == null) {
                Random random = new Random(layout.seed ^ (room.id * 0x9E3779B97F4A7C15L));
                boolean arena = template.patternId.equals("arena");
                // Story space stays calm and centered; boss space keeps its
                // floor open. Combat and travel rooms use the varied ruins set.
                int motif = template.role.equals("story") ? 3 :
                    template.encounterRole.equals("boss") ? 4 : random.nextInt(3);
                int left = 1 + random.nextInt(2);
                int length = Math.min((arena ? 3 : 2) + random.nextInt(2),
                    (room.width - 2 * left - 1) / 2);
                int right = room.width - left - length;
                int platformRow = room.height - 1 - 3;
                int roofRow = 1;
                // Two opposing ledges frame the center of the room. Wood is
                // traversable from below; cracked stone marks an optional edge.
                if (motif == 0) {
                    place(tiles, platformRow, left, length, '=', template);
                    place(tiles, platformRow, right, length, '_', template);
                    crackEnd(tiles, platformRow, right + length - 1);
                } else if (motif == 1) {
                    place(tiles, platformRow, left, length, '_', template);
                    crackEnd(tiles, platformRow, left);
                    place(tiles, platformRow, right, length, '=', template);
                } else if (motif == 2) {
                    place(tiles, platformRow, left, length, '=', template);
                    place(tiles, platformRow, right, length, '_', template);
                    int upperRow = room.height - 1 - 5;
                    if (upperRow > 2)
                        place(tiles, upperRow, room.width / 2 - 1, 2, '_', template);
                } else {
                    char material = motif == 3 ? '_' : '=';
                    place(tiles, platformRow, left, length, material, template);
                    place(tiles, platformRow, right, length, material, template);
                }
                // Matching ceiling corbels make the platforms read as part of
                // one built chamber. A top doorway reserves the whole shaft.
                int corbel = 1 + random.nextInt(2);
                place(tiles, roofRow, 1, corbel, '#', template);
                place(tiles, roofRow, room.width - 1 - corbel, corbel, '#', template);
                if (arena && motif != 2) {
                    place(tiles, roofRow + 1, 1, 1, '#', template);
                    place(tiles, roofRow + 1, room.width - 2, 1, '#', template);
                }
            }
            List<String> rows = new ArrayList<>();
            for (char[] row : tiles) rows.add(new String(row));
            generated.add(rows);
        }
        GeneratedRoomTiles result = new GeneratedRoomTiles(layout, generated);
        RoomRouteReachability.validate(result);
        return result;
    }

    private static boolean clear(char[][] tiles, int row, int x, int width,
                                 RoomTemplateCatalog.RoomTemplate template) {
        if (row < 0 || row >= tiles.length || x < 1 || x + width >= tiles[row].length) return false;
        RoomTemplateCatalog.Door top = template.door("top");
        for (int i = x; i < x + width; i++) {
            if (tiles[row][i] != ' ' ||
                (top != null && i >= top.offset - 1 && i < top.offset + top.span + 1))
                return false;
        }
        return true;
    }

    private static void place(char[][] tiles, int row, int x, int width, char tile,
                              RoomTemplateCatalog.RoomTemplate template) {
        if (!clear(tiles, row, x, width, template)) return;
        for (int i = x; i < x + width; i++) tiles[row][i] = tile;
    }

    private static void crackEnd(char[][] tiles, int row, int x) {
        if (tiles[row][x] == '_') tiles[row][x] = 'B';
    }
}
