package com.cursedarchie.platformer.actors;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool;
import com.cursedarchie.platformer.actors.enemies.logic.EnemyStateMachine;
import com.cursedarchie.platformer.actors.enemies.logic.states.DeadState;

public abstract class Enemy extends Actor<Enemy.EnemyState> {

     public enum EnemyState{
        IDLE,
        PATROL,
        CHASE,
        ATTACK,
        RETREAT,
        DYING
     }

    /**
     * Enemy attributes... (open to details)
     *
     * @param EnemyState enum of states that enemy can have
     * @param grounded flag that indicates isn't enemy falling
     * @param damageDealt flag that indicates is enemy end his attack
     * @param canSeeHero flag that indicates is enemy can see hero
     * @param attackTime current time of attack animation
     */
    private float size;
    private float maxHealth;
    private float attackDuration;
    private float viewDistance;
    private float viewAngle;
    private float maxAcceleration;
    private boolean grounded = false;
    private boolean damageDealt = false;
    private boolean canSeeHero = false;
    private boolean canAttackHero = false;
    private boolean canDealDamage = false;



    private float attackTime = 0f;
    private Vector2 heroLastKnownPos = new Vector2();

    private final Pool<Rectangle> rectPool = new Pool<Rectangle>() {
        @Override
        protected Rectangle newObject() {
            return new Rectangle();
        }
    };

    private final EnemyStateMachine stateMachine;

    public Enemy(Vector2 pos, float size, float maxHealth) {
        this.size = size;
        this.position.set(pos);
        this.bounds.x = pos.x;
        this.bounds.y = pos.y;
        this.bounds.width = size;
        this.bounds.height = size;
        this.state = EnemyState.IDLE;
        this.stateTime = 0f;
        this.health = maxHealth;
        this.facingLeft = true;
        this.alive = true;
        this.stateMachine = new EnemyStateMachine(this);
    }

    /*===========================================GETTERS/SETTERS===================================================*/
    @Override
    public float getMaxHealth() {
        return maxHealth;
    }
    public void setMaxHealth(float maxHealth) {
        this.maxHealth = maxHealth;
    }

    /**
     * @return This method return true when enemy stay on any block
     */
    public boolean isGrounded() {
        return grounded;
    }
    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
    }

    /**
     * @return size. this.bounds.width = this.bounds.height = size
     */
    public float getSize() {
        return this.size;
    }
    public void setSize(float size) {
        this.size = size;
        this.getBounds().setSize(size, size);
    }

    /**
     * @return True when enemy can see hero
     */
    public boolean isCanSeeHero() {
        return canSeeHero;
    }
    public void setCanSeeHero(boolean canSeeHero) {
        this.canSeeHero = canSeeHero;
    }

    /**
     * @return float of Angle of the field of view
     */
    public float getViewAngle() {
        return viewAngle;
    }
    public void setViewAngle(float viewAngle) {
        this.viewAngle = viewAngle;
    }

    /**
     * @return the distance of view
     */
    public float getViewDistance() {
        return viewDistance;
    }
    public void setViewDistance(float viewDistance) {
        this.viewDistance = viewDistance;
    }

    /**
     * @return the time before causing damage
     */
    public float getAttackDuration() {
        return attackDuration;
    }
    public void setAttackDuration(float attackDuration) {
        this.attackDuration = attackDuration;
    }

    /**
     * @return maximum enemy's acceleration
     */
    public float getMaxAcceleration() {
        return maxAcceleration;
    }
    public void setMaxAcceleration(float maxAcceleration) {
        this.maxAcceleration = maxAcceleration;
    }

    public EnemyStateMachine getStateMachine() {
        return stateMachine;
    }

    public Vector2 getHeroLastKnownPos() {
        return heroLastKnownPos;
    }

    public void setHeroLastKnownPos(Vector2 heroLastKnownPos) {
        this.heroLastKnownPos = heroLastKnownPos;
    }

    public Rectangle getAttackRect() {
        Rectangle attackRect = rectPool.obtain();

        if (this.isFacingLeft()) {
            attackRect.set(
                this.getBounds().x - this.getBounds().width/2,
                this.getBounds().y - this.getBounds().height/2,
                this.getBounds().width,
                this.getBounds().height);
        } else {
            attackRect.set(
                this.getBounds().x + this.getBounds().width/2,
                this.getBounds().y + this.getBounds().height/2,
                this.getBounds().width,
                this.getBounds().height
            );
        }
        return  attackRect;
    }

    public Pool<Rectangle> getRectPool() {
        return rectPool;
    }

    public float getAttackTime() {
        return attackTime;
    }

    public void setAttackTime(float attackTime) {
        this.attackTime = attackTime;
    }

    public boolean isCanAttackHero() {
        return canAttackHero;
    }

    public void setCanAttackHero(boolean canAttackHero) {
        this.canAttackHero = canAttackHero;
    }

    public void setCanDealDamage(boolean canDealDamage) {
        this.canDealDamage = canDealDamage;
    }

    public boolean isDamageDealt() {
        return damageDealt;
    }

    public void setDamageDealt(boolean damageDealt) {
        this.damageDealt = damageDealt;
    }

    /*============================================================================================================*/

    /**
     * @return true if hero so nearby to deal damage
     */
    public boolean isCanDealDamage() {
        return canDealDamage;
    }
    public void markDamageDealt() {
        damageDealt = true;
    }

    /**
     * @param damage the damage is will take
     */
    @Override
    public void takeDamage(float damage) {
        this.health -= damage;
        if (this.health <= 0) {
            this.die();
        }
    }

    public void moveLeft() {
        this.setAccelerationX(-this.getMaxAcceleration());
        this.setFacingLeft(true);
    }

    public void moveRight() {
        this.setAccelerationX(this.getMaxAcceleration());
        this.setFacingLeft(false);
    }

    public void stopMoving() {
        this.setAccelerationX(0.0f);
    }

    /**
     * @Description: dying logic. Starts DeadState. Do not call another States in Enemy class!
     */
    public void die() {
        stateMachine.changeState(new DeadState());
    }
}
