package com.cursedarchie.platformer.actors.hero.states.motion;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroState;

public class JumpState implements HeroState {

    @Override
    public void enter(Hero hero) {
        Gdx.app.log("STATE", "JUMP");
    }

    @Override
    public void update(Hero hero, float delta) {
        if (hero.isGrounded() && hero.getBody().getLinearVelocity().y <= 0f) {
            if (Math.abs(hero.getBody().getLinearVelocity().x) > 0.1f) {
                hero.getMotionStateMachine().changeState(new MoveState());
            } else {
                hero.getMotionStateMachine().changeState(new MotionIdleState());
            }
        }
    }

    @Override
    public void exit(Hero hero) {
        Gdx.app.log("HERO", "Finished Jump");
    }
}
