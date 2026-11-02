package com.cursedarchie.platformer.actors.enemies.states.combat;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;

import java.util.HashSet;
import java.util.Set;

public class AttackState implements EnemyState {
    private final Set<Actor> hitTargets = new HashSet<>();
    private final boolean lunge;
    private boolean facingLeft;

    public AttackState() { this(false); }
    public AttackState(boolean lunge) { this.lunge = lunge; }
    public boolean isLunge() { return lunge; }
    public boolean isFacingLeft() { return facingLeft; }
    public float startup(Enemy enemy) { return lunge ? .48f : enemy.getAttackStartup(); }
    public float activeEnd(Enemy enemy) { return lunge ? .67f : enemy.getAttackActiveEnd(); }

    @Override
    public void enter(Enemy enemy) {
        enemy.clearAttackRequest();
        hitTargets.clear();
        facingLeft = enemy.isFacingLeft();
        enemy.setLeftAttackActive(false);
        enemy.setRightAttackActive(false);
        Gdx.app.log("INFO", "Start Attacking");
    }

    @Override
    public void update(Enemy enemy, float delta) {
        enemy.setCombatStateTime(enemy.getCombatStateTime() + delta);

        boolean activeFrame = enemy.getCombatStateTime() >= startup(enemy) &&
            enemy.getCombatStateTime() < activeEnd(enemy);
        enemy.setLeftAttackActive(activeFrame && facingLeft);
        enemy.setRightAttackActive(activeFrame && !facingLeft);

        if (activeFrame) {
            if (lunge) {
                enemy.getBody().setLinearVelocity(facingLeft ? -11f : 11f,
                    enemy.getBody().getLinearVelocity().y);
            }

            for (Actor target : enemy.getAttackTargets()) {
                if (hitTargets.contains(target) || !enemy.isTargetInAttack(target, facingLeft)) continue;

                target.takeDamage(enemy.getDamage());
                hitTargets.add(target);
                Gdx.app.log(
                    "COMBAT",
                    enemy.getClass().getSimpleName()
                        + " dealt damage to "
                        + target.getClass().getSimpleName()
                );
            }

        }

        if (enemy.getCombatStateTime() >= (lunge ? .88f : enemy.getAttackDuration())) {
            enemy.getCombatStateMachine().changeState(new CombatIdleState());
        }
    }

    @Override
    public void exit (Enemy enemy) {
        enemy.setAttackHandled(false);
        enemy.setLeftAttackActive(false);
        enemy.setRightAttackActive(false);
        enemy.beginAttackCooldown(lunge && hitTargets.isEmpty() ? .4f : 0f);
        Gdx.app.log("INFO", "Finished Attacking");
    }
}
