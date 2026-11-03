package com.cursedarchie.platformer.actors.enemies.states.combat;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.physics.CombatHit;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;

import java.util.HashSet;
import java.util.Set;

public class AttackState implements EnemyState {
    private final Set<Actor> hitTargets = new HashSet<>();
    private final boolean lunge;
    private final boolean area;
    private boolean facingLeft;
    private boolean strikeCueSent;

    public AttackState() { this(false, false); }
    public AttackState(boolean lunge) { this(lunge, false); }
    public AttackState(boolean lunge, boolean area) { this.lunge = lunge; this.area = area; }
    public boolean isLunge() { return lunge; }
    public boolean isArea() { return area; }
    public boolean isFacingLeft() { return facingLeft; }
    public float startup(Enemy enemy) { return area ? .65f : lunge ? .48f : enemy.getAttackStartup(); }
    public float activeEnd(Enemy enemy) { return area ? .84f : lunge ? .67f : enemy.getAttackActiveEnd(); }

    @Override
    public void enter(Enemy enemy) {
        enemy.clearAttackRequest();
        hitTargets.clear();
        strikeCueSent = false;
        facingLeft = enemy.isFacingLeft();
        if ("scavenger".equals(enemy.getArchetypeId())) enemy.emitCue(Enemy.Cue.MELEE_WINDUP);
        enemy.setLeftAttackActive(false);
        enemy.setRightAttackActive(false);
        Gdx.app.log("INFO", "Start Attacking");
    }

    @Override
    public void update(Enemy enemy, float delta) {
        enemy.setCombatStateTime(enemy.getCombatStateTime() + delta);

        boolean activeFrame = enemy.getCombatStateTime() >= startup(enemy) &&
            enemy.getCombatStateTime() < activeEnd(enemy);
        if (activeFrame && !strikeCueSent) {
            strikeCueSent = true;
            if ("scavenger".equals(enemy.getArchetypeId())) enemy.emitCue(Enemy.Cue.MELEE_STRIKE);
        }
        enemy.setLeftAttackActive(activeFrame && facingLeft);
        enemy.setRightAttackActive(activeFrame && !facingLeft);

        if (activeFrame) {
            if (area) {
                for (Actor target : enemy.getVisibleActors()) {
                    if (!(target instanceof Hero) || hitTargets.contains(target) ||
                        Math.abs(target.getPosition().x - enemy.getPosition().x) > 2.4f ||
                        Math.abs(target.getPosition().y - enemy.getPosition().y) > 1.2f) continue;
                    ((Hero) target).receiveHit(new CombatHit(CombatHit.Type.MELEE_HEAVY,
                        enemy.getDamage() * 1.25f, enemy.getPosition().x, 2.5f, 5f, enemy));
                    hitTargets.add(target);
                }
            }
            if (lunge) {
                enemy.getBody().setLinearVelocity(facingLeft ? -11f : 11f,
                    enemy.getBody().getLinearVelocity().y);
            }

            for (Actor target : enemy.getAttackTargets()) {
                if (area || hitTargets.contains(target) || !enemy.isTargetInAttack(target, facingLeft)) continue;

                if (target instanceof Hero)
                    ((Hero) target).receiveHit(new CombatHit(lunge ? CombatHit.Type.MELEE_HEAVY :
                        CombatHit.Type.MELEE_LIGHT, enemy.getDamage(), enemy.getPosition().x,
                        lunge ? 2f : 1f, lunge ? 3f : 0f, enemy));
                else target.takeDamage(enemy.getDamage());
                hitTargets.add(target);
                Gdx.app.log(
                    "COMBAT",
                    enemy.getClass().getSimpleName()
                        + " dealt damage to "
                        + target.getClass().getSimpleName()
                );
            }

        }

        if (enemy.getCombatStateTime() >= (area ? 1.08f : lunge ? .88f : enemy.getAttackDuration())) {
            enemy.getCombatStateMachine().changeState(new CombatIdleState());
        }
    }

    @Override
    public void exit (Enemy enemy) {
        enemy.setAttackHandled(false);
        enemy.setLeftAttackActive(false);
        enemy.setRightAttackActive(false);
        enemy.beginAttackCooldown(lunge && hitTargets.isEmpty() ? .4f : 0f);
        Gdx.app.log("INFO", "Finished Attacking");
    }
}
