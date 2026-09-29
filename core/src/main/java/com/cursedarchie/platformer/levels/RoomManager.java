package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.cursedarchie.platformer.levels.wrappers.RoomData;

import java.util.*;

public class RoomManager {

    private final Map<Integer, RoomData> cache = new HashMap<>();
    private String loadedLocation;

    private final JsonReader jsonReader = new JsonReader();

    public LocationInstance loadLocation(String location) {

        FileHandle baseDir = Gdx.files.internal("levels/" + location);
        RoomValidator.validateLocation(baseDir);
        cache.clear();

        FileHandle indexFile = baseDir.child("index.json");
        JsonValue index = jsonReader.parse(indexFile);

        JsonValue rooms = index.get("rooms");

        LocationInstance locationInstance = new LocationInstance(location);

        if (rooms == null) {
            throw new RuntimeException("index.json missing 'rooms' array");
        }

        for (JsonValue roomIdNode : rooms) {

            int id = roomIdNode.asInt();

            FileHandle roomFile = baseDir.child(id + ".json");

            if (!roomFile.exists()) {
                throw new RuntimeException("Missing room file: " + roomFile.path());
            }

            RoomData data = RoomLoader.load(roomFile);

            cache.put(data.id, data);

            RoomInstance room = new RoomInstance(data);

            room.buildTiles();

            locationInstance.addRoom(room);
        }
        buildRoomLayout(locationInstance);

        loadedLocation = location;
        return locationInstance;
    }

    private void buildRoomLayout(LocationInstance location) {

        RoomInstance start = location.getRoom(0);

        start.setWorldPosition(0, 0);

        Set<Integer> visited = new HashSet<>();
        Queue<RoomInstance> queue = new LinkedList<>();

        queue.add(start);
        visited.add(start.getRoomId());

        while (!queue.isEmpty()) {

            RoomInstance room = queue.poll();

            float x = room.getWorldX();
            float y = room.getWorldY();

            // RIGHT
            Integer rightId = room.getRightNeighbor();
            if (rightId != null && !visited.contains(rightId)) {

                RoomInstance right = location.getRoom(rightId);

                right.setWorldPosition(
                    x + room.getWidth(),
                    y
                );

                visited.add(rightId);
                queue.add(right);
            }

            // LEFT
            Integer leftId = room.getLeftNeighbor();
            if (leftId != null && !visited.contains(leftId)) {

                RoomInstance left = location.getRoom(leftId);

                left.setWorldPosition(
                    x - left.getWidth(),
                    y
                );

                visited.add(leftId);
                queue.add(left);
            }

            // UP
            Integer upId = room.getUpNeighbor();
            if (upId != null && !visited.contains(upId)) {

                RoomInstance up = location.getRoom(upId);

                up.setWorldPosition(
                    x,
                    y + room.getHeight()
                );

                visited.add(upId);
                queue.add(up);
            }

            // DOWN
            Integer downId = room.getDownNeighbor();
            if (downId != null && !visited.contains(downId)) {

                RoomInstance down = location.getRoom(downId);

                down.setWorldPosition(
                    x,
                    y - down.getHeight()
                );

                visited.add(downId);
                queue.add(down);
            }
        }
    }

    private void processNeighbor(
        RoomInstance room,
        Integer neighborId,
        LocationInstance location,
        Set<Integer> visited,
        Queue<RoomInstance> queue,
        float worldX,
        float worldY
    ) {

        if (neighborId == null) {
            return;
        }

        if (visited.contains(neighborId)) {
            return;
        }

        RoomInstance neighbor =
            location.getRoom(neighborId);

        neighbor.setWorldPosition(
            worldX,
            worldY
        );

        visited.add(neighborId);

        queue.add(neighbor);
    }
}
