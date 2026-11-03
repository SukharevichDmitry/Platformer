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
import com.cursedarchie.platformer.controllers.ControlBindings;
import com.cursedarchie.platformer.controllers.ControlBindings.Action;
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
    private int source = -1;
    private int pointerStart = -1;
    private int craftFirst = -1;
    private int craftSecond = -1;
    private String craftMessage = "";
    private boolean abilitiesPage;

    BagOverlay() {
        bandage.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        note.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    void resize(int width, int height) { camera.setToOrtho(false, width, height); camera.update(); }
    void move(int dx, int dy) {
        if (selected >= Inventory.CAPACITY) {
            if (dy < 0) selected = selected == Inventory.CAPACITY ? 3 : 5;
            else if (dx != 0) selected = selected == Inventory.CAPACITY ? 3 : 5;
            return;
        }
        int x = Math.max(0, Math.min(Inventory.COLUMNS - 1, selected % Inventory.COLUMNS + dx));
        int y = Math.max(0, Math.min(Inventory.ROWS - 1, selected / Inventory.COLUMNS + dy));
        selected = dy > 0 && selected / Inventory.COLUMNS == Inventory.ROWS - 1 ?
            Inventory.CAPACITY + (x == 0 ? 0 : 1) : y * Inventory.COLUMNS + x;
    }
    int selectedSlot() { return selected; }
    int sourceSlot() { return source; }
    void cancelSelection() { source = -1; pointerStart = -1; craftFirst = craftSecond = -1; craftMessage = ""; }
    void combine(GameWorld world) {
        Inventory inventory = world.getInventory();
        if (craftFirst < 0) {
            if (inventory.itemId(selected) == null) { craftMessage = "Select an item first"; return; }
            craftFirst = selected;
            source = -1;
            craftMessage = "Choose a highlighted ingredient";
        } else if (craftSecond >= 0) {
            String result = inventory.combinationResult(craftFirst, craftSecond);
            if (world.combineItems(craftFirst, craftSecond)) {
                selected = craftFirst;
                craftMessage = "Created " + resultName(result);
            } else craftMessage = "Cannot combine these items";
            craftFirst = craftSecond = -1;
        } else selectIngredient(world);
    }
    private void selectIngredient(GameWorld world) {
        if (selected == craftFirst) return;
        String result = world.getInventory().combinationResult(craftFirst, selected);
        if (result == null) { craftMessage = "These items cannot be combined"; return; }
        if (!ItemCatalog.REINFORCED_BANDAGE.equals(result) &&
            world.getInventory().count(result) > 0) {
            craftMessage = "You already have this relic"; return;
        }
        craftSecond = selected;
        craftMessage = "Result: " + resultName(result) + ". C/Interact: create";
    }
    private static String resultName(String id) {
        if (ItemCatalog.WEIGHTED_BRACER.equals(id)) return "WEIGHTED BRACER";
        if (ItemCatalog.REINFORCED_BANDAGE.equals(id)) return "REINFORCED BANDAGE";
        return "FUSED RELIC";
    }
    void confirm(GameWorld world) {
        if (craftFirst >= 0) {
            if (craftSecond >= 0) combine(world);
            else selectIngredient(world);
            return;
        }
        if (source < 0) {
            if (world.getInventory().itemId(selected) != null) source = selected;
        } else {
            world.moveInventorySlot(source, selected);
            source = -1;
        }
    }
    void pointerDown(int screenX, int screenY) {
        pointerStart = slotAt(screenX, screenY);
        if (pointerStart >= 0) selected = pointerStart;
    }
    void pointerUp(int screenX, int screenY, GameWorld world) {
        if (pointerStart < 0) return;
        int destination = slotAt(screenX, screenY);
        if (craftFirst >= 0) {
            if (destination >= 0) { selected = destination; confirm(world); }
            pointerStart = -1;
            return;
        }
        if (destination >= 0 && destination != pointerStart &&
            world.getInventory().itemId(pointerStart) != null) {
            world.moveInventorySlot(pointerStart, destination);
            source = -1;
            selected = destination;
        } else if (destination == pointerStart) confirm(world);
        pointerStart = -1;
    }
    private int slotAt(int screenX, int screenY) {
        float x = screenX, y = camera.viewportHeight - screenY;
        for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
            float sx = slotX(i), sy = slotY(i);
            if (x >= sx && x <= sx + CELL && y >= sy && y <= sy + CELL) return i;
        }
        return -1;
    }
    private float gridLeft() { return (camera.viewportWidth - (Inventory.COLUMNS * CELL +
        (Inventory.COLUMNS - 1) * GAP)) / 2f; }
    private float gridBottom() { return (camera.viewportHeight - (Inventory.ROWS * CELL +
        (Inventory.ROWS - 1) * GAP)) / 2f - 8f; }
    private float pocketGap() { return Math.max(12f, Math.min(158f,
        (camera.viewportWidth - (Inventory.COLUMNS * CELL + (Inventory.COLUMNS - 1) * GAP) -
            2f * CELL) / 2f - 20f)); }
    private float slotX(int slot) { return slot < Inventory.CAPACITY ?
        gridLeft() + (slot % Inventory.COLUMNS) * (CELL + GAP) :
        slot == Inventory.CAPACITY ? gridLeft() - CELL - pocketGap() :
            gridLeft() + Inventory.COLUMNS * CELL + (Inventory.COLUMNS - 1) * GAP + pocketGap(); }
    private float slotY(int slot) { return slot < Inventory.CAPACITY ?
        gridBottom() + (Inventory.ROWS - 1 - slot / Inventory.COLUMNS) * (CELL + GAP) :
        Math.max(12f, gridBottom() - camera.viewportHeight * .15f); }
    void togglePage() { abilitiesPage = !abilitiesPage; }
    boolean isAbilitiesPage() { return abilitiesPage; }

    void render(GameWorld world, ControlBindings bindings) {
        if (abilitiesPage) {
            renderAbilities(world, bindings.keyName(Action.DASH));
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
            shapes.setColor(i == craftFirst ? Color.CYAN :
                i == craftSecond ? Color.GREEN : i == selected ? Color.GOLD :
                craftFirst >= 0 && world.getInventory().combinationResult(craftFirst, i) != null ?
                    Color.LIME : i == source ? Color.CYAN : new Color(.40f, .31f, .23f, 1f));
            shapes.rect(x - 2f, y - 2f, CELL + 4f, CELL + 4f);
            shapes.setColor(.13f, .11f, .10f, 1f);
            shapes.rect(x + 2f, y + 2f, CELL - 4f, CELL - 4f);
        }
        for (int pocket = 0; pocket < Inventory.POCKETS; pocket++) {
            float x = pocket == 0 ? left - CELL - pocketGap : left + gridWidth + pocketGap;
            float y = pocketBottom;
            int slot = Inventory.CAPACITY + pocket;
            shapes.setColor(slot == craftFirst ? Color.CYAN :
                slot == craftSecond ? Color.GREEN : slot == selected ? Color.GOLD :
                craftFirst >= 0 && world.getInventory().combinationResult(craftFirst, slot) != null ?
                    Color.LIME : slot == source ? Color.CYAN : new Color(.70f, .51f, .29f, 1f));
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
        font.draw(batch, bindings.keyName(Action.BAG) + "/Esc: close", left, bottom + gridHeight + 55f);
        font.draw(batch, bindings.keyName(Action.INTERACT) + ": select/move   Arrows: destination",
            left, bottom - 75f);
        font.draw(batch, "Mouse: click or drag to move", left, bottom - 94f);
        font.draw(batch, craftFirst >= 0 ? craftMessage : "C: combine selected item", left, bottom - 113f);
        for (int pocket = 0; pocket < Inventory.POCKETS; pocket++)
            font.draw(batch, bindings.keyName(pocket == 0 ? Action.POCKET_1 : Action.POCKET_2),
                slotX(Inventory.CAPACITY + pocket), pocketBottom + CELL + 17f);
        Inventory inventory = world.getInventory();
        for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
            String id = inventory.itemId(i);
            if (id == null) continue;
            float x = slotX(i), y = slotY(i);
            drawItem(id, x, y);
            if (inventory.count(i) > 1)
                font.draw(batch, Integer.toString(inventory.count(i)), x + 51f, y + 18f);
        }
        String item = inventory.itemId(source >= 0 ? source : selected);
        font.draw(batch, item == null ? "EMPTY" :
            ItemCatalog.SEALED_NOTE.equals(item) ? "SEALED NOTE" :
            ItemCatalog.BLADE_SHARD.equals(item) ? "BLADE SHARD: STRONGER STRIKES" :
            ItemCatalog.IMPACT_CORE.equals(item) ? "IMPACT CORE: MORE RECOIL" :
            ItemCatalog.FUSED_RELIC.equals(item) ? "FUSED RELIC: POWER + RECOIL" :
            ItemCatalog.RED_BRACER.equals(item) ? "RED BRACER: +25% BLOCK STABILITY" :
            ItemCatalog.WEIGHTED_BRACER.equals(item) ? "WEIGHTED BRACER: BLOCK COUNTER" :
            ItemCatalog.SMOKE_INCENSE.equals(item) ? "SMOKE INCENSE: 3 SEC COVER" :
            ItemCatalog.SEWING_KIT.equals(item) ? "SEWING KIT: 3 USES WITH BANDAGE" :
            ItemCatalog.REINFORCED_BANDAGE.equals(item) ? "REINFORCED BANDAGE: HEAL 2" :
            "BANDAGE x" + inventory.count(source >= 0 ? source : selected), left, bottom - 25f);
        if (craftFirst < 0 && !craftMessage.isEmpty())
            font.draw(batch, craftMessage, left, bottom - 113f);
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
            if (id != null) drawItem(id, x, bottom);
            font.setColor(.95f, .86f, .68f, 1f);
            if (inventory.count(slot) > 1)
                font.draw(batch, Integer.toString(inventory.count(slot)), x + 51f, bottom + 18f);
        }
        batch.end();
    }

    private void drawItem(String id, float x, float y) {
        if (ItemCatalog.BLADE_SHARD.equals(id)) batch.setColor(.65f, .85f, 1f, 1f);
        else if (ItemCatalog.IMPACT_CORE.equals(id)) batch.setColor(1f, .65f, .45f, 1f);
        else if (ItemCatalog.FUSED_RELIC.equals(id)) batch.setColor(.9f, .55f, 1f, 1f);
        else if (ItemCatalog.RED_BRACER.equals(id) || ItemCatalog.WEIGHTED_BRACER.equals(id))
            batch.setColor(1f, .35f, .3f, 1f);
        else if (ItemCatalog.SMOKE_INCENSE.equals(id)) batch.setColor(.65f, .72f, .73f, 1f);
        else if (ItemCatalog.SEWING_KIT.equals(id)) batch.setColor(.97f, .82f, .52f, 1f);
        else if (ItemCatalog.REINFORCED_BANDAGE.equals(id)) batch.setColor(.5f, 1f, .75f, 1f);
        batch.draw(ItemCatalog.FIELD_BANDAGE.equals(id) ? bandage : note,
            x + 9f, y + 9f, 48f, 48f);
        batch.setColor(Color.WHITE);
    }

    @Override public void dispose() { note.dispose(); bandage.dispose(); font.dispose(); batch.dispose(); shapes.dispose(); }
}
