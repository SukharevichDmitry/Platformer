    package com.cursedarchie.platformer.screens;
    import com.badlogic.gdx.*;
    import com.badlogic.gdx.Input.Keys;
    import com.badlogic.gdx.graphics.GL20;
    import com.cursedarchie.platformer.Main;
    import com.cursedarchie.platformer.utils.WorldRenderer;
    import com.cursedarchie.platformer.world.GameWorld;

    public class GameScreen implements Screen {

        private final GameWorld gameWorld;
        private final WorldRenderer renderer;
        private final Main game;

        public GameScreen(Main game) {
            Gdx.app.log("GameScreen", "Initializing Game Screen");
            this.game = game;
            gameWorld = new GameWorld();
            renderer = new WorldRenderer(gameWorld, true);
        }

        @Override
        public void show() {
            InputMultiplexer multiplexer = new InputMultiplexer();
            multiplexer.addProcessor(new InputAdapter() {
                @Override
                public boolean keyDown(int keycode) {
                    if (keycode == Keys.ESCAPE) {
                        pause();
                        return true;
                    }
                    return false;
                }
            });
            multiplexer.addProcessor(gameWorld.getHeroController());

            Gdx.input.setInputProcessor(multiplexer);
        }

        @Override
        public void render(float delta) {
            Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            gameWorld.update(delta);
            renderer.render();

            if (!gameWorld.getHero().isAlive()) {
                Gdx.app.log("INFO", "HERO DEAD");
                dispose();
                game.goToMenu();
            }

        }

        @Override
        public void resize(int width, int height) {
            renderer.setSize(width, height);
        }

        @Override
        public void pause() {
            game.pauseGame(this);
        }

        @Override
        public void hide() {
        }
        @Override
        public void resume() {
        }
        @Override
        public void dispose() {}

    }
