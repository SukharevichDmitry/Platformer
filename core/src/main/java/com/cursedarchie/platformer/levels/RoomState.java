package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.enemies.states.EnemyState;

public class RoomState {

    boolean visitedFirstTime = false;

    boolean enemiesActivated = false;

    public Array<EnemyState> enemies = new Array<>();
}
