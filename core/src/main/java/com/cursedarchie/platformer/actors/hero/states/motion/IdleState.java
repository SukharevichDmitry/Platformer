package com.cursedarchie.platformer.actors.hero.states.motion;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroState;

public class IdleState implements HeroState {
    @Override
    public void enter(Hero hero) {
        Gdx.app.log("STATE", "IDLE");
        hero.setMotionState(Hero.HeroMotionState.IDLE);
        hero.setMotionStateTime(0f);
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
