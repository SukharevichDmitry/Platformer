package com.cursedarchie.platformer.tiles.types;

import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.tiles.TileMetrics;

/** Full-height tile with an independent fixture so a sword hit can remove it. */
public final class BreakableBlock extends Tile {
    public BreakableBlock(Vector2 pos) { super(pos, TileMetrics.SIZE, TileMetrics.SIZE); }
}
