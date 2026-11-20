package com.cursedarchie.platformer.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Tunable numbers of the "landed on an enemy from above" event: the hero and a standard enemy lose their
 * balance and fall side by side. Loaded from {@code config/head_collision.json}; missing keys use the defaults.
 */
public final class HeadCollisionConfig {
    /** Time the hero spends in FALL_COLLISION (the fall itself). */
    public float heroFallDuration = .22f;
    /** Time the hero then spends in FALL_RECOVERY before control returns. */
    public float heroFallRecovery = .55f;
    /** Time the enemy spends in KNOCKDOWN_FROM_ABOVE (the fall itself). */
    public float enemyKnockdownDuration = .22f;
    /** Time the enemy then spends in KNOCKDOWN_RECOVERY: longer than the hero's, so the player can reposition. */
    public float enemyKnockdownRecovery = .70f;
    /** A new fall onto the same enemy is refused for this long after an event. */
    public float repeatCollisionLock = .20f;
    /** Gap kept between the two bodies after the fall (never exactly zero). */
    public float smallGap = .05f;
    /** How far the enemy itself slides in the direction of the fall. */
    public float enemyShift = .20f;

    /**
     * Only a fall of at least this fraction of the hero's maximum jump height topples anybody. A plain jump onto an
     * enemy (which never drops further than a jump is high) just pushes the hero back.
     */
    public float minFallHeightRatio = .9f;
    /** The mild push-back for a jump onto a head: an added sideways speed (for this long) and a small hop. Control stays. */
    public float pushbackSpeed = 6f;
    public float pushbackHop = 12f;
    public float pushbackDuration = .25f;

    private static HeadCollisionConfig current;

    public static synchronized HeadCollisionConfig get() {
        if (current == null) current = load();
        return current;
    }

    public static synchronized void set(HeadCollisionConfig config) { current = config; }

    public static HeadCollisionConfig load() {
        try {
            if (Gdx.files != null && Gdx.files.internal("config/head_collision.json").exists())
                return parse(Gdx.files.internal("config/head_collision.json").readString("UTF-8"));
        } catch (RuntimeException ignored) {
            // A broken file must never break the game: fall through to the defaults.
        }
        return new HeadCollisionConfig();
    }

    public static HeadCollisionConfig parse(String json) {
        JsonValue root = new JsonReader().parse(json);
        HeadCollisionConfig d = new HeadCollisionConfig();
        d.heroFallDuration = Math.max(.05f, root.getFloat("heroFallDuration", d.heroFallDuration));
        d.heroFallRecovery = Math.max(.05f, root.getFloat("heroFallRecovery", d.heroFallRecovery));
        d.enemyKnockdownDuration = Math.max(.05f, root.getFloat("enemyKnockdownDuration", d.enemyKnockdownDuration));
        d.enemyKnockdownRecovery = Math.max(.05f, root.getFloat("enemyKnockdownRecovery", d.enemyKnockdownRecovery));
        d.repeatCollisionLock = Math.max(0f, root.getFloat("repeatCollisionLock", d.repeatCollisionLock));
        d.smallGap = Math.max(.01f, root.getFloat("smallGap", d.smallGap));
        d.enemyShift = Math.max(0f, root.getFloat("enemyShift", d.enemyShift));
        d.minFallHeightRatio = Math.max(.1f, root.getFloat("minFallHeightRatio", d.minFallHeightRatio));
        d.pushbackSpeed = Math.max(0f, root.getFloat("pushbackSpeed", d.pushbackSpeed));
        d.pushbackHop = Math.max(0f, root.getFloat("pushbackHop", d.pushbackHop));
        d.pushbackDuration = Math.max(.05f, root.getFloat("pushbackDuration", d.pushbackDuration));
        return d;
    }
}
