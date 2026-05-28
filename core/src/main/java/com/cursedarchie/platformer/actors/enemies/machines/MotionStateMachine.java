package com.cursedarchie.platformer.actors.enemies.machines;

import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;
import com.cursedarchie.platformer.actors.enemies.states.motion.ChaseState;
import com.cursedarchie.platformer.actors.enemies.states.motion.MotionIdleState;
import com.cursedarchie.platformer.actors.enemies.states.motion.PatrolState;

public class MotionStateMachine {
    private final Enemy enemy;
    private EnemyState currentMotionState;

    public MotionStateMachine(Enemy enemy) {
        this.enemy = enemy;
        this.currentMotionState = new MotionIdleState();
        this.currentMotionState.enter(enemy);
    }

    public void update(float delta) {
        if (currentMotionState != null) {
            currentMotionState.update(enemy, delta);
        }
    }

    public void applyIntent(Enemy.Intent intent) {

        switch (intent) {

            case CHASE:
                changeState(new ChaseState());
                break;

            case PATROL:
                changeState(new PatrolState());
                break;

            case IDLE:
                changeState(new MotionIdleState());
                break;
        }
    }

    public void changeState(EnemyState newState) {

        if (currentMotionState != null &&
            currentMotionState.getClass() == newState.getClass()) {
            return;
        }

        if (currentMotionState != null) {
            currentMotionState.exit(enemy);
        }

        currentMotionState = newState;
        enemy.setMotionStateTime(0f);
        currentMotionState.enter(enemy);
    }

    public EnemyState getCurrentState () {
        return currentMotionState;
    }
}
