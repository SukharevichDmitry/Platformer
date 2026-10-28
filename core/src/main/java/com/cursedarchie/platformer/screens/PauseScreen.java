package com.cursedarchie.platformer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cursedarchie.platformer.Main;
import com.cursedarchie.platformer.controllers.ControlBindings.Action;

public class PauseScreen implements Screen {
    private final Stage stage;
    private final Skin skin;
    private final Main game;
    private final Screen previousScreen;

    public PauseScreen(Main game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;
        stage = new Stage(new ScreenViewport());
        skin = UiSkin.create();

        TextButton continueButton = new TextButton("Continue", skin);
        TextButton exitButton = new TextButton("To Menu", skin);
        TextButton settingsButton = new TextButton("Settings", skin);

        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.continueGame(previousScreen);
            }
        });

        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.goToMenu();
            }
        });
        settingsButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.openSettings(PauseScreen.this);
            }
        });

        Table table = new Table();
        table.setFillParent(true);
        table.center();

        table.add(continueButton).width(200).height(50).pad(10);
        table.row();
        table.add(settingsButton).width(200).height(50).pad(10);
        table.row();
        table.add(exitButton).width(200).height(50).pad(10);

        stage.addActor(table);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputMultiplexer(new InputAdapter() {
            @Override public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE || game.getControls().matches(Action.PAUSE, keycode)) {
                    game.continueGame(previousScreen);
                    return true;
                }
                return false;
            }
        }, stage));
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0.2f, 1);
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
