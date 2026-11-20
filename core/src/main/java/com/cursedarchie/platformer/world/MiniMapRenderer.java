package com.cursedarchie.platformer.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.cursedarchie.platformer.levels.RoomInstance;

/** Screen-space room map. Discovery is owned by GameWorld, never by rendering. */
public final class MiniMapRenderer implements Disposable {
    private static final float PANEL_WIDTH = 190f;
    private static final float PANEL_HEIGHT = 110f;
    private static final float MARGIN = 14f;
    private static final float PADDING = 9f;
    private static final Color BACKGROUND = new Color(0.04f, 0.06f, 0.10f, 0.72f);
    private static final Color VISITED = new Color(0.47f, 0.59f, 0.69f, 1f);
    private static final Color CURRENT = new Color(0.98f, 0.80f, 0.32f, 1f);

    private final GameWorld world;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final Matrix4 projection = new Matrix4();
    private int screenWidth;
    private int screenHeight;

    public MiniMapRenderer(GameWorld world) {
        this.world = world;
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        projection.setToOrtho2D(0f, 0f, width, height);
    }

    public void render() {
        if (screenWidth <= 0 || screenHeight <= 0) return;
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (RoomInstance room : world.getLocation().getRooms()) {
            minX = Math.min(minX, room.getWorldX());
            minY = Math.min(minY, room.getWorldY());
            maxX = Math.max(maxX, room.getWorldX() + room.getWidth());
            maxY = Math.max(maxY, room.getWorldY() + room.getHeight());
        }
        if (minX == Float.POSITIVE_INFINITY) return;

        float panelWidth = Math.min(PANEL_WIDTH, screenWidth * 0.32f);
        float panelHeight = Math.min(PANEL_HEIGHT, screenHeight * 0.25f);
        float x = screenWidth - MARGIN - panelWidth;
        float y = screenHeight - MARGIN - panelHeight;
        float scale = Math.min((panelWidth - 2f * PADDING) / (maxX - minX),
            (panelHeight - 2f * PADDING) / (maxY - minY));
        float mapWidth = (maxX - minX) * scale;
        float mapHeight = (maxY - minY) * scale;
        float originX = x + (panelWidth - mapWidth) / 2f;
        float originY = y + (panelHeight - mapHeight) / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeType.Filled);
        shapes.setColor(BACKGROUND);
        shapes.rect(x, y, panelWidth, panelHeight);
        for (RoomInstance room : world.getLocation().getRooms()) {
            if (!world.isRoomVisited(room.getRoomId())) continue;
            boolean current = room == world.getCurrentRoom();
            shapes.setColor(current ? CURRENT : VISITED);
            // Keep a small gap between touching room rectangles for readability.
            shapes.rect(originX + (room.getWorldX() - minX) * scale + 1f,
                originY + (room.getWorldY() - minY) * scale + 1f,
                Math.max(1f, room.getWidth() * scale - 2f),
                Math.max(1f, room.getHeight() * scale - 2f));
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }
}
