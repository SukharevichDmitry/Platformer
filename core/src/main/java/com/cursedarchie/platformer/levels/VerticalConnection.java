package com.cursedarchie.platformer.levels;

/** Traversable shaft joining the floor of a lower room to an upper room. */
public final class VerticalConnection {
    /** Matches the visible narrow ladder, with a small hero-center input allowance. */
    public static final float GRAB_CENTER_TOLERANCE = 0.60f;
    public final int lowerRoomId;
    public final int upperRoomId;
    public final float centerX;
    public final float halfWidth;
    public final float bottomY;
    public final float topY;

    VerticalConnection(PlacedRoomGraph.DoorJoin join,
                       PlacedRoomGraph.PlacedRoom lower, PlacedRoomGraph.PlacedRoom upper) {
        lowerRoomId = lower.id;
        upperRoomId = upper.id;
        centerX = join.worldX + join.span / 2f;
        halfWidth = join.span / 2f;
        bottomY = lower.worldY + 2.5f;
        // Give the hero's feet clearance above the upper floor before stepping sideways.
        topY = upper.worldY + 3.3f;
    }

    public boolean contains(float x, float y, float heroWidth) {
        float clearance = Math.min(GRAB_CENTER_TOLERANCE, halfWidth - heroWidth / 2f + 0.1f);
        return Math.abs(x - centerX) <= clearance &&
            y >= bottomY - 0.4f && y <= topY + 0.4f;
    }
}
