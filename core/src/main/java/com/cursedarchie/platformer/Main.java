    package com.cursedarchie.platformer;

    import com.badlogic.gdx.Game;
    import com.cursedarchie.platformer.screens.MenuScreen;

    public class Main extends Game {

        @Override
        public void create() {
            setScreen(new MenuScreen(this));
        }

        @Override
        public void render() {
            super.render();
        }

        public void showMenu() {
            setScreen(new MenuScreen(this));
        }

        public void startGame() {
            setScreen(new com.cursedarchie.platformer.screens.GameScreen(this));
        }
    }
