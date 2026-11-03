package com.cursedarchie.platformer.actors.enemies;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.enemies.logic.EnemyBrain;
import com.cursedarchie.platformer.actors.enemies.machines.MotionStateMachine;
import com.cursedarchie.platformer.actors.enemies.states.combat.AttackState;
import com.cursedarchie.platformer.actors.enemies.states.motion.*;
import com.cursedarchie.platformer.actors.enemies.machines.CombatStateMachine;
import com.cursedarchie.platformer.physics.PhysicsTags;
import com.cursedarchie.platformer.physics.PhysicsCategories;
import com.cursedarchie.platformer.physics.CombatHit;
import com.cursedarchie.platformer.world.GameWorld;

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
        RETREAT_DASH, DODGE
    }

    /** Fraction of the draw after which the aim direction is frozen. */
    public static final float RANGED_AIM_LOCK_AT = .5f;
    /** Seconds before release during which the "about to fire" cue is shown. */
    public static final float RANGED_RELEASE_CUE_TIME = .14f;
    public static final float DODGE_EFFECT_DURATION = .5f;
    public static final float DODGE_TINT_DURATION = .22f;
    public static final float RETREAT_TELEGRAPH_TIME = .2f;
    public static final float RETREAT_DASH_TIME = .18f;
    public static final float RETREAT_RECOVER_TIME = .28f;
    public static final float RETREAT_TRAIL_FADE = .35f;
    private static final float RETREAT_DASH_SPEED = 11f;
    private static final float RETREAT_REPEAT_DELAY = .9f;
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
    private float attackCooldownRemaining;
    private float staggerRemaining;
    private float jumpCooldownRemaining;
    private float rangedCooldownRemaining = .8f;
    private float rangedWindupRemaining;
    private float rangedWindupDuration;
    private final Vector2 rangedAim = new Vector2();
    private int rangedShotSequence;
    private int rangedShotMode;
    private boolean rangedShotFacingLeft;
    private float rangedShotFlashRemaining;
    private float rangedShotFlashDuration = .12f;
    private float rangedRecoveryRemaining;
    private float rangedRecoveryDuration;
    private float rangedLockFlashRemaining;
    private float rangedFizzleRemaining;
    private RetreatPhase retreatPhase = RetreatPhase.NONE;
    private float retreatPhaseTime;
    private float retreatRepeatRemaining;
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
    private float blockCooldownRemaining = .8f;
    private float dodgeCooldownRemaining;
    private float dodgeFlashRemaining;
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
            PhysicsCategories.HERO
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
    public boolean isStaggered() { return staggerRemaining > 0f; }
    public boolean isRangedWindingUp() { return rangedWindupRemaining > 0f; }
    public boolean isRangedRecovering() { return rangedRecoveryRemaining > 0f; }
    /** True from the first frame of the draw until the bow is lowered again. */
    public boolean isRangedBusy() { return isRangedWindingUp() || isRangedRecovering(); }
    public boolean isRangedShotFacingLeft() { return rangedShotFacingLeft; }
    public Vector2 getRangedAim() { return rangedAim; }
    public float getRangedWindupProgress() { return rangedWindupDuration == 0f ? 0f :
        1f - rangedWindupRemaining / rangedWindupDuration; }
    /** 0 = standard shot, 1 = charged shot, 2 = three-arrow volley. */
    public int getRangedShotMode() { return rangedShotMode; }
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

    public RetreatPhase getRetreatPhase() { return retreatPhase; }
    public boolean isRetreatActive() { return retreatPhase != RetreatPhase.NONE; }
    public float getRetreatPhaseProgress() {
        float length = retreatPhase == RetreatPhase.TELEGRAPH ? RETREAT_TELEGRAPH_TIME :
            retreatPhase == RetreatPhase.DASH ? RETREAT_DASH_TIME :
            retreatPhase == RetreatPhase.RECOVER ? RETREAT_RECOVER_TIME : 0f;
        return length == 0f ? 0f : Math.min(1f, retreatPhaseTime / length);
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
    public float getDodgeFlashRemaining() { return dodgeFlashRemaining; }
    public boolean isNextAttackLunge() { return nextAttackLunge; }
    public boolean isAttacking() { return combatStateMachine.getCurrentState() instanceof AttackState; }
    public float getSupportFlashRemaining() { return supportFlashRemaining; }
    public void updateSpecialActions(Hero hero, Array<Enemy> allies, float delta) {
        updateSpecialActions(null, hero, allies, delta);
    }
    public void updateSpecialActions(GameWorld gameWorld, Hero hero, Array<Enemy> allies, float delta) {
        if (!active || !alive) return;
        rangedCooldownRemaining = Math.max(0f, rangedCooldownRemaining - delta);
        rangedShotFlashRemaining = Math.max(0f, rangedShotFlashRemaining - delta);
        blockRemaining = Math.max(0f, blockRemaining - delta);
        blockCooldownRemaining = Math.max(0f, blockCooldownRemaining - delta);
        dodgeCooldownRemaining = Math.max(0f, dodgeCooldownRemaining - delta);
        dodgeFlashRemaining = Math.max(0f, dodgeFlashRemaining - delta);
        supportCooldownRemaining = Math.max(0f, supportCooldownRemaining - delta);
        supportFlashRemaining = Math.max(0f, supportFlashRemaining - delta);
        if ("scavenger".equals(archetypeId)) {
            float dx = hero.getPosition().x - getPosition().x;
            float dy = Math.abs(hero.getPosition().y - getPosition().y);
            rangedRecoveryRemaining = Math.max(0f, rangedRecoveryRemaining - delta);
            rangedLockFlashRemaining = Math.max(0f, rangedLockFlashRemaining - delta);
            rangedFizzleRemaining = Math.max(0f, rangedFizzleRemaining - delta);
            if (rangedWindupRemaining > 0f) {
                // Standard shot sequence: stop -> draw (aim follows) -> aim locked -> release cue -> fire -> recovery.
                boolean aimWasOpen = getRangedWindupProgress() < RANGED_AIM_LOCK_AT;
                if (aimWasOpen) trackAim(hero);
                rangedWindupRemaining -= delta;
                if (rangedWindupRemaining <= 0f) {
                    releaseRangedShot(gameWorld, hero, dx, dy);
                } else if (aimWasOpen && getRangedWindupProgress() >= RANGED_AIM_LOCK_AT) {
                    rangedLockFlashRemaining = .14f;
                }
            } else if (rangedCooldownRemaining == 0f && rangedRecoveryRemaining == 0f &&
                !isRetreatActive() && !isAttacking() && canSeeHero &&
                Math.abs(dx) > 2.4f && Math.abs(dx) < 7f && dy < 1.25f) {
                rangedShotMode = ++rangedShotSequence % 5 == 0 ? 2 :
                    rangedShotSequence % 3 == 0 ? 1 : 0;
                rangedWindupDuration = rangedShotMode == 2 ? 1.2f :
                    rangedShotMode == 1 ? 1.05f : .7f;
                rangedWindupRemaining = rangedWindupDuration;
                rangedFizzleRemaining = 0f;
                trackAim(hero);
                moveStop();
                emitCue(rangedShotMode == 2 ? Cue.BOW_DRAW_VOLLEY :
                    rangedShotMode == 1 ? Cue.BOW_DRAW_CHARGED : Cue.BOW_DRAW_STANDARD);
            }
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
    /** Aim follows the hero only until the lock point; afterwards the shot direction is fixed and dodgeable. */
    private void trackAim(Hero hero) {
        rangedAim.set(hero.getPosition()).sub(getPosition()).nor();
        rangedShotFacingLeft = hero.getPosition().x < getPosition().x;
        facingLeft = rangedShotFacingLeft;
    }

    private void releaseRangedShot(GameWorld gameWorld, Hero hero, float dx, float dy) {
        rangedWindupRemaining = 0f;
        rangedCooldownRemaining = 2.2f;
        if (isStaggered()) {
            // A staggered archer loses the shot: show a broken-off draw instead of a muzzle flash.
            rangedFizzleRemaining = .3f;
            rangedRecoveryDuration = .2f;
            rangedRecoveryRemaining = rangedRecoveryDuration;
            return;
        }
        rangedShotFlashDuration = rangedShotMode == 2 ? .22f : rangedShotMode == 1 ? .18f : .1f;
        rangedShotFlashRemaining = rangedShotFlashDuration;
        rangedRecoveryDuration = rangedShotMode == 2 ? .5f : rangedShotMode == 1 ? .42f : .28f;
        rangedRecoveryRemaining = rangedRecoveryDuration;
        emitCue(rangedShotMode == 2 ? Cue.SHOT_VOLLEY :
            rangedShotMode == 1 ? Cue.SHOT_CHARGED : Cue.SHOT_STANDARD);
        if (gameWorld != null) gameWorld.fireArrowVolley(this, rangedAim, rangedShotMode);
        else if (hero.isAlive() && Math.abs(dx) < 7f && dy < 1.25f &&
            (dx < 0f) == rangedShotFacingLeft)
            hero.receiveHit(new CombatHit(CombatHit.Type.PROJECTILE, .5f,
                getPosition().x, 0f, 0f, this));
    }

    /*==========================================RETREAT CYCLE=========================================================*/

    public void beginRetreat() {
        // Re-entering while still skidding out of a dash must not restart the cycle.
        if (retreatPhase == RetreatPhase.RECOVER) return;
        retreatPhase = RetreatPhase.NONE;
        retreatPhaseTime = 0f;
    }

    /**
     * Called every tick while the motion state is RETREAT. {@code away} is the direction away from the hero.
     * Cycle: telegraph (hold still, lean back) -> dash backwards -> skid back into stance (see {@link #update}).
     * Between cycles the archer simply backpedals while facing the hero.
     */
    public void tickRetreat(float away, float delta) {
        switch (retreatPhase) {
            case NONE:
                if (retreatRepeatRemaining == 0f && hasGroundAhead(away)) {
                    retreatPhase = RetreatPhase.TELEGRAPH;
                    retreatPhaseTime = 0f;
                    moveStop();
                } else if (hasGroundAhead(away)) moveHorizontal(away);
                else moveStop();
                break;
            case TELEGRAPH:
                moveStop();
                if (retreatPhaseTime >= RETREAT_TELEGRAPH_TIME) {
                    if (hasGroundAhead(away)) {
                        retreatPhase = RetreatPhase.DASH;
                        retreatPhaseTime = 0f;
                        beginTrail(away, false);
                        emitCue(Cue.RETREAT_DASH);
                    } else finishRetreatCycle();
                }
                break;
            case DASH:
                if (!isStaggered())
                    body.setLinearVelocity(away * RETREAT_DASH_SPEED, body.getLinearVelocity().y);
                if (retreatPhaseTime >= RETREAT_DASH_TIME || !hasGroundAhead(away)) {
                    retreatPhase = RetreatPhase.RECOVER;
                    retreatPhaseTime = 0f;
                }
                break;
            case RECOVER:
                // No velocity writes here: damping brakes the dash, so the archer skids to a stop.
                break;
        }
        // Always keep facing the hero (the combat stance), never the direction of travel.
        facingLeft = away > 0f;
    }

    /** Leaving the RETREAT state cancels a telegraph but lets a dash finish as a skid back into stance. */
    public void endRetreat() {
        if (retreatPhase == RetreatPhase.TELEGRAPH) {
            retreatPhase = RetreatPhase.NONE;
            retreatPhaseTime = 0f;
        } else if (retreatPhase == RetreatPhase.DASH) {
            retreatPhase = RetreatPhase.RECOVER;
            retreatPhaseTime = 0f;
        }
    }

    private void finishRetreatCycle() {
        retreatPhase = RetreatPhase.NONE;
        retreatPhaseTime = 0f;
        retreatRepeatRemaining = RETREAT_REPEAT_DELAY;
    }

    private void beginTrail(float direction, boolean dodge) {
        retreatTrailFromX = getPosition().x;
        retreatTrailToX = getPosition().x;
        retreatTrailDirection = direction;
        retreatTrailDodge = dodge;
        retreatTrailRemaining = (dodge ? .3f : RETREAT_DASH_TIME + RETREAT_RECOVER_TIME) + RETREAT_TRAIL_FADE;
    }

    public boolean isRetreatRecovering() { return retreatPhase == RetreatPhase.RECOVER; }

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
    public void takeMeleeHit(CombatHit hit) {
        if (!alive) return;
        if ("scavenger".equals(archetypeId) && hit.type == CombatHit.Type.MELEE_LIGHT &&
            dodgeCooldownRemaining == 0f && !isStaggered()) {
            float away = hit.sourceX < getPosition().x ? 1f : -1f;
            if (hasGroundAhead(away)) {
                // Evasion: remember where the blow landed so the renderer can show a whiffed strike.
                dodgeOriginX = getPosition().x;
                dodgeOriginY = getPosition().y;
                dodgeDirection = away;
                body.setLinearVelocity(away * 9f, body.getLinearVelocity().y);
                dodgeCooldownRemaining = 2.5f;
                dodgeFlashRemaining = DODGE_EFFECT_DURATION;
                retreatRepeatRemaining = Math.max(retreatRepeatRemaining, .6f);
                beginTrail(away, true);
                facingLeft = away > 0f;
                emitCue(Cue.DODGE);
                return;
            }
        }
        float amount = hit.damage;
        float sourceX = hit.sourceX;
        float recoil = hit.recoil;
        boolean heavy = hit.type == CombatHit.Type.MELEE_HEAVY;
        if (heavy && isBlocking()) blockRemaining = 0f;
        if (!heavy && "red_collector".equals(archetypeId) && isBlocking() && !isStaggered() &&
            ((sourceX < getPosition().x) == facingLeft) &&
            !(combatStateMachine.getCurrentState() instanceof AttackState)) {
            amount *= .5f;
            recoil *= .25f;
            blockRemaining = 0f;
        }
        takeDamage(amount);
        if (!alive) return;
        float resistance = "boss".equals(archetypeId) ? .1f :
            "red_collector".equals(archetypeId) ? heavy ? .75f : .4f : 1f;
        body.setLinearVelocity(Math.copySign(recoil * resistance, getPosition().x - sourceX),
            Math.max(body.getLinearVelocity().y, 1.5f * resistance));
        staggerRemaining = "boss".equals(archetypeId) ? .05f : heavy ? .24f : .14f;
    }

    public boolean isCanAttackHero() {
        return canAttackHero;
    }

    public float getAttackStartup() { return attackStartup; }
    public float getAttackActiveEnd() { return attackActiveEnd; }

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
            combatStateMachine.getCurrentState() instanceof AttackState) return;

        lungeRequested = "default".equals(archetypeId) && nextAttackLunge;
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
        combatStateMachine.changeState(new AttackState(lungeRequested, areaRequested));
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
        // Let the retreat dash skid to a halt instead of being cut off by a stop command.
        if (dir == 0f && retreatPhase == RetreatPhase.RECOVER) return;

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

        attackCooldownRemaining = Math.max(0f, attackCooldownRemaining - delta);
        staggerRemaining = Math.max(0f, staggerRemaining - delta);
        jumpCooldownRemaining = Math.max(0f, jumpCooldownRemaining - delta);
        patrolPauseRemaining = Math.max(0f, patrolPauseRemaining - delta);
        retreatRepeatRemaining = Math.max(0f, retreatRepeatRemaining - delta);
        if (retreatPhase != RetreatPhase.NONE) {
            retreatPhaseTime += delta;
            if (retreatPhase == RetreatPhase.RECOVER) {
                // Back to combat stance: turn to the hero while the dash momentum bleeds off.
                facingLeft = heroLastKnownPos.x < getPosition().x;
                if (retreatPhaseTime >= RETREAT_RECOVER_TIME) finishRetreatCycle();
            }
        }
        if (retreatTrailRemaining > 0f) {
            retreatTrailRemaining = Math.max(0f, retreatTrailRemaining - delta);
            if (Math.abs(body.getLinearVelocity().x) > .5f) retreatTrailToX = getPosition().x;
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
