package com.cursedarchie.platformer.actors.enemies.states.motion;

import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;

public class RetreatState implements EnemyState {
    @Override
    public void enter(Enemy enemy) {
    }

    @Override
    public void update (Enemy enemy, float delta) {
        float direction = enemy.getHeroLastKnownPos().x < enemy.getPosition().x ? 1f : -1f;
        if (enemy.hasGroundAhead(direction)) enemy.moveHorizontal(direction);
        else enemy.moveStop();
        enemy.setFacingLeft(direction > 0f);
    }

    @Override
    public void exit (Enemy enemy) {
        enemy.moveStop();
    }
}
