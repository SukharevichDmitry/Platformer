package com.cursedarchie.platformer.actors.enemies;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.enemies.logic.EnemyBrain;
import com.cursedarchie.platformer.actors.enemies.machines.MotionStateMachine;
import com.cursedarchie.platformer.actors.enemies.states.combat.AttackState;
import com.cursedarchie.platformer.actors.enemies.states.motion.*;
import com.cursedarchie.platformer.actors.enemies.machines.CombatStateMachine;
import com.cursedarchie.platformer.physics.PhysicsTags;
import com.cursedarchie.platformer.physics.PhysicsCategories;

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
        CHASE
    }

    private Intent currentIntent = null;
    private EnemyBrain brain;
    private boolean canSeeHero = false;
    private boolean canAttackHero = false;
    private boolean attackRequested;
    private Vector2 heroLastKnownPos = new Vector2();
    protected MotionStateMachine motionStateMachine;
    protected CombatStateMachine combatStateMachine;


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
        this.velocity = cfg.velocity;

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
                PhysicsCategories.HERO |
                PhysicsCategories.GROUND |
                PhysicsCategories.ENEMY |
                PhysicsCategories.HERO_ATTACK |
                PhysicsCategories.VISION
            )
        );

        //FOOT_SENSOR============================================================

        createFootSensor(
            PhysicsCategories.ENEMY,
            (short)(
                PhysicsCategories.WALL |
                PhysicsCategories.GROUND |
                PhysicsCategories.HERO |
                PhysicsCategories.ENEMY
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
        this.heroLastKnownPos = heroLastKnownPos;
    }

    public boolean isCanAttackHero() {
        return canAttackHero;
    }
    public void setCanAttackHero(boolean canAttackHero) {
        this.canAttackHero = canAttackHero;
    }


    /*==============================================COMBAT============================================================*/

    public void requestAttack() {
        if (combatStateMachine.getCurrentState() instanceof AttackState) return;

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
        combatStateMachine.changeState(new AttackState());
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
        if (body != null) {
            syncCollisionRects();
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
