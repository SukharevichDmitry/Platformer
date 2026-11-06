package com.cursedarchie.platformer.actors.enemies.group;

import com.cursedarchie.platformer.actors.enemies.Enemy;

/**
 * One position around the hero. Slots hang off the hero (side * offset) and move with it; the manager owns them.
 * <p>In this side-scrolling game the six close-range slots of the brief become three tiers per side of the hero:
 * FRONT (nearest), SIDE (middle) and REAR (outer), left and right. Which enemy can really strike from a tier
 * depends on its own reach, so "attack capable" is decided per occupant by the manager.</p>
 */
public final class CombatSlot {
    public enum Type { MELEE, RESERVE, RANGED, SUPPORT }

    public final int id;
    public final String name;
    public final Type type;
    /** -1 = left of the hero, +1 = right. */
    public final int side;
    /** Distance from the hero's centre. */
    public final float offset;
    /** Lower is better. */
    public final int priority;

    /** World position, refreshed every step. */
    public float x, y;
    /** Ground under it and no wall between it and the hero. */
    public boolean valid;
    /** A melee slot on valid ground: an enemy of suitable reach standing here can hit the hero. */
    public boolean attackCapable;

    private Enemy owner;

    CombatSlot(int id, String name, Type type, int side, float offset, int priority) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.side = side;
        this.offset = offset;
        this.priority = priority;
    }

    public Enemy getOwner() { return owner; }
    void setOwner(Enemy owner) { this.owner = owner; }
    public boolean isOccupied() { return owner != null; }
    public boolean isAttackCapable() { return attackCapable; }
    /** The occupant is mid-attack, so the slot may not be reassigned until the attack (incl. recovery) ends. */
    public boolean isReserved() { return owner != null && (owner.isAttacking() || owner.isDashPhase()); }

    @Override public String toString() { return name; }
}
