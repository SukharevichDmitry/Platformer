package com.cursedarchie.platformer.actors.hero.states.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.utils.Array;
import java.util.Comparator;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroState;
import com.cursedarchie.platformer.combat.AttackDefinition;
import com.cursedarchie.platformer.combat.AttackRhythmConfig;
import com.cursedarchie.platformer.combat.BreakthroughConfig;
import com.cursedarchie.platformer.combat.AttackRuntime;
import com.cursedarchie.platformer.physics.CombatHit;

/** Gameplay authority for one hero strike; rendering only reads its runtime. */
public class AttackState implements HeroState {
    public enum Phase { STARTUP, ACTIVE, RECOVERY }
    private final boolean heavy;
    private final int comboStep;
    private final Rectangle attackBounds = new Rectangle();
    private final Rectangle hurtBounds = new Rectangle();
    private AttackRuntime runtime;
    private boolean lunged;
    // Breakthrough dash bookkeeping (heavy strike only).
    private boolean dashing, dashEnded;
    private float dashOriginX, dashPrevX, dashExpectedStep, dashScale = 1f;
    private float plannedDistance, dashEndX, firstHitX, firstHitY = Float.NaN, firstHitTime = -1f;
    private boolean wallLimited;
    private int dashFrames, dashHits;
    private final Array<Enemy> sweepFound = new Array<Enemy>();
    private final Rectangle sweepBox = new Rectangle();

    public AttackState() { this(false, 1); }
    public AttackState(boolean heavy, int comboStep) { this.heavy = heavy; this.comboStep = comboStep; }
    public boolean isHeavy() { return heavy; }
    public int getComboStep() { return comboStep; }
    public AttackRuntime getRuntime() { return runtime; }
    public AttackDefinition getDefinition() { return runtime.definition(); }
    public boolean isFacingLeft() { return runtime != null && runtime.facesLeft(); }
    public float getStartup() { return runtime.definition().startupDuration; }
    public float getActiveEnd() { return runtime.definition().activeEnd(); }
    public float getDuration(Hero hero) { return runtime.duration(); }
    /** The heavy attack's direction is fixed from the first frame of the charge. */
    public boolean isDirectionLocked() { return true; }
    /** Distance the dash will really cover, fixed before the dash starts: min(configured, free space ahead). */
    public float getPlannedDistance() { return plannedDistance; }
    public boolean isWallLimited() { return wallLimited; }
    public boolean isDashEnded() { return dashEnded; }
    public float getDashEndX() { return dashEndX; }
    public float getFirstHitX() { return firstHitX; }
    public float getFirstHitY() { return firstHitY; }
    public float getFirstHitTime() { return firstHitTime; }
    public boolean isDashing() { return dashing && !dashEnded; }
    public int getDashHits() { return dashHits; }
    public float getDashOriginX() { return dashOriginX; }
    public Phase getPhase(Hero hero) {
        return runtime.phase() == AttackRuntime.Phase.STARTUP ? Phase.STARTUP :
            runtime.phase() == AttackRuntime.Phase.ACTIVE ? Phase.ACTIVE : Phase.RECOVERY;
    }
    public boolean isInRecovery(Hero hero) { return runtime.phase() == AttackRuntime.Phase.RECOVERY; }
    public boolean canQueueFollowUp(Hero hero) {
        return !heavy && comboStep == 1 && runtime.acceptsNextInput();
    }
    /** Only the first light strike chains into the second; the combo always ends after strike two. */
    public boolean chainsToNext() { return !heavy && comboStep == 1; }
    /** Light strikes one and two take part in the attack rhythm; heavy, counter and jump strikes do not. */
    public boolean usesRhythm() { return !heavy && (comboStep == 1 || comboStep == 2); }

    @Override public void enter(Hero hero) {
        runtime = new AttackRuntime(hero.attackDefinition(heavy, comboStep), hero.isFacingLeft());
        lunged = false;
        dashing = false;
        firstHitY = Float.NaN;
        firstHitTime = -1f;
        plannedDistance = 0f;
        wallLimited = false;
        dashEnded = false;
        dashScale = 1f;
        dashHits = 0;
        dashFrames = 0;
        hero.setLeftAttackActive(false);
        hero.setRightAttackActive(false);
        if (heavy) {
            hero.getBody().setLinearVelocity(0f, hero.getBody().getLinearVelocity().y);
            planDash(hero);
        }
        Gdx.app.log("HERO", "Start Attacking " + runtime.definition().id);
    }

    @Override public void update(Hero hero, float delta) {
        if (heavy && !hero.isAlive()) {   // a dead hero never carries on charging or dashing
            hero.getCombatStateMachine().changeState(new CombatIdleState());
            return;
        }
        runtime.advance(delta);
        hero.setCombatStateTime(runtime.elapsed());
        if (heavy) {
            updateBreakthrough(hero, delta);
            if (runtime.isComplete() && !runtime.hitAnyTarget()) hero.markWhiff();
            if (runtime.isComplete()) hero.getCombatStateMachine().changeState(new CombatIdleState());
            return;
        }
        boolean active = runtime.isActive();
        hero.setLeftAttackActive(active && runtime.facesLeft());
        hero.setRightAttackActive(active && !runtime.facesLeft());
        if (active && !lunged && comboStep != 4) {
            hero.getBody().setLinearVelocity(runtime.direction() *
                    (heavy ? 6f : comboStep == 2 ? 6.5f : 4f),
                hero.getBody().getLinearVelocity().y);
            lunged = true;
        }
        if (active) for (Actor target : hero.getAttackTargets()) {
            if (!runtime.canHit(hero, target, attackBounds, hurtBounds)) continue;
            AttackDefinition attack = runtime.definition();
            AttackRhythmConfig rhythm = AttackRhythmConfig.get();
            boolean hurried = runtime.isHurried();
            float damage = hero.getDamage() * attack.damageMultiplier *
                hero.meleeDamageMultiplier(heavy, comboStep) * (hurried ? rhythm.hurriedDamageMultiplier : 1f);
            float knockback = (attack.knockback + hero.meleeRecoilBonus()) *
                (hurried ? rhythm.hurriedKnockbackMultiplier : 1f) *
                (comboStep == 2 && !heavy ? rhythm.secondStrikeKnockbackMultiplier : 1f);
            float stagger = attack.staggerDuration * (hurried ? rhythm.hurriedStaggerMultiplier : 1f);
            CombatHit hit = new CombatHit(attack.type, damage, hero, target,
                runtime.direction(), attack.stabilityDamage, knockback, stagger, attack.blockable);
            float healthBefore = target.getHealth();
            boolean applied = target instanceof Enemy ? ((Enemy) target).takeMeleeHit(hit) : true;
            if (!(target instanceof Enemy)) target.takeDamage(damage);
            if (!applied || target.getHealth() == healthBefore && !(target instanceof Enemy)) continue;
            hero.emitCombatHit(hit);
            runtime.recordHit(target);
            Gdx.app.log("HERO", "dealt damage to " + target.getClass().getSimpleName());
        }
        if (runtime.isComplete() && !runtime.hitAnyTarget()) hero.markWhiff();
        if (runtime.isComplete()) hero.getCombatStateMachine().changeState(new CombatIdleState());
    }

    /** Charge -> locked charge -> dash -> recovery; the dash sweeps its path and hits each enemy once. */
    private void updateBreakthrough(Hero hero, float delta) {
        BreakthroughConfig cfg = BreakthroughConfig.get();
        AttackRuntime.Phase phase = runtime.phase();
        hero.setLeftAttackActive(false);
        hero.setRightAttackActive(false);
        if (phase == AttackRuntime.Phase.STARTUP) {
            hero.getBody().setLinearVelocity(0f, hero.getBody().getLinearVelocity().y);   // fully stationary charge
            return;
        }
        if (phase == AttackRuntime.Phase.ACTIVE && !dashEnded) {
            float dir = runtime.direction();
            if (!dashing) {
                dashing = true;
                dashOriginX = hero.getPosition().x;
                dashPrevX = dashOriginX;
                dashExpectedStep = 0f;
                hero.setBreakthroughDashing(true);
            }
            float curX = hero.getPosition().x;
            sweep(hero, dashPrevX, curX);
            float remaining = plannedDistance - Math.abs(curX - dashOriginX);
            if (remaining <= .01f) {
                // Arrived at the arrow's end: an abrupt stop (with an impact when a wall shortened the path).
                if (wallLimited) hero.noteBreakthroughImpact(curX + dir * hero.getSize() / 2f, hero.getPosition().y, dir);
                runtime.jumpTo(runtime.definition().activeEnd());
                endDash(hero, dir, wallLimited);
                return;
            }
            if (dashFrames >= 2 && Math.abs(curX - dashPrevX) < dashExpectedStep * .35f) {
                // Solid geometry stopped the hero: impact, then straight into recovery.
                hero.noteBreakthroughImpact(curX + dir * hero.getSize() / 2f, hero.getPosition().y, dir);
                hero.getBody().setLinearVelocity(0f, hero.getBody().getLinearVelocity().y);
                runtime.jumpTo(runtime.definition().activeEnd());
                endDash(hero, dir, true);
                return;
            }
            float speed = (cfg.dashSpeed > 0f ? cfg.dashSpeed : cfg.dashDistance / cfg.dashDuration) * dashScale;
            speed = Math.min(speed, remaining / delta);   // never overshoot the planned end point
            hero.getBody().setLinearVelocity(dir * speed, 0f);
            dashExpectedStep = speed * delta;
            dashPrevX = curX;
            dashFrames++;
        } else if (dashing && !dashEnded) {
            sweep(hero, dashPrevX, hero.getPosition().x);
            endDash(hero, runtime.direction(), false);
        }
    }

    private void endDash(Hero hero, float dir, boolean blocked) {
        dashEnded = true;
        dashEndX = hero.getPosition().x;
        hero.setBreakthroughDashing(false);
        hero.getBody().setLinearVelocity(0f, hero.getBody().getLinearVelocity().y);   // abrupt stop, no glide
    }

    /** Fixes the dash length before it starts: horizontal probes ahead, minus the body's half width. */
    private void planDash(Hero hero) {
        BreakthroughConfig cfg = BreakthroughConfig.get();
        final float dir = runtime.direction(), half = hero.getSize() / 2f;
        final float max = Math.max(cfg.dashDistance, cfg.minDistance);
        final float[] free = {max};
        float x = hero.getPosition().x, y = hero.getPosition().y;
        for (float dy : new float[] {0f, .35f, -.3f}) {
            final float[] best = {max + half};
            hero.getBody().getWorld().rayCast(new RayCastCallback() {
                @Override public float reportRayFixture(Fixture fixture, com.badlogic.gdx.math.Vector2 point,
                                                        com.badlogic.gdx.math.Vector2 normal, float fraction) {
                    if (fixture.isSensor() || !"level".equals(fixture.getBody().getUserData())) return -1f;
                    best[0] = Math.min(best[0], Math.abs(point.x - hero.getPosition().x));
                    return fraction;
                }
            }, new com.badlogic.gdx.math.Vector2(x, y + dy), new com.badlogic.gdx.math.Vector2(x + dir * (max + half), y + dy));
            free[0] = Math.min(free[0], best[0] - half);
        }
        wallLimited = free[0] < cfg.dashDistance - .01f;
        plannedDistance = free[0] < cfg.minDistance ? 0f : Math.min(cfg.dashDistance, free[0]);
    }

    private boolean blockedByLevel(Hero hero, float tx, float ty) {
        final boolean[] blocked = {false};
        hero.getBody().getWorld().rayCast(new RayCastCallback() {
            @Override public float reportRayFixture(Fixture fixture, com.badlogic.gdx.math.Vector2 point,
                                                    com.badlogic.gdx.math.Vector2 normal, float fraction) {
                if (!fixture.isSensor() && "level".equals(fixture.getBody().getUserData())) {
                    blocked[0] = true;
                    return 0f;
                }
                return -1f;
            }
        }, hero.getPosition(), new com.badlogic.gdx.math.Vector2(tx, ty));
        return blocked[0];
    }

    /** Swept hit test: every enemy touched between the previous and current position, nearest first. */
    private void sweep(final Hero hero, float fromX, float toX) {
        final float half = .65f;
        float y = hero.getPosition().y;
        sweepFound.clear();
        hero.getBody().getWorld().QueryAABB(new QueryCallback() {
            @Override public boolean reportFixture(Fixture fixture) {
                Object owner = fixture.getBody().getUserData();
                if (owner instanceof Enemy && ((Enemy) owner).isAlive() && !sweepFound.contains((Enemy) owner, true) &&
                    !runtime.hasStruck((Enemy) owner)) sweepFound.add((Enemy) owner);
                return true;
            }
        }, Math.min(fromX, toX) - half, y - .7f, Math.max(fromX, toX) + half, y + .7f);
        if (sweepFound.size == 0) return;
        sweepBox.set(Math.min(fromX, toX) - half, y - .6f, Math.abs(toX - fromX) + 2f * half, 1.2f);
        final float origin = dashOriginX;
        sweepFound.sort(new Comparator<Enemy>() {
            @Override public int compare(Enemy a, Enemy b) {
                return Float.compare(Math.abs(a.getPosition().x - origin), Math.abs(b.getPosition().x - origin));
            }
        });
        BreakthroughConfig cfg = BreakthroughConfig.get();
        AttackDefinition attack = runtime.definition();
        for (Enemy target : sweepFound) {
            target.getHurtboxBounds(hurtBounds);
            if (!sweepBox.overlaps(hurtBounds)) continue;
            if (blockedByLevel(hero, target.getPosition().x, target.getPosition().y)) continue;   // never through a wall
            boolean first = dashHits == 0;
            float damageScale = first ? cfg.firstHitMultiplier : cfg.followHitMultiplier;
            float damage = hero.getDamage() * attack.damageMultiplier *
                hero.meleeDamageMultiplier(true, comboStep) * damageScale;
            float knockback = (attack.knockback + hero.meleeRecoilBonus()) * (first ? 1f : cfg.followKnockbackMultiplier);
            float stagger = attack.staggerDuration * (first ? 1f : cfg.followStaggerMultiplier);
            // Only the first enemy takes a full heavy blow; the rest are shouldered aside with a lighter hit.
            CombatHit hit = new CombatHit(first ? attack.type : CombatHit.Type.MELEE_LIGHT, damage, hero, target,
                runtime.direction(), attack.stabilityDamage, knockback, stagger, attack.blockable);
            if (!target.takeMeleeHit(hit)) continue;
            hero.emitCombatHit(hit);
            runtime.recordHit(target);
            if (dashHits == 0) {
                firstHitX = target.getPosition().x;
                firstHitY = target.getPosition().y;
                firstHitTime = runtime.elapsed();
            }
            dashHits++;
            if (target.getSize() >= 2f) dashScale = Math.min(dashScale, cfg.heavyEnemySlow);
            Gdx.app.log("HERO", "breakthrough hit " + dashHits + " on " + target.getClass().getSimpleName());
        }
    }

    @Override public void exit(Hero hero) {
        if (heavy) {
            hero.setBreakthroughDashing(false);
            hero.beginHeavyCooldown();   // the cooldown counts from the end of the recovery
        }
        hero.setAttackHandled(false);
        hero.setLeftAttackActive(false);
        hero.setRightAttackActive(false);
        Gdx.app.log("HERO", "Finished Attacking");
    }
}
