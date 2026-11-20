package com.cursedarchie.platformer.actors.enemies.group;

/** What an enemy is doing with respect to the hero's combat formation. */
public enum SlotState {
    /** Not part of the formation (patrolling, searching, or the group system is idle for it). */
    NONE,
    /** Has been given a melee attack slot and is walking (or leaping) to it. */
    SEEKING_ATTACK_SLOT,
    /** Stands in its melee attack slot: the only state from which a melee attack may start. */
    ATTACK_POSITION,
    /** No melee slot free: waits combat-ready on a surrounding position and takes a freed slot when one opens. */
    SURROUNDING,
    /** Archer holding its firing position. */
    RANGED_POSITION,
    /** Support caster holding its position behind the front line. */
    SUPPORT_POSITION,
    /** Has just attacked (or given up its slot) and steps away so another enemy can use it. */
    LEAVING_SLOT
}
