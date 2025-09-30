package com.cursedarchie.platformer.actors.enemies;

import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.actors.Enemy;

public class Boss extends Enemy {

    public Boss(Vector2 pos) {
        super(pos, 4f, 10f);
        this.setMaxAcceleration(10f);
        this.setDamage(1f);
        this.setAttackDuration(2f);
        this.setViewDistance(5f);
        this.setViewAngle(90f);
    }

}
