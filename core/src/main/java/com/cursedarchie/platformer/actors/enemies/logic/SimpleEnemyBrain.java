package com.cursedarchie.platformer.actors.enemies.logic;

import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.physics.EnemyPerception;
import com.cursedarchie.platformer.world.GameWorld;

import static com.cursedarchie.platformer.actors.enemies.Enemy.Intent.*;

public class SimpleEnemyBrain implements EnemyBrain {

    private final EnemyPerception perception;
    private float searchRemaining;
    private float noticeRemaining = .45f;

    public SimpleEnemyBrain(GameWorld world) {
        this.perception = new EnemyPerception(world);
    }

    @Override
    public void think(Enemy enemy, GameWorld world, float delta) {

        perception.lookForHero(enemy);
        perception.prepareToAttack(enemy);

        if (enemy.isCanSeeHero()) {
            searchRemaining = 1.4f;
            noticeRemaining = Math.max(0f, noticeRemaining - delta);
        } else {
            searchRemaining = Math.max(0f, searchRemaining - delta);
            noticeRemaining = searchRemaining == 0f ? .45f : 0f;
        }

        boolean scavenger = "scavenger".equals(enemy.getArchetypeId());
        float distance = Math.abs(enemy.getHeroLastKnownPos().x - enemy.getPosition().x);
        float retreatDirection = enemy.getHeroLastKnownPos().x < enemy.getPosition().x ? 1f : -1f;

        if (enemy.isStaggered() || enemy.isDodging() || enemy.isActionRecovering()) {
            enemy.setIntent(IDLE);
            return;
        }
        if (enemy.isRetreating()) {
            enemy.setIntent(RETREAT);
            return;
        }
        if (noticeRemaining == 0f && enemy.isCanAttackHero() && !enemy.isRangedWindingUp() &&
            !enemy.isBlocking() && (!scavenger || distance < 1.6f)) {
            enemy.requestAttack();
            enemy.setIntent(IDLE);
            return;
        }

        if (enemy.isAttacking() || enemy.isRangedWindingUp() || enemy.isBlocking() ||
            (noticeRemaining > 0f && enemy.isCanSeeHero()) ||
            (!scavenger && enemy.isCanAttackHero())) {
            enemy.setIntent(IDLE);
        } else if (scavenger && enemy.isCanSeeHero() && distance < 2.7f &&
            enemy.canBeginRetreat(retreatDirection)) {
            enemy.setIntent(RETREAT);
        } else if (scavenger && enemy.isCanSeeHero() && distance < 5.5f) {
            enemy.setIntent(IDLE);
        } else if (enemy.isCanSeeHero() || searchRemaining > 0f) {
            enemy.setIntent(CHASE);
        } else {
            enemy.setIntent(PATROL);
        }
    }
}
