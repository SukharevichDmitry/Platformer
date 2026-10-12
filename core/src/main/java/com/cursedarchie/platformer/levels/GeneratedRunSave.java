package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.utils.Array;

/** Versioned persistent description of one generated location. */
public final class GeneratedRunSave {
    public int formatVersion;
    public int profileVersion;
    public String locationId;
    public long seed;
    public String topologySignature;
    public String placementSignature;
    public Array<String> narrativeChoices = new Array<>();
    public Array<String> defeatedSpawnIds = new Array<>();

    public GeneratedRunSave() { }
}
