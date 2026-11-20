package com.cursedarchie.platformer.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

/** Small screen-space frame-rate readout for tuning combat and movement. */
public final class FpsOverlay implements Disposable {
    private final OrthographicCamera camera = new OrthographicCamera();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private int shownFps = -1;
    private String label = "FPS: 0";

    public FpsOverlay() {
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
        camera.update();
    }

    public void render() {
        int fps = Gdx.graphics.getFramesPerSecond();
        if (fps != shownFps) {
            shownFps = fps;
            label = "FPS: " + fps;
        }
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(0f, 0f, 0f, 0.9f);
        font.draw(batch, label, 13f, 23f);
        font.setColor(0.9f, 0.94f, 1f, 1f);
        font.draw(batch, label, 12f, 24f);
        batch.end();
    }

    @Override
    public void dispose() {
        font.dispose();
        batch.dispose();
    }
}
