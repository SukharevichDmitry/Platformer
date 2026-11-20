package com.cursedarchie.platformer.tiles;

import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.tiles.types.DefaultBlock;
import com.cursedarchie.platformer.tiles.types.HalfOfBlock;
import com.cursedarchie.platformer.tiles.types.OneWayPlatform;
import com.cursedarchie.platformer.tiles.types.BreakableBlock;

public class TileFactory {

    public static Tile create(char c, Vector2 worldPos) {
        switch (c) {
            case '#': return new DefaultBlock(worldPos);
            case '_': return new HalfOfBlock(worldPos);
            case '=': return new OneWayPlatform(worldPos);
            case 'B': return new BreakableBlock(worldPos);
            default: return null;
        }
    }
}
