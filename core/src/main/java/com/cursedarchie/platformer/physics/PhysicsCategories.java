package com.cursedarchie.platformer.physics;

public final class PhysicsCategories {

    private PhysicsCategories() {}

    // Основные тела
    public static final short HERO        = 0x0001; // 0000 0000 0000 0001
    public static final short ENEMY       = 0x0002; // 0000 0000 0000 0010
    public static final short WALL        = 0x0004; // 0000 0000 0000 0100
    public static final short GROUND      = 0x0008; // 0000 0000 0000 1000

    // Сенсоры
    public static final short HERO_ATTACK = 0x0010; // 0000 0000 0001 0000
    public static final short ENEMY_ATTACK= 0x0020; // 0000 0000 0010 0000
    public static final short VISION      = 0x0040; // 0000 0000 0100 0000

}
