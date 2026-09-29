package com.cursedarchie.platformer.levels;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LocationInstance {

    private final String location;

    private final Map<Integer, RoomInstance> rooms =
        new HashMap<>();

    public LocationInstance(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    public void addRoom(RoomInstance room) {
        rooms.put(room.getRoomId(), room);
    }

    public RoomInstance getRoom(int id) {
        return rooms.get(id);
    }

    public Collection<RoomInstance> getRooms() {
        return rooms.values();
    }
}
