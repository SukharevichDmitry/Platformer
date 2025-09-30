package com.cursedarchie.platformer.actors.enemies;

import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.actors.NewEnemy;

public class Boss extends NewEnemy {

    public Boss(Vector2 pos) {
        super(pos, 4f, 10f);
        this.setMaxAcceleration(10f);
        this.setDamage(1f);
        this.setAttackDuration(2f);
        this.setViewDistance(5f);
        this.setViewAngle(90f);
    }

}
