package com.cursedarchie.platformer.actors.enemies;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.cursedarchie.platformer.actors.enemies.types.Boss;
import com.cursedarchie.platformer.actors.enemies.types.DefaultEnemy;
import com.cursedarchie.platformer.physics.AttackConfig;

public class EnemyFactory {
    public static final class Definition {
        public final String id;
        public final float bodySize;
        public final int difficultyCost;

        private Definition(String id, float bodySize, int difficultyCost) {
            this.id = id;
            this.bodySize = bodySize;
            this.difficultyCost = difficultyCost;
        }
    }

    private static final Definition DEFAULT = new Definition("default", 0.5f, 1);
    private static final Definition BOSS = new Definition("boss", 1.5f, 3);

    public static Definition definition(String type) {
        if (DEFAULT.id.equals(type)) return DEFAULT;
        if (BOSS.id.equals(type)) return BOSS;
        throw new IllegalArgumentException("Unsupported enemy type: " + type);
    }

    public static boolean supportsType(String type) {
        return DEFAULT.id.equals(type) || BOSS.id.equals(type);
    }

    public static Enemy createDefaultEnemy(World world, Vector2 pos) {

        EnemyConfig cfg = new EnemyConfig();
        cfg.size = DEFAULT.bodySize;
        cfg.maxHealth = 3f;

        cfg.viewDistance = 5f;
        cfg.viewAngle = 45f;

        cfg.damage = 1f;
        cfg.attackDuration = 0.65f;
        cfg.attackStartup = 0.25f;
        cfg.attackActiveEnd = 0.37f;
        cfg.attackCooldown = 0.55f;

        cfg.velocity = 3.2f;

        cfg.attackLeft = new AttackConfig(0.4f, 0.4f, -0.5f, 0f);
        cfg.attackRight = new AttackConfig(0.4f, 0.4f, 0.5f, 0f);

        return new DefaultEnemy(world, pos, cfg);
    }

    public static Enemy createBoss(World world, Vector2 pos) {

        EnemyConfig cfg = new EnemyConfig();
        cfg.size = BOSS.bodySize;
        cfg.maxHealth = 8f;

        cfg.viewDistance = 4f;
        cfg.viewAngle = 90f;

        cfg.damage = 2f;
        cfg.attackDuration = 1.1f;
        cfg.attackStartup = 0.5f;
        cfg.attackActiveEnd = 0.68f;
        cfg.attackCooldown = 1f;

        cfg.velocity = 1.8f;

        cfg.attackLeft = new AttackConfig(0.8f, 1f, -1.05f, 0f);
        cfg.attackRight = new AttackConfig(0.8f, 1f, 1.05f, 0f);

        return new Boss(world, pos, cfg);
    }
}
