package com.cursedarchie.platformer.actors.hero;

import com.cursedarchie.platformer.actors.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroIdleState;


public class HeroStateMachine {
    private Hero hero;
    private HeroState currentState;

    public HeroStateMachine (Hero hero) {
        this.hero = hero;
        this.currentState = new HeroIdleState();
        this.currentState.enter(hero);
    }

    public void update(float delta) {
        if (currentState != null) {
            currentState.update(hero, delta);
        }
    }

    public void changeState (HeroState newState) {
        if (currentState != null) {
            currentState.exit(hero);
        }
        currentState = newState;
        hero.setStateTime(0f);
        currentState.enter(hero);
    }

    public HeroState getCurrentState () {
        return currentState;
    }
}
