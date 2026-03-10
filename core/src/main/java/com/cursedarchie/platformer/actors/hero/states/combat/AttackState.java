package com.cursedarchie.platformer.actors.hero.states.combat;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroState;
import com.cursedarchie.platformer.actors.hero.states.motion.IdleState;

public class AttackState implements HeroState {
    @Override
    public void enter(Hero hero) {
        hero.setCombatState(Hero.HeroCombatState.ATTACK);
        hero.setCombatStateTime(0f);
        hero.setAttackHandled(false);
        Gdx.app.log("HERO", "Start Attacking");
    }

    @Override
    public void update(Hero hero, float delta) {
        hero.setCombatStateTime(hero.getCombatStateTime() + delta);

        if (hero.getCombatStateTime() >= hero.getAttackDuration()) {
            hero.getCombatStateMachine().changeState(new IdleState());
            hero.setCombatState(Hero.HeroCombatState.IDLE);
        }
    }

    @Override
    public void exit(Hero hero) {
        hero.setAttackHandled(false);
        Gdx.app.log("HERO", "Finished Attacking");
    }
}
