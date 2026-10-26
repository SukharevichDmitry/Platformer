package com.cursedarchie.platformer.world;

import com.cursedarchie.platformer.levels.RoomInstance;
import com.cursedarchie.platformer.levels.wrappers.RoomData;

import java.util.Arrays;
import java.util.List;

/** Room-facing masonry must follow the half-unit physical grid. */
public final class StoneLightingCheck {
    public static void main(String[] args) {
        RoomInstance walled = room(0, 0,
            "######",
            "#    #",
            "#    #",
            "######");
        List<RoomInstance> rooms = Arrays.asList(walled);
        bits(rooms, 0, 7, 15, 0, "outside top-left has no lit face");
        bits(rooms, 4, 6, 15, 4, "ceiling underside faces the room");
        bits(rooms, 1, 4, 15, 2, "left wall inner half-cell faces the room");
        bits(rooms, 0, 4, 15, 0, "left wall outer half-cell stays dark");

        RoomInstance neighbor = room(6, 0,
            "  ", "  ", "  ", "  ");
        bits(Arrays.asList(walled, neighbor), 11, 4, 15, 2,
            "adjacent room opens the shared wall face");

        RoomInstance breakable = room(0, 0,
            "   ", " #B", "   ");
        bits(Arrays.asList(breakable), 3, 2, 2, 0,
            "full breakable mini-cell shields its neighbor");
        breakable.removeTile(4, 2);
        bits(Arrays.asList(breakable), 3, 2, 2, 2,
            "removing one breakable mini-cell exposes the neighbor");

        RoomInstance corner = room(0, 0,
            "    ",
            " #  ",
            " ## ",
            "    ");
        bits(Arrays.asList(corner), 3, 3, 16, 16,
            "inside corner joins top and right faces");
        System.out.println("STONE LIGHTING PASSED: half-unit faces, seams, breakage and corners");
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
