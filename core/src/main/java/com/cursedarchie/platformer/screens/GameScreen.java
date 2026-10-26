    package com.cursedarchie.platformer.screens;
    import com.badlogic.gdx.*;
    import com.badlogic.gdx.Input.Keys;
    import com.badlogic.gdx.graphics.GL20;
    import com.cursedarchie.platformer.Main;
    import com.cursedarchie.platformer.world.WorldRenderer;
    import com.cursedarchie.platformer.world.GameWorld;
    import com.cursedarchie.platformer.world.GenerationSeedOverlay;
    import com.cursedarchie.platformer.world.MiniMapRenderer;
    import com.cursedarchie.platformer.world.LocationTitleOverlay;
    import com.cursedarchie.platformer.world.FpsOverlay;

    public class GameScreen implements Screen {

        private final GameWorld gameWorld;
        private final WorldRenderer renderer;
        private final GenerationSeedOverlay seedOverlay;
        private final MiniMapRenderer miniMap;
        private final LocationTitleOverlay locationTitle;
        private final FpsOverlay fpsOverlay;
        private final Main game;
        private final long constructionStartedNs = System.nanoTime();
        private final float initializationMs;
        private float startupSeconds;
        private float worstFrameMs;
        private float worstUpdateMs;
        private float worstRenderMs;
        private int slowStartupFrames;
        private boolean startupTimingReported;
        private float deathFeedbackSeconds;
        private boolean disposed;

        public GameScreen(Main game) {
            Gdx.app.log("GameScreen", "Initializing Game Screen");
            this.game = game;
            gameWorld = new GameWorld();
            renderer = new WorldRenderer(gameWorld, false);
            seedOverlay = new GenerationSeedOverlay(gameWorld);
            miniMap = new MiniMapRenderer(gameWorld);
            locationTitle = new LocationTitleOverlay(gameWorld);
            fpsOverlay = new FpsOverlay();
            initializationMs = (System.nanoTime() - constructionStartedNs) / 1_000_000f;
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

            long updateStartedNs = System.nanoTime();
            gameWorld.update(delta);
            locationTitle.update(delta);
            float updateMs = (System.nanoTime() - updateStartedNs) / 1_000_000f;
            long renderStartedNs = System.nanoTime();
            renderer.render(delta);
            miniMap.render();
            seedOverlay.render();
            locationTitle.render();
            fpsOverlay.render();
            float renderMs = (System.nanoTime() - renderStartedNs) / 1_000_000f;
            recordStartupTiming(delta, updateMs, renderMs);

            if (!gameWorld.getHero().isAlive()) {
                deathFeedbackSeconds += Math.max(0f, delta);
                if (deathFeedbackSeconds >= 0.25f) {
                    Gdx.app.log("INFO", "HERO DEAD");
                    game.goToMenu();
                }
            } else {
                deathFeedbackSeconds = 0f;
            }

        }

        private void recordStartupTiming(float delta, float updateMs, float renderMs) {
            if (startupTimingReported) return;
            startupSeconds += delta;
            worstFrameMs = Math.max(worstFrameMs, delta * 1000f);
            worstUpdateMs = Math.max(worstUpdateMs, updateMs);
            worstRenderMs = Math.max(worstRenderMs, renderMs);
            if (delta > 0.05f) slowStartupFrames++;
            if (startupSeconds >= 10f) {
                startupTimingReported = true;
                Gdx.app.log("STARTUP TIMING", "init=" + initializationMs + "ms, worst frame=" +
                    worstFrameMs + "ms, worst update=" + worstUpdateMs + "ms, worst render=" +
                    worstRenderMs + "ms, frames over 50ms=" + slowStartupFrames);
            }
        }

        @Override
        public void resize(int width, int height) {
            renderer.setSize(width, height);
            seedOverlay.resize(width, height);
            miniMap.resize(width, height);
            locationTitle.resize(width, height);
            fpsOverlay.resize(width, height);
        }

        @Override
        public void pause() {
            gameWorld.getHeroController().clearInput();
            game.pauseGame(this);
        }

        @Override
        public void hide() {
            gameWorld.getHeroController().clearInput();
        }
        @Override
        public void resume() {
        }
        @Override
        public void dispose() {
            if (disposed) return;
            disposed = true;
            renderer.dispose();
            seedOverlay.dispose();
            miniMap.dispose();
            locationTitle.dispose();
            fpsOverlay.dispose();
            gameWorld.dispose();
        }

        public boolean isDisposed() { return disposed; }
        public GameWorld getGameWorld() { return gameWorld; }

    }
