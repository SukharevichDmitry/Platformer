package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.files.FileHandle;

import java.io.File;

/** Checks authored generation templates without creating a physics world. */
public final class RoomTemplateCatalogCheck {
    public static void main(String[] args) {
        if (args.length != 2) throw new IllegalArgumentException("Expected assets and fixture directories");
        FileHandle generation = new FileHandle(new File(args[0], "generation"));
        GenerationProfile profile = GenerationProfile.load(generation.child("ruins.json"));
        FileHandle sample = generation.child("ruins.templates.json");
        RoomTemplateCatalog catalog = RoomTemplateCatalog.load(sample, profile);
        if (!catalog.locationId.equals("ruins") || catalog.requiredRoute.size() != 4 ||
            catalog.templates.size() != 8 || catalog.optionalBranches.size() != 1 ||
            catalog.routeFillers.size() != 2 ||
            !catalog.requiredRoute.get(0).equals("entry") ||
            !catalog.requiredRoute.get(3).equals("exit_gate") ||
            catalog.templates.get("entry").door("left") == null ||
            catalog.optionalBranches.get(0).templateIds.size() != 2)
            throw new AssertionError("Sample catalog lost its story route or optional branch");

        FileHandle fixture = new FileHandle(new File(args[1], "generation/templates.json"));
        fixture.parent().mkdirs();
        String valid = sample.readString();

        fixture.writeString(valid.replace("\"patternId\": \"passage\"", "\"patternId\": \"missing\""), false);
        expectError(fixture, profile, "patternId is unknown");

        fixture.writeString(valid.replace("\"narrativeEventId\": \"ruins_bell\"", "\"narrativeEventId\": null"), false);
        expectError(fixture, profile, "narrativeEventId is required");

        fixture.writeString(valid.replace("\"#        \"", "\"#########\""), false);
        expectError(fixture, profile, "is blocked by a tile");

        fixture.writeString(valid.replace("\"requiredRoute\": [\"entry\", \"bell_hall\", \"story_shrine\", \"exit_gate\"]",
            "\"requiredRoute\": [\"entry\", \"story_shrine\", \"bell_hall\", \"exit_gate\"]"), false);
        expectError(fixture, profile, "requiredRoute must run start, landmark, story, exit");

        fixture.writeString(valid.replace("\"anchorSide\": \"left\"", "\"anchorSide\": \"bottom\""), false);
        expectError(fixture, profile, "anchorSide has no door");

        fixture.writeString(valid.replace("\"anchorSide\": \"left\"", "\"anchorSide\": \"right\""), false);
        expectError(fixture, profile, "anchorSide is already used by requiredRoute");

        fixture.writeString(valid.replace("\"templateIds\": [\"cache\", \"ambush\"]", "\"templateIds\": [\"entry\"]"), false);
        expectError(fixture, profile, "references nonoptional template");

        fixture.writeString(valid.replace("\"routeFillers\": [\"collapsed_passage\", \"guarded_hall\"]",
            "\"routeFillers\": [\"cache\", \"guarded_hall\"]"), false);
        expectError(fixture, profile, "routeFillers template cache needs left and right doors");

        System.out.println("ROOM TEMPLATE CATALOG PASSED: required route, optional branch and malformed template diagnostics");
    }

    private static void expectError(FileHandle file, GenerationProfile profile, String detail) {
        try {
            RoomTemplateCatalog.load(file, profile);
            throw new AssertionError("Expected catalog error containing " + detail);
        } catch (IllegalArgumentException expected) {
            if (!expected.getMessage().contains(file.name()) || !expected.getMessage().contains(detail))
                throw new AssertionError("Unclear catalog error: " + expected.getMessage());
        }
    }
}
