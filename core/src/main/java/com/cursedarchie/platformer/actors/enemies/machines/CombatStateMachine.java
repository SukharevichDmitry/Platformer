package com.cursedarchie.platformer.actors.enemies.machines;

import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;
import com.cursedarchie.platformer.actors.enemies.states.combat.CombatIdleState;


public class CombatStateMachine {
    private final Enemy enemy;
    private EnemyState currentCombatState;

    public CombatStateMachine(Enemy enemy) {
        this.enemy = enemy;
        this.currentCombatState = new CombatIdleState();
        this.currentCombatState.enter(enemy);
    }

    public void update(float delta) {
        if (currentCombatState != null) {
            currentCombatState.update(enemy, delta);
        }
    }


    public void changeState(EnemyState newState) {
        if (currentCombatState != null) {
            currentCombatState.exit(enemy);
        }

        currentCombatState = newState;
        enemy.setCombatStateTime(0f);
        currentCombatState.enter(enemy);
    }

    public EnemyState getCurrentState () {
        return currentCombatState;
    }
}
