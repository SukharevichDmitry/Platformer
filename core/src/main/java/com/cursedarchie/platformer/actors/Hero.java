package com.cursedarchie.platformer.actors;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool;
import com.cursedarchie.platformer.actors.hero.HeroStateMachine;

public class Hero extends Actor<Hero.HeroState> {

    public enum HeroState {
        IDLE,
        WALK,
        JUMP,
        ATTACK,
        DIE
    }

    /**
     * Hero attributes... (open to details)
     *
     * @param HeroState enum of states that hero can have
     */
    private float size = 0.5f;
    private float maxHealth = 3;

    private HeroStateMachine stateMachine;
    private boolean attackHandled = false;
    private boolean grounded = false;
    private boolean jumpPressed = false;
    private float attackDuration;

    public Hero(Vector2 pos) {
        super();
        Gdx.app.log("Hero", "Creating New Hero");
        Gdx.app.log("Hero", "Initial velocity: " + velocity);

        this.position.set(pos);
        this.bounds.x = pos.x;
        this.bounds.y = pos.y;
        this.bounds.width = size;
        this.bounds.height = size;
        this.state = HeroState.IDLE;
        this.stateTime = 0f;
        this.alive = true;
        this.health = maxHealth;
        this.facingLeft = false;
        this.damage = 1f;
        this.stateMachine = new HeroStateMachine(this);
        this.attackDuration = 0.3f;
    }

    private Pool<Rectangle> rectPool = new Pool<Rectangle>() {
        @Override
        protected Rectangle newObject() {
            return new Rectangle();
        }
    };

    /*================================================================================================================*/
    /**
     * @return maximum health of actor
     */
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

    public HeroStateMachine getStateMachine() {
        return stateMachine;
    }

    public boolean isJumpPressed() {
        return this.jumpPressed;
    }

    public void setJumpPressed(boolean pressed) {
        this.jumpPressed = pressed;
    }

    public float getAttackDuration() {
        return attackDuration;
    }

    public void setAttackDuration(float attackDuration) {
        this.attackDuration = attackDuration;
    }

    /*================================================================================================================*/

    @Override
    public void takeDamage(float damage) {
        this.health -= damage;
        if (this.getHealth() <= 0) {
            heroDie();
        }
    }

    private void heroDie() {
        this.setAlive(false);
    }
}
