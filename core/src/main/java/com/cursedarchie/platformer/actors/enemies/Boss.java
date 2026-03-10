package com.cursedarchie.platformer.actors.enemies;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

public class Boss extends Enemy {

    public Boss(World world, Vector2 pos) {
        super(world, pos, 4f, 10f);
        this.setMaxAcceleration(10f);
        this.setDamage(1f);
        this.setAttackDuration(2f);
        this.setViewDistance(5f);
        this.setViewAngle(90f);
    }

}
