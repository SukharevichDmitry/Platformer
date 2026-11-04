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
    private AttackQuality quality = AttackQuality.NORMAL;
    private InputResult inputResult = InputResult.NO_INPUT;

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
    public float duration() {
        float hurriedExtra = quality == AttackQuality.HURRIED ?
            definition.recoveryDuration * (AttackRhythmConfig.get().hurriedRecoveryMultiplier - 1f) : 0f;
        return definition.duration + hurriedExtra + (struck.size > 0 ? 0f : definition.missRecovery);
    }

    public AttackQuality quality() { return quality; }
    public boolean isHurried() { return quality == AttackQuality.HURRIED; }
    /** The one processed press of this strike; later presses never replace it. */
    public InputResult inputResult() { return inputResult; }

    /** Time at which a follow-up press starts to count as correct. */
    public float comboWindowStart() { return definition.nextInputTime; }
    public float comboWindowEnd() {
        AttackRhythmConfig cfg = AttackRhythmConfig.get();
        return Math.min(duration(), comboWindowStart() + cfg.comboWindowLength + cfg.comboWindowBonus);
    }
    /** True while a press would continue the combo and nothing has been pressed yet. */
    public boolean isInComboWindow() {
        return quality == AttackQuality.NORMAL && inputResult == InputResult.NO_INPUT &&
            elapsed + EPSILON >= comboWindowStart() && elapsed <= comboWindowEnd();
    }

    /**
     * Classifies one fresh attack press (the caller has already filtered out held-button repeats).
     * Only the first classified press counts: further presses are ignored and never queue, so spamming
     * cannot extend the combo or cut recovery short. A press so early that the swing has barely begun is
     * ignored without penalty. {@code chains} says whether a correct press continues into a second strike.
     */
    public InputResult registerAttackPress(boolean chains) {
        if (inputResult != InputResult.NO_INPUT) return InputResult.NO_INPUT;
        float start = comboWindowStart();
        if (elapsed + EPSILON < start - AttackRhythmConfig.get().earlyInputWindow) return InputResult.NO_INPUT;
        if (elapsed + EPSILON < start) {
            inputResult = InputResult.EARLY_INPUT;
            quality = AttackQuality.HURRIED;
        } else if (chains && elapsed <= comboWindowEnd()) inputResult = InputResult.COMBO_BUFFERED;
        else inputResult = InputResult.LATE_INPUT;
        return inputResult;
    }

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
        float reach = isHurried() ? AttackRhythmConfig.get().hurriedReachMultiplier : 1f;
        float range = definition.range * reach, width = definition.hitboxWidth * reach;
        float centreX = origin.x + direction() * (range - width / 2f);
        return out.set(centreX - width / 2f,
            origin.y + definition.offsetY - definition.hitboxHeight / 2f,
            width, definition.hitboxHeight);
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
