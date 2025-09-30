package com.cursedarchie.platformer.actors.enemies.logic.states;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.NewEnemy;
import com.cursedarchie.platformer.actors.enemies.logic.EnemyState;

public class IdleState implements EnemyState {

    @Override
    public void enter(NewEnemy enemy) {
        Gdx.app.log("Enemy", "Started Idling");
    }

    @Override
    public void update (NewEnemy enemy, float delta) {
        if (enemy.isCanSeeHero()) {
            enemy.getStateMachine().changeState(new ChaseState());
            return;
        }
    }

    @Override
    public void exit (NewEnemy enemy) {
        Gdx.app.log("Enemy", "Finished Idling");
    }
}
