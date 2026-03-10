package com.cursedarchie.platformer.controllers;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.enemies.Boss;
import com.cursedarchie.platformer.actors.enemies.DefaultEnemy;
import com.cursedarchie.platformer.actors.enemies.logic.EnemyPerception;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.utils.EnemySpawnData;
import com.cursedarchie.platformer.world.GameWorld;

public class EnemyController {

    private static final float MOVE_SPEED = 3f;
    private static final float LINEAR_DAMPING = 0.85f;

    private final GameWorld gameWorld;
    private final Array<Enemy> enemies;
    private final EnemyPerception perception;

    public EnemyController(GameWorld gameWorld) {
        this.gameWorld = gameWorld;
        this.enemies = gameWorld.getLevel().getAliveEnemies();
        this.perception = new EnemyPerception(gameWorld);
    }

    public void update(float delta) {
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) continue;

            updateMovement(enemy);
            perceptionLogic(enemy);
            enemy.getStateMachine().update(delta);
        }
    }

    private void updateMovement(Enemy enemy) {
        Body body = enemy.getBody();

        float targetVx = 0;

        if (enemy.isFacingLeft()) targetVx = -MOVE_SPEED;
        else targetVx = MOVE_SPEED;

        float currentVy = body.getLinearVelocity().y;
        body.setLinearVelocity(targetVx, currentVy);
        body.setLinearDamping(LINEAR_DAMPING);
    }

    private void perceptionLogic(Enemy enemy) {
        perception.canSeeHero(enemy);
        perception.checkHeroToAttack(enemy);

        if (enemy.isCanDealDamage()) {
            perception.dealDamage(enemy);
        }
    }

    public void spawnEnemy(EnemySpawnData spawn, World world) {
        Enemy enemy = null;

        switch (spawn.type) {
            case "default":
                enemy = new DefaultEnemy(world, spawn.position);
                break;
            case "boss":
                enemy = new Boss(world, spawn.position);
                break;
        }

        if (enemy != null) {
            gameWorld.getLevel().addEnemy(enemy);
        }
    }
}
