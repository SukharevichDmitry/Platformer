package com.cursedarchie.platformer.screens;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cursedarchie.platformer.controllers.ControlBindings;
import com.cursedarchie.platformer.controllers.ControlBindings.Action;
import com.cursedarchie.platformer.levels.wrappers.InteractionData;
import com.cursedarchie.platformer.world.GameWorld;

/** Wide lower dialogue panel with an animated NPC figure and keyboard choices. */
final class DialogueOverlay implements Disposable {
    private static final float LETTERS_PER_SECOND = 32f;
    private static final Color WOOD = new Color(.29f, .16f, .09f, 1f);
    private static final Color WOOD_EDGE = new Color(.55f, .36f, .19f, 1f);
    private static final Color PARCHMENT = new Color(.88f, .78f, .58f, 1f);
    private static final Color INK = new Color(.18f, .12f, .08f, 1f);

    private final Stage stage = new Stage(new ScreenViewport());
    private final Skin skin = UiSkin.create();
    private final GameWorld world;
    private final ControlBindings bindings;
    private final Group root = new Group();
    private final Image frame;
    private final Image frameInset;
    private final Image textArea;
    private final Image npcBackdrop;
    private final Image npcFigure;
    private final Texture[] npcTextures = new Texture[3];
    private final TextureRegionDrawable[] npcDrawables = new TextureRegionDrawable[3];
    private final Label name;
    private final Label message;
    private final TextButton accept;
    private final TextButton refuse;
    private String fullText = "";
    private int visibleCharacters;
    private float letterProgress;
    private float animationTime;
    private float figureBaseY;
    private boolean open;
    private String npcId = "keeper";
    private int selected;

    DialogueOverlay(GameWorld world, ControlBindings bindings) {
        this.world = world;
        this.bindings = bindings;
        skin.add("dialogue", new Label.LabelStyle(skin.getFont("default-font"), INK));
        TextButton.TextButtonStyle choiceStyle = new TextButton.TextButtonStyle();
        choiceStyle.font = skin.getFont("default-font");
        choiceStyle.fontColor = new Color(.97f, .88f, .69f, 1f);
        choiceStyle.up = skin.newDrawable("white", new Color(.33f, .20f, .12f, 1f));
        choiceStyle.over = skin.newDrawable("white", new Color(.46f, .28f, .15f, 1f));
        choiceStyle.down = skin.newDrawable("white", new Color(.19f, .10f, .07f, 1f));
        skin.add("choice", choiceStyle);

        root.setVisible(false);
        frame = block(WOOD_EDGE);
        frameInset = block(WOOD);
        textArea = block(PARCHMENT);
        npcBackdrop = block(new Color(.12f, .16f, .19f, .95f));
        root.addActor(frame);
        root.addActor(frameInset);
        root.addActor(textArea);
        root.addActor(npcBackdrop);

        name = new Label("THE KEEPER", skin, "dialogue");
        message = new Label("", skin, "dialogue");
        message.setWrap(true);
        root.addActor(name);
        root.addActor(message);

        accept = choice("ACCEPT (-1 MAX HP)", true);
        refuse = choice("REFUSE", false);
        root.addActor(accept);
        root.addActor(refuse);

        for (int i = 0; i < npcTextures.length; i++) {
            npcTextures[i] = createFigure(i);
            npcDrawables[i] = new TextureRegionDrawable(new TextureRegion(npcTextures[i]));
        }
        npcFigure = new Image(npcDrawables[0]);
        root.addActor(npcFigure);
        stage.addActor(root);
    }

    private Image block(Color color) {
        return new Image(skin.newDrawable("white", color));
    }

    private TextButton choice(String title, boolean yes) {
        TextButton button = new TextButton(title, skin, "choice");
        button.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (textComplete()) choose(yes);
            }
        });
        return button;
    }

    Stage stage() { return stage; }
    boolean isOpen() { return open; }

    void open(InteractionData npc) {
        npcId = npc.id;
        open = true;
        root.setVisible(true);
        selected = 0;
        animationTime = 0f;
        name.setText("shelter_warden".equals(npcId) ? "THE WARDEN" : "THE KEEPER");
        npcFigure.setColor("shelter_warden".equals(npcId) ?
            new Color(.83f, .64f, .43f, 1f) : Color.WHITE);
        refresh();
    }

    boolean keyDown(int keycode) {
        if (!open) return false;
        if (bindings.matches(Action.PAUSE, keycode) || keycode == Keys.ESCAPE) close();
        else if (keycode == Keys.SPACE) revealAll();
        else if (textComplete() && accept.isVisible()) {
            if (bindings.matches(Action.UP, keycode)) selected = 0;
            else if (bindings.matches(Action.DOWN, keycode)) selected = 1;
            else if (keycode == Keys.ENTER || keycode == Keys.NUMPAD_ENTER) choose(selected == 0);
            refreshSelection();
        } else if (textComplete() && (keycode == Keys.ENTER || keycode == Keys.NUMPAD_ENTER)) close();
        return true;
    }

    private void close() {
        open = false;
        root.setVisible(false);
    }

    private boolean textComplete() { return visibleCharacters >= fullText.length(); }

    private void revealAll() {
        visibleCharacters = fullText.length();
        message.setText(fullText);
        refreshChoices();
    }

    private void setDialogue(String text) {
        fullText = text;
        visibleCharacters = 0;
        letterProgress = 0f;
        message.setText("");
        refreshChoices();
    }

    private void choose(boolean acceptOffer) {
        if (!textComplete() || !accept.isVisible()) return;
        if ("shelter_warden".equals(npcId)) {
            if (world.hasRunFlag(GameWorld.NEIGHBOR_LIST_FOUND))
                world.resolveNeighborList(acceptOffer);
            else if (acceptOffer) world.acceptNeighborSearch();
            else { close(); return; }
            refresh();
            return;
        }
        if (acceptOffer) world.acceptKeeperOffer();
        else world.refuseKeeperOffer();
        refresh();
    }

    private void refresh() {
        if ("shelter_warden".equals(npcId)) {
            if (world.hasRunFlag(GameWorld.NEIGHBOR_LIST_RETURNED))
                setDialogue("The families have their names. We will find a way to bring them home.");
            else if (world.hasRunFlag(GameWorld.NEIGHBOR_LIST_KEPT))
                setDialogue("Keep the list, then. Those names are people, not a bargaining chip.");
            else if (world.hasRunFlag(GameWorld.NEIGHBOR_LIST_FOUND))
                setDialogue("You found the list of the taken. Will you give their names to the families, or keep the evidence for your own search?");
            else if (world.hasRunFlag(GameWorld.NEIGHBOR_SEARCH))
                setDialogue("The raiders went down the open street. Look for the list they dropped before the patrol returns.");
            else
                setDialogue("People vanished in the raid. If you find the captors' list, bring their names back to us.");
        } else if (world.hasEchoDash()) {
            setDialogue("The echo is yours. Press " + bindings.keyName(Action.DASH) +
                " to dash. Guard the heart you kept.");
        } else if (world.hasRunFlag(GameWorld.KEEPER_REFUSAL)) {
            setDialogue("Then keep your heart. The ruins will remember your answer.");
        } else {
            setDialogue("I can bind the ruins' echo to your steps. Give me one heart, and you may dash through danger.");
        }
        refreshSelection();
    }

    private void refreshChoices() {
        boolean show = textComplete() && ("shelter_warden".equals(npcId) ?
            !world.hasRunFlag(GameWorld.NEIGHBOR_LIST_RETURNED) &&
                !world.hasRunFlag(GameWorld.NEIGHBOR_LIST_KEPT) &&
                (!world.hasRunFlag(GameWorld.NEIGHBOR_SEARCH) ||
                    world.hasRunFlag(GameWorld.NEIGHBOR_LIST_FOUND)) :
            !world.hasEchoDash() && !world.hasRunFlag(GameWorld.KEEPER_REFUSAL));
        accept.setVisible(show);
        refuse.setVisible(show);
    }

    private void refreshSelection() {
        String yes = "shelter_warden".equals(npcId) ?
            world.hasRunFlag(GameWorld.NEIGHBOR_LIST_FOUND) ? "GIVE THE NAMES" : "HELP SEARCH" :
            "ACCEPT (-1 MAX HP)";
        String no = "shelter_warden".equals(npcId) ?
            world.hasRunFlag(GameWorld.NEIGHBOR_LIST_FOUND) ? "KEEP THE LIST" : "LATER" : "REFUSE";
        accept.setText((selected == 0 ? "> " : "  ") + yes);
        refuse.setText((selected == 1 ? "> " : "  ") + no);
    }

    void render(float delta) {
        if (!open) return;
        float elapsed = Math.max(0f, delta);
        if (!textComplete()) {
            letterProgress += elapsed * LETTERS_PER_SECOND;
            int next = Math.min(fullText.length(), visibleCharacters + (int) letterProgress);
            if (next != visibleCharacters) {
                letterProgress -= next - visibleCharacters;
                visibleCharacters = next;
                message.setText(fullText.substring(0, visibleCharacters));
                refreshChoices();
            }
        }
        animationTime += elapsed;
        int frameIndex = ((int) (animationTime * 4f)) % 12;
        npcFigure.setDrawable(npcDrawables[frameIndex == 7 ? 1 : frameIndex == 3 ? 2 : 0]);
        npcFigure.setY(figureBaseY + (float) Math.sin(animationTime * 2.5f) * 3f);
        stage.act(delta);
        stage.draw();
    }

    void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        root.setSize(width, height);
        float margin = Math.max(12f, width * .028f);
        float panelX = margin;
        float panelY = Math.max(10f, height * .016f);
        float panelWidth = width - margin * 2f;
        float panelHeight = Math.min(300f, Math.max(190f, height * .235f));
        frame.setBounds(panelX, panelY, panelWidth, panelHeight);
        frameInset.setBounds(panelX + 5f, panelY + 5f, panelWidth - 10f, panelHeight - 10f);

        float npcWidth = Math.max(120f, panelWidth * .21f);
        float npcX = panelX + panelWidth - npcWidth - 14f;
        npcBackdrop.setBounds(npcX, panelY + 7f, npcWidth, panelHeight - 14f);
        float textX = panelX + Math.max(17f, panelWidth * .03f);
        float textY = panelY + 19f;
        float textWidth = npcX - textX - 18f;
        float textHeight = panelHeight - 38f;
        textArea.setBounds(textX, textY, textWidth, textHeight);

        float inset = Math.max(12f, textWidth * .025f);
        float fontScale = Math.min(1.65f, Math.max(.9f, width / 1000f));
        name.setFontScale(fontScale);
        message.setFontScale(fontScale);
        accept.getLabel().setFontScale(fontScale);
        refuse.getLabel().setFontScale(fontScale);
        name.setBounds(textX + inset, textY + textHeight - 35f, textWidth - inset * 2f, 28f);
        float buttonHeight = Math.max(29f, Math.min(39f, panelHeight * .16f));
        float buttonWidth = Math.min(430f, textWidth - inset * 2f);
        float firstButtonY = textY + inset + buttonHeight + 7f;
        accept.setBounds(textX + inset, firstButtonY, buttonWidth, buttonHeight);
        refuse.setBounds(textX + inset, textY + inset, buttonWidth, buttonHeight);
        float messageBottom = firstButtonY + buttonHeight + 6f;
        message.setBounds(textX + inset, messageBottom,
            textWidth - inset * 2f, Math.max(22f, textY + textHeight - 35f - messageBottom));

        float figureHeight = Math.min(height * .48f, 470f);
        float figureWidth = Math.min(npcWidth * .9f, figureHeight * .62f);
        figureBaseY = panelY + 8f;
        npcFigure.setBounds(npcX + (npcWidth - figureWidth) / 2f, figureBaseY,
            figureWidth, figureHeight);
    }

    private static Texture createFigure(int frame) {
        Pixmap pixels = new Pixmap(48, 80, Pixmap.Format.RGBA8888);
        pixels.setColor(.10f, .15f, .19f, 1f);
        pixels.fillRectangle(13, 8, 22, 68);
        pixels.setColor(.26f, .38f, .42f, 1f);
        pixels.fillRectangle(10, 28, 28, 45);
        pixels.fillRectangle(7, 37 + (frame == 2 ? 2 : 0), 8, 23);
        pixels.fillRectangle(33, 37 - (frame == 2 ? 2 : 0), 8, 23);
        pixels.setColor(.46f, .54f, .52f, 1f);
        pixels.fillRectangle(13, 35, 22, 34);
        pixels.setColor(.77f, .65f, .48f, 1f);
        pixels.fillRectangle(16, 14, 16, 19);
        pixels.setColor(.10f, .15f, .19f, 1f);
        pixels.fillRectangle(13, 8, 22, 10);
        pixels.setColor(.96f, .76f, .41f, 1f);
        if (frame != 1) {
            pixels.fillRectangle(19, 24, 3, 2);
            pixels.fillRectangle(27, 24, 3, 2);
        } else {
            pixels.drawLine(19, 25, 21, 25);
            pixels.drawLine(27, 25, 29, 25);
        }
        pixels.setColor(.65f, .53f, .33f, 1f);
        pixels.fillRectangle(22, 45, 4, 25);
        Texture texture = new Texture(pixels);
        pixels.dispose();
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    @Override public void dispose() {
        stage.dispose();
        skin.dispose();
        for (Texture texture : npcTextures) texture.dispose();
    }
}
