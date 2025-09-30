package com.cursedarchie.platformer.actors.tiles;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class HalfOfBlock extends Tile{


    public HalfOfBlock(Vector2 pos) {
        super(pos);
        this.setSize(0.5f);
        this.bounds.width = SIZE * 2;
        this.bounds.height = SIZE;
    }

    @Override
    public TextureRegion getTextureRegion() {
        return new TextureRegion(atlas.findRegion("block"));
    }

}
