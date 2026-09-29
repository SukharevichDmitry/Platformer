package com.cursedarchie.platformer;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.cursedarchie.platformer.screens.GameScreen;
import com.cursedarchie.platformer.screens.MenuScreen;
import com.cursedarchie.platformer.screens.PauseScreen;

public class Main extends Game {
    private GameScreen activeGame;

    @Override
    public void create() {
        setScreen(new MenuScreen(this));
    }

    public void goToMenu() {
        Screen previous = getScreen();
        GameScreen endingGame = activeGame;
        activeGame = null;
        setScreen(new MenuScreen(this));
        disposeLater(previous);
        if (endingGame != previous) disposeLater(endingGame);
    }

    public void startGame() {
        Screen previous = getScreen();
        GameScreen newGame = new GameScreen(this);
        GameScreen endingGame = activeGame;
        activeGame = newGame;
        setScreen(newGame);
        disposeLater(previous);
        if (endingGame != previous) disposeLater(endingGame);
    }

    public void pauseGame(Screen previousScreen) {
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
        Gdx.app.exit();
    }

    @Override
    public void dispose() {
        Screen current = getScreen();
        if (current != null) current.dispose();
        if (activeGame != null && activeGame != current) activeGame.dispose();
        activeGame = null;
    }
}
