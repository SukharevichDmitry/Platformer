package com.cursedarchie.platformer.actors.enemies.group;

import com.cursedarchie.platformer.actors.enemies.Enemy;

/** How an enemy takes part in the formation. */
public enum CombatRole {
    STANDARD, FAST, HEAVY, RANGED, SUPPORT;

    public static CombatRole of(Enemy enemy) {
        String id = enemy.getArchetypeId();
        if ("scavenger".equals(id)) return RANGED;
        if ("overseer".equals(id)) return SUPPORT;
        if ("infiltrator".equals(id)) return FAST;
        if ("heavy_hunter".equals(id) || "boss".equals(id)) return HEAVY;
        return STANDARD;
    }

    public boolean isMelee() { return this == STANDARD || this == FAST || this == HEAVY; }
}
