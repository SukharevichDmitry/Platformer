package com.cursedarchie.platformer.controllers;

import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.enemies.EnemyFactory;
import com.cursedarchie.platformer.actors.enemies.logic.SimpleEnemyBrain;
import com.cursedarchie.platformer.actors.enemies.types.Boss;
import com.cursedarchie.platformer.actors.enemies.types.DefaultEnemy;
import com.cursedarchie.platformer.actors.enemies.physics.EnemyPerception;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.utils.EnemySpawnData;
import com.cursedarchie.platformer.world.GameWorld;

public class EnemyController {

    private final GameWorld gameWorld;

    public EnemyController(GameWorld gameWorld) {
        this.gameWorld = gameWorld;
    }

    public void update(float delta) {

        Array<Enemy> enemies = gameWorld.getLevel().getAliveEnemies();
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) continue;

            enemy.update(delta);
        }
    }

    public void spawnEnemy(EnemySpawnData spawn, World world) {
        Enemy enemy = null;
        switch (spawn.type) {
            case "default":
                enemy = EnemyFactory.createDefaultEnemy(world, spawn.position);
                enemy.setBrain(new SimpleEnemyBrain(gameWorld));
                break;
            case "boss":
                enemy = EnemyFactory.createBoss(world, spawn.position);
                break;
        }

        if (enemy != null) {
            gameWorld.getLevel().addEnemy(enemy);
        }
    }
}
