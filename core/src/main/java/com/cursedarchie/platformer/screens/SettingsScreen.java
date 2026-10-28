package com.cursedarchie.platformer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cursedarchie.platformer.Main;
import com.cursedarchie.platformer.controllers.ControlBindings;
import com.cursedarchie.platformer.controllers.ControlBindings.Action;

import java.util.HashSet;
import java.util.Set;

/** Two settings tabs. Enter saves both drafts; Escape leaves without saving. */
public final class SettingsScreen implements Screen {
    private static final int[][] RESOLUTIONS = {
        {640, 480}, {800, 600}, {960, 540}, {1024, 768},
        {1280, 720}, {1440, 900}, {1600, 900}, {1920, 1080}
    };

    private final Stage stage = new Stage(new ScreenViewport());
    private final Skin skin = UiSkin.create();
    private final Main game;
    private final Screen returnTo;
    private final ControlBindings draft;
    private final TextButton[] keyButtons = new TextButton[Action.values().length];
    private final Table controlsTable = new Table();
    private final Table displayTable = new Table();
    private final Label status;
    private final Label resolutionLabel;
    private TextButton fullscreenButton;
    private int draftWidth;
    private int draftHeight;
    private boolean draftFullscreen;
    private Action waiting;

    public SettingsScreen(Main game, Screen returnTo) {
        this.game = game;
        this.returnTo = returnTo;
        draft = game.getControls().draft();
        int[] windowedResolution = game.getDisplaySettings().windowedResolution();
        draftWidth = windowedResolution[0];
        draftHeight = windowedResolution[1];
        draftFullscreen = Gdx.graphics.isFullscreen();
        skin.add("default", new Label.LabelStyle(skin.getFont("default-font"), Color.WHITE));
        status = new Label("Enter: save   Esc: back without saving", skin);
        resolutionLabel = new Label("", skin);

        buildControls();
        buildDisplay();
        controlsTable.setVisible(true);
        displayTable.setVisible(false);

        TextButton controlsTab = button("CONTROLS", () -> showTab(true));
        TextButton displayTab = button("DISPLAY", () -> showTab(false));
        TextButton save = button("SAVE (ENTER)", this::save);
        TextButton back = button("BACK (ESC)", () -> game.closeSettings(returnTo));
        Stack tabs = new Stack();
        tabs.add(controlsTable);
        tabs.add(displayTable);

        Table layout = new Table();
        layout.setFillParent(true);
        layout.center();
        layout.add(new Label("SETTINGS", skin)).colspan(2).padBottom(12f).row();
        layout.add(controlsTab).width(180f).height(38f).pad(4f);
        layout.add(displayTab).width(180f).height(38f).pad(4f).row();
        layout.add(tabs).colspan(2).width(520f).height(280f).pad(8f).row();
        layout.add(status).colspan(2).pad(8f).row();
        layout.add(save).width(180f).height(40f).pad(5f);
        layout.add(back).width(180f).height(40f).pad(5f);
        stage.addActor(layout);
    }

    private TextButton button(String title, Runnable action) {
        TextButton button = new TextButton(title, skin);
        button.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { action.run(); }
        });
        return button;
    }

    private void buildControls() {
        Action[] actions = Action.values();
        int rows = (actions.length + 1) / 2;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < 2; column++) {
                int index = row + column * rows;
                if (index >= actions.length) {
                    controlsTable.add();
                    controlsTable.add();
                    continue;
                }
                Action action = actions[index];
                TextButton keyButton = button(draft.keyName(action), () -> {
                    waiting = action;
                    status.setText("Press a key for " + action.label);
                    refreshKeys();
                });
                keyButtons[action.ordinal()] = keyButton;
                controlsTable.add(new Label(action.label, skin)).width(115f).left().pad(2f);
                controlsTable.add(keyButton).width(120f).height(32f).pad(2f);
            }
            controlsTable.row();
        }
        TextButton reset = button("RESET DEFAULTS", () -> {
            waiting = null;
            draft.resetToDefaults();
            refreshKeys();
            status.setText("Defaults selected. Enter to save");
        });
        controlsTable.add(reset).colspan(4).width(180f).height(36f).padTop(7f);
    }

    private void buildDisplay() {
        displayTable.add(new Label("WINDOW RESOLUTION", skin)).colspan(2).padBottom(8f).row();
        displayTable.add(resolutionLabel).colspan(2).padBottom(8f).row();
        Set<String> listed = new HashSet<>();
        int count = 0;
        for (int[] option : RESOLUTIONS) {
            if (!game.getDisplaySettings().supported(option[0], option[1])) continue;
            addResolution(option[0], option[1]);
            listed.add(option[0] + "x" + option[1]);
            if (++count % 2 == 0) displayTable.row();
        }
        String current = draftWidth + "x" + draftHeight;
        if (!listed.contains(current)) {
            addResolution(draftWidth, draftHeight);
            if (++count % 2 == 0) displayTable.row();
        }
        if (count % 2 != 0) displayTable.row();
        fullscreenButton = button("", () -> {
            draftFullscreen = !draftFullscreen;
            refreshResolution();
            status.setText("Display mode selected. Enter to save");
        });
        displayTable.add(fullscreenButton).colspan(2).width(220f).height(36f).padTop(5f).row();
        refreshResolution();
    }

    private void addResolution(int width, int height) {
        displayTable.add(button(width + " x " + height, () -> {
            draftWidth = width;
            draftHeight = height;
            refreshResolution();
            status.setText("Resolution selected. Enter to save");
        })).width(180f).height(36f).pad(3f);
    }

    private void showTab(boolean controls) {
        waiting = null;
        refreshKeys();
        controlsTable.setVisible(controls);
        displayTable.setVisible(!controls);
        status.setText(controls ? "Controls: Enter to save, Esc to back" :
            "Display: Enter to save, Esc to back");
    }

    private void refreshKeys() {
        for (Action action : Action.values()) keyButtons[action.ordinal()].setText(
            waiting == action ? "PRESS KEY..." : draft.keyName(action));
    }

    private void refreshResolution() {
        resolutionLabel.setText("Window size: " + draftWidth + " x " + draftHeight);
        fullscreenButton.setText("FULLSCREEN: " + (draftFullscreen ? "ON" : "OFF"));
    }

    private void save() {
        waiting = null;
        if (!game.getDisplaySettings().apply(draftWidth, draftHeight, draftFullscreen)) {
            status.setText("This display mode is unavailable");
            refreshKeys();
            return;
        }
        game.getControls().apply(draft);
        refreshKeys();
        status.setText("Settings saved");
    }

    @Override public void show() {
        Gdx.input.setInputProcessor(new InputMultiplexer(new InputAdapter() {
            @Override public boolean keyDown(int keycode) {
                if (keycode == Keys.ESCAPE) {
                    game.closeSettings(returnTo);
                    return true;
                }
                if (keycode == Keys.ENTER || keycode == Keys.NUMPAD_ENTER) {
                    save();
                    return true;
                }
                if (waiting == null) return false;
                Action action = waiting;
                if (draft.rebind(action, keycode)) {
                    waiting = null;
                    status.setText(action.label + " selected. Enter to save");
                } else status.setText("Key already assigned or unavailable");
                refreshKeys();
                return true;
            }
        }, stage));
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(.06f, .05f, .05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() { stage.dispose(); skin.dispose(); }
}
