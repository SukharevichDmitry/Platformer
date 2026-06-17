package com.cursedarchie.platformer.tiles.types;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.tiles.Tile;

public class DefaultBlock extends Tile {

    private static final TextureRegion TEXTURE = ATLAS.findRegion("block");

    public DefaultBlock(Vector2 pos) {
        super(pos);
        this.setSize(1f);
        this.bounds.width = SIZE;
        this.bounds.height = SIZE;
    }

    @Override
    public TextureRegion getTextureRegion() {
        return TEXTURE;
    }

}
