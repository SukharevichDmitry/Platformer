package com.cursedarchie.platformer.utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.enemies.logic.EnemyPerception;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.actors.Enemy;
import com.cursedarchie.platformer.world.World;

public class EnemyController {

    private static final float GRAVITY 			= -60f;
    private static final float DAMP 			= 0.85f;
    private static final float MAX_VEL 			= 6f;
    private final World world;
    private Array<Enemy> enemies;
    private final EnemyPerception perception;

    private final Array<Tile> collidable = new Array<Tile>();

    public EnemyController(World world) {
        this.world = world;
        this.enemies = world.getLevel().getAliveEnemies();
        this.perception = new EnemyPerception(world);
    }

    public void updateEnemies() {
        this.enemies = world.getLevel().getAliveEnemies();
    }

    public void update(float delta) {
        updateEnemies();
        world.getEnemyCollisionRects().clear();

        for (Enemy enemy : this.enemies) {
            if (enemy.isAlive()){
                updateVelocity(delta, enemy);
                checkCollisionWithTiles(delta, enemy);
                enemy.update(delta);

                perceptionLogic(enemy);
                enemy.getStateMachine().update(delta);
            }
        }
    }

    private void updateVelocity(float delta, Enemy enemy) {
        enemy.getVelocity().x += enemy.getAcceleration().x * delta;
        enemy.getVelocity().y += GRAVITY * delta;

        enemy.getVelocity().x *= DAMP;
        enemy.getVelocity().x = MathUtils.clamp(enemy.getVelocity().x, -MAX_VEL, MAX_VEL);
    }

    private void checkCollisionWithTiles(float delta, Enemy enemy) {
        enemy.setGrounded(false);
        enemy.getVelocity().scl(delta);

        Rectangle enemyRect = enemy.getRectPool().obtain();
        enemyRect.set(
            enemy.getBounds().x,
            enemy.getBounds().y,
            enemy.getBounds().width,
            enemy.getBounds().height
        );

        int startY = (int) enemy.getBounds().y;
        int endY = (int) (enemy.getBounds().y + enemy.getBounds().height);

        checkHorizontalCollisionWithTiles(enemy, enemyRect, startY, endY);
        checkVerticalCollisionWithTiles(enemy, enemyRect, startY, endY);

        enemy.getVelocity().scl(1 / (delta + 0.00001f));

        enemy.getBounds().setPosition(enemy.getPosition());

        enemy.getRectPool().free(enemyRect);
    }
    private void checkHorizontalCollisionWithTiles(Enemy enemy, Rectangle enemyRect, int startY, int endY) {
        int startX, endX;

        if (enemy.getVelocity().x < 0) {
            startX = endX = (int) Math.floor(enemy.getBounds().x + enemy.getVelocity().x);
        } else {
            startX = endX = (int) Math.floor(enemy.getBounds().x + enemy.getBounds().width + enemy.getVelocity().x);
        }

        populateCollidableTiles(startX, startY, endX, endY);
        enemyRect.x += enemy.getVelocity().x;

        for (Tile tile : collidable) {
            if (tile != null && enemyRect.overlaps(tile.getBounds())) {
                world.getEnemyCollisionRects().add(tile.getBounds());
                if (enemy.getVelocity().x > 0) {
                    enemy.getPosition().x = tile.getBounds().x - enemy.getBounds().width;
                } else {
                    enemy.getPosition().x = tile.getBounds().x + tile.getBounds().width;
                }
                enemy.getVelocity().x = 0;
                break;
            }
        }

        enemyRect.x = enemy.getPosition().x;
    }
    private void checkVerticalCollisionWithTiles(Enemy enemy, Rectangle enemyRect, int startY, int endY) {
        int startX = (int) enemy.getBounds().x;
        int endX = (int) (enemy.getBounds().x + enemy.getBounds().width);

        if (enemy.getVelocity().y < 0) {
            startY = endY = (int) Math.floor(enemy.getBounds().y + enemy.getVelocity().y);
        } else {
            startY = endY = (int) Math.floor(enemy.getBounds().y + enemy.getBounds().height + enemy.getVelocity().y);
        }
        populateCollidableTiles(startX, startY, endX, endY);

        enemyRect.y += enemy.getVelocity().y;

        for (Tile tile : collidable) {
            if (tile == null) continue;
            if (enemyRect.overlaps(tile.getBounds())) {
                world.getEnemyCollisionRects().add(tile.getBounds());
                if (enemy.getVelocity().y > 0) {
                    enemy.getPosition().y = tile.getBounds().y - enemy.getBounds().height;
                } else {
                    enemy.getPosition().y = tile.getBounds().y + tile.getBounds().height;
                    enemy.setGrounded(true);
                }
                enemy.getVelocity().y = 0;
                break;
            }
        }

        enemyRect.y = enemy.getPosition().y;
    }
    private void populateCollidableTiles(int startX, int startY, int endX, int endY) {
        collidable.clear();
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                if (x >= 0 && x < world.getLevel().getWidth() &&
                    y >= 0 && y < world.getLevel().getHeight()) {
                    collidable.add(world.getLevel().getTile(x, y));
                }
            }
        }
    }

    private void perceptionLogic(Enemy enemy) {
        perception.canSeeHero(enemy);
        perception.checkHeroToAttack(enemy);

        if (enemy.isCanDealDamage()) {
            perception.dealDamage(enemy);
        }
    }
}
