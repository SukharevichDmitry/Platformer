package com.cursedarchie.platformer.actors;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;

public abstract class Actor<M extends Enum<M>, C extends Enum<C>> {

    protected M motionState;
    protected C combatState;
    protected boolean facingLeft;
    protected float motionStateTime;
    protected float combatStateTime;
    protected boolean alive;
    protected float health;
    protected float damage;

    protected Array<Rectangle> collisionRects = new Array<>();
    protected Body body;

    public boolean isFacingLeft() {
        return facingLeft;
    }
    public void setFacingLeft(boolean facingLeft) {
        this.facingLeft = facingLeft;
    }

    public Vector2 getPosition() {
        if (body != null) {
            return body.getPosition();
        }
        return new Vector2();
    }

    public Rectangle getBounds() {
        Rectangle bounds = new Rectangle();
        if (body != null) {
            Vector2 position = body.getPosition();
            bounds.x = position.x - bounds.width / 2f;
            bounds.y = position.y - bounds.height / 2f;
        }
        return bounds;
    }

    public float getMotionStateTime() {
        return motionStateTime;
    }
    public void setMotionStateTime(float motionStateTime) {
        this.motionStateTime = motionStateTime;
    }

    public float getCombatStateTime() {
        return combatStateTime;
    }
    public void setCombatStateTime(float combatStateTime) {
        this.combatStateTime = combatStateTime;
    }

    public M getMotionState() {
        return motionState;
    }
    public void setMotionState(M motionState) {
        this.motionState = motionState;
    }

    public C getCombatState() {
        return combatState;
    }
    public void setCombatState(C combatState) {
        this.combatState = combatState;
    }

    public boolean isAlive() {
        return alive;
    }
    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public abstract void takeDamage (float damage);

    public abstract float getMaxHealth();

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

    public void update() {
        if (body != null) {
            Vector2 position = body.getPosition();
            for (Rectangle rect : collisionRects) {
                rect.setPosition(position.x, position.y);
            }
        }
    }

    public Array<Rectangle> getCollisionRects() {
        return collisionRects;
    }
}
