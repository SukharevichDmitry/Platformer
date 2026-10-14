package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.files.FileHandle;

import java.io.File;

/** Standalone profile check that needs no graphics context. */
public final class GenerationProfileCheck {
    public static void main(String[] args) {
        if (args.length != 2) throw new IllegalArgumentException("Expected assets and fixture directories");
        FileHandle sample = new FileHandle(new File(args[0], "generation/ruins.json"));
        GenerationProfile profile = GenerationProfile.load(sample);
        if (profile.version != 2 || !"ruins".equals(profile.locationId) ||
            !"production".equals(profile.status) || profile.tuningTargets == null ||
            profile.tuningTargets.combatRoomPercent.min != 30 || profile.roomCount.min != 5 ||
            profile.roomCount.max != 10 || profile.allowedEnemyTypes.size() != 2 ||
            profile.roomPatterns.size() != 2 || profile.roomPatterns.get(0).width.max != 12 ||
            profile.roomPatterns.get(0).roomQuota.max != 7 ||
            profile.difficultyBudget != 12 || profile.roomPatterns.get(1).difficultyBudget != 4)
            throw new AssertionError("Sample generation profile did not load its constraints");

        FileHandle fixture = new FileHandle(new File(args[1], "generation/profile.json"));
        fixture.parent().mkdirs();
        String valid = sample.readString();
        fixture.writeString(valid.replace("\"min\": 5, \"max\": 10", "\"min\": 11, \"max\": 10"), false);
        expectError(fixture, "roomCount");

        fixture.writeString(valid.replace("\"version\": 2", "\"version\": 0"), false);
        expectError(fixture, "version");

        fixture.writeString(valid.replace("\"status\": \"production\"", "\"status\": \"released\""), false);
        expectError(fixture, "status");

        fixture.writeString(valid.replace("\"combatRoomPercent\": {\"min\": 30, \"max\": 50}",
            "\"combatRoomPercent\": {\"min\": 51, \"max\": 50}"), false);
        expectError(fixture, "tuningTargets.combatRoomPercent");

        fixture.writeString(valid.replace("\"default\", \"boss\"", "\"default\", \"enemy2\""), false);
        expectError(fixture, "unsupported type enemy2");

        fixture.writeString(valid.replace("\"id\": \"arena\"", "\"id\": \"passage\""), false);
        expectError(fixture, "duplicates passage");

        fixture.writeString(valid.replace("\"min\": 0, \"max\": 1", "\"min\": 2, \"max\": 1"), false);
        expectError(fixture, "enemySpawns");

        fixture.writeString(valid.replace("\"difficultyBudget\": 12", "\"difficultyBudget\": -1"), false);
        expectError(fixture, "difficultyBudget");

        fixture.writeString(valid.replace("\"difficultyBudget\": 4", "\"difficultyBudget\": 0"), false);
        expectError(fixture, "difficultyBudget cannot satisfy enemySpawns");

        fixture.writeString(valid.replace("\"returnTransition\": {\"location\": \"forest\", \"id\": 0}",
            "\"returnTransition\": {\"location\": \"ruins\", \"id\": 0}"), false);
        expectError(fixture, "returnTransition");

        fixture.writeString(valid.replace("\"min\": 2, \"max\": 7", "\"min\": 11, \"max\": 11"), false);
        expectError(fixture, "roomQuota totals cannot satisfy roomCount");

        fixture.writeString("{broken", false);
        expectError(fixture, "invalid JSON");
        System.out.println("GENERATION PROFILE PASSED: constrained sample and malformed profile diagnostics");
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
