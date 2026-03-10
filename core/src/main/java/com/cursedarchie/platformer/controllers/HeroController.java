package com.cursedarchie.platformer.controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.actors.hero.HeroPerception;
import com.cursedarchie.platformer.actors.hero.states.combat.AttackState;
import com.cursedarchie.platformer.actors.hero.states.motion.IdleState;
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

    private static final float MOVE_SPEED = 5f;
    private static final float JUMP_FORCE = 2f;
    private final GameWorld gameWorld;
    private final Hero hero;
    private boolean attackKeyWasPressed = false;
    private final HeroPerception perception;
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
        this.perception = new HeroPerception(gameWorld);
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

        keys.put(Keys.LEFT, Gdx.input.isKeyPressed(Input.Keys.LEFT));
        keys.put(Keys.RIGHT, Gdx.input.isKeyPressed(Input.Keys.RIGHT));
        keys.put(Keys.JUMP, Gdx.input.isKeyPressed(Input.Keys.SPACE));
        keys.put(Keys.ATTACK, Gdx.input.isKeyPressed(Input.Keys.X));

        processInput();

        checkPositionForLevelChange();
        checkHeroPositionForDeathFall();

        hero.update();
        perceptionLogic(hero);
        hero.getMotionStateMachine().update(delta);
        hero.getCombatStateMachine().update(delta);
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
        Vector2 velocity = hero.getBody().getLinearVelocity();
        float moveX = 0;

        if (keys.get(Keys.LEFT)) {
            moveX = -MOVE_SPEED;
            hero.setFacingLeft(true);

            if (!(hero.getMotionStateMachine().getCurrentState() instanceof JumpState)) {
                hero.getMotionStateMachine().changeState(new MoveState());
            }
        } else if (keys.get(Keys.RIGHT)) {
            moveX = MOVE_SPEED;
            hero.setFacingLeft(false);

            if (!(hero.getMotionStateMachine().getCurrentState() instanceof JumpState)) {
                hero.getMotionStateMachine().changeState(new MoveState());
            }
        } else {
            if (hero.isGrounded()) {
                hero.getMotionStateMachine().changeState(new IdleState());
            }
        }

        hero.getBody().setLinearVelocity(moveX, velocity.y);

        if (keys.get(Keys.JUMP) && hero.isGrounded()) {
            hero.getBody().applyLinearImpulse(
                new Vector2(0, JUMP_FORCE),
                hero.getBody().getWorldCenter(),
                true
            );
            hero.setGrounded(false);
            hero.getMotionStateMachine().changeState(new JumpState());
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

    private void perceptionLogic(Hero hero) {
        perception.checkAttackingEnemies(hero);
    }
}
