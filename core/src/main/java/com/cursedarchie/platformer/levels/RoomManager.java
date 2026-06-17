package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.cursedarchie.platformer.levels.wrappers.RoomData;

import java.util.HashMap;
import java.util.Map;

public class RoomManager {

    private final Map<Integer, RoomData> cache = new HashMap<>();
    private String loadedLocation;

    private final JsonReader jsonReader = new JsonReader();

    private void loadLocation(String location) {

        cache.clear();

        FileHandle baseDir = Gdx.files.internal("levels/" + location);

        FileHandle indexFile = baseDir.child("index.json");
        JsonValue index = jsonReader.parse(indexFile);

        JsonValue rooms = index.get("rooms");

        if (rooms == null) {
            throw new RuntimeException("index.json missing 'rooms' array");
        }

        for (JsonValue roomIdNode : rooms) {

            int id = roomIdNode.asInt();

            FileHandle roomFile = baseDir.child(id + ".json");

            if (!roomFile.exists()) {
                throw new RuntimeException("Missing room file: " + roomFile.path());
            }

            RoomData room = RoomLoader.load(roomFile);

            cache.put(room.id, room);
        }

        loadedLocation = location;
    }

    public RoomInstance getRoom(String location, int roomId) {

        if (!location.equals(loadedLocation)) {
            loadLocation(location);
        }

        RoomData data = cache.get(roomId);

        if (data == null) {
            throw new RuntimeException(
                "Room " + roomId + " not found in location " + location
            );
        }

        RoomInstance room = new RoomInstance(data);
        room.buildTiles();

        return room;
    }
}
