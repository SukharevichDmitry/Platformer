package com.cursedarchie.platformer.actors.enemies.states.motion;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;

public class MotionIdleState implements EnemyState {

    @Override
    public void enter(Enemy enemy) {
        Gdx.app.log("Enemy", "Motion Idling Started");
    }

    @Override
    public void update (Enemy enemy, float delta) {
        enemy.moveStop();
    }

    @Override
    public void exit (Enemy enemy) {
        Gdx.app.log("Enemy", "Motion Idling Finished");
    }
}
