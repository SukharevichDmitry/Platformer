package com.cursedarchie.platformer.tiles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public abstract class Tile {

    protected static final TextureAtlas ATLAS =
        new TextureAtlas(
            Gdx.files.internal("atlases/game.atlas")
        );

    protected float SIZE;
    protected Vector2  position;
    protected Rectangle  bounds = new Rectangle();
    private boolean isBlockingSight = true;


    public Tile(Vector2 localPos) {
        this.position = localPos;
        this.bounds.setX(localPos.x);
        this.bounds.setY(localPos.y);
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

    public boolean isSolid() {
        return true;
    }

    public boolean isBlockingSight() {
        return isBlockingSight;
    }
    public void setBlockingSight(boolean blockingSight) {
        isBlockingSight = blockingSight;
    }

    public abstract TextureRegion getTextureRegion();
}
