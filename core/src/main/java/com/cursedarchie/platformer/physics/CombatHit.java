package com.cursedarchie.platformer.physics;

import com.cursedarchie.platformer.actors.Actor;

/** Damage source and force are explicit so guards never infer attack type from enemy names. */
public final class CombatHit {
    public enum Type { MELEE_LIGHT, MELEE_HEAVY, PROJECTILE }
    public final Type type;
    public final float damage;
    public final float sourceX;
    public final float stabilityDamage;
    public final float recoil;
    public final float direction;
    public final float staggerDuration;
    public final boolean blockable;
    public final Actor source;
    public final Actor target;

    public CombatHit(Type type, float damage, float sourceX, float stabilityDamage,
                     float recoil, Actor source) {
        this.type = type;
        this.damage = damage;
        this.sourceX = sourceX;
        this.stabilityDamage = stabilityDamage;
        this.recoil = recoil;
        this.direction = 0f;
        this.staggerDuration = type == Type.MELEE_HEAVY ? .24f : type == Type.MELEE_LIGHT ? .14f : .08f;
        this.blockable = type != Type.PROJECTILE;
        this.source = source;
        this.target = null;
    }

    public CombatHit(Type type, float damage, Actor source, Actor target, float direction,
                     float stabilityDamage, float recoil, float staggerDuration, boolean blockable) {
        if (target == null) throw new IllegalArgumentException("Combat hit target is required");
        this.type = type;
        this.damage = damage;
        this.source = source;
        this.target = target;
        this.sourceX = source == null ? target.getPosition().x - direction : source.getPosition().x;
        this.direction = Math.signum(direction);
        this.stabilityDamage = stabilityDamage;
        this.recoil = recoil;
        this.staggerDuration = Math.max(0f, staggerDuration);
        this.blockable = blockable;
    }
}
