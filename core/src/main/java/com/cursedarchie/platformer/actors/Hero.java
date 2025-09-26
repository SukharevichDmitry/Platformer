package com.cursedarchie.platformer.actors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Hero {

    public enum State {
        IDLE, WALKING, JUMPING, ATTACKING, DYING
    }

    public static final float SIZE = 0.5f;
    public static final float MAX_HEALTH = 3;

    float health = MAX_HEALTH;
    float damage = 1f;

    Vector2 position = new Vector2();
    Vector2 acceleration = new Vector2();
    Vector2 velocity = new Vector2();
    Rectangle bounds = new Rectangle();
    State state = State.IDLE;
    boolean facingLeft = true;
    float stateTime = 0f;
    boolean isAlive = true;


    public Hero(Vector2 pos) {
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

    public Vector2 getPosition() {
        return position;
    }
    public void setPosition(Vector2 position) {
        this.position.set(position);
        this.bounds.setX(position.x);
        this.bounds.setY(position.y);
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

    public State getState() {
        return state;
    }
    public void setState(State state) {
        this.state = state;
    }

    public boolean isAlive() {
        return isAlive;
    }
    public void setAlive(boolean alive) {
        this.isAlive = alive;
    }



    public void takeDamage (float damage) {
        this.health -= damage;
        Gdx.app.log("INFO", "HERO TAKES DAMAGE. HP: " + this.getHealth());
    }

    public float getMaxHealth() {
        return MAX_HEALTH;
    }
    public float getHealth() {
        return health;
    }
    public void setHealth(float health) {
        this.health = health;
    }

    public float getDamage() {
        return damage;
    }
    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void update(float delta) {
        position.add(velocity.cpy().scl(delta));
        bounds.x = position.x;
        bounds.y = position.y;
        stateTime += delta;
    }
}
