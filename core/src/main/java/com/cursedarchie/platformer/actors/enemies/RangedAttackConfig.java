package com.cursedarchie.platformer.actors.enemies;

/** Timing and projectile settings for each ranged attack the scavenger can choose. */
public final class RangedAttackConfig {
    public enum Type { NORMAL, CHARGED, VOLLEY }

    public static final RangedAttackConfig NORMAL = new RangedAttackConfig(Type.NORMAL,
        .7f, 2.2f, 7f, 15f, .5f, 0f, .08f, 1, 0f, 9f);
    public static final RangedAttackConfig CHARGED = new RangedAttackConfig(Type.CHARGED,
        1.05f, 2.2f, 7f, 18f, 1f, 5f, .2f, 1, 0f, 3.5f);
    public static final RangedAttackConfig VOLLEY = new RangedAttackConfig(Type.VOLLEY,
        1.2f, 2.2f, 7f, 15f, .5f, 0f, .08f, 3, .22f, 9f);

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
    /** Downward acceleration of the arrow (world units/s^2): the standard shot arcs, the empowered one flies flatter. */
    public final float gravity;

    public static RangedAttackConfig forMode(int mode) {
        return mode == 1 ? CHARGED : mode == 2 ? VOLLEY : NORMAL;
    }

    /**
     * Launch direction (unit vector) that makes a projectile with this config's speed and gravity pass through
     * the target; falls back to the maximum-range angle when the target is out of reach.
     */
    public com.badlogic.gdx.math.Vector2 launchDirection(float fromX, float fromY, float toX, float toY) {
        float dx = toX - fromX, dy = toY - fromY;
        float side = dx < 0f ? -1f : 1f;
        float ax = Math.max(.001f, Math.abs(dx));
        float v2 = speed * speed;
        float disc = v2 * v2 - gravity * (gravity * ax * ax + 2f * dy * v2);
        double angle = disc < 0f ? Math.PI / 4.0 :
            Math.atan((v2 - Math.sqrt(disc)) / (gravity * ax));   // the flatter of the two solutions
        return new com.badlogic.gdx.math.Vector2((float) Math.cos(angle) * side, (float) Math.sin(angle));
    }

    private RangedAttackConfig(Type type, float startup, float cooldown, float maxRange, float speed,
                               float damage, float knockback, float stagger, int projectileCount,
                               float spreadRadians, float gravity) {
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
        this.gravity = gravity;
    }
}
