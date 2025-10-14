package com.cursedarchie.platformer.actors.hero.states;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.Hero;
import com.cursedarchie.platformer.actors.hero.HeroState;

public class HeroIdleState implements HeroState {
    @Override
    public void enter(Hero hero) {
        hero.setStateTime(0f);
        hero.getAcceleration().x = 0f;
        Gdx.app.log("HERO", "Started Idling");
    }

    @Override
    public void update(Hero hero, float delta) {

    }

    @Override
    public void exit(Hero hero) {
        Gdx.app.log("HERO", "Finished Idling");
    }
}
