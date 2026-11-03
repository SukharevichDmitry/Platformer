package com.cursedarchie.platformer.actors.hero.states.combat;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.physics.CombatHit;
import com.cursedarchie.platformer.actors.hero.states.HeroState;

import java.util.HashSet;
import java.util.Set;

public class AttackState implements HeroState {
    private final Set<Actor> hitTargets = new HashSet<>();
    private final boolean heavy;
    private final int comboStep;
    private boolean lunged;
    private boolean hit;
    private boolean soundPlayed;
    private boolean facingLeft;

    public AttackState() { this(false, 1); }
    public AttackState(boolean heavy, int comboStep) {
        this.heavy = heavy;
        this.comboStep = comboStep;
    }
    public boolean isHeavy() { return heavy; }
    public int getComboStep() { return comboStep; }
    public boolean isFacingLeft() { return facingLeft; }
    public float getStartup() { return heavy ? .52f : comboStep == 2 ? .10f : Hero.ATTACK_STARTUP; }
    public float getActiveEnd() { return heavy ? .70f : comboStep == 2 ? .25f : Hero.ATTACK_ACTIVE_END; }
    public boolean isInRecovery(Hero hero) { return hero.getCombatStateTime() >= getActiveEnd(); }
    public boolean consumeSoundCue(Hero hero) {
        if (soundPlayed || hero.getCombatStateTime() < getStartup()) return false;
        soundPlayed = true;
        return true;
    }

    @Override
    public void enter(Hero hero) {
        hitTargets.clear();
        lunged = false;
        hit = false;
        soundPlayed = false;
        facingLeft = hero.isFacingLeft();
        hero.setLeftAttackActive(false);
        hero.setRightAttackActive(false);
        Gdx.app.log("HERO", "Start Attacking");
    }

    @Override
    public void update(Hero hero, float delta) {

        hero.setCombatStateTime(hero.getCombatStateTime() + delta);

        boolean activeFrame = hero.getCombatStateTime() >= getStartup() &&
            hero.getCombatStateTime() < getActiveEnd();
        hero.setLeftAttackActive(activeFrame && facingLeft);
        hero.setRightAttackActive(activeFrame && !facingLeft);

        if (activeFrame) {
            if (!lunged) {
                float forward = facingLeft ? -1f : 1f;
                hero.getBody().setLinearVelocity(forward * (heavy ? 6f : 4f),
                    hero.getBody().getLinearVelocity().y);
                lunged = true;
            }
            for (Actor target : hero.getAttackTargets()) {
                if (hitTargets.contains(target) || !hero.isTargetInAttack(target, facingLeft)) continue;
                float damage = hero.getDamage() * (heavy ? 2f : comboStep == 2 ? 1.5f : 1f) *
                    hero.meleeDamageMultiplier(heavy, comboStep);
                if (target instanceof Enemy) {
                    ((Enemy) target).takeMeleeHit(new CombatHit(
                        heavy ? CombatHit.Type.MELEE_HEAVY : CombatHit.Type.MELEE_LIGHT,
                        damage, hero.getPosition().x, heavy ? 2f : 1f,
                        (heavy ? 10f : comboStep == 2 ? 8f : 5f) + hero.meleeRecoilBonus(), hero));
                } else target.takeDamage(damage);
                hitTargets.add(target);
                hit = true;

                Gdx.app.log("HERO",
                    "dealt damage to " + target.getClass().getSimpleName()
                );
            }
        }

        if (hero.getCombatStateTime() >= (heavy ? hit ? Hero.HEAVY_ATTACK_DURATION : 1.12f :
            comboStep == 2 ? .40f : hero.getAttackDuration())) {
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
