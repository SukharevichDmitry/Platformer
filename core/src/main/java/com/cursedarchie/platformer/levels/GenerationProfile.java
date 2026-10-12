package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.cursedarchie.platformer.actors.enemies.EnemyFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Validated constraints for a future seeded location generator. */
public final class GenerationProfile {
    public final int version;
    public static final class IntRange {
        public final int min;
        public final int max;

        private IntRange(int min, int max) {
            this.min = min;
            this.max = max;
        }
    }

    public static final class RoomPattern {
        public final String id;
        public final String form;
        public final int weight;
        public final IntRange width;
        public final IntRange height;
        public final IntRange roomQuota;
        public final IntRange enemySpawns;
        public final int difficultyBudget;
        public final Set<String> allowedEnemyTypes;

        private RoomPattern(String id, String form, int weight, IntRange width, IntRange height,
                            IntRange roomQuota, IntRange enemySpawns, int difficultyBudget,
                            Set<String> allowedEnemyTypes) {
            this.id = id;
            this.form = form;
            this.weight = weight;
            this.width = width;
            this.height = height;
            this.roomQuota = roomQuota;
            this.enemySpawns = enemySpawns;
            this.difficultyBudget = difficultyBudget;
            this.allowedEnemyTypes = Collections.unmodifiableSet(allowedEnemyTypes);
        }
    }

    public final String locationId;
    public final String returnLocation;
    public final int returnRoomId;
    public final IntRange roomCount;
    public final int difficultyBudget;
    public final Set<String> allowedEnemyTypes;
    public final List<RoomPattern> roomPatterns;

    private GenerationProfile(int version, String locationId, String returnLocation, int returnRoomId,
                              IntRange roomCount, int difficultyBudget,
                              Set<String> allowedEnemyTypes,
                              List<RoomPattern> roomPatterns) {
        this.version = version;
        this.locationId = locationId;
        this.returnLocation = returnLocation;
        this.returnRoomId = returnRoomId;
        this.roomCount = roomCount;
        this.difficultyBudget = difficultyBudget;
        this.allowedEnemyTypes = Collections.unmodifiableSet(allowedEnemyTypes);
        this.roomPatterns = Collections.unmodifiableList(roomPatterns);
    }

    public static GenerationProfile load(FileHandle file) {
        if (!file.exists()) throw invalid(file, "file is missing");
        if (file.length() > 128 * 1024) throw invalid(file, "file is too large");

        JsonValue root;
        try {
            root = new JsonReader().parse(file);
        } catch (RuntimeException e) {
            throw invalid(file, "invalid JSON: " + e.getMessage());
        }
        if (!root.isObject()) throw invalid(file, "root must be an object");

        int version = integer(root.get("version"), file, "version");
        if (version < 1 || version > 1_000_000)
            throw invalid(file, "version must be between 1 and 1000000");
        String locationId = identifier(root.get("locationId"), file, "locationId");
        JsonValue transition = root.get("returnTransition");
        if (transition == null || !transition.isObject())
            throw invalid(file, "returnTransition must be an object");
        String returnLocation = identifier(transition.get("location"), file, "returnTransition.location");
        int returnRoomId = integer(transition.get("id"), file, "returnTransition.id");
        if (returnRoomId < 0 || returnRoomId >= 256 || returnLocation.equals(locationId))
            throw invalid(file, "returnTransition must target an authored room outside this location");
        IntRange roomCount = range(root.get("roomCount"), file, "roomCount", 1, 64);
        int difficultyBudget = budget(root.get("difficultyBudget"), file, "difficultyBudget");
        Set<String> enemyTypes = enemyTypes(root.get("allowedEnemyTypes"), file,
            "allowedEnemyTypes", null, true);

        JsonValue patterns = root.get("roomPatterns");
        if (patterns == null || !patterns.isArray() || patterns.size == 0 || patterns.size > 64)
            throw invalid(file, "roomPatterns must contain 1 to 64 patterns");

        List<RoomPattern> parsed = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        int minimumRooms = 0;
        int maximumRooms = 0;
        for (JsonValue node : patterns) {
            String field = "roomPatterns[" + parsed.size() + "]";
            if (!node.isObject()) throw invalid(file, field + " must be an object");
            String id = identifier(node.get("id"), file, field + ".id");
            if (!ids.add(id)) throw invalid(file, field + ".id duplicates " + id);
            String form = string(node.get("form"), file, field + ".form");
            if (!form.equals("flat") && !form.equals("platforms") && !form.equals("vertical"))
                throw invalid(file, field + ".form must be flat, platforms, or vertical");
            int weight = integer(node.get("weight"), file, field + ".weight");
            if (weight < 1 || weight > 100)
                throw invalid(file, field + ".weight must be between 1 and 100");
            IntRange width = range(node.get("width"), file, field + ".width", 5, 64);
            IntRange height = range(node.get("height"), file, field + ".height", 4, 64);
            IntRange roomQuota = range(node.get("roomQuota"), file, field + ".roomQuota", 0, 64);
            IntRange spawns = range(node.get("enemySpawns"), file, field + ".enemySpawns", 0, 16);
            int roomBudget = budget(node.get("difficultyBudget"), file, field + ".difficultyBudget");
            Set<String> patternTypes = enemyTypes(node.get("allowedEnemyTypes"), file,
                field + ".allowedEnemyTypes", enemyTypes, spawns.max > 0);
            if (spawns.min > roomBudget || (spawns.max > 0 && roomBudget == 0))
                throw invalid(file, field + ".difficultyBudget cannot satisfy enemySpawns");
            parsed.add(new RoomPattern(id, form, weight, width, height, roomQuota, spawns,
                roomBudget, patternTypes));
            minimumRooms += roomQuota.min;
            maximumRooms += roomQuota.max;
        }
        if (minimumRooms > roomCount.max || maximumRooms < roomCount.min)
            throw invalid(file, "roomQuota totals cannot satisfy roomCount");
        return new GenerationProfile(version, locationId, returnLocation, returnRoomId,
            roomCount, difficultyBudget, enemyTypes, parsed);
    }

    private static int budget(JsonValue value, FileHandle file, String field) {
        int result = integer(value, file, field);
        if (result < 0 || result > 256) throw invalid(file, field + " must be between 0 and 256");
        return result;
    }

    private static IntRange range(JsonValue value, FileHandle file, String field, int lower, int upper) {
        if (value == null || !value.isObject()) throw invalid(file, field + " must be an object");
        int min = integer(value.get("min"), file, field + ".min");
        int max = integer(value.get("max"), file, field + ".max");
        if (min < lower || max > upper || min > max)
            throw invalid(file, field + " must satisfy " + lower + " <= min <= max <= " + upper);
        return new IntRange(min, max);
    }

    private static Set<String> enemyTypes(JsonValue value, FileHandle file, String field,
                                          Set<String> locationTypes, boolean required) {
        if (value == null || !value.isArray() || value.size > 16 || (required && value.size == 0))
            throw invalid(file, field + " must be an array of " + (required ? "1 to 16" : "0 to 16") + " types");
        Set<String> types = new LinkedHashSet<>();
        for (JsonValue entry : value) {
            String type = identifier(entry, file, field + " entry");
            if (!EnemyFactory.supportsType(type)) throw invalid(file, field + " has unsupported type " + type);
            if (locationTypes != null && !locationTypes.contains(type))
                throw invalid(file, field + " has type " + type + " outside the location palette");
            if (!types.add(type)) throw invalid(file, field + " repeats type " + type);
        }
        return types;
    }

    private static String identifier(JsonValue value, FileHandle file, String field) {
        String id = string(value, file, field);
        if (!id.matches("[A-Za-z0-9_-]{1,64}"))
            throw invalid(file, field + " must be a 1 to 64 character identifier");
        return id;
    }

    private static String string(JsonValue value, FileHandle file, String field) {
        if (value == null || !value.isString()) throw invalid(file, field + " must be a string");
        return value.asString();
    }

    private static int integer(JsonValue value, FileHandle file, String field) {
        if (value == null || !value.isNumber()) throw invalid(file, field + " must be an integer");
        double number = value.asDouble();
        if (!Double.isFinite(number) || number != Math.rint(number) || number < Integer.MIN_VALUE || number > Integer.MAX_VALUE)
            throw invalid(file, field + " must be an integer");
        return (int) number;
    }

    private static IllegalArgumentException invalid(FileHandle file, String detail) {
        return new IllegalArgumentException(file.path() + ": " + detail);
    }
}
