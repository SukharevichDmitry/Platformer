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
    public boolean isRangedShotFacingLeft() { return rangedShotFacingLeft; }
    public Vector2 getRangedAim() { return rangedAim; }
    public float getRangedWindupProgress() { return rangedWindupDuration == 0f ? 0f :
        1f - rangedWindupRemaining / rangedWindupDuration; }
    public int getRangedShotMode() { return rangedShotMode; }
    public float getRangedShotFlashRemaining() { return rangedShotFlashRemaining; }
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
            if (rangedWindupRemaining > 0f) {
                rangedWindupRemaining -= delta;
                if (rangedWindupRemaining <= 0f) {
                    rangedWindupRemaining = 0f;
                    rangedCooldownRemaining = 2.2f;
                    rangedShotFlashRemaining = .12f;
                    if (!isStaggered()) {
                        if (gameWorld != null) gameWorld.fireArrowVolley(this, rangedAim,
                            rangedShotMode);
                        else if (hero.isAlive() && Math.abs(dx) < 7f && dy < 1.25f &&
                            (dx < 0f) == rangedShotFacingLeft)
                            hero.receiveHit(new CombatHit(CombatHit.Type.PROJECTILE, .5f,
                                getPosition().x, 0f, 0f, this));
                    }
                }
            } else if (rangedCooldownRemaining == 0f && canSeeHero &&
                Math.abs(dx) > 2.4f && Math.abs(dx) < 7f && dy < 1.25f) {
                rangedShotMode = ++rangedShotSequence % 5 == 0 ? 2 :
                    rangedShotSequence % 3 == 0 ? 1 : 0;
                rangedWindupDuration = rangedShotMode == 2 ? 1.2f :
                    rangedShotMode == 1 ? 1.05f : .7f;
                rangedWindupRemaining = rangedWindupDuration;
                rangedAim.set(hero.getPosition()).sub(getPosition()).nor();
                rangedShotFacingLeft = dx < 0f;
                moveStop();
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
                body.setLinearVelocity(away * 9f, body.getLinearVelocity().y);
                dodgeCooldownRemaining = 2.5f;
                dodgeFlashRemaining = .22f;
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
