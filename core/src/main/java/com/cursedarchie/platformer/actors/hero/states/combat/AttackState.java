package com.cursedarchie.platformer.actors.hero.states.combat;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroState;

import java.util.HashSet;
import java.util.Set;

public class AttackState implements HeroState {
    private final Set<Actor> hitTargets = new HashSet<>();

    @Override
    public void enter(Hero hero) {
        hitTargets.clear();
        hero.setLeftAttackActive(false);
        hero.setRightAttackActive(false);
        Gdx.app.log("HERO", "Start Attacking");
    }

    @Override
    public void update(Hero hero, float delta) {

        hero.setCombatStateTime(hero.getCombatStateTime() + delta);

        boolean activeFrame = hero.getCombatStateTime() >= Hero.ATTACK_STARTUP &&
            hero.getCombatStateTime() < Hero.ATTACK_ACTIVE_END;
        hero.setLeftAttackActive(activeFrame && hero.isFacingLeft());
        hero.setRightAttackActive(activeFrame && !hero.isFacingLeft());

        if (activeFrame) {
            for (Actor target : hero.getAttackTargets()) {
                if (hitTargets.contains(target) || !hero.isTargetInFacingAttack(target)) continue;
                target.takeDamage(hero.getDamage());
                hitTargets.add(target);

                Gdx.app.log("HERO",
                    "dealt damage to " + target.getClass().getSimpleName()
                );
            }
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
