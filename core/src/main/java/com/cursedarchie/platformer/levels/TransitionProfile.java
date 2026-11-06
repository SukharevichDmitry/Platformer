package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * One animated location transition from {@code config/transitions.json}, named by a portal's
 * {@code transition} field. The alley turn is a 2.5D camera turn drawn from captured 2D frames; physics and the
 * Box2D world never rotate, and the hero's gameplay state is untouched while control is locked.
 */
public final class TransitionProfile {
    public static final String FILE = "config/transitions.json";

    public final String id;
    public final String kind;
    /** +1 counter-clockwise around the vertical axis (entering an alley), -1 clockwise (leaving it). */
    public final int direction;
    public final float turnDuration, settleDuration, switchAt, heroTurnUntil;
    /** Foreground column: width as a fraction of the screen; it fully hides the hero around switchAt. */
    public final float occluderWidth, coverFrom, coverTo;
    /** The view eases this fraction of the screen width to the right while the camera settles. */
    public final float cameraShift, fadePeak;
    public final int dustCount;
    public final String sound;
    public final float soundVolume;
    /** The source location, room and return spawn are remembered for the matching exit. */
    public final boolean savesOrigin;
    public final String originSpawnId;

    private TransitionProfile(String id, JsonValue v) {
        this.id = id;
        kind = v.getString("kind", "alley_turn");
        String turn = v.getString("direction", "counterclockwise");
        if (!"counterclockwise".equals(turn) && !"clockwise".equals(turn))
            throw new IllegalArgumentException("Transition " + id + " has an unknown direction: " + turn);
        direction = "clockwise".equals(turn) ? -1 : 1;
        turnDuration = positive(v, "turnDuration", .9f);
        settleDuration = Math.max(0f, v.getFloat("settleDuration", .3f));
        switchAt = fraction(v, "switchAt", .5f);
        heroTurnUntil = fraction(v, "heroTurnUntil", .38f);
        occluderWidth = fraction(v, "occluderWidth", .17f);
        coverFrom = fraction(v, "coverFrom", .34f);
        coverTo = fraction(v, "coverTo", .66f);
        if (!(coverFrom < switchAt && switchAt < coverTo))
            throw new IllegalArgumentException("Transition " + id + " must hide the hero at its switch point");
        cameraShift = Math.max(0f, v.getFloat("cameraShift", 0f));
        fadePeak = fraction(v, "fadePeak", .3f);
        dustCount = Math.max(0, v.getInt("dustCount", 0));
        sound = v.getString("sound", null);
        soundVolume = fraction(v, "soundVolume", .25f);
        savesOrigin = v.getBoolean("savesOrigin", false);
        originSpawnId = v.getString("originSpawnId", null);
        if (savesOrigin && originSpawnId == null)
            throw new IllegalArgumentException("Transition " + id + " saves an origin but names no originSpawnId");
    }

    public float totalDuration() { return turnDuration + settleDuration; }

    private static float positive(JsonValue v, String key, float fallback) {
        float value = v.getFloat(key, fallback);
        if (!(value > 0f)) throw new IllegalArgumentException(key + " must be positive");
        return value;
    }

    private static float fraction(JsonValue v, String key, float fallback) {
        float value = v.getFloat(key, fallback);
        if (value < 0f || value > 1f) throw new IllegalArgumentException(key + " must be between 0 and 1");
        return value;
    }

    private static ObjectMap<String, TransitionProfile> loaded;

    public static ObjectMap<String, TransitionProfile> parse(String json) {
        ObjectMap<String, TransitionProfile> profiles = new ObjectMap<>();
        for (JsonValue entry = new JsonReader().parse(json).child; entry != null; entry = entry.next)
            if (entry.isObject()) profiles.put(entry.name, new TransitionProfile(entry.name, entry));
        return profiles;
    }

    public static ObjectMap<String, TransitionProfile> load(FileHandle file) {
        return parse(file.readString("UTF-8"));
    }

    /** Profile by id from the game's assets, or null for an unknown or missing id. */
    public static synchronized TransitionProfile get(String id) {
        if (id == null) return null;
        if (loaded == null) loaded = load(Gdx.files.internal(FILE));
        return loaded.get(id);
    }
}
