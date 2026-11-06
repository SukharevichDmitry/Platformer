package com.cursedarchie.platformer;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Preferences;
import com.cursedarchie.platformer.run.RunCheckpoint;
import com.cursedarchie.platformer.run.RunCheckpointStore;
import com.cursedarchie.platformer.world.GameWorld;
import com.cursedarchie.platformer.screens.GameScreen;
import com.cursedarchie.platformer.screens.MenuScreen;
import com.cursedarchie.platformer.screens.PauseScreen;
import com.cursedarchie.platformer.screens.SettingsScreen;
import com.cursedarchie.platformer.screens.DisplaySettings;
import com.cursedarchie.platformer.controllers.ControlBindings;

public class Main extends Game {
    private GameScreen activeGame;
    private Preferences runPreferences;
    private RunCheckpointStore runStore;
    private ControlBindings controls;
    private DisplaySettings displaySettings;

    @Override
    public void create() {
        runPreferences = Gdx.app.getPreferences(getClass().getName() + ".run");
        runStore = new RunCheckpointStore(runPreferences);
        Preferences settings = Gdx.app.getPreferences(getClass().getName() + ".settings");
        controls = new ControlBindings(settings);
        displaySettings = new DisplaySettings(settings);
        displaySettings.restore();
        setScreen(new MenuScreen(this));
    }

    public boolean canContinueRun() { return runStore.load() != null; }
    public ControlBindings getControls() { return controls; }
    public DisplaySettings getDisplaySettings() { return displaySettings; }

    public void openSettings(Screen returnTo) { setScreen(new SettingsScreen(this, returnTo)); }

    public void closeSettings(Screen returnTo) {
        Screen settings = getScreen();
        setScreen(returnTo);
        disposeLater(settings);
    }

    public void goToMenu() {
        saveOrClearCurrentRun();
        Screen previous = getScreen();
        GameScreen endingGame = activeGame;
        activeGame = null;
        setScreen(new MenuScreen(this));
        disposeLater(previous);
        if (endingGame != previous) disposeLater(endingGame);
    }

    public void startGame() {
        runStore.clearRun();
        Screen previous = getScreen();
        GameScreen newGame = new GameScreen(this, new GameWorld(new java.util.Random().nextLong(), runPreferences));
        GameScreen endingGame = activeGame;
        activeGame = newGame;
        setScreen(newGame);
        saveOrClearCurrentRun();
        disposeLater(previous);
        if (endingGame != previous) disposeLater(endingGame);
    }

    public void continueSavedRun() {
        RunCheckpoint checkpoint = runStore.load();
        if (checkpoint == null) return;
        GameWorld world;
        try { world = new GameWorld(checkpoint, runPreferences); }
        catch (RuntimeException invalid) {
            runStore.clearRun();
            Gdx.app.error("RunCheckpoint", "Saved run could not be restored", invalid);
            return;
        }
        Screen previous = getScreen();
        GameScreen newGame = new GameScreen(this, world);
        GameScreen endingGame = activeGame;
        activeGame = newGame;
        setScreen(newGame);
        saveOrClearCurrentRun();
        disposeLater(previous);
        if (endingGame != previous) disposeLater(endingGame);
    }

    public void pauseGame(Screen previousScreen) {
        saveOrClearCurrentRun();
        setScreen(new PauseScreen(this, previousScreen));
    }

    public void continueGame(Screen previousScreen) {
        Screen pauseScreen = getScreen();
        setScreen(previousScreen);
        disposeLater(pauseScreen);
    }

    private void disposeLater(Screen screen) {
        if (screen != null) Gdx.app.postRunnable(screen::dispose);
    }

    public void exitGame() {
        saveOrClearCurrentRun();
        Gdx.app.exit();
    }

    public void autosaveGame() { saveOrClearCurrentRun(); }

    @Override
    public void pause() { saveOrClearCurrentRun(); }

    private void saveOrClearCurrentRun() {
        if (activeGame == null) return;
        GameWorld world = activeGame.getGameWorld();
        if (world.getHero().isAlive()) runStore.save(world.checkpoint());
        else runStore.clearRun();
    }

    @Override
    public void dispose() {
        saveOrClearCurrentRun();
        Screen current = getScreen();
        if (current != null) current.dispose();
        if (activeGame != null && activeGame != current) activeGame.dispose();
        activeGame = null;
    }
}
