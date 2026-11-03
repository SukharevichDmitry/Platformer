package com.cursedarchie.platformer.combat;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/** Logical body rectangle that attacks and projectiles test against, centred on the actor. */
public final class Hurtbox {
    public final float width;
    public final float height;
    public final float offsetX;
    public final float offsetY;

    public Hurtbox(float width, float height, float offsetX, float offsetY) {
        this.width = width;
        this.height = height;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    /** Writes the world-space rectangle for an actor whose body is at {@code center}. */
    public Rectangle bounds(Vector2 center, Rectangle out) {
        return out.set(center.x + offsetX - width / 2f, center.y + offsetY - height / 2f, width, height);
    }
}
