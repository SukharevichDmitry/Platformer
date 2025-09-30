package com.cursedarchie.platformer.actors.enemies.logic;

import com.cursedarchie.platformer.actors.NewEnemy;

public interface EnemyState {
    void enter(NewEnemy enemy);
    void update (NewEnemy enemy, float delta);
    void exit (NewEnemy enemy);
}
