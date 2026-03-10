package com.cursedarchie.platformer.actors.hero.machines;

import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroState;
import com.cursedarchie.platformer.actors.hero.states.motion.IdleState;


public class MotionStateMachine {
    private final Hero hero;
    private HeroState currentMotionState;

    public MotionStateMachine(Hero hero) {
        this.hero = hero;
        this.currentMotionState = new IdleState();
        this.currentMotionState.enter(hero);
    }

    public void update(float delta) {
        if (currentMotionState != null) {
            currentMotionState.update(hero, delta);
        }
    }

    public void changeState (HeroState newState) {
        if (currentMotionState != null) {
            currentMotionState.exit(hero);
        }
        currentMotionState = newState;
        hero.setMotionStateTime(0f);
        currentMotionState.enter(hero);
    }

    public HeroState getCurrentState () {
        return currentMotionState;
    }
}
