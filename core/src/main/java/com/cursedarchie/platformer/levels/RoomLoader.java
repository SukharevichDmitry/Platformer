package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.cursedarchie.platformer.levels.wrappers.RoomData;

public class RoomLoader {

    private static final Json json = new Json();

    public static RoomData load(FileHandle file) {
        return json.fromJson(RoomData.class, file);
    }
}
