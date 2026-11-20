package com.cursedarchie.platformer.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

/** Small development overlay that makes a procedural run reproducible from a screenshot. */
public final class GenerationSeedOverlay implements Disposable {
    private final GameWorld world;
    private final OrthographicCamera camera = new OrthographicCamera();
    private SpriteBatch batch;
    private BitmapFont font;

    public GenerationSeedOverlay(GameWorld world) {
        this.world = world;
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
        camera.update();
    }

    public void render() {
        Long seed = world.getLocation().getGenerationSeed();
        if (seed == null) return;
        if (batch == null) {
            batch = new SpriteBatch();
            font = new BitmapFont();
            font.setColor(Color.WHITE);
        }
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.draw(batch, world.getLocation().getLocation() + " seed: " + seed, 12f, camera.viewportHeight - 12f);
        batch.end();
    }

    @Override
    public void dispose() {
        if (font != null) font.dispose();
        if (batch != null) batch.dispose();
    }
}
