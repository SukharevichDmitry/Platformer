package com.cursedarchie.platformer.tiles;

/** Physical size of one terrain cell. Room dimensions remain in world units. */
public final class TileMetrics {
    public static final int CELLS_PER_UNIT = 2;
    public static final float SIZE = 1f / CELLS_PER_UNIT;

    private TileMetrics() {}
}
