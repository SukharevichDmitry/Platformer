package com.cursedarchie.platformer.tiles.types;

import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.tiles.TileMetrics;

public class HalfOfBlock extends Tile {
    public static final float HEIGHT = TileMetrics.SIZE / 2f;

    public HalfOfBlock(Vector2 pos) {
        super(pos, TileMetrics.SIZE, HEIGHT);
    }

}
