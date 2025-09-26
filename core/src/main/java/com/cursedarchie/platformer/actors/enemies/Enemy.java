package com.cursedarchie.platformer.actors.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public abstract class Enemy {

    public enum EnemyState {
        IDLE, WALKING, JUMPING, ATTACKING, DYING
    }

    public float SIZE;
    private float MAX_HEALTH;
    private float MAX_ACCELERATION;
    private float health;
    private float damage;
    private float ATTACK_DURATION;
    private float VIEW_DISTANCE;
    private float VIEW_ANGLE;

    Vector2 position = new Vector2();
    Vector2 acceleration = new Vector2();
    Vector2 velocity = new Vector2();
    Rectangle bounds = new Rectangle();
    private EnemyState enemyState = EnemyState.IDLE;
    private boolean facingLeft = true;
    private float stateTime = 0f;
    private boolean grounded = false;
    private boolean isAlive = true;
    private boolean damageDealt = false;
    private boolean isHeroInSight = false;

    private float attackTime = 0f;

    public Enemy(Vector2 pos) {
        this.position.set(pos);
        this.bounds.x = pos.x;
        this.bounds.y = pos.y;
        this.bounds.width = SIZE;
        this.bounds.height = SIZE;
    }

    public boolean isFacingLeft() {
        return facingLeft;
    }
    public void setFacingLeft(boolean facingLeft) {
        this.facingLeft = facingLeft;
    }

    public boolean isGrounded() {
        return grounded;
    }
    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
    }

    public Vector2 getPosition() {
        return position;
    }
    public void setPosition(Vector2 position) {
        this.position.set(position);
        this.bounds.setX(position.x);
        this.bounds.setY(position.y);
    }

    public float getMaxAcceleration() {
        return MAX_ACCELERATION;
    }
    public void setMaxAcceleration(float maxAcceleration) {
        this.MAX_ACCELERATION = maxAcceleration;
    }
    public Vector2 getAcceleration() {
        return acceleration;
    }
    public void setAcceleration(Vector2 acceleration) {
        this.acceleration.set(acceleration);
    }

    public Vector2 getVelocity() {
        return velocity;
    }
    public void setVelocity(Vector2 velocity) {
        this.velocity.set(velocity);
    }

    public Rectangle getBounds() {
        return bounds;
    }
    public void setBounds(Rectangle bounds) {
        this.bounds.set(bounds);
    }

    public float getStateTime() {
        return stateTime;
    }
    public void setStateTime(float stateTime) {
        this.stateTime = stateTime;
    }

    public EnemyState getState() {
        return enemyState;
    }
    public void setState(EnemyState enemyState) {
        if (this.enemyState != enemyState) {
            Gdx.app.log("Enemy", "EnemyState change: " + this.enemyState + " -> " + enemyState);
            this.enemyState = enemyState;
            stateTime = 0f;
        }

    }

    public float getSize() {
        return SIZE;
    }
    public void setSize(float SIZE) {
        this.SIZE = SIZE;
    }

    public boolean isHeroInSight() {
        return isHeroInSight;
    }
    public void setHeroInSight(boolean heroInSight) {
        isHeroInSight = heroInSight;
    }
    public float getViewAngle() {
        return VIEW_ANGLE;
    }
    public void setViewAngle(float VIEW_ANGLE) {
        this.VIEW_ANGLE = VIEW_ANGLE;
    }
    public float getViewDistance() {
        return VIEW_DISTANCE;
    }
    public void setViewDistance(float VIEW_DISTANCE) {
        this.VIEW_DISTANCE = VIEW_DISTANCE;
    }


    public float getAttackDuration() {
        return ATTACK_DURATION;
    }
    public void setAttackDuration(float ATTACK_DURATION) {
        this.ATTACK_DURATION = ATTACK_DURATION;
    }
    public void takeDamage (float damage) {
        this.health -= damage;
        Gdx.app.log("INFO", "TAKE DAMAGE. HP: " + this.getHealth());
    }
    public float getDamage() {
        return damage;
    }
    public void setDamage(float damage) {
        this.damage = damage;
    }
    public void startAttack() {
        if (getState() != EnemyState.ATTACKING) {
            setState(EnemyState.ATTACKING);
            attackTime = 0f;
            damageDealt = false;
        }
    }
    public void updateAttack(float delta) {
        if (getState() == EnemyState.ATTACKING) {
            attackTime += delta;
            if (attackTime >= ATTACK_DURATION) {
                setState(EnemyState.IDLE);
            }
        }
    }
    public boolean canDealDamage() {
        return getState() == EnemyState.ATTACKING && !damageDealt && attackTime >= ATTACK_DURATION / 2f;
    }
    public void markDamageDealt() {
        damageDealt = true;
    }

    public float getMaxHealth() {
        return MAX_HEALTH;
    }
    public void setMaxHealth(float maxHealth) {
        MAX_HEALTH = maxHealth;
    }
    public float getHealth() {
        return health;
    }
    public void setHealth(float health) {
        this.health = health;
    }
    public boolean isAlive() {
        return isAlive;
    }
    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    public void update(float delta) {
        position.add(velocity.cpy().scl(delta));
        bounds.x = position.x;
        bounds.y = position.y;
        stateTime += delta;
    }
}
