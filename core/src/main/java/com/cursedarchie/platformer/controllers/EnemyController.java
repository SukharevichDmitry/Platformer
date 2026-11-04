package com.cursedarchie.platformer.controllers;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.enemies.EnemyFactory;
import com.cursedarchie.platformer.actors.enemies.logic.SimpleEnemyBrain;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.levels.wrappers.EnemySpawnData;
import com.cursedarchie.platformer.world.GameWorld;

public class EnemyController {

    private final GameWorld gameWorld;

    public EnemyController(GameWorld gameWorld) {
        this.gameWorld = gameWorld;
    }

    public void update(float delta) {

        Array<Enemy> enemies = gameWorld.getAliveEnemies();
        assignAttackSlots(enemies);
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) continue;

            enemy.update(delta);
            enemy.updateSpecialActions(gameWorld, gameWorld.getHero(), enemies, delta);
        }
        separate(enemies);
    }

    /** No more than this many melee enemies may be winding up or striking at the same time. */
    public static final int MAX_SIMULTANEOUS_ATTACKERS = 2;
    private static final float ENGAGE_RANGE = 9f;

    private void assignAttackSlots(Array<Enemy> enemies) {
        Vector2 hero = gameWorld.getHero().getPosition();
        int used = 0;
        for (Enemy e : enemies) {
            if (!isSlotUser(e) || !e.isAlive()) continue;
            if (e.isAttacking() || e.isDashPhase()) used++;   // an attack in progress always keeps its slot
        }
        // Everyone else competes for the remaining slots, nearest to the hero first.
        Array<Enemy> waiting = new Array<>();
        for (Enemy e : enemies) {
            if (!e.isAlive()) continue;
            if (!isSlotUser(e)) { e.setAttackPermit(true); continue; }
            if (e.isAttacking() || e.isDashPhase()) { e.setAttackPermit(true); continue; }
            e.setAttackPermit(false);
            if (e.getPosition().dst2(hero) < ENGAGE_RANGE * ENGAGE_RANGE) waiting.add(e);
        }
        waiting.sort((a, b) -> Float.compare(a.getPosition().dst2(hero), b.getPosition().dst2(hero)));
        for (int i = 0; i < waiting.size && used < MAX_SIMULTANEOUS_ATTACKERS; i++, used++)
            waiting.get(i).setAttackPermit(true);
        // A lone enemy far from the fight is never held back.
        for (Enemy e : enemies)
            if (e.isAlive() && isSlotUser(e) && e.getPosition().dst2(hero) >= ENGAGE_RANGE * ENGAGE_RANGE)
                e.setAttackPermit(true);
    }

    /** Melee roles share the attack slots; archers and the support caster act on their own rules. */
    private static boolean isSlotUser(Enemy e) {
        String id = e.getArchetypeId();
        return !"scavenger".equals(id) && !"overseer".equals(id);
    }

    /** Gentle push so bodies of ground enemies do not stack on one spot. */
    private void separate(Array<Enemy> enemies) {
        Vector2 hero = gameWorld.getHero().getPosition();
        for (int i = 0; i < enemies.size; i++) {
            Enemy a = enemies.get(i);
            if (!a.isAlive() || a.isAttacking() || a.isStaggered() || a.isDodging() || a.isDashing()) continue;
            for (int j = i + 1; j < enemies.size; j++) {
                Enemy b = enemies.get(j);
                if (!b.isAlive() || b.isAttacking() || b.isStaggered() || b.isDodging() || b.isDashing()) continue;
                float dx = b.getPosition().x - a.getPosition().x;
                float minGap = (a.getSize() + b.getSize()) * .45f;
                if (Math.abs(dx) >= minGap || Math.abs(b.getPosition().y - a.getPosition().y) > .9f) continue;
                // The one farther from the hero yields; ties go to the second.
                Enemy yielding = a.getPosition().dst2(hero) > b.getPosition().dst2(hero) ? a : b;
                Enemy other = yielding == a ? b : a;
                float away = yielding.getPosition().x >= other.getPosition().x ? 1f : -1f;
                if (yielding.getPosition().x == other.getPosition().x) away = yielding == a ? -1f : 1f;
                yielding.getBody().setLinearVelocity(away * 2.2f, yielding.getBody().getLinearVelocity().y);
            }
        }
    }

    public Enemy spawnEnemy(EnemySpawnData spawn, Vector2 worldPos, World world) {
        Enemy enemy = null;
        switch (spawn.type) {
            case "default":
                enemy = EnemyFactory.createDefaultEnemy(world, worldPos);
                enemy.setBrain(new SimpleEnemyBrain(gameWorld));
                break;
            case "boss":
                enemy = EnemyFactory.createBoss(world, worldPos);
                enemy.setBrain(new SimpleEnemyBrain(gameWorld));
                break;
            case "scavenger":
                enemy = EnemyFactory.createScavenger(world, worldPos);
                enemy.setBrain(new SimpleEnemyBrain(gameWorld));
                break;
            case "red_collector":
                enemy = EnemyFactory.createRedCollector(world, worldPos);
                enemy.setBrain(new SimpleEnemyBrain(gameWorld));
                break;
            case "infiltrator":
                enemy = EnemyFactory.createInfiltrator(world, worldPos);
                enemy.setBrain(new SimpleEnemyBrain(gameWorld));
                break;
            case "overseer":
                enemy = EnemyFactory.createOverseer(world, worldPos);
                enemy.setBrain(new SimpleEnemyBrain(gameWorld));
                break;
            case "heavy_hunter":
                enemy = EnemyFactory.createHeavyHunter(world, worldPos);
                enemy.setBrain(new SimpleEnemyBrain(gameWorld));
                break;
        }

        if (enemy != null) {
            gameWorld.addEnemy(enemy);
        }
        return enemy;
    }
}
