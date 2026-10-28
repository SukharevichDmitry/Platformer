package com.cursedarchie.platformer.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.cursedarchie.platformer.Main;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.screens.GameScreen;
import com.cursedarchie.platformer.screens.MenuScreen;
import com.cursedarchie.platformer.screens.PauseScreen;
import com.cursedarchie.platformer.screens.SettingsScreen;
import com.cursedarchie.platformer.controllers.ControlBindings;
import com.cursedarchie.platformer.controllers.ControlBindings.Action;
import com.cursedarchie.platformer.world.GameWorld;
import com.cursedarchie.platformer.run.RunCheckpoint;
import com.cursedarchie.platformer.run.RunCheckpointStore;

/** Integration check using real GL, native libraries, packaged assets and UI input. */
public final class PackagedSmokeTest extends Main {
    private int frames;
    private Screen firstGame;
    private GameScreen deathGame;
    private boolean complete;
    private int menuTextures;
    private int gameplayTextures;
    private RunCheckpoint savedRun;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setInitialVisible(false);
        config.setWindowedMode(640, 480);
        config.setForegroundFPS(60);
        PackagedSmokeTest test = new PackagedSmokeTest();
        new Lwjgl3Application(test, config);
        if (!test.complete) throw new AssertionError("Smoke test ended before completion");
        System.out.println("PACKAGED SMOKE PASSED: repeated start/ruins border travel, menu, pause, restart, death, exit");
    }

    @Override public void render() {
        super.render();
        if (++frames % 20 != 0) return;
        switch (frames / 20) {
            case 1:
                expect(MenuScreen.class);
                menuTextures = Texture.getNumManagedTextures();
                getControls().resetToDefaults();
                click("SETTINGS");
                expect(SettingsScreen.class);
                getScreen().render(0f);
                click("Left");
                Gdx.input.getInputProcessor().keyDown(Input.Keys.A);
                if (getControls().key(Action.LEFT) != Input.Keys.LEFT)
                    throw new AssertionError("Draft control was applied before Enter");
                Gdx.input.getInputProcessor().keyDown(Input.Keys.ENTER);
                int liveKey = getControls().key(Action.LEFT);
                int restoredKey = new ControlBindings(
                    Gdx.app.getPreferences(getClass().getName() + ".settings")).key(Action.LEFT);
                if (liveKey != Input.Keys.A || restoredKey != Input.Keys.A)
                    throw new AssertionError("Remapped key was not saved: live=" + liveKey +
                        ", restored=" + restoredKey + ", wanted=" + Input.Keys.A);
                if (getControls().rebind(Action.LEFT, Input.Keys.RIGHT) ||
                    getControls().key(Action.LEFT) != Input.Keys.A)
                    throw new AssertionError("Duplicate control binding was accepted");
                if (getDisplaySettings().supported(800, 600)) {
                    click("DISPLAY");
                    getScreen().render(0f);
                    click("800 x 600");
                    click("FULLSCREEN: OFF");
                    Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
                    expect(MenuScreen.class);
                    if (Gdx.graphics.getWidth() != 640 || Gdx.graphics.getHeight() != 480 ||
                        Gdx.graphics.isFullscreen())
                        throw new AssertionError("Escape applied unsaved display settings");
                    click("SETTINGS");
                    getScreen().render(0f);
                    click("DISPLAY");
                    getScreen().render(0f);
                    click("800 x 600");
                    Gdx.input.getInputProcessor().keyDown(Input.Keys.ENTER);
                    if (Gdx.app.getPreferences(getClass().getName() + ".settings")
                        .getInteger("display.width") != 800)
                        throw new AssertionError("Enter did not persist the resolution");
                    click("FULLSCREEN: OFF");
                    Gdx.input.getInputProcessor().keyDown(Input.Keys.ENTER);
                    if (!Gdx.graphics.isFullscreen() || !Gdx.app.getPreferences(
                        getClass().getName() + ".settings").getBoolean("display.fullscreen"))
                        throw new AssertionError("Enter did not persist fullscreen mode");
                }
                Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
                expect(MenuScreen.class);
                if (!getDisplaySettings().apply(640, 480))
                    throw new AssertionError("Could not restore smoke-test window size");
                getControls().resetToDefaults();
                if (new ControlBindings(Gdx.app.getPreferences(getClass().getName() + ".settings"))
                    .key(Action.LEFT) != Input.Keys.LEFT)
                    throw new AssertionError("Reset controls was not persisted");
                click("START");
                expect(GameScreen.class);
                if (!canContinueRun()) throw new AssertionError("START did not create an immediate checkpoint");
                firstGame = getScreen();
                break;
            case 2:
                // The previous menu is disposed asynchronously after START; baseline
                // only once that disposal and the first gameplay render have completed.
                assertOpeningItemVisible();
                if (System.getenv("PLATFORMER_CAPTURE_ROAD") != null) captureRoadFrame();
                gameplayTextures = Texture.getNumManagedTextures();
                exerciseTransitions(((GameScreen) getScreen()).getGameWorld());
                exerciseBorderTravel(((GameScreen) getScreen()).getGameWorld());
                exerciseGroundSeam(((GameScreen) getScreen()).getGameWorld());
                exerciseNeighborQuest(((GameScreen) getScreen()).getGameWorld());
                exerciseKeeperDialogue(((GameScreen) getScreen()).getGameWorld());
                GameWorld autosaveWorld = ((GameScreen) getScreen()).getGameWorld();
                autosaveWorld.getHero().setHealth(2.5f);
                ((GameScreen) getScreen()).render(5.1f);
                RunCheckpoint automatic = new RunCheckpointStore(
                    Gdx.app.getPreferences(getClass().getName() + ".run")).load();
                if (automatic == null ||
                    Math.abs(automatic.heroHealth - autosaveWorld.getHero().getHealth()) > 0.001f)
                    throw new AssertionError("Periodic autosave did not capture live hero state");
                if (Texture.getNumManagedTextures() != gameplayTextures) throw new AssertionError("Gameplay texture count grew");
                Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
                expect(PauseScreen.class);
                break;
            case 3:
                Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
                expect(GameScreen.class);
                if (getScreen() != firstGame) throw new AssertionError("Continue replaced the game");
                break;
            case 4:
                ((GameScreen) firstGame).getGameWorld().getHero().setHealth(2f);
                savedRun = ((GameScreen) firstGame).getGameWorld().checkpoint();
                Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
                expect(PauseScreen.class);
                getScreen().render(0f);
                click("Settings");
                expect(SettingsScreen.class);
                Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
                expect(PauseScreen.class);
                break;
            case 5:
                click("To Menu");
                expect(MenuScreen.class);
                break;
            case 6:
                if (!((GameScreen) firstGame).isDisposed()) throw new AssertionError("Old game was not disposed");
                if (Texture.getNumManagedTextures() != menuTextures) throw new AssertionError("Menu texture count grew");
                if (!canContinueRun()) throw new AssertionError("Paused run cannot Continue");
                click("CONTINUE RUN");
                expect(GameScreen.class);
                if (getScreen() == firstGame) throw new AssertionError("Continue reused disposed game");
                GameWorld continued = ((GameScreen) getScreen()).getGameWorld();
                if (continued.checkpoint().seed != savedRun.seed ||
                    continued.getHero().getHealth() != 2f ||
                    continued.getCurrentRoom().getRoomId() != savedRun.roomId ||
                    Math.abs(continued.getHero().getPosition().x - savedRun.heroX) > 0.01f ||
                    Math.abs(continued.getHero().getPosition().y - savedRun.heroY) > 0.01f)
                    throw new AssertionError("Continue changed saved hero or room state");
                break;
            case 7:
                if (Texture.getNumManagedTextures() != gameplayTextures) throw new AssertionError("Restart texture count grew");
                Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
                expect(PauseScreen.class);
                break;
            case 8:
                click("To Menu");
                expect(MenuScreen.class);
                break;
            case 9:
                click("START");
                expect(GameScreen.class);
                deathGame = (GameScreen) getScreen();
                deathGame.getGameWorld().getHero().takeDamage(10000);
                break;
            case 10:
                expect(MenuScreen.class);
                if (canContinueRun()) throw new AssertionError("Death left a Continue run");
                if (!deathGame.isDisposed()) throw new AssertionError("Dead game was not disposed");
                if (Texture.getNumManagedTextures() != menuTextures) throw new AssertionError("Death leaked textures");
                click("EXIT");
                complete = true;
                break;
            default: throw new AssertionError("Smoke test timed out");
        }
    }

    /** Optional visual artifact for checking road scale and joins in a real GL frame. */
    private void captureRoadFrame() {
        Pixmap frame = Pixmap.createFromFrameBuffer(0, 0,
            Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            PixmapIO.PNG writer = new PixmapIO.PNG();
            try {
                writer.setFlipY(true);
                writer.write(Gdx.files.local("road-visual-check.png"), frame);
            } catch (java.io.IOException ex) {
                throw new RuntimeException("Could not capture road frame", ex);
            } finally {
                writer.dispose();
            }
        } finally {
            frame.dispose();
        }
    }

    /** The packaged asset and world renderer must place an actual sprite in the opening view. */
    private void assertOpeningItemVisible() {
        GameWorld world = ((GameScreen) getScreen()).getGameWorld();
        if (world.getNearbyInteraction() == null ||
            !"first_bandage".equals(world.getNearbyInteraction().id))
            throw new AssertionError("Opening bandage is missing from gameplay state");
        Pixmap frame = Pixmap.createFromFrameBuffer(0, 0,
            Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            int brightItemPixels = 0;
            for (int y = 80; y < 340; y++) {
                for (int x = 330; x < 375; x++) {
                    int rgba = frame.getPixel(x, y);
                    int red = (rgba >>> 24) & 255;
                    int green = (rgba >>> 16) & 255;
                    int blue = (rgba >>> 8) & 255;
                    if (red > 190 && green > 155 && blue > 100) brightItemPixels++;
                }
            }
            if (brightItemPixels < 30)
                throw new AssertionError("Opening bandage sprite was not drawn");
            int labelPixels = 0;
            for (int y = 80; y < 340; y++) {
                for (int x = 305; x < 400; x++) {
                    int rgba = frame.getPixel(x, y);
                    int red = (rgba >>> 24) & 255;
                    int green = (rgba >>> 16) & 255;
                    int blue = (rgba >>> 8) & 255;
                    if (red > 140 && green > 120 && blue > 80) labelPixels++;
                }
            }
            if (labelPixels < 80)
                throw new AssertionError("Opening interaction label was not drawn");
        } finally {
            frame.dispose();
        }
    }

    private void exerciseTransitions(GameWorld world) {
        int startBodies = world.getWorld().getBodyCount();
        int startEnemies = world.getEnemies().size;
        int ruinsBodies = -1;
        int ruinsEnemies = -1;
        for (int i = 0; i < 20; i++) {
            world.changeLocation("ruins", 0);
            if (ruinsBodies < 0) {
                ruinsBodies = world.getWorld().getBodyCount();
                ruinsEnemies = world.getEnemies().size;
            }
            assertLocation(world, "ruins", ruinsBodies, ruinsEnemies);
            world.changeLocation("start", 0);
            assertLocation(world, "start", startBodies, startEnemies);
        }
        try {
            world.changeLocation("ruins", 99);
            throw new AssertionError("Missing room was accepted");
        } catch (IllegalArgumentException expected) {
            assertLocation(world, "start", startBodies, startEnemies);
        }
    }

    private void assertLocation(GameWorld world, String name, int bodies, int enemies) {
        if (!name.equals(world.getLocation().getLocation()) ||
            world.getWorld().getBodyCount() != bodies || world.getEnemies().size != enemies ||
            bodies != 2 + enemies) {
            throw new AssertionError("Location resources did not return to baseline: " + name);
        }
    }

    private void exerciseBorderTravel(GameWorld world) {
        world.changeLocation("ruins", 0);
        int ruinsBodies = world.getWorld().getBodyCount();
        int ruinsEnemies = world.getEnemies().size;
        for (int i = 0; i < 20; i++) {
            world.changeLocation("start", 3);
            float right = world.getCurrentRoom().getWorldX() + world.getCurrentRoom().getWidth();
            float y = world.getCurrentRoom().getWorldY() + 4;
            world.getHero().getBody().setTransform(right - 0.2f, y, 0);
            world.update(1f / 60f);
            assertLocation(world, "ruins", ruinsBodies, ruinsEnemies);

            float left = world.getCurrentRoom().getWorldX();
            y = world.getCurrentRoom().getWorldY() + 4;
            world.getHero().getBody().setTransform(left + 0.2f, y, 0);
            world.update(1f / 60f);
            assertLocation(world, "start", 2, 0);
            if (world.getCurrentRoom().getRoomId() != 3 ||
                world.getHero().getBody().getPosition().x >=
                    world.getCurrentRoom().getWorldX() + world.getCurrentRoom().getWidth() - 0.4f) {
                throw new AssertionError("Return spawn is in the wrong room or retriggers the ruins exit");
            }
            world.update(1f / 60f);
            assertLocation(world, "start", 2, 0);
        }
    }

    private void exerciseGroundSeam(GameWorld world) {
        world.changeLocation("start", 0);
        for (Enemy enemy : world.getEnemies()) enemy.setAlive(false);
        world.update(1f / 60f);

        for (int roomId = 1; roomId <= 3; roomId++) {
            float seam = world.getLocation().getRoom(roomId).getWorldX();
            int[] floorFixtures = {0};
            world.getWorld().QueryAABB(fixture -> {
                if ("level".equals(fixture.getUserData())) floorFixtures[0]++;
                return true;
            }, seam - 0.02f, 1f, seam + 0.02f, 1f);
            if (floorFixtures[0] != 1) {
                throw new AssertionError("Room seam " + roomId + " has " + floorFixtures[0] + " separate floor fixtures");
            }
            for (float startY : new float[]{2.5f, 2.52f, 2.6f}) {
                world.getHero().getBody().setTransform(seam - 1.4f, startY, 0);
                world.getHero().getBody().setLinearVelocity(0, 0);
                for (int i = 0; i < 30; i++) world.update(1f / 60f);
                world.getHeroController().keyDown(Input.Keys.RIGHT);
                for (int i = 0; i < 30; i++) world.update(1f / 60f);
                world.getHeroController().keyUp(Input.Keys.RIGHT);
                if (world.getHero().getPosition().x < seam + 1f) {
                    throw new AssertionError("Hero stuck crossing room seam " + roomId + " to the right at x=" + world.getHero().getPosition().x);
                }

                world.getHeroController().keyDown(Input.Keys.LEFT);
                for (int i = 0; i < 36; i++) world.update(1f / 60f);
                world.getHeroController().keyUp(Input.Keys.LEFT);
                if (world.getHero().getPosition().x > seam - 1f) {
                    throw new AssertionError("Hero stuck crossing room seam " + roomId + " to the left at x=" + world.getHero().getPosition().x);
                }
            }
        }
    }

    private void exerciseKeeperDialogue(GameWorld world) {
        world.changeLocation("keeper_chamber", 0);
        world.getHero().getBody().setTransform(12f, 3f, 0f);
        getScreen().render(0f);
        Gdx.input.getInputProcessor().keyDown(Input.Keys.E);
        Gdx.input.getInputProcessor().keyUp(Input.Keys.E);
        getScreen().render(0f);
        Gdx.input.getInputProcessor().keyDown(Input.Keys.ENTER);
        if (world.hasRunFlag(GameWorld.KEEPER_REFUSAL))
            throw new AssertionError("Choice was applied before dialogue text completed");
        Gdx.input.getInputProcessor().keyDown(Input.Keys.SPACE);
        Gdx.input.getInputProcessor().keyDown(Input.Keys.DOWN);
        Gdx.input.getInputProcessor().keyDown(Input.Keys.ENTER);
        if (!world.hasRunFlag(GameWorld.KEEPER_REFUSAL) || world.hasEchoDash())
            throw new AssertionError("Keyboard dialogue refusal was not applied");
        Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
        expect(GameScreen.class);
        world.changeLocation("start", 0);
    }

    private void exerciseNeighborQuest(GameWorld world) {
        world.changeLocation("start", 1);
        world.getHero().getBody().setTransform(world.getCurrentRoom().getWorldX() + 12f, 3f, 0f);
        getScreen().render(0f);
        Gdx.input.getInputProcessor().keyDown(Input.Keys.E);
        Gdx.input.getInputProcessor().keyUp(Input.Keys.E);
        Gdx.input.getInputProcessor().keyDown(Input.Keys.SPACE);
        Gdx.input.getInputProcessor().keyDown(Input.Keys.ENTER);
        if (!world.hasRunFlag(GameWorld.NEIGHBOR_SEARCH))
            throw new AssertionError("Warden quest was not accepted");
        Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
        world.getHero().getBody().setTransform(world.getLocation().getRoom(3).getWorldX() + 44f, 3f, 0f);
        world.update(GameWorld.FIXED_STEP);
        if (!world.interact() || !world.hasRunFlag(GameWorld.NEIGHBOR_LIST_FOUND))
            throw new AssertionError("Missing-neighbors list was not found");
        world.getHero().getBody().setTransform(world.getLocation().getRoom(1).getWorldX() + 12f, 3f, 0f);
        world.update(GameWorld.FIXED_STEP);
        Gdx.input.getInputProcessor().keyDown(Input.Keys.E);
        Gdx.input.getInputProcessor().keyUp(Input.Keys.E);
        Gdx.input.getInputProcessor().keyDown(Input.Keys.SPACE);
        Gdx.input.getInputProcessor().keyDown(Input.Keys.ENTER);
        if (!world.hasRunFlag(GameWorld.NEIGHBOR_LIST_RETURNED) ||
            world.hasRunFlag(GameWorld.NEIGHBOR_LIST_KEPT))
            throw new AssertionError("Warden quest choice was not applied once");
        Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
        world.changeLocation("start", 0);
    }

    private void expect(Class<? extends Screen> type) {
        if (!type.isInstance(getScreen())) throw new AssertionError("Expected " + type.getSimpleName());
    }

    private void click(String label) {
        InputProcessor processor = Gdx.input.getInputProcessor();
        Stage stage = null;
        if (processor instanceof Stage) stage = (Stage) processor;
        else if (processor instanceof InputMultiplexer) {
            for (InputProcessor candidate : ((InputMultiplexer) processor).getProcessors())
                if (candidate instanceof Stage) stage = (Stage) candidate;
        }
        if (stage == null) throw new AssertionError("No UI stage for button: " + label);
        TextButton button = find(stage.getRoot(), label);
        if (button == null) throw new AssertionError("Missing button: " + label);
        Vector2 point = button.localToStageCoordinates(new Vector2(button.getWidth() / 2, button.getHeight() / 2));
        stage.stageToScreenCoordinates(point);
        stage.touchDown((int) point.x, (int) point.y, 0, Input.Buttons.LEFT);
        stage.touchUp((int) point.x, (int) point.y, 0, Input.Buttons.LEFT);
    }

    private TextButton find(Group group, String label) {
        for (Actor actor : group.getChildren()) {
            if (actor instanceof TextButton && ((TextButton) actor).getText().toString().equals(label)) {
                return (TextButton) actor;
            }
            if (actor instanceof Group) {
                TextButton found = find((Group) actor, label);
                if (found != null) return found;
            }
        }
        return null;
    }
}
