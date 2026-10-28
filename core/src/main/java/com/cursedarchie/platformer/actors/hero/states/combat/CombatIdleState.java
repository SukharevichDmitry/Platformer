package com.cursedarchie.platformer.actors.hero.states.combat;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroState;

public class CombatIdleState implements HeroState {
    @Override
    public void enter(Hero hero) {
        Gdx.app.log("STATE", "COMBAT IDLE");
    }

    @Override
    public void update(Hero hero, float delta) {

    }

    @Override
    public void exit(Hero hero) {
    }
}
