package com.cursedarchie.platformer.actors.hero.states.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroState;
import com.cursedarchie.platformer.combat.AttackDefinition;
import com.cursedarchie.platformer.combat.AttackRhythmConfig;
import com.cursedarchie.platformer.combat.AttackRuntime;
import com.cursedarchie.platformer.physics.CombatHit;

/** Gameplay authority for one hero strike; rendering only reads its runtime. */
public class AttackState implements HeroState {
    public static final int COMBO_FIRST = 1;
    public static final int COMBO_SECOND = 2;
    public static final int COMBO_FINISHER = 3;
    public static final int COUNTER_ATTACK = 4;
    public static final int JUMP_ATTACK = 5;
    public enum Phase { STARTUP, ACTIVE, RECOVERY, COMBO_READY }
    private final int comboStep;
    private final Rectangle attackBounds = new Rectangle();
    private final Rectangle hurtBounds = new Rectangle();
    private AttackRuntime runtime;
    private boolean lunged;
    public AttackState() { this(1); }
    public AttackState(int comboStep) { this.comboStep = comboStep; }
    public int getComboStep() { return comboStep; }
    public AttackRuntime getRuntime() { return runtime; }
    public AttackDefinition getDefinition() { return runtime.definition(); }
    public boolean isFacingLeft() { return runtime != null && runtime.facesLeft(); }
    public float getStartup() { return runtime.definition().startupDuration; }
    public float getActiveEnd() { return runtime.definition().activeEnd(); }
    public float getDuration(Hero hero) { return runtime.duration(); }
    public Phase getPhase(Hero hero) {
        if (chainsToNext() && runtime.isComboReady()) return Phase.COMBO_READY;
        return runtime.phase() == AttackRuntime.Phase.STARTUP ? Phase.STARTUP :
            runtime.phase() == AttackRuntime.Phase.ACTIVE ? Phase.ACTIVE : Phase.RECOVERY;
    }
    public boolean isInRecovery(Hero hero) { return runtime.phase() == AttackRuntime.Phase.RECOVERY; }
    public boolean isFinisherRecovery(Hero hero) {
        return comboStep == COMBO_FINISHER && runtime.phase() == AttackRuntime.Phase.RECOVERY;
    }
    public boolean canQueueFollowUp(Hero hero) {
        return chainsToNext() && runtime.isInComboWindow();
    }
    /** The first two light strikes and the jump attack open windows; the third strike is the finisher. */
    public boolean chainsToNext() {
        return comboStep == COMBO_FIRST || comboStep == COMBO_SECOND || comboStep == JUMP_ATTACK;
    }
    /** Only chainable light strikes classify follow-up timing. */
    public boolean usesRhythm() { return chainsToNext(); }

    @Override public void enter(Hero hero) {
        lunged = false;
        if (comboStep == COMBO_FIRST) hero.setLightTarget(null); // a new chain picks its opponent afresh
        hero.setLeftAttackActive(false);
        hero.setRightAttackActive(false);
        runtime = new AttackRuntime(hero.attackDefinition(comboStep), hero.isFacingLeft());
        Gdx.app.log("HERO", "Start Attacking " + runtime.definition().id);
    }

    @Override public void update(Hero hero, float delta) {
        runtime.advance(delta);
        hero.setCombatStateTime(runtime.elapsed());
        boolean active = runtime.isActive();
        hero.setLeftAttackActive(active && runtime.facesLeft());
        hero.setRightAttackActive(active && !runtime.facesLeft());
        if (active && !lunged && comboStep != JUMP_ATTACK) {
            hero.getBody().setLinearVelocity(runtime.direction() *
                    (comboStep == COMBO_FINISHER ? 7.5f :
                        comboStep == COMBO_SECOND ? 6.5f : 4f),
                hero.getBody().getLinearVelocity().y);
            lunged = true;
        }
        if (active) for (Actor target : strikeTargets(hero)) {
            if (!runtime.canHit(hero, target, attackBounds, hurtBounds)) continue;
            if (comboStep == COMBO_FINISHER && target instanceof Enemy &&
                blockedByLevel(hero, target.getPosition().x, target.getPosition().y)) continue;
            AttackDefinition attack = runtime.definition();
            AttackRhythmConfig rhythm = AttackRhythmConfig.get();
            boolean hurried = runtime.isHurried();
            float damage = hero.getDamage() * attack.damageMultiplier *
                hero.meleeDamageMultiplier(comboStep) * (hurried ? rhythm.hurriedDamageMultiplier : 1f);
            float knockback = (attack.knockback + hero.meleeRecoilBonus()) *
                (hurried ? rhythm.hurriedKnockbackMultiplier : 1f) *
                (comboStep == COMBO_SECOND ? rhythm.secondStrikeKnockbackMultiplier : 1f);
            float stagger = attack.staggerDuration * (hurried ? rhythm.hurriedStaggerMultiplier : 1f);
            CombatHit hit = new CombatHit(attack.type, damage, hero, target,
                runtime.direction(), attack.stabilityDamage, knockback, stagger, attack.blockable);
            float healthBefore = target.getHealth();
            boolean applied = target instanceof Enemy ? ((Enemy) target).takeMeleeHit(hit) : true;
            if (!(target instanceof Enemy)) target.takeDamage(damage);
            if (!applied || target.getHealth() == healthBefore && !(target instanceof Enemy)) continue;
            hero.emitCombatHit(hit);
            runtime.recordHit(target);
            Gdx.app.log("HERO", "dealt damage to " + target.getClass().getSimpleName());
        }
        if (runtime.isComplete() && !runtime.hitAnyTarget()) hero.markWhiff();
        if (runtime.isComplete()) hero.getCombatStateMachine().changeState(new CombatIdleState());
    }

    private final java.util.List<Actor> strikeList = new java.util.ArrayList<>();

    /**
     * Light strikes 1 and 2 hit exactly one opponent, no splash: the enemy already locked on to by the previous
     * strike if it is still in the zone, otherwise the nearest one in the zone (which then becomes the lock). The
     * finisher and the jump attack keep hitting everything in their zone.
     */
    private Iterable<Actor> strikeTargets(Hero hero) {
        if (comboStep != COMBO_FIRST && comboStep != COMBO_SECOND) return hero.getAttackTargets();
        strikeList.clear();
        if (runtime.hitAnyTarget()) return strikeList;   // this swing has already struck its one target
        Actor locked = hero.getLightTarget();
        if (locked != null && locked.isAlive() && hero.getAttackTargets().contains(locked) &&
            runtime.canHit(hero, locked, attackBounds, hurtBounds)) {
            strikeList.add(locked);
            return strikeList;
        }
        Actor nearest = null;
        float best = Float.MAX_VALUE;
        for (Actor candidate : hero.getAttackTargets()) {
            if (!candidate.isAlive() || !runtime.canHit(hero, candidate, attackBounds, hurtBounds)) continue;
            float distance = Math.abs(candidate.getPosition().x - hero.getPosition().x);
            if (distance < best) { best = distance; nearest = candidate; }
        }
        if (nearest != null) {
            hero.setLightTarget(nearest);
            strikeList.add(nearest);
        }
        return strikeList;
    }

    private boolean blockedByLevel(Hero hero, float tx, float ty) {
        final boolean[] blocked = {false};
        hero.getBody().getWorld().rayCast(new RayCastCallback() {
            @Override public float reportRayFixture(com.badlogic.gdx.physics.box2d.Fixture fixture, com.badlogic.gdx.math.Vector2 point,
                                                    com.badlogic.gdx.math.Vector2 normal, float fraction) {
                if (!fixture.isSensor() && "level".equals(fixture.getBody().getUserData())) {
                    blocked[0] = true;
                    return 0f;
                }
                return -1f;
            }
        }, hero.getPosition(), new com.badlogic.gdx.math.Vector2(tx, ty));
        return blocked[0];
    }

    @Override public void exit(Hero hero) {
        hero.setAttackHandled(false);
        hero.setLeftAttackActive(false);
        hero.setRightAttackActive(false);
        Gdx.app.log("HERO", "Finished Attacking");
    }
}
