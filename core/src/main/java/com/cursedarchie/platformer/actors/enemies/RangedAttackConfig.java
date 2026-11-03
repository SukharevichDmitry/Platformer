package com.cursedarchie.platformer.actors.enemies;

/** Timing and projectile settings for each ranged attack the scavenger can choose. */
public final class RangedAttackConfig {
    public enum Type { NORMAL, CHARGED, VOLLEY }

    public static final RangedAttackConfig NORMAL = new RangedAttackConfig(Type.NORMAL,
        .7f, 2.2f, 7f, 15f, .5f, 0f, .08f, 1, 0f);
    public static final RangedAttackConfig CHARGED = new RangedAttackConfig(Type.CHARGED,
        1.05f, 2.2f, 7f, 18f, 1f, 5f, .2f, 1, 0f);
    public static final RangedAttackConfig VOLLEY = new RangedAttackConfig(Type.VOLLEY,
        1.2f, 2.2f, 7f, 15f, .5f, 0f, .08f, 3, .22f);

    public final Type type;
    /** Wind-up before the shot is released. */
    public final float startup;
    /** Delay before the next ranged attack may start. */
    public final float cooldown;
    public final float maxRange;
    public final float speed;
    public final float damage;
    public final float knockback;
    public final float stagger;
    public final int projectileCount;
    /** Angle between neighbouring arrows of a volley, in radians. */
    public final float spreadRadians;

    private RangedAttackConfig(Type type, float startup, float cooldown, float maxRange, float speed,
                               float damage, float knockback, float stagger, int projectileCount,
                               float spreadRadians) {
        this.type = type;
        this.startup = startup;
        this.cooldown = cooldown;
        this.maxRange = maxRange;
        this.speed = speed;
        this.damage = damage;
        this.knockback = knockback;
        this.stagger = stagger;
        this.projectileCount = projectileCount;
        this.spreadRadians = spreadRadians;
    }
}
