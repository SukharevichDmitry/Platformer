package com.cursedarchie.platformer.actors.enemies.logic;

import com.cursedarchie.platformer.actors.NewEnemy;
import com.cursedarchie.platformer.actors.enemies.logic.states.IdleState;

public class EnemyStateMachine {
    private NewEnemy enemy;
    private EnemyState currentState;

    public EnemyStateMachine (NewEnemy enemy) {
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
        currentState.enter(enemy);
    }
}
