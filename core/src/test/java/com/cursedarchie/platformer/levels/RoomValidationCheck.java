package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.files.FileHandle;

import java.io.File;

/** Standalone content check run by Gradle's check task without a graphics context. */
public final class RoomValidationCheck {
    private static final String VALID_ROOM = "{\"location\":\"sample\",\"id\":0,\"width\":3,\"height\":2,"
        + "\"tileRows\":[\"###\",\"#  \"],\"enemySpawns\":[],\"neighbors\":{}}";

    public static void main(String[] args) {
        if (args.length != 2) throw new IllegalArgumentException("Expected assets and fixture directories");
        FileHandle levels = new FileHandle(new File(args[0], "levels"));
        RoomValidator.validateLocation(levels.child("start"));
        RoomValidator.validateLocation(levels.child("forest"));

        FileHandle sample = new FileHandle(new File(args[1], "levels/sample"));
        sample.mkdirs();
        sample.child("index.json").writeString("{\"rooms\":[0]}", false);
        sample.child("0.json").writeString(VALID_ROOM, false);
        RoomValidator.validateLocation(sample);

        sample.child("0.json").writeString(VALID_ROOM.replace("###", "##"), false);
        expectError(sample, "0.json", "tileRows[0] has width 2");

        sample.child("0.json").writeString(VALID_ROOM.replace("\"neighbors\":{}", "\"neighbors\":{\"right\":9}"), false);
        expectError(sample, "0.json", "neighbors.right references a room absent");

        sample.child("0.json").writeString(VALID_ROOM.replace("\"enemySpawns\":[]", "\"enemySpawns\":[{\"type\":\"default\",\"position\":{\"x\":3,\"y\":0}}]"), false);
        expectError(sample, "0.json", "position is outside the room");

        sample.child("0.json").writeString("{\"id\":0,\"tileRows\":[", false);
        expectError(sample, "0.json", "invalid JSON");

        sample.child("0.json").writeString(VALID_ROOM, false);
        sample.child("index.json").writeString("{\"rooms\":[1]}", false);
        expectError(sample, "index.json", "rooms must include room 0");
        System.out.println("ROOM VALIDATION PASSED: real locations and malformed room diagnostics");
    }

    private static void expectError(FileHandle directory, String file, String detail) {
        try {
            RoomValidator.validateLocation(directory);
            throw new AssertionError("Expected validation failure containing " + detail);
        } catch (IllegalArgumentException expected) {
            if (!expected.getMessage().contains(file) || !expected.getMessage().contains(detail))
                throw new AssertionError("Unclear validation error: " + expected.getMessage());
        }
    }
}
