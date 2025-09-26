package com.cursedarchie.platformer.actors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public abstract class Actor<T extends Enum<T>> {

    T state;

    boolean facingLeft;
    Vector2 position = new Vector2();
    Rectangle bounds = new Rectangle();
    Vector2 acceleration = new Vector2();
    Vector2 velocity = new Vector2();
    float stateTime;
    boolean isAlive;
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
        return isAlive;
    }
    public void setAlive(boolean alive) {
        this.isAlive = alive;
    }

    /**
     * @param damage the damage is will take
     */
    public void takeDamage (float damage) {
        this.health -= damage;
        Gdx.app.log("INFO", "HERO TAKES DAMAGE. HP: " + this.getHealth());
    }

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
