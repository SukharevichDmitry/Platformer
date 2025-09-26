package com.cursedarchie.platformer;

import com.badlogic.gdx.ApplicationListener;
import com.cursedarchie.platformer.screens.GameScreen;
import com.cursedarchie.platformer.screens.MenuScreen;
import com.badlogic.gdx.Game;
import com.cursedarchie.platformer.world.World;

public class Main extends Game {

    private MenuScreen menuScreen;

//    @Override
//    public void create() {
//        //TODO initialization of all objects
//        setScreen(new GameScreen(this));
//    }

    @Override
    public void create() {
        menuScreen = new MenuScreen(this);
        setScreen(menuScreen);
    }

    @Override
    public void render() {
        //TODO listen clicks, work with logic, render objects
        super.render();
    }

    public void showMenu() {
        setScreen(new MenuScreen(this));
    }


//    @Override
//    public void resize(int i, int i1) {
//        //TODO for settings of camera, GUI and new size
//    }
//

//    @Override
//    public void pause() {
//        //TODO calls when window wraps
//    }
//
//    @Override
//    public void resume() {
//        //TODO continue game process
//    }
//
//    @Override
//    public void dispose() {
//        //TODO for data cleaning after closing
//    }
}
