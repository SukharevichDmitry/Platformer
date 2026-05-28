package com.cursedarchie.platformer.actors.enemies;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.cursedarchie.platformer.actors.enemies.types.DefaultEnemy;

public class EnemyFactory {

    public static Enemy createDefaultEnemy(World world, Vector2 pos) {

        EnemyConfig cfg = new EnemyConfig();
        cfg.size = 0.5f;
        cfg.maxHealth = 3f;

        cfg.viewDistance = 5f;
        cfg.viewAngle = 45f;

        cfg.damage = 1f;
        cfg.attackDuration = 3f;

        cfg.velocity = 2f;

        return new DefaultEnemy(world, pos, cfg);
    }

    public static Enemy createBoss(World world, Vector2 pos) {

        EnemyConfig cfg = new EnemyConfig();
        cfg.size = 4f;
        cfg.maxHealth = 8f;

        cfg.viewDistance = 4f;
        cfg.viewAngle = 90f;

        cfg.damage = 2f;
        cfg.attackDuration = 4f;

        cfg.velocity = 1f;

        return new DefaultEnemy(world, pos, cfg);
    }
}
