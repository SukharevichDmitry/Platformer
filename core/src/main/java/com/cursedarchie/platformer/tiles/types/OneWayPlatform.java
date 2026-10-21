package com.cursedarchie.platformer.tiles.types;

import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.tiles.Tile;

/** Thin ledge: Box2D preSolve grants support only to actors approaching from above. */
public final class OneWayPlatform extends Tile {
    public static final float BOTTOM_OFFSET = 0.82f;
    public static final float HEIGHT = 0.18f;

    public OneWayPlatform(Vector2 pos) {
        super(new Vector2(pos.x, pos.y + BOTTOM_OFFSET), 1f, HEIGHT);
    }
}
