package com.cursedarchie.platformer.actors.enemies;

import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.actors.Enemy;

public class DefaultEnemy extends Enemy {


    public DefaultEnemy(Vector2 pos) {
        super(pos);
        this.setSize(0.5f);
        this.setMaxHealth(3f);
        this.setMaxAcceleration(50f);
        this.setDamage(1f);
        this.setAttackDuration(0.5f);
        this.setViewDistance(6f);
        this.setViewAngle(90f);
        this.setHealth(this.getMaxHealth());
        this.getBounds().setWidth(this.getSize());
        this.getBounds().setHeight(this.getSize());
    }
}
