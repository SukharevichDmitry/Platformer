package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Validates authored room files before any tiles or physics bodies are built. */
public final class RoomValidator {
    private static final int MAX_ROOMS = 256;
    private static final int MAX_DIMENSION = 256;
    private static final int MAX_SPAWNS = 256;
    private static final long MAX_FILE_BYTES = 1024 * 1024;

    private RoomValidator() { }

    public static void validateLocation(FileHandle directory) {
        FileHandle indexFile = directory.child("index.json");
        Set<Integer> ids = readRoomIds(indexFile);
        if (!ids.contains(0)) throw invalid(indexFile, "rooms must include room 0");

        Map<Integer, JsonValue> rooms = new HashMap<>();
        for (int id : ids) {
            FileHandle roomFile = directory.child(id + ".json");
            JsonValue room = readObject(roomFile);
            if (integer(room.get("id"), roomFile, "id") != id)
                throw invalid(roomFile, "id does not match index entry " + id);
            if (!string(room.get("location"), roomFile, "location").equals(directory.name()))
                throw invalid(roomFile, "location must be " + directory.name());
            int width = integer(room.get("width"), roomFile, "width");
            int height = integer(room.get("height"), roomFile, "height");
            if (width < 1 || width > MAX_DIMENSION || height < 1 || height > MAX_DIMENSION)
                throw invalid(roomFile, "width and height must be between 1 and " + MAX_DIMENSION);
            checkTiles(room.get("tileRows"), roomFile, width, height);
            checkSpawns(room.get("enemySpawns"), roomFile, width, height);
            if (room.get("neighbors") == null || !room.get("neighbors").isObject())
                throw invalid(roomFile, "neighbors must be an object");
            rooms.put(id, room);
        }

        for (int id : ids) {
            FileHandle roomFile = directory.child(id + ".json");
            JsonValue room = rooms.get(id);
            for (String direction : new String[] {"left", "right", "up", "down"}) {
                JsonValue neighbor = room.get("neighbors").get(direction);
                if (neighbor != null && !neighbor.isNull() &&
                    !ids.contains(integer(neighbor, roomFile, "neighbors." + direction)))
                    throw invalid(roomFile, "neighbors." + direction + " references a room absent from index.json");
            }
            checkTransition(room.get("locationEntry"), roomFile, directory, ids, "locationEntry");
            checkTransition(room.get("locationExit"), roomFile, directory, ids, "locationExit");
        }
    }

    private static Set<Integer> readRoomIds(FileHandle indexFile) {
        JsonValue index = readObject(indexFile);
        JsonValue list = index.get("rooms");
        if (list == null || !list.isArray() || list.size == 0 || list.size > MAX_ROOMS)
            throw invalid(indexFile, "rooms must be an array of 1 to " + MAX_ROOMS + " IDs");
        Set<Integer> ids = new HashSet<>();
        for (JsonValue node : list) {
            int id = integer(node, indexFile, "rooms entry");
            if (id < 0 || id >= MAX_ROOMS || !ids.add(id))
                throw invalid(indexFile, "invalid or duplicate room ID " + id);
        }
        return ids;
    }

    private static void checkTiles(JsonValue rows, FileHandle file, int width, int height) {
        if (rows == null || !rows.isArray() || rows.size != height)
            throw invalid(file, "tileRows must contain exactly " + height + " rows");
        int rowNumber = 0;
        for (JsonValue row : rows) {
            String tiles = string(row, file, "tileRows[" + rowNumber + "]");
            if (tiles.length() != width)
                throw invalid(file, "tileRows[" + rowNumber + "] has width " + tiles.length() + "; expected " + width);
            for (int x = 0; x < width; x++) {
                char tile = tiles.charAt(x);
                if (tile != ' ' && tile != '#' && tile != '_')
                    throw invalid(file, "tileRows[" + rowNumber + "][" + x + "] has unknown tile '" + tile + "'");
            }
            rowNumber++;
        }
    }

    private static void checkSpawns(JsonValue spawns, FileHandle file, int width, int height) {
        if (spawns == null || !spawns.isArray() || spawns.size > MAX_SPAWNS)
            throw invalid(file, "enemySpawns must be an array of at most " + MAX_SPAWNS);
        int index = 0;
        for (JsonValue spawn : spawns) {
            if (!spawn.isObject()) throw invalid(file, "enemySpawns[" + index + "] must be an object");
            String type = string(spawn.get("type"), file, "enemySpawns[" + index + "].type");
            if (!type.equals("default") && !type.equals("boss"))
                throw invalid(file, "enemySpawns[" + index + "] has unknown type " + type);
            JsonValue position = spawn.get("position");
            if (position == null || !position.isObject())
                throw invalid(file, "enemySpawns[" + index + "].position must be an object");
            double x = number(position.get("x"), file, "enemySpawns[" + index + "].position.x");
            double y = number(position.get("y"), file, "enemySpawns[" + index + "].position.y");
            if (x < 0 || x >= width || y < 0 || y >= height)
                throw invalid(file, "enemySpawns[" + index + "] position is outside the room");
            index++;
        }
    }

    private static void checkTransition(JsonValue transition, FileHandle file, FileHandle directory,
                                        Set<Integer> ownIds, String field) {
        if (transition == null || transition.isNull()) return;
        if (!transition.isObject()) throw invalid(file, field + " must be an object");
        String location = string(transition.get("location"), file, field + ".location");
        if (!location.matches("[A-Za-z0-9_-]{1,64}"))
            throw invalid(file, field + ".location has an invalid name");
        int id = integer(transition.get("id"), file, field + ".id");
        if (id < 0 || id >= MAX_ROOMS) throw invalid(file, field + ".id is out of range");
        FileHandle generatedProfile = directory.parent().parent().child("generation").child(location + ".json");
        if (generatedProfile.exists()) {
            GenerationProfile profile = GenerationProfile.load(generatedProfile);
            RoomTemplateCatalog.load(generatedProfile.parent().child(location + ".templates.json"), profile);
            if (!profile.locationId.equals(location) || id != 0)
                throw invalid(file, field + " must target generated location " + location + " entry 0");
            return;
        }
        FileHandle target = directory.parent().child(location);
        Set<Integer> targetIds = location.equals(directory.name()) ? ownIds : readRoomIds(target.child("index.json"));
        if (!targetIds.contains(id) || !target.child(id + ".json").exists())
            throw invalid(file, field + " references missing room " + location + "/" + id);
    }

    private static JsonValue readObject(FileHandle file) {
        if (!file.exists()) throw invalid(file, "file is missing");
        if (file.length() > MAX_FILE_BYTES) throw invalid(file, "file exceeds " + MAX_FILE_BYTES + " bytes");
        try {
            JsonValue value = new JsonReader().parse(file);
            if (!value.isObject()) throw invalid(file, "root must be an object");
            return value;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw invalid(file, "invalid JSON: " + e.getMessage());
        }
    }

    private static int integer(JsonValue value, FileHandle file, String field) {
        double number = number(value, file, field);
        if (number != Math.rint(number) || number < Integer.MIN_VALUE || number > Integer.MAX_VALUE)
            throw invalid(file, field + " must be an integer");
        return (int) number;
    }

    private static double number(JsonValue value, FileHandle file, String field) {
        if (value == null || !value.isNumber()) throw invalid(file, field + " must be a number");
        double number = value.asDouble();
        if (!Double.isFinite(number)) throw invalid(file, field + " must be finite");
        return number;
    }

    private static String string(JsonValue value, FileHandle file, String field) {
        if (value == null || !value.isString()) throw invalid(file, field + " must be a string");
        return value.asString();
    }

    private static IllegalArgumentException invalid(FileHandle file, String detail) {
        return new IllegalArgumentException(file.path() + ": " + detail);
    }
}
