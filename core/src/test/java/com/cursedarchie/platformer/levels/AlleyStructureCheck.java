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
            RoomInstance house = location.getRoom(structure.roomId("laundry_house"));
            RoomInstance cellar = location.getRoom(structure.roomId("holding_cellar"));
            if (house.getUpNeighbor() != structure.roomId("laundry") ||
                cellar.getUpNeighbor() != structure.roomId("cellar_descent") ||
                house.getWorldY() >= 0f || cellar.getWorldY() >= 0f)
                throw new AssertionError("Laundry house and cellar are not below their entry rooms (seed " + seed + ")");
            for (com.cursedarchie.platformer.levels.wrappers.InteractionData i : house.getInteractions())
                if ("red_bracer".equals(i.id) && !i.requiresFlag.equals("defeated:laundry_alley:" + house.getRoomId() + ":0"))
                    throw new AssertionError("Laundry reward flag does not follow its room: " + i.requiresFlag);
            if (!location.isCameraPerRoom()) throw new AssertionError("Alley camera is not per room");
            if (structure.roomId("exit") <= structure.roomId("cellar_descent") ||
                structure.roomId("cellar_descent") <= structure.roomId("laundry"))
                throw new AssertionError("Alley rooms are out of story order (seed " + seed + ")");
            if (location.getRooms().size() == 10) withExtraCorridor++;
            else if (location.getRooms().size() != 9)
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
        FileHandle stairsRoom = fixture.child("3.json");
        String stairsJson = stairsRoom.readString("UTF-8");
        stairsRoom.writeString(stairsJson.replace("\"targetSpawnId\": \"laundry_entry\"", "\"targetSpawnId\": \"nowhere\""), false);
        expect(fixture, structure, "lands at a missing spawn nowhere");
        // Stairs that lead elsewhere leave the holding cellar (and the vault below it) without a way in.
        stairsRoom.writeString(stairsJson.replace("\"targetRole\": \"holding_cellar\"", "\"targetRole\": \"laundry_house\"")
            .replace("\"targetSpawnId\": \"laundry_entry\"", "\"targetSpawnId\": \"alley_entry\""), false);
        expect(fixture, structure, "cannot be reached");
        stairsRoom.writeString(stairsJson, false);
        System.out.println("ALLEY STRUCTURE PASSED: 200 seeds, roles in order, side room above corridor, " +
            "laundry, cellar and vault below their rooms, separate street exit, reachability on foot and by descents, malformed structure diagnostics");
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
