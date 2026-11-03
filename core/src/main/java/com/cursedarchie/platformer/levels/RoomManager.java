package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.cursedarchie.platformer.levels.wrappers.RoomData;
import com.cursedarchie.platformer.levels.wrappers.InteractionData;
import com.cursedarchie.platformer.levels.wrappers.Neighbors;
import com.cursedarchie.platformer.run.RunCheckpoint;

import java.util.*;

public class RoomManager {

    private final Map<Integer, RoomData> cache = new HashMap<>();
    private final Map<String, LocationInstance> generatedLocations = new HashMap<>();
    private final long runSeed;
    private final GeneratedRunStore runStore;
    private String loadedLocation;

    private final JsonReader jsonReader = new JsonReader();

    public RoomManager() {
        // START creates a new run. Persistence is opt-in for an eventual Continue flow.
        this(new Random().nextLong(), new GeneratedRunStore(null));
    }

    public RoomManager(long runSeed) { this(runSeed, new GeneratedRunStore(null)); }

    public RoomManager(long runSeed, Preferences preferences) {
        this(runSeed, new GeneratedRunStore(preferences));
    }

    private RoomManager(long runSeed, GeneratedRunStore runStore) {
        this.runSeed = runSeed;
        this.runStore = runStore;
    }

    public long getRunSeed() { return runSeed; }

    public Collection<GeneratedRunSave> getGeneratedRuns() {
        List<GeneratedRunSave> runs = new ArrayList<>();
        for (LocationInstance location : generatedLocations.values()) runs.add(location.getGeneratedRun());
        return runs;
    }

    /** Continue must not regenerate a missing or incompatible saved location. */
    public boolean hasSavedGeneratedReference(RunCheckpoint.GeneratedLocationRef ref) {
        FileHandle profileFile = Gdx.files.internal("generation/" + ref.locationId + ".json");
        if (!profileFile.exists()) return false;
        int currentVersion = GenerationProfile.load(profileFile).version;
        boolean oldDoorwayUpgrade = "ruins".equals(ref.locationId) &&
            ref.profileVersion == 7 && currentVersion == 8;
        if (ref.profileVersion != currentVersion && !oldDoorwayUpgrade) return false;
        GeneratedRunSave saved = runStore.load(ref.locationId, currentVersion);
        return saved != null && saved.topologySignature.equals(ref.topologySignature) &&
            saved.placementSignature.equals(ref.placementSignature);
    }

    public LocationInstance loadLocation(String location) {
        if ("laundry_alley".equals(location)) return generateAlley();
        FileHandle profile = Gdx.files.internal("generation/" + location + ".json");
        if (profile.exists()) {
            LocationInstance existing = generatedLocations.get(location);
            if (existing != null) return existing;
            GenerationProfile generationProfile = GenerationProfile.load(profile);
            GeneratedRunSave restored = runStore.load(location, generationProfile.version);
            LocationInstance generated;
            try {
                generated = new GeneratedLocationFactory().load(profile,
                    Gdx.files.internal("generation/" + location + ".templates.json"), runSeed, restored);
            } catch (IllegalArgumentException invalidSave) {
                if (restored == null) throw invalidSave;
                runStore.remove(location);
                generated = new GeneratedLocationFactory().load(profile,
                    Gdx.files.internal("generation/" + location + ".templates.json"), runSeed, null);
            }
            runStore.save(generated.getGeneratedRun());
            generatedLocations.put(location, generated);
            loadedLocation = location;
            return generated;
        }

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
            if ("laundry_house".equals(location) || "catchment_cellar".equals(location)) {
                for (InteractionData interaction : data.interactions) {
                    if (!"alley_return".equals(interaction.id)) continue;
                    interaction.targetRoomId = "laundry_house".equals(location) ? alleyRoomCount() - 2 : alleyRoomCount() - 1;
                }
            }

            cache.put(data.id, data);

            RoomInstance room = new RoomInstance(data);

            room.buildTiles();

            locationInstance.addRoom(room);
        }
        buildRoomLayout(locationInstance);

        loadedLocation = location;
        return locationInstance;
    }

    private int alleyRoomCount() {
        return 4 + Math.floorMod(runSeed, 4);
    }

    /** Deterministic corridor: entry, supplies, optional passages, laundry, cellar. */
    private LocationInstance generateAlley() {
        FileHandle directory = Gdx.files.internal("levels/laundry_alley");
        RoomValidator.validateLocation(directory);
        Random random = new Random(runSeed ^ 0x4A11E7L);
        int count = alleyRoomCount();
        LocationInstance alley = new LocationInstance("laundry_alley");
        for (int id = 0; id < count; id++) {
            int template = id == 0 ? 0 : id == 1 ? 1 : id == count - 2 ? 2 : id == count - 1 ? 3 : 1;
            RoomData data = RoomLoader.load(directory.child(template + ".json"));
            data.id = id;
            data.neighbors = new Neighbors();
            data.neighbors.left = id == 0 ? null : id - 1;
            data.neighbors.right = id == count - 1 ? null : id + 1;
            if (id > 1 && id < count - 2) {
                data.interactions.clear();
                data.enemySpawns.clear();
            }
            if (id != 0) {
                int width = 26 + random.nextInt(11);
                data.width = width;
                data.tileRows = new String[14];
                for (int y = 0; y < 12; y++) data.tileRows[y] = " ".repeat(width);
                data.tileRows[12] = "#".repeat(width);
                data.tileRows[13] = "#".repeat(width);
            }
            RoomInstance room = new RoomInstance(data);
            room.buildTiles();
            alley.addRoom(room);
        }
        buildRoomLayout(alley);
        return alley;
    }

    public void saveGeneratedRun(LocationInstance location) {
        if (location != null && location.isGenerated()) runStore.save(location.getGeneratedRun());
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
