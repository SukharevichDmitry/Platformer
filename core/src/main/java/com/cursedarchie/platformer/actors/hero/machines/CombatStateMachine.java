package com.cursedarchie.platformer.actors.hero.machines;

import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroState;
import com.cursedarchie.platformer.actors.hero.states.motion.IdleState;


public class CombatStateMachine {
    private final Hero hero;
    private HeroState currentCombatState;

    public CombatStateMachine(Hero hero) {
        this.hero = hero;
        this.currentCombatState = new IdleState();
        this.currentCombatState.enter(hero);
    }

    public void update(float delta) {
        if (currentCombatState != null) {
            currentCombatState.update(hero, delta);
        }
    }

    public void changeState (HeroState newState) {
        if (currentCombatState != null) {
            currentCombatState.exit(hero);
        }
        currentCombatState = newState;
        hero.setCombatStateTime(0f);
        currentCombatState.enter(hero);
    }

    public HeroState getCurrentState () {
        return currentCombatState;
    }
}
