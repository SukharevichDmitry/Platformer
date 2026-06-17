package com.cursedarchie.platformer.actors.enemies.logic;

import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.physics.EnemyPerception;
import com.cursedarchie.platformer.world.GameWorld;

import static com.cursedarchie.platformer.actors.enemies.Enemy.Intent.*;

public class SimpleEnemyBrain implements EnemyBrain {

    private final EnemyPerception perception;

    public SimpleEnemyBrain(GameWorld world) {
        this.perception = new EnemyPerception(world);
    }

    @Override
    public void think(Enemy enemy, GameWorld world, float delta) {

        perception.lookForHero(enemy);
        perception.prepareToAttack(enemy);


        if (enemy.isCanAttackHero()) {
            enemy.requestAttack();
        }

        if (enemy.isCanSeeHero()) {
            enemy.setIntent(CHASE);
        } else {
            enemy.setIntent(PATROL);
        }
//
//        if (!enemy.isCanSeeHero() && enemy.getCurrentIntent() == CHASE) {
//            enemy.setIntent(IDLE);
//        }
    }
}
