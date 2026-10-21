package com.cursedarchie.platformer.world;

import com.cursedarchie.platformer.levels.RoomInstance;
import com.cursedarchie.platformer.levels.wrappers.RoomData;

import java.util.Arrays;
import java.util.List;

/** Focused checks for room-facing edges and the two sides of an inside corner. */
public final class StoneLightingCheck {
    public static void main(String[] args) {
        RoomInstance walled = room(0, 0,
            "######",
            "#    #",
            "# ## #",
            "#    #",
            "######");
        List<RoomInstance> rooms = Arrays.asList(walled);
        faces(rooms, 0, 4, 0, "outer top-left corner");
        corners(rooms, 0, 4, 64, "inner diagonal lights while outer faces stay dark");
        corners(rooms, 0, 0, 16, "inner diagonal lights while exterior corners stay dark");
        faces(rooms, 2, 4, 4, "ceiling lights its underside");
        faces(rooms, 0, 2, 2, "left wall lights its room-facing side");
        corners(rooms, 1, 4, 128, "ceiling underside meets lit wall face");
        corners(rooms, 0, 3, 16, "wall face meets lit ceiling underside");
        faces(rooms, 2, 2, 13, "floating stone lights three room-facing sides");
        faces(rooms, 3, 2, 7, "adjacent floating stone connects its faces");

        RoomInstance breakable = room(0, 0,
            "   ",
            " #B",
            "   ");
        faces(Arrays.asList(breakable), 1, 1, 13,
            "an intact full-height breakable block shields the adjoining face");
        breakable.removeTile(2, 1);
        faces(Arrays.asList(breakable), 1, 1, 15,
            "removing the breakable block exposes the adjoining face");

        RoomInstance neighbor = room(6, 0,
            "  ",
            "  ",
            "  ",
            "  ",
            "  ");
        faces(Arrays.asList(walled, neighbor), 5, 2, 10,
            "open cell in adjacent room lights the shared seam");

        RoomInstance hanging = room(0, 0,
            "       ",
            "       ",
            "   #   ",
            "  ###  ",
            "       ",
            "       ");
        rooms = Arrays.asList(hanging);
        faces(rooms, 2, 2, 13, "left hanging end lights top, bottom and left");
        faces(rooms, 4, 2, 7, "right hanging end lights top, bottom and right");
        corners(rooms, 2, 2, 16, "platform left inside corner");
        corners(rooms, 3, 3, 64 | 128, "pillar lower inside corners");
        corners(rooms, 4, 2, 32, "platform right inside corner");
        faces(rooms, 3, 2, 4, "floating platform underside below pillar");

        RoomInstance raisedFloor = room(0, 0,
            "      ",
            "  ##  ",
            "######",
            "######");
        rooms = Arrays.asList(raisedFloor);
        faces(rooms, 2, 1, 0, "left supporting block has no exposed face");
        corners(rooms, 2, 1, 32, "left supporting block lights its upper-left corner");
        faces(rooms, 3, 1, 0, "right supporting block has no exposed face");
        corners(rooms, 3, 1, 16, "right supporting block lights its upper-right corner");
        corners(rooms, 1, 1, 16, "floor beside left support joins the lit corner");
        corners(rooms, 4, 1, 32, "floor beside right support joins the lit corner");

        RoomInstance standalone = room(0, 0,
            "   ",
            " # ",
            "   ");
        faces(Arrays.asList(standalone), 1, 1, 15,
            "standalone stone lights all four room-facing sides");
        System.out.println("STONE LIGHTING PASSED: room bounds, floating faces, seams and inside corners");
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

    private static void faces(List<RoomInstance> rooms, int x, int y,
                              int expected, String description) {
        int actual = StoneLighting.mask(rooms, x, y) & 15;
        if (actual != expected)
            throw new AssertionError(description + ": expected " + expected + ", got " + actual);
    }

    private static void corners(List<RoomInstance> rooms, int x, int y,
                                int expected, String description) {
        int actual = StoneLighting.mask(rooms, x, y) & 240;
        if (actual != expected)
            throw new AssertionError(description + ": expected " + expected + ", got " + actual);
    }
}
