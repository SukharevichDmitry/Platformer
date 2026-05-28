package com.cursedarchie.platformer.utils;

import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.tiles.types.DefaultBlock;
import com.cursedarchie.platformer.tiles.types.HalfOfBlock;
import com.cursedarchie.platformer.tiles.Tile;

public class TileFactory {

    public static Tile create(int id, Vector2 pos) {
        switch (id) {
            case 1: return new DefaultBlock(pos);
            case 2: return new HalfOfBlock(pos);
            default: return null;
        }
    }
}
