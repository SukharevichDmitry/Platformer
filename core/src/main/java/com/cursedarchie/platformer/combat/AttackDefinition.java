package com.cursedarchie.platformer.combat;

import com.cursedarchie.platformer.physics.CombatHit;

/**
 * Immutable timing, hitbox and impact data for one melee attack.
 *
 * <p>The attack is {@code startup -> active -> recovery}; only the active phase can deal damage.
 * {@code range} is the farthest reach from the attacker's centre, so the hitbox centre sits at
 * {@code range - hitboxWidth / 2} in front of the attacker.
 */
public final class AttackDefinition {
    public final String id;
    public final CombatHit.Type type;
    public final float startupDuration;
    public final float activeDuration;
    public final float recoveryDuration;
    public final float range;
    public final float hitboxWidth;
    public final float hitboxHeight;
    public final float offsetY;
    public final float damageMultiplier;
    public final float knockback;
    public final float staggerDuration;
    public final float stabilityDamage;
    public final boolean blockable;
    /** Time since the attack began after which a follow-up input is accepted (during recovery). */
    public final float nextInputTime;
    /** Total length of the attack; the state ends once this has elapsed. */
    public final float duration;
    /** Extra recovery added when the attack finishes without striking any target (0 when unused). */
    public final float missRecovery;

    public AttackDefinition(String id, CombatHit.Type type, float startupDuration, float activeDuration,
                            float recoveryDuration, float range, float hitboxWidth, float hitboxHeight,
                            float offsetY, float damageMultiplier, float knockback, float staggerDuration,
                            float stabilityDamage, boolean blockable, float nextInputTime, float duration) {
        this(id, type, startupDuration, activeDuration, recoveryDuration, range, hitboxWidth, hitboxHeight,
            offsetY, damageMultiplier, knockback, staggerDuration, stabilityDamage, blockable,
            nextInputTime, duration, 0f);
    }

    public AttackDefinition(String id, CombatHit.Type type, float startupDuration, float activeDuration,
                            float recoveryDuration, float range, float hitboxWidth, float hitboxHeight,
                            float offsetY, float damageMultiplier, float knockback, float staggerDuration,
                            float stabilityDamage, boolean blockable, float nextInputTime, float duration,
                            float missRecovery) {
        this.id = id;
        this.type = type;
        this.startupDuration = startupDuration;
        this.activeDuration = activeDuration;
        this.recoveryDuration = recoveryDuration;
        this.range = range;
        this.hitboxWidth = hitboxWidth;
        this.hitboxHeight = hitboxHeight;
        this.offsetY = offsetY;
        this.damageMultiplier = damageMultiplier;
        this.knockback = knockback;
        this.staggerDuration = staggerDuration;
        this.stabilityDamage = stabilityDamage;
        this.blockable = blockable;
        this.nextInputTime = nextInputTime;
        this.duration = duration;
        this.missRecovery = missRecovery;
    }

    /** Time since the start at which the active phase ends and recovery begins. */
    public float activeEnd() { return startupDuration + activeDuration; }
}
