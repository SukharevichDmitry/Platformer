package com.cursedarchie.platformer.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.cursedarchie.platformer.levels.LocationInstance;

/** Brief screen-space title on entry to a location, not on every room change. */
public final class LocationTitleOverlay implements Disposable {
    private static final float FADE_IN_SECONDS = 0.4f;
    private static final float HOLD_SECONDS = 2f;
    private static final float FADE_OUT_SECONDS = 0.8f;
    private static final float TOTAL_SECONDS = FADE_IN_SECONDS + HOLD_SECONDS + FADE_OUT_SECONDS;

    private final GameWorld world;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final GlyphLayout layout = new GlyphLayout();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private LocationInstance shownLocation;
    private String title;
    private float elapsed = TOTAL_SECONDS;

    public LocationTitleOverlay(GameWorld world) {
        this.world = world;
        font.getData().setScale(1.5f);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
    }

    public void update(float delta) {
        LocationInstance current = world.getLocation();
        if (current != shownLocation) {
            shownLocation = current;
            title = displayName(current.getLocation());
            elapsed = 0f;
            layout.setText(font, title);
        }
        elapsed = Math.min(TOTAL_SECONDS, elapsed + Math.max(0f, delta));
    }

    public String getTitle() { return title; }

    public float getOpacity() {
        if (elapsed < FADE_IN_SECONDS) return elapsed / FADE_IN_SECONDS;
        if (elapsed < FADE_IN_SECONDS + HOLD_SECONDS) return 1f;
        return Math.max(0f, (TOTAL_SECONDS - elapsed) / FADE_OUT_SECONDS);
    }

    public void render() {
        float opacity = getOpacity();
        if (opacity <= 0f || title == null) return;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        float x = (camera.viewportWidth - layout.width) / 2f;
        float y = camera.viewportHeight - 42f + 6f * (1f - opacity);
        // Draw the string with the current font tint; a cached GlyphLayout retains
        // its original glyph colors and would otherwise ignore the changing alpha.
        font.setColor(0f, 0f, 0f, 0.6f * opacity);
        font.draw(batch, title, x + 1f, y - 1f);
        font.setColor(1f, 0.94f, 0.78f, opacity);
        font.draw(batch, title, x, y);
        batch.end();
    }

    private static String displayName(String locationId) {
        String words = locationId.replace('_', ' ').replace('-', ' ');
        if (words.isEmpty()) return words;
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    @Override
    public void dispose() {
        font.dispose();
        batch.dispose();
    }
}
