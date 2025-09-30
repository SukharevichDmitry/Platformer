package com.cursedarchie.platformer.actors.enemies.logic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.actors.Hero;
import com.cursedarchie.platformer.actors.Enemy;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.world.World;

public class EnemyPerception {
    World world;

    public EnemyPerception(World world) {
        this.world = world;
    }

    public void canSeeHero(Enemy enemy) {
        Hero hero = world.getHero();

        Rectangle enemyRect = enemy.getBounds();
        Rectangle heroRect = hero.getBounds();

        Vector2 enemyCenter = new Vector2(enemyRect.x + enemyRect.width / 2f, enemyRect.y + enemyRect.height / 2f);
        Vector2 heroCenter = new Vector2(heroRect.x + heroRect.width / 2f, heroRect.y + heroRect.height / 2f);

        if (heroCenter.dst(enemyCenter) <= enemy.getViewDistance()) {
            Vector2 toHero = new Vector2(heroCenter).sub(enemyCenter).nor();

            Vector2 enemyDirection = enemy.isFacingLeft() ? new Vector2(-1, 0) : new Vector2(1, 0);

            float dot = toHero.dot(enemyDirection);
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

    private void updateHeroLastKnownPos(Enemy enemy) {
        enemy.setHeroLastKnownPos(world.getHero().getPosition());
    }

    public void checkHeroToAttack(float delta, Enemy enemy) {
        Hero hero = world.getHero();
        enemy.setCanAttackHero(enemy.getAttackRect().overlaps(hero.getBounds()));
    }

    public void dealDamage(Enemy enemy) {
        Hero hero = world.getHero();

        Gdx.app.log("INFO", "ATTACKING");
        Rectangle attackRect = enemy.getAttackRect();

        if (attackRect.overlaps(hero.getBounds())) {
            hero.takeDamage(enemy.getDamage());
            enemy.markDamageDealt();
            enemy.getRectPool().free(attackRect);
        }
    }
}
