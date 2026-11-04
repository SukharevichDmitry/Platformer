package com.cursedarchie.platformer.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Tunable numbers of the attack rhythm (hurried strikes and the combo window).
 * Loaded from {@code config/attack_rhythm.json}; any missing key or file falls back to the defaults below.
 */
public final class AttackRhythmConfig {
    /** How long the combo window stays open once it has started. */
    public float comboWindowLength = .15f;
    /** Extra window length for gentler difficulty settings. */
    public float comboWindowBonus = 0f;
    public float hurriedDamageMultiplier = .8f;
    public float hurriedReachMultiplier = .85f;
    public float hurriedKnockbackMultiplier = .6f;
    public float hurriedStaggerMultiplier = .6f;
    public float hurriedRecoveryMultiplier = 1.3f;
    /** Flat seconds added to a hurried strike's recovery on top of the multiplier. */
    public float hurriedRecoveryExtra = .2f;
    public float secondStrikeKnockbackMultiplier = 1.15f;
    /** Rhythm highlight: full strength for new players, fading to the minimum after this many clean combos. */
    public int highlightFadeCombos = 12;
    public float highlightMinIntensity = .3f;

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
        d.comboWindowBonus = root.getFloat("comboWindowBonus", d.comboWindowBonus);
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
