package com.cursedarchie.platformer.actors.enemies.group;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Tunable numbers of the group-positioning system (combat slots around the hero and soft pushing between
 * allies). Loaded from {@code config/group_positioning.json}; missing keys keep the defaults below.
 * All distances are metres measured from the hero's centre along the floor.
 */
public final class GroupPositioningConfig {
    /** Close-range melee tiers on each side of the hero: nearest ("front"), middle ("side") and outer ("rear"). */
    public float frontOffset = 1.05f, sideOffset = 1.65f, rearOffset = 2.3f;
    /** Surrounding (reserve) positions: where an enemy waits, combat-ready, when no melee slot is free. */
    public float reserveStart = 3.2f, reserveStep = 1.0f;
    public int reserveCount = 3;
    /** Archers keep rangedMin <= distance <= rangedMax; their slots lie between those limits. */
    public float rangedMinDistance = 3.0f, rangedMaxDistance = 7.2f, rangedFirst = 3.6f, rangedStep = 1.6f;
    public int rangedCount = 3;
    /** The support caster stays at least this far from the hero and behind the front line. */
    public float supportMinDistance = 3.6f, supportFirst = 5.4f, supportStep = 1.5f;
    public int supportCount = 2;
    /** Enemies farther than this (horizontally) from the hero take no part in the formation. */
    public float engageRange = 12f;
    /** Minimum centre distance between two allies, by pair kind. Soft: it steers, it never blocks. */
    public float standardMinDistance = .6f, heavyStandardMinDistance = 1.2f, heavyHeavyMinDistance = 1.9f,
        rangedAllyMinDistance = 1.0f, supportAllyMinDistance = 1.4f;
    /** force = direction * overlap * separationStrength, then capped at maxSeparationSpeed (m/s). */
    public float separationStrength = 5.5f, maxSeparationSpeed = 1.9f;
    /** An enemy counts as "in position" within arriveTolerance and stays so until restartTolerance is exceeded. */
    public float arriveTolerance = .12f, restartTolerance = .3f;
    /** Cost bonus that keeps an enemy on the slot it already has, so assignments do not flicker. */
    public float stickiness = 1.2f;
    /** Step-away time after an attack (standard / fast); heavies hold a slot at least heavyHoldTime. */
    public float leaveDuration = .7f, fastLeaveDuration = .4f, heavyHoldTime = 2.5f;
    /** Leaping over the hero to reach a free slot on the other side: launch point, reach, cooldown, speed cap. */
    public float launchDistance = 2.0f, leapMaxStartDistance = 2.7f, leapCooldown = 2.5f, leapSpeedCap = 9f;
    /** Extra cost of choosing a slot on the far side of the hero (infiltrators pay almost nothing). */
    public float crossingPenalty = 3.5f;

    private static GroupPositioningConfig current;

    public static synchronized GroupPositioningConfig get() {
        if (current == null) current = load();
        return current;
    }

    public static synchronized void set(GroupPositioningConfig config) { current = config; }

    public static GroupPositioningConfig load() {
        try {
            if (Gdx.files != null && Gdx.files.internal("config/group_positioning.json").exists())
                return parse(Gdx.files.internal("config/group_positioning.json").readString("UTF-8"));
        } catch (RuntimeException ignored) {
            // A broken file must never break the game: fall through to the defaults.
        }
        return new GroupPositioningConfig();
    }

    public static GroupPositioningConfig parse(String json) {
        JsonValue r = new JsonReader().parse(json);
        GroupPositioningConfig d = new GroupPositioningConfig();
        d.frontOffset = Math.max(.5f, r.getFloat("frontOffset", d.frontOffset));
        d.sideOffset = Math.max(d.frontOffset + .3f, r.getFloat("sideOffset", d.sideOffset));
        d.rearOffset = Math.max(d.sideOffset + .3f, r.getFloat("rearOffset", d.rearOffset));
        d.reserveStart = Math.max(d.rearOffset + .3f, r.getFloat("reserveStart", d.reserveStart));
        d.reserveStep = Math.max(.5f, r.getFloat("reserveStep", d.reserveStep));
        d.reserveCount = Math.max(1, r.getInt("reserveCount", d.reserveCount));
        d.rangedMinDistance = Math.max(1f, r.getFloat("rangedMinDistance", d.rangedMinDistance));
        d.rangedMaxDistance = Math.max(d.rangedMinDistance + 1f, r.getFloat("rangedMaxDistance", d.rangedMaxDistance));
        d.rangedFirst = Math.max(d.rangedMinDistance, r.getFloat("rangedFirst", d.rangedFirst));
        d.rangedStep = Math.max(.5f, r.getFloat("rangedStep", d.rangedStep));
        d.rangedCount = Math.max(1, r.getInt("rangedCount", d.rangedCount));
        d.supportMinDistance = Math.max(1f, r.getFloat("supportMinDistance", d.supportMinDistance));
        d.supportFirst = Math.max(d.supportMinDistance, r.getFloat("supportFirst", d.supportFirst));
        d.supportStep = Math.max(.5f, r.getFloat("supportStep", d.supportStep));
        d.supportCount = Math.max(1, r.getInt("supportCount", d.supportCount));
        d.engageRange = Math.max(4f, r.getFloat("engageRange", d.engageRange));
        d.standardMinDistance = Math.max(.3f, r.getFloat("standardMinDistance", d.standardMinDistance));
        d.heavyStandardMinDistance = Math.max(.5f, r.getFloat("heavyStandardMinDistance", d.heavyStandardMinDistance));
        d.heavyHeavyMinDistance = Math.max(.5f, r.getFloat("heavyHeavyMinDistance", d.heavyHeavyMinDistance));
        d.rangedAllyMinDistance = Math.max(.3f, r.getFloat("rangedAllyMinDistance", d.rangedAllyMinDistance));
        d.supportAllyMinDistance = Math.max(.3f, r.getFloat("supportAllyMinDistance", d.supportAllyMinDistance));
        d.separationStrength = Math.max(0f, r.getFloat("separationStrength", d.separationStrength));
        d.maxSeparationSpeed = Math.max(0f, r.getFloat("maxSeparationSpeed", d.maxSeparationSpeed));
        d.arriveTolerance = Math.max(.02f, r.getFloat("arriveTolerance", d.arriveTolerance));
        d.restartTolerance = Math.max(d.arriveTolerance, r.getFloat("restartTolerance", d.restartTolerance));
        d.stickiness = Math.max(0f, r.getFloat("stickiness", d.stickiness));
        d.leaveDuration = Math.max(.1f, r.getFloat("leaveDuration", d.leaveDuration));
        d.fastLeaveDuration = Math.max(.1f, r.getFloat("fastLeaveDuration", d.fastLeaveDuration));
        d.heavyHoldTime = Math.max(0f, r.getFloat("heavyHoldTime", d.heavyHoldTime));
        d.launchDistance = Math.max(1f, r.getFloat("launchDistance", d.launchDistance));
        d.leapMaxStartDistance = Math.max(d.launchDistance, r.getFloat("leapMaxStartDistance", d.leapMaxStartDistance));
        d.leapCooldown = Math.max(.5f, r.getFloat("leapCooldown", d.leapCooldown));
        d.leapSpeedCap = Math.max(4f, r.getFloat("leapSpeedCap", d.leapSpeedCap));
        d.crossingPenalty = Math.max(0f, r.getFloat("crossingPenalty", d.crossingPenalty));
        return d;
    }
}
