package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.cursedarchie.platformer.levels.wrappers.InteractionData;
import com.cursedarchie.platformer.levels.wrappers.Neighbors;
import com.cursedarchie.platformer.levels.wrappers.RoomData;

import java.util.Random;

/**
 * A multi-room alley built from {@code structure.json} in its level directory: a left-to-right sequence of rooms
 * with story roles (entrance, corridor, laundry, cellar descent, exit), optional seeded rooms, rooms placed above
 * a room (a side room reached through a gap in its floor) and rooms below one (laundry house, holding cellar,
 * vault) reached by in-location descents. Room templates are the directory's numbered room files. The built
 * location is validated: every required role is present, rooms do not overlap, the exit is its own room and
 * leads back to the street at a different spawn than the entrance, and every room and interaction can be reached
 * from the entrance spawn on foot or through the location's own descents. Pure data and file I/O, so it also
 * runs in headless checks.
 */
public final class AlleyStructure {
    public static final String FILE = "structure.json";
    /** Seed salt: keeps the alley's choices independent of the street generator using the same run seed. */
    private static final long SEED_SALT = 0x4A11E7L;

    private final LocationInstance location;
    private final ObjectMap<String, Integer> firstRoomByRole = new ObjectMap<>();
    private final String startRole, startSpawn;

    private AlleyStructure(FileHandle directory, long seed) {
        JsonValue root = new JsonReader().parse(directory.child(FILE));
        String name = root.getString("location", directory.name());
        location = new LocationInstance(name);
        startRole = root.get("start").getString("role");
        startSpawn = root.get("start").getString("spawn");
        Random random = new Random(seed ^ SEED_SALT);

        Array<RoomData> sequence = new Array<>();
        Array<String> roles = new Array<>();
        for (JsonValue entry : root.get("sequence")) {
            // Optional rooms draw from the seed whether or not they appear, so later choices stay stable.
            float roll = random.nextFloat();
            if (entry.has("chance") && roll >= entry.getFloat("chance")) continue;
            sequence.add(template(directory, entry));
            roles.add(entry.getString("role"));
        }
        Array<RoomData> rooms = new Array<>(sequence);
        for (int id = 0; id < sequence.size; id++) {
            RoomData data = sequence.get(id);
            data.id = id;
            data.neighbors = new Neighbors();
            data.neighbors.left = id == 0 ? null : id - 1;
            data.neighbors.right = id == sequence.size - 1 ? null : id + 1;
            if (!firstRoomByRole.containsKey(roles.get(id))) firstRoomByRole.put(roles.get(id), id);
        }
        addStacked(directory, root.get("above"), "over", true, sequence, rooms);
        addStacked(directory, root.get("below"), "under", false, sequence, rooms);
        for (RoomData data : rooms) {
            data.location = name;
            resolveRoles(directory, data, rooms);
            RoomInstance room = new RoomInstance(data);
            room.buildTiles();
            location.addRoom(room);
        }
        location.setCameraPerRoom("room".equals(root.getString("camera", "location")));
        RoomManager.buildRoomLayout(location);
        validate(directory, root);
    }

    /** Rooms on top of ("over") or under ("under") an earlier room, one per side of each base room. */
    private void addStacked(FileHandle directory, JsonValue entries, String key, boolean up,
                            Array<RoomData> sequence, Array<RoomData> rooms) {
        if (entries == null) return;
        for (JsonValue entry : entries) {
            String baseRole = entry.getString(key);
            Integer baseId = firstRoomByRole.get(baseRole);
            if (baseId == null) throw invalid(directory, entry.getString("role") + " has no '" + baseRole + "' room");
            RoomData base = rooms.get(baseId);
            if ((up ? base.neighbors.up : base.neighbors.down) != null)
                throw invalid(directory, "two rooms share one side of room " + baseId);
            RoomData data = template(directory, entry);
            // A room above is entered through its floor, so it must stay over the room it sits on.
            if (up && data.width > base.width) throw invalid(directory, entry.getString("role") + " is wider than the room below");
            data.id = rooms.size;
            data.neighbors = new Neighbors();
            if (up) { data.neighbors.down = baseId; base.neighbors.up = data.id; }
            else { data.neighbors.up = baseId; base.neighbors.down = data.id; }
            rooms.add(data);
            firstRoomByRole.put(entry.getString("role"), data.id);
        }
    }

    /**
     * Portals naming a {@code targetRole} lead to that room of this location; {@code {room:role}} in a flag
     * becomes {@code location:roomId}, so defeat flags follow the room wherever the seed places it.
     */
    private void resolveRoles(FileHandle directory, RoomData data, Array<RoomData> rooms) {
        for (InteractionData interaction : data.interactions) {
            if (interaction.requiresFlag != null) interaction.requiresFlag = roomTokens(directory, interaction.requiresFlag);
            if (interaction.targetRole == null) continue;
            Integer target = firstRoomByRole.get(interaction.targetRole);
            if (target == null)
                throw invalid(directory, interaction.id + " leads to a missing room role: " + interaction.targetRole);
            boolean spawnFound = false;
            for (com.cursedarchie.platformer.levels.wrappers.SpawnPointData spawn : rooms.get(target).spawnPoints)
                if (spawn.id.equals(interaction.targetSpawnId)) spawnFound = true;
            if (!spawnFound)
                throw invalid(directory, interaction.id + " lands at a missing spawn " + interaction.targetSpawnId);
            interaction.targetLocation = location.getLocation();
            interaction.targetRoomId = target;
        }
    }

    private String roomTokens(FileHandle directory, String text) {
        StringBuilder out = new StringBuilder();
        int at = 0;
        for (int open = text.indexOf("{room:"); open >= 0; open = text.indexOf("{room:", at)) {
            int close = text.indexOf('}', open);
            String role = text.substring(open + 6, close);
            Integer id = firstRoomByRole.get(role);
            if (id == null) throw invalid(directory, "flag names a missing room role: " + role);
            out.append(text, at, open).append(location.getLocation()).append(':').append(id);
            at = close + 1;
        }
        return out.append(text.substring(at)).toString();
    }

    public static AlleyStructure build(FileHandle directory, long seed) {
        RoomValidator.validateLocation(directory);
        return new AlleyStructure(directory, seed);
    }

    public LocationInstance location() { return location; }

    /** Id of the first room with this role, or -1. */
    public int roomId(String role) {
        Integer id = firstRoomByRole.get(role);
        return id == null ? -1 : id;
    }

    private static RoomData template(FileHandle directory, JsonValue entry) {
        FileHandle file = directory.child(entry.getInt("template") + ".json");
        if (!file.exists()) throw invalid(directory, "missing room template " + file.name());
        return RoomLoader.load(file);
    }

    private void validate(FileHandle directory, JsonValue root) {
        JsonValue required = root.get("requiredRoles");
        if (required != null) for (JsonValue role : required)
            if (!firstRoomByRole.containsKey(role.asString()))
                throw invalid(directory, "required room role is missing: " + role.asString());
        int entrance = roomId(startRole);
        int exit = roomId("exit");
        if (exit >= 0) {
            if (exit == entrance) throw invalid(directory, "the exit must be its own room, not the entrance");
            InteractionData leave = null;
            for (InteractionData interaction : location.getRoom(exit).getInteractions())
                if ("portal".equals(interaction.kind) && interaction.returnToOrigin) leave = interaction;
            if (leave == null) throw invalid(directory, "exit room has no portal back to the street");
            TransitionProfile out = profiles(directory).get(leave.transition);
            if (out == null || out.direction >= 0)
                throw invalid(directory, "exit portal must use a clockwise alley transition");
            // The exit and the entrance are stored separately: the exit lands at its own street spawn.
            if (leave.targetSpawnId == null)
                throw invalid(directory, "exit portal needs its own targetSpawnId on the street");
            for (TransitionProfile profile : profiles(directory).values())
                if (profile.savesOrigin && leave.targetSpawnId.equals(profile.originSpawnId))
                    throw invalid(directory, "exit portal must not land at the entrance's return spawn");
        }
        RoomInstance[] all = location.getRooms().toArray(new RoomInstance[0]);
        for (int i = 0; i < all.length; i++)
            for (int j = i + 1; j < all.length; j++)
                if (all[i].getWorldX() < all[j].getWorldX() + all[j].getWidth() &&
                    all[j].getWorldX() < all[i].getWorldX() + all[i].getWidth() &&
                    all[i].getWorldY() < all[j].getWorldY() + all[j].getHeight() &&
                    all[j].getWorldY() < all[i].getWorldY() + all[i].getHeight())
                    throw invalid(directory, "rooms " + all[i].getRoomId() + " and " + all[j].getRoomId() + " overlap");
        try {
            LocationReachability.validate(location, entrance, startSpawn);
            // The far mouth on the street leads into the exit room; the whole alley must work from there too.
            JsonValue far = root.get("farEntry");
            if (far != null) LocationReachability.validate(location, roomId(far.getString("role")), far.getString("spawn"));
        } catch (IllegalArgumentException e) {
            throw invalid(directory, e.getMessage());
        }
    }

    private ObjectMap<String, TransitionProfile> profiles;

    private ObjectMap<String, TransitionProfile> profiles(FileHandle directory) {
        if (profiles == null) {
            FileHandle file = directory.parent().parent().child(TransitionProfile.FILE);
            profiles = file.exists() ? TransitionProfile.load(file) : new ObjectMap<String, TransitionProfile>();
        }
        return profiles;
    }

    private static IllegalArgumentException invalid(FileHandle directory, String detail) {
        return new IllegalArgumentException(directory.path() + "/" + FILE + ": " + detail);
    }
}
