package com.cursedarchie.platformer.actors.enemies.states.motion;

import com.badlogic.gdx.Gdx;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;

public class PatrolState implements EnemyState {
    @Override
    public void enter(Enemy enemy) {
        Gdx.app.log("Enemy", "Started Patroling");
    }

    @Override
    public void update (Enemy enemy, float delta) {
        if (enemy.isStaggered() || !enemy.isGrounded()) return;
        if (enemy.getPatrolPauseRemaining() > 0f) { enemy.moveStop(); return; }
        float x = enemy.getPosition().x;
        float direction = enemy.getPatrolDirection();
        if ((direction < 0f && x < enemy.getPatrolOriginX() - 1.6f) ||
            (direction > 0f && x > enemy.getPatrolOriginX() + 1.6f) ||
            !enemy.hasGroundAhead(direction)) {
            enemy.reversePatrol();
            enemy.moveStop();
            return;
        }
        enemy.moveHorizontal(direction * .45f);
    }

    @Override
    public void exit (Enemy enemy) {
        Gdx.app.log("Enemy", "Finished Patroling");
    }
}
