package com.cursedarchie.platformer.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/** Small stone-carved pixel capitals for world item labels. */
final class ItemLabelFont implements Disposable {
    private static final String LETTERS = "ABDEGNOT";
    private static final String[][] GLYPHS = {
        {".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"}, // A
        {"####.", "#...#", "#...#", "####.", "#...#", "#...#", "####."}, // B
        {"####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####."}, // D
        {"#####", "#....", "#....", "####.", "#....", "#....", "#####"}, // E
        {".####", "#....", "#....", "#.###", "#...#", "#...#", ".###."}, // G
        {"#...#", "##..#", "#.#.#", "#.#.#", "#..##", "#...#", "#...#"}, // N
        {".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."}, // O
        {"#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."}  // T
    };
    private static final float PIXEL = 0.038f;
    private final Texture texture;
    private final TextureRegion[] regions = new TextureRegion[LETTERS.length()];

    ItemLabelFont() {
        Pixmap pixels = new Pixmap(LETTERS.length() * 6, 7, Pixmap.Format.RGBA8888);
        pixels.setColor(Color.WHITE);
        for (int i = 0; i < GLYPHS.length; i++) {
            for (int y = 0; y < 7; y++) {
                for (int x = 0; x < 5; x++) {
                    if (GLYPHS[i][y].charAt(x) == '#') pixels.drawPixel(i * 6 + x, y);
                }
            }
        }
        texture = new Texture(pixels);
        pixels.dispose();
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        for (int i = 0; i < regions.length; i++)
            regions[i] = new TextureRegion(texture, i * 6, 0, 5, 7);
    }

    void drawCentered(SpriteBatch batch, String label, float centerX, float bottomY, Color color) {
        float width = (label.length() * 6 - 1) * PIXEL;
        draw(batch, label, centerX - width / 2f + PIXEL, bottomY - PIXEL, Color.BLACK);
        draw(batch, label, centerX - width / 2f, bottomY, color);
        batch.setColor(Color.WHITE);
    }

    private void draw(SpriteBatch batch, String label, float left, float bottom, Color color) {
        batch.setColor(color);
        for (int i = 0; i < label.length(); i++) {
            int index = LETTERS.indexOf(label.charAt(i));
            if (index >= 0)
                batch.draw(regions[index], left + i * 6 * PIXEL, bottom, 5 * PIXEL, 7 * PIXEL);
        }
    }

    @Override public void dispose() { texture.dispose(); }
}
