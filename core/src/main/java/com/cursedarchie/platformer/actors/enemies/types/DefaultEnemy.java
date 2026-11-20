package com.cursedarchie.platformer.actors.enemies.types;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.EnemyConfig;

public class DefaultEnemy extends Enemy {

    public DefaultEnemy(World world, Vector2 pos, EnemyConfig cfg) {
        super(world, pos, cfg);
    }
}
