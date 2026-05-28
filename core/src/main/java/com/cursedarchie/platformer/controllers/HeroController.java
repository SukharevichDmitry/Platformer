package com.cursedarchie.platformer.controllers;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.actors.hero.states.combat.AttackState;
import com.cursedarchie.platformer.actors.hero.states.motion.MotionIdleState;
import com.cursedarchie.platformer.actors.hero.states.motion.JumpState;
import com.cursedarchie.platformer.actors.hero.states.motion.MoveState;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.world.GameWorld;

import java.util.HashMap;
import java.util.Map;


public class HeroController implements InputProcessor {

    enum Keys {
        LEFT, RIGHT, JUMP, ATTACK
    }

    private final GameWorld gameWorld;
    private final Hero hero;
    private boolean attackKeyWasPressed = false;
    private final Map<Keys, Boolean> keys = new HashMap<>();
    {
        keys.put(Keys.LEFT, false);
        keys.put(Keys.RIGHT, false);
        keys.put(Keys.JUMP, false);
        keys.put(Keys.ATTACK, false);
    }

    public HeroController(GameWorld gameWorld) {
        this.gameWorld = gameWorld;
        this.hero = gameWorld.getHero();
    }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.LEFT:
                keys.put(Keys.LEFT, true);
                break;
            case Input.Keys.RIGHT:
                keys.put(Keys.RIGHT, true);
                break;
            case Input.Keys.SPACE:
                keys.put(Keys.JUMP, true);
                break;
            case Input.Keys.X:
                keys.put(Keys.ATTACK, true);
                break;
        }
        return true;
    }
    @Override
    public boolean keyUp(int keycode) {
        switch (keycode) {
            case Input.Keys.LEFT:
                keys.put(Keys.LEFT, false);
                break;
            case Input.Keys.RIGHT:
                keys.put(Keys.RIGHT, false);
                break;
            case Input.Keys.SPACE:
                keys.put(Keys.JUMP, false);
                break;
            case Input.Keys.X:
                keys.put(Keys.ATTACK, false);
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

        processInput();

        checkPositionForLevelChange();
        checkHeroPositionForDeathFall();

        hero.update(delta);
    }

    private void checkPositionForLevelChange() {
        float heroLeft = hero.getPosition().x - hero.getSize() / 2f;
        float heroRight = hero.getPosition().x + hero.getSize() / 2f;

        float width = gameWorld.getLevel().getWidth();
        float height = gameWorld.getLevel().getHeight();

        if (heroRight < hero.getSize() / 2f) {
            gameWorld.goToPreviousLevel();
        } else if (heroLeft > width - hero.getSize() / 2f) {
            gameWorld.goToNextLevel();
        }
    }
    private void checkHeroPositionForDeathFall() {
        if ((hero.getPosition().y + hero.getBounds().height) < 0) {
            hero.setAlive(false);
        }
    }

    private void processInput() {
        handleMovementInput();
        handleCombatInput();
    }

    private void handleMovementInput() {
        float moveX = 0;
        boolean leftKey = keys.get(Keys.LEFT);
        boolean rightKey = keys.get(Keys.RIGHT);
        boolean jumpKey = keys.get(Keys.JUMP);
        boolean isJumpState = hero.getMotionStateMachine().getCurrentState() instanceof JumpState;

        if (leftKey) {
            moveX = -hero.getVelocity();
            hero.setFacingLeft(true);
        }
        if (rightKey) {
            moveX = hero.getVelocity();
            hero.setFacingLeft(false);
        }

        hero.getBody().setLinearVelocity(moveX, hero.getBody().getLinearVelocity().y);

        if (jumpKey && hero.isGrounded()) {
            hero.getBody().applyLinearImpulse(
                new Vector2(0, hero.getJumpForce()),
                hero.getBody().getWorldCenter(),
                true
            );
            hero.getMotionStateMachine().changeState(new JumpState());
        }

        if (!isJumpState) {
            if (moveX != 0) {
                hero.getMotionStateMachine().changeState(new MoveState());
            } else {
                hero.getMotionStateMachine().changeState(new MotionIdleState());
            }
        }
    }
    private void handleCombatInput() {
        if (keys.get(Keys.ATTACK)) {
            if (!attackKeyWasPressed) {
                hero.getCombatStateMachine().changeState(new AttackState());
                attackKeyWasPressed = true;
            }
        } else {
            attackKeyWasPressed = false;
        }
    }
}
