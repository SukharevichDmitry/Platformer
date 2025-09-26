package com.cursedarchie.platformer.utils;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.cursedarchie.platformer.actors.tiles.Tile;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.Hero;
import com.cursedarchie.platformer.actors.Hero.State;
import com.cursedarchie.platformer.world.World;

import java.util.HashMap;
import java.util.Map;


public class HeroController {

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
    private Hero hero;
    private long jumpPressedTime;
    private boolean jumpingPressed;
    private boolean grounded = false;
    private boolean attackingPressed = false;
    private boolean attackHandled = false;

    private Pool<Rectangle> rectPool = new Pool<Rectangle>() {
        @Override
        protected Rectangle newObject() {
            return new Rectangle();
        }
    };

    static Map<Keys, Boolean> keys = new HashMap<HeroController.Keys, Boolean>();
    static {
        keys.put(Keys.LEFT, false);
        keys.put(Keys.RIGHT, false);
        keys.put(Keys.JUMP, false);
        keys.put(Keys.ATTACK, false);
    };

    private Array<Tile> collidable = new Array<Tile>();

    public HeroController(World world) {
        this.world = world;
        this.hero = world.getHero();
    }

    public void leftPressed() {
        keys.get(keys.put(Keys.LEFT, true));
    }
    public void rightPressed() {
        keys.get(keys.put(Keys.RIGHT, true));
    }
    public void jumpPressed() {
        keys.get(keys.put(Keys.JUMP, true));
    }
    public void attackPressed() {
        keys.get(keys.put(Keys.ATTACK, true));
    }
    public void leftReleased() {
        keys.get(keys.put(Keys.LEFT, false));
    }
    public void rightReleased() {
        keys.get(keys.put(Keys.RIGHT, false));
    }
    public void jumpReleased() {
        keys.get(keys.put(Keys.JUMP, false));
        jumpingPressed = false;
    }
    public void attackReleased() {
        keys.get(keys.put(Keys.ATTACK, false));
        attackingPressed = false;
    }

    public void update(float delta) {
        processInput();

        if (grounded && hero.getState().equals(State.JUMPING)) {
            hero.setState(State.IDLE);
        }

        hero.getAcceleration().y = GRAVITY;
        hero.getAcceleration().scl(delta);
        hero.getVelocity().add(hero.getAcceleration().x, hero.getAcceleration().y);
        checkCollisionWithTiles(delta);
        checkHeroPositionForLevelChange();

        hero.getVelocity().x *= DAMP;
        if (hero.getVelocity().x > MAX_VEL) {
            hero.getVelocity().x = MAX_VEL;
        }
        if (hero.getVelocity().x < -MAX_VEL) {
            hero.getVelocity().x = -MAX_VEL;
        }

        heroDying();
        checkHeroPositionForDeathFall();

        hero.update(delta);

        if (hero.getState().equals(State.ATTACKING)) {
            checkAttackingEnemies();
        } else attackHandled = false;
    }

    private void checkCollisionWithTiles(float delta) {
        hero.getVelocity().scl(delta);

        Rectangle heroRect = rectPool.obtain();
        heroRect.set(
            hero.getBounds().x,
            hero.getBounds().y,
            hero.getBounds().width,
            hero.getBounds().height
        );

        int startX, endX;
        int startY = (int) hero.getBounds().y;
        int endY = (int) (hero.getBounds().y + hero.getBounds().height);

        if (hero.getVelocity().x < 0) {
            startX = endX = (int) Math.floor(hero.getBounds().x + hero.getVelocity().x);
        } else {
            startX = endX = (int) Math.floor(hero.getBounds().x + hero.getBounds().width + hero.getVelocity().x);
        }

        populateCollidableBlocks(startX, startY, endX, endY);

        heroRect.x += hero.getVelocity().x;

        world.getHeroCollisionRects().clear();

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

        startX = (int) hero.getBounds().x;
        endX = (int) (hero.getBounds().x + hero.getBounds().width);

        if (hero.getVelocity().y < 0) {
            startY = endY = (int) Math.floor(hero.getBounds().y + hero.getVelocity().y);
        } else {
            startY = endY = (int) Math.floor(hero.getBounds().y + hero.getBounds().height + hero.getVelocity().y);
        }
        populateCollidableBlocks(startX, startY, endX, endY);

        heroRect.y += hero.getVelocity().y;

        for (Tile tile : collidable) {
            if (tile == null) continue;
            if (heroRect.overlaps(tile.getBounds())) {
                world.getHeroCollisionRects().add(tile.getBounds());
                if (hero.getVelocity().y > 0) {
                    hero.getPosition().y = tile.getBounds().y - hero.getBounds().height;
                } else {
                    hero.getPosition().y = tile.getBounds().y + tile.getBounds().height;
                    grounded = true;
                }
                hero.getVelocity().y = 0;
                break;
            }
        }


        heroRect.y = hero.getPosition().y;

        hero.getBounds().x = hero.getPosition().x;
        hero.getBounds().y = hero.getPosition().y;


        hero.getVelocity().scl(1 / (delta + 0.00001f));
    }

    private void populateCollidableBlocks(int startX, int startY, int endX, int endY) {
        collidable.clear();
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                if (x >= 0 && x < world.getLevel().getWidth() &&
                    y >= 0 && y < world.getLevel().getHeight()) {
                    collidable.add(world.getLevel().get(x, y));
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
    }

    private void checkAttackingEnemies () {
        if (attackHandled) return;


        Rectangle attackRect = rectPool.obtain();
        if (hero.isFacingLeft()) {
            attackRect.set(
                hero.getBounds().x - hero.getBounds().width/2,
                hero.getBounds().y - hero.getBounds().height/2,
                hero.getBounds().width,
                hero.getBounds().height
            );
        } else {
            attackRect.set(
                hero.getBounds().x + hero.getBounds().width/2,
                hero.getBounds().y + hero.getBounds().height/2,
                hero.getBounds().width,
                hero.getBounds().height
            );
        }

        Array<Enemy> enemies = world.getLevel().getEnemies();

        for (Enemy enemy : enemies) {
            if (attackRect.overlaps(enemy.getBounds())) {
                enemy.takeDamage(hero.getDamage());
                attackHandled = true;
            }

        }
    }

    private boolean processInput() {
        if (keys.get(Keys.JUMP)) {
            if (!hero.getState().equals(State.JUMPING)) {
                jumpingPressed = true;
                jumpPressedTime = System.currentTimeMillis();
                hero.setState(State.JUMPING);
                hero.getVelocity().y = MAX_JUMP_SPEED;
                grounded = false;
            } else {
                if (jumpingPressed && ((System.currentTimeMillis() - jumpPressedTime) >= LONG_JUMP_PRESS)) {
                    jumpingPressed = false;
                } else {
                    if (jumpingPressed) {
                        hero.getVelocity().y = MAX_JUMP_SPEED;
                    }
                }
            }
        }
        if (keys.get(Keys.LEFT)) {
            hero.setFacingLeft(true);
            if (!hero.getState().equals(State.JUMPING)) {
                hero.setState(State.WALKING);
            }
            hero.getAcceleration().x = -ACCELERATION;
        } else if (keys.get(Keys.RIGHT)) {
            hero.setFacingLeft(false);
            if (!hero.getState().equals(State.JUMPING)) {
                hero.setState(State.WALKING);
            }
            hero.getAcceleration().x = ACCELERATION;
        } else {
            if (!hero.getState().equals(State.JUMPING) && !hero.getState().equals(State.ATTACKING)) {
                hero.setState(State.IDLE);
            }
            hero.getAcceleration().x = 0;
        }

        if (keys.get(Keys.LEFT) && keys.get(Keys.RIGHT)){
            if (!hero.getState().equals(State.JUMPING)) {
                hero.setState(State.IDLE);
            }
            hero.getAcceleration().x = 0;
        }

        if (keys.get(Keys.ATTACK) && !attackingPressed) {
            hero.setState(State.ATTACKING);
            hero.setStateTime(0);
            attackingPressed = true;
        }

        return false;
    }

    private void heroDying() {
        if (hero.getHealth() <= 0) {
            hero.setAlive(false);
        }
    }
}
