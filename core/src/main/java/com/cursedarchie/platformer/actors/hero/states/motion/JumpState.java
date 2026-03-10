package com.cursedarchie.platformer.actors.hero.states.motion;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroState;

public class JumpState implements HeroState {

    private long jumpStartTime;

    @Override
    public void enter(Hero hero) {
        Gdx.app.log("STATE", "JUMP");
        hero.setMotionState(Hero.HeroMotionState.JUMP);
        hero.setMotionStateTime(0f);
    }

    @Override
    public void update(Hero hero, float delta) {
        if (hero.isGrounded()) {
            if (Math.abs(hero.getBody().getLinearVelocity().x) > 0.1f) {
                hero.getMotionStateMachine().changeState(new MoveState());
            } else {
                hero.getMotionStateMachine().changeState(new IdleState());
            }
        }
    }

    @Override
    public void exit(Hero hero) {
        Gdx.app.log("HERO", "Finished Jump");
    }
}
