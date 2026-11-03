package com.cursedarchie.platformer.actors.enemies.states.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.combat.AttackDefinition;
import com.cursedarchie.platformer.combat.AttackRuntime;
import com.cursedarchie.platformer.physics.CombatHit;

import java.util.Set;

/** Shared phase-based melee authority for every enemy attack variant. */
public class AttackState implements EnemyState {
    private final boolean lunge;
    private final boolean area;
    private final Rectangle attackBounds = new Rectangle();
    private final Rectangle hurtBounds = new Rectangle();
    private AttackRuntime runtime;
    private boolean lunged;
    private boolean strikeCueSent;

    public AttackState() { this(false, false); }
    public AttackState(boolean lunge) { this(lunge, false); }
    public AttackState(boolean lunge, boolean area) { this.lunge = lunge; this.area = area; }
    public boolean isLunge() { return lunge; }
    public boolean isArea() { return area; }
    public boolean isFacingLeft() { return runtime != null && runtime.facesLeft(); }
    public AttackRuntime getRuntime() { return runtime; }
    public AttackDefinition getDefinition() { return runtime.definition(); }
    public float startup(Enemy enemy) { return runtime.definition().startupDuration; }
    public float activeEnd(Enemy enemy) { return runtime.definition().activeEnd(); }

    @Override public void enter(Enemy enemy) {
        enemy.clearAttackRequest();
        runtime = new AttackRuntime(enemy.attackDefinition(lunge, area), enemy.isFacingLeft());
        lunged = false;
        strikeCueSent = false;
        // Presentation only: the archer's blade rasp telegraphs the melee break from its ranged stance.
        if ("scavenger".equals(enemy.getArchetypeId())) enemy.emitCue(Enemy.Cue.MELEE_WINDUP);
        enemy.setLeftAttackActive(false);
        enemy.setRightAttackActive(false);
        Gdx.app.log("INFO", "Start Attacking " + runtime.definition().id);
    }

    @Override public void update(Enemy enemy, float delta) {
        runtime.advance(delta);
        enemy.setCombatStateTime(runtime.elapsed());
        boolean active = runtime.isActive();
        if (active && !strikeCueSent) {
            strikeCueSent = true;
            if ("scavenger".equals(enemy.getArchetypeId())) enemy.emitCue(Enemy.Cue.MELEE_STRIKE);
        }
        enemy.setLeftAttackActive(active && runtime.facesLeft());
        enemy.setRightAttackActive(active && !runtime.facesLeft());
        if (active && lunge && !lunged) {
            enemy.getBody().setLinearVelocity(runtime.direction() * 11f,
                enemy.getBody().getLinearVelocity().y);
            lunged = true;
        }
        if (active) {
            Set<Actor> candidates = area ? enemy.getVisibleActors() : enemy.getAttackTargets();
            for (Actor target : candidates) {
                if (!(target instanceof Hero) ||
                    !runtime.canHit(enemy, target, attackBounds, hurtBounds)) continue;
                AttackDefinition attack = runtime.definition();
                CombatHit hit = new CombatHit(attack.type,
                    enemy.getDamage() * attack.damageMultiplier, enemy, target,
                    runtime.direction(), attack.stabilityDamage, attack.knockback,
                    attack.staggerDuration, attack.blockable);
                if (!((Hero) target).receiveHit(hit)) continue;
                enemy.emitCombatHit(hit);
                runtime.recordHit(target);
                Gdx.app.log("COMBAT", enemy.getClass().getSimpleName() +
                    " dealt damage to " + target.getClass().getSimpleName());
            }
        }
        if (runtime.isComplete()) enemy.getCombatStateMachine().changeState(new CombatIdleState());
    }

    @Override public void exit(Enemy enemy) {
        enemy.setAttackHandled(false);
        enemy.setLeftAttackActive(false);
        enemy.setRightAttackActive(false);
        enemy.beginAttackCooldown(lunge && !runtime.hitAnyTarget() ? .4f : 0f);
        Gdx.app.log("INFO", "Finished Attacking");
    }
}
