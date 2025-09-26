package com.cursedarchie.platformer.lwjgl3;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class AtlasGenerator {
    public static void main(final String[] args) {
        TexturePacker.process(
            "assets/hero",  // исходная папка
            "assets/atlases", // куда собирать
            "game" // имя пакета
        );
        System.out.println("DONE");
    }
}
