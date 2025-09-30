package com.cursedarchie.platformer.actors.enemies.logic.states;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.NewEnemy;
import com.cursedarchie.platformer.actors.enemies.logic.EnemyState;

public class PatrolState implements EnemyState {
    @Override
    public void enter(NewEnemy enemy) {
        Gdx.app.log("Enemy", "Started Patroling");
    }

    @Override
    public void update (NewEnemy enemy, float delta) {

    }

    @Override
    public void exit (NewEnemy enemy) {
        Gdx.app.log("Enemy", "Finished Patroling");
    }
}
