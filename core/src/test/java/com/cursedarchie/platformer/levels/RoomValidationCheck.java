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
        RoomValidator.validateLocation(levels.child("keeper_chamber"));
        RoomValidator.validateLocation(levels.child("laundry_alley"));
        RoomValidator.validateLocation(levels.child("laundry_house"));
        RoomValidator.validateLocation(levels.child("catchment_cellar"));
        RoomValidator.validateLocation(levels.child("transfer_gate"));

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

        String interaction = "{\"id\":\"note\",\"kind\":\"evidence\",\"visualId\":\"sealed_note\",\"eventId\":\"note_read\",\"position\":{\"x\":1.5,\"y\":0.5}}";
        sample.child("0.json").writeString(VALID_ROOM.replace("\"neighbors\":{}", "\"interactions\":[" + interaction + "],\"neighbors\":{}"), false);
        RoomValidator.validateLocation(sample);
        sample.child("0.json").writeString(VALID_ROOM.replace("\"neighbors\":{}", "\"interactions\":[" + interaction + "," + interaction + "],\"neighbors\":{}"), false);
        expectError(sample, "0.json", "id is invalid or duplicated");
        sample.child("0.json").writeString(VALID_ROOM.replace("\"neighbors\":{}", "\"interactions\":[" + interaction.replace("\"x\":1.5", "\"x\":0") + "],\"neighbors\":{}"), false);
        expectError(sample, "0.json", "must be placed in empty room space");
        sample.child("0.json").writeString(VALID_ROOM.replace("\"neighbors\":{}", "\"interactions\":[" + interaction.replace("\"x\":1.5", "\"x\":1.1") + "],\"neighbors\":{}"), false);
        expectError(sample, "0.json", "needs clearance from walls");

        String portal = "{\"id\":\"exit\",\"kind\":\"portal\",\"visualId\":\"street_exit\",\"eventId\":\"left\","
            + "\"position\":{\"x\":1.5,\"y\":0.5},\"returnToOrigin\":true,\"transition\":\"no_such_turn\"}";
        sample.child("0.json").writeString(VALID_ROOM.replace("\"neighbors\":{}", "\"interactions\":[" + portal + "],\"neighbors\":{}"), false);
        expectError(sample, "0.json", "portal transition is unknown: no_such_turn");

        // Every shipped transition hides the hero at its swap; a profile that would show the jump is rejected.
        com.badlogic.gdx.utils.ObjectMap<String, TransitionProfile> profiles =
            TransitionProfile.load(new FileHandle(new File(args[0], TransitionProfile.FILE)));
        if (!profiles.containsKey("alley_enter") || !profiles.containsKey("alley_exit") ||
            profiles.get("alley_enter").direction != 1 || profiles.get("alley_exit").direction != -1)
            throw new AssertionError("Alley transitions must turn counter-clockwise in and clockwise out");
        try {
            TransitionProfile.parse("{\"bad\":{\"switchAt\":0.9,\"coverFrom\":0.3,\"coverTo\":0.6}}");
            throw new AssertionError("A transition that swaps outside its cover window was accepted");
        } catch (IllegalArgumentException expected) {
            if (!expected.getMessage().contains("hide the hero")) throw expected;
        }

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
