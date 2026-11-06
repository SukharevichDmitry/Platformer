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
 * with story roles (entrance, corridor, laundry, cellar descent, exit), optional seeded rooms, and rooms placed
 * above a sequence room (a side room reached through a gap in its floor). Room templates are the directory's
 * numbered room files. The built location is validated: every required role is present, the exit is its own
 * room and leads back to the street at a different spawn than the entrance, and every room and interaction can be
 * reached on foot from the entrance spawn. Pure data and file I/O, so it also runs in headless checks.
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
        JsonValue above = root.get("above");
        if (above != null) for (JsonValue entry : above) {
            String over = entry.getString("over");
            Integer baseId = firstRoomByRole.get(over);
            if (baseId == null) throw invalid(directory, "above room has no '" + over + "' room to sit on");
            RoomData base = sequence.get(baseId);
            if (base.neighbors.up != null) throw invalid(directory, "two rooms sit above room " + baseId);
            RoomData data = template(directory, entry);
            if (data.width > base.width) throw invalid(directory, entry.getString("role") + " is wider than the room below");
            data.id = rooms.size;
            data.neighbors = new Neighbors();
            data.neighbors.down = baseId;
            base.neighbors.up = data.id;
            rooms.add(data);
            firstRoomByRole.put(entry.getString("role"), data.id);
        }
        for (RoomData data : rooms) {
            data.location = name;
            RoomInstance room = new RoomInstance(data);
            room.buildTiles();
            location.addRoom(room);
        }
        RoomManager.buildRoomLayout(location);
        validate(directory, root);
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
        JsonValue linked = root.get("linked");
        if (linked != null) for (JsonValue link : linked) {
            int from = roomId(link.getString("from"));
            boolean found = false;
            if (from >= 0) for (InteractionData interaction : location.getRoom(from).getInteractions())
                if ("portal".equals(interaction.kind) && link.getString("location").equals(interaction.targetLocation))
                    found = true;
            if (!found) throw invalid(directory, link.getString("role") + " is not linked from " + link.getString("from"));
        }
        try {
            LocationReachability.validate(location, entrance, startSpawn);
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
