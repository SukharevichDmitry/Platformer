    package com.cursedarchie.platformer.screens;
    import com.badlogic.gdx.*;
    import com.cursedarchie.platformer.controllers.ControlBindings.Action;
    import com.badlogic.gdx.graphics.GL20;
    import com.cursedarchie.platformer.Main;
    import com.cursedarchie.platformer.world.WorldRenderer;
    import com.cursedarchie.platformer.world.GameWorld;
    import com.cursedarchie.platformer.world.GenerationSeedOverlay;
    import com.cursedarchie.platformer.world.MiniMapRenderer;
    import com.cursedarchie.platformer.world.LocationTitleOverlay;
    import com.cursedarchie.platformer.world.FpsOverlay;
    import com.cursedarchie.platformer.levels.wrappers.InteractionData;

    public class GameScreen implements Screen {

        private final GameWorld gameWorld;
        private final WorldRenderer renderer;
        private final GenerationSeedOverlay seedOverlay;
        private final MiniMapRenderer miniMap;
        private final LocationTitleOverlay locationTitle;
        private final FpsOverlay fpsOverlay;
        private final BagOverlay bagOverlay;
        private final DialogueOverlay dialogue;
        private boolean bagOpen;
        private boolean npcInteractHeld;
        private final boolean[] pocketTransferHeld = new boolean[2];
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
        private float autosaveSeconds;
        private long lastSavedRevision;
        private boolean deathSaveCleared;
        private boolean disposed;

        public GameScreen(Main game, GameWorld gameWorld) {
            Gdx.app.log("GameScreen", "Initializing Game Screen");
            this.game = game;
            this.gameWorld = gameWorld;
            lastSavedRevision = gameWorld.getPersistenceRevision();
            renderer = new WorldRenderer(gameWorld, false);
            seedOverlay = new GenerationSeedOverlay(gameWorld);
            miniMap = new MiniMapRenderer(gameWorld);
            locationTitle = new LocationTitleOverlay(gameWorld);
            fpsOverlay = new FpsOverlay();
            bagOverlay = new BagOverlay();
            dialogue = new DialogueOverlay(gameWorld, game.getControls());
            initializationMs = (System.nanoTime() - constructionStartedNs) / 1_000_000f;
        }

        @Override
        public void show() {
            gameWorld.getHeroController().setBindings(game.getControls());
            InputMultiplexer multiplexer = new InputMultiplexer();
            multiplexer.addProcessor(new InputAdapter() {
                @Override
                public boolean keyDown(int keycode) {
                    if (dialogue.isOpen()) return dialogue.keyDown(keycode);
                    if (!bagOpen && game.getControls().matches(Action.INTERACT, keycode)) {
                        if (npcInteractHeld) return true;
                        InteractionData nearby = gameWorld.getNearbyInteraction();
                        if (nearby != null && "npc".equals(nearby.kind)) {
                            npcInteractHeld = true;
                            dialogue.open(nearby);
                            gameWorld.getHeroController().clearInput();
                            return true;
                        }
                    }
                    if (game.getControls().matches(Action.BAG, keycode) ||
                        (bagOpen && game.getControls().matches(Action.PAUSE, keycode))) {
                        bagOpen = !bagOpen;
                        pocketTransferHeld[0] = false;
                        pocketTransferHeld[1] = false;
                        gameWorld.getHeroController().clearInput();
                        return true;
                    }
                    if (bagOpen) {
                        if (keycode == Input.Keys.TAB) {
                            bagOverlay.togglePage();
                            return true;
                        }
                        if (bagOverlay.isAbilitiesPage()) return true;
                        if (game.getControls().matches(Action.LEFT, keycode)) bagOverlay.move(-1, 0);
                        else if (game.getControls().matches(Action.RIGHT, keycode)) bagOverlay.move(1, 0);
                        else if (game.getControls().matches(Action.UP, keycode)) bagOverlay.move(0, -1);
                        else if (game.getControls().matches(Action.DOWN, keycode)) bagOverlay.move(0, 1);
                        else if (game.getControls().matches(Action.POCKET_1, keycode)) {
                            if (!pocketTransferHeld[0]) gameWorld.moveToPocket(bagOverlay.selectedSlot(), 0);
                            pocketTransferHeld[0] = true;
                        } else if (game.getControls().matches(Action.POCKET_2, keycode)) {
                            if (!pocketTransferHeld[1]) gameWorld.moveToPocket(bagOverlay.selectedSlot(), 1);
                            pocketTransferHeld[1] = true;
                        }
                        return true;
                    }
                    if (game.getControls().matches(Action.PAUSE, keycode)) {
                        pause();
                        return true;
                    }
                    return false;
                }
                @Override public boolean keyUp(int keycode) {
                    if (game.getControls().matches(Action.INTERACT, keycode)) npcInteractHeld = false;
                    if (dialogue.isOpen()) return true;
                    if (game.getControls().matches(Action.POCKET_1, keycode)) pocketTransferHeld[0] = false;
                    if (game.getControls().matches(Action.POCKET_2, keycode)) pocketTransferHeld[1] = false;
                    return bagOpen;
                }
            });
            multiplexer.addProcessor(dialogue.stage());
            multiplexer.addProcessor(gameWorld.getHeroController());

            Gdx.input.setInputProcessor(multiplexer);
        }

        @Override
        public void render(float delta) {
            Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            long updateStartedNs = System.nanoTime();
            if (!bagOpen && !dialogue.isOpen()) gameWorld.update(delta);
            autosaveSeconds += Math.max(0f, delta);
            if (!gameWorld.getHero().isAlive()) {
                if (!deathSaveCleared) {
                    game.autosaveGame();
                    deathSaveCleared = true;
                }
            } else if (gameWorld.getPersistenceRevision() != lastSavedRevision || autosaveSeconds >= 5f) {
                game.autosaveGame();
                lastSavedRevision = gameWorld.getPersistenceRevision();
                autosaveSeconds = 0f;
            }
            locationTitle.update(delta);
            float updateMs = (System.nanoTime() - updateStartedNs) / 1_000_000f;
            long renderStartedNs = System.nanoTime();
            renderer.render(delta);
            miniMap.render();
            seedOverlay.render();
            locationTitle.render();
            fpsOverlay.render();
            if (!bagOpen) bagOverlay.renderPockets(gameWorld);
            if (bagOpen) bagOverlay.render(gameWorld, game.getControls().keyName(Action.DASH));
            dialogue.render(delta);
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
            bagOverlay.resize(width, height);
            dialogue.resize(width, height);
        }

        @Override
        public void pause() {
            gameWorld.getHeroController().clearInput();
            game.pauseGame(this);
        }

        @Override
        public void hide() {
            gameWorld.getHeroController().clearInput();
            npcInteractHeld = false;
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
            bagOverlay.dispose();
            dialogue.dispose();
            gameWorld.dispose();
        }

        public boolean isDisposed() { return disposed; }
        public GameWorld getGameWorld() { return gameWorld; }

    }
