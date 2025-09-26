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

    private Main game;
    private Stage stage;
    private Skin skin;

    public MenuScreen(Main game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());

        // Загрузим стандартный skin из LibGDX (у тебя должен быть skin файл в assets)
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        // Создаем кнопки
        TextButton startButton = new TextButton("START", skin);
        TextButton exitButton = new TextButton("EXIT", skin);

        // Добавляем слушатели на кнопки
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen(game));
                dispose();
            }
        });

        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        // Используем Table для удобного расположения кнопок по центру
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        // Добавляем кнопки с отступами
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
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
