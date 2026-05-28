package com.cursedarchie.platformer.actors.enemies.states.combat;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;

public class CombatIdleState implements EnemyState {
    @Override
    public void enter(Enemy enemy) {
        Gdx.app.log("ENEMY", "Combat Idle Started");
    }

    @Override
    public void update(Enemy enemy, float delta) {

    }

    @Override
    public void exit(Enemy enemy) {
        Gdx.app.log("ENEMY", "Combat Idle Finished");
    }
}
