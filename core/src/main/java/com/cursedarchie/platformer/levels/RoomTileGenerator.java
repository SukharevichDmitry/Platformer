package com.cursedarchie.platformer.levels;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Adds bounded platform and cover pieces above the guaranteed walking corridor. */
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
            if (template.role.equals("optional")) {
                Random random = new Random(layout.seed ^ (room.id * 0x9E3779B97F4A7C15L));
                // Each piece is a contiguous 2-4 tile ledge. Keep the lower two air rows clear.
                int pieces = template.patternId.equals("arena") ? 2 : 1;
                for (int piece = 0; piece < pieces; piece++) {
                    int length = 2 + random.nextInt(template.patternId.equals("arena") ? 3 : 2);
                    int x = 2 + random.nextInt(room.width - length - 3);
                    int floorY = 4 + (template.patternId.equals("arena") ? random.nextInt(2) : 0);
                    int row = room.height - 1 - floorY;
                    boolean clear = true;
                    for (int i = x; i < x + length; i++)
                        if (tiles[row][i] != ' ') clear = false;
                    if (clear) for (int i = x; i < x + length; i++) tiles[row][i] = '_';
                }
                // A short solid ceiling piece changes cover without touching entries or floor.
                int coverX = 2 + random.nextInt(room.width - 5);
                int coverRow = room.height - 1 - (template.patternId.equals("arena") ? 6 : 5);
                if (tiles[coverRow][coverX] == ' ' && tiles[coverRow][coverX + 1] == ' ') {
                    tiles[coverRow][coverX] = '#';
                    tiles[coverRow][coverX + 1] = '#';
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
}
