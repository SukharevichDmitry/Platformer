package com.cursedarchie.platformer.actors.hero;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.hero.machines.CombatStateMachine;
import com.cursedarchie.platformer.actors.hero.machines.MotionStateMachine;
import com.cursedarchie.platformer.physics.AttackConfig;
import com.cursedarchie.platformer.physics.PhysicsTags;
import com.cursedarchie.platformer.physics.PhysicsCategories;
import com.cursedarchie.platformer.run.Inventory;
import com.cursedarchie.platformer.run.ItemCatalog;
import com.cursedarchie.platformer.physics.CombatHit;
import com.cursedarchie.platformer.combat.AttackDefinition;
import com.cursedarchie.platformer.combat.BreakthroughConfig;
import com.cursedarchie.platformer.combat.Hurtbox;
import com.cursedarchie.platformer.actors.hero.states.combat.AttackState;
import com.cursedarchie.platformer.actors.hero.states.combat.CombatIdleState;

public class Hero extends Actor<Hero.HeroMotionState, Hero.HeroCombatState> {
    public static final float BODY_SIZE = 1f;
    public static final float WALK_SPEED = 13f;
    public static final float AIR_ACCELERATION = 150f;
    public static final float AIR_BRAKING = 100f;
    public static final float JUMP_SPEED = 32.8f;
    public static final float FALL_GRAVITY_SCALE = 1.25f;
    public static final float RELEASE_GRAVITY_SCALE = 1.5f;
    public static final float MAX_FALL_SPEED = 28f;
    public static final float ATTACK_STARTUP = 0.12f;
    public static final float ATTACK_ACTIVE_END = 0.24f;
    public static final float ATTACK_DURATION = 0.36f;
    public static final float HEAVY_ATTACK_DURATION = 0.92f;
    public static final float DAMAGE_INVULNERABILITY = 0.5f;

    /*===========================================PARAMETERS===========================================================*/

    public enum HeroMotionState {
        IDLE,
        WALK,
        JUMP,
        DIE
    }

    public enum HeroCombatState {
        IDLE,
        ATTACK
    }

    protected MotionStateMachine motionStateMachine;
    protected CombatStateMachine combatStateMachine;
    private float invulnerabilityRemaining;
    private float staggerRemaining;
    private float lastHitSourceX = Float.NaN;
    private float platformDropRemaining;
    private boolean blockHeld;
    private float blockStability = 3f;
    private float blockBrokenRemaining;
    private float blockCounterCooldown;
    private float blockFlashRemaining;
    private float heavyCooldownRemaining, blockHeavyFlashRemaining, blockBreakFlashRemaining;
    private final AttackConfig firstAttackLeft = new AttackConfig(.9f, .82f, -1.0f, .02f);
    private final AttackConfig firstAttackRight = new AttackConfig(.9f, .82f, 1.0f, .02f);
    private final AttackConfig secondAttackLeft = new AttackConfig(1.32f, .66f, -1.38f, .16f);
    private final AttackConfig secondAttackRight = new AttackConfig(1.32f, .66f, 1.38f, .16f);
    private static final AttackDefinition LIGHT_1 = new AttackDefinition("hero_light_1",
        CombatHit.Type.MELEE_LIGHT, .12f, .12f, .12f,
        1.45f, .9f, .82f, .02f, 1f, 5f, .14f, 1f, true, .25f, .36f);
    private static final AttackDefinition LIGHT_2 = new AttackDefinition("hero_light_2",
        CombatHit.Type.MELEE_LIGHT, .08f, .15f, .23f,
        2.04f, 1.32f, .66f, .16f, 1.1f, 8.5f, .2f, 1f, true, .46f, .46f);
    /** The breakthrough dash: charge (startup), dash (active), recovery; timings come from the config file. */
    private static AttackDefinition heavyDefinition(float chargeTime) {
        BreakthroughConfig c = BreakthroughConfig.get();
        float total = chargeTime + c.dashDuration + c.recoveryTime;
        return new AttackDefinition("hero_heavy", CombatHit.Type.MELEE_HEAVY, chargeTime, c.dashDuration,
            c.recoveryTime, 1.9f, 1.3f, 1.05f, .04f, c.damageMultiplier, c.knockback, .3f, 2f, true, total, total, 0f);
    }
    public AttackDefinition breakthroughDefinition(float chargeTime) { return heavyDefinition(chargeTime); }
    private boolean breakthroughDashing;
    private int breakthroughImpactId;
    private float breakthroughImpactRemaining, breakthroughImpactX, breakthroughImpactY, breakthroughImpactDir;
    public boolean isBreakthroughDashing() { return breakthroughDashing; }
    public void setBreakthroughDashing(boolean value) { breakthroughDashing = value; }
    /** The dash ran into a wall: renderers show an impact at this point. */
    public void noteBreakthroughImpact(float x, float y, float direction) {
        breakthroughImpactId++;
        breakthroughImpactRemaining = .45f;
        breakthroughImpactX = x;
        breakthroughImpactY = y;
        breakthroughImpactDir = direction;
    }
    public int getBreakthroughImpactId() { return breakthroughImpactId; }
    public float getBreakthroughImpactRemaining() { return breakthroughImpactRemaining; }
    public float getBreakthroughImpactX() { return breakthroughImpactX; }
    public float getBreakthroughImpactY() { return breakthroughImpactY; }
    public float getBreakthroughImpactDirection() { return breakthroughImpactDir; }
    public com.cursedarchie.platformer.combat.HeavyState getHeavyState() {
        Object cur = combatStateMachine.getCurrentState();
        if (!(cur instanceof AttackState) || !((AttackState) cur).isHeavy())
            return com.cursedarchie.platformer.combat.HeavyState.NONE;
        switch (((AttackState) cur).getRuntime().phase()) {
            case STARTUP: return com.cursedarchie.platformer.combat.HeavyState.HEAVY_CHARGE;
            case ACTIVE: return com.cursedarchie.platformer.combat.HeavyState.HEAVY_DASH;
            default: return com.cursedarchie.platformer.combat.HeavyState.HEAVY_RECOVERY;
        }
    }
    /** Block cannot be raised while staggered or mid-swing; an already raised guard drops when staggered. */
    public void setBlockHeld(boolean value) {
        boolean wasHeld = blockHeld;
        blockHeld = value && !isRolling() && !isStaggered() && !(combatStateMachine.getCurrentState() instanceof AttackState);
    }
    public boolean isBlocking() {
        return blockHeld && !isStaggered() && blockBrokenRemaining == 0f && blockStability > 0f && !attackLocksBlock();
    }
    /** A heavy attack, or the recovery of a rushed strike, leaves the hero unable to guard. */
    private boolean attackLocksBlock() {
        if (!(combatStateMachine.getCurrentState() instanceof AttackState)) return false;
        AttackState a = (AttackState) combatStateMachine.getCurrentState();
        return a.isHeavy() || a.getRuntime() != null && a.getRuntime().isHurried() && a.isInRecovery(this);
    }
    public boolean canHeavyAttack() { return heavyCooldownRemaining == 0f; }
    public void beginHeavyCooldown() { heavyCooldownRemaining = BreakthroughConfig.get().cooldown; }
    public static final float ROLL_DURATION = .32f, ROLL_INVULNERABLE = .2f, ROLL_COOLDOWN = 1.1f, ROLL_SPEED = 11f;
    public static final float PERFECT_BLOCK_WINDOW = .18f, COUNTER_WINDOW = .7f;
    private float rollRemaining, rollCooldownRemaining, rollDirection, blockAge, counterWindowRemaining, perfectFlashRemaining;
    private boolean jumpAttackUsed;
    public boolean isRolling() { return rollRemaining > 0f; }
    public float getRollDirection() { return rollDirection; }
    public float getRollProgress() { return rollRemaining > 0f ? 1f - rollRemaining / ROLL_DURATION : 0f; }
    public boolean isRollInvulnerable() { return rollRemaining > ROLL_DURATION - ROLL_INVULNERABLE; }
    public float getRollCooldownRemaining() { return rollCooldownRemaining; }
    public boolean isCounterReady() { return counterWindowRemaining > 0f; }
    public float getPerfectFlashRemaining() { return perfectFlashRemaining; }
    public void consumeCounter() { counterWindowRemaining = 0f; }
    public boolean isAirborne() { return !isGrounded(); }
    public boolean canJumpAttack() { return isAirborne() && !jumpAttackUsed && !isStaggered(); }
    public void markJumpAttackUsed() { jumpAttackUsed = true; }
    /** A roll cannot cancel any attack recovery, interrupt a stagger, or be spammed (cooldown). */
    public boolean tryRoll(float direction) {
        if (rollRemaining > 0f || rollCooldownRemaining > 0f || isStaggered() || isBlockBroken() || !isGrounded() ||
            combatStateMachine.getCurrentState() instanceof AttackState) return false;
        rollDirection = direction == 0f ? (facingLeft ? -1f : 1f) : Math.signum(direction);   // fixed at start
        rollRemaining = ROLL_DURATION;
        rollCooldownRemaining = ROLL_COOLDOWN;
        blockHeld = false;
        return true;
    }
    private boolean whiffPending;
    public void markWhiff() { whiffPending = true; }
    public boolean consumeWhiff() { boolean w = whiffPending; whiffPending = false; return w; }
    public float getHeavyCooldownRemaining() { return heavyCooldownRemaining; }
    public float getBlockHeavyFlashRemaining() { return blockHeavyFlashRemaining; }
    public float getBlockBreakFlashRemaining() { return blockBreakFlashRemaining; }
    public com.cursedarchie.platformer.combat.CombatPhase getCombatPhase() {
        if (!alive) return com.cursedarchie.platformer.combat.CombatPhase.DEAD;
        if (isStaggered() || isBlockBroken()) return com.cursedarchie.platformer.combat.CombatPhase.STAGGER;
        if (isRolling()) return com.cursedarchie.platformer.combat.CombatPhase.DODGE;
        Object cur = combatStateMachine.getCurrentState();
        if (cur instanceof AttackState && ((AttackState) cur).getRuntime() != null) {
            switch (((AttackState) cur).getRuntime().phase()) {
                case STARTUP: return com.cursedarchie.platformer.combat.CombatPhase.WINDUP;
                case ACTIVE: return com.cursedarchie.platformer.combat.CombatPhase.ACTIVE;
                default: return com.cursedarchie.platformer.combat.CombatPhase.RECOVERY;
            }
        }
        return isBlocking() ? com.cursedarchie.platformer.combat.CombatPhase.BLOCK :
            com.cursedarchie.platformer.combat.CombatPhase.IDLE;
    }
    /** True while a hero swing is winding up or active: the signal enemies use to decide to guard. */
    public boolean isAttackTelegraphing() {
        Object cur = combatStateMachine.getCurrentState();
        if (!(cur instanceof AttackState)) return false;
        AttackState a = (AttackState) cur;
        return a != null && a.getRuntime() != null &&
            a.getRuntime().phase() != com.cursedarchie.platformer.combat.AttackRuntime.Phase.RECOVERY;
    }
    public boolean isBlockBroken() { return blockBrokenRemaining > 0f; }
    public float getBlockFlashRemaining() { return blockFlashRemaining; }
    public float getBlockStability() { return blockStability; }
    public float getMaxBlockStability() {
        return inventory != null && inventory.count(ItemCatalog.WEIGHTED_BRACER) > 0 ? 3.75f :
            inventory != null && inventory.count(ItemCatalog.RED_BRACER) > 0 ? 3.75f : 3f;
    }
    public boolean receiveHit(CombatHit hit) {
        if (!alive || invulnerabilityRemaining > 0f || isRollInvulnerable()) return false;
        boolean frontal = (hit.sourceX < getPosition().x) == facingLeft;
        if (hit.blockable && frontal && isBlocking() && blockAge < PERFECT_BLOCK_WINDOW) {
            // Perfect block: no damage and no stability cost, the attacker is staggered and a counter opens.
            counterWindowRemaining = COUNTER_WINDOW;
            perfectFlashRemaining = .35f;
            blockFlashRemaining = .18f;
            if (hit.source instanceof com.cursedarchie.platformer.actors.enemies.Enemy)
                ((com.cursedarchie.platformer.actors.enemies.Enemy) hit.source).applyStagger(.55f);
            return true;
        }
        if (hit.blockable && frontal && isBlocking()) {
            blockStability = Math.max(0f, blockStability - hit.stabilityDamage);
            blockFlashRemaining = .18f;
            if (hit.type == CombatHit.Type.MELEE_HEAVY) {
                takeDamage(hit.damage * .5f);
                blockHeavyFlashRemaining = .3f;
                // A strong blow shoves the guard back even though it holds.
                body.setLinearVelocity(Math.copySign(Math.max(3f, hit.recoil * .6f), getPosition().x - hit.sourceX),
                    body.getLinearVelocity().y);
                if (inventory != null && inventory.count(ItemCatalog.WEIGHTED_BRACER) > 0 &&
                    blockCounterCooldown == 0f && hit.source != null) {
                    hit.source.getBody().setLinearVelocity(
                        Math.copySign(3f, hit.sourceX - getPosition().x),
                        hit.source.getBody().getLinearVelocity().y);
                    blockCounterCooldown = 3f;
                }
            }
            if (blockStability == 0f) { blockBrokenRemaining = .85f; blockBreakFlashRemaining = .45f; }
            return true;
        }
        float healthBefore = health;
        takeDamage(hit.damage);
        lastHitSourceX = hit.sourceX;
        if (alive) staggerRemaining = Math.max(staggerRemaining, hit.staggerDuration);
        // A hit breaks off a swing, a charge or a recovery; a dash in full flight carries on (no free cancel,
        // but also no free invulnerability: the damage above still lands).
        com.cursedarchie.platformer.combat.HeavyState heavyState = getHeavyState();
        boolean heavyHit = hit.type == CombatHit.Type.MELEE_HEAVY ||
            hit.staggerDuration >= com.cursedarchie.platformer.combat.BreakthroughConfig.get().interruptStagger;
        boolean keepsHeavy = heavyState == com.cursedarchie.platformer.combat.HeavyState.HEAVY_DASH ||
            heavyState == com.cursedarchie.platformer.combat.HeavyState.HEAVY_CHARGE && !heavyHit;
        if (combatStateMachine.getCurrentState() instanceof AttackState && (!alive || !keepsHeavy))
            combatStateMachine.changeState(new CombatIdleState());
        if (hit.recoil > 0f && alive && !keepsHeavy)
            body.setLinearVelocity(hit.direction == 0f ?
                    Math.copySign(hit.recoil, getPosition().x - hit.sourceX) : hit.direction * hit.recoil,
                body.getLinearVelocity().y);
        return health < healthBefore;
    }
    public float getPlatformDropRemaining() { return platformDropRemaining; }
    public void setPlatformDropRemaining(float seconds) { platformDropRemaining = Math.max(0f, seconds); }
    private Inventory inventory;

    /*===========================================CONSTRUCTOR==========================================================*/

    public Hero(Vector2 pos, World world) {
        super();

        //PARAMETERS=============================================================

        this.motionStateMachine = new MotionStateMachine(this);
        this.combatStateMachine = new CombatStateMachine(this);
        this.motionState = HeroMotionState.IDLE;
        this.combatState = HeroCombatState.IDLE;
        this.motionStateTime = 0f;
        this.combatStateTime = 0f;
        this.size = BODY_SIZE;
        this.maxHealth = 3;
        this.health = maxHealth;
        this.velocity = WALK_SPEED;
        this.jumpForce = JUMP_SPEED;
        this.alive = true;
        this.facingLeft = false;
        this.attackHandled = false;
        this.damage = 1f;
        this.attackDuration = ATTACK_DURATION;
        this.attackLeft = firstAttackLeft;
        this.attackRight = firstAttackRight;
        this.hurtbox = new Hurtbox(.78f, .94f, 0f, .02f);

        //BODY===================================================================

        createBody(
            world,
            pos,
            PhysicsCategories.HERO,
            (short)(
                PhysicsCategories.WALL |
                PhysicsCategories.GROUND |
                PhysicsCategories.ENEMY |
                PhysicsCategories.ENEMY_ATTACK |
                PhysicsCategories.VISION
            )
        );

        //FOOT_SENSOR============================================================

        createFootSensor(
            PhysicsCategories.HERO,
            (short)(
                PhysicsCategories.WALL |
                PhysicsCategories.GROUND
            )
        );

        //ATTACK_SENSORS=========================================================

        createAttackSensors(
            PhysicsTags.HERO_ATTACK_SENSOR,
            PhysicsCategories.HERO_ATTACK,
            PhysicsCategories.ENEMY,
            secondAttackLeft,
            secondAttackRight
        );

        //=======================================================================
    }

    /*=========================================GETTERS/SETTERS========================================================*/

    public MotionStateMachine getMotionStateMachine() {
        return motionStateMachine;
    }
    public CombatStateMachine getCombatStateMachine() {
        return combatStateMachine;
    }
    public float getInvulnerabilityRemaining() { return invulnerabilityRemaining; }
    private int rhythmCombos;
    /** Counts clean, correctly timed combo presses; the timing highlight fades as this grows. */
    public void noteRhythmSuccess() { rhythmCombos++; }
    public int getRhythmCombos() { return rhythmCombos; }
    public boolean isStaggered() { return staggerRemaining > 0f; }
    public float getLastHitSourceX() { return lastHitSourceX; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public float meleeDamageMultiplier(boolean heavy, int comboStep) {
        if (inventory == null) return 1f;
        boolean fused = inventory.count(ItemCatalog.FUSED_RELIC) > 0;
        if (!fused && inventory.count(ItemCatalog.BLADE_SHARD) == 0) return 1f;
        float base = ItemCatalog.bladeShard().bonus;
        float bonus = heavy ? base * 3f : comboStep == 2 ? base * 2f : base;
        if (fused && heavy) bonus += ItemCatalog.fusedRelic().bonus;
        return 1f + bonus;
    }
    public float meleeRecoilBonus() {
        return inventory != null && (inventory.count(ItemCatalog.IMPACT_CORE) > 0 ||
            inventory.count(ItemCatalog.FUSED_RELIC) > 0) ?
            ItemCatalog.impactCore().bonus : 0f;
    }

    private static final AttackDefinition COUNTER = new AttackDefinition("hero_counter",
        CombatHit.Type.MELEE_HEAVY, .08f, .14f, .3f,
        1.7f, 1.2f, .9f, .02f, 1.5f, 8f, .6f, 1.5f, true, .6f, .52f, .3f);
    private static final AttackDefinition JUMP_ATTACK = new AttackDefinition("hero_jump_attack",
        CombatHit.Type.MELEE_LIGHT, .08f, .16f, .1f,
        1.25f, .85f, .8f, -.3f, .9f, 4f, .12f, 1f, true, .34f, .34f, .5f);
    public AttackDefinition attackDefinition(boolean heavy, int comboStep) {
        if (!heavy && comboStep == 3) return COUNTER;
        if (!heavy && comboStep == 4) return JUMP_ATTACK;
        BreakthroughConfig cfg = BreakthroughConfig.get();
        return heavy ? heavyDefinition(cfg.chargeTimeForDistance(cfg.dashDistance)) : comboStep == 2 ? LIGHT_2 : LIGHT_1;
    }

    /** The two light strikes intentionally use different logical hitboxes. */
    public boolean isTargetInAttack(Actor target, boolean attackFacesLeft, int comboStep) {
        AttackConfig attack = comboStep == 2 ?
            (attackFacesLeft ? secondAttackLeft : secondAttackRight) :
            (attackFacesLeft ? firstAttackLeft : firstAttackRight);
        return isTargetInAttack(target, attack);
    }

    /*==========================================CONTROLLERS===========================================================*/

    @Override
    public void update(float delta) {
        blockBrokenRemaining = Math.max(0f, blockBrokenRemaining - delta);
        blockCounterCooldown = Math.max(0f, blockCounterCooldown - delta);
        blockFlashRemaining = Math.max(0f, blockFlashRemaining - delta);
        heavyCooldownRemaining = Math.max(0f, heavyCooldownRemaining - delta);
        breakthroughImpactRemaining = Math.max(0f, breakthroughImpactRemaining - delta);
        rollCooldownRemaining = Math.max(0f, rollCooldownRemaining - delta);
        rollRemaining = Math.max(0f, rollRemaining - delta);
        counterWindowRemaining = Math.max(0f, counterWindowRemaining - delta);
        perfectFlashRemaining = Math.max(0f, perfectFlashRemaining - delta);
        blockAge = blockHeld ? blockAge + delta : 0f;
        if (isGrounded()) jumpAttackUsed = false;
        blockHeavyFlashRemaining = Math.max(0f, blockHeavyFlashRemaining - delta);
        blockBreakFlashRemaining = Math.max(0f, blockBreakFlashRemaining - delta);
        if (isStaggered()) blockHeld = false;
        if (!blockHeld && blockBrokenRemaining == 0f)
            blockStability = Math.min(getMaxBlockStability(), blockStability + 1.15f * delta);
        invulnerabilityRemaining = Math.max(0f, invulnerabilityRemaining - delta);
        staggerRemaining = Math.max(0f, staggerRemaining - delta);
        motionStateMachine.update(delta);
        combatStateMachine.update(delta);
    }

    @Override
    public void takeDamage(float damage) {
        if (!alive || invulnerabilityRemaining > 0f) return;
        super.takeDamage(damage);
        if (alive) invulnerabilityRemaining = DAMAGE_INVULNERABILITY;
    }

    @Override
    protected void die() {
        this.setAlive(false);
    }
}
