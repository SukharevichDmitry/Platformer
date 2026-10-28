package com.cursedarchie.platformer.actors.enemies.states.motion;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;

public class ChaseState implements EnemyState {
    @Override
    public void enter(Enemy enemy) {
        Gdx.app.log("Enemy", "Started Chasing");
    }

    @Override
    public void update(Enemy enemy, float delta) {
        Vector2 target = enemy.getHeroLastKnownPos();
        if (target == null) {
            return;
        }

        if (target.x < enemy.getPosition().x) enemy.moveLeft();
        else enemy.moveRight();
    }

    @Override
    public void exit (Enemy enemy) {
        enemy.moveStop();
        Gdx.app.log("Enemy", "Finished Chasing");
    }
}
