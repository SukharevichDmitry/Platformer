package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.tiles.TileFactory;
import com.cursedarchie.platformer.tiles.TileMetrics;

/** Finishes generated ceiling ribs at tile resolution. */
public final class RoomFineDecorator {
    private RoomFineDecorator() {}

    public static int decorate(RoomInstance room, RoomTemplateCatalog.RoomTemplate template) {
        if ("ruins".equals(template.biome)) return 0;
        // Door shafts must remain empty for the full height of the join.
        if (template.door("top") != null || template.door("bottom") != null) return 0;
        Tile[][] cells = room.getTiles();
        int ceilingY = room.getTileRows() - 4;
        int left = 2;
        while (left < room.getTileColumns() / 2 && cells[left][ceilingY] != null) left++;
        int right = room.getTileColumns() - 3;
        while (right > room.getTileColumns() / 2 && cells[right][ceilingY] != null) right--;
        int added = 0;
        // Each rib steps toward the chamber center, then drops by one tile.
        added += stone(cells, left, ceilingY);
        added += stone(cells, left - 1, ceilingY - 1);
        added += stone(cells, right, ceilingY);
        added += stone(cells, right + 1, ceilingY - 1);
        return added;
    }

    private static int stone(Tile[][] cells, int x, int y) {
        if (x < 1 || x >= cells.length - 1 || y < 6 || y >= cells[x].length - 2 ||
            cells[x][y] != null) return 0;
        cells[x][y] = TileFactory.create('#',
            new Vector2(x * TileMetrics.SIZE, y * TileMetrics.SIZE));
        return 1;
    }
}
