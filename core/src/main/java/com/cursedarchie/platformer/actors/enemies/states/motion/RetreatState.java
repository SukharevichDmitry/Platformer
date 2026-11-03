package com.cursedarchie.platformer.actors.enemies.states.motion;

import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;

public class RetreatState implements EnemyState {
    private float direction;

    @Override
    public void enter(Enemy enemy) {
        direction = enemy.getHeroLastKnownPos().x < enemy.getPosition().x ? 1f : -1f;
        if (!enemy.beginRetreat(direction)) enemy.moveStop();
    }

    @Override
    public void update (Enemy enemy, float delta) {
        if (!enemy.isRetreating() || !enemy.hasClearRetreatPath(direction)) {
            enemy.finishRetreat();
            return;
        }
        enemy.moveHorizontal(direction);
        enemy.setFacingLeft(direction < 0f);
    }

    @Override
    public void exit (Enemy enemy) {
        if (enemy.isRetreating()) enemy.finishRetreat();
        else enemy.moveStop();
    }
}
