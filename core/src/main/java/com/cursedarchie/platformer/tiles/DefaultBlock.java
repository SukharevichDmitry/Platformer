package com.cursedarchie.platformer.tiles;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class DefaultBlock extends Tile{


    public DefaultBlock(Vector2 pos) {
        super(pos);
        this.setSize(1f);
        this.bounds.width = SIZE;
        this.bounds.height = SIZE;
    }

    @Override
    public TextureRegion getTextureRegion() {
        return new TextureRegion(atlas.findRegion("block"));
    }

}
