package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/** Stores generated runs as versioned JSON in cross-platform libGDX Preferences. */
public final class GeneratedRunStore {
    private static final int FORMAT_VERSION = 1;
    private static final String KEY_PREFIX = "location.";

    private final Preferences preferences;
    private final Json json = new Json(JsonWriter.OutputType.json);

    public GeneratedRunStore(Preferences preferences) {
        this.preferences = preferences;
        json.setIgnoreUnknownFields(false);
        json.setUsePrototypes(false);
    }

    public GeneratedRunSave load(String locationId, int profileVersion) {
        if (preferences == null) return null;
        String key = KEY_PREFIX + locationId;
        if (!preferences.contains(key)) return null;
        try {
            GeneratedRunSave save = json.fromJson(GeneratedRunSave.class, preferences.getString(key));
            if (save == null || save.formatVersion != FORMAT_VERSION ||
                save.profileVersion != profileVersion || !locationId.equals(save.locationId) ||
                save.topologySignature == null || save.placementSignature == null ||
                save.narrativeChoices == null || save.defeatedSpawnIds == null || save.brokenTileIds == null) {
                remove(locationId);
                return null;
            }
            return save;
        } catch (RuntimeException invalid) {
            remove(locationId);
            return null;
        }
    }

    public void save(GeneratedRunSave save) {
        if (preferences == null) return;
        save.formatVersion = FORMAT_VERSION;
        preferences.putString(KEY_PREFIX + save.locationId, json.toJson(save));
        preferences.flush();
    }

    public void remove(String locationId) {
        if (preferences == null) return;
        preferences.remove(KEY_PREFIX + locationId);
        preferences.flush();
    }
}
