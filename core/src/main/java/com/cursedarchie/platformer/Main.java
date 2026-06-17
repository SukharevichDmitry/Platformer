    package com.cursedarchie.platformer;

    import com.badlogic.gdx.Game;
    import com.badlogic.gdx.Gdx;
    import com.badlogic.gdx.Screen;
    import com.cursedarchie.platformer.screens.GameScreen;
    import com.cursedarchie.platformer.screens.MenuScreen;
    import com.cursedarchie.platformer.screens.PauseScreen;

    public class Main extends Game {

        @Override
        public void create() {
            setScreen(new MenuScreen(this));
        }

        @Override
        public void render() {
            super.render();
        }

        public void goToMenu() {
            setScreen(new MenuScreen(this));
        }

        public void startGame() {
            setScreen(new GameScreen(this));
        }

        public void pauseGame(Screen previousScreen) {
            setScreen(new PauseScreen(this, previousScreen));
        }

        public void continueGame(Screen previousScreen) {
            setScreen(previousScreen);
        }

        public void exitGame() {
            Gdx.app.exit();
        }
    }
