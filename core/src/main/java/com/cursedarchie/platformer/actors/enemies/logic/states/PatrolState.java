package com.cursedarchie.platformer.actors.enemies.logic.states;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.Enemy;
import com.cursedarchie.platformer.actors.enemies.logic.EnemyState;

public class PatrolState implements EnemyState {
    @Override
    public void enter(Enemy enemy) {
        Gdx.app.log("Enemy", "Started Patroling");
    }

    @Override
    public void update (Enemy enemy, float delta) {

    }

    @Override
    public void exit (Enemy enemy) {
        Gdx.app.log("Enemy", "Finished Patroling");
    }
}
