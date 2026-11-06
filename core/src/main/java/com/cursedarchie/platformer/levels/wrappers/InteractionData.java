package com.cursedarchie.platformer.levels.wrappers;

import com.badlogic.gdx.math.Vector2;

/** Authored room object; its identity and effect are independent of art. */
public final class InteractionData {
    public String id;
    public String kind;
    public String eventId;
    public String itemId;
    public String visualId;
    public Vector2 position;
    public String targetLocation;
    public int targetRoomId;
    public String targetSpawnId;
    public boolean returnToOrigin;
    public String requiresFlag;
    /** Portal only: id of an animated transition in config/transitions.json, e.g. "alley_enter". */
    public String transition;

    public InteractionData() { }
}
