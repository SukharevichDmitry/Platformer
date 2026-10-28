package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.files.FileHandle;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.world.GameWorld;
import com.cursedarchie.platformer.levels.wrappers.RoomData;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.tiles.TileMetrics;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Checks seeded tile pieces and grounded hero routes across many layouts. */
public final class RoomTileGenerationCheck {
    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("Expected assets directory");
        FileHandle generation = new FileHandle(new File(args[0], "generation"));
        GenerationProfile profile = GenerationProfile.load(generation.child("ruins.json"));
        RoomTemplateCatalog catalog = RoomTemplateCatalog.load(generation.child("ruins.templates.json"), profile);
        RoomGraphGenerator graphGenerator = new RoomGraphGenerator();
        RoomLayoutPlacer placer = new RoomLayoutPlacer();
        RoomTileGenerator tileGenerator = new RoomTileGenerator();
        String firstSignature = null;
        boolean varied = false;
        Set<String> entryLayouts = new HashSet<>();
        Set<String> storyLayouts = new HashSet<>();
        int addedPlatforms = 0;
        int addedOneWay = 0;
        int addedBreakable = 0;
        int addedCover = 0;
        float nominalJumpRise = Hero.JUMP_SPEED * Hero.JUMP_SPEED / (2f * GameWorld.GRAVITY);
        if (nominalJumpRise < 6f)
            throw new AssertionError("Jump cannot reach the highest intended template ledges");
        for (long seed = 0; seed < 1000; seed++) {
            PlacedRoomGraph layout = placer.place(graphGenerator.generate(profile, catalog, seed), catalog, profile);
            GeneratedRoomTiles tiles = tileGenerator.generate(layout, catalog);
            GeneratedRoomTiles repeated = tileGenerator.generate(layout, catalog);
            if (!tiles.tileSignature().equals(repeated.tileSignature()))
                throw new AssertionError("Seed " + seed + " changed its tile map");
            RoomRouteReachability.validate(tiles);
            if (firstSignature == null) firstSignature = tiles.tileSignature();
            else if (!firstSignature.equals(tiles.tileSignature())) varied = true;
            for (PlacedRoomGraph.PlacedRoom room : layout.rooms) {
                RoomData data = new RoomData();
                data.width = room.width;
                data.height = room.height;
                data.tileRows = tiles.roomRows(room.id).toArray(new String[0]);
                RoomInstance physicalRoom = new RoomInstance(data);
                physicalRoom.buildTiles();
                if (seed == 0) {
                    for (int row = 0; row < room.height; row++) {
                        for (int x = 0; x < room.width; x++) {
                            char symbol = data.tileRows[row].charAt(x);
                            Tile cell = physicalRoom.getTiles()[x][room.height - 1 - row];
                            if ((symbol == ' ') != (cell == null))
                                throw new AssertionError("One map character did not map to one tile");
                            if (symbol == '#' && (cell.getBounds().x != x ||
                                cell.getBounds().width != 1f))
                                throw new AssertionError("Stone is not aligned to its map cell");
                        }
                    }
                }
                if (physicalRoom.getTiles().length != room.width ||
                    physicalRoom.getTiles()[0].length != room.height)
                    throw new AssertionError("Room tile grid differs from its world dimensions");
                if (RoomFineDecorator.decorate(physicalRoom,
                    catalog.templates.get(room.templateId)) != 0)
                    throw new AssertionError("Open-air room gained a ceiling rib");
                for (int fineX = 2; fineX < physicalRoom.getTileColumns() - 2; fineX++)
                    for (int fineY = 2; fineY < 6; fineY++)
                        if (physicalRoom.getTiles()[fineX][fineY] != null)
                            throw new AssertionError("Fine decoration blocked the walking corridor");
                Tile firstStone = physicalRoom.getTiles()[0][0];
                if (firstStone != null && firstStone.getBounds().width != TileMetrics.SIZE)
                    throw new AssertionError("Stone cell has the old width");
                if (room.templateId.equals("entry"))
                    entryLayouts.add(String.join("/", tiles.roomRows(room.id)));
                if (room.templateId.equals("story_shrine"))
                    storyLayouts.add(String.join("/", tiles.roomRows(room.id)));
                for (int y = 0; y < room.height; y++) {
                    String before = room.tileRows.get(y);
                    String after = tiles.roomRows(room.id).get(y);
                    for (int x = 0; x < room.width; x++) {
                        if (before.charAt(x) == after.charAt(x)) continue;
                        if (before.charAt(x) != ' ' ||
                            (after.charAt(x) != '_' && after.charAt(x) != '#' &&
                                after.charAt(x) != '=' && after.charAt(x) != 'B') ||
                            y >= room.height - 6)
                            throw new AssertionError("Seed " + seed + " changed a protected tile");
                        if (after.charAt(x) == '_' || after.charAt(x) == '=' || after.charAt(x) == 'B') {
                            int ledgeHeight = room.height - 1 - y;
                            if (ledgeHeight != 6 && ledgeHeight != 7 && ledgeHeight != 10)
                                throw new AssertionError("Generated ledge is outside the reachable tiers");
                            addedPlatforms++;
                            if (after.charAt(x) == '=') addedOneWay++;
                            if (after.charAt(x) == 'B') addedBreakable++;
                        }
                        else addedCover++;
                    }
                }
            }
            if (seed == 0) {
                List<List<String>> blocked = new ArrayList<>(tiles.rooms);
                List<String> rows = new ArrayList<>(blocked.get(0));
                int rowIndex = layout.room(0).height - 3;
                char[] row = rows.get(rowIndex).toCharArray();
                row[2] = '#';
                rows.set(rowIndex, new String(row));
                blocked.set(0, rows);
                try {
                    RoomRouteReachability.validate(new GeneratedRoomTiles(layout, blocked));
                    throw new AssertionError("Blocked walking lane passed reachability");
                } catch (IllegalArgumentException expected) {
                    if (!expected.getMessage().contains("walking corridor blocked")) throw expected;
                }
            }
        }
        if (!varied || entryLayouts.size() < 8 || storyLayouts.size() < 4 ||
            addedPlatforms == 0 || addedOneWay == 0 ||
            addedBreakable == 0 || addedCover == 0)
            throw new AssertionError("Seeded rooms lack visible layout variation");
        System.out.println("ROOM TILES PASSED: 1000 seeds, " + entryLayouts.size() +
            " entry and " + storyLayouts.size() +
            " story layouts, open-air motifs and reachable routes");
    }
}
