package com.cursedarchie.platformer.actors.enemies.states.motion;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;

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
