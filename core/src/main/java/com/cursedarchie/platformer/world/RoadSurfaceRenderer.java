package com.cursedarchie.platformer.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.cursedarchie.platformer.levels.LocationInstance;
import com.cursedarchie.platformer.levels.RoomInstance;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.tiles.TileMetrics;
import com.cursedarchie.platformer.tiles.types.DefaultBlock;

/** Visual macro modules over the one-unit collision grid. */
final class RoadSurfaceRenderer implements Disposable {
    private static final int COLUMNS_PER_MODULE = 8;
    private static final int PIXELS_PER_COLUMN = 64;
    private static final int MODULE_HEIGHT_PIXELS = 1024;
    private static final int TRANSPARENT_TOP_PIXELS = 38;
    private static final float COLUMN_WIDTH = TileMetrics.SIZE / 4f;
    private static final float MODULE_HEIGHT = MODULE_HEIGHT_PIXELS * COLUMN_WIDTH / PIXELS_PER_COLUMN;
    private static final float SURFACE_FROM_BOTTOM = MODULE_HEIGHT -
        TRANSPARENT_TOP_PIXELS * COLUMN_WIDTH / PIXELS_PER_COLUMN;
    // The visible pavement continues behind the hero's feet, giving the road
    // depth without moving the Box2D floor or changing jump/collision behavior.
    private static final float WALK_LINE_INSET = .18f;

    private final Texture[] textures = new Texture[3];
    private final TextureRegion[][] columns = new TextureRegion[3][COLUMNS_PER_MODULE];
    private final TextureRegion clippedColumn = new TextureRegion();

    RoadSurfaceRenderer() {
        for (int variant = 0; variant < textures.length; variant++) {
            textures[variant] = new Texture(Gdx.files.internal(
                "terrain/ruins-road-" + (char) ('A' + variant) + ".png"));
            textures[variant].setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            if (textures[variant].getWidth() != COLUMNS_PER_MODULE * PIXELS_PER_COLUMN ||
                textures[variant].getHeight() != MODULE_HEIGHT_PIXELS)
                throw new IllegalArgumentException("Unexpected road macro-module size");
            for (int column = 0; column < COLUMNS_PER_MODULE; column++)
                columns[variant][column] = new TextureRegion(textures[variant],
                    column * PIXELS_PER_COLUMN, 0, PIXELS_PER_COLUMN, MODULE_HEIGHT_PIXELS);
        }
    }

    static boolean covers(RoomInstance room, int cellX, int cellY) {
        if (!("start".equals(room.getLocation()) || "ruins".equals(room.getLocation())) ||
            cellY > 1) return false;
        Tile[][] tiles = room.getTiles();
        return tiles != null && cellX >= 0 && cellX < tiles.length && tiles[cellX].length > 1 &&
            tiles[cellX][0] instanceof DefaultBlock &&
            tiles[cellX][1] instanceof DefaultBlock;
    }

    void draw(LocationInstance location, SpriteBatch batch,
              float visibleLeft, float visibleRight) {
        Long generatedSeed = location.getGenerationSeed();
        long seed = generatedSeed == null ? 0L : generatedSeed;
        for (RoomInstance room : location.getRooms()) {
            if (!("start".equals(room.getLocation()) || "ruins".equals(room.getLocation())) ||
                room.getWorldX() > visibleRight || room.getWorldX() + room.getWidth() < visibleLeft)
                continue;
            float bottom = room.getWorldY() + 2f + WALK_LINE_INSET - SURFACE_FROM_BOTTOM;
            for (int cellX = 0; cellX < room.getTileColumns(); cellX++) {
                if (!covers(room, cellX, 0)) continue;
                float cellLeft = room.getWorldX() + cellX * TileMetrics.SIZE;
                float cellRight = cellLeft + TileMetrics.SIZE;
                if (cellRight < visibleLeft || cellLeft > visibleRight) continue;
                int firstColumn = (int) Math.floor((cellLeft + .00001f) / COLUMN_WIDTH);
                int lastColumn = (int) Math.ceil((cellRight - .00001f) / COLUMN_WIDTH) - 1;
                for (int worldColumn = firstColumn; worldColumn <= lastColumn; worldColumn++) {
                    float columnLeft = worldColumn * COLUMN_WIDTH;
                    float drawLeft = Math.max(cellLeft, columnLeft);
                    float drawRight = Math.min(cellRight, columnLeft + COLUMN_WIDTH);
                    if (drawRight <= drawLeft || drawRight < visibleLeft || drawLeft > visibleRight)
                        continue;
                    int module = Math.floorDiv(worldColumn, COLUMNS_PER_MODULE);
                    int variant = variant(seed, module);
                    int column = Math.floorMod(worldColumn, COLUMNS_PER_MODULE);
                    TextureRegion source = columns[variant][column];
                    clipHorizontally(source, drawLeft, drawRight, columnLeft);
                    batch.draw(clippedColumn, drawLeft, bottom,
                        drawRight - drawLeft, MODULE_HEIGHT);
                }
            }
        }
    }

    private void clipHorizontally(TextureRegion source, float drawLeft,
                                  float drawRight, float columnLeft) {
        clippedColumn.setRegion(source);
        float uSpan = source.getU2() - source.getU();
        clippedColumn.setU(source.getU() + (drawLeft - columnLeft) / COLUMN_WIDTH * uSpan);
        clippedColumn.setU2(source.getU() + (drawRight - columnLeft) / COLUMN_WIDTH * uSpan);
    }

    private static int variant(long seed, int module) {
        long value = seed ^ (module * 0x9e3779b97f4a7c15L);
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        value ^= value >>> 31;
        return (int) Long.remainderUnsigned(value, 3);
    }

    @Override public void dispose() {
        for (Texture texture : textures) if (texture != null) texture.dispose();
    }
}
