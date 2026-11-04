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
    private static final Definition HEAVY_HUNTER = new Definition("heavy_hunter", 1.7f, 3);
    private static final Definition INFILTRATOR = new Definition("infiltrator", 0.85f, 1);
    private static final Definition OVERSEER = new Definition("overseer", 1f, 2);

    public static Definition definition(String type) {
        if (DEFAULT.id.equals(type)) return DEFAULT;
        if (BOSS.id.equals(type)) return BOSS;
        if (SCAVENGER.id.equals(type)) return SCAVENGER;
        if (RED_COLLECTOR.id.equals(type)) return RED_COLLECTOR;
        if (HEAVY_HUNTER.id.equals(type)) return HEAVY_HUNTER;
        if (INFILTRATOR.id.equals(type)) return INFILTRATOR;
        if (OVERSEER.id.equals(type)) return OVERSEER;
        throw new IllegalArgumentException("Unsupported enemy type: " + type);
    }

    public static boolean supportsType(String type) {
        return DEFAULT.id.equals(type) || BOSS.id.equals(type) ||
        SCAVENGER.id.equals(type) || RED_COLLECTOR.id.equals(type) || HEAVY_HUNTER.id.equals(type) ||
        INFILTRATOR.id.equals(type) || OVERSEER.id.equals(type);
    }

    public static Enemy createDefaultEnemy(World world, Vector2 pos) {

        EnemyConfig cfg = new EnemyConfig();
        cfg.size = DEFAULT.bodySize;
        cfg.maxHealth = 3f;

        cfg.viewDistance = 5.5f;
        cfg.viewAngle = 110f;

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
        cfg.viewDistance = 6f;
        cfg.viewAngle = 125f;
        cfg.damage = 1f;
        cfg.attackDuration = .55f;
        cfg.attackStartup = .20f;
        cfg.attackActiveEnd = .31f;
        cfg.attackCooldown = .8f;
        cfg.velocity = 6.2f;
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
        cfg.viewDistance = 5.5f;
        cfg.viewAngle = 105f;
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

    public static Enemy createHeavyHunter(World world, Vector2 pos) {
        EnemyConfig cfg = new EnemyConfig();
        cfg.size = HEAVY_HUNTER.bodySize;
        cfg.maxHealth = 9f;
        cfg.viewDistance = 5f;
        cfg.viewAngle = 115f;
        cfg.damage = 1.4f;
        cfg.attackDuration = 1.08f;
        cfg.attackStartup = .65f;
        cfg.attackActiveEnd = .84f;
        cfg.attackCooldown = 1.6f;
        cfg.velocity = 3.0f;
        cfg.attackLeft = new AttackConfig(1.2f, 1f, -1.3f, 0f);
        cfg.attackRight = new AttackConfig(1.2f, 1f, 1.3f, 0f);
        Enemy enemy = new DefaultEnemy(world, pos, cfg);
        enemy.setArchetypeId(HEAVY_HUNTER.id);
        return enemy;
    }

    /** Fast flanker: fragile and quick; sidesteps a telegraphed light strike, then lunges from the side. */
    public static Enemy createInfiltrator(World world, Vector2 pos) {
        EnemyConfig cfg = new EnemyConfig();
        cfg.size = INFILTRATOR.bodySize;
        cfg.maxHealth = 2f;
        cfg.viewDistance = 6.5f;
        cfg.viewAngle = 130f;
        cfg.damage = 1f;
        cfg.attackDuration = .5f;
        cfg.attackStartup = .18f;
        cfg.attackActiveEnd = .28f;
        cfg.attackCooldown = .7f;
        cfg.velocity = 8f;
        cfg.attackLeft = new AttackConfig(.65f, .7f, -.8f, 0f);
        cfg.attackRight = new AttackConfig(.65f, .7f, .8f, 0f);
        Enemy enemy = new DefaultEnemy(world, pos, cfg);
        enemy.setArchetypeId(INFILTRATOR.id);
        return enemy;
    }

    /** Support caster: weak melee, periodically buffs a nearby ally (faster attacks, tougher stagger). */
    public static Enemy createOverseer(World world, Vector2 pos) {
        EnemyConfig cfg = new EnemyConfig();
        cfg.size = OVERSEER.bodySize;
        cfg.maxHealth = 2f;
        cfg.viewDistance = 6f;
        cfg.viewAngle = 120f;
        cfg.damage = .5f;
        cfg.attackDuration = .8f;
        cfg.attackStartup = .4f;
        cfg.attackActiveEnd = .52f;
        cfg.attackCooldown = 1.4f;
        cfg.velocity = 4.5f;
        cfg.attackLeft = new AttackConfig(.7f, .7f, -.9f, 0f);
        cfg.attackRight = new AttackConfig(.7f, .7f, .9f, 0f);
        Enemy enemy = new DefaultEnemy(world, pos, cfg);
        enemy.setArchetypeId(OVERSEER.id);
        return enemy;
    }

    public static Enemy createBoss(World world, Vector2 pos) {

        EnemyConfig cfg = new EnemyConfig();
        cfg.size = BOSS.bodySize;
        cfg.maxHealth = 8f;

        cfg.viewDistance = 5f;
        cfg.viewAngle = 115f;

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
