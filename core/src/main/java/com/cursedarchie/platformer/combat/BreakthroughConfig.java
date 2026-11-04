package com.cursedarchie.platformer.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Tunable numbers of the hero's empowered "breakthrough" dash, loaded from {@code config/breakthrough.json}.
 * Any missing key or file falls back to the defaults below.
 */
public final class BreakthroughConfig {
    public float chargeTime = .75f;
    /** Maximum dash distance; the real one is min(this, free space ahead). Below minDistance the dash has no travel. */
    public float dashDistance = 3f;
    public float minDistance = .5f;
    public float dashDuration = .25f;
    /** Fixed dash speed; 0 derives it from distance / duration. */
    public float dashSpeed = 0f;
    public float recoveryTime = .7f;
    public float damageMultiplier = 2f;
    public float knockback = 10f;
    /** A hit with at least this stagger (or any heavy hit) interrupts the charge. */
    public float interruptStagger = .3f;
    public float cooldown = 1.6f;
    public float firstHitMultiplier = 1f;
    public float followHitMultiplier = .5f;
    public float followKnockbackMultiplier = .5f;
    public float followStaggerMultiplier = .5f;
    /** Dash speed factor after striking a heavy (large) enemy: slows the dash but never cancels it. */
    public float heavyEnemySlow = .55f;

    private static BreakthroughConfig current;

    public static synchronized BreakthroughConfig get() {
        if (current == null) current = load();
        return current;
    }

    public static synchronized void set(BreakthroughConfig config) { current = config; }

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
        d.chargeTime = root.getFloat("chargeTime", d.chargeTime);
        d.minDistance = root.getFloat("minDistance", d.minDistance);
        d.dashSpeed = root.getFloat("dashSpeed", d.dashSpeed);
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
        d.heavyEnemySlow = root.getFloat("heavyEnemySlow", d.heavyEnemySlow);
        return d;
    }
}
