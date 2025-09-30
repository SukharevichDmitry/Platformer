package com.cursedarchie.platformer.actors.enemies.logic.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.actors.NewEnemy;
import com.cursedarchie.platformer.actors.enemies.logic.EnemyState;

public class ChaseState implements EnemyState {
    @Override
    public void enter(NewEnemy enemy) {
        Gdx.app.log("Enemy", "Started Chasing");
    }

    @Override
    public void update (NewEnemy enemy, float delta) {
        if(!enemy.isCanSeeHero() && enemy.getHeroLastKnownPos() != null) {
            enemy.getStateMachine().changeState(new PatrolState());
            return;
        }

        Vector2 target = enemy.getHeroLastKnownPos();
        if (target != null) {
            if (target.x < enemy.getPosition().x) {
                enemy.moveLeft();
            } else if (target.x > enemy.getPosition().x) {
                enemy.moveRight();
            }
            if (enemy.isCanAttackHero()) {
                enemy.getStateMachine().changeState(new AttackState());
            }
        }
    }

    @Override
    public void exit (NewEnemy enemy) {
        Gdx.app.log("Enemy", "Finished Chasing");
    }
}
