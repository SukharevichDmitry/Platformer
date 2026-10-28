package com.cursedarchie.platformer.actors.enemies.physics;

import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.physics.PhysicsCategories;
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

    public void lookForHero(Enemy enemy) {
        boolean canSeeHero = false;

        Vector2 enemyCenter = enemy.getBody().getPosition();
        for (Actor actor : enemy.getVisibleActors()) {
            Vector2 heroCenter = actor.getBody().getPosition();

            tmp.set(heroCenter).sub(enemyCenter).nor();
            dir.set(enemy.isFacingLeft() ? -1f : 1f, 0f);

            float dot = tmp.dot(dir);

            float viewCosThreshold =
                (float)Math.cos(
                    Math.toRadians(enemy.getViewAngle() / 2f)
                );

            if (dot < viewCosThreshold) {
                continue;
            }

            boolean blocked =
                isLineBlocked(enemyCenter, heroCenter);

            if (!blocked && actor instanceof Hero) {
                canSeeHero = true;
                updateHeroLastKnownPos(enemy, actor);
                break;
            }
        }

        enemy.setCanSeeHero(canSeeHero);
    }

    private boolean isLineBlocked(Vector2 from, Vector2 to) {
        lineBlocked = false;

        gameWorld.getWorld().rayCast((fixture, point, normal, fraction) -> {

            if (fixture.isSensor()) {
                return -1;
            }

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

    private void updateHeroLastKnownPos(Enemy enemy, Actor hero) {
        enemy.setHeroLastKnownPos(hero.getPosition());
    }

    public void prepareToAttack(Enemy enemy) {
        boolean canAttack = false;
        if (enemy.isCanSeeHero()) {
            float enemyX = enemy.getPosition().x;
            for (Actor target : enemy.getAttackTargets()) {
                if (!(target instanceof Hero) || !target.isAlive()) continue;
                float dx = target.getPosition().x - enemyX;
                if ((enemy.isFacingLeft() && dx < 0f) || (!enemy.isFacingLeft() && dx > 0f)) {
                    canAttack = true;
                    break;
                }
            }
        }
        enemy.setCanAttackHero(canAttack);
    }
}
