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
    /** The final part of the charge during which the direction is already locked. */
    public float lockTime = .17f;
    public float dashDuration = .32f;
    public float dashDistance = 3f;
    public float recoveryTime = .65f;
    public float cooldown = 1.6f;
    public float firstHitMultiplier = 1f;
    public float followHitMultiplier = .5f;
    public float followKnockbackMultiplier = .5f;
    public float followStaggerMultiplier = .5f;
    /** Dash speed factor after striking a heavy (large) enemy: slows the dash but never cancels it. */
    public float heavyEnemySlow = .55f;
    public float chargeMoveFactor = .25f;
    public float recoveryMoveFactor = .5f;

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
        d.lockTime = root.getFloat("lockTime", d.lockTime);
        d.dashDuration = root.getFloat("dashDuration", d.dashDuration);
        d.dashDistance = root.getFloat("dashDistance", d.dashDistance);
        d.recoveryTime = root.getFloat("recoveryTime", d.recoveryTime);
        d.cooldown = root.getFloat("cooldown", d.cooldown);
        d.firstHitMultiplier = root.getFloat("firstHitMultiplier", d.firstHitMultiplier);
        d.followHitMultiplier = root.getFloat("followHitMultiplier", d.followHitMultiplier);
        d.followKnockbackMultiplier = root.getFloat("followKnockbackMultiplier", d.followKnockbackMultiplier);
        d.followStaggerMultiplier = root.getFloat("followStaggerMultiplier", d.followStaggerMultiplier);
        d.heavyEnemySlow = root.getFloat("heavyEnemySlow", d.heavyEnemySlow);
        d.chargeMoveFactor = root.getFloat("chargeMoveFactor", d.chargeMoveFactor);
        d.recoveryMoveFactor = root.getFloat("recoveryMoveFactor", d.recoveryMoveFactor);
        return d;
    }
}
