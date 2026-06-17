package com.cursedarchie.platformer.actors.enemies.states.combat;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;

public class AttackState implements EnemyState {
    @Override
    public void enter(Enemy enemy) {
        enemy.clearAttackRequest();

        enemy.setLeftAttackActive(enemy.isFacingLeft());
        enemy.setRightAttackActive(!enemy.isFacingLeft());
        Gdx.app.log("INFO", "Start Attacking");
    }

    @Override
    public void update(Enemy enemy, float delta) {
        enemy.setCombatStateTime(enemy.getCombatStateTime() + delta);

        boolean activeFrame =
            !enemy.isAttackHandled() && enemy.getCombatStateTime() >= enemy.getAttackDuration() * 0.5f;

        if (activeFrame) {

            for (Actor target : enemy.getAttackTargets()) {
                boolean inFront =
                    (enemy.isFacingLeft() && target.getPosition().x < enemy.getPosition().x) ||
                        (!enemy.isFacingLeft() && target.getPosition().x > enemy.getPosition().x);
                if (!inFront) continue;

                target.takeDamage(enemy.getDamage());
                Gdx.app.log(
                    "COMBAT",
                    enemy.getClass().getSimpleName()
                        + " dealt damage to "
                        + target.getClass().getSimpleName()
                );
            }

            enemy.setAttackHandled(true);
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
        Gdx.app.log("INFO", "Finished Attacking");
    }
}
