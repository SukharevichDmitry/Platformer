package com.cursedarchie.platformer.actors.enemies.logic;

import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.world.GameWorld;

public interface EnemyBrain {
    void think(Enemy enemy, GameWorld world, float delta);
}
