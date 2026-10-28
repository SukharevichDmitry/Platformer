package com.cursedarchie.platformer.run;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import java.util.ArrayList;

/** One active run, stored beside its generated-location records. */
public final class RunCheckpointStore {
    private static final int FORMAT_VERSION = 7;
    private static final int WORLD_LAYOUT_VERSION = 10;
    private static final String KEY = "run.active";
    private final Preferences preferences;
    private final Json json = new Json(JsonWriter.OutputType.json);

    public RunCheckpointStore(Preferences preferences) {
        this.preferences = preferences;
        json.setUsePrototypes(false);
        json.setIgnoreUnknownFields(false);
    }

    public RunCheckpoint load() {
        if (!preferences.contains(KEY)) return null;
        try {
            RunCheckpoint checkpoint = json.fromJson(RunCheckpoint.class, preferences.getString(KEY));
            if (checkpoint == null || checkpoint.formatVersion < 2 ||
                checkpoint.formatVersion > FORMAT_VERSION ||
                checkpoint.worldLayoutVersion != WORLD_LAYOUT_VERSION ||
                checkpoint.locationId == null || checkpoint.locationId.isEmpty() ||
                checkpoint.roomId < 0 || checkpoint.visitedRooms == null ||
                checkpoint.generatedLocations == null ||
                checkpoint.consumedInteractionIds == null || checkpoint.runFlags == null ||
                !Float.isFinite(checkpoint.heroX) || !Float.isFinite(checkpoint.heroY) ||
                !Float.isFinite(checkpoint.heroHealth) || checkpoint.heroHealth <= 0f ||
                checkpoint.fieldBandages < 0 || checkpoint.fieldBandages > ItemCatalog.bandage().maxStack ||
                (checkpoint.formatVersion >= 4 &&
                    (checkpoint.inventory == null || checkpoint.inventory.size !=
                        (checkpoint.formatVersion >= 7 ? Inventory.TOTAL_SLOTS :
                            checkpoint.formatVersion == 6 ? 18 : 16)))) {
                clearRun();
                return null;
            }
            if (checkpoint.formatVersion >= 4) {
                Inventory bag = new Inventory();
                for (int i = 0; i < checkpoint.inventory.size; i++) {
                    RunCheckpoint.InventorySlot slot = checkpoint.inventory.get(i);
                    if (slot == null) throw new IllegalArgumentException("Missing inventory slot");
                    if (slot.itemId == null && slot.count == 0) continue;
                    if (!ItemCatalog.supports(slot.itemId) || slot.count < 1 ||
                        slot.count > ItemCatalog.maxStack(slot.itemId))
                        throw new IllegalArgumentException("Invalid inventory slot");
                    if (checkpoint.formatVersion >= 7) bag.setSlot(i, slot.itemId, slot.count);
                }
            }
            return checkpoint;
        } catch (RuntimeException invalid) {
            clearRun();
            return null;
        }
    }

    public void save(RunCheckpoint checkpoint) {
        checkpoint.formatVersion = FORMAT_VERSION;
        checkpoint.worldLayoutVersion = WORLD_LAYOUT_VERSION;
        preferences.putString(KEY, json.toJson(checkpoint));
        preferences.flush();
    }

    /** Remove only run-owned records; leave application settings intact. */
    public void clearRun() {
        preferences.remove(KEY);
        for (String key : new ArrayList<>(preferences.get().keySet())) {
            if (key.startsWith("location.")) preferences.remove(key);
        }
        preferences.flush();
    }
}
