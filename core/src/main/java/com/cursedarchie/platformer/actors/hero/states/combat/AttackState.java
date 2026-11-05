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
    private static final float DASH_WALL_SKIN = .02f;
    public static final int COMBO_FIRST = 1;
    public static final int COMBO_SECOND = 2;
    public static final int COMBO_FINISHER = 3;
    public static final int COUNTER_ATTACK = 4;
    public static final int JUMP_ATTACK = 5;
    public enum Phase { STARTUP, ACTIVE, RECOVERY, COMBO_READY }
    private final boolean heavy;
    private final int comboStep;
    private final Rectangle attackBounds = new Rectangle();
    private final Rectangle hurtBounds = new Rectangle();
    private AttackRuntime runtime;
    private boolean lunged;
    // Breakthrough dash bookkeeping (heavy strike only).
    private boolean dashing, dashEnded;
    private float dashOriginX, dashStartY, dashElapsed;
    private boolean blockedPending;
    private int airborneFrames;
    private float plannedDistance, dashEndX, firstHitX, firstHitY = Float.NaN, firstHitTime = -1f;
    private boolean wallLimited;
    private int dashHits;
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
        if (chainsToNext() && runtime.isComboReady()) return Phase.COMBO_READY;
        return runtime.phase() == AttackRuntime.Phase.STARTUP ? Phase.STARTUP :
            runtime.phase() == AttackRuntime.Phase.ACTIVE ? Phase.ACTIVE : Phase.RECOVERY;
    }
    public boolean isInRecovery(Hero hero) { return runtime.phase() == AttackRuntime.Phase.RECOVERY; }
    public boolean isFinisherRecovery(Hero hero) {
        return comboStep == COMBO_FINISHER && runtime.phase() == AttackRuntime.Phase.RECOVERY;
    }
    public boolean canQueueFollowUp(Hero hero) {
        return !heavy && comboStep < COMBO_FINISHER && runtime.isInComboWindow();
    }
    /** The first two light strikes open windows; the third is the finisher. */
    public boolean chainsToNext() {
        return !heavy && (comboStep == COMBO_FIRST || comboStep == COMBO_SECOND);
    }
    /** Only chainable light strikes classify follow-up timing. */
    public boolean usesRhythm() { return chainsToNext(); }

    @Override public void enter(Hero hero) {
        lunged = false;
        dashing = false;
        firstHitY = Float.NaN;
        firstHitTime = -1f;
        plannedDistance = 0f;
        wallLimited = false;
        dashEnded = false;
        dashElapsed = 0f;
        blockedPending = false;
        airborneFrames = 0;
        dashHits = 0;
        hero.setLeftAttackActive(false);
        hero.setRightAttackActive(false);
        if (heavy) {
            hero.getBody().setLinearVelocity(0f, hero.getBody().getLinearVelocity().y);
            float direction = hero.isFacingLeft() ? -1f : 1f;
            planDash(hero, direction);
            float chargeTime = BreakthroughConfig.get().chargeTimeForDistance(plannedDistance);
            runtime = new AttackRuntime(hero.breakthroughDefinition(chargeTime), hero.isFacingLeft());
        } else {
            runtime = new AttackRuntime(hero.attackDefinition(false, comboStep), hero.isFacingLeft());
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
        if (active && !lunged && comboStep != JUMP_ATTACK) {
            hero.getBody().setLinearVelocity(runtime.direction() *
                    (heavy ? 6f : comboStep == COMBO_FINISHER ? 7.5f :
                        comboStep == COMBO_SECOND ? 6.5f : 4f),
                hero.getBody().getLinearVelocity().y);
            lunged = true;
        }
        if (active) for (Actor target : hero.getAttackTargets()) {
            if (!runtime.canHit(hero, target, attackBounds, hurtBounds)) continue;
            if (comboStep == COMBO_FINISHER && target instanceof Enemy &&
                blockedByLevel(hero, target.getPosition().x, target.getPosition().y)) continue;
            AttackDefinition attack = runtime.definition();
            AttackRhythmConfig rhythm = AttackRhythmConfig.get();
            boolean hurried = runtime.isHurried();
            float damage = hero.getDamage() * attack.damageMultiplier *
                hero.meleeDamageMultiplier(heavy, comboStep) * (hurried ? rhythm.hurriedDamageMultiplier : 1f);
            float knockback = (attack.knockback + hero.meleeRecoilBonus()) *
                (hurried ? rhythm.hurriedKnockbackMultiplier : 1f) *
                (comboStep == COMBO_SECOND && !heavy ? rhythm.secondStrikeKnockbackMultiplier : 1f);
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

    /** Charge -> constant-speed dash -> stationary recovery; the dash sweeps its path and hits each enemy once. */
    private void updateBreakthrough(Hero hero, float delta) {
        BreakthroughConfig cfg = BreakthroughConfig.get();
        AttackRuntime.Phase phase = runtime.phase();
        hero.setLeftAttackActive(false);
        hero.setRightAttackActive(false);
        if (phase == AttackRuntime.Phase.STARTUP) {
            hero.getBody().setLinearVelocity(0f, hero.getBody().getLinearVelocity().y);   // fully stationary charge
            // Heavy attacks are ground-only: a charge that loses the floor (knocked off a ledge) is dropped.
            airborneFrames = hero.isGrounded() ? 0 : airborneFrames + 1;
            if (airborneFrames > 6) hero.getCombatStateMachine().changeState(new CombatIdleState());
            return;
        }
        if (phase == AttackRuntime.Phase.ACTIVE && !dashEnded) {
            float dir = runtime.direction();
            if (!dashing) {
                dashing = true;
                dashOriginX = hero.getPosition().x;
                dashStartY = hero.getPosition().y;
                blockedPending = false;
                hero.setBreakthroughDashing(true);
                hero.getBody().setGravityScale(0f);   // a ground dash stays on its own level
            }
            if (blockedPending) {   // the previous step ran into geometry: stop right here
                runtime.jumpTo(runtime.definition().activeEnd());
                endDash(hero, dir, true);
                return;
            }
            float curX = hero.getPosition().x;
            float remaining = Math.max(0f, plannedDistance - Math.abs(curX - dashOriginX));
            // Even steps over the frames the dash has left: constant speed that lands exactly on the end point,
            // independent of walking speed or any easing.
            int framesLeft = 1 + Math.max(0,
                (int) Math.ceil((runtime.definition().activeEnd() - runtime.elapsed()) / delta - 1f - 1e-3f));
            float want = remaining / framesLeft;
            if (want <= .001f) {   // reached the end point
                runtime.jumpTo(runtime.definition().activeEnd());
                endDash(hero, dir, false);
                return;
            }
            // Swept wall test along this step: never move farther than the last safe point.
            float allowed = freeAhead(hero, dir, want);
            if (allowed < want - 1e-4f) {
                if (allowed <= .002f) {
                    runtime.jumpTo(runtime.definition().activeEnd());
                    endDash(hero, dir, true);
                    return;
                }
                want = allowed;
                blockedPending = true;
            }
            sweep(hero, curX, curX + dir * want);
            float vy = Math.max(-2f, Math.min(2f, (dashStartY - hero.getPosition().y) / delta * .5f));
            hero.getBody().setLinearVelocity(dir * want / delta, vy);
        } else if (phase == AttackRuntime.Phase.RECOVERY) {
            // The dash hitbox is gone the moment recovery starts; the body is simply stopped where it is.
            if (dashing && !dashEnded) endDash(hero, runtime.direction(), blockedPending);
            hero.getBody().setLinearVelocity(0f, hero.getBody().getLinearVelocity().y);
        }
    }

    private void endDash(Hero hero, float dir, boolean blocked) {
        dashEnded = true;
        dashEndX = hero.getPosition().x;
        wallLimited = blocked;
        hero.setBreakthroughDashing(false);
        hero.getBody().setGravityScale(1f);
        hero.getBody().setLinearVelocity(0f, 0f);   // immediate stop, nothing left to slide
        if (blocked)
            hero.noteBreakthroughImpact(dashEndX + dir * hero.getSize() / 2f, hero.getPosition().y, dir);
    }

    /**
     * The dash route is fixed by the attack parameters alone: walls never shorten it (or the charge that
     * depends on it). They only stop the body during the dash itself.
     */
    private void planDash(Hero hero, final float dir) {
        plannedDistance = BreakthroughConfig.get().dashDistance;
        wallLimited = false;
    }

    /** How far the body can move along {@code dir} (up to {@code length}) before level geometry stops it. */
    private float freeAhead(final Hero hero, final float dir, float length) {
        final float half = hero.getSize() / 2f;
        final float[] free = {length};
        float x = hero.getPosition().x, y = hero.getPosition().y;
        for (float dy : new float[] {0f, half * .8f, -half * .8f}) {
            hero.getBody().getWorld().rayCast(new RayCastCallback() {
                @Override public float reportRayFixture(Fixture fixture, com.badlogic.gdx.math.Vector2 point,
                                                        com.badlogic.gdx.math.Vector2 normal, float fraction) {
                    if (fixture.isSensor() || !"level".equals(fixture.getBody().getUserData())) return -1f;
                    free[0] = Math.min(free[0], Math.abs(point.x - hero.getPosition().x) - half - DASH_WALL_SKIN);
                    return fraction;
                }
            }, new com.badlogic.gdx.math.Vector2(x, y + dy),
               new com.badlogic.gdx.math.Vector2(x + dir * (length + half + DASH_WALL_SKIN), y + dy));
        }
        return Math.max(0f, free[0]);
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
            Gdx.app.log("HERO", "breakthrough hit " + dashHits + " on " + target.getClass().getSimpleName());
        }
    }

    @Override public void exit(Hero hero) {
        if (heavy) {
            hero.setBreakthroughDashing(false);
            hero.getBody().setGravityScale(1f);
            hero.beginHeavyCooldown();   // the cooldown counts from the end of stationary recovery
        }
        hero.setAttackHandled(false);
        hero.setLeftAttackActive(false);
        hero.setRightAttackActive(false);
        Gdx.app.log("HERO", "Finished Attacking");
    }
}
