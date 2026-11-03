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
    public final Actor source;

    public CombatHit(Type type, float damage, float sourceX, float stabilityDamage,
                     float recoil, Actor source) {
        this.type = type;
        this.damage = damage;
        this.sourceX = sourceX;
        this.stabilityDamage = stabilityDamage;
        this.recoil = recoil;
        this.source = source;
    }
}
