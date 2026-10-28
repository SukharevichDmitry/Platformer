package com.cursedarchie.platformer.actors.hero;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.hero.machines.CombatStateMachine;
import com.cursedarchie.platformer.actors.hero.machines.MotionStateMachine;
import com.cursedarchie.platformer.physics.AttackConfig;
import com.cursedarchie.platformer.physics.PhysicsTags;
import com.cursedarchie.platformer.physics.PhysicsCategories;

public class Hero extends Actor<Hero.HeroMotionState, Hero.HeroCombatState> {
    public static final float BODY_SIZE = 1f;
    public static final float WALK_SPEED = 13f;
    public static final float GROUND_ACCELERATION = 150f;
    public static final float GROUND_BRAKING = 170f;
    public static final float AIR_ACCELERATION = 90f;
    public static final float AIR_BRAKING = 44f;
    public static final float JUMP_SPEED = 32.8f;
    public static final float FALL_GRAVITY_SCALE = 1.25f;
    public static final float RELEASE_GRAVITY_SCALE = 1.5f;
    public static final float MAX_FALL_SPEED = 28f;
    public static final float ATTACK_STARTUP = 0.12f;
    public static final float ATTACK_ACTIVE_END = 0.24f;
    public static final float ATTACK_DURATION = 0.36f;
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

    /*==========================================CONTROLLERS===========================================================*/

    @Override
    public void update(float delta) {
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
