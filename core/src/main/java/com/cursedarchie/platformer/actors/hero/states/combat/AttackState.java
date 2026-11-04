package com.cursedarchie.platformer.actors.hero.states.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroState;
import com.cursedarchie.platformer.combat.AttackDefinition;
import com.cursedarchie.platformer.combat.AttackRuntime;
import com.cursedarchie.platformer.physics.CombatHit;

/** Gameplay authority for one hero strike; rendering only reads its runtime. */
public class AttackState implements HeroState {
    public enum Phase { STARTUP, ACTIVE, RECOVERY }
    private final boolean heavy;
    private final int comboStep;
    private final Rectangle attackBounds = new Rectangle();
    private final Rectangle hurtBounds = new Rectangle();
    private AttackRuntime runtime;
    private boolean lunged;

    public AttackState() { this(false, 1); }
    public AttackState(boolean heavy, int comboStep) { this.heavy = heavy; this.comboStep = comboStep; }
    public boolean isHeavy() { return heavy; }
    public int getComboStep() { return comboStep; }
    public AttackRuntime getRuntime() { return runtime; }
    public AttackDefinition getDefinition() { return runtime.definition(); }
    public boolean isFacingLeft() { return runtime != null && runtime.facesLeft(); }
    public float getStartup() { return runtime.definition().startupDuration; }
    public float getActiveEnd() { return runtime.definition().activeEnd(); }
    public float getDuration(Hero hero) { return runtime.duration(); }
    public Phase getPhase(Hero hero) {
        return runtime.phase() == AttackRuntime.Phase.STARTUP ? Phase.STARTUP :
            runtime.phase() == AttackRuntime.Phase.ACTIVE ? Phase.ACTIVE : Phase.RECOVERY;
    }
    public boolean isInRecovery(Hero hero) { return runtime.phase() == AttackRuntime.Phase.RECOVERY; }
    public boolean canQueueFollowUp(Hero hero) {
        return !heavy && comboStep == 1 && runtime.acceptsNextInput();
    }

    @Override public void enter(Hero hero) {
        runtime = new AttackRuntime(hero.attackDefinition(heavy, comboStep), hero.isFacingLeft());
        lunged = false;
        hero.setLeftAttackActive(false);
        hero.setRightAttackActive(false);
        Gdx.app.log("HERO", "Start Attacking " + runtime.definition().id);
    }

    @Override public void update(Hero hero, float delta) {
        runtime.advance(delta);
        hero.setCombatStateTime(runtime.elapsed());
        boolean active = runtime.isActive();
        hero.setLeftAttackActive(active && runtime.facesLeft());
        hero.setRightAttackActive(active && !runtime.facesLeft());
        if (active && !lunged && comboStep != 4) {
            hero.getBody().setLinearVelocity(runtime.direction() *
                    (heavy ? 6f : comboStep == 2 ? 6.5f : 4f),
                hero.getBody().getLinearVelocity().y);
            lunged = true;
        }
        if (active) for (Actor target : hero.getAttackTargets()) {
            if (!runtime.canHit(hero, target, attackBounds, hurtBounds)) continue;
            AttackDefinition attack = runtime.definition();
            float damage = hero.getDamage() * attack.damageMultiplier *
                hero.meleeDamageMultiplier(heavy, comboStep);
            CombatHit hit = new CombatHit(attack.type, damage, hero, target,
                runtime.direction(), attack.stabilityDamage,
                attack.knockback + hero.meleeRecoilBonus(), attack.staggerDuration,
                attack.blockable);
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

    @Override public void exit(Hero hero) {
        hero.setAttackHandled(false);
        hero.setLeftAttackActive(false);
        hero.setRightAttackActive(false);
        Gdx.app.log("HERO", "Finished Attacking");
    }
}
