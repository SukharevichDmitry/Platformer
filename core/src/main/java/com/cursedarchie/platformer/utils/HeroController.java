package com.cursedarchie.platformer.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.hero.HeroPerception;
import com.cursedarchie.platformer.actors.hero.states.HeroAttackState;
import com.cursedarchie.platformer.actors.hero.states.HeroIdleState;
import com.cursedarchie.platformer.actors.hero.states.HeroJumpState;
import com.cursedarchie.platformer.actors.hero.states.HeroMoveState;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.actors.Hero;
import com.cursedarchie.platformer.actors.Hero.HeroState;
import com.cursedarchie.platformer.world.World;

import java.util.HashMap;
import java.util.Map;


public class HeroController implements InputProcessor {

    enum Keys {
        LEFT, RIGHT, JUMP, ATTACK
    }

    private static final long LONG_JUMP_PRESS 	= 150L;
    private static final float ACCELERATION 	= 60f;
    private static final float GRAVITY 			= -60f;
    private static final float MAX_JUMP_SPEED	= 11f;
    private static final float DAMP 			= 0.90f;
    private static final float MAX_VEL 			= 8f;


    private final World world;
    private final Hero hero;
//    private boolean attackKeyWasPressed = false;


    private final HeroPerception perception;

    private final Map<Keys, Boolean> keys = new HashMap<HeroController.Keys, Boolean>();
    {
        keys.put(Keys.LEFT, false);
        keys.put(Keys.RIGHT, false);
        keys.put(Keys.JUMP, false);
        keys.put(Keys.ATTACK, false);
    };

    private Array<Tile> collidable = new Array<Tile>();

    public HeroController(World world) {
        Gdx.app.log("Hero Controller", "Initializing Hero Controller");
        this.world = world;
        this.hero = world.getHero();
        this.perception = new HeroPerception(world);
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

        if (hero.isAlive()) {
            keys.put(Keys.LEFT, Gdx.input.isKeyPressed(Input.Keys.LEFT));
            keys.put(Keys.RIGHT, Gdx.input.isKeyPressed(Input.Keys.RIGHT));
            keys.put(Keys.JUMP, Gdx.input.isKeyPressed(Input.Keys.SPACE));
            keys.put(Keys.ATTACK, Gdx.input.isKeyPressed(Input.Keys.X));

            processInput();

            if (hero.isGrounded() && hero.getState().equals(HeroState.JUMP)) {
                hero.setState(HeroState.IDLE);
            }

            updateVelocity(delta, hero);
            checkCollisionWithTiles(delta);
            checkHeroPositionForLevelChange();
            checkHeroPositionForDeathFall();
            hero.update(delta);

            perceptionLogic(hero);
            hero.getStateMachine().update(delta);
        }
    }

    private void updateVelocity(float delta, Hero hero) {
        hero.getVelocity().x += hero.getAcceleration().x * delta;
        hero.getVelocity().y += GRAVITY * delta;

        hero.getVelocity().x *= DAMP;
        hero.getVelocity().x = MathUtils.clamp(hero.getVelocity().x, -MAX_VEL, MAX_VEL);
    }

    private void checkCollisionWithTiles(float delta) {
        hero.getVelocity().scl(delta);

        Rectangle heroRect = hero.getRectPool().obtain();
        heroRect.set(
            hero.getBounds().x,
            hero.getBounds().y,
            hero.getBounds().width,
            hero.getBounds().height
        );

        world.getHeroCollisionRects().clear();

        int startY = (int) hero.getBounds().y;
        int endY = (int) (hero.getBounds().y + hero.getBounds().height);

        checkHorizontalCollisionWithTiles(hero, heroRect, startY, endY);
        checkVerticalCollisionWithTiles(hero, heroRect, startY, endY);

        hero.getVelocity().scl(1 / (delta));

        hero.getRectPool().free(heroRect);
    }
    private void checkHorizontalCollisionWithTiles(Hero hero, Rectangle heroRect, int startY, int endY) {
        int startX, endX;

        if (hero.getVelocity().x < 0) {
            startX = endX = (int) Math.floor(hero.getBounds().x + hero.getVelocity().x);
        } else {
            startX = endX = (int) Math.floor(hero.getBounds().x + hero.getBounds().width + hero.getVelocity().x);
        }
        populateCollidableTiles(startX, startY, endX, endY);
        heroRect.x += hero.getVelocity().x;

        for (Tile tile : collidable) {
            if (tile == null) continue;
            if (heroRect.overlaps(tile.getBounds())) {
                world.getHeroCollisionRects().add(tile.getBounds());
                if (hero.getVelocity().x > 0) {
                    hero.getPosition().x = tile.getBounds().x - hero.getBounds().width;
                } else {
                    hero.getPosition().x = tile.getBounds().x + tile.getBounds().width;
                }
                hero.getVelocity().x = 0;
                break;
            }
        }
        heroRect.x = hero.getPosition().x;
    }
    private void checkVerticalCollisionWithTiles(Hero hero, Rectangle heroRect, int startY, int endY) {
        int startX = (int) hero.getBounds().x;
        int endX = (int) (hero.getBounds().x + hero.getBounds().width);

        if (hero.getVelocity().y < 0) {
            startY = endY = (int) Math.floor(hero.getBounds().y + hero.getVelocity().y);
        } else {
            startY = endY = (int) Math.floor(hero.getBounds().y + hero.getBounds().height + hero.getVelocity().y);
        }
        populateCollidableTiles(startX, startY, endX, endY);
        heroRect.y += hero.getVelocity().y;

        for (Tile tile : collidable) {
            if (tile == null) continue;
            if (heroRect.overlaps(tile.getBounds())) {
                world.getHeroCollisionRects().add(tile.getBounds());
                if (hero.getVelocity().y > 0) {
                    hero.getPosition().y = tile.getBounds().y - hero.getBounds().height;
                } else {
                    hero.getPosition().y = tile.getBounds().y + tile.getBounds().height;
                    hero.setGrounded(true);
                }
                hero.getVelocity().y = 0;
                break;
            }
        }

        heroRect.y = hero.getPosition().y;
    }
    private void populateCollidableTiles(int startX, int startY, int endX, int endY) {
        collidable.clear();
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                if (x >= 0 && x < world.getLevel().getWidth() &&
                    y >= 0 && y < world.getLevel().getHeight()) {
                    collidable.add(world.getLevel().getTile(x, y));
                    Gdx.app.log("populate col t", x + " " + y);
                }
            }
        }
    }

    private void checkHeroPositionForLevelChange() {
        if ((hero.getPosition().x + hero.getBounds().width) < 0) {
            world.goToPreviousLevel();
        } else if ((hero.getPosition().x) > world.getLevel().getWidth()) {
            world.goToNextLevel();
        }
    }
    private void checkHeroPositionForDeathFall() {
        if ((hero.getPosition().y + hero.getBounds().height) < 0) {
            hero.setAlive(false);
        }
        Gdx.app.log("hero velocity", hero.getVelocity().toString());
    }

    private void processInput() {
//        boolean attackKeyPressed = keys.get(Keys.ATTACK);
//        if (attackKeyPressed && !attackKeyWasPressed) {
//            hero.getStateMachine().changeState(new HeroAttackState());
//            hero.setAttackPressed(true);
//        }
//        attackKeyWasPressed = attackKeyPressed;

        if(keys.get(Keys.ATTACK)) {
            hero.setAttackHandled(true);
            if(!(hero.getStateMachine().getCurrentState() instanceof HeroAttackState)) {
                hero.getStateMachine().changeState(new HeroAttackState());
            }
        }

        if (keys.get(Keys.JUMP)) {
            hero.setJumpPressed(true);
            if (!(hero.getStateMachine().getCurrentState() instanceof HeroJumpState)) {
                hero.getVelocity().y = MAX_JUMP_SPEED;
                hero.setGrounded(false);
                hero.getStateMachine().changeState(new HeroJumpState());
            }
        } else {
            hero.setJumpPressed(false);
        }

        if (keys.get(Keys.LEFT)) {
            hero.setFacingLeft(true);
            hero.getAcceleration().x = -ACCELERATION;


            if (!(hero.getStateMachine().getCurrentState() instanceof HeroJumpState) &&
                !(hero.getStateMachine().getCurrentState() instanceof HeroMoveState)) {
                hero.getStateMachine().changeState(new HeroMoveState());
            }
        }

        else if (keys.get(Keys.RIGHT)) {
            hero.setFacingLeft(false);
            hero.getAcceleration().x = ACCELERATION;

            if (!(hero.getStateMachine().getCurrentState() instanceof HeroJumpState) &&
                !(hero.getStateMachine().getCurrentState() instanceof HeroMoveState)) {
                hero.getStateMachine().changeState(new HeroMoveState());
            }
        }

        else {
            hero.getAcceleration().x = 0;
            if (!(hero.getStateMachine().getCurrentState() instanceof HeroJumpState) &&
                !(hero.getStateMachine().getCurrentState() instanceof HeroAttackState) &&
                !(hero.getStateMachine().getCurrentState() instanceof HeroIdleState) &&
                !(hero.getStateMachine().getCurrentState() instanceof HeroMoveState)) {
                hero.getStateMachine().changeState(new HeroIdleState());
            }
        }

    }

    private void perceptionLogic(Hero hero) {
        perception.checkAttackingEnemies(hero);
    }
}
