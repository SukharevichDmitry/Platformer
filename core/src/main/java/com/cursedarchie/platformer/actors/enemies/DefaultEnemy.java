package com.cursedarchie.platformer.actors.enemies;

import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.actors.NewEnemy;

public class DefaultEnemy extends NewEnemy {


    public DefaultEnemy(Vector2 pos) {
        super(pos, 0.5f, 3f);
        this.setMaxAcceleration(50f);
        this.setDamage(1f);
        this.setAttackDuration(0.5f);
        this.setViewDistance(6f);
        this.setViewAngle(90f);
    }
}
