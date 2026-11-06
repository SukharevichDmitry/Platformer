package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.files.FileHandle;

import java.io.File;

/** Headless check of the multi-room alley: roles, separate exit, side room above a corridor, reachability. */
public final class AlleyStructureCheck {
    public static void main(String[] args) {
        if (args.length != 2) throw new IllegalArgumentException("Expected assets and fixture directories");
        FileHandle alley = new FileHandle(new File(args[0], "levels/laundry_alley"));
        int withExtraCorridor = 0;
        for (long seed = 0; seed < 200; seed++) {
            AlleyStructure structure = AlleyStructure.build(alley, seed);
            LocationInstance location = structure.location();
            int side = structure.roomId("side_room"), corridor = structure.roomId("corridor");
            RoomInstance sideRoom = location.getRoom(side), below = location.getRoom(corridor);
            if (sideRoom.getDownNeighbor() == null || sideRoom.getDownNeighbor() != corridor ||
                sideRoom.getWorldY() != below.getWorldY() + below.getHeight())
                throw new AssertionError("Side room does not sit on the first corridor (seed " + seed + ")");
            if (structure.roomId("exit") <= structure.roomId("cellar_descent") ||
                structure.roomId("cellar_descent") <= structure.roomId("laundry"))
                throw new AssertionError("Alley rooms are out of story order (seed " + seed + ")");
            if (location.getRooms().size() == 7) withExtraCorridor++;
            else if (location.getRooms().size() != 6)
                throw new AssertionError("Unexpected alley size " + location.getRooms().size());
        }
        if (withExtraCorridor < 60 || withExtraCorridor > 140)
            throw new AssertionError("Optional corridor appears in " + withExtraCorridor + " of 200 seeds");
        if (AlleyStructure.build(alley, 7L).location().getRooms().size() !=
            AlleyStructure.build(alley, 7L).location().getRooms().size())
            throw new AssertionError("Alley is not reproducible from its seed");

        // Malformed structures fail with a clear reason.
        // The validator follows portals into sibling locations, so the fixture carries every level directory.
        FileHandle fixtureLevels = new FileHandle(new File(args[1], "alley-levels/levels"));
        fixtureLevels.parent().deleteDirectory();
        for (FileHandle level : alley.parent().list()) level.copyTo(fixtureLevels);
        FileHandle fixture = fixtureLevels.child("laundry_alley");
        new FileHandle(new File(args[0], TransitionProfile.FILE))
            .copyTo(new FileHandle(new File(args[1], "alley-levels/" + TransitionProfile.FILE)));
        String structure = alley.child(AlleyStructure.FILE).readString("UTF-8");
        expect(fixture, structure.replace("\"role\": \"exit\",\n      \"template\": 4", "\"role\": \"exit\",\n      \"template\": 0"),
            "exit portal needs its own targetSpawnId on the street");
        expect(fixture, structure.replace("\"over\": \"corridor\"", "\"over\": \"laundry\""), "cannot be reached on foot");
        expect(fixture, structure.replace("\"side_room\",\n    \"laundry\"", "\"side_room\",\n    \"rooftop\",\n    \"laundry\""),
            "required room role is missing: rooftop");
        System.out.println("ALLEY STRUCTURE PASSED: 200 seeds, roles in order, side room above corridor, " +
            "separate street exit, on-foot reachability, malformed structure diagnostics");
    }

    private static void expect(FileHandle fixture, String structure, String detail) {
        fixture.child(AlleyStructure.FILE).writeString(structure, false);
        try {
            AlleyStructure.build(fixture, 1L);
            throw new AssertionError("Expected alley structure failure containing: " + detail);
        } catch (IllegalArgumentException expected) {
            if (!expected.getMessage().contains(detail))
                throw new AssertionError("Unclear alley structure error: " + expected.getMessage());
        }
    }
}
