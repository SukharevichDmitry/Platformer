package com.cursedarchie.platformer.world;

import com.cursedarchie.platformer.levels.RoomInstance;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.tiles.TileMetrics;

/** Selects painted stone faces from room interior space, including room seams. */
final class StoneLighting {
    private static final int OUTSIDE = 0;
    private static final int OPEN = 1;
    private static final int SOLID = 2;

    private StoneLighting() {}

    /** Low four bits are top/right/bottom/left faces; high four are their corners. */
    static int mask(Iterable<RoomInstance> rooms, int worldX, int worldY) {
        int top = cell(rooms, worldX, worldY + 1);
        int right = cell(rooms, worldX + 1, worldY);
        int bottom = cell(rooms, worldX, worldY - 1);
        int left = cell(rooms, worldX - 1, worldY);
        int faces = (top == OPEN ? 1 : 0) | (right == OPEN ? 2 : 0)
            | (bottom == OPEN ? 4 : 0) | (left == OPEN ? 8 : 0);

        // A three-stone joint lights all three stones: the two blocks with
        // exposed faces and the solid block diagonally opposite the open cell.
        // Space outside every room cannot light any of these corners.
        int corners = 0;
        int topRight = cell(rooms, worldX + 1, worldY + 1);
        int topLeft = cell(rooms, worldX - 1, worldY + 1);
        int bottomRight = cell(rooms, worldX + 1, worldY - 1);
        int bottomLeft = cell(rooms, worldX - 1, worldY - 1);
        if ((topRight == SOLID &&
            ((top == OPEN && right == SOLID) || (right == OPEN && top == SOLID))) ||
            (topRight == OPEN && top == SOLID && right == SOLID))
            corners |= 16; // top right
        if ((topLeft == SOLID &&
            ((top == OPEN && left == SOLID) || (left == OPEN && top == SOLID))) ||
            (topLeft == OPEN && top == SOLID && left == SOLID))
            corners |= 32; // top left
        if ((bottomRight == SOLID &&
            ((bottom == OPEN && right == SOLID) || (right == OPEN && bottom == SOLID))) ||
            (bottomRight == OPEN && bottom == SOLID && right == SOLID))
            corners |= 64; // bottom right
        if ((bottomLeft == SOLID &&
            ((bottom == OPEN && left == SOLID) || (left == OPEN && bottom == SOLID))) ||
            (bottomLeft == OPEN && bottom == SOLID && left == SOLID))
            corners |= 128; // bottom left
        return faces | corners;
    }

    private static int cell(Iterable<RoomInstance> rooms, int worldX, int worldY) {
        boolean inside = false;
        for (RoomInstance room : rooms) {
            int x = worldX - Math.round(room.getWorldX());
            int y = worldY - Math.round(room.getWorldY());
            if (x < 0 || y < 0 || x >= room.getTileColumns() || y >= room.getTileRows()) continue;
            inside = true;
            Tile[][] tiles = room.getTiles();
            Tile tile = tiles == null ? null : tiles[x][y];
            // Breakable masonry also fills its cell, though it is excluded
            // from merged terrain fixtures and reports isFullCellSolid false.
            if (tile != null && tile.getBounds().width >= TileMetrics.SIZE &&
                tile.getBounds().height >= TileMetrics.SIZE)
                return SOLID;
        }
        return inside ? OPEN : OUTSIDE;
    }
}
