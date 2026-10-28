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

    private static final Definition DEFAULT = new Definition("default", 1f, 1);
    private static final Definition BOSS = new Definition("boss", 3f, 3);
    private static final Definition SCAVENGER = new Definition("scavenger", 0.9f, 1);
    private static final Definition RED_COLLECTOR = new Definition("red_collector", 1.4f, 2);

    public static Definition definition(String type) {
        if (DEFAULT.id.equals(type)) return DEFAULT;
        if (BOSS.id.equals(type)) return BOSS;
        if (SCAVENGER.id.equals(type)) return SCAVENGER;
        if (RED_COLLECTOR.id.equals(type)) return RED_COLLECTOR;
        throw new IllegalArgumentException("Unsupported enemy type: " + type);
    }

    public static boolean supportsType(String type) {
        return DEFAULT.id.equals(type) || BOSS.id.equals(type) ||
            SCAVENGER.id.equals(type) || RED_COLLECTOR.id.equals(type);
    }

    public static Enemy createDefaultEnemy(World world, Vector2 pos) {

        EnemyConfig cfg = new EnemyConfig();
        cfg.size = DEFAULT.bodySize;
        cfg.maxHealth = 3f;

        cfg.viewDistance = 10f;
        cfg.viewAngle = 45f;

        cfg.damage = 1f;
        cfg.attackDuration = 0.65f;
        cfg.attackStartup = 0.25f;
        cfg.attackActiveEnd = 0.37f;
        cfg.attackCooldown = 0.55f;

        cfg.velocity = 6.4f;

        cfg.attackLeft = new AttackConfig(0.8f, 0.8f, -1f, 0f);
        cfg.attackRight = new AttackConfig(0.8f, 0.8f, 1f, 0f);

        return new DefaultEnemy(world, pos, cfg);
    }

    /** Fast ruins scavenger: fragile, closes distance quickly, short tell. */
    public static Enemy createScavenger(World world, Vector2 pos) {
        EnemyConfig cfg = new EnemyConfig();
        cfg.size = SCAVENGER.bodySize;
        cfg.maxHealth = 2f;
        cfg.viewDistance = 12f;
        cfg.viewAngle = 70f;
        cfg.damage = 1f;
        cfg.attackDuration = .55f;
        cfg.attackStartup = .20f;
        cfg.attackActiveEnd = .31f;
        cfg.attackCooldown = .8f;
        cfg.velocity = 8.6f;
        cfg.attackLeft = new AttackConfig(.7f, .7f, -.86f, 0f);
        cfg.attackRight = new AttackConfig(.7f, .7f, .86f, 0f);
        Enemy enemy = new DefaultEnemy(world, pos, cfg);
        enemy.setArchetypeId(SCAVENGER.id);
        return enemy;
    }

    /** Red patrol collector: slower, sturdier, with a longer visible windup. */
    public static Enemy createRedCollector(World world, Vector2 pos) {
        EnemyConfig cfg = new EnemyConfig();
        cfg.size = RED_COLLECTOR.bodySize;
        cfg.maxHealth = 5f;
        cfg.viewDistance = 10f;
        cfg.viewAngle = 55f;
        cfg.damage = 1.5f;
        cfg.attackDuration = 1f;
        cfg.attackStartup = .46f;
        cfg.attackActiveEnd = .63f;
        cfg.attackCooldown = 1.1f;
        cfg.velocity = 4.6f;
        cfg.attackLeft = new AttackConfig(1.1f, 1f, -1.3f, 0f);
        cfg.attackRight = new AttackConfig(1.1f, 1f, 1.3f, 0f);
        Enemy enemy = new DefaultEnemy(world, pos, cfg);
        enemy.setArchetypeId(RED_COLLECTOR.id);
        return enemy;
    }

    public static Enemy createBoss(World world, Vector2 pos) {

        EnemyConfig cfg = new EnemyConfig();
        cfg.size = BOSS.bodySize;
        cfg.maxHealth = 8f;

        cfg.viewDistance = 8f;
        cfg.viewAngle = 90f;

        cfg.damage = 2f;
        cfg.attackDuration = 1.1f;
        cfg.attackStartup = 0.5f;
        cfg.attackActiveEnd = 0.68f;
        cfg.attackCooldown = 1f;

        cfg.velocity = 3.6f;

        cfg.attackLeft = new AttackConfig(1.6f, 2f, -2.1f, 0f);
        cfg.attackRight = new AttackConfig(1.6f, 2f, 2.1f, 0f);

        Enemy enemy = new Boss(world, pos, cfg);
        enemy.setArchetypeId(BOSS.id);
        return enemy;
    }
}
