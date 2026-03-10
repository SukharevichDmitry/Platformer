package com.cursedarchie.platformer.actors.hero;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Pool;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.hero.machines.CombatStateMachine;
import com.cursedarchie.platformer.actors.hero.machines.MotionStateMachine;
import com.cursedarchie.platformer.physics.PhysicsCategories;

public class Hero extends Actor<Hero.HeroMotionState, Hero.HeroCombatState> {

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

    private final Body body;
    private final MotionStateMachine motionStateMachine;
    private final CombatStateMachine combatStateMachine;
    private final Fixture attackRight;
    private final Fixture attackLeft;
    private final Pool<Rectangle> rectPool = new Pool<Rectangle>() {
        @Override
        protected Rectangle newObject() {
            return new Rectangle();
        }
    };

    private float size = 0.5f;
    private float maxHealth = 3;
    private boolean attackHandled = false;
    private boolean grounded = false;
    private float attackDuration;

    public Hero(Vector2 pos, World world) {
        super();

        this.motionState = HeroMotionState.IDLE;
        this.combatState = HeroCombatState.IDLE;
        this.motionStateTime = 0f;
        this.combatStateTime = 0f;
        this.alive = true;
        this.health = maxHealth;
        this.facingLeft = false;
        this.damage = 1f;
        this.motionStateMachine = new MotionStateMachine(this);
        this.combatStateMachine = new CombatStateMachine(this);
        this.attackDuration = 0.3f;

        //============================================================

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(pos.x, pos.y);
        body = world.createBody(bodyDef);
        body.setFixedRotation(true);

        //BODY============================================================

        PolygonShape bodyShape = new PolygonShape();
        bodyShape.setAsBox(size / 2f, size / 2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = bodyShape;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0f;
        fixtureDef.restitution = 0f;

        fixtureDef.filter.categoryBits = PhysicsCategories.HERO;
        fixtureDef.filter.maskBits =
            PhysicsCategories.WALL |
            PhysicsCategories.ENEMY |
            PhysicsCategories.GROUND;

        body.createFixture(fixtureDef);
        bodyShape.dispose();

        //FOOT_SENSOR============================================================

        PolygonShape footShape = new PolygonShape();
        float footWidth = size / 2.5f;
        float footHeight = 0.1f;
        Vector2 footOffset = new Vector2(0, -size / 2f + 0.1f);

        footShape.setAsBox(
            footWidth,
            footHeight,
            footOffset,
            0
        );

        FixtureDef footFixtureDef = new FixtureDef();
        footFixtureDef.shape = footShape;
        footFixtureDef.isSensor = true;
        footFixtureDef.filter.categoryBits = PhysicsCategories.HERO;
        footFixtureDef.filter.maskBits = PhysicsCategories.WALL;

        Fixture foot = body.createFixture(footFixtureDef);
        foot.setUserData("hero_foot");
        footShape.dispose();

        //ATTACK_RIGHT==============================================================
        PolygonShape attackRightShape = new PolygonShape();

        float attackWidth = size * 0.8f;
        float attackHeight = size * 0.8f;

        attackRightShape.setAsBox(
            attackWidth/2,
            attackHeight/2,
            new Vector2(size, 0),
            0
        );

        FixtureDef attackRightDef = new FixtureDef();
        attackRightDef.shape = attackRightShape;
        attackRightDef.isSensor = true;
        attackRightDef.filter.categoryBits = PhysicsCategories.HERO_ATTACK;
        attackRightDef.filter.maskBits = PhysicsCategories.ENEMY;

        attackRight = body.createFixture(attackRightDef);
        attackRight.setUserData("hero_attack");

        attackRightShape.dispose();

        //ATTACK_LEFT================================================================
        PolygonShape attackLeftShape = new PolygonShape();

        attackLeftShape.setAsBox(
            attackWidth/2,
            attackHeight/2,
            new Vector2(-size, 0),
            0
        );

        FixtureDef attackLeftDef = new FixtureDef();
        attackLeftDef.shape = attackLeftShape;
        attackLeftDef.isSensor = true;
        attackLeftDef.filter.categoryBits = PhysicsCategories.HERO_ATTACK;
        attackLeftDef.filter.maskBits = PhysicsCategories.ENEMY;

        attackLeft = body.createFixture(attackLeftDef);
        attackLeft.setUserData("hero_attack");

        attackLeftShape.dispose();
    }

    /*================================================================================================================*/

    public Body getBody() {
        return body;
    }

    public Vector2 getPosition() {
        return body.getPosition();
    }

    @Override
    public float getMaxHealth() {
        return maxHealth;
    }
    public void setMaxHealth(float maxHealth) {
        this.maxHealth = maxHealth;
    }

    public float getSize() {
        return this.size;
    }
    public void setSize(float size) {
        this.size = size;
        this.getBounds().setSize(size, size);
    }

    public Pool<Rectangle> getRectPool() {
        return rectPool;
    }

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

    public boolean isAttackHandled() {
        return attackHandled;
    }

    public void setAttackHandled(boolean attackHandled) {
        this.attackHandled = attackHandled;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
    }

    public MotionStateMachine getMotionStateMachine() {
        return motionStateMachine;
    }

    public CombatStateMachine getCombatStateMachine() {
        return combatStateMachine;
    }

    public float getAttackDuration() {
        return attackDuration;
    }

    public void setAttackDuration(float attackDuration) {
        this.attackDuration = attackDuration;
    }

    /*================================================================================================================*/

    @Override
    public void update() {

        if (body != null) {
            Vector2 position = body.getPosition();
            for (Rectangle rect : collisionRects) {
                rect.setPosition(position.x, position.y);
            }
        }

        if(combatState == HeroCombatState.ATTACK &&
            combatStateTime > 0.08f &&
            combatStateTime < 0.18f){

            if(facingLeft){
                attackLeft.setSensor(false);
                attackRight.setSensor(true);
            }
            else{
                attackRight.setSensor(false);
                attackLeft.setSensor(true);
            }

        }
        else{
            attackLeft.setSensor(true);
            attackRight.setSensor(true);
        }
    }

    @Override
    public void takeDamage(float damage) {
        health -= damage;
        if (this.getHealth() <= 0) {
            heroDie();
        }
    }

    private void heroDie() {
        this.setAlive(false);
    }
}
