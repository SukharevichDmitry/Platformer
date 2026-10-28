package com.cursedarchie.platformer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Preferences;

/** Persist display mode and the preferred windowed resolution independently of the active run. */
public final class DisplaySettings {
    private static final String WIDTH = "display.width";
    private static final String HEIGHT = "display.height";
    private static final String FULLSCREEN = "display.fullscreen";
    private final Preferences preferences;

    public DisplaySettings(Preferences preferences) { this.preferences = preferences; }

    public void restore() {
        if (preferences.getBoolean(FULLSCREEN, false)) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else if (preferences.contains(WIDTH) && preferences.contains(HEIGHT)) {
            int width = preferences.getInteger(WIDTH);
            int height = preferences.getInteger(HEIGHT);
            if (supported(width, height)) Gdx.graphics.setWindowedMode(width, height);
        }
    }

    public boolean apply(int width, int height) {
        return apply(width, height, false);
    }

    public boolean apply(int width, int height, boolean fullscreen) {
        if (!supported(width, height)) return false;
        if (fullscreen) {
            if (!Gdx.graphics.isFullscreen() &&
                !Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode())) return false;
        } else if ((Gdx.graphics.isFullscreen() || Gdx.graphics.getWidth() != width ||
            Gdx.graphics.getHeight() != height) && !Gdx.graphics.setWindowedMode(width, height)) return false;
        preferences.putInteger(WIDTH, width).putInteger(HEIGHT, height)
            .putBoolean(FULLSCREEN, fullscreen).flush();
        return true;
    }

    public int[] windowedResolution() {
        int width = preferences.getInteger(WIDTH, Gdx.graphics.isFullscreen() ?
            Math.min(1280, Gdx.graphics.getDisplayMode().width) : Gdx.graphics.getWidth());
        int height = preferences.getInteger(HEIGHT, Gdx.graphics.isFullscreen() ?
            Math.min(720, Gdx.graphics.getDisplayMode().height) : Gdx.graphics.getHeight());
        if (supported(width, height)) return new int[] {width, height};
        return new int[] {Math.min(1280, Gdx.graphics.getDisplayMode().width),
            Math.min(720, Gdx.graphics.getDisplayMode().height)};
    }

    public boolean supported(int width, int height) {
        Graphics.DisplayMode mode = Gdx.graphics.getDisplayMode();
        return width >= 640 && height >= 480 && width <= mode.width && height <= mode.height;
    }
}
