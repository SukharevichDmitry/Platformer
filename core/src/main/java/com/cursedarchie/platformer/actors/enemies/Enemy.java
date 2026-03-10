package com.cursedarchie.platformer.actors.enemies;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Pool;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.enemies.logic.EnemyStateMachine;
import com.cursedarchie.platformer.actors.enemies.logic.states.DeadState;
import com.cursedarchie.platformer.actors.hero.Hero;

public abstract class Enemy extends Actor<Enemy.EnemyMotionState, Enemy.EnemyCombatState> {

    private final Body body;

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

    public Enemy(World world, Vector2 pos, float size, float maxHealth) {
        this.size = size;
        this.motionState = EnemyMotionState.IDLE;
        this.motionStateTime = 0f;
        this.combatStateTime = 0f;
        this.health = maxHealth;
        this.facingLeft = true;
        this.alive = true;
        this.stateMachine = new EnemyStateMachine(this);

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(pos);
        this.body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(size / 2f, size / 2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0.2f;
        fixtureDef.restitution = 0f;

        this.body.createFixture(fixtureDef);
        shape.dispose();
    }

    /*===========================================GETTERS/SETTERS===================================================*/
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
        return size;
    }
    public void setSize(float size) {
        this.size = size;
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
        Vector2 position = body.getPosition();

        if (this.isFacingLeft()) {
            attackRect.set(
                position.x - size,
                position.y - size,
                size,
                size
            );
        } else {
            attackRect.set(
                position.x + size,
                position.y + size,
                size,
                size
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
        health -= damage;
        if (health <= 0) {
            die();
        }
    }

    public void moveLeft() {
        body.setLinearVelocity(-maxAcceleration, body.getLinearVelocity().y);
        setFacingLeft(true);
    }

    public void moveRight() {
        body.setLinearVelocity(maxAcceleration, body.getLinearVelocity().y);
        setFacingLeft(false);
    }

    public void stopMoving() {
        body.setLinearVelocity(0f, body.getLinearVelocity().y);
    }

    /**
     * @Description: dying logic. Starts DeadState. Do not call another States in Enemy class!
     */
    public void die() {
        stateMachine.changeState(new DeadState());
    }
}
