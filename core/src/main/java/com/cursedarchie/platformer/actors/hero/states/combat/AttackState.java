package com.cursedarchie.platformer.actors.hero.states.combat;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroState;

public class AttackState implements HeroState {
    @Override
    public void enter(Hero hero) {
        hero.setLeftAttackActive(hero.isFacingLeft());
        hero.setRightAttackActive(!hero.isFacingLeft());
        Gdx.app.log("HERO", "Start Attacking");
    }

    @Override
    public void update(Hero hero, float delta) {

        hero.setCombatStateTime(hero.getCombatStateTime() + delta);

        boolean activeFrame =
            !hero.isAttackHandled() && hero.getCombatStateTime() >= hero.getAttackDuration() * 0.5f;

        if (activeFrame) {
            for (Actor target : hero.getAttackTargets()) {
                target.takeDamage(hero.getDamage());

                Gdx.app.log("HERO",
                    "dealt damage to " + target.getClass().getSimpleName()
                );
            }
            hero.setAttackHandled(true);
        }

        if (hero.getCombatStateTime() >= hero.getAttackDuration()) {
            hero.getCombatStateMachine().changeState(new CombatIdleState());
        }
    }

    @Override
    public void exit(Hero hero) {
        hero.setAttackHandled(false);
        hero.setLeftAttackActive(false);
        hero.setRightAttackActive(false);
        Gdx.app.log("HERO", "Finished Attacking");
    }
}
