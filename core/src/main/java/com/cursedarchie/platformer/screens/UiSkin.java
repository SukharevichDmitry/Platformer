package com.cursedarchie.platformer.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

/** Self-contained prototype UI using the font bundled with libGDX. */
final class UiSkin {
    private UiSkin() { }

    static Skin create() {
        Skin skin = new Skin();
        skin.add("default-font", new BitmapFont());
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        Texture texture = new Texture(pixel);
        pixel.dispose();
        skin.add("white", texture);

        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle();
        button.font = skin.getFont("default-font");
        button.fontColor = Color.WHITE;
        button.up = skin.newDrawable("white", new Color(0.18f, 0.23f, 0.32f, 1));
        button.over = skin.newDrawable("white", new Color(0.28f, 0.36f, 0.48f, 1));
        button.down = skin.newDrawable("white", new Color(0.10f, 0.14f, 0.21f, 1));
        skin.add("default", button);
        return skin;
    }
}
