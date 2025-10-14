package com.cursedarchie.platformer.actors.hero.states;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.Hero;
import com.cursedarchie.platformer.actors.hero.HeroState;

public class DeadState implements HeroState {
    @Override
    public void enter(Hero hero) {
        Gdx.app.log("HERO", "Started Dying");
    }

    @Override
    public void update(Hero hero, float delta) {
        hero.setHealth(0);
        hero.setDamage(0);
        hero.setAlive(false);
    }

    @Override
    public void exit(Hero hero) {
        Gdx.app.log("HERO", "Finished Dying");
    }
}
