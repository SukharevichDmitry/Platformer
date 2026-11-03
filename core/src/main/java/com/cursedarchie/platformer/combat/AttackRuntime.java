package com.cursedarchie.platformer.combat;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectSet;
import com.cursedarchie.platformer.actors.Actor;

/** Running state of one attack: elapsed time, locked direction and the targets already struck. */
public final class AttackRuntime {
    public enum Phase { STARTUP, ACTIVE, RECOVERY }

    private static final float EPSILON = 1e-4f;

    private final AttackDefinition definition;
    private final boolean facesLeft;
    private final ObjectSet<Actor> struck = new ObjectSet<>();
    private float elapsed;

    public AttackRuntime(AttackDefinition definition, boolean facesLeft) {
        this.definition = definition;
        this.facesLeft = facesLeft;
    }

    public AttackDefinition definition() { return definition; }
    public boolean facesLeft() { return facesLeft; }
    /** Direction locked when the attack began: -1 left, +1 right. */
    public float direction() { return facesLeft ? -1f : 1f; }
    public float elapsed() { return elapsed; }
    /** Total length of this swing: a swing that struck nothing pays the definition's miss recovery. */
    public float duration() { return definition.duration + (struck.size > 0 ? 0f : definition.missRecovery); }

    public void advance(float delta) { elapsed += delta; }

    public Phase phase() {
        if (elapsed + EPSILON < definition.startupDuration) return Phase.STARTUP;
        if (elapsed + EPSILON < definition.activeEnd()) return Phase.ACTIVE;
        return Phase.RECOVERY;
    }

    public boolean isActive() { return phase() == Phase.ACTIVE; }
    public boolean isComplete() { return elapsed + EPSILON >= duration(); }

    /** A follow-up press is accepted only during recovery, once the combo window has opened. */
    public boolean acceptsNextInput() {
        return phase() == Phase.RECOVERY && elapsed + EPSILON >= definition.nextInputTime;
    }

    /** World-space hitbox in front of {@code origin}, on the side the attack is facing. */
    public Rectangle hitbox(Vector2 origin, Rectangle out) {
        float centreX = origin.x + direction() * (definition.range - definition.hitboxWidth / 2f);
        return out.set(centreX - definition.hitboxWidth / 2f,
            origin.y + definition.offsetY - definition.hitboxHeight / 2f,
            definition.hitboxWidth, definition.hitboxHeight);
    }

    /** True when the attack is overlapping the target's hurtbox and has not already struck it. */
    public boolean canHit(Actor attacker, Actor target, Rectangle attackBounds, Rectangle hurtBounds) {
        if (attacker == null || target == null || struck.contains(target)) return false;
        hitbox(attacker.getPosition(), attackBounds);
        target.getHurtboxBounds(hurtBounds);
        return attackBounds.overlaps(hurtBounds);
    }

    public void recordHit(Actor target) { struck.add(target); }
    public boolean hitAnyTarget() { return struck.size > 0; }
}
