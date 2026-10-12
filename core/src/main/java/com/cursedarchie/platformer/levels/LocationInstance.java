package com.cursedarchie.platformer.levels;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LocationInstance {

    private final String location;

    private final Map<Integer, RoomInstance> rooms =
        new HashMap<>();
    private Long generationSeed;
    private int entryRoomId;

    public LocationInstance(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    public boolean isGenerated() { return generationSeed != null; }
    public Long getGenerationSeed() { return generationSeed; }
    public int getEntryRoomId() { return entryRoomId; }

    void setGeneration(long seed, int entryRoomId) {
        this.generationSeed = seed;
        this.entryRoomId = entryRoomId;
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
