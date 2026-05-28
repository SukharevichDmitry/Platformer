package com.cursedarchie.platformer.actors.enemies;

import com.cursedarchie.platformer.physics.AttackConfig;

public class EnemyConfig {
    public float size;
    public float maxHealth;
    public float viewDistance;
    public float viewAngle;
    public float damage;
    public float attackDuration;
    public float velocity;
    public AttackConfig attackLeft = new AttackConfig();
    public AttackConfig attackRight = new AttackConfig();
}
