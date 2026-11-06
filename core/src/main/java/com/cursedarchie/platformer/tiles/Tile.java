package com.cursedarchie.platformer.tiles;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public abstract class Tile {

    protected final Vector2 position;
    protected final Rectangle bounds;
    private boolean isBlockingSight = true;


    public Tile(Vector2 localPos, float width, float height) {
        this.position = localPos;
        this.bounds = new Rectangle(localPos.x, localPos.y, width, height);
    }

    public Vector2 getPosition() {
        return position;
    }
    public Rectangle getBounds() {
        return bounds;
    }
    public boolean isSolid() {
        return true;
    }

    /** Only whole grid cells may join the location's merged terrain fixtures. */
    public boolean isFullCellSolid() {
        return false;
    }

    public boolean isBlockingSight() {
        return isBlockingSight;
    }
    public void setBlockingSight(boolean blockingSight) {
        isBlockingSight = blockingSight;
    }

}
