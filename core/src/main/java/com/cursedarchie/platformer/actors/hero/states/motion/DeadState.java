package com.cursedarchie.platformer.actors.hero.states.motion;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroState;

public class DeadState implements HeroState {
    @Override
    public void enter(Hero hero) {
        Gdx.app.log("HERO", "Started Dying");
        hero.setMotionState(Hero.HeroMotionState.DIE);
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
