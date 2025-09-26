package com.cursedarchie.platformer.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.cursedarchie.platformer.actors.tiles.Tile;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.Enemy.EnemyState;
import com.cursedarchie.platformer.actors.Hero;
import com.cursedarchie.platformer.world.World;


public class EnemyController{

    private static final float GRAVITY 			= -60f;
    private static final float DAMP 			= 0.85f;
    private static final float MAX_VEL 			= 6f;


    private final World world;
    private Array<Enemy> enemies;

    private final Pool<Rectangle> rectPool = new Pool<Rectangle>() {
        @Override
        protected Rectangle newObject() {
            return new Rectangle();
        }
    };

    private final Array<Tile> collidable = new Array<Tile>();

    public EnemyController(World world) {
        this.world = world;
        this.enemies = world.getLevel().getEnemies();
    }

    public void updateEnemies() {
        this.enemies = world.getLevel().getEnemies();
    }

    public void update(float delta) {
        if (this.enemies.size != world.getLevel().getEnemies().size) {
            updateEnemies();
        }

        world.getEnemyCollisionRects().clear();
        for (Enemy enemy : new Array<>(enemies)) {
            if (enemy.isAlive()){
                if (enemy.isGrounded() && enemy.getState().equals(EnemyState.JUMPING)) {
                    enemy.setState(EnemyState.IDLE);
                }

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

                enemy.update(delta);
                enemyDying(enemy);
                noticeHero(enemy);

                if(enemy.isHeroInSight()) {
                    chaseHero(enemy);
                    checkHeroToAttack(delta, enemy);
                } else {
                    if (enemy.getState() != EnemyState.ATTACKING && enemy.getState() != EnemyState.JUMPING) {
                        enemy.setState(EnemyState.IDLE);
                        enemy.getAcceleration().x = 0;
                    }
                }
            }
        }
    }

    private void noticeHero(Enemy enemy) {
        Hero hero = world.getHero();

        Rectangle enemyRect = enemy.getBounds();
        Rectangle heroRect = hero.getBounds();

        Vector2 enemyCenter = new Vector2(enemyRect.x + enemyRect.width / 2f, enemyRect.y + enemyRect.height / 2f);
        Vector2 heroCenter = new Vector2(heroRect.x + heroRect.width / 2f, heroRect.y + heroRect.height / 2f);

        Vector2 toHero = new Vector2(heroCenter).sub(enemyCenter).nor();

        Vector2 enemyDirection = enemy.isFacingLeft() ? new Vector2(-1, 0) : new Vector2(1, 0);

        float dot = toHero.dot(enemyDirection);
        float viewCosThreshold = (float) Math.cos(Math.toRadians(enemy.getViewAngle() / 2f));
        if (dot >= viewCosThreshold) {
            enemy.setHeroInSight(!isLineBlocked(enemyCenter, heroCenter));
        } else {
            enemy.setHeroInSight(false);
        }
    }

    private boolean isLineBlocked(Vector2 from, Vector2 to) {
        int steps = (int) (from.dst(to) * 10);
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            float x = from.x + t * (to.x - from.x);
            float y = from.y + t * (to.y - from.y);

            int tileX = (int) Math.floor(x);
            int tileY = (int) Math.floor(y);

            if (tileX < 0 || tileY < 0 || tileX >= world.getLevel().getWidth() || tileY >= world.getLevel().getHeight()) {
                continue;
            }

            Tile tile = world.getLevel().get(tileX, tileY);
            if (tile != null && tile.isBlockingSight()) {
                if (Intersector.intersectSegmentRectangle(from, to, tile.getBounds())) {
                    Gdx.app.log("LineBlocked", "Blocked by tile at (" + tileX + ", " + tileY + "), bounds: " + tile.getBounds());
                    return true;
                }
            }
        }
        return false;
    }

    private void chaseHero(Enemy enemy) {
        if (enemy.getState() == EnemyState.ATTACKING) return;
        Hero hero = world.getHero();

        enemy.setState(EnemyState.WALKING);
        if(enemy.isFacingLeft() && hero.getPosition().x + hero.getBounds().width < getAttackRect(enemy).x) {
            enemy.getAcceleration().x = -enemy.getMaxAcceleration();
        } else if(!enemy.isFacingLeft() && hero.getPosition().x > getAttackRect(enemy).x + getAttackRect(enemy).width){
            enemy.getAcceleration().x = enemy.getMaxAcceleration();
        }

    }

    private void checkHeroToAttack(float delta, Enemy enemy) {
        Hero hero = world.getHero();
        enemy.updateAttack(delta);

        if (!(enemy.getState().equals(EnemyState.ATTACKING))) {
            Rectangle attackRect = getAttackRect(enemy);

            if(attackRect.overlaps(hero.getBounds())) {
                enemy.startAttack();
                Gdx.app.log("INFO", "START ATTACK " + enemy.getState() + " " + (enemy.getState().equals(EnemyState.ATTACKING)));
            }
            rectPool.free(attackRect);
        } else {

            if (enemy.canDealDamage()) {
                Gdx.app.log("INFO", "ATTACKING");
                Rectangle attackRect = getAttackRect(enemy);

                if (attackRect.overlaps(hero.getBounds())) {
                    hero.takeDamage(enemy.getDamage());
                    enemy.markDamageDealt();
                    rectPool.free(attackRect);
                }

            }
        }

    }

    private Rectangle getAttackRect(Enemy enemy) {
        Rectangle attackRect = rectPool.obtain();

        if (enemy.isFacingLeft()) {
            attackRect.set(
                enemy.getBounds().x - enemy.getBounds().width/2,
                enemy.getBounds().y - enemy.getBounds().height/2,
                enemy.getBounds().width,
                enemy.getBounds().height);
        } else {
            attackRect.set(
                enemy.getBounds().x + enemy.getBounds().width/2,
                enemy.getBounds().y + enemy.getBounds().height/2,
                enemy.getBounds().width,
                enemy.getBounds().height
            );
        }
        return  attackRect;
    }

    private void checkCollisionWithTiles(float delta) {
        for (Enemy enemy : enemies) {
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

    private void enemyDying(Enemy enemy) {
        if (enemy.getHealth() <= 0) {
            enemy.setHealth(0);
            enemy.setDamage(0);
            enemy.setPosition(new Vector2(99, 99));
            enemy.setAlive(false);
        }
    }
}
