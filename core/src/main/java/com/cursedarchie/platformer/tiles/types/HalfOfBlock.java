package com.cursedarchie.platformer.tiles.types;

import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.tiles.Tile;

public class HalfOfBlock extends Tile {
    public static final float HEIGHT = 0.5f;

    public HalfOfBlock(Vector2 pos) {
        super(pos, 1f, HEIGHT);
    }

}
