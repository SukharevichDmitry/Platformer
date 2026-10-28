package com.cursedarchie.platformer.run;

import com.badlogic.gdx.utils.Array;

/** Suspend point for one living run. All fields belong to this run only. */
public final class RunCheckpoint {
    public int formatVersion;
    public int worldLayoutVersion;
    public long seed;
    public String locationId;
    public int roomId;
    public float heroX;
    public float heroY;
    public float heroHealth;
    public Array<String> visitedRooms = new Array<>();
    public Array<String> consumedInteractionIds = new Array<>();
    public Array<String> runFlags = new Array<>();
    public int fieldBandages;
    public Array<InventorySlot> inventory = new Array<>();
    public Array<GeneratedLocationRef> generatedLocations = new Array<>();

    public static final class InventorySlot {
        public String itemId;
        public int count;
        public InventorySlot() { }
    }

    public RunCheckpoint() { }

    /** Expected location record prevents Continue from mixing incompatible run data. */
    public static final class GeneratedLocationRef {
        public String locationId;
        public int profileVersion;
        public String topologySignature;
        public String placementSignature;

        public GeneratedLocationRef() { }
    }
}
