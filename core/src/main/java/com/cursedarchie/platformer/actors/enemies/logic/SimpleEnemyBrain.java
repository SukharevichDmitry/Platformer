package com.cursedarchie.platformer.actors.enemies.logic;

import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.group.CombatRole;
import com.cursedarchie.platformer.actors.enemies.group.GroupPositioningConfig;
import com.cursedarchie.platformer.actors.enemies.group.SlotState;
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

        boolean scavenger = "scavenger".equals(enemy.getArchetypeId());
        if (enemy.isCanSeeHero()) {
            // Archers keep the hero in mind much longer (3-5 s) than melee enemies.
            searchRemaining = scavenger ? 3.5f : 1.4f;
            noticeRemaining = Math.max(0f, noticeRemaining - delta);
        } else {
            searchRemaining = Math.max(0f, searchRemaining - delta);
            noticeRemaining = searchRemaining == 0f ? .45f : 0f;
        }

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
        // Priority: stagger / recovery, then the attack already under way, then retreat and defence, then
        // positioning (attack slot, surrounding spot, firing spot, support spot), then search and patrol.
        if (enemy.isStaggered() || enemy.isDodging() || enemy.isActionRecovering()) {
            enemy.setIntent(IDLE);
            return;
        }
        // An attack that has begun is never abandoned for a new position: stand and finish it.
        if (enemy.isAttacking() || enemy.isRangedBusy() || enemy.isBlocking() || enemy.isCasting() ||
            enemy.isDashPhase()) {
            enemy.setIntent(IDLE);
            return;
        }
        if (enemy.isRetreating()) {
            enemy.setIntent(RETREAT);
            return;
        }
        // Hero jumped over or slipped past: swing to face the last known position at once.
        if (enemy.isCanSeeHero() || searchRemaining > 0f) enemy.turnToward(enemy.getHeroLastKnownPos().x);

        String id = enemy.getArchetypeId();
        CombatRole role = enemy.getCombatRole();
        GroupPositioningConfig cfg = GroupPositioningConfig.get();
        boolean slotted = enemy.isCanSeeHero() && enemy.isSlotManaged() && enemy.hasSlotTarget();
        float retreatDirection = enemy.getHeroLastKnownPos().x < enemy.getPosition().x ? 1f : -1f;

        if (role == CombatRole.SUPPORT) {
            // Support stays in the rear: back away when the hero closes in, otherwise hold its support spot.
            if (enemy.isCanSeeHero()) {
                if (distance < cfg.supportMinDistance - .4f && !enemy.isSlotCrossing() &&
                    enemy.canBeginRetreat(retreatDirection)) { enemy.setIntent(RETREAT); return; }
                if (slotted) { moveToSlot(enemy, cfg); return; }
                if (distance < 5.2f && !enemy.isCanAttackHero()) { enemy.setIntent(IDLE); return; }
            }
        } else if (role == CombatRole.RANGED) {
            // Archer whose shot is blocked by an ally repositions instead of shooting through it.
            if (enemy.isLineBlocked() && !enemy.isSlotCrossing() && enemy.canBeginRetreat(retreatDirection)) {
                enemy.setIntent(RETREAT);
                return;
            }
            // Too close to the hero: open the distance (never join the melee).
            if (enemy.isCanSeeHero() && distance < cfg.rangedMinDistance - .3f && !enemy.isSlotCrossing() &&
                enemy.canBeginRetreat(retreatDirection)) {
                enemy.setIntent(RETREAT);
                return;
            }
            // Point-blank strike only as self-defence.
            if (noticeRemaining == 0f && enemy.isCanAttackHero() && distance < 1.6f) {
                enemy.requestAttack();
                enemy.setIntent(IDLE);
                return;
            }
            if (slotted) {
                if (noticeRemaining > 0f) { enemy.setIntent(IDLE); return; }
                moveToSlot(enemy, cfg);
                return;
            }
            if (enemy.isCanSeeHero() && distance < 2.7f && enemy.canBeginRetreat(retreatDirection)) {
                enemy.setIntent(RETREAT);
                return;
            }
            if (enemy.isCanSeeHero() && distance < 5.5f) { enemy.setIntent(IDLE); return; }
        } else {
            // Breaker plugs its passage: it will not follow the hero far from where it stood.
            if ("heavy_hunter".equals(id) && enemy.isBeyondLeash(5f) && !enemy.isCanAttackHero()) {
                enemy.setIntent(IDLE);
                return;
            }
            if (slotted) {
                if (noticeRemaining > 0f) { enemy.setIntent(IDLE); return; }
                // Attack only from an attack slot, standing in it, with the hero in reach and in sight.
                if (enemy.hasAttackSlot() && enemy.getSlotState() == SlotState.ATTACK_POSITION &&
                    enemy.isCanAttackHero() && !enemy.isRangedWindingUp()) {
                    enemy.requestAttack();
                    enemy.setIntent(IDLE);
                    return;
                }
                moveToSlot(enemy, cfg);
                return;
            }
            if (noticeRemaining == 0f && enemy.isCanAttackHero() && enemy.hasAttackSlot() == enemy.isSlotManaged()) {
                // Not (yet) part of a formation: the plain rule applies.
                enemy.requestAttack();
                enemy.setIntent(IDLE);
                return;
            }
            if (noticeRemaining > 0f && enemy.isCanSeeHero()) { enemy.setIntent(IDLE); return; }
            if (enemy.isCanAttackHero() && !enemy.isSlotManaged()) { enemy.setIntent(IDLE); return; }
        }

        if (enemy.isCanSeeHero() || searchRemaining > 0f) {
            enemy.setIntent(CHASE);
        } else {
            enemy.setIntent(PATROL);
        }
    }

    /** Walk to the slot target; stand still once there (with hysteresis, so a small push does not restart it). */
    private static void moveToSlot(Enemy enemy, GroupPositioningConfig cfg) {
        float off = Math.abs(enemy.getSlotTargetX() - enemy.getPosition().x);
        float tolerance = enemy.getCurrentIntent() == CHASE ? cfg.arriveTolerance : cfg.restartTolerance;
        enemy.setIntent(off <= tolerance ? IDLE : CHASE);
    }
}
