package com.cursedarchie.platformer.actors.hero;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.Enemy;
import com.cursedarchie.platformer.actors.Hero;
import com.cursedarchie.platformer.world.World;

public class HeroPerception {
    World world;

    public HeroPerception(World world) {
        this.world = world;
    }

    public void checkAttackingEnemies (Hero hero) {
        if (hero.isAttackHandled()) return;

        Rectangle attackRect = hero.getAttackRect();
        Array<Enemy> enemies = world.getLevel().getAliveEnemies();

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
