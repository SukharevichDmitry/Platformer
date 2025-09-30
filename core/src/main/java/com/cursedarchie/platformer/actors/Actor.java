package com.cursedarchie.platformer.actors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.actors.enemies.logic.states.DyingState;

public abstract class Actor<T extends Enum<T>> {

    /**
     * Actor's attributes... (open to details)
     *
     * @param state actor's state in current time
     * @param facingLeft indicates the direction the actor is looking at
     * @param position indicates coordinates of actor
     * @param bounds indicates square of actor's location (x, y, width, height)
     * @param acceleration value which change speed
     * @param velocity speed
     * @param stateTime time that actor keeps it's state
     * @param alive indicates is actor alive
     * @param health value of health
     * @param damage value of damage that actor can deal
     */
    T state;
    boolean facingLeft;
    Vector2 position = new Vector2();
    Rectangle bounds = new Rectangle();
    Vector2 acceleration = new Vector2();
    Vector2 velocity = new Vector2();
    float stateTime;
    boolean alive;
    float health;
    float damage;


    /**
     *  @return This method return true when actor is facing left
     */
    public boolean isFacingLeft() {
        return facingLeft;
    }
    public void setFacingLeft(boolean facingLeft) {
        this.facingLeft = facingLeft;
    }

    /**
     * @return This method return position of enemy coordinates (x, y)
     */
    public Vector2 getPosition() {
        return position;
    }
    public void setPosition(Vector2 position) {
        this.position.set(position);
        this.bounds.setX(position.x);
        this.bounds.setY(position.y);
    }

    /**
     * @return current acceleration
     */
    public Vector2 getAcceleration() {
        return acceleration;
    }
    public void setAccelerationX(float ax) {
        this.acceleration.x = ax;
    }
    public void setAccelerationY(float ay) {
        this.acceleration.y = ay;
    }
    public void setAcceleration(Vector2 acceleration) {
        this.acceleration.set(acceleration);
    }

    /**
     * @return current speed
     */
    public Vector2 getVelocity() {
        return velocity;
    }
    public void setVelocity(Vector2 velocity) {
        this.velocity.set(velocity);
    }

    /**
     * @return square where actor is located (x, y, width, height)
     */
    public Rectangle getBounds() {
        return bounds;
    }
    public void setBounds(Rectangle bounds) {
        this.bounds.set(bounds);
    }

    /**
     * @return time which actor staying in current state
     */
    public float getStateTime() {
        return stateTime;
    }
    public void setStateTime(float stateTime) {
        this.stateTime = stateTime;
    }

    /**
     * @return current state
     */
    public T getState() {
        return state;
    }
    public void setState(T state) {
        this.state = state;
    }

    /**
     * @return true if actor is alive
     */
    public boolean isAlive() {
        return alive;
    }
    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    /**
     * @param damage the damage is will take
     */
    public abstract void takeDamage (float damage);

    /**
     * @return maximum health of actor
     */
    public abstract float getMaxHealth();

    /**
     * @return current actor's health
     */
    public float getHealth() {
        return health;
    }
    public void setHealth(float health) {
        this.health = health;
    }

    /**
     * @return current actor's damage
     */
    public float getDamage() {
        return damage;
    }
    public void setDamage(float damage) {
        this.damage = damage;
    }

    /**
     * @param delta unit of time
     * @Description: move actor with it velocity
     */
    public void update(float delta) {
        position.add(velocity.cpy().scl(delta));
        bounds.x = position.x;
        bounds.y = position.y;
        stateTime += delta;
    }
}
