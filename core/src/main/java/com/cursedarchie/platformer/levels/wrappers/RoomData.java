package com.cursedarchie.platformer.levels.wrappers;

import com.badlogic.gdx.utils.Array;

public class RoomData {

    public String location;
    public int id;
    public String sceneryTheme;

    public int width;
    public int height;

    public float worldX;
    public float worldY;

    public String[] tileRows;

    public Array<EnemySpawnData> enemySpawns = new Array<>();
    public Array<InteractionData> interactions = new Array<>();

    public Neighbors neighbors;

    public LocationTransition locationEntry;
    public LocationTransition locationExit;
}
