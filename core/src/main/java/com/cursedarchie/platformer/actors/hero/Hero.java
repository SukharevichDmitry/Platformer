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
        this.size = 0.5f;
        this.maxHealth = 3;
        this.health = maxHealth;
        this.velocity = 5f;
        this.jumpForce = 1f;
        this.alive = true;
        this.facingLeft = false;
        this.attackHandled = false;
        this.damage = 1f;
        this.attackDuration = 0.6f;
        this.attackLeft = new AttackConfig(0.4f, 0.4f, -0.5f, 0f);
        this.attackRight = new AttackConfig(0.4f, 0.4f, 0.5f, 0f);

        //BODY===================================================================

        createBody(
            world,
            pos,
            PhysicsCategories.HERO,
            (short)(
                PhysicsCategories.WALL |
                PhysicsCategories.ENEMY |
                PhysicsCategories.GROUND |
                PhysicsCategories.ENEMY_ATTACK |
                PhysicsCategories.VISION
            )
        );

        //FOOT_SENSOR============================================================

        createFootSensor(
            PhysicsCategories.HERO,
            (short)(
                PhysicsCategories.WALL |
                PhysicsCategories.GROUND |
                PhysicsCategories.ENEMY
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

    /*==========================================CONTROLLERS===========================================================*/

    @Override
    public void update(float delta) {
        if (body != null) {
            syncCollisionRects();
        }

        motionStateMachine.update(delta);
        combatStateMachine.update(delta);
    }

    @Override
    protected void die() {
        this.setAlive(false);
    }
}
