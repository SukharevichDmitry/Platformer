package com.cursedarchie.platformer.actors.enemies.states.motion;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;

public class ChaseState implements EnemyState {
    @Override
    public void enter(Enemy enemy) {
        Gdx.app.log("Enemy", "Started Chasing");
    }

    @Override
    public void update(Enemy enemy, float delta) {
        Vector2 target = enemy.getHeroLastKnownPos();
        if (target == null) {
            return;
        }

        // In a group fight the enemy walks to its own combat slot; otherwise it closes on the hero's last position.
        boolean toSlot = enemy.isCanSeeHero() && enemy.hasSlotTarget();
        float goal = toSlot ? enemy.getSlotTargetX() : target.x;
        float dx = goal - enemy.getPosition().x;
        if (Math.abs(dx) < (toSlot ? .1f : .55f)) enemy.moveStop();
        else if (dx < 0f) enemy.moveLeft();
        else enemy.moveRight();
        if (Math.abs(dx) < 3f) enemy.pursueElevatedTarget(target.y);
    }

    @Override
    public void exit (Enemy enemy) {
        enemy.moveStop();
        Gdx.app.log("Enemy", "Finished Chasing");
    }
}
