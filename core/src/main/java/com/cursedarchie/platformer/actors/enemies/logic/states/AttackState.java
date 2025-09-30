package com.cursedarchie.platformer.actors.enemies.logic.states;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.Enemy;
import com.cursedarchie.platformer.actors.enemies.logic.EnemyState;

public class AttackState implements EnemyState {
    @Override
    public void enter(Enemy enemy) {
        enemy.stopMoving();
        enemy.setAttackTime(0f);
        enemy.setDamageDealt(false);
        Gdx.app.log("INFO", "Start Attacking");
    }

    @Override
    public void update (Enemy enemy, float delta) {
        enemy.setAttackTime(enemy.getAttackTime() + delta);
        if (enemy.getAttackTime() >= enemy.getAttackDuration()) {
            enemy.getStateMachine().changeState(new ChaseState());
        }
        enemy.setCanDealDamage(
            !enemy.isDamageDealt() &&
            enemy.getAttackTime() >= enemy.getAttackDuration() / 2f &&
            enemy.isCanAttackHero());
    }

    @Override
    public void exit (Enemy enemy) {
        Gdx.app.log("INFO", "Finished Attacking");

    }
}
