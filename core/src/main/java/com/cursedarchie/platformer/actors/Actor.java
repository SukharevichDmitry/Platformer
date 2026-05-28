package com.cursedarchie.platformer.actors;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.cursedarchie.platformer.physics.PhysicsTags;

import java.util.HashSet;
import java.util.Set;

public abstract class Actor<M extends Enum<M>, C extends Enum<C>> {

    /*===========================================PARAMETERS===========================================================*/

    protected Body body;

    protected Fixture attackRightSensor;
    protected Fixture attackLeftSensor;
    protected Fixture visionSensor;

    protected int footContacts;

    protected M motionState;
    protected C combatState;

    protected boolean facingLeft;
    protected boolean alive;
    protected boolean grounded;
    protected boolean attackHandled;
    protected boolean leftAttackActive;
    protected boolean rightAttackActive;

    protected float motionStateTime;
    protected float combatStateTime;
    protected float size;
    protected float health;
    protected float maxHealth;
    protected float velocity;
    protected float jumpForce;
    protected float viewDistance;
    protected float viewAngle;
    protected float damage;
    protected float attackDuration;

    protected Array<Rectangle> collisionRects = new Array<>();
    protected Pool<Rectangle> rectPool = new Pool<Rectangle>() {
        @Override
        protected Rectangle newObject() {
            return new Rectangle();
        }
    };

    private final Set<Actor> attackTargets = new HashSet<>();
    protected final Set<Actor> visibleActors = new HashSet<>();

    /*===========================================HELPERS==============================================================*/

    protected void createBody(
        World world,
        Vector2 pos,
        short category,
        short mask
    ) {

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(pos);

        Body createdBody = world.createBody(bodyDef);

        createdBody.setFixedRotation(true);
        createdBody.setUserData(this);

        PolygonShape bodyShape = new PolygonShape();

        bodyShape.setAsBox(size / 2f, size / 2f);

        FixtureDef fixtureDef = new FixtureDef();

        fixtureDef.shape = bodyShape;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0f;
        fixtureDef.restitution = 0f;

        fixtureDef.filter.categoryBits = category;
        fixtureDef.filter.maskBits = mask;

        Fixture bodyFixture = createdBody.createFixture(fixtureDef);
        bodyFixture.setUserData("BODY");

        bodyShape.dispose();

        this.body = createdBody;

    }
    public Body getBody() {
        return body;
    }

    protected void createFootSensor(
        short category,
        short mask
    ) {

        PolygonShape footShape = new PolygonShape();

        float footWidth = size / 2.5f;
        float footHeight = 0.1f;

        Vector2 footOffset =
            new Vector2(0, -size / 2f + 0.1f);

        footShape.setAsBox(
            footWidth,
            footHeight,
            footOffset,
            0
        );

        FixtureDef footFixtureDef =
            new FixtureDef();

        footFixtureDef.shape = footShape;
        footFixtureDef.isSensor = true;

        footFixtureDef.filter.categoryBits =
            category;

        footFixtureDef.filter.maskBits =
            mask;

        Fixture foot =
            body.createFixture(footFixtureDef);

        foot.setUserData(PhysicsTags.FOOT_SENSOR);

        footShape.dispose();

    }

    protected void createVisionSensor(
        float radius,
        short category,
        short mask
    ) {

        CircleShape shape = new CircleShape();
        shape.setRadius(radius);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.isSensor = true;
        fixtureDef.filter.categoryBits = category;
        fixtureDef.filter.maskBits = mask;

        visionSensor = body.createFixture(fixtureDef);
        visionSensor.setUserData(PhysicsTags.VISION_SENSOR);

        shape.dispose();
    }

    protected void createAttackSensors(
        String tag,
        short category,
        short mask
    ) {

        attackRightSensor = createAttackSensor(
            new Vector2(size, 0),
            tag,
            category,
            mask
        );

        attackLeftSensor = createAttackSensor(
            new Vector2(-size, 0),
            tag,
            category,
            mask
        );
    }

    protected Fixture createAttackSensor(
        Vector2 offset,
        String tag,
        short category,
        short mask
    ) {

        PolygonShape shape = new PolygonShape();

        float attackWidth = size * 0.8f;
        float attackHeight = size * 0.8f;

        shape.setAsBox(
            attackWidth / 2f,
            attackHeight / 2f,
            offset,
            0
        );

        FixtureDef def = new FixtureDef();
        def.shape = shape;
        def.isSensor = true;

        def.filter.categoryBits = category;
        def.filter.maskBits = mask;

        Fixture fixture = body.createFixture(def);

        fixture.setUserData(tag);

        shape.dispose();

        return fixture;
    }

    /*===========================================GETTERS/SETTERS======================================================*/

    public Rectangle getAttackRect() {
        Rectangle attackRect = rectPool.obtain();
        if (this.isFacingLeft()) {
            attackRect.set(
                this.getBounds().x - this.getBounds().width/2,
                this.getBounds().y - this.getBounds().height/2,
                this.getBounds().width,
                this.getBounds().height
            );
        } else {
            attackRect.set(
                this.getBounds().x + this.getBounds().width/2,
                this.getBounds().y + this.getBounds().height/2,
                this.getBounds().width,
                this.getBounds().height
            );
        }
        return attackRect;
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

            bounds.set(
                position.x - size / 2f,
                position.y - size / 2f,
                size,
                size
            );
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

    public float getSize() {
        return size;
    }
    public void setSize(float size) {
        this.size = size;
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

    public float getAttackDuration() {
        return attackDuration;
    }
    public void setAttackDuration(float attackDuration) {
        this.attackDuration = attackDuration;
    }

    public boolean isAlive() {
        return alive;
    }
    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isFacingLeft() {
        return facingLeft;
    }
    public void setFacingLeft(boolean facingLeft) {
        this.facingLeft = facingLeft;
    }

    public float getMaxHealth() {
        return maxHealth;
    }
    public void setMaxHealth(float maxHealth) {
        this.maxHealth = maxHealth;
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

    public float getViewDistance() {
        return viewDistance;
    }
    public void setViewDistance(float viewDistance) {
        this.viewDistance = viewDistance;
    }

    public float getViewAngle() {
        return viewAngle;
    }
    public void setViewAngle(float viewAngle) {
        this.viewAngle = viewAngle;
    }

    public float getVelocity() {
        return velocity;
    }
    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }

    public float getJumpForce() {
        return jumpForce;
    }
    public void setJumpForce(float jumpForce) {
        this.jumpForce = jumpForce;
    }

    public boolean isGrounded() {
        return grounded;
    }
    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
    }

    public int getFootContacts() {
        return footContacts;
    }
    public void setFootContacts(int footContacts) {
        this.footContacts = footContacts;
    }

    public boolean isLeftAttackActive() {
        return leftAttackActive;
    }
    public void setLeftAttackActive(boolean leftAttackActive) {
        this.leftAttackActive = leftAttackActive;
    }

    public boolean isRightAttackActive() {
        return rightAttackActive;
    }
    public void setRightAttackActive(boolean rightAttackActive) {
        this.rightAttackActive = rightAttackActive;
    }

    public void addAttackTarget(Actor actor) {
            attackTargets.add(actor);
    }
    public void removeAttackTarget(Actor actor) {
        attackTargets.remove(actor);
    }
    public Set<Actor> getAttackTargets() {
        return attackTargets;
    }

    public void addVisibleActor(Actor actor) {
        visibleActors.add(actor);
    }
    public void removeVisibleActor(Actor actor) {
        visibleActors.remove(actor);
    }
    public Set<Actor> getVisibleActors() {
        return visibleActors;
    }

    public boolean isAttackHandled() {
        return attackHandled;
    }
    public void setAttackHandled(boolean attackHandled) {
        this.attackHandled = attackHandled;
    }

    /*===========================================CONTROLLERS==========================================================*/

    public void takeDamage(float damage) {
        health -= damage;
        if (this.getHealth() <= 0) {
            setAlive(false);
            die();
        }
    }

    protected void syncCollisionRects() {
        Vector2 position = body.getPosition();
        for (Rectangle rect : collisionRects) {
            rect.setPosition(position.x, position.y);
        }
    }

    protected abstract void die();

    protected abstract void update(float delta);

    public Array<Rectangle> getCollisionRects() {
        return collisionRects;
    }
}
