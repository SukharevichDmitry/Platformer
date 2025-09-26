package com.cursedarchie.platformer.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.cursedarchie.platformer.Main;

public class PlatformerDesktop {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Platformer");
        config.setWindowedMode(1440, 960);
        config.setResizable(false);
        new Lwjgl3Application(new Main(), config);
    }
}
