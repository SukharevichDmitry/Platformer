package com.cursedarchie.platformer.actors.enemies.logic;

import com.cursedarchie.platformer.actors.Enemy;

public interface EnemyState {
    void enter(Enemy enemy);
    void update (Enemy enemy, float delta);
    void exit (Enemy enemy);
}
