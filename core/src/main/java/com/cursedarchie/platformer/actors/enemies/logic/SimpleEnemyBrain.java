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
        // Awareness: unaware -> alert (brief) -> tracking; losing sight heads for the last known spot, then searches.
        if (enemy.isCanSeeHero()) {
            if (enemy.getAwareness() == Enemy.Awareness.UNAWARE || enemy.getAwareness() == Enemy.Awareness.LOST ||
                enemy.getAwareness() == Enemy.Awareness.SEARCHING) {
                enemy.setAwareness(Enemy.Awareness.ALERT);
                enemy.turnToward(enemy.getHeroLastKnownPos().x);
            } else if (enemy.getAwareness() == Enemy.Awareness.ALERT && noticeRemaining == 0f) {
                enemy.setAwareness(Enemy.Awareness.TRACKING);
            }
        } else if (searchRemaining > 0f) {
            enemy.setAwareness(distance > 1f ? Enemy.Awareness.LOST : Enemy.Awareness.SEARCHING);
        } else {
            enemy.setAwareness(Enemy.Awareness.UNAWARE);
        }
        float retreatDirection = enemy.getHeroLastKnownPos().x < enemy.getPosition().x ? 1f : -1f;

        if (enemy.isStaggered() || enemy.isDodging() || enemy.isActionRecovering()) {
            enemy.setIntent(IDLE);
            return;
        }
        if (enemy.isRetreating()) {
            enemy.setIntent(RETREAT);
            return;
        }
        // Hero jumped over or slipped past: swing to face the last known position at once.
        if ((enemy.isCanSeeHero() || searchRemaining > 0f) && !enemy.isAttacking() && !enemy.isRangedBusy() &&
            !enemy.isBlocking() && !enemy.isCasting() && !enemy.isDashPhase())
            enemy.turnToward(enemy.getHeroLastKnownPos().x);
        String id = enemy.getArchetypeId();
        boolean overseer = "overseer".equals(id);
        // Support stays in the rear: back away when the hero closes in, hang back otherwise.
        if (overseer && enemy.isCanSeeHero() && !enemy.isAttacking()) {
            if (distance < 3.2f && enemy.canBeginRetreat(retreatDirection)) { enemy.setIntent(RETREAT); return; }
            if (distance < 5.2f && !enemy.isCanAttackHero()) { enemy.setIntent(IDLE); return; }
        }
        // Archer whose shot is blocked by an ally repositions instead of shooting through it.
        if (scavenger && enemy.isLineBlocked() && enemy.canBeginRetreat(retreatDirection)) {
            enemy.setIntent(RETREAT);
            return;
        }
        // Breaker plugs its passage: it will not follow the hero far from where it stood.
        if ("heavy_hunter".equals(id) && enemy.isBeyondLeash(5f) && !enemy.isAttacking() && !enemy.isCanAttackHero()) {
            enemy.setIntent(IDLE);
            return;
        }
        // No free attack slot: keep a little distance and wait instead of crowding the hero.
        if (!scavenger && !enemy.hasAttackPermit() && enemy.isCanSeeHero() && !enemy.isAttacking() && distance < 2.6f) {
            enemy.setIntent(IDLE);
            return;
        }
        if (noticeRemaining == 0f && enemy.isCanAttackHero() && !enemy.isRangedWindingUp() &&
            !enemy.isBlocking() && (!scavenger || distance < 1.6f)) {
            enemy.requestAttack();
            enemy.setIntent(IDLE);
            return;
        }

        if (enemy.isAttacking() || enemy.isRangedBusy() || enemy.isBlocking() ||
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
