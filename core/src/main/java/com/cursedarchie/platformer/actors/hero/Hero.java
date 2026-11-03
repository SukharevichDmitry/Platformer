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
    private float platformDropRemaining;
    private boolean blockHeld;
    private float blockStability = 3f;
    private float blockBrokenRemaining;
    private float blockCounterCooldown;
    private float blockFlashRemaining;
    public void setBlockHeld(boolean value) { blockHeld = value; }
    public boolean isBlocking() { return blockHeld && blockBrokenRemaining == 0f && blockStability > 0f; }
    public boolean isBlockBroken() { return blockBrokenRemaining > 0f; }
    public float getBlockFlashRemaining() { return blockFlashRemaining; }
    public float getBlockStability() { return blockStability; }
    public float getMaxBlockStability() {
        return inventory != null && inventory.count(ItemCatalog.WEIGHTED_BRACER) > 0 ? 3.75f :
            inventory != null && inventory.count(ItemCatalog.RED_BRACER) > 0 ? 3.75f : 3f;
    }
    public void receiveHit(CombatHit hit) {
        boolean frontal = (hit.sourceX < getPosition().x) == facingLeft;
        if (hit.type != CombatHit.Type.PROJECTILE && frontal && isBlocking()) {
            blockStability = Math.max(0f, blockStability - hit.stabilityDamage);
            blockFlashRemaining = .18f;
            if (hit.type == CombatHit.Type.MELEE_HEAVY) {
                takeDamage(hit.damage * .5f);
                if (inventory != null && inventory.count(ItemCatalog.WEIGHTED_BRACER) > 0 &&
                    blockCounterCooldown == 0f && hit.source != null) {
                    hit.source.getBody().setLinearVelocity(
                        Math.copySign(3f, hit.sourceX - getPosition().x),
                        hit.source.getBody().getLinearVelocity().y);
                    blockCounterCooldown = 3f;
                }
            }
            if (blockStability == 0f) blockBrokenRemaining = .85f;
            return;
        }
        takeDamage(hit.damage);
        if (hit.recoil > 0f && alive)
            body.setLinearVelocity(Math.copySign(hit.recoil, getPosition().x - hit.sourceX),
                body.getLinearVelocity().y);
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
        this.attackLeft = new AttackConfig(0.8f, 0.8f, -1f, 0f);
        this.attackRight = new AttackConfig(0.8f, 0.8f, 1f, 0f);

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
            PhysicsCategories.ENEMY
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

    /*==========================================CONTROLLERS===========================================================*/

    @Override
    public void update(float delta) {
        blockBrokenRemaining = Math.max(0f, blockBrokenRemaining - delta);
        blockCounterCooldown = Math.max(0f, blockCounterCooldown - delta);
        blockFlashRemaining = Math.max(0f, blockFlashRemaining - delta);
        if (!blockHeld && blockBrokenRemaining == 0f)
            blockStability = Math.min(getMaxBlockStability(), blockStability + 1.15f * delta);
        invulnerabilityRemaining = Math.max(0f, invulnerabilityRemaining - delta);
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
