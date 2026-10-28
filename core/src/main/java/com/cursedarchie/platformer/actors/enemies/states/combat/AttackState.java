package com.cursedarchie.platformer.actors.enemies.states.combat;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;

import java.util.HashSet;
import java.util.Set;

public class AttackState implements EnemyState {
    private final Set<Actor> hitTargets = new HashSet<>();

    @Override
    public void enter(Enemy enemy) {
        enemy.clearAttackRequest();
        hitTargets.clear();
        enemy.setLeftAttackActive(false);
        enemy.setRightAttackActive(false);
        Gdx.app.log("INFO", "Start Attacking");
    }

    @Override
    public void update(Enemy enemy, float delta) {
        enemy.setCombatStateTime(enemy.getCombatStateTime() + delta);

        boolean activeFrame = enemy.getCombatStateTime() >= enemy.getAttackStartup() &&
            enemy.getCombatStateTime() < enemy.getAttackActiveEnd();
        enemy.setLeftAttackActive(activeFrame && enemy.isFacingLeft());
        enemy.setRightAttackActive(activeFrame && !enemy.isFacingLeft());

        if (activeFrame) {

            for (Actor target : enemy.getAttackTargets()) {
                if (hitTargets.contains(target) || !enemy.isTargetInFacingAttack(target)) continue;

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

        if (enemy.getCombatStateTime() >= enemy.getAttackDuration()) {
            enemy.getCombatStateMachine().changeState(new CombatIdleState());
        }
    }

    @Override
    public void exit (Enemy enemy) {
        enemy.setAttackHandled(false);
        enemy.setLeftAttackActive(false);
        enemy.setRightAttackActive(false);
        enemy.beginAttackCooldown();
        Gdx.app.log("INFO", "Finished Attacking");
    }
}
