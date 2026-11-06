package com.cursedarchie.platformer.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Tunable numbers of the three-strike rhythm and its finisher.
 * Loaded from {@code config/attack_rhythm.json}; any missing key or file falls back to the defaults below.
 */
public final class AttackRhythmConfig {
    /** How long the combo window stays open once it has started. */
    public float comboWindowLength = .15f;
    /** Slightly more generous window for LIGHT_2 -> FINISHER. */
    public float finisherComboWindowLength = .20f;
    /** Extra window length for gentler difficulty settings. */
    public float comboWindowBonus = 0f;
    /** A correct press is discarded if it cannot be consumed within this time. */
    public float inputBufferTime = .18f;
    public float light1WindowStart = .22f;
    public float light2WindowStart = .27f;
    public float finisherStartupDuration = .10f;
    public float finisherActiveDuration = .15f;
    public float finisherRecoveryDuration = .68f;
    public float finisherRange = 2.3f;
    public float finisherHitboxWidth = 1.65f;
    public float finisherHitboxHeight = 1.1f;
    public float finisherOffsetY = .08f;
    public float finisherDamageMultiplier = 1.45f;
    public float finisherKnockback = 12f;
    public float finisherStaggerDuration = .62f;
    public float finisherShieldDamageMultiplier = 2.5f;
    public float hurriedDamageMultiplier = .8f;
    public float hurriedReachMultiplier = .85f;
    public float hurriedKnockbackMultiplier = .6f;
    public float hurriedStaggerMultiplier = .6f;
    public float hurriedRecoveryMultiplier = 1.3f;
    /** Flat seconds added to a hurried strike's recovery on top of the multiplier. */
    public float hurriedRecoveryExtra = .2f;
    public float secondStrikeKnockbackMultiplier = 1.15f;
    /** Rhythm highlight: full strength for new players, fading to the minimum after this many clean combos. */
    public int highlightFadeCombos = 0;
    public float highlightMinIntensity = 1f;

    private static AttackRhythmConfig current;

    public static synchronized AttackRhythmConfig get() {
        if (current == null) current = load();
        return current;
    }

    /** Replaces the active config (used by difficulty settings and tests). */
    public static synchronized void set(AttackRhythmConfig config) { current = config; }

    public static AttackRhythmConfig load() {
        try {
            if (Gdx.files != null && Gdx.files.internal("config/attack_rhythm.json").exists())
                return parse(Gdx.files.internal("config/attack_rhythm.json").readString("UTF-8"));
        } catch (RuntimeException ignored) {
            // A broken file must never break combat: fall through to the defaults.
        }
        return new AttackRhythmConfig();
    }

    public static AttackRhythmConfig parse(String json) {
        JsonValue root = new JsonReader().parse(json);
        AttackRhythmConfig d = new AttackRhythmConfig();
        d.comboWindowLength = root.getFloat("comboWindowLength", d.comboWindowLength);
        d.finisherComboWindowLength = Math.max(d.comboWindowLength,
            root.getFloat("finisherComboWindowLength", d.finisherComboWindowLength));
        d.comboWindowBonus = root.getFloat("comboWindowBonus", d.comboWindowBonus);
        d.inputBufferTime = Math.max(.01f, root.getFloat("inputBufferTime", d.inputBufferTime));
        d.light1WindowStart = Math.max(0f, root.getFloat("light1WindowStart", d.light1WindowStart));
        d.light2WindowStart = Math.max(0f, root.getFloat("light2WindowStart", d.light2WindowStart));
        JsonValue finisher = root.get("finisher");
        if (finisher != null && finisher.isObject()) {
            d.finisherStartupDuration = Math.max(0f,
                finisher.getFloat("startupDuration", d.finisherStartupDuration));
            d.finisherActiveDuration = Math.max(.01f,
                finisher.getFloat("activeDuration", d.finisherActiveDuration));
            d.finisherRecoveryDuration = Math.max(.01f,
                finisher.getFloat("recoveryDuration", d.finisherRecoveryDuration));
            d.finisherRange = Math.max(.1f, finisher.getFloat("range", d.finisherRange));
            d.finisherHitboxWidth = Math.max(.1f,
                finisher.getFloat("hitboxWidth", d.finisherHitboxWidth));
            d.finisherHitboxHeight = Math.max(.1f,
                finisher.getFloat("hitboxHeight", d.finisherHitboxHeight));
            d.finisherOffsetY = finisher.getFloat("offsetY", d.finisherOffsetY);
            d.finisherDamageMultiplier = Math.max(0f,
                finisher.getFloat("damageMultiplier", d.finisherDamageMultiplier));
            d.finisherKnockback = Math.max(0f, finisher.getFloat("knockback", d.finisherKnockback));
            d.finisherStaggerDuration = Math.max(0f,
                finisher.getFloat("staggerDuration", d.finisherStaggerDuration));
            d.finisherShieldDamageMultiplier = Math.max(0f,
                finisher.getFloat("shieldDamageMultiplier", d.finisherShieldDamageMultiplier));
        }
        d.hurriedDamageMultiplier = root.getFloat("hurriedDamageMultiplier", d.hurriedDamageMultiplier);
        d.hurriedReachMultiplier = root.getFloat("hurriedReachMultiplier", d.hurriedReachMultiplier);
        d.hurriedKnockbackMultiplier = root.getFloat("hurriedKnockbackMultiplier", d.hurriedKnockbackMultiplier);
        d.hurriedStaggerMultiplier = root.getFloat("hurriedStaggerMultiplier", d.hurriedStaggerMultiplier);
        d.hurriedRecoveryMultiplier = root.getFloat("hurriedRecoveryMultiplier", d.hurriedRecoveryMultiplier);
        d.secondStrikeKnockbackMultiplier =
            root.getFloat("secondStrikeKnockbackMultiplier", d.secondStrikeKnockbackMultiplier);
        d.hurriedRecoveryExtra = root.getFloat("hurriedRecoveryExtra", d.hurriedRecoveryExtra);
        d.highlightFadeCombos = root.getInt("highlightFadeCombos", d.highlightFadeCombos);
        d.highlightMinIntensity = root.getFloat("highlightMinIntensity", d.highlightMinIntensity);
        return d;
    }

    /** Strength of the timing highlight after {@code cleanCombos} successful combos (1 = brightest). */
    public float highlightIntensity(int cleanCombos) {
        float t = highlightFadeCombos <= 0 ? 1f : Math.min(1f, cleanCombos / (float) highlightFadeCombos);
        return 1f + (highlightMinIntensity - 1f) * t;
    }
}
