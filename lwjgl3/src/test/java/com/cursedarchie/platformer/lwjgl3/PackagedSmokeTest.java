package com.cursedarchie.platformer.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Texture;
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
import com.cursedarchie.platformer.world.GameWorld;

/** Integration check using real GL, native libraries, packaged assets and UI input. */
public final class PackagedSmokeTest extends Main {
    private int frames;
    private Screen firstGame;
    private GameScreen deathGame;
    private boolean complete;
    private int menuTextures;

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
                click("START");
                expect(GameScreen.class);
                firstGame = getScreen();
                break;
            case 2:
                exerciseTransitions(((GameScreen) getScreen()).getGameWorld());
                exerciseBorderTravel(((GameScreen) getScreen()).getGameWorld());
                exerciseGroundSeam(((GameScreen) getScreen()).getGameWorld());
                if (Texture.getNumManagedTextures() != menuTextures) throw new AssertionError("Gameplay texture count grew");
                Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
                expect(PauseScreen.class);
                break;
            case 3:
                click("Continue");
                expect(GameScreen.class);
                if (getScreen() != firstGame) throw new AssertionError("Continue replaced the game");
                break;
            case 4:
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
                click("START");
                expect(GameScreen.class);
                if (getScreen() == firstGame) throw new AssertionError("Restart reused the game");
                break;
            case 7:
                if (Texture.getNumManagedTextures() != menuTextures) throw new AssertionError("Restart texture count grew");
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
                if (!deathGame.isDisposed()) throw new AssertionError("Dead game was not disposed");
                if (Texture.getNumManagedTextures() != menuTextures) throw new AssertionError("Death leaked textures");
                click("EXIT");
                complete = true;
                break;
            default: throw new AssertionError("Smoke test timed out");
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
            world.changeLocation("start", 2);
            float right = world.getCurrentRoom().getWorldX() + world.getCurrentRoom().getWidth();
            float y = world.getCurrentRoom().getWorldY() + 2;
            world.getHero().getBody().setTransform(right - 0.1f, y, 0);
            world.update(1f / 60f);
            assertLocation(world, "ruins", ruinsBodies, ruinsEnemies);

            float left = world.getCurrentRoom().getWorldX();
            y = world.getCurrentRoom().getWorldY() + 2;
            world.getHero().getBody().setTransform(left + 0.1f, y, 0);
            world.update(1f / 60f);
            assertLocation(world, "start", 4, 2);
            if (world.getCurrentRoom().getRoomId() != 2 ||
                world.getHero().getBody().getPosition().x >=
                    world.getCurrentRoom().getWorldX() + world.getCurrentRoom().getWidth() - 0.2f) {
                throw new AssertionError("Return spawn is in the wrong room or retriggers the ruins exit");
            }
            world.update(1f / 60f);
            assertLocation(world, "start", 4, 2);
        }
    }

    private void exerciseGroundSeam(GameWorld world) {
        world.changeLocation("start", 0);
        for (Enemy enemy : world.getEnemies()) enemy.setAlive(false);
        world.update(1f / 60f);

        for (int roomId = 1; roomId <= 2; roomId++) {
            float seam = world.getLocation().getRoom(roomId).getWorldX();
            int[] floorFixtures = {0};
            world.getWorld().QueryAABB(fixture -> {
                if ("level".equals(fixture.getUserData())) floorFixtures[0]++;
                return true;
            }, seam - 0.01f, 0.5f, seam + 0.01f, 0.5f);
            if (floorFixtures[0] != 1) {
                throw new AssertionError("Room seam " + roomId + " has " + floorFixtures[0] + " separate floor fixtures");
            }
            for (float startY : new float[]{1.25f, 1.26f, 1.3f}) {
                world.getHero().getBody().setTransform(seam - 0.7f, startY, 0);
                world.getHero().getBody().setLinearVelocity(0, 0);
                for (int i = 0; i < 30; i++) world.update(1f / 60f);
                world.getHeroController().keyDown(Input.Keys.RIGHT);
                for (int i = 0; i < 30; i++) world.update(1f / 60f);
                world.getHeroController().keyUp(Input.Keys.RIGHT);
                if (world.getHero().getPosition().x < seam + 0.5f) {
                    throw new AssertionError("Hero stuck crossing room seam " + roomId + " to the right at x=" + world.getHero().getPosition().x);
                }

                world.getHeroController().keyDown(Input.Keys.LEFT);
                for (int i = 0; i < 36; i++) world.update(1f / 60f);
                world.getHeroController().keyUp(Input.Keys.LEFT);
                if (world.getHero().getPosition().x > seam - 0.5f) {
                    throw new AssertionError("Hero stuck crossing room seam " + roomId + " to the left at x=" + world.getHero().getPosition().x);
                }
            }
        }
    }

    private void expect(Class<? extends Screen> type) {
        if (!type.isInstance(getScreen())) throw new AssertionError("Expected " + type.getSimpleName());
    }

    private void click(String label) {
        Stage stage = (Stage) Gdx.input.getInputProcessor();
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
