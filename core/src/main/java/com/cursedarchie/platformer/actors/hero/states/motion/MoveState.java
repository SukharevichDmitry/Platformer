package com.cursedarchie.platformer.actors.hero.states.motion;

import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroState;

public class MoveState implements HeroState {
    @Override
    public void enter(Hero hero) {
    }

    @Override
    public void update(Hero hero, float delta) {
        // Если скорость по X очень маленькая — герой остановился
        if (Math.abs(hero.getBody().getLinearVelocity().x) < 0.1f) {
            hero.getMotionStateMachine().changeState(new MotionIdleState());
        }
    }

    @Override
    public void exit(Hero hero) {
    }
}
