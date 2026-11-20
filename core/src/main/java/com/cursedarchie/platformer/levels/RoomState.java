package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;

public class RoomState {
    private boolean enemiesSpawned;

    boolean visitedFirstTime = false;

    boolean enemiesActivated = false;

    public Array<EnemyState> enemies = new Array<>();

    public boolean isEnemiesSpawned() {
        return enemiesSpawned;
    }

    public void setEnemiesSpawned(boolean value) {
        enemiesSpawned = value;
    }
}
