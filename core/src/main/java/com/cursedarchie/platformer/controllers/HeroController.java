package com.cursedarchie.platformer.controllers;

import com.badlogic.gdx.InputProcessor;
import com.cursedarchie.platformer.actors.hero.states.combat.AttackState;
import com.cursedarchie.platformer.actors.hero.states.motion.MotionIdleState;
import com.cursedarchie.platformer.actors.hero.states.motion.JumpState;
import com.cursedarchie.platformer.actors.hero.states.motion.MoveState;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.world.GameWorld;
import com.cursedarchie.platformer.levels.VerticalConnection;
import com.cursedarchie.platformer.run.Inventory;
import com.cursedarchie.platformer.controllers.ControlBindings.Action;

/** Collects input events and applies them only during GameWorld's fixed simulation steps. */
public class HeroController implements InputProcessor {
    private final Hero hero;
    private final GameWorld gameWorld;
    private static final float CLIMB_SPEED = 9f;
    private static final float CLIMB_HORIZONTAL_SPEED = 8f;
    private static final float ATTACK_BUFFER_SECONDS = 0.12f;
    private static final float JUMP_BUFFER_SECONDS = 0.1f;
    private static final float DASH_SECONDS = 0.15f;
    private static final float DASH_SPEED = 26f;
    private static final float DASH_COOLDOWN = 0.8f;
    private boolean leftHeld;
    private boolean rightHeld;
    private boolean upHeld;
    private boolean downHeld;
    private boolean climbing;
    private boolean climbRegrabBlocked;
    private boolean attackHeld;
    private boolean heavyAttackHeld;
    private boolean blockHeld;
    private boolean interactHeld;
    private boolean interactQueued;
    private int pocketQueued = -1;
    private ControlBindings bindings = new ControlBindings(null);
    private float attackBufferRemaining;
    private float heavyAttackBufferRemaining;
    private int comboStep;
    /** Exact follow-up step buffered by the current strike (2 or 3); zero means none. */
    private int queuedComboStep;
    private float queuedComboRemaining;
    private float observedHealth;
    private boolean jumpHeld;
    private float jumpBufferRemaining;
    private boolean dashQueued;
    private boolean dashHeld;
    private float dashRemaining;
    private float dashCooldownRemaining;
    public HeroController(GameWorld gameWorld) {
        this.gameWorld = gameWorld;
        this.hero = gameWorld.getHero();
        this.observedHealth = hero.getHealth();
    }

    public void setBindings(ControlBindings bindings) { this.bindings = bindings; clearInput(); }

    @Override
    public boolean keyDown(int keycode) {
        if (!hero.isAlive()) return true;
        if (bindings.matches(Action.LEFT, keycode)) leftHeld = true;
        else if (bindings.matches(Action.RIGHT, keycode)) rightHeld = true;
        else if (bindings.matches(Action.UP, keycode)) upHeld = true;
        else if (bindings.matches(Action.DOWN, keycode)) {
            if (!downHeld && gameWorld.isHeroOnOneWaySurface()) {
                hero.setPlatformDropRemaining(0.32f);
                hero.setGrounded(false);
                climbing = false;
                climbRegrabBlocked = true;
                hero.getBody().setLinearVelocity(hero.getBody().getLinearVelocity().x, -4f);
            }
            downHeld = true;
        }
        else if (bindings.matches(Action.JUMP, keycode)) {
            if (!jumpHeld) jumpBufferRemaining = JUMP_BUFFER_SECONDS;
            jumpHeld = true;
        } else if (bindings.matches(Action.ATTACK, keycode)) {
            if (!attackHeld) {
                AttackState current = hero.getCombatStateMachine().getCurrentState() instanceof AttackState
                    ? (AttackState) hero.getCombatStateMachine().getCurrentState() : null;
                // A fresh press belongs to the current chain only inside that strike's own recovery window.
                // The queued step is explicit so strike two can continue to the finisher instead of restarting.
                if (current == null) attackBufferRemaining = ATTACK_BUFFER_SECONDS;
                else if (current.usesRhythm()) {
                    // Only the first press of a strike is classified; the rest are ignored, never queued.
                    switch (current.getRuntime().registerAttackPress(current.chainsToNext())) {
                        case COMBO_BUFFERED:
                            queuedComboStep = current.getComboStep() + 1;
                            // A press already accepted inside COMBO_READY must survive until this strike ends.
                            // The configurable lifetime remains the floor; one fixed step covers boundary rounding.
                            queuedComboRemaining = Math.max(
                                com.cursedarchie.platformer.combat.AttackRhythmConfig.get().inputBufferTime,
                                current.getRuntime().duration() - current.getRuntime().elapsed() + GameWorld.FIXED_STEP);
                            hero.noteRhythmSuccess();
                            break;
                        case EARLY_INPUT:
                        case LATE_INPUT:
                            clearComboBuffer();
                            break;
                        default: break;
                    }
                }
            }
            attackHeld = true;
        } else if (bindings.matches(Action.HEAVY_ATTACK, keycode)) {
            AttackState now = hero.getCombatStateMachine().getCurrentState() instanceof AttackState ?
                (AttackState) hero.getCombatStateMachine().getCurrentState() : null;
            // A heavy or rushed strike takes no queued input; mashing during it is simply lost.
            boolean locked = now != null && (now.isHeavy() || now.getRuntime().isHurried());
            // Heavy attacks are ground-only: an airborne press is discarded, never buffered for the landing.
            if (!heavyAttackHeld && !locked && heavyAllowedHere()) heavyAttackBufferRemaining =
                now != null ? 1.12f + ATTACK_BUFFER_SECONDS : ATTACK_BUFFER_SECONDS;
            heavyAttackHeld = true;
        } else if (bindings.matches(Action.BLOCK, keycode)) {
            blockHeld = true;
            hero.setBlockHeld(true);
        } else if (bindings.matches(Action.DASH, keycode)) {
            if (!dashHeld) dashQueued = true;
            dashHeld = true;
        } else if (bindings.matches(Action.INTERACT, keycode)) {
            if (!interactHeld) interactQueued = true;
            interactHeld = true;
        } else if (bindings.matches(Action.POCKET_1, keycode)) pocketQueued = 0;
        else if (bindings.matches(Action.POCKET_2, keycode)) pocketQueued = 1;
        return true;
    }
    @Override
    public boolean keyUp(int keycode) {
        if (bindings.matches(Action.LEFT, keycode)) leftHeld = false;
        else if (bindings.matches(Action.RIGHT, keycode)) rightHeld = false;
        else if (bindings.matches(Action.UP, keycode)) upHeld = false;
        else if (bindings.matches(Action.DOWN, keycode)) downHeld = false;
        else if (bindings.matches(Action.JUMP, keycode)) jumpHeld = false;
        else if (bindings.matches(Action.ATTACK, keycode)) attackHeld = false;
        else if (bindings.matches(Action.HEAVY_ATTACK, keycode)) heavyAttackHeld = false;
        else if (bindings.matches(Action.BLOCK, keycode)) {
            blockHeld = false;
            hero.setBlockHeld(false);
        }
        else if (bindings.matches(Action.DASH, keycode)) dashHeld = false;
        else if (bindings.matches(Action.INTERACT, keycode)) interactHeld = false;
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
            attackBufferRemaining = 0f;
            heavyAttackBufferRemaining = 0f;
            clearComboBuffer();
            return;
        }

        dashCooldownRemaining = Math.max(0f, dashCooldownRemaining - delta);
        hero.setPlatformDropRemaining(hero.getPlatformDropRemaining() - delta);
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
        hero.setPlatformDropRemaining(0f);
        climbing = false;
        climbRegrabBlocked = false;
        jumpHeld = false;
        jumpBufferRemaining = 0f;
        dashQueued = false;
        dashHeld = false;
        dashRemaining = 0f;
        if (hero.getBody() != null) hero.getBody().setGravityScale(1f);
        attackHeld = false;
        heavyAttackHeld = false;
        blockHeld = false;
        hero.setBlockHeld(false);
        attackBufferRemaining = 0f;
        heavyAttackBufferRemaining = 0f;
        clearComboBuffer();
        observedHealth = hero.getHealth();
        interactHeld = false;
        interactQueued = false;
        pocketQueued = -1;
    }


    private void checkHeroPositionForDeathFall() {
        if ((hero.getPosition().y + hero.getBounds().height) < 0) {
            hero.setAlive(false);
        }
    }

    private void processInput(float delta) {
        if (interactQueued) {
            gameWorld.interact();
            interactQueued = false;
        }
        if (pocketQueued >= 0) {
            gameWorld.usePocketItem(Inventory.CAPACITY + pocketQueued);
            pocketQueued = -1;
        }
        handleMovementInput(delta);
        handleCombatInput(delta);
    }

    private void handleMovementInput(float delta) {
        AttackState heavyAttack = hero.getCombatStateMachine().getCurrentState() instanceof AttackState &&
            ((AttackState) hero.getCombatStateMachine().getCurrentState()).isHeavy() ?
            (AttackState) hero.getCombatStateMachine().getCurrentState() : null;
        if (hero.isBreakthroughDashing()) {
            // The dash owns the hero's velocity; walking physics, steering and jumping are suspended.
            climbing = false;
            dashQueued = false;
            return;
        }
        if (hero.isStaggered()) {
            climbing = false;
            hero.getBody().setLinearVelocity(approach(hero.getBody().getLinearVelocity().x,
                0f, Hero.AIR_BRAKING * delta), hero.getBody().getLinearVelocity().y);
            return;
        }
        if (hero.isRolling()) {
            // Roll: direction was fixed when it began; steering and gravity-drop are ignored until it ends.
            hero.getBody().setLinearVelocity(hero.getRollDirection() * Hero.ROLL_SPEED, hero.getBody().getLinearVelocity().y);
            dashQueued = false;
            return;
        }
        if (dashQueued && !gameWorld.hasEchoDash()) {
            float dir = leftHeld && !rightHeld ? -1f : rightHeld && !leftHeld ? 1f : 0f;
            if (hero.tryRoll(dir)) {
                climbing = false;
                dashQueued = false;
                return;
            }
        }
        if (dashQueued && gameWorld.hasEchoDash() && dashCooldownRemaining <= 0f) {
            dashRemaining = DASH_SECONDS;
            dashCooldownRemaining = DASH_COOLDOWN;
            climbing = false;
        }
        dashQueued = false;
        if (dashRemaining > 0f) {
            dashRemaining = Math.max(0f, dashRemaining - delta);
            hero.getBody().setGravityScale(0f);
            hero.getBody().setLinearVelocity(hero.isFacingLeft() ? -DASH_SPEED : DASH_SPEED, 0f);
            if (dashRemaining == 0f) hero.getBody().setGravityScale(1f);
            return;
        }
        if (heavyAttack != null) {
            // Charge and recovery are stationary; only HEAVY_DASH changes horizontal position.
            hero.getBody().setLinearVelocity(0f,
                Math.max(hero.getBody().getLinearVelocity().y, -Hero.MAX_FALL_SPEED));
            jumpBufferRemaining = 0f;
            return;
        }
        float direction = 0f;
        boolean isJumpState = hero.getMotionStateMachine().getCurrentState() instanceof JumpState;

        boolean facingLocked = hero.getCombatStateMachine().getCurrentState() instanceof AttackState;
        if (leftHeld && !rightHeld) {
            direction = -1f;
            if (!facingLocked) hero.setFacingLeft(true);
        } else if (rightHeld && !leftHeld) {
            direction = 1f;
            if (!facingLocked) hero.setFacingLeft(false);
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
                if (Math.abs(distance) >= 0.04f)
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
        AttackState attack = hero.getCombatStateMachine().getCurrentState() instanceof AttackState ?
            (AttackState) hero.getCombatStateMachine().getCurrentState() : null;
        float targetX = direction * hero.getVelocity() * (blockHeld ? .55f : 1f);
        // Ground input is direct: reversing or releasing a key takes effect in this step.
        float acceleration = direction == 0f ? Hero.AIR_BRAKING : Hero.AIR_ACCELERATION;
        float moveX = hero.isGrounded() ? targetX : approach(currentX, targetX, acceleration * delta);
        float moveY = Math.max(currentY, -Hero.MAX_FALL_SPEED);
        hero.getBody().setLinearVelocity(moveX, moveY);

        if (jumpBufferRemaining > 0f && hero.isGrounded() && moveY <= 0.1f &&
            !(attack != null && attack.isHeavy())) {
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

    /** A heavy attack may only be started from the ground. */
    private boolean heavyAllowedHere() {
        return hero.isGrounded() && hero.getBody().getLinearVelocity().y < .5f;
    }

    private static float approach(float current, float target, float step) {
        if (current < target) return Math.min(current + step, target);
        return Math.max(current - step, target);
    }
    private void clearComboBuffer() {
        queuedComboStep = 0;
        queuedComboRemaining = 0f;
        comboStep = 0;
    }
    private void handleCombatInput(float delta) {
        boolean tookDamage = hero.getHealth() < observedHealth;
        observedHealth = hero.getHealth();
        if (!hero.isAlive() || tookDamage || hero.isRolling() || dashRemaining > 0f) {
            attackBufferRemaining = 0f;
            heavyAttackBufferRemaining = 0f;
            clearComboBuffer();
            return;
        }
        if (hero.getCombatStateMachine().getCurrentState() instanceof AttackState &&
            ((AttackState) hero.getCombatStateMachine().getCurrentState()).isHeavy()) {
            // Charge, dash and recovery take no attack input at all: anything pressed or buffered is discarded,
            // so no standard strike can follow the heavy attack. A held button is not a new press.
            attackBufferRemaining = 0f;
            heavyAttackBufferRemaining = 0f;
            clearComboBuffer();
            return;
        }
        boolean counterPressed = hero.isCounterReady() && attackBufferRemaining > 0f && !hero.isStaggered() &&
            !(hero.getCombatStateMachine().getCurrentState() instanceof AttackState);
        if (counterPressed) {
            hero.consumeCounter();
            blockHeld = false;
            hero.setBlockHeld(false);
            clearComboBuffer();
            attackBufferRemaining = 0f;
            hero.getCombatStateMachine().changeState(new AttackState(false, AttackState.COUNTER_ATTACK));
            return;
        }
        if (blockHeld || hero.isStaggered()) {
            attackBufferRemaining = 0f;
            heavyAttackBufferRemaining = 0f;
            clearComboBuffer();
            return;
        }
        if (!(hero.getCombatStateMachine().getCurrentState() instanceof AttackState)) {
            if (heavyAttackBufferRemaining > 0f && (!hero.canHeavyAttack() || !heavyAllowedHere()))
                heavyAttackBufferRemaining = 0f;
            if (heavyAttackBufferRemaining > 0f) {
                hero.beginHeavyCooldown();
                hero.getCombatStateMachine().changeState(new AttackState(true, 0));
                heavyAttackBufferRemaining = 0f;
                attackBufferRemaining = 0f;
                clearComboBuffer();
            } else if (queuedComboStep > 0) {
                comboStep = queuedComboStep;
                hero.getCombatStateMachine().changeState(new AttackState(false, comboStep));
                queuedComboStep = 0;
                queuedComboRemaining = 0f;
                attackBufferRemaining = 0f;
            } else if (attackBufferRemaining > 0f && hero.canJumpAttack()) {
                hero.markJumpAttackUsed();
                comboStep = 0;
                hero.getCombatStateMachine().changeState(new AttackState(false, AttackState.JUMP_ATTACK));
                attackBufferRemaining = 0f;
            } else if (attackBufferRemaining > 0f) {
                comboStep = AttackState.COMBO_FIRST;
                hero.getCombatStateMachine().changeState(new AttackState(false, comboStep));
                attackBufferRemaining = 0f;
            }
        }
        attackBufferRemaining = Math.max(0f, attackBufferRemaining - delta);
        heavyAttackBufferRemaining = Math.max(0f, heavyAttackBufferRemaining - delta);
        if (queuedComboStep > 0) {
            queuedComboRemaining = Math.max(0f, queuedComboRemaining - delta);
            if (queuedComboRemaining == 0f) clearComboBuffer();
        }
    }
}
