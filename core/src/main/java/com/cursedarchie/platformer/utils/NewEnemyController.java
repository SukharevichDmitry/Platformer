package com.cursedarchie.platformer.utils;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.cursedarchie.platformer.actors.enemies.logic.EnemyPerception;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.actors.NewEnemy;
import com.cursedarchie.platformer.world.World;

public class NewEnemyController{

    private static final float GRAVITY 			= -60f;
    private static final float DAMP 			= 0.85f;
    private static final float MAX_VEL 			= 6f;


    private final World world;
    private Array<NewEnemy> enemies;
    private final EnemyPerception perception;

    private final Pool<Rectangle> rectPool = new Pool<Rectangle>() {
        @Override
        protected Rectangle newObject() {
            return new Rectangle();
        }
    };

    private final Array<Tile> collidable = new Array<Tile>();

    public NewEnemyController(World world) {
        this.world = world;
        this.enemies = world.getLevel().getEnemies();
        this.perception = new EnemyPerception(world);
    }

    public void updateEnemies() {
        this.enemies = world.getLevel().getEnemies();
    }

        public void update(float delta) {
            updateEnemies();

            world.getEnemyCollisionRects().clear();
            for (NewEnemy enemy : new Array<>(enemies)) {
                if (enemy.isAlive()){

                    enemy.getAcceleration().y = GRAVITY;
                    enemy.getAcceleration().scl(delta);
                    enemy.getVelocity().add(enemy.getAcceleration().x, enemy.getAcceleration().y);
                    checkCollisionWithTiles(delta);

                    enemy.getVelocity().x *= DAMP;
                    if (enemy.getVelocity().x > MAX_VEL) {
                        enemy.getVelocity().x = MAX_VEL;
                    }
                    if (enemy.getVelocity().x < -MAX_VEL) {
                        enemy.getVelocity().x = -MAX_VEL;
                    }

                    enemy.getPosition().add(enemy.getVelocity().cpy().scl(delta));
                    enemy.getBounds().x = enemy.getPosition().x;
                    enemy.getBounds().y = enemy.getPosition().y;
                    enemy.setStateTime(enemy.getStateTime() + delta);

                    // Немного не та логика, которую хотелось бы. Грязно сука
                    perception.canSeeHero(enemy);
                    perception.checkHeroToAttack(delta, enemy);

                    if(enemy.isCanSeeHero()) {
                        perception.checkHeroToAttack(delta, enemy);
                    }
                    if (enemy.isCanDealDamage()) {
                        perception.dealDamage(enemy);
                    }

                    enemy.getStateMachine().update(delta);
                }
            }
        }

    private void checkCollisionWithTiles(float delta) {
        for (NewEnemy enemy : enemies) {
            enemy.setGrounded(false);
            enemy.getVelocity().scl(delta);

            Rectangle enemyRect = rectPool.obtain();
            enemyRect.set(
                enemy.getBounds().x,
                enemy.getBounds().y,
                enemy.getBounds().width,
                enemy.getBounds().height
            );

            int startX, endX;
            int startY = (int) enemy.getBounds().y;
            int endY = (int) (enemy.getBounds().y + enemy.getBounds().height);

            if (enemy.getVelocity().x < 0) {
                startX = endX = (int) Math.floor(enemy.getBounds().x + enemy.getVelocity().x);
            } else {
                startX = endX = (int) Math.floor(enemy.getBounds().x + enemy.getBounds().width + enemy.getVelocity().x);
            }

            populateCollidableTiles(startX, startY, endX, endY);

            enemyRect.x += enemy.getVelocity().x;



            for (Tile tile : collidable) {
                if (tile == null) continue;
                if (enemyRect.overlaps(tile.getBounds())) {
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

            startX = (int) enemy.getBounds().x;
            endX = (int) (enemy.getBounds().x + enemy.getBounds().width);

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

            enemy.getBounds().x = enemy.getPosition().x;
            enemy.getBounds().y = enemy.getPosition().y;

            enemy.getVelocity().scl(1 / (delta + 0.00001f));
        }
    }

    private void populateCollidableTiles(int startX, int startY, int endX, int endY) {
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

}
