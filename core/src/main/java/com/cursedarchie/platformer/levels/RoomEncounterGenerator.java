package com.cursedarchie.platformer.levels;

import com.cursedarchie.platformer.actors.enemies.EnemyFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Selects enemies only after the final tile rows and door joins are known. */
public final class RoomEncounterGenerator {
    public GeneratedRoomEncounters generate(GeneratedRoomTiles tiles, RoomTemplateCatalog catalog,
                                            GenerationProfile profile) {
        if (tiles == null || catalog == null || profile == null ||
            !tiles.layout.locationId.equals(catalog.locationId) ||
            !tiles.layout.locationId.equals(profile.locationId))
            throw new IllegalArgumentException("Matching tiles, template catalog, and profile are required");
        List<List<GeneratedRoomEncounters.Spawn>> rooms = new ArrayList<>();
        int remaining = profile.difficultyBudget;
        for (PlacedRoomGraph.PlacedRoom room : tiles.layout.rooms) {
            RoomTemplateCatalog.RoomTemplate template = template(catalog, room);
            GenerationProfile.RoomPattern pattern = pattern(profile, template);
            int futureMinimum = 0;
            for (int id = room.id + 1; id < tiles.layout.rooms.size(); id++) {
                RoomTemplateCatalog.RoomTemplate future = template(catalog, tiles.layout.room(id));
                futureMinimum += minimumCost(future, pattern(profile, future));
            }
            int available = Math.min(pattern.difficultyBudget, remaining - futureMinimum);
            if (available < minimumCost(template, pattern))
                throw invalid(tiles, room.id, "difficulty budget cannot satisfy mandatory encounter");
            List<GeneratedRoomEncounters.Spawn> spawns = new ArrayList<>();
            Random random = new Random(tiles.layout.seed ^ (room.id * 0xD1B54A32D192ED03L) ^ 0x1F123BB5L);
            int minimum = minimumCount(template, pattern);
            int desired = template.encounterRole.equals("safe") ? 0 :
                minimum + random.nextInt(pattern.enemySpawns.max - minimum + 1);
            for (int index = 0; index < desired; index++) {
                String type = selectType(template, pattern, index, random, available);
                if (type == null) {
                    if (index < minimum) throw invalid(tiles, room.id, "difficulty budget prevents required spawn");
                    break;
                }
                GeneratedRoomEncounters.Spawn spawn = chooseSpawn(tiles, room, template, spawns, type, random);
                if (spawn == null) {
                    if (index < minimum) throw invalid(tiles, room.id, "no safe terrain for required spawn");
                    break;
                }
                spawns.add(spawn);
                int cost = EnemyFactory.definition(type).difficultyCost;
                available -= cost;
                remaining -= cost;
            }
            rooms.add(spawns);
        }
        GeneratedRoomEncounters result = new GeneratedRoomEncounters(tiles, rooms);
        validate(result, catalog, profile);
        return result;
    }

    public static void validate(GeneratedRoomEncounters encounters, RoomTemplateCatalog catalog,
                                GenerationProfile profile) {
        if (encounters == null || catalog == null || profile == null ||
            encounters.rooms.size() != encounters.tiles.layout.rooms.size())
            throw new IllegalArgumentException("Encounters must match the placed rooms");
        int locationCost = 0;
        for (PlacedRoomGraph.PlacedRoom room : encounters.tiles.layout.rooms) {
            RoomTemplateCatalog.RoomTemplate template = template(catalog, room);
            GenerationProfile.RoomPattern pattern = pattern(profile, template);
            List<GeneratedRoomEncounters.Spawn> spawns = encounters.roomSpawns(room.id);
            if (spawns.size() < minimumCount(template, pattern) ||
                spawns.size() > pattern.enemySpawns.max)
                throw invalid(encounters.tiles, room.id, "spawn count violates pattern or encounter role");
            int roomCost = 0;
            int bosses = 0;
            for (GeneratedRoomEncounters.Spawn spawn : spawns) {
                if (!profile.allowedEnemyTypes.contains(spawn.type) ||
                    !pattern.allowedEnemyTypes.contains(spawn.type) ||
                    !EnemyFactory.supportsType(spawn.type) ||
                    (!template.encounterRole.equals("boss") && spawn.type.equals("boss")))
                    throw invalid(encounters.tiles, room.id, "enemy type violates palette or encounter role");
                if (spawn.type.equals("boss")) bosses++;
                if (!safePosition(encounters.tiles, room, template, spawn))
                    throw invalid(encounters.tiles, room.id, "spawn violates terrain, entry, or story clearance");
                for (GeneratedRoomEncounters.Spawn previous : spawns) {
                    if (previous == spawn) break;
                    float spacing = (EnemyFactory.definition(previous.type).bodySize +
                        EnemyFactory.definition(spawn.type).bodySize) / 2f + 2f;
                    if (Math.abs(previous.x - spawn.x) < spacing)
                        throw invalid(encounters.tiles, room.id, "enemy spawns overlap");
                }
                roomCost += EnemyFactory.definition(spawn.type).difficultyCost;
            }
            if (template.encounterRole.equals("boss") && bosses != 1)
                throw invalid(encounters.tiles, room.id, "boss encounter needs exactly one boss");
            if (roomCost > pattern.difficultyBudget)
                throw invalid(encounters.tiles, room.id, "room difficulty budget exceeded");
            locationCost += roomCost;
        }
        if (locationCost > profile.difficultyBudget)
            throw new IllegalArgumentException("Seed " + encounters.tiles.layout.seed +
                ": location difficulty budget exceeded");
    }

    private static GeneratedRoomEncounters.Spawn chooseSpawn(GeneratedRoomTiles tiles,
            PlacedRoomGraph.PlacedRoom room, RoomTemplateCatalog.RoomTemplate template,
            List<GeneratedRoomEncounters.Spawn> existing, String type, Random random) {
        List<Integer> positions = new ArrayList<>();
        for (int x = 6; x <= room.width - 8; x++) positions.add(x);
        Collections.shuffle(positions, random);
        float size = EnemyFactory.definition(type).bodySize;
        for (int tileX : positions) {
            GeneratedRoomEncounters.Spawn spawn = new GeneratedRoomEncounters.Spawn(
                type, tileX + 0.5f, 2f + size / 2f + 0.1f);
            if (!safePosition(tiles, room, template, spawn)) continue;
            boolean separated = true;
            for (GeneratedRoomEncounters.Spawn previous : existing) {
                float spacing = (EnemyFactory.definition(previous.type).bodySize + size) / 2f + 2f;
                if (Math.abs(previous.x - spawn.x) < spacing) separated = false;
            }
            if (separated) return spawn;
        }
        return null;
    }

    private static boolean safePosition(GeneratedRoomTiles tiles, PlacedRoomGraph.PlacedRoom room,
                                        RoomTemplateCatalog.RoomTemplate template,
                                        GeneratedRoomEncounters.Spawn spawn) {
        float size = EnemyFactory.definition(spawn.type).bodySize;
        if (spawn.x < 7f || spawn.x > room.width - 7f ||
            Math.abs(spawn.y - (2f + size / 2f + 0.1f)) > 0.001f ||
            (template.narrativeEventId != null &&
                Math.abs(spawn.x - room.width / 2f) < 4f + size / 2f)) return false;
        int left = (int) Math.floor(spawn.x - size / 2f);
        int right = (int) Math.floor(spawn.x + size / 2f);
        int top = (int) Math.floor(spawn.y + size / 2f);
        if (left < 1 || right >= room.width - 1 || top >= room.height) return false;
        List<String> rows = tiles.roomRows(room.id);
        for (int x = left; x <= right; x++) {
            if (rows.get(room.height - 1).charAt(x) != '#' ||
                rows.get(room.height - 2).charAt(x) != '#') return false;
            for (int y = 2; y <= top; y++)
                if (rows.get(room.height - 1 - y).charAt(x) != ' ') return false;
        }
        return true;
    }

    private static String selectType(RoomTemplateCatalog.RoomTemplate template,
            GenerationProfile.RoomPattern pattern, int index, Random random, int available) {
        if (template.encounterRole.equals("boss") && index == 0)
            return available >= EnemyFactory.definition("boss").difficultyCost ? "boss" : null;
        List<String> types = new ArrayList<>();
        for (String type : pattern.allowedEnemyTypes)
            if (!type.equals("boss") && EnemyFactory.definition(type).difficultyCost <= available)
                types.add(type);
        return types.isEmpty() ? null : types.get(random.nextInt(types.size()));
    }

    private static int minimumCount(RoomTemplateCatalog.RoomTemplate template,
                                    GenerationProfile.RoomPattern pattern) {
        if (template.encounterRole.equals("safe")) return 0;
        return Math.max(1, pattern.enemySpawns.min);
    }

    private static int minimumCost(RoomTemplateCatalog.RoomTemplate template,
                                   GenerationProfile.RoomPattern pattern) {
        int count = minimumCount(template, pattern);
        if (count == 0) return 0;
        int cheapest = Integer.MAX_VALUE;
        for (String type : pattern.allowedEnemyTypes)
            if (!type.equals("boss"))
                cheapest = Math.min(cheapest, EnemyFactory.definition(type).difficultyCost);
        if (template.encounterRole.equals("boss"))
            return EnemyFactory.definition("boss").difficultyCost +
                (count - 1) * (count > 1 ? cheapest : 0);
        return count * cheapest;
    }

    private static RoomTemplateCatalog.RoomTemplate template(RoomTemplateCatalog catalog,
                                                               PlacedRoomGraph.PlacedRoom room) {
        RoomTemplateCatalog.RoomTemplate template = catalog.templates.get(room.templateId);
        if (template == null) throw new IllegalArgumentException("Unknown template " + room.templateId);
        return template;
    }

    private static GenerationProfile.RoomPattern pattern(GenerationProfile profile,
                                                            RoomTemplateCatalog.RoomTemplate template) {
        for (GenerationProfile.RoomPattern pattern : profile.roomPatterns)
            if (pattern.id.equals(template.patternId)) return pattern;
        throw new IllegalArgumentException("Unknown pattern " + template.patternId);
    }

    private static IllegalArgumentException invalid(GeneratedRoomTiles tiles, int roomId, String detail) {
        return new IllegalArgumentException("Seed " + tiles.layout.seed + " room " + roomId + ": " + detail);
    }
}
