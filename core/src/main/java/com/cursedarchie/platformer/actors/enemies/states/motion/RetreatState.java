package com.cursedarchie.platformer.actors.enemies.states.motion;

import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;

/** Archer retreat: the phases (telegraph, dash, skid back into stance) live in {@link Enemy#tickRetreat}. */
public class RetreatState implements EnemyState {
    @Override
    public void enter(Enemy enemy) {
        enemy.beginRetreat();
    }

    @Override
    public void update (Enemy enemy, float delta) {
        float direction = enemy.getHeroLastKnownPos().x < enemy.getPosition().x ? 1f : -1f;
        enemy.tickRetreat(direction, delta);
    }

    @Override
    public void exit (Enemy enemy) {
        enemy.endRetreat();
        // A finishing dash keeps its momentum and skids to a stop; everything else stops at once.
        if (!enemy.isRetreatRecovering()) enemy.moveStop();
    }
}
