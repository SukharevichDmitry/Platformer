package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.files.FileHandle;
import java.io.File;

/** Checks that authored open-air templates still form a valid route. */
public final class RoomTemplateCatalogCheck {
    public static void main(String[] args) {
        if (args.length != 2) throw new IllegalArgumentException("Expected assets and fixture directories");
        FileHandle generation = new FileHandle(new File(args[0], "generation"));
        GenerationProfile profile = GenerationProfile.load(generation.child("ruins.json"));
        FileHandle sample = generation.child("ruins.templates.json");
        RoomTemplateCatalog catalog = RoomTemplateCatalog.load(sample, profile);
        if (!catalog.locationId.equals("ruins") || catalog.requiredRoute.size() != 6 ||
            catalog.templates.size() != 11 || catalog.optionalBranches.size() != 1 ||
            catalog.routeFillers.size() != 3 || !catalog.requiredRoute.get(0).equals("entry") ||
            !catalog.requiredRoute.get(5).equals("exit_gate") ||
            catalog.templates.get("entry").door("left") == null ||
            catalog.optionalBranches.get(0).templateIds.size() != 2)
            throw new AssertionError("Expanded catalog lost its story route or branch");

        FileHandle fixture = new FileHandle(new File(args[1], "generation/templates.json"));
        fixture.parent().mkdirs();
        String valid = compactJson(sample.readString());
        check(fixture, profile, valid, "\"patternId\":\"passage\"", "\"patternId\":\"missing\"", "patternId is unknown");
        check(fixture, profile, valid, "\"narrativeEventId\":\"ruins_bell\"", "\"narrativeEventId\":null", "narrativeEventId is required");
        check(fixture, profile, valid, "\"anchorSide\":\"left\"", "\"anchorSide\":\"bottom\"", "anchorSide has no door");
        check(fixture, profile, valid, "\"anchorSide\":\"left\"", "\"anchorSide\":\"right\"", "anchorSide is already used by requiredRoute");
        check(fixture, profile, valid, "\"templateIds\":[\"cache\",\"ambush\"]", "\"templateIds\":[\"entry\"]", "references nonoptional template");
        check(fixture, profile, valid, "\"routeFillers\":[\"collapsed_passage\",\"guarded_hall\",\"ruined_workshop\"]",
            "\"routeFillers\":[\"cache\",\"guarded_hall\",\"ruined_workshop\"]", "routeFillers template cache needs left and right doors");
        System.out.println("ROOM TEMPLATE CATALOG PASSED: larger open-air route and malformed template diagnostics");
    }

    private static void check(FileHandle fixture, GenerationProfile profile, String valid,
                              String before, String after, String detail) {
        if (!valid.contains(before)) throw new AssertionError("Missing fixture target: " + before);
        fixture.writeString(valid.replace(before, after), false);
        try {
            RoomTemplateCatalog.load(fixture, profile);
            throw new AssertionError("Expected catalog error containing " + detail);
        } catch (IllegalArgumentException expected) {
            if (!expected.getMessage().contains(fixture.name()) || !expected.getMessage().contains(detail))
                throw new AssertionError("Unclear catalog error: " + expected.getMessage());
        }
    }

    private static String compactJson(String source) {
        StringBuilder compact = new StringBuilder(source.length());
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (!quoted && Character.isWhitespace(c)) continue;
            compact.append(c);
            if (c == '"' && !escaped) quoted = !quoted;
            if (quoted && c == '\\' && !escaped) escaped = true;
            else escaped = false;
        }
        return compact.toString();
    }
}
