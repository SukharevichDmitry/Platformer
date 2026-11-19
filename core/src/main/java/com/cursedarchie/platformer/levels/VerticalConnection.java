package com.cursedarchie.platformer.levels;

/** Traversable shaft joining the floor of a lower room to an upper room. */
public final class VerticalConnection {
    public enum Type { STAIR_DESCENT }
    public enum Direction { DOWN }

    /** Matches the visible narrow ladder, with a small hero-center input allowance. */
    public static final float GRAB_CENTER_TOLERANCE = 0.60f;
    public final Type type;
    public final String id;
    public final String sourceLocation;
    public final int sourceRoomId;
    public final String sourceConnectionId;
    public final String targetLocation;
    public final int targetRoomId;
    public final String targetConnectionId;
    public final Direction direction;
    public final int lowerRoomId;
    public final int upperRoomId;
    public final float centerX;
    public final float halfWidth;
    public final float bottomY;
    public final float topY;

    VerticalConnection(PlacedRoomGraph.DoorJoin join,
                       PlacedRoomGraph.PlacedRoom lower, PlacedRoomGraph.PlacedRoom upper) {
        type = null;
        id = null;
        sourceLocation = targetLocation = null;
        sourceConnectionId = targetConnectionId = null;
        sourceRoomId = upper.id;
        targetRoomId = lower.id;
        direction = null;
        lowerRoomId = lower.id;
        upperRoomId = upper.id;
        centerX = join.worldX + join.span / 2f;
        halfWidth = join.span / 2f;
        bottomY = lower.worldY + 2.5f;
        // Give the hero's feet clearance above the upper floor before stepping sideways.
        topY = upper.worldY + 3.3f;
    }

    private VerticalConnection(String location, String id, String sourceConnectionId,
                               String targetConnectionId, RoomInstance upper,
                               RoomInstance lower, float centerX) {
        type = Type.STAIR_DESCENT;
        this.id = id;
        sourceLocation = targetLocation = location;
        sourceRoomId = upper.getRoomId();
        targetRoomId = lower.getRoomId();
        this.sourceConnectionId = sourceConnectionId;
        this.targetConnectionId = targetConnectionId;
        direction = Direction.DOWN;
        upperRoomId = upper.getRoomId();
        lowerRoomId = lower.getRoomId();
        this.centerX = centerX;
        halfWidth = .56f;
        bottomY = lower.getWorldY() + 2.5f;
        topY = upper.getWorldY() + 3.3f;
    }

    static VerticalConnection stairDescent(String location, String id,
                                           String sourceConnectionId,
                                           String targetConnectionId,
                                           RoomInstance upper, RoomInstance lower,
                                           float centerX) {
        return new VerticalConnection(location, id, sourceConnectionId,
            targetConnectionId, upper, lower, centerX);
    }

    public boolean contains(float x, float y, float heroWidth) {
        float clearance = Math.min(GRAB_CENTER_TOLERANCE, halfWidth - heroWidth / 2f + 0.1f);
        return Math.abs(x - centerX) <= clearance &&
            y >= bottomY - 0.4f && y <= topY + 0.4f;
    }
}
