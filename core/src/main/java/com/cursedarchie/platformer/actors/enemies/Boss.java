package com.cursedarchie.platformer.actors.enemies;

import com.badlogic.gdx.math.Vector2;

public class Boss extends Enemy {

    public Boss(Vector2 pos) {
        super(pos);
        this.setSize(4f);
        this.setMaxHealth(10f);
        this.setMaxAcceleration(10f);
        this.setDamage(1f);
        this.setAttackDuration(2f);
        this.setViewDistance(5f);
        this.setViewAngle(90f);
        this.setHealth(this.getMaxHealth());
        this.getBounds().setWidth(this.getSize());
        this.getBounds().setHeight(this.getSize());
    }

    // Можно добавить уникальные способности, например:
    public void specialAttack() {
        if (isAlive() && getState() != EnemyState.ATTACKING) {
            startAttack();
        }
    }

}
