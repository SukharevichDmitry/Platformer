package com.cursedarchie.platformer.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Tunable numbers of the hero's empowered "breakthrough" dash, loaded from {@code config/breakthrough.json}.
 * Any missing key or file falls back to the defaults below.
 */
public final class BreakthroughConfig {
    public float referenceRunSpeed = 5f;
    public float minChargeTime = .15f;
    public float maxChargeTime = .8f;
    /** Maximum dash distance; the real one is min(this, free space ahead). Below minDistance the dash has no travel. */
    public float dashDistance = 3f;
    public float minDistance = .5f;
    public float dashDuration = .18f;
    /** Short, stationary recovery after the body has already stopped at the endpoint. */
    public float recoveryTime = .1f;
    public float damageMultiplier = 2f;
    public float knockback = 10f;
    /** A hit with at least this stagger (or any heavy hit) interrupts the charge. */
    public float interruptStagger = .3f;
    public float cooldown = 1.6f;
    public float firstHitMultiplier = 1f;
    public float followHitMultiplier = .5f;
    public float followKnockbackMultiplier = .5f;
    public float followStaggerMultiplier = .5f;

    private static BreakthroughConfig current;

    public static synchronized BreakthroughConfig get() {
        if (current == null) current = load();
        return current;
    }

    public static synchronized void set(BreakthroughConfig config) { current = config; }

    /** Charge is based only on the already wall-limited route, never on the hero's current velocity. */
    public float chargeTimeForDistance(float distance) {
        float raw = Math.max(0f, distance) / Math.max(.01f, referenceRunSpeed);
        return Math.max(minChargeTime, Math.min(maxChargeTime, raw));
    }

    public static BreakthroughConfig load() {
        try {
            if (Gdx.files != null && Gdx.files.internal("config/breakthrough.json").exists())
                return parse(Gdx.files.internal("config/breakthrough.json").readString("UTF-8"));
        } catch (RuntimeException ignored) {
            // A broken file must never break combat: use the defaults.
        }
        return new BreakthroughConfig();
    }

    public static BreakthroughConfig parse(String json) {
        JsonValue root = new JsonReader().parse(json);
        BreakthroughConfig d = new BreakthroughConfig();
        JsonValue heavyDash = root.get("heavyDash");
        JsonValue timing = heavyDash != null && heavyDash.isObject() ? heavyDash : root;
        d.referenceRunSpeed = Math.max(.01f,
            timing.getFloat("referenceRunSpeed", d.referenceRunSpeed));
        d.minChargeTime = Math.max(0f, timing.getFloat("minChargeTime", d.minChargeTime));
        d.maxChargeTime = Math.max(d.minChargeTime,
            timing.getFloat("maxChargeTime", d.maxChargeTime));
        d.minDistance = root.getFloat("minDistance", d.minDistance);
        d.damageMultiplier = root.getFloat("damageMultiplier", d.damageMultiplier);
        d.knockback = root.getFloat("knockback", d.knockback);
        d.interruptStagger = root.getFloat("interruptStagger", d.interruptStagger);
        d.dashDuration = root.getFloat("dashDuration", d.dashDuration);
        d.dashDistance = root.getFloat("dashDistance", d.dashDistance);
        d.recoveryTime = root.getFloat("recoveryTime", d.recoveryTime);
        d.cooldown = root.getFloat("cooldown", d.cooldown);
        d.firstHitMultiplier = root.getFloat("firstHitMultiplier", d.firstHitMultiplier);
        d.followHitMultiplier = root.getFloat("followHitMultiplier", d.followHitMultiplier);
        d.followKnockbackMultiplier = root.getFloat("followKnockbackMultiplier", d.followKnockbackMultiplier);
        d.followStaggerMultiplier = root.getFloat("followStaggerMultiplier", d.followStaggerMultiplier);
        return d;
    }
}
