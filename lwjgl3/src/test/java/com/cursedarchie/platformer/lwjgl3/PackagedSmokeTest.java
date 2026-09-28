package com.cursedarchie.platformer.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.cursedarchie.platformer.Main;
import com.cursedarchie.platformer.screens.GameScreen;
import com.cursedarchie.platformer.screens.MenuScreen;
import com.cursedarchie.platformer.screens.PauseScreen;

/** Integration check using real GL, native libraries, packaged assets and UI input. */
public final class PackagedSmokeTest extends Main {
    private int frames;
    private Screen firstGame;
    private boolean complete;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setInitialVisible(false);
        config.setWindowedMode(640, 480);
        config.setForegroundFPS(60);
        PackagedSmokeTest test = new PackagedSmokeTest();
        new Lwjgl3Application(test, config);
        if (!test.complete) throw new AssertionError("Smoke test ended before completion");
        System.out.println("PACKAGED SMOKE PASSED: menu, start, pause, continue, menu, restart, exit");
    }

    @Override public void render() {
        super.render();
        if (++frames % 20 != 0) return;
        switch (frames / 20) {
            case 1:
                expect(MenuScreen.class);
                click("START");
                expect(GameScreen.class);
                firstGame = getScreen();
                break;
            case 2:
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
                click("START");
                expect(GameScreen.class);
                if (getScreen() == firstGame) throw new AssertionError("Restart reused the game");
                break;
            case 7:
                Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
                expect(PauseScreen.class);
                break;
            case 8:
                click("To Menu");
                expect(MenuScreen.class);
                break;
            case 9:
                click("EXIT");
                complete = true;
                break;
            default: throw new AssertionError("Smoke test timed out");
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
