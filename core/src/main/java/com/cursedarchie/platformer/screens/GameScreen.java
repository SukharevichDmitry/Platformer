package com.cursedarchie.platformer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.cursedarchie.platformer.Main;
import com.cursedarchie.platformer.utils.EnemyController;
import com.cursedarchie.platformer.utils.HeroController;
import com.cursedarchie.platformer.utils.WorldRenderer;
import com.cursedarchie.platformer.world.World;

public class GameScreen implements Screen, InputProcessor {

    private World world;
    private WorldRenderer renderer;
    private HeroController heroController;
    private EnemyController enemyController;
    private int width, height;


    private Main game;
    public GameScreen(Main game) {
        this.game = game;
    }


    @Override
    public void show() {
        world = new World();
        renderer = new WorldRenderer(world, true);
        heroController = new HeroController(world);
        enemyController = new EnemyController(world);
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        heroController.update(delta);
        enemyController.update(delta);
        renderer.render();


        if (!world.getHero().isAlive()) {
            Gdx.app.log("INFO", "HERO DEAD");
            dispose();
            game.showMenu();
        }
    }

    @Override
    public void resize(int width, int height) {
        renderer.setSize(width, height);
        this.width = width;
        this.height = height;
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {

    }




    @Override
    public boolean keyDown(int keycode) {
        if(keycode == Keys.LEFT) {
            heroController.leftPressed();
        }
        if (keycode == Keys.RIGHT) {
            heroController.rightPressed();
        }
        if (keycode == Keys.SPACE) {
            heroController.jumpPressed();
        }
        if (keycode == Keys.X) {
            heroController.attackPressed();
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Keys.LEFT) {
            heroController.leftReleased();
        }
        if (keycode == Keys.RIGHT) {
            heroController.rightReleased();
        }
        if (keycode == Keys.SPACE) {
            heroController.jumpReleased();
        }
        if (keycode == Keys.X) {
            heroController.attackReleased();
        }
        return true;
    }

    @Override
    public boolean keyTyped(char c) {
        return false;
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        if (x < width /2 && y > height /2) {
            heroController.leftPressed();
        }
        if (x > width /2 && y < height /2) {
            heroController.rightPressed();
        }
        return true;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        if (x < width /2 && y > height /2) {
            heroController.leftReleased();
        }
        if (x > width /2 && y < height /2) {
            heroController.rightReleased();
        }
        return false;
    }

    @Override
    public boolean touchCancelled(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchDragged(int i, int i1, int i2) {
        return false;
    }

    @Override
    public boolean mouseMoved(int i, int i1) {
        return false;
    }

    @Override
    public boolean scrolled(float v, float v1) {
        return false;
    }

}
