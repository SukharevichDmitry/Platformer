package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.utils.EnemySpawnData;

public class Level {

    public int width = 10;
    public int height = 7;

    public int[][] tileMap;
    public Array<EnemySpawnData> enemySpawns;
    public Array<Enemy> enemies;

    public Level() {
        tileMap = new int[width][height];
        enemySpawns = new Array<>();
        enemies = new Array<>();
    }

    public int[][] getTileMap() {
        return tileMap;
    }

    public Array<EnemySpawnData> getEnemySpawns() {
        return enemySpawns;
    }

    public Array<Enemy> getEnemies() {
        return enemies;
    }

    public Array<Enemy> getAliveEnemies() {
        Array<Enemy> alive = new Array<>();
        for (Enemy e : enemies) {
            if (e.isAlive()) alive.add(e);
        }
        return alive;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }

    public void clearEnemies() {
        enemies.clear();
    }
}
