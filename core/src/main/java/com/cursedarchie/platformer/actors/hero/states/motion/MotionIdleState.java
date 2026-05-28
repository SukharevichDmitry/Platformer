package com.cursedarchie.platformer.actors.hero.states.motion;

import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroState;

public class MotionIdleState implements HeroState {
    @Override
    public void enter(Hero hero) {
    }

    @Override
    public void update(Hero hero, float delta) {
        if (Math.abs(hero.getBody().getLinearVelocity().x) > 0.1f) {
            hero.getMotionStateMachine().changeState(new MoveState());
        }
    }
    @Override
    public void exit(Hero hero) {
    }
}
