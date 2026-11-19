package com.cursedarchie.platformer.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

/** Central pixel-art sampling policy. Apply it at every texture/atlas loading boundary. */
public final class PixelArtTextures {
    private PixelArtTextures() {}

    public static Texture nearest(Texture texture) {
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    public static TextureAtlas nearest(TextureAtlas atlas) {
        for (Texture texture : atlas.getTextures()) nearest(texture);
        return atlas;
    }
}
