package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.files.FileHandle;
import java.io.File;

/** Checks the production profile and rejects malformed generation contracts. */
public final class GenerationProfileCheck {
    public static void main(String[] args) {
        if (args.length != 2) throw new IllegalArgumentException("Expected assets and fixture directories");
        FileHandle sample = new FileHandle(new File(args[0], "generation/ruins.json"));
        GenerationProfile profile = GenerationProfile.load(sample);
        if (profile.version != 10 || !"ruins".equals(profile.locationId) ||
            !"start".equals(profile.returnLocation) || profile.returnRoomId != 3 ||
            !"keeper_chamber".equals(profile.forwardLocation) || profile.forwardRoomId != 0 ||
            !"production".equals(profile.status) || profile.tuningTargets == null ||
            profile.roomCount.min != 11 || profile.roomCount.max != 14 ||
            profile.roomPatterns.size() != 2 || profile.roomPatterns.get(0).width.min != 40 ||
            profile.roomPatterns.get(1).width.min != 56 || profile.difficultyBudget != 23)
            throw new AssertionError("Production ruins profile lost its larger open-air constraints");

        FileHandle fixture = new FileHandle(new File(args[1], "generation/profile.json"));
        fixture.parent().mkdirs();
        String valid = sample.readString().replaceAll("\\s+", "");
        check(fixture, valid, "\"roomCount\":{\"min\":11,\"max\":14",
            "\"roomCount\":{\"min\":15,\"max\":14", "roomCount");
        check(fixture, valid, "\"version\":10", "\"version\":0", "version");
        check(fixture, valid, "\"forwardTransition\":{\"location\":\"keeper_chamber\",\"id\":0}",
            "\"forwardTransition\":{\"location\":\"ruins\",\"id\":0}", "forwardTransition");
        check(fixture, valid, "\"status\":\"production\"", "\"status\":\"released\"", "status");
        check(fixture, valid, "\"combatRoomPercent\":{\"min\":40,\"max\":65}",
            "\"combatRoomPercent\":{\"min\":66,\"max\":65}", "tuningTargets.combatRoomPercent");
        check(fixture, valid, "\"default\",\"scavenger\"", "\"default\",\"enemy2\"", "unsupported type enemy2");
        check(fixture, valid, "\"id\":\"arena\"", "\"id\":\"passage\"", "duplicates passage");
        check(fixture, valid, "\"enemySpawns\":{\"min\":0,\"max\":1}",
            "\"enemySpawns\":{\"min\":2,\"max\":1}", "enemySpawns");
        check(fixture, valid, "\"difficultyBudget\":23", "\"difficultyBudget\":-1", "difficultyBudget");
        check(fixture, valid, "\"returnTransition\":{\"location\":\"start\",\"id\":3}",
            "\"returnTransition\":{\"location\":\"ruins\",\"id\":0}", "returnTransition");
        fixture.writeString("{broken", false);
        expectError(fixture, "invalid JSON");
        System.out.println("GENERATION PROFILE PASSED: expanded ruins and malformed profile diagnostics");
    }

    private static void check(FileHandle fixture, String valid, String before, String after, String error) {
        if (!valid.contains(before)) throw new AssertionError("Missing fixture target: " + before);
        fixture.writeString(valid.replace(before, after), false);
        expectError(fixture, error);
    }

    private static void expectError(FileHandle file, String detail) {
        try {
            GenerationProfile.load(file);
            throw new AssertionError("Expected profile error containing " + detail);
        } catch (IllegalArgumentException expected) {
            if (!expected.getMessage().contains(file.name()) || !expected.getMessage().contains(detail))
                throw new AssertionError("Unclear profile error: " + expected.getMessage());
        }
    }
}
