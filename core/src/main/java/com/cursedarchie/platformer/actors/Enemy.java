package com.cursedarchie.platformer.actors;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;

public abstract class Enemy extends Actor<Enemy.EnemyState> {

    public enum EnemyState {
        IDLE, WALKING, JUMPING, ATTACKING, DYING
    }

    public float SIZE;
    private float MAX_HEALTH;
    private float ATTACK_DURATION;
    private float VIEW_DISTANCE;
    private float VIEW_ANGLE;
    private float MAX_ACCELERATION;

    private boolean grounded = false;
    private boolean damageDealt = false;
    private boolean isHeroInSight = false;

    private float attackTime = 0f;

    public Enemy(Vector2 pos) {
        this.position.set(pos);
        this.bounds.x = pos.x;
        this.bounds.y = pos.y;
        this.bounds.width = SIZE;
        this.bounds.height = SIZE;
        this.state = EnemyState.IDLE;
        this.stateTime = 0f;
        this.isAlive = true;
        this.health = MAX_HEALTH;
        this.facingLeft = true;
    }

    /**
     * @param state value of EnemyState enum
     */
    @Override
    public void setState(EnemyState state) {
        if (this.state != state) {
            Gdx.app.log("Enemy", "EnemyState change: " + this.state + " -> " + state);
            this.state = state;
            stateTime = 0f;
        }
    }

    /**
     * @return maximum health of actor
     */
    @Override
    public float getMaxHealth() {
        return MAX_HEALTH;
    }
    public void setMaxHealth(float maxHealth) {
        MAX_HEALTH = maxHealth;
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
     * @return SIZE. this.bounds.width = this.bounds.height = SIZE
     */
    public float getSize() {
        return SIZE;
    }
    public void setSize(float SIZE) {
        this.SIZE = SIZE;
    }

    /**
     * @return True when enemy can see hero
     */
    public boolean isHeroInSight() {
        return isHeroInSight;
    }
    public void setHeroInSight(boolean heroInSight) {
        isHeroInSight = heroInSight;
    }

    /**
     * @return float of Angle of the field of view
     */
    public float getViewAngle() {
        return VIEW_ANGLE;
    }
    public void setViewAngle(float VIEW_ANGLE) {
        this.VIEW_ANGLE = VIEW_ANGLE;
    }

    /**
     * @return the distance of view
     */
    public float getViewDistance() {
        return VIEW_DISTANCE;
    }
    public void setViewDistance(float VIEW_DISTANCE) {
        this.VIEW_DISTANCE = VIEW_DISTANCE;
    }

    /**
     * @return the time before causing damage
     */
    public float getAttackDuration() {
        return ATTACK_DURATION;
    }
    public void setAttackDuration(float ATTACK_DURATION) {
        this.ATTACK_DURATION = ATTACK_DURATION;
    }

    /**
     * @Description: checks if already attacks. If not, set state and start count AttackTime.
     */
    public void startAttack() {
        if (getState() != EnemyState.ATTACKING) {
            setState(EnemyState.ATTACKING);
            attackTime = 0f;
            damageDealt = false;
        }
    }

    /**
     * @param delta unit of time
     * @Description: if enemy attacks update AttackTime, if enemy had already attacked set IDLE state
     */
    public void updateAttack(float delta) {
        if (getState() == EnemyState.ATTACKING) {
            attackTime += delta;
            if (attackTime >= ATTACK_DURATION) {
                setState(EnemyState.IDLE);
            }
        }
    }

    /**
     * @return true if hero so nearby to deal damage
     */
    public boolean canDealDamage() {
        return getState() == EnemyState.ATTACKING && !damageDealt && attackTime >= ATTACK_DURATION / 2f;
    }
    public void markDamageDealt() {
        damageDealt = true;
    }

    /**
     * @return maximum enemy's acceleration
     */
    public float getMaxAcceleration() {
        return MAX_ACCELERATION;
    }
    public void setMaxAcceleration(float maxAcceleration) {
        this.MAX_ACCELERATION = maxAcceleration;
    }
}
