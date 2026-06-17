package com.cursedarchie.platformer.tiles;

import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.tiles.types.DefaultBlock;
import com.cursedarchie.platformer.tiles.types.HalfOfBlock;

public class TileFactory {

    public static Tile create(char c, Vector2 pos) {
        switch (c) {
            case '#': return new DefaultBlock(pos);
            case '_': return new HalfOfBlock(pos);
            default: return null;
        }
    }
}
