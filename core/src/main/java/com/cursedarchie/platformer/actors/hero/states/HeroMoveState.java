package com.cursedarchie.platformer.actors.hero.states;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.Hero;
import com.cursedarchie.platformer.actors.hero.HeroState;

public class HeroMoveState implements HeroState {
    @Override
    public void enter(Hero hero) {
        hero.setStateTime(0f);
        Gdx.app.log("HERO", "Started Moving");
    }

    @Override
    public void update(Hero hero, float delta) {
        if (Math.abs(hero.getAcceleration().x) == 0f) {
            hero.getStateMachine().changeState(new HeroIdleState());
        }
    }

    @Override
    public void exit(Hero hero) {
        Gdx.app.log("HERO", "Stopped Moving");
    }
}
