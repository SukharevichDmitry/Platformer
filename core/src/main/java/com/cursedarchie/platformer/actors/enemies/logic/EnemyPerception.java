package com.cursedarchie.platformer.actors.enemies.logic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.physics.PhysicsCategories;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.world.GameWorld;

public class EnemyPerception {
    GameWorld gameWorld;
    private final Vector2 hitPoint = new Vector2();
    private boolean lineBlocked;
    private final Vector2 tmp = new Vector2();
    private final Vector2 dir = new Vector2();

    public EnemyPerception(GameWorld gameWorld) {
        this.gameWorld = gameWorld;
    }

    public void canSeeHero(Enemy enemy) {
        Hero hero = gameWorld.getHero();

        Vector2 enemyCenter = enemy.getBody().getPosition();
        Vector2 heroCenter = hero.getBody().getPosition();

        if (heroCenter.dst(enemyCenter) <= enemy.getViewDistance()) {
            tmp.set(heroCenter).sub(enemyCenter).nor();
            dir.set(enemy.isFacingLeft() ? -1f : 1f, 0f);

            float dot = tmp.dot(dir);
            float viewCosThreshold = (float) Math.cos(Math.toRadians(enemy.getViewAngle() / 2f));
            if (dot >= viewCosThreshold) {
                enemy.setCanSeeHero(!isLineBlocked(enemyCenter, heroCenter));
                updateHeroLastKnownPos(enemy);
            } else {
                enemy.setCanSeeHero(false);
            }
        }
    }

    private boolean isLineBlocked(Vector2 from, Vector2 to) {
        lineBlocked = false;

        gameWorld.getWorld().rayCast((fixture, point, normal, fraction) -> {

            Object userData = fixture.getBody().getUserData();

            if (userData instanceof Hero) {
                return -1;
            }

            if (fixture.getFilterData().categoryBits == PhysicsCategories.WALL) {
                lineBlocked = true;
                hitPoint.set(point);
                return 0;
            }

            return -1;
        }, from, to);

        return lineBlocked;
    }

    private void updateHeroLastKnownPos(Enemy enemy) {
        enemy.setHeroLastKnownPos(gameWorld.getHero().getPosition());
    }

    public void checkHeroToAttack(Enemy enemy) {
        Hero hero = gameWorld.getHero();
        enemy.setCanAttackHero(enemy.getAttackRect().overlaps(hero.getBounds()));
    }

    public void dealDamage(Enemy enemy) {
        Hero hero = gameWorld.getHero();

        Gdx.app.log("INFO", "DEAL DAMAGE");
        Rectangle attackRect = enemy.getAttackRect();

        if (attackRect.overlaps(hero.getBounds())) {
            hero.takeDamage(enemy.getDamage());
            enemy.markDamageDealt();
            enemy.getRectPool().free(attackRect);
        }
    }
}
