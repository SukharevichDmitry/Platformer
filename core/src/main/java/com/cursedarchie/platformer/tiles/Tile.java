package com.cursedarchie.platformer.tiles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public abstract class Tile {

    public float SIZE;
    TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("atlases/game.atlas"));

    Vector2  position = new Vector2();
    Rectangle  bounds = new Rectangle();
    private boolean isBlockingSight = true;


    public Tile(Vector2 pos) {
        this.position = pos;
        this.bounds.setX(pos.x);
        this.bounds.setY(pos.y);
        this.bounds.width = SIZE;
        this.bounds.height = SIZE;
    }

    public Vector2 getPosition() {
        return position;
    }
    public Rectangle getBounds() {
        return bounds;
    }
    public void setSize(float SIZE) {
        this.SIZE = SIZE;
    }
    public float getSize() {
        return SIZE;
    }


    public boolean isBlockingSight() {
        return isBlockingSight;
    }
    public void setBlockingSight(boolean blockingSight) {
        isBlockingSight = blockingSight;
    }

    public abstract TextureRegion getTextureRegion();
}
