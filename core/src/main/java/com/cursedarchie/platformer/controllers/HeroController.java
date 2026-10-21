package com.cursedarchie.platformer.controllers;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.cursedarchie.platformer.actors.hero.states.combat.AttackState;
import com.cursedarchie.platformer.actors.hero.states.motion.MotionIdleState;
import com.cursedarchie.platformer.actors.hero.states.motion.JumpState;
import com.cursedarchie.platformer.actors.hero.states.motion.MoveState;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.world.GameWorld;
import com.cursedarchie.platformer.levels.VerticalConnection;

/** Collects input events and applies them only during GameWorld's fixed simulation steps. */
public class HeroController implements InputProcessor {
    private final Hero hero;
    private final GameWorld gameWorld;
    private static final float CLIMB_SPEED = 4.5f;
    private static final float CLIMB_HORIZONTAL_SPEED = 4f;
    private static final float ATTACK_BUFFER_SECONDS = 0.12f;
    private static final float JUMP_BUFFER_SECONDS = 0.1f;
    private boolean leftHeld;
    private boolean rightHeld;
    private boolean upHeld;
    private boolean downHeld;
    private boolean climbing;
    private boolean climbRegrabBlocked;
    private boolean attackHeld;
    private float attackBufferRemaining;
    private boolean jumpHeld;
    private float jumpBufferRemaining;
    public HeroController(GameWorld gameWorld) {
        this.gameWorld = gameWorld;
        this.hero = gameWorld.getHero();
    }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.LEFT:
                leftHeld = true;
                break;
            case Input.Keys.RIGHT:
                rightHeld = true;
                break;
            case Input.Keys.UP:
                upHeld = true;
                break;
            case Input.Keys.DOWN:
                downHeld = true;
                break;
            case Input.Keys.SPACE:
                if (!jumpHeld) jumpBufferRemaining = JUMP_BUFFER_SECONDS;
                jumpHeld = true;
                break;
            case Input.Keys.X:
                if (!attackHeld) attackBufferRemaining = ATTACK_BUFFER_SECONDS;
                attackHeld = true;
                break;
        }
        return true;
    }
    @Override
    public boolean keyUp(int keycode) {
        switch (keycode) {
            case Input.Keys.LEFT:
                leftHeld = false;
                break;
            case Input.Keys.RIGHT:
                rightHeld = false;
                break;
            case Input.Keys.UP:
                upHeld = false;
                break;
            case Input.Keys.DOWN:
                downHeld = false;
                break;
            case Input.Keys.SPACE:
                jumpHeld = false;
                break;
            case Input.Keys.X:
                attackHeld = false;
                break;
        }
        return true;
    }
    @Override
    public boolean keyTyped(char c) {
        return false;
    }
    @Override
    public boolean touchDown(int i, int i1, int i2, int i3) {
        return false;
    }
    @Override
    public boolean touchUp(int i, int i1, int i2, int i3) {
        return false;
    }
    @Override
    public boolean touchCancelled(int i, int i1, int i2, int i3) {
        return false;
    }
    @Override
    public boolean touchDragged(int i, int i1, int i2) {
        return false;
    }
    @Override
    public boolean mouseMoved(int i, int i1) {
        return false;
    }
    @Override
    public boolean scrolled(float v, float v1) {
        return false;
    }

    public void update(float delta) {
        if (!hero.isAlive()) {
            return;
        }

        processInput(delta);

        checkHeroPositionForDeathFall();

        hero.update(delta);
    }

    public void clearInput() {
        // Pause/hide must discard both held controls and one-shot buffered actions.
        leftHeld = false;
        rightHeld = false;
        upHeld = false;
        downHeld = false;
        climbing = false;
        climbRegrabBlocked = false;
        jumpHeld = false;
        jumpBufferRemaining = 0f;
        attackHeld = false;
        attackBufferRemaining = 0f;
    }


    private void checkHeroPositionForDeathFall() {
        if ((hero.getPosition().y + hero.getBounds().height) < 0) {
            hero.setAlive(false);
        }
    }

    private void processInput(float delta) {
        handleMovementInput(delta);
        handleCombatInput(delta);
    }

    private void handleMovementInput(float delta) {
        float direction = 0f;
        boolean isJumpState = hero.getMotionStateMachine().getCurrentState() instanceof JumpState;

        if (leftHeld && !rightHeld) {
            direction = -1f;
            hero.setFacingLeft(true);
        } else if (rightHeld && !leftHeld) {
            direction = 1f;
            hero.setFacingLeft(false);
        }

        float currentX = hero.getBody().getLinearVelocity().x;
        float currentY = hero.getBody().getLinearVelocity().y;
        VerticalConnection shaft = gameWorld.getLocation().climbConnectionAt(
            hero.getPosition().x, hero.getPosition().y, Hero.BODY_SIZE);
        if (shaft == null) climbing = false;
        if (!upHeld && !downHeld) climbRegrabBlocked = false;
        if (shaft != null && !climbRegrabBlocked && upHeld != downHeld) climbing = true;
        if (shaft != null && climbing) {
            if (jumpBufferRemaining > 0f) {
                // Space always releases the ladder, even when no terrain foot contact exists.
                climbing = false;
                climbRegrabBlocked = true;
                jumpBufferRemaining = 0f;
                hero.getBody().setGravityScale(1f);
                hero.getBody().setLinearVelocity(direction * hero.getVelocity(), hero.getJumpForce());
                if (isJumpState) hero.getMotionStateMachine().changeState(new MotionIdleState());
                hero.getMotionStateMachine().changeState(new JumpState());
                return;
            }
            float climbY = 0f;
            if (upHeld != downHeld) {
                float targetY = upHeld ? shaft.topY : shaft.bottomY;
                float distance = targetY - hero.getPosition().y;
                if (Math.abs(distance) >= 0.02f)
                    climbY = Math.copySign(Math.min(CLIMB_SPEED, Math.abs(distance) / delta), distance);
            }
            // Releasing Up/Down grips the shaft; Left/Right can traverse or leave it.
            float climbX = direction * CLIMB_HORIZONTAL_SPEED;
            hero.getBody().setGravityScale(0f);
            hero.getBody().setLinearVelocity(climbX, climbY);
            if (isJumpState) hero.getMotionStateMachine().changeState(new MotionIdleState());
            jumpBufferRemaining = Math.max(0f, jumpBufferRemaining - delta);
            return;
        }
        float targetX = direction * hero.getVelocity();
        float acceleration = hero.isGrounded()
            ? (direction == 0f ? Hero.GROUND_BRAKING : Hero.GROUND_ACCELERATION)
            : (direction == 0f ? Hero.AIR_BRAKING : Hero.AIR_ACCELERATION);
        float moveX = approach(currentX, targetX, acceleration * delta);
        float moveY = Math.max(currentY, -Hero.MAX_FALL_SPEED);
        hero.getBody().setLinearVelocity(moveX, moveY);

        if (jumpBufferRemaining > 0f && hero.isGrounded() && moveY <= 0.1f) {
            // Grounding was refreshed after the previous physics step, but JumpState may
            // still be active until hero.update below. Leave it so re-entering JumpState
            // starts a new jump animation instead of being ignored as the same state.
            if (isJumpState) hero.getMotionStateMachine().changeState(new MotionIdleState());
            hero.getBody().setLinearVelocity(moveX, hero.getJumpForce());
            hero.getBody().setGravityScale(1f);
            jumpBufferRemaining = 0f;
            hero.getMotionStateMachine().changeState(new JumpState());
            return;
        }
        jumpBufferRemaining = Math.max(0f, jumpBufferRemaining - delta);

        float gravityScale = moveY < 0f ? Hero.FALL_GRAVITY_SCALE
            : (!jumpHeld && moveY > 0f ? Hero.RELEASE_GRAVITY_SCALE : 1f);
        hero.getBody().setGravityScale(gravityScale);

        if (!isJumpState) {
            if (moveX != 0) {
                hero.getMotionStateMachine().changeState(new MoveState());
            } else {
                hero.getMotionStateMachine().changeState(new MotionIdleState());
            }
        }
    }

    private static float approach(float current, float target, float step) {
        if (current < target) return Math.min(current + step, target);
        return Math.max(current - step, target);
    }
    private void handleCombatInput(float delta) {
        if (attackBufferRemaining > 0f &&
            !(hero.getCombatStateMachine().getCurrentState() instanceof AttackState)) {
            hero.getCombatStateMachine().changeState(new AttackState());
            attackBufferRemaining = 0f;
        } else {
            attackBufferRemaining = Math.max(0f, attackBufferRemaining - delta);
        }
    }
}
