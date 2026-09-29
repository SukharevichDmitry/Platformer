package com.cursedarchie.platformer.screens;

import com.badlogic.gdx.Gdx;
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

public class MenuScreen implements Screen {
    private final Stage stage;
    private final Skin skin;

    public MenuScreen(Main game) {

        stage = new Stage(new ScreenViewport());
        skin = UiSkin.create();

        TextButton startButton = new TextButton("START", skin);
        TextButton exitButton = new TextButton("EXIT", skin);

        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.startGame();
            }
        });

        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.exitGame();
            }
        });

        Table table = new Table();
        table.setFillParent(true);
        table.center();

        table.add(startButton).width(200).height(50).pad(10);
        table.row();
        table.add(exitButton).width(200).height(50).pad(10);

        stage.addActor(table);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0.2f, 1); // темно-синий фон
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() { }
    @Override
    public void resume() { }
    @Override
    public void hide() { }
    @Override
    public void dispose() { stage.dispose(); skin.dispose(); }
}
