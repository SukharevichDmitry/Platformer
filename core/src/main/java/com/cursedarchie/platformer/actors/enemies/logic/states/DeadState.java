package com.cursedarchie.platformer.actors.enemies.logic.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.actors.Enemy;
import com.cursedarchie.platformer.actors.enemies.logic.EnemyState;

public class DeadState implements EnemyState {
    @Override
    public void enter(Enemy enemy) {
        Gdx.app.log("Enemy", "Started Dying");
    }

    @Override
    public void update (Enemy enemy, float delta) {
        enemy.setHealth(0);
        enemy.setDamage(0);
        enemy.setPosition(new Vector2(99, 99));
        enemy.setAlive(false);
    }

    @Override
    public void exit (Enemy enemy) {
        Gdx.app.log("Enemy", "Finished Dying");
    }
}
