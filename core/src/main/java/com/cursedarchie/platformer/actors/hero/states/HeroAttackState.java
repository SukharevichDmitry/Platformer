package com.cursedarchie.platformer.actors.hero.states;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.Hero;
import com.cursedarchie.platformer.actors.hero.HeroState;

public class HeroAttackState implements HeroState {
    @Override
    public void enter(Hero hero) {
        hero.setStateTime(0f);
        hero.setAttackHandled(false);
        Gdx.app.log("HERO", "Start Attacking");
    }

    @Override
    public void update(Hero hero, float delta) {
        hero.setStateTime(hero.getStateTime() + delta);

        if (hero.getStateTime() >= hero.getAttackDuration()) {
            hero.getStateMachine().changeState(new HeroIdleState());
        }
    }

    @Override
    public void exit(Hero hero) {
        hero.setAttackHandled(false);
        Gdx.app.log("HERO", "Finished Attacking");
    }
}
