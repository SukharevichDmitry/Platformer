package com.cursedarchie.platformer.actors.hero;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.world.GameWorld;

public class HeroPerception {
    GameWorld gameWorld;

    public HeroPerception(GameWorld gameWorld) {
        this.gameWorld = gameWorld;
    }

    public void checkAttackingEnemies (Hero hero) {
        if (hero.isAttackHandled()) return;

        Rectangle attackRect = hero.getAttackRect();
        Array<Enemy> enemies = gameWorld.getLevel().getAliveEnemies();

        for (Enemy enemy : enemies) {
            if (attackRect.overlaps(enemy.getBounds())) {
                enemy.takeDamage(hero.getDamage());
                hero.setAttackHandled(true);
            }
        }
        if (hero.isAttackHandled()) {
            hero.getRectPool().free(attackRect);
        }
    }

}
