package com.cursedarchie.platformer.levels.wrappers;

import com.badlogic.gdx.utils.Array;

public class RoomData {

    public String location;
    public int id;

    public RoomCoord coord;

    public int width;
    public int height;

    public String[] tileRows;

    public Array<EnemySpawnData> enemySpawns = new Array<>();

    public Neighbors neighbors;

    public LocationTransition locationEntry;
    public LocationTransition locationExit;
}
