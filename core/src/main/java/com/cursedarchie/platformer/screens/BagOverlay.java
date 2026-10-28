package com.cursedarchie.platformer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.Disposable;
import com.cursedarchie.platformer.run.Inventory;
import com.cursedarchie.platformer.run.ItemCatalog;
import com.cursedarchie.platformer.world.GameWorld;

/** Readable first bag layout; faction decoration can be layered around the stable grid later. */
final class BagOverlay implements Disposable {
    private static final float CELL = 68f;
    private static final float GAP = 8f;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final Texture bandage = new Texture(Gdx.files.internal("interactions/field-bandage.png"));
    private final Texture note = new Texture(Gdx.files.internal("interactions/sealed-note.png"));
    private int selected;
    private boolean abilitiesPage;

    BagOverlay() {
        bandage.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        note.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    void resize(int width, int height) { camera.setToOrtho(false, width, height); camera.update(); }
    void move(int dx, int dy) {
        int x = Math.max(0, Math.min(Inventory.COLUMNS - 1, selected % Inventory.COLUMNS + dx));
        int y = Math.max(0, Math.min(Inventory.ROWS - 1, selected / Inventory.COLUMNS + dy));
        selected = y * Inventory.COLUMNS + x;
    }
    int selectedSlot() { return selected; }
    void togglePage() { abilitiesPage = !abilitiesPage; }
    boolean isAbilitiesPage() { return abilitiesPage; }

    void render(GameWorld world, String dashKey) {
        if (abilitiesPage) {
            renderAbilities(world, dashKey);
            return;
        }
        float w = camera.viewportWidth, h = camera.viewportHeight;
        float gridWidth = Inventory.COLUMNS * CELL + (Inventory.COLUMNS - 1) * GAP;
        float gridHeight = Inventory.ROWS * CELL + (Inventory.ROWS - 1) * GAP;
        float left = (w - gridWidth) / 2f;
        float bottom = (h - gridHeight) / 2f - 8f;
        float pocketGap = Math.max(12f,
            Math.min(158f, (w - gridWidth - 2f * CELL) / 2f - 20f));
        float pocketBottom = Math.max(12f, bottom - h * .15f);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, .74f);
        shapes.rect(0f, 0f, w, h);
        shapes.setColor(.24f, .17f, .12f, 1f);
        shapes.rect(left - 28f, bottom - 62f, gridWidth + 56f, gridHeight + 116f);
        shapes.setColor(.52f, .37f, .23f, 1f);
        shapes.rect(left - 20f, bottom - 54f, gridWidth + 40f, gridHeight + 100f);
        shapes.setColor(.10f, .08f, .07f, 1f);
        shapes.rect(left - 15f, bottom - 49f, gridWidth + 30f, gridHeight + 90f);
        for (int i = 0; i < Inventory.CAPACITY; i++) {
            float x = left + (i % Inventory.COLUMNS) * (CELL + GAP);
            float y = bottom + (Inventory.ROWS - 1 - i / Inventory.COLUMNS) * (CELL + GAP);
            shapes.setColor(i == selected ? new Color(.88f, .72f, .42f, 1f) : new Color(.40f, .31f, .23f, 1f));
            shapes.rect(x - 2f, y - 2f, CELL + 4f, CELL + 4f);
            shapes.setColor(.13f, .11f, .10f, 1f);
            shapes.rect(x + 2f, y + 2f, CELL - 4f, CELL - 4f);
        }
        for (int pocket = 0; pocket < Inventory.POCKETS; pocket++) {
            float x = pocket == 0 ? left - CELL - pocketGap : left + gridWidth + pocketGap;
            float y = pocketBottom;
            shapes.setColor(.70f, .51f, .29f, 1f);
            shapes.rect(x - 2f, y - 2f, CELL + 4f, CELL + 4f);
            shapes.setColor(.13f, .11f, .10f, 1f);
            shapes.rect(x + 2f, y + 2f, CELL - 4f, CELL - 4f);
        }
        shapes.end();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(.95f, .86f, .68f, 1f);
        font.draw(batch, "BAG  |  ABILITIES [TAB]", left, bottom + gridHeight + 35f);
        font.draw(batch, "6 SLOTS", left + gridWidth - 60f, bottom + gridHeight + 35f);
        Inventory inventory = world.getInventory();
        for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
            String id = inventory.itemId(i);
            if (id == null) continue;
            float x = i < Inventory.CAPACITY ? left + (i % Inventory.COLUMNS) * (CELL + GAP) :
                i == Inventory.CAPACITY ? left - CELL - pocketGap : left + gridWidth + pocketGap;
            float y = i < Inventory.CAPACITY ?
                bottom + (Inventory.ROWS - 1 - i / Inventory.COLUMNS) * (CELL + GAP) : pocketBottom;
            batch.draw(ItemCatalog.SEALED_NOTE.equals(id) ? note : bandage,
                x + 9f, y + 9f, 48f, 48f);
            if (inventory.count(i) > 1)
                font.draw(batch, Integer.toString(inventory.count(i)), x + 51f, y + 18f);
        }
        String item = inventory.itemId(selected);
        font.draw(batch, item == null ? "EMPTY" : ItemCatalog.SEALED_NOTE.equals(item) ?
            "SEALED NOTE" : "BANDAGE x" + inventory.count(selected), left, bottom - 25f);
        batch.end();
    }

    private void renderAbilities(GameWorld world, String dashKey) {
        float w = camera.viewportWidth, h = camera.viewportHeight;
        float panelWidth = Math.min(w - 36f, 610f);
        float panelHeight = Math.min(h - 36f, 345f);
        float x = (w - panelWidth) / 2f;
        float y = (h - panelHeight) / 2f;
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, .78f);
        shapes.rect(0f, 0f, w, h);
        shapes.setColor(.35f, .25f, .17f, 1f);
        shapes.rect(x, y, panelWidth, panelHeight);
        shapes.setColor(.83f, .73f, .55f, 1f);
        shapes.rect(x + 7f, y + 7f, panelWidth - 14f, panelHeight - 14f);
        shapes.setColor(.25f, .17f, .12f, 1f);
        shapes.rect(x + 20f, y + panelHeight - 58f, panelWidth - 40f, 2f);
        shapes.end();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(.19f, .12f, .08f, 1f);
        font.draw(batch, "BAG [TAB]  |  ABILITIES", x + 25f, y + panelHeight - 29f);
        if (world.hasEchoDash()) {
            font.draw(batch, "ECHO DASH", x + 25f, y + panelHeight - 85f);
            font.draw(batch, "Dash through danger. Key: " + dashKey,
                x + 25f, y + panelHeight - 112f);
            font.draw(batch, "The Keeper bound an echo to your steps.",
                x + 25f, y + panelHeight - 139f);
            font.draw(batch, "Price: one point of maximum health for this run.",
                x + 25f, y + panelHeight - 166f);
        } else {
            font.draw(batch, "No abilities recorded in this run.",
                x + 25f, y + panelHeight - 90f);
        }
        batch.end();
    }

    void renderPockets(GameWorld world) {
        float right = camera.viewportWidth - 12f;
        float bottom = 12f;
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeType.Filled);
        for (int pocket = 0; pocket < Inventory.POCKETS; pocket++) {
            float x = right - (Inventory.POCKETS - pocket) * (CELL + GAP);
            shapes.setColor(.55f, .39f, .22f, .9f);
            shapes.rect(x, bottom, CELL, CELL);
            shapes.setColor(.12f, .10f, .09f, .9f);
            shapes.rect(x + 3f, bottom + 3f, CELL - 6f, CELL - 6f);
        }
        shapes.end();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        Inventory inventory = world.getInventory();
        for (int pocket = 0; pocket < Inventory.POCKETS; pocket++) {
            int slot = Inventory.CAPACITY + pocket;
            float x = right - (Inventory.POCKETS - pocket) * (CELL + GAP);
            String id = inventory.itemId(slot);
            if (id != null) batch.draw(ItemCatalog.SEALED_NOTE.equals(id) ? note : bandage,
                x + 9f, bottom + 9f, 48f, 48f);
            font.setColor(.95f, .86f, .68f, 1f);
            if (inventory.count(slot) > 1)
                font.draw(batch, Integer.toString(inventory.count(slot)), x + 51f, bottom + 18f);
        }
        batch.end();
    }

    @Override public void dispose() { note.dispose(); bandage.dispose(); font.dispose(); batch.dispose(); shapes.dispose(); }
}
