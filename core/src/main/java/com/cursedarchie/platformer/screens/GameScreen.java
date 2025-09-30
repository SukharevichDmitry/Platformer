    package com.cursedarchie.platformer.screens;

    import com.badlogic.gdx.*;
    import com.badlogic.gdx.Input.Keys;
    import com.badlogic.gdx.graphics.GL20;

    import com.cursedarchie.platformer.Main;
    import com.cursedarchie.platformer.utils.EnemyController;
    import com.cursedarchie.platformer.utils.HeroController;
    import com.cursedarchie.platformer.utils.WorldRenderer;
    import com.cursedarchie.platformer.world.World;

    public class GameScreen implements Screen {

        private World world;
        private WorldRenderer renderer;
        private HeroController heroController;
        private EnemyController enemyController;


        private Main game;
        public GameScreen(Main game) {
            this.game = game;
            world = new World();
            renderer = new WorldRenderer(world, true);
            heroController = new HeroController(world);
            enemyController = new EnemyController(world);
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
            multiplexer.addProcessor(heroController);

            Gdx.input.setInputProcessor(multiplexer);
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
        }

        @Override
        public void pause() {
            game.setScreen(new PauseScreen(game, this));
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
