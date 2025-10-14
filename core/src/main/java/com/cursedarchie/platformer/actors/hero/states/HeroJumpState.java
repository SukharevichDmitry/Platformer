package com.cursedarchie.platformer.actors.hero.states;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.Hero;
import com.cursedarchie.platformer.actors.hero.HeroState;

public class HeroJumpState implements HeroState {

    private long jumpStartTime;

    @Override
    public void enter(Hero hero) {
        // Старт прыжка
        jumpStartTime = System.currentTimeMillis();
        hero.setStateTime(0f);
        hero.setGrounded(false);
        hero.getVelocity().y = JumpConstants.MAX_JUMP_SPEED;
        Gdx.app.log("HERO", "Started Jump");
    }

    @Override
    public void update(Hero hero, float delta) {
        hero.setStateTime(hero.getStateTime() + delta);

        if (hero.isJumpPressed()) {
            long heldTime = System.currentTimeMillis() - jumpStartTime;
            if (heldTime < JumpConstants.LONG_JUMP_PRESS) {
                hero.getVelocity().y = JumpConstants.MAX_JUMP_SPEED;
            }
        }
        if (hero.isGrounded()) {
            if (hero.getVelocity().x != 0) {
                hero.getStateMachine().changeState(new HeroMoveState());
            } else {
                hero.getStateMachine().changeState(new HeroIdleState());
            }
        }
    }

    @Override
    public void exit(Hero hero) {
        Gdx.app.log("HERO", "Finished Jump");
    }

    // Вспомогательный интерфейс для хранения констант прыжка
    public static class JumpConstants {
        public static final long LONG_JUMP_PRESS = 150L;   // миллисекунды
        public static final float MAX_JUMP_SPEED = 11f;    // начальная скорость прыжка
    }
}
