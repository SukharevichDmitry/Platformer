package com.cursedarchie.platformer.actors.enemies.states.motion;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;

public class DeadState implements EnemyState {
    @Override
    public void enter(Enemy enemy) {
        Gdx.app.log("Enemy", "Started Dying");
    }

    @Override
    public void update(Enemy enemy, float delta) {
        enemy.setHealth(0);
        enemy.setDamage(0);
        enemy.setAlive(false);
    }

    @Override
    public void exit (Enemy enemy) {
        Gdx.app.log("Enemy", "Finished Dying");
    }
}
