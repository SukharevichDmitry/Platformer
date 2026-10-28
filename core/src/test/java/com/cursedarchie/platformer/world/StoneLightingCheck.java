package com.cursedarchie.platformer.world;

import com.cursedarchie.platformer.levels.RoomInstance;
import com.cursedarchie.platformer.levels.wrappers.RoomData;

import java.util.Arrays;
import java.util.List;

/** Room-facing masonry follows the one-character terrain grid. */
public final class StoneLightingCheck {
    public static void main(String[] args) {
        RoomInstance walled = room(0, 0,
            "######",
            "#    #",
            "#    #",
            "######");
        List<RoomInstance> rooms = Arrays.asList(walled);
        bits(rooms, 0, 4, 15, 0, "outside top-left has no lit face");
        bits(rooms, 2, 3, 15, 4, "ceiling underside faces the room");
        bits(rooms, 0, 2, 15, 2, "left wall faces the room");

        RoomInstance neighbor = room(6, 0,
            "  ", "  ", "  ", "  ");
        bits(Arrays.asList(walled, neighbor), 5, 2, 2, 2,
            "adjacent room opens the shared wall face");

        RoomInstance breakable = room(0, 0,
            "   ", " #B", "   ");
        bits(Arrays.asList(breakable), 1, 1, 2, 0,
            "full breakable cell shields its neighbor");
        breakable.removeTile(2, 1);
        bits(Arrays.asList(breakable), 1, 1, 2, 2,
            "removing one breakable cell exposes the neighbor");

        RoomInstance corner = room(0, 0,
            "    ",
            " #  ",
            " ## ",
            "    ");
        bits(Arrays.asList(corner), 1, 1, 16, 16,
            "inside corner joins top and right faces");
        System.out.println("STONE LIGHTING PASSED: tile faces, seams, breakage and corners");
    }

    private static RoomInstance room(int x, int y, String... rows) {
        RoomData data = new RoomData();
        data.width = rows[0].length();
        data.height = rows.length;
        data.worldX = x;
        data.worldY = y;
        data.tileRows = rows;
        RoomInstance room = new RoomInstance(data);
        room.buildTiles();
        return room;
    }

    private static void bits(List<RoomInstance> rooms, int x, int y, int mask,
                             int expected, String description) {
        int actual = StoneLighting.mask(rooms, x, y) & mask;
        if (actual != expected)
            throw new AssertionError(description + ": expected " + expected + ", got " + actual);
    }
}
