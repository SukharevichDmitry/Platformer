package com.cursedarchie.platformer.actors.enemies;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.enemies.logic.EnemyBrain;
import com.cursedarchie.platformer.actors.enemies.machines.MotionStateMachine;
import com.cursedarchie.platformer.actors.enemies.states.combat.AttackState;
import com.cursedarchie.platformer.actors.enemies.states.combat.CombatIdleState;
import com.cursedarchie.platformer.actors.enemies.states.motion.*;
import com.cursedarchie.platformer.actors.enemies.machines.CombatStateMachine;
import com.cursedarchie.platformer.physics.PhysicsTags;
import com.cursedarchie.platformer.physics.PhysicsCategories;
import com.cursedarchie.platformer.physics.CombatHit;
import com.cursedarchie.platformer.physics.AttackConfig;
import com.cursedarchie.platformer.world.GameWorld;
import com.cursedarchie.platformer.combat.AttackDefinition;
import com.cursedarchie.platformer.combat.Hurtbox;

public abstract class Enemy extends Actor<Enemy.EnemyMotionState, Enemy.EnemyCombatState> {

    /*===========================================PARAMETERS===========================================================*/

    public enum EnemyMotionState {
        IDLE,
        PATROL,
        CHASE,
        RETREAT,
        DYING
    }

    public enum EnemyCombatState {
        IDLE,
        ATTACK
    }

    public enum Intent {
        IDLE,
        PATROL,
        CHASE,
        RETREAT
    }

    /**
     * Readable stages of the archer's ranged action. DRAW still follows the hero, LOCKED has a frozen
     * aim line, CUE is the last instant before release, RECOVERY is the lowered-bow vulnerability window.
     */
    public enum RangedPhase { NONE, DRAW, LOCKED, CUE, RECOVERY }

    /** Archer retreat cycle: lean-back tell, backward dash, then a skid back into combat stance. */
    public enum RetreatPhase { NONE, TELEGRAPH, DASH, RECOVER }

    /** One-shot audio cues. The simulation only queues them; the renderer plays them. */
    public enum Cue {
        BOW_DRAW_STANDARD, BOW_DRAW_CHARGED, BOW_DRAW_VOLLEY,
        SHOT_STANDARD, SHOT_CHARGED, SHOT_VOLLEY,
        MELEE_WINDUP, MELEE_STRIKE,
        RETREAT_DASH, DODGE,
        RAIDER_DASH, RAIDER_GUARD,
        OVERSEER_CAST, OVERSEER_BUFF, BREAKER_WINDUP, BREAKER_SLAM,
        ALERT
    }

    /** Fraction of the draw after which the aim direction is frozen. */
    public static final float RANGED_AIM_LOCK_AT = .5f;
    /** Seconds before release during which the "about to fire" cue is shown. */
    public static final float RANGED_RELEASE_CUE_TIME = .14f;
    public static final float DODGE_EFFECT_DURATION = .5f;
    public static final float DODGE_TINT_DURATION = .22f;
    /** Gameplay length of one retreat step; the first part is shown as the lean-back telegraph. */
    public static final float RETREAT_DURATION = .38f;
    public static final float RETREAT_TELEGRAPH_TIME = .1f;
    public static final float RETREAT_DASH_TIME = RETREAT_DURATION - RETREAT_TELEGRAPH_TIME;
    /** Matches the action-recovery window that follows a retreat. */
    public static final float RETREAT_RECOVER_TIME = .22f;
    public static final float RETREAT_TRAIL_FADE = .35f;
    private static final int MAX_QUEUED_CUES = 8;

    private Intent currentIntent = null;
    private EnemyBrain brain;
    private boolean canSeeHero = false;
    private boolean canAttackHero = false;
    private boolean attackRequested;
    private boolean lungeRequested;
    private boolean nextAttackLunge;
    private boolean areaRequested;
    private boolean nextAreaAttack;
    private final float attackStartup;
    private final float attackActiveEnd;
    private final float attackCooldown;
    private final AttackDefinition baseAttackDefinition;
    private float attackCooldownRemaining;
    private float staggerRemaining;
    private float lastHitSourceX = Float.NaN;
    private float jumpCooldownRemaining;
    private float rangedCooldownRemaining = .8f;
    private float rangedWindupRemaining;
    private float rangedWindupDuration;
    private final Vector2 rangedAim = new Vector2();
    private final Vector2 rangedTargetPosition = new Vector2();
    private int rangedShotSequence;
    private RangedAttackConfig rangedAttackConfig = RangedAttackConfig.NORMAL;
    private boolean rangedShotFacingLeft;
    private float rangedShotFlashRemaining;
    private float rangedShotFlashDuration = .12f;
    private float rangedRecoveryRemaining;
    private float rangedRecoveryDuration;
    private float rangedLockFlashRemaining;
    private float rangedFizzleRemaining;
    /** True while the post-retreat action recovery is running (shown as the return to combat stance). */
    private boolean retreatRecovering;
    private float retreatTrailRemaining;
    private float retreatTrailFromX;
    private float retreatTrailToX;
    private float retreatTrailDirection;
    private boolean retreatTrailDodge;
    private float dodgeOriginX;
    private float dodgeOriginY;
    private float dodgeDirection;
    private final Array<Cue> cues = new Array<>(true, MAX_QUEUED_CUES);
    private float blockRemaining;
    // Raider pre-attack dash: windup (telegraph) -> dash (no damage) -> normal attack.
    public static final float DASH_WINDUP = .22f, DASH_DURATION = .16f, DASH_COOLDOWN = 2.4f, DASH_SPEED = 11f;
    public static final float RAIDER_BLOCK_DURATION = .8f, RAIDER_BLOCK_COOLDOWN = 3f, BLOCK_HEAVY_DAMAGE = .35f;
    private float dashWindupRemaining, dashRemaining, dashCooldownRemaining = 1f, dashDirection;
    private float blockHitFlashRemaining, blockBreakFlashRemaining;
    // Infiltrator: sidestep a telegraphed strike, then flank with a lunge.
    public static final float SIDESTEP_COOLDOWN = 4.5f, SIDESTEP_RECOVERY = .55f, FLANK_WINDOW = 2.2f;
    private float flankRemaining;
    private boolean sidestepToggle;
    // Breaker alternates sweep and ground slam.
    private boolean nextSlam, slamRequested;
    // Overseer: telegraphed, interruptible buff on an ally.
    public static final float CAST_TIME = 1f, CAST_COOLDOWN = 9f, CAST_INTERRUPTED_COOLDOWN = 3f,
        BUFF_DURATION = 7f, CAST_RANGE = 6f;
    private float castRemaining, buffRemaining;
    // Group coordination (set by EnemyController): attack slots, spawn anchor and nearby allies.
    private boolean attackPermit = true;
    private float anchorX = Float.NaN;
    private Array<Enemy> groupAllies;
    private float lineBlockedRemaining;
    private Enemy castTarget;
    private float blockCooldownRemaining = .8f;
    private float dodgeCooldownRemaining;
    private float dodgeFlashRemaining;
    private float dodgeRemaining;
    private float actionRecoveryRemaining;
    private float retreatRemaining;
    private float retreatCooldownRemaining;
    private float supportCooldownRemaining;
    private float supportFlashRemaining;
    private Vector2 heroLastKnownPos = new Vector2();
    private final float patrolOriginX;
    private final World physicsWorld;
    private float patrolDirection = -1f;
    private float patrolPauseRemaining;
    protected MotionStateMachine motionStateMachine;
    protected CombatStateMachine combatStateMachine;
    private boolean active = true;
    private int roomId;
    private String persistentSpawnId;
    private String archetypeId = "default";

    public String getArchetypeId() { return archetypeId; }
    public void setArchetypeId(String archetypeId) { this.archetypeId = archetypeId; }

    /*===========================================CONSTRUCTOR=======================================================*/

    public Enemy(World world, Vector2 pos, EnemyConfig cfg) {
        super();

        //PARAMETERS=============================================================

        this.size = cfg.size;
        this.maxHealth = cfg.maxHealth;
        this.viewDistance = cfg.viewDistance;
        this.viewAngle = cfg.viewAngle;
        this.health = cfg.maxHealth;
        this.damage = cfg.damage;
        this.attackDuration = cfg.attackDuration;
        this.attackStartup = cfg.attackStartup;
        this.attackActiveEnd = cfg.attackActiveEnd;
        this.attackCooldown = cfg.attackCooldown;
        this.velocity = cfg.velocity;
        this.attackLeft = cfg.attackLeft;
        this.attackRight = cfg.attackRight;
        float reach = Math.abs(cfg.attackRight.offsetX) + cfg.attackRight.width / 2f;
        this.baseAttackDefinition = new AttackDefinition("enemy_light", CombatHit.Type.MELEE_LIGHT,
            cfg.attackStartup, cfg.attackActiveEnd - cfg.attackStartup,
            cfg.attackDuration - cfg.attackActiveEnd, reach, cfg.attackRight.width,
            cfg.attackRight.height, cfg.attackRight.offsetY, 1f, 0f, .14f, 1f,
            true, cfg.attackDuration, cfg.attackDuration);
        this.hurtbox = new Hurtbox(cfg.size * .78f, cfg.size * .92f, 0f, 0f);
        this.physicsWorld = world;
        this.patrolOriginX = pos.x;

        this.motionStateMachine = new MotionStateMachine(this);
        this.combatStateMachine = new CombatStateMachine(this);
        this.motionState = EnemyMotionState.IDLE;
        this.combatState = EnemyCombatState.IDLE;

        this.motionStateTime = 0f;
        this.combatStateTime = 0f;
        this.alive = true;
        this.facingLeft = true;
        this.attackHandled = false;

        //BODY===================================================================

        createBody(
            world,
            pos,
            PhysicsCategories.ENEMY,
            (short)(
                PhysicsCategories.WALL |
                PhysicsCategories.GROUND |
                PhysicsCategories.HERO |
                PhysicsCategories.HERO_ATTACK |
                PhysicsCategories.VISION
            )
        );

        // Enemies block the hero but should not be displaced by ordinary walking contact.
        Fixture bodyFixture = body.getFixtureList().first();
        bodyFixture.setDensity(2000f);
        body.resetMassData();
        body.setLinearDamping(8f);

        //FOOT_SENSOR============================================================

        createFootSensor(
            PhysicsCategories.ENEMY,
            (short)(
                PhysicsCategories.WALL |
                PhysicsCategories.GROUND
            )
        );

        //ATTACK_SENSORS=========================================================

        createAttackSensors(
            PhysicsTags.ENEMY_ATTACK_SENSOR,
            PhysicsCategories.ENEMY_ATTACK,
            PhysicsCategories.HERO,
            new AttackConfig(2.4f, Math.max(1.2f, size), -1.2f, 0f),
            new AttackConfig(2.4f, Math.max(1.2f, size), 1.2f, 0f)
        );

        //VISION=================================================================

        createVisionSensor(
            viewDistance,
            PhysicsCategories.VISION,
            PhysicsCategories.HERO
        );

        //=======================================================================
    }

    /*===========================================GETTERS/SETTERS===================================================*/

    public EnemyBrain getBrain() {
        return brain;
    }
    public void setBrain(EnemyBrain brain) {
        this.brain = brain;
    }

    public Intent getCurrentIntent() {
        return currentIntent;
    }
    public void setIntent(Intent newIntent) {
        this.currentIntent = newIntent;
    }

    public MotionStateMachine getMotionStateMachine() {
        return motionStateMachine;
    }
    public CombatStateMachine getCombatStateMachine() {
        return combatStateMachine;
    }

    public boolean isCanSeeHero() {
        return canSeeHero;
    }
    public void setCanSeeHero(boolean canSeeHero) {
        this.canSeeHero = canSeeHero;
    }

    public Vector2 getHeroLastKnownPos() {
        return heroLastKnownPos;
    }
    public void setHeroLastKnownPos(Vector2 heroLastKnownPos) {
        this.heroLastKnownPos.set(heroLastKnownPos);
    }
    public float getPatrolOriginX() { return patrolOriginX; }
    public float getPatrolDirection() { return patrolDirection; }
    public void reversePatrol() { patrolDirection = -patrolDirection; patrolPauseRemaining = .35f; }
    public float getPatrolPauseRemaining() { return patrolPauseRemaining; }
    public boolean hasGroundAhead(float direction) {
        final boolean[] ground = {false};
        float x = getPosition().x + direction * (size / 2f + .5f);
        float feet = getPosition().y - size / 2f;
        physicsWorld.rayCast((fixture, point, normal, fraction) -> {
            if (!fixture.isSensor() && "level".equals(fixture.getBody().getUserData())) {
                ground[0] = true;
                return 0f;
            }
            return -1f;
        }, x, feet + .15f, x, feet - 1.25f);
        return ground[0];
    }
    public boolean hasClearRetreatPath(float direction) {
        if (direction == 0f || !hasGroundAt(direction, .7f) || !hasGroundAt(direction, 1.6f)) return false;
        final boolean[] blocked = {false};
        float fromX = getPosition().x + direction * size / 2f;
        float toX = fromX + direction * 1.35f;
        physicsWorld.rayCast((fixture, point, normal, fraction) -> {
            if (!fixture.isSensor() && "level".equals(fixture.getBody().getUserData())) {
                blocked[0] = true;
                return 0f;
            }
            return -1f;
        }, fromX, getPosition().y, toX, getPosition().y);
        return !blocked[0];
    }
    private boolean hasGroundAt(float direction, float distance) {
        final boolean[] ground = {false};
        float x = getPosition().x + direction * (size / 2f + distance);
        float feet = getPosition().y - size / 2f;
        physicsWorld.rayCast((fixture, point, normal, fraction) -> {
            if (!fixture.isSensor() && "level".equals(fixture.getBody().getUserData())) {
                ground[0] = true;
                return 0f;
            }
            return -1f;
        }, x, feet + .12f, x, feet - 1.25f);
        return ground[0];
    }
    public boolean isStaggered() { return staggerRemaining > 0f; }
    public float getLastHitSourceX() { return lastHitSourceX; }
    public boolean isRangedWindingUp() { return rangedWindupRemaining > 0f; }
    public boolean isRangedRecovering() { return rangedRecoveryRemaining > 0f; }
    /** True from the first frame of the draw until the bow is lowered again. */
    public boolean isRangedBusy() { return isRangedWindingUp() || isRangedRecovering(); }
    public boolean isRangedShotFacingLeft() { return rangedShotFacingLeft; }
    public Vector2 getRangedAim() { return rangedAim; }
    public Vector2 getRangedTargetPosition() { return rangedTargetPosition; }
    public float getRangedWindupProgress() { return rangedWindupDuration == 0f ? 0f :
        1f - rangedWindupRemaining / rangedWindupDuration; }
    /** 0 = standard shot, 1 = charged shot, 2 = three-arrow volley (derived from the active config). */
    public int getRangedShotMode() { return rangedAttackConfig.type.ordinal(); }
    public RangedAttackConfig getRangedAttackConfig() { return rangedAttackConfig; }
    public float getRangedShotFlashRemaining() { return rangedShotFlashRemaining; }
    public float getRangedShotFlashDuration() { return rangedShotFlashDuration; }
    public float getRangedRecoveryProgress() { return rangedRecoveryDuration == 0f ? 1f :
        1f - rangedRecoveryRemaining / rangedRecoveryDuration; }
    /** Short pulse right after the aim direction freezes. */
    public float getRangedLockFlashRemaining() { return rangedLockFlashRemaining; }
    /** Remaining time of the "draw broken off" effect when a shot was lost to a stagger. */
    public float getRangedFizzleRemaining() { return rangedFizzleRemaining; }
    /** 0 until the release cue starts, then rises to 1 at the moment the arrow leaves the bow. */
    public float getRangedReleaseCueProgress() {
        return rangedWindupRemaining <= 0f || rangedWindupRemaining > RANGED_RELEASE_CUE_TIME ? 0f :
            1f - rangedWindupRemaining / RANGED_RELEASE_CUE_TIME;
    }
    public RangedPhase getRangedPhase() {
        if (rangedWindupRemaining > 0f) {
            if (getRangedWindupProgress() < RANGED_AIM_LOCK_AT) return RangedPhase.DRAW;
            return rangedWindupRemaining <= RANGED_RELEASE_CUE_TIME ? RangedPhase.CUE : RangedPhase.LOCKED;
        }
        return rangedRecoveryRemaining > 0f ? RangedPhase.RECOVERY : RangedPhase.NONE;
    }

    /** Derived from the gameplay timers: telegraph/dash while retreating, recover while settling back. */
    public RetreatPhase getRetreatPhase() {
        if (retreatRemaining > 0f)
            return RETREAT_DURATION - retreatRemaining < RETREAT_TELEGRAPH_TIME ?
                RetreatPhase.TELEGRAPH : RetreatPhase.DASH;
        return retreatRecovering && actionRecoveryRemaining > 0f ? RetreatPhase.RECOVER : RetreatPhase.NONE;
    }
    public boolean isRetreatActive() { return getRetreatPhase() != RetreatPhase.NONE; }
    public boolean isRetreatRecovering() { return getRetreatPhase() == RetreatPhase.RECOVER; }
    public float getRetreatPhaseProgress() {
        float elapsed = RETREAT_DURATION - retreatRemaining;
        switch (getRetreatPhase()) {
            case TELEGRAPH: return Math.min(1f, elapsed / RETREAT_TELEGRAPH_TIME);
            case DASH: return Math.min(1f, (elapsed - RETREAT_TELEGRAPH_TIME) / RETREAT_DASH_TIME);
            case RECOVER: return Math.min(1f, 1f - actionRecoveryRemaining / RETREAT_RECOVER_TIME);
            default: return 0f;
        }
    }
    public float getRetreatTrailRemaining() { return retreatTrailRemaining; }
    public float getRetreatTrailFromX() { return retreatTrailFromX; }
    public float getRetreatTrailToX() { return retreatTrailToX; }
    public float getRetreatTrailDirection() { return retreatTrailDirection; }
    public boolean isRetreatTrailDodge() { return retreatTrailDodge; }
    public float getDodgeOriginX() { return dodgeOriginX; }
    public float getDodgeOriginY() { return dodgeOriginY; }
    public float getDodgeDirection() { return dodgeDirection; }

    /** Next queued audio cue, or null. Cues are produced by the simulation and consumed by rendering. */
    public Cue pollCue() { return cues.size == 0 ? null : cues.removeIndex(0); }
    public void emitCue(Cue cue) { if (cues.size < MAX_QUEUED_CUES) cues.add(cue); }

    public boolean isBlocking() { return blockRemaining > 0f; }
    /** Test hook: raise the raider guard immediately. */
    public void forceBlockForTest() { blockRemaining = RAIDER_BLOCK_DURATION; }
    /** What the enemy currently knows about the hero; drives the alert/lost/search presentation. */
    public enum Awareness { UNAWARE, ALERT, TRACKING, LOST, SEARCHING }
    public static final float TURN_FLASH = .18f, TURN_ATTACK_GRACE = .2f;
    private Awareness awareness = Awareness.UNAWARE;
    private float awarenessTime, turnFlashRemaining, turnGraceRemaining, searchLookRemaining;
    public Awareness getAwareness() { return awareness; }
    public float getAwarenessTime() { return awarenessTime; }
    public float getTurnFlashRemaining() { return turnFlashRemaining; }
    public void setAwareness(Awareness value) {
        if (value == awareness) return;
        if (value == Awareness.ALERT) emitCue(Cue.ALERT);
        awareness = value;
        awarenessTime = 0f;
        searchLookRemaining = .65f;
    }
    /**
     * Snap to face a world x without any patrol-style turn animation. A short grace period follows so a
     * freshly turned enemy always goes through its attack startup instead of striking on the same frame.
     */
    public void turnToward(float x) {
        if (Math.abs(x - getPosition().x) < .15f) return;   // hero straight above/below: keep facing
        boolean wantLeft = x < getPosition().x;
        if (wantLeft == facingLeft) return;
        facingLeft = wantLeft;
        turnFlashRemaining = TURN_FLASH;
        turnGraceRemaining = TURN_ATTACK_GRACE;
        attackRequested = false;
    }
    private float stompCooldownRemaining, stompFlashRemaining;
    public float getStompFlashRemaining() { return stompFlashRemaining; }
    /** Hero landed on this enemy's head: brief stagger, no damage; returns false while the cooldown runs. */
    public boolean takeHeadStomp() {
        if (!alive || stompCooldownRemaining > 0f) return false;
        stompCooldownRemaining = .8f;
        stompFlashRemaining = .3f;
        applyStagger(.28f);
        return true;
    }
    public void applyStagger(float seconds) {
        if (!alive) return;
        staggerRemaining = Math.max(staggerRemaining, seconds);
        blockRemaining = 0f;
        cancelRangedAttack();
    }
    public void setAttackPermit(boolean permit) { attackPermit = permit; }
    public boolean hasAttackPermit() { return attackPermit; }
    public boolean isLineBlocked() { return lineBlockedRemaining > 0f; }
    /** Breaker holds its ground: it will not chase far past where it first stood. */
    public boolean isBeyondLeash(float limit) { return !Float.isNaN(anchorX) && Math.abs(getPosition().x - anchorX) > limit; }
    public boolean isCasting() { return castRemaining > 0f; }
    public float getCastProgress() { return castRemaining > 0f ? 1f - castRemaining / CAST_TIME : 0f; }
    public Enemy getCastTarget() { return castTarget; }
    public boolean isBuffed() { return buffRemaining > 0f; }
    public void receiveBuff(float seconds) { buffRemaining = Math.max(buffRemaining, seconds); }
    public boolean isFlanking() { return flankRemaining > 0f; }
    public boolean isNextSlam() { return nextSlam; }
    public boolean isDashWindingUp() { return dashWindupRemaining > 0f; }
    public boolean isDashing() { return dashRemaining > 0f; }
    public boolean isDashPhase() { return dashWindupRemaining > 0f || dashRemaining > 0f; }
    public float getBlockHitFlashRemaining() { return blockHitFlashRemaining; }
    public float getBlockBreakFlashRemaining() { return blockBreakFlashRemaining; }
    public float getBlockProgress() { return blockRemaining / RAIDER_BLOCK_DURATION; }
    public com.cursedarchie.platformer.combat.CombatPhase getCombatPhase() {
        if (!alive) return com.cursedarchie.platformer.combat.CombatPhase.DEAD;
        if (isStaggered()) return com.cursedarchie.platformer.combat.CombatPhase.STAGGER;
        if (isDodging() || isDashing()) return com.cursedarchie.platformer.combat.CombatPhase.DODGE;
        if (isBlocking()) return com.cursedarchie.platformer.combat.CombatPhase.BLOCK;
        if (isDashWindingUp() || isRangedWindingUp() || isCasting()) return com.cursedarchie.platformer.combat.CombatPhase.WINDUP;
        if (isActionRecovering()) return com.cursedarchie.platformer.combat.CombatPhase.RECOVERY;
        return com.cursedarchie.platformer.combat.CombatPhase.IDLE;
    }
    public float getDodgeFlashRemaining() { return dodgeFlashRemaining; }
    public boolean isNextAttackLunge() { return nextAttackLunge; }
    public boolean isAttacking() { return combatStateMachine.getCurrentState() instanceof AttackState; }
    public float getSupportFlashRemaining() { return supportFlashRemaining; }
    public boolean isDodging() { return dodgeRemaining > 0f; }
    public boolean isActionRecovering() { return actionRecoveryRemaining > 0f; }
    public boolean isRetreating() { return retreatRemaining > 0f; }
    public boolean canBeginRetreat(float direction) {
        return retreatCooldownRemaining == 0f && actionRecoveryRemaining == 0f &&
            !isStaggered() && !isAttacking() && !isRangedWindingUp() &&
            hasClearRetreatPath(direction);
    }
    public boolean beginRetreat(float direction) {
        if (!canBeginRetreat(direction)) return false;
        retreatRemaining = RETREAT_DURATION;
        retreatCooldownRemaining = 1.2f;
        retreatRecovering = false;
        beginTrail(direction, false);
        emitCue(Cue.RETREAT_DASH);
        return true;
    }
    public void finishRetreat() {
        if (retreatRemaining > 0f) {
            retreatRemaining = 0f;
            retreatRecovering = true;
        }
        actionRecoveryRemaining = Math.max(actionRecoveryRemaining, RETREAT_RECOVER_TIME);
        moveStop();
    }
    public void updateSpecialActions(Hero hero, Array<Enemy> allies, float delta) {
        updateSpecialActions(null, hero, allies, delta);
    }
    public void updateSpecialActions(GameWorld gameWorld, Hero hero, Array<Enemy> allies, float delta) {
        if (!active || !alive) return;
        if (Float.isNaN(anchorX)) anchorX = getPosition().x;
        groupAllies = allies;
        lineBlockedRemaining = Math.max(0f, lineBlockedRemaining - delta);
        rangedCooldownRemaining = Math.max(0f, rangedCooldownRemaining - delta);
        rangedShotFlashRemaining = Math.max(0f, rangedShotFlashRemaining - delta);
        blockRemaining = Math.max(0f, blockRemaining - delta);
        blockCooldownRemaining = Math.max(0f, blockCooldownRemaining - delta);
        blockHitFlashRemaining = Math.max(0f, blockHitFlashRemaining - delta);
        blockBreakFlashRemaining = Math.max(0f, blockBreakFlashRemaining - delta);
        dashCooldownRemaining = Math.max(0f, dashCooldownRemaining - delta);
        flankRemaining = Math.max(0f, flankRemaining - delta);
        stompCooldownRemaining = Math.max(0f, stompCooldownRemaining - delta);
        turnFlashRemaining = Math.max(0f, turnFlashRemaining - delta);
        turnGraceRemaining = Math.max(0f, turnGraceRemaining - delta);
        awarenessTime += delta;
        // Searching: after reaching the last known position the enemy looks around instead of freezing.
        if (awareness == Awareness.SEARCHING && !isAttacking() && !isStaggered() && !isRangedBusy()) {
            searchLookRemaining -= delta;
            if (searchLookRemaining <= 0f) {
                facingLeft = !facingLeft;
                turnFlashRemaining = TURN_FLASH;
                searchLookRemaining = .65f;
            }
        }
        stompFlashRemaining = Math.max(0f, stompFlashRemaining - delta);
        buffRemaining = Math.max(0f, buffRemaining - delta);
        if (dashWindupRemaining > 0f) {
            dashWindupRemaining = Math.max(0f, dashWindupRemaining - delta);
            if (dashWindupRemaining == 0f) {
                dashRemaining = DASH_DURATION;
                body.setLinearVelocity(dashDirection * DASH_SPEED, body.getLinearVelocity().y);
            }
        } else if (dashRemaining > 0f) {
            dashRemaining = Math.max(0f, dashRemaining - delta);
            body.setLinearVelocity(dashDirection * DASH_SPEED, body.getLinearVelocity().y);
            if (dashRemaining == 0f) {
                moveStop();
                dashCooldownRemaining = DASH_COOLDOWN;
                attackCooldownRemaining = 0f;
                attackRequested = false;
                requestAttack();
            }
        }
        dodgeCooldownRemaining = Math.max(0f, dodgeCooldownRemaining - delta);
        dodgeFlashRemaining = Math.max(0f, dodgeFlashRemaining - delta);
        dodgeRemaining = Math.max(0f, dodgeRemaining - delta);
        actionRecoveryRemaining = Math.max(0f, actionRecoveryRemaining - delta);
        float previousRetreatRemaining = retreatRemaining;
        retreatRemaining = Math.max(0f, retreatRemaining - delta);
        if (previousRetreatRemaining > 0f && retreatRemaining == 0f) {
            actionRecoveryRemaining = Math.max(actionRecoveryRemaining, RETREAT_RECOVER_TIME);
            retreatRecovering = true;
        }
        if (actionRecoveryRemaining == 0f) retreatRecovering = false;
        retreatCooldownRemaining = Math.max(0f, retreatCooldownRemaining - delta);
        supportCooldownRemaining = Math.max(0f, supportCooldownRemaining - delta);
        supportFlashRemaining = Math.max(0f, supportFlashRemaining - delta);
        if ("default".equals(archetypeId)) updateRaider(hero);
        if ("infiltrator".equals(archetypeId)) updateInfiltrator(hero);
        if ("overseer".equals(archetypeId)) updateOverseer(allies, delta);
        if ("scavenger".equals(archetypeId)) {
            float dx = hero.getPosition().x - getPosition().x;
            float dy = Math.abs(hero.getPosition().y - getPosition().y);
            rangedRecoveryRemaining = Math.max(0f, rangedRecoveryRemaining - delta);
            rangedLockFlashRemaining = Math.max(0f, rangedLockFlashRemaining - delta);
            rangedFizzleRemaining = Math.max(0f, rangedFizzleRemaining - delta);
            if (isDodging() || isActionRecovering() || isRetreating() || isStaggered() || isAttacking()) {
                if (rangedWindupRemaining > 0f) interruptRangedAttack();
                return;
            }
            if (rangedWindupRemaining > 0f) {
                if (Math.abs(dx) <= 2.4f) {
                    interruptRangedAttack();
                    return;
                }
                boolean wasOpen = getRangedWindupProgress() < RANGED_AIM_LOCK_AT;
                // Main preparation phase: the aim point follows the hero; once locked it no longer moves.
                if (wasOpen) aimAt(hero.getPosition());
                rangedWindupRemaining -= delta;
                if (rangedWindupRemaining <= 0f) {
                    releaseRangedShot(gameWorld);
                } else if (wasOpen && getRangedWindupProgress() >= RANGED_AIM_LOCK_AT) {
                    // Presentation only: the committed aim line turns solid at this point.
                    rangedLockFlashRemaining = .14f;
                }
            } else beginRangedAttack(selectRangedAttack(), hero);
        } else if ("red_collector".equals(archetypeId)) {
            if (blockCooldownRemaining == 0f && canSeeHero && !isStaggered() &&
                !(combatStateMachine.getCurrentState() instanceof AttackState) &&
                getPosition().dst2(hero.getPosition()) < 3f * 3f) {
                blockRemaining = .9f;
                blockCooldownRemaining = 3.2f;
            }
            if (supportCooldownRemaining > 0f) return;
            for (int i = 0; i < allies.size; i++) {
                Enemy ally = allies.get(i);
                if (ally == this || !ally.isAlive() || !ally.isActive() ||
                    ally.getHealth() >= ally.getMaxHealth() ||
                    ally.getPosition().dst2(getPosition()) > 4f * 4f) continue;
                ally.setHealth(Math.min(ally.getMaxHealth(), ally.getHealth() + .75f));
                supportCooldownRemaining = 4.5f;
                supportFlashRemaining = .3f;
                break;
            }
        }
    }

    private RangedAttackConfig selectRangedAttack() {
        int sequence = rangedShotSequence + 1;
        return sequence % 5 == 0 ? RangedAttackConfig.VOLLEY :
            sequence % 3 == 0 ? RangedAttackConfig.CHARGED : RangedAttackConfig.NORMAL;
    }

    private void updateInfiltrator(Hero hero) {
        if (isStaggered() || isAttacking() || isDodging() || isActionRecovering()) return;
        float dx = hero.getPosition().x - getPosition().x;
        float dy = Math.abs(hero.getPosition().y - getPosition().y);
        float dist = Math.abs(dx);
        // Flank: right after a sidestep it lunges in without waiting for the normal attack cooldown.
        if (flankRemaining > 0f && canSeeHero && dy < 1.1f && dist < 3.4f) {
            facingLeft = dx < 0f;
            attackCooldownRemaining = 0f;
            requestAttack();
            return;
        }
        // Sidestep a telegraphed hero swing, but only every other time and then with a long cooldown.
        if (dodgeCooldownRemaining == 0f && canSeeHero && dist < 2.2f && dy < 1.2f && hero.isAttackTelegraphing()) {
            sidestepToggle = !sidestepToggle;
            if (!sidestepToggle) return;
            float away = dx > 0f ? -1f : 1f;
            if (!hasClearRetreatPath(away)) return;   // never sidestep into a wall or off a ledge
            dodgeOriginX = getPosition().x;
            dodgeOriginY = getPosition().y;
            dodgeDirection = away;
            body.setLinearVelocity(away * 9f, body.getLinearVelocity().y);
            dodgeCooldownRemaining = SIDESTEP_COOLDOWN;
            dodgeFlashRemaining = DODGE_EFFECT_DURATION;
            dodgeRemaining = .16f;
            actionRecoveryRemaining = SIDESTEP_RECOVERY;   // wide open for a punish
            attackRequested = false;
            currentIntent = Intent.IDLE;
            beginTrail(away, true);
            facingLeft = away > 0f;
            flankRemaining = FLANK_WINDOW;
            emitCue(Cue.DODGE);
        }
    }

    private void updateOverseer(Array<Enemy> allies, float delta) {
        if (isStaggered()) { cancelCast(CAST_INTERRUPTED_COOLDOWN); return; }
        if (castRemaining > 0f) {
            if (castTarget == null || !castTarget.isAlive() ||
                castTarget.getPosition().dst2(getPosition()) > CAST_RANGE * CAST_RANGE * 1.4f) {
                cancelCast(CAST_INTERRUPTED_COOLDOWN);
                return;
            }
            moveStop();
            castRemaining = Math.max(0f, castRemaining - delta);
            if (castRemaining == 0f) {
                castTarget.receiveBuff(BUFF_DURATION);
                castTarget.emitCue(Cue.OVERSEER_BUFF);
                castTarget = null;
                supportCooldownRemaining = CAST_COOLDOWN;
                actionRecoveryRemaining = Math.max(actionRecoveryRemaining, .5f);
            }
            return;
        }
        if (supportCooldownRemaining > 0f || isAttacking() || isActionRecovering() || allies == null) return;
        Enemy best = null;
        for (int i = 0; i < allies.size; i++) {
            Enemy ally = allies.get(i);
            if (ally == this || !ally.isAlive() || !ally.isActive() || ally.isBuffed() ||
                "overseer".equals(ally.getArchetypeId()) ||
                ally.getPosition().dst2(getPosition()) > CAST_RANGE * CAST_RANGE) continue;
            if (best == null || ally.getPosition().dst2(getPosition()) < best.getPosition().dst2(getPosition()))
                best = ally;
        }
        if (best != null) {
            castTarget = best;
            castRemaining = CAST_TIME;
            attackRequested = false;
            moveStop();
            emitCue(Cue.OVERSEER_CAST);
        }
    }

    private void cancelCast(float cooldown) {
        if (castRemaining <= 0f) return;
        castRemaining = 0f;
        castTarget = null;
        supportCooldownRemaining = Math.max(supportCooldownRemaining, cooldown);
    }

    private boolean raiderBusy() {
        return isStaggered() || isAttacking() || isDodging() || isActionRecovering() || isDashPhase();
    }
    private void updateRaider(Hero hero) {
        if (raiderBusy()) return;
        float dx = hero.getPosition().x - getPosition().x;
        float dy = Math.abs(hero.getPosition().y - getPosition().y);
        float dist = Math.abs(dx);
        // Priority 2: block a telegraphed hero attack that is about to land on this side.
        if (blockCooldownRemaining == 0f && !isBlocking() && canSeeHero && dist < 2.2f && dy < 1.2f &&
            (dx < 0) == facingLeft && hero.isAttackTelegraphing()) {
            blockRemaining = RAIDER_BLOCK_DURATION;
            blockCooldownRemaining = RAIDER_BLOCK_COOLDOWN;
            emitCue(Cue.RAIDER_GUARD);
            moveStop();
            return;
        }
        // Priority 4: short dash when the hero hovers just outside attack range.
        if (!isBlocking() && dashCooldownRemaining == 0f && attackCooldownRemaining == 0f && canSeeHero &&
            dy < 1f && dist > getAttackApproachDistance(hero) + .15f && dist < getAttackApproachDistance(hero) + 1.1f &&
            hasClearRetreatPath(Math.signum(dx))) {
            dashDirection = Math.signum(dx);
            facingLeft = dashDirection < 0f;
            dashWindupRemaining = DASH_WINDUP;
            emitCue(Cue.RAIDER_DASH);
            dashCooldownRemaining = DASH_COOLDOWN;
            attackRequested = false;
            moveStop();
        }
    }

    public boolean beginRangedAttack(RangedAttackConfig config, Hero hero) {
        if (!"scavenger".equals(archetypeId) || config == null || hero == null ||
            rangedCooldownRemaining > 0f || rangedWindupRemaining > 0f ||
            rangedRecoveryRemaining > 0f || !canSeeHero ||
            isStaggered() || isAttacking() || isDodging() || isRetreating() ||
            isActionRecovering() || currentIntent == Intent.RETREAT) return false;
        float dx = hero.getPosition().x - getPosition().x;
        float dy = Math.abs(hero.getPosition().y - getPosition().y);
        if (Math.abs(dx) <= 2.4f || Math.abs(dx) >= config.maxRange || dy >= 1.25f ||
            !hasLineOfFire(hero.getPosition())) return false;
        if (isLineBlockedByAlly(hero.getPosition())) {
            lineBlockedRemaining = .8f;   // brain reacts by repositioning instead of shooting through a friend
            return false;
        }
        rangedShotSequence++;
        rangedAttackConfig = config;
        rangedWindupDuration = config.startup;
        rangedWindupRemaining = config.startup;
        aimAt(hero.getPosition());
        rangedFizzleRemaining = 0f;
        moveStop();
        emitCue(config.type == RangedAttackConfig.Type.VOLLEY ? Cue.BOW_DRAW_VOLLEY :
            config.type == RangedAttackConfig.Type.CHARGED ? Cue.BOW_DRAW_CHARGED : Cue.BOW_DRAW_STANDARD);
        return true;
    }

    /** Points the draw at a target with the arc needed to land on it; the shooter turns to face it. */
    private void aimAt(Vector2 target) {
        rangedTargetPosition.set(target);
        rangedAim.set(rangedAttackConfig.launchDirection(getPosition().x, getPosition().y + .1f, target.x, target.y));
        rangedShotFacingLeft = rangedAim.x < 0f;
        facingLeft = rangedShotFacingLeft;
    }

    public void cancelRangedAttack() {
        rangedWindupRemaining = 0f;
        rangedWindupDuration = 0f;
    }

    /** True when another enemy stands on the straight shot between this archer and the target. */
    public boolean isLineBlockedByAlly(Vector2 target) {
        if (groupAllies == null) return false;
        Vector2 a = getPosition();
        float minX = Math.min(a.x, target.x), maxX = Math.max(a.x, target.x);
        for (int i = 0; i < groupAllies.size; i++) {
            Enemy ally = groupAllies.get(i);
            if (ally == this || !ally.isAlive() || ally.getPosition().x < minX + .3f || ally.getPosition().x > maxX - .3f)
                continue;
            float t = (ally.getPosition().x - a.x) / (target.x - a.x == 0f ? 1f : target.x - a.x);
            float lineY = a.y + t * (target.y - a.y);
            if (Math.abs(ally.getPosition().y - lineY) < ally.getSize() * .5f + .1f) return true;
        }
        return false;
    }

    private boolean hasLineOfFire(Vector2 target) {
        final boolean[] blocked = {false};
        physicsWorld.rayCast((fixture, point, normal, fraction) -> {
            if (!fixture.isSensor() && "level".equals(fixture.getBody().getUserData())) {
                blocked[0] = true;
                return 0f;
            }
            return -1f;
        }, getPosition(), target);
        return !blocked[0];
    }

    /** A draw that is broken off: a stagger shows a "lost draw" effect, anything else just lowers the bow. */
    private void interruptRangedAttack() {
        boolean lostToStagger = isStaggered() && rangedWindupRemaining > 0f;
        cancelRangedAttack();
        if (lostToStagger) {
            rangedFizzleRemaining = .3f;
            rangedRecoveryDuration = .2f;
            rangedRecoveryRemaining = rangedRecoveryDuration;
        }
    }

    /** Fires the committed shot (gameplay: config + GameWorld.fireArrows) and starts the visual follow-through. */
    private void releaseRangedShot(GameWorld gameWorld) {
        rangedWindupRemaining = 0f;
        rangedCooldownRemaining = rangedAttackConfig.cooldown;
        int mode = getRangedShotMode();
        rangedShotFlashDuration = mode == 2 ? .22f : mode == 1 ? .18f : .1f;
        rangedShotFlashRemaining = rangedShotFlashDuration;
        rangedRecoveryDuration = mode == 2 ? .5f : mode == 1 ? .42f : .28f;
        rangedRecoveryRemaining = rangedRecoveryDuration;
        emitCue(mode == 2 ? Cue.SHOT_VOLLEY : mode == 1 ? Cue.SHOT_CHARGED : Cue.SHOT_STANDARD);
        if (gameWorld != null) gameWorld.fireArrows(this, rangedAim, rangedAttackConfig);
    }

    private void beginTrail(float direction, boolean dodge) {
        retreatTrailFromX = getPosition().x;
        retreatTrailToX = getPosition().x;
        retreatTrailDirection = direction;
        retreatTrailDodge = dodge;
        retreatTrailRemaining = (dodge ? .3f : RETREAT_DURATION + RETREAT_RECOVER_TIME) + RETREAT_TRAIL_FADE;
    }
    public void pursueElevatedTarget(float targetY) {
        if (jumpCooldownRemaining > 0f || !grounded ||
            targetY < getPosition().y + 1.15f || "boss".equals(archetypeId)) return;
        body.setLinearVelocity(body.getLinearVelocity().x, 19f);
        jumpCooldownRemaining = 1.1f;
    }

    /** Melee recoil is deliberately weaker on the collector and boss. */
    public void takeMeleeHit(float amount, float sourceX, float recoil, boolean heavy) {
        takeMeleeHit(new CombatHit(heavy ? CombatHit.Type.MELEE_HEAVY :
            CombatHit.Type.MELEE_LIGHT, amount, sourceX, heavy ? 2f : 1f, recoil, null));
    }
    public boolean takeMeleeHit(CombatHit hit) {
        if (!alive) return false;
        boolean finisher = hit.type == CombatHit.Type.MELEE_FINISHER;
        boolean wasAttacking = isAttacking();
        // Sidestep i-frame: only light strikes whiff; heavy and area hits still catch the infiltrator mid-step.
        if ("infiltrator".equals(archetypeId) && isDodging() && hit.type == CombatHit.Type.MELEE_LIGHT) return false;
        if ("scavenger".equals(archetypeId) && hit.type == CombatHit.Type.MELEE_LIGHT &&
            dodgeCooldownRemaining == 0f && !isStaggered() && !isAttacking() &&
            !isRangedWindingUp() && !isRetreating() && !isActionRecovering()) {
            float away = hit.sourceX < getPosition().x ? 1f : -1f;
            if (hasClearRetreatPath(away)) {
                // Presentation: remember where the blow landed so the renderer can show a whiffed strike.
                dodgeOriginX = getPosition().x;
                dodgeOriginY = getPosition().y;
                dodgeDirection = away;
                body.setLinearVelocity(away * 9f, body.getLinearVelocity().y);
                dodgeCooldownRemaining = 2.5f;
                dodgeFlashRemaining = DODGE_EFFECT_DURATION;
                dodgeRemaining = .16f;
                actionRecoveryRemaining = .34f;
                retreatRecovering = false;
                cancelRangedAttack();
                attackRequested = false;
                currentIntent = Intent.IDLE;
                beginTrail(away, true);
                facingLeft = away > 0f;
                emitCue(Cue.DODGE);
                return false;
            }
        }
        float amount = hit.damage;
        float sourceX = hit.sourceX;
        float recoil = hit.recoil;
        boolean heavy = hit.type == CombatHit.Type.MELEE_HEAVY || finisher;
        boolean raiderGuard = "default".equals(archetypeId) && hit.blockable && isBlocking() && !isStaggered() &&
            !isAttacking() && ((sourceX < getPosition().x) == facingLeft);
        if (raiderGuard) {
            if (heavy) {
                // Heavy strikes break the guard, hurt for a fraction and knock the raider back.
                amount *= BLOCK_HEAVY_DAMAGE;
                blockRemaining = 0f;
                blockBreakFlashRemaining = .4f;
            } else {
                // Light strikes bounce off: no damage, no stagger, only a small slide.
                takeDamage(0f);
                lastHitSourceX = sourceX;
                blockHitFlashRemaining = .22f;
                body.setLinearVelocity(Math.copySign(1.2f, getPosition().x - sourceX), body.getLinearVelocity().y);
                return true;
            }
        } else if (heavy && hit.blockable && isBlocking()) blockRemaining = 0f;
        if (!heavy && hit.blockable && "red_collector".equals(archetypeId) && isBlocking() && !isStaggered() &&
            ((sourceX < getPosition().x) == facingLeft) &&
            !(combatStateMachine.getCurrentState() instanceof AttackState)) {
            amount *= .5f;
            recoil *= .25f;
            blockRemaining = 0f;
        }
        takeDamage(amount);
        lastHitSourceX = sourceX;
        if (!alive) return true;
        if (castRemaining > 0f) cancelCast(CAST_INTERRUPTED_COOLDOWN);
        float resistance = "boss".equals(archetypeId) ? .1f :
            "heavy_hunter".equals(archetypeId) ? heavy ? .6f : .3f :
            "red_collector".equals(archetypeId) ? heavy ? .75f : .4f : 1f;
        body.setLinearVelocity((hit.direction == 0f ?
                Math.copySign(recoil, getPosition().x - sourceX) : hit.direction * recoil) * resistance,
            Math.max(body.getLinearVelocity().y, 1.5f * resistance));
        float stagger = "boss".equals(archetypeId) ? Math.min(finisher ? .12f : .05f, hit.staggerDuration) :
            "heavy_hunter".equals(archetypeId) ? hit.staggerDuration * (heavy ? .6f : .12f) : hit.staggerDuration;
        if (buffRemaining > 0f) stagger *= .5f;
        // Ordinary light hits may react and push, but never cancel an enemy attack already in progress.
        if (wasAttacking && hit.type == CombatHit.Type.MELEE_LIGHT) stagger = 0f;
        staggerRemaining = Math.max(staggerRemaining, stagger);
        if (finisher) {
            // The finisher is the sole combo step with hard interrupt authority.
            attackRequested = false;
            lungeRequested = false;
            areaRequested = false;
            slamRequested = false;
            blockRemaining = 0f;
            blockBreakFlashRemaining = .45f;
            cancelRangedAttack();
            currentIntent = Intent.IDLE;
            if (combatStateMachine.getCurrentState() instanceof AttackState)
                combatStateMachine.changeState(new CombatIdleState());
        }
        return true;
    }

    public boolean isCanAttackHero() {
        return canAttackHero;
    }

    public float getAttackStartup() { return attackStartup; }
    public float getAttackActiveEnd() { return attackActiveEnd; }
    public AttackDefinition attackDefinition(boolean lunge, boolean area, boolean slam) {
        if (slam) return new AttackDefinition("breaker_slam", CombatHit.Type.MELEE_HEAVY,
            1.15f, .16f, .6f, 1.7f, 3.4f, .5f, -.55f,
            1.5f, 9f, .35f, 3f, true, 1.9f, 1.9f, .4f);
        if (area) return new AttackDefinition("breaker_sweep", CombatHit.Type.MELEE_HEAVY,
            .8f, .2f, .5f, 2.7f, 2.2f, 1.2f, 0f,
            1.25f, 7f, .3f, 2.5f, true, 1.5f, 1.5f, .3f);
        return attackDefinition(lunge, area);
    }

    public AttackDefinition attackDefinition(boolean lunge, boolean area) {
        if (area) return new AttackDefinition("enemy_area", CombatHit.Type.MELEE_HEAVY,
            .65f, .19f, .24f, 2.4f, 2.4f, 1.2f, 0f,
            1.25f, 5f, .28f, 2.5f, true, 1.08f, 1.08f);
        if (lunge) return new AttackDefinition("enemy_lunge", CombatHit.Type.MELEE_HEAVY,
            .48f, .19f, .21f, 1.9f, 1.25f, 1f, 0f,
            1f, 3f, .22f, 2f, true, .88f, .88f);
        return baseAttackDefinition;
    }

    private final com.badlogic.gdx.math.Rectangle reachBox = new com.badlogic.gdx.math.Rectangle();
    private final com.badlogic.gdx.math.Rectangle reachHurt = new com.badlogic.gdx.math.Rectangle();

    /**
     * True when this enemy's standard-attack hitbox, placed where it would be at the strike, already overlaps the
     * target's hurtbox. This is the same geometry the strike itself uses, so "in reach" never disagrees with a hit.
     */
    public boolean isTargetInStandardReach(Actor target) {
        if (target == null || !target.isAlive()) return false;
        AttackDefinition d = baseAttackDefinition;
        float dir = facingLeft ? -1f : 1f;
        float centreX = getPosition().x + dir * (d.range - d.hitboxWidth / 2f);
        reachBox.set(centreX - d.hitboxWidth / 2f, getPosition().y + d.offsetY - d.hitboxHeight / 2f,
            d.hitboxWidth, d.hitboxHeight);
        target.getHurtboxBounds(reachHurt);
        return reachBox.overlaps(reachHurt);
    }

    /**
     * Centre-to-centre horizontal distance at which the standard hitbox just touches the target's hurtbox:
     * the stopping distance is derived from the weapon's reach and the hero's hurtbox, never from body size.
     */
    public float getAttackApproachDistance(Actor target) {
        target.getHurtboxBounds(reachHurt);
        return baseAttackDefinition.range + reachHurt.width / 2f;
    }

    public void beginAttackCooldown() {
        attackCooldownRemaining = attackCooldown;
    }
    public void beginAttackCooldown(float extra) { attackCooldownRemaining = attackCooldown + extra; }
    public void setCanAttackHero(boolean canAttackHero) {
        this.canAttackHero = canAttackHero;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    public boolean isActive() {
        return active;
    }
    public String getPersistentSpawnId() { return persistentSpawnId; }
    public void setPersistentSpawnId(String value) { persistentSpawnId = value; }

    /*==============================================COMBAT============================================================*/

    public void requestAttack() {
        if (attackCooldownRemaining > 0f || isBlocking() ||
            isStaggered() || isDodging() || isRetreating() || isActionRecovering() ||
            isRangedWindingUp() || combatStateMachine.getCurrentState() instanceof AttackState) return;

        if (isCasting() || !attackPermit || turnGraceRemaining > 0f) return;
        lungeRequested = "default".equals(archetypeId) && nextAttackLunge ||
            "infiltrator".equals(archetypeId) && flankRemaining > 0f;
        slamRequested = "heavy_hunter".equals(archetypeId) && nextSlam;
        areaRequested = "heavy_hunter".equals(archetypeId) ||
            "boss".equals(archetypeId) && nextAreaAttack;
        attackRequested = true;
    }

    public void clearAttackRequest() {
        attackRequested = false;
    }

    public void processCombatRequests() {

        if (!attackRequested) {
            return;
        }

        if (combatStateMachine.getCurrentState() instanceof AttackState) {
            return;
        }

        attackRequested = false;
        if ("scavenger".equals(archetypeId)) {
            // Melee clearly breaks the ranged stance: drop any bow-lowering pose and lost-draw effect.
            rangedRecoveryRemaining = 0f;
            rangedFizzleRemaining = 0f;
            rangedLockFlashRemaining = 0f;
            rangedShotFlashRemaining = 0f;
        }
        combatStateMachine.changeState(new AttackState(lungeRequested, areaRequested, slamRequested));
        if ("infiltrator".equals(archetypeId)) flankRemaining = 0f;
        if ("heavy_hunter".equals(archetypeId)) nextSlam = !nextSlam;
        if ("default".equals(archetypeId)) nextAttackLunge = !nextAttackLunge;
        if ("boss".equals(archetypeId)) nextAreaAttack = !nextAreaAttack;
    }


    /*============================================CONTROLLERS=========================================================*/

    public void moveLeft() {
        moveHorizontal(-1);
    }

    public void moveRight() {
        moveHorizontal(1);
    }
    public void moveStop() {
        moveHorizontal(0);
    }

    public void moveHorizontal(float dir) {

        if (staggerRemaining > 0f) return;

        Vector2 v = body.getLinearVelocity();

        body.setLinearVelocity(
            dir * velocity,
            v.y
        );

        if (dir != 0) {
            facingLeft = dir < 0;
        }
    }

    @Override
    public void update(float delta) {
        if (!active) {
            return;
        }

        attackCooldownRemaining = Math.max(0f, attackCooldownRemaining - delta * (buffRemaining > 0f ? 1.6f : 1f));
        staggerRemaining = Math.max(0f, staggerRemaining - delta);
        jumpCooldownRemaining = Math.max(0f, jumpCooldownRemaining - delta);
        patrolPauseRemaining = Math.max(0f, patrolPauseRemaining - delta);
        // Back to combat stance: after a retreat the archer turns to face the hero again.
        if (retreatRecovering && actionRecoveryRemaining > 0f)
            facingLeft = heroLastKnownPos.x < getPosition().x;
        if (retreatTrailRemaining > 0f) {
            retreatTrailRemaining = Math.max(0f, retreatTrailRemaining - delta);
            if (Math.abs(body.getLinearVelocity().x) > .5f) retreatTrailToX = getPosition().x;
        }

        // Reactions and evasive movement own the actor until they finish. This keeps
        // the priority chain deterministic and prevents attacks from running together.
        if (isStaggered() || isDodging() || isActionRecovering() || isDashPhase() || isCasting()) {
            cancelRangedAttack();
            attackRequested = false;
            currentIntent = Intent.IDLE;
            if (combatStateMachine.getCurrentState() instanceof AttackState)
                combatStateMachine.changeState(new CombatIdleState());
            if (!isDodging() && !isDashing()) moveStop();
            if (isStaggered()) { dashWindupRemaining = 0f; dashRemaining = 0f; cancelCast(CAST_INTERRUPTED_COOLDOWN); }
            return;
        }

        if (brain != null) {
            brain.think(this, null, delta);
        }

        motionStateMachine.applyIntent(currentIntent);
        motionStateMachine.update(delta);

        processCombatRequests();
        combatStateMachine.update(delta);
    }

    @Override
    protected void die() {
        motionStateMachine.changeState(new DeadState());
    }

}
