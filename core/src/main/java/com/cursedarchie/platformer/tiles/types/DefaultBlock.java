package com.cursedarchie.platformer.tiles.types;

import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.tiles.TileMetrics;

public class DefaultBlock extends Tile {

    public DefaultBlock(Vector2 pos) {
        super(pos, TileMetrics.SIZE, TileMetrics.SIZE);
    }

    @Override
    public boolean isFullCellSolid() {
        return true;
    }

}
