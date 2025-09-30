package com.cursedarchie.platformer.actors.enemies.logic;

import com.cursedarchie.platformer.actors.Enemy;
import com.cursedarchie.platformer.actors.enemies.logic.states.IdleState;

public class EnemyStateMachine {
    private Enemy enemy;
    private EnemyState currentState;

    public EnemyStateMachine (Enemy enemy) {
        this.enemy = enemy;
        this.currentState = new IdleState();
        this.currentState.enter(enemy);
    }

    public void update(float delta) {
        if (currentState != null) {
            currentState.update(enemy, delta);
        }
    }

    public void changeState (EnemyState newState) {
        if (currentState != null) {
            currentState.exit(enemy);
        }
        currentState = newState;
        enemy.setStateTime(0f);
        currentState.enter(enemy);
    }

    public EnemyState getCurrentState () {
        return currentState;
    }
}
