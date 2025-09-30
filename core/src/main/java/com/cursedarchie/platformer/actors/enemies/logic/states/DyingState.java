package com.cursedarchie.platformer.actors.enemies.logic.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.actors.NewEnemy;
import com.cursedarchie.platformer.actors.enemies.logic.EnemyState;

public class DyingState implements EnemyState {
    @Override
    public void enter(NewEnemy enemy) {
        Gdx.app.log("Enemy", "Started Dying");
    }

    @Override
    public void update (NewEnemy enemy, float delta) {
        enemy.setHealth(0);
        enemy.setDamage(0);
        enemy.setPosition(new Vector2(99, 99));
        enemy.setAlive(false);
        enemy.getStateMachine().changeState(null);
    }

    @Override
    public void exit (NewEnemy enemy) {
        Gdx.app.log("Enemy", "Ended Dying");
    }
}
