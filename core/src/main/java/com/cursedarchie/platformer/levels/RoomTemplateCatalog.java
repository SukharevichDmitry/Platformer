package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Authored geometry and story sockets for a future generated location. */
public final class RoomTemplateCatalog {
    public static final class Door {
        public final String side;
        public final int offset;
        public final int span;

        private Door(String side, int offset, int span) {
            this.side = side;
            this.offset = offset;
            this.span = span;
        }
    }

    public static final class RoomTemplate {
        public final String id;
        public final String patternId;
        public final String role;
        public final String biome;
        public final String encounterRole;
        public final String narrativeEventId;
        public final int width;
        public final int height;
        public final List<String> tileRows;
        public final List<Door> doors;

        private RoomTemplate(String id, String patternId, String role, String biome,
                             String encounterRole, String narrativeEventId, int width, int height,
                             List<String> tileRows, List<Door> doors) {
            this.id = id;
            this.patternId = patternId;
            this.role = role;
            this.biome = biome;
            this.encounterRole = encounterRole;
            this.narrativeEventId = narrativeEventId;
            this.width = width;
            this.height = height;
            this.tileRows = Collections.unmodifiableList(tileRows);
            this.doors = Collections.unmodifiableList(doors);
        }

        public Door door(String side) {
            for (Door door : doors) if (door.side.equals(side)) return door;
            return null;
        }
    }

    public static final class BranchSlot {
        public final String anchorTemplate;
        public final String anchorSide;
        public final List<String> templateIds;

        private BranchSlot(String anchorTemplate, String anchorSide, List<String> templateIds) {
            this.anchorTemplate = anchorTemplate;
            this.anchorSide = anchorSide;
            this.templateIds = Collections.unmodifiableList(templateIds);
        }
    }

    public final String locationId;
    public final Map<String, RoomTemplate> templates;
    public final List<String> requiredRoute;
    public final List<String> routeFillers;
    public final List<BranchSlot> optionalBranches;

    private RoomTemplateCatalog(String locationId, Map<String, RoomTemplate> templates,
                                List<String> requiredRoute, List<String> routeFillers,
                                List<BranchSlot> optionalBranches) {
        this.locationId = locationId;
        this.templates = Collections.unmodifiableMap(templates);
        this.requiredRoute = Collections.unmodifiableList(requiredRoute);
        this.routeFillers = Collections.unmodifiableList(routeFillers);
        this.optionalBranches = Collections.unmodifiableList(optionalBranches);
    }

    public static RoomTemplateCatalog load(FileHandle file, GenerationProfile profile) {
        if (profile == null) throw new IllegalArgumentException("profile is required");
        if (!file.exists()) throw invalid(file, "file is missing");
        if (file.length() > 512 * 1024) throw invalid(file, "file is too large");
        JsonValue root;
        try {
            root = new JsonReader().parse(file);
        } catch (RuntimeException e) {
            throw invalid(file, "invalid JSON: " + e.getMessage());
        }
        if (!root.isObject()) throw invalid(file, "root must be an object");
        String locationId = identifier(root.get("locationId"), file, "locationId");
        if (!locationId.equals(profile.locationId))
            throw invalid(file, "locationId must match profile " + profile.locationId);

        JsonValue nodes = root.get("templates");
        if (nodes == null || !nodes.isArray() || nodes.size < 4 || nodes.size > 64)
            throw invalid(file, "templates must contain 4 to 64 rooms");
        Map<String, RoomTemplate> templates = new LinkedHashMap<>();
        Set<String> narrativeEvents = new LinkedHashSet<>();
        for (JsonValue node : nodes) {
            String field = "templates[" + templates.size() + "]";
            RoomTemplate template = parseTemplate(node, file, field, profile);
            if (templates.putIfAbsent(template.id, template) != null)
                throw invalid(file, field + ".id duplicates " + template.id);
            if (template.narrativeEventId != null && !narrativeEvents.add(template.narrativeEventId))
                throw invalid(file, field + ".narrativeEventId duplicates " + template.narrativeEventId);
        }

        List<String> route = identifiers(root.get("requiredRoute"), file, "requiredRoute", 4, profile.roomCount.max);
        Set<String> routeIds = new LinkedHashSet<>(route);
        if (routeIds.size() != route.size()) throw invalid(file, "requiredRoute repeats a template");
        int landmark = -1;
        int story = -1;
        int starts = 0;
        int exits = 0;
        Map<String, Set<String>> routeSockets = new LinkedHashMap<>();
        for (int i = 0; i < route.size(); i++) {
            RoomTemplate template = templates.get(route.get(i));
            if (template == null) throw invalid(file, "requiredRoute references unknown template " + route.get(i));
            if (template.role.equals("optional"))
                throw invalid(file, "requiredRoute includes optional template " + template.id);
            if (template.role.equals("landmark")) landmark = i;
            if (template.role.equals("story")) story = i;
            if (template.role.equals("start")) starts++;
            if (template.role.equals("exit")) exits++;
            if (i > 0 && !reserveRouteConnection(templates.get(route.get(i - 1)), template, routeSockets))
                throw invalid(file, "requiredRoute has no matching doors between " + route.get(i - 1) + " and " + template.id);
        }
        if (!templates.get(route.get(0)).role.equals("start") ||
            !templates.get(route.get(route.size() - 1)).role.equals("exit") ||
            starts != 1 || exits != 1 || landmark < 1 || story <= landmark || story >= route.size() - 1)
            throw invalid(file, "requiredRoute must run start, landmark, story, exit in order");
        for (RoomTemplate template : templates.values()) {
            if (!template.role.equals("optional") && !routeIds.contains(template.id))
                throw invalid(file, "required template " + template.id + " is absent from requiredRoute");
        }

        JsonValue branchNodes = root.get("optionalBranches");
        if (branchNodes == null || !branchNodes.isArray() || branchNodes.size > 32)
            throw invalid(file, "optionalBranches must be an array of at most 32 slots");
        List<BranchSlot> branches = new ArrayList<>();
        Set<String> usedSockets = new LinkedHashSet<>();
        Set<String> optionalIds = new LinkedHashSet<>();
        for (JsonValue node : branchNodes) {
            String field = "optionalBranches[" + branches.size() + "]";
            if (!node.isObject()) throw invalid(file, field + " must be an object");
            String anchorId = identifier(node.get("anchorTemplate"), file, field + ".anchorTemplate");
            String side = side(node.get("anchorSide"), file, field + ".anchorSide");
            RoomTemplate anchor = templates.get(anchorId);
            if (anchor == null || !routeIds.contains(anchorId))
                throw invalid(file, field + ".anchorTemplate must be on requiredRoute");
            if (anchor.door(side) == null)
                throw invalid(file, field + ".anchorSide has no door on " + anchorId);
            if (routeSockets.get(anchorId) != null && routeSockets.get(anchorId).contains(side))
                throw invalid(file, field + ".anchorSide is already used by requiredRoute");
            if (!usedSockets.add(anchorId + ":" + side))
                throw invalid(file, field + " repeats branch socket " + anchorId + ":" + side);
            List<String> candidates = identifiers(node.get("templateIds"), file, field + ".templateIds", 1, 16);
            for (String candidateId : candidates) {
                RoomTemplate candidate = templates.get(candidateId);
                if (candidate == null || !candidate.role.equals("optional"))
                    throw invalid(file, field + " references nonoptional template " + candidateId);
                if (candidate.door(opposite(side)) == null)
                    throw invalid(file, field + " template " + candidateId + " lacks " + opposite(side) + " door");
                if (!optionalIds.add(candidateId))
                    throw invalid(file, field + " repeats optional template " + candidateId);
            }
            branches.add(new BranchSlot(anchorId, side, candidates));
        }
        List<String> fillers = identifiers(root.get("routeFillers"), file, "routeFillers", 0, 64);
        for (String fillerId : fillers) {
            RoomTemplate filler = templates.get(fillerId);
            if (filler == null || !filler.role.equals("optional"))
                throw invalid(file, "routeFillers references nonoptional template " + fillerId);
            if (filler.door("left") == null || filler.door("right") == null)
                throw invalid(file, "routeFillers template " + fillerId + " needs left and right doors");
            if (!optionalIds.add(fillerId))
                throw invalid(file, "routeFillers repeats branch template " + fillerId);
        }
        for (RoomTemplate template : templates.values()) {
            if (template.role.equals("optional") && !optionalIds.contains(template.id))
                throw invalid(file, "optional template " + template.id + " is not assigned to a branch or routeFillers");
        }
        if (profile.roomCount.min > route.size() && branches.isEmpty() && fillers.isEmpty())
            throw invalid(file, "roomCount.min needs optional rooms beyond the requiredRoute");
        return new RoomTemplateCatalog(locationId, templates, route, fillers, branches);
    }

    private static RoomTemplate parseTemplate(JsonValue node, FileHandle file, String field, GenerationProfile profile) {
        if (!node.isObject()) throw invalid(file, field + " must be an object");
        String id = identifier(node.get("id"), file, field + ".id");
        String patternId = identifier(node.get("patternId"), file, field + ".patternId");
        GenerationProfile.RoomPattern pattern = null;
        for (GenerationProfile.RoomPattern candidate : profile.roomPatterns)
            if (candidate.id.equals(patternId)) pattern = candidate;
        if (pattern == null) throw invalid(file, field + ".patternId is unknown: " + patternId);
        String role = string(node.get("role"), file, field + ".role");
        if (!role.equals("start") && !role.equals("landmark") && !role.equals("story") &&
            !role.equals("exit") && !role.equals("optional"))
            throw invalid(file, field + ".role is unknown: " + role);
        String biome = identifier(node.get("biome"), file, field + ".biome");
        String encounterRole = string(node.get("encounterRole"), file, field + ".encounterRole");
        if (!encounterRole.equals("safe") && !encounterRole.equals("combat") && !encounterRole.equals("boss"))
            throw invalid(file, field + ".encounterRole is unknown: " + encounterRole);
        if (encounterRole.equals("boss") && !pattern.allowedEnemyTypes.contains("boss"))
            throw invalid(file, field + ".encounterRole requires boss in pattern palette");
        if (!encounterRole.equals("safe") && pattern.enemySpawns.max == 0)
            throw invalid(file, field + ".encounterRole requires enemySpawns.max above zero");
        if (!encounterRole.equals("safe") && pattern.allowedEnemyTypes.stream().noneMatch(type -> !type.equals("boss")) &&
            (!encounterRole.equals("boss") || pattern.enemySpawns.min > 1))
            throw invalid(file, field + ".encounterRole requires a regular enemy type");
        if (encounterRole.equals("boss") && pattern.difficultyBudget <
            com.cursedarchie.platformer.actors.enemies.EnemyFactory.definition("boss").difficultyCost)
            throw invalid(file, field + ".encounterRole exceeds pattern difficultyBudget");
        if (encounterRole.equals("safe") && pattern.enemySpawns.min > 0)
            throw invalid(file, field + ".encounterRole safe conflicts with mandatory pattern spawns");
        String eventId = null;
        JsonValue event = node.get("narrativeEventId");
        if (event != null && !event.isNull()) eventId = identifier(event, file, field + ".narrativeEventId");
        if (!role.equals("optional") && eventId == null)
            throw invalid(file, field + ".narrativeEventId is required for story anchors");
        int width = integer(node.get("width"), file, field + ".width");
        int height = integer(node.get("height"), file, field + ".height");
        if (width < pattern.width.min || width > pattern.width.max ||
            height < pattern.height.min || height > pattern.height.max)
            throw invalid(file, field + " dimensions are outside pattern " + patternId);

        JsonValue rows = node.get("tileRows");
        if (rows == null || !rows.isArray() || rows.size != height)
            throw invalid(file, field + ".tileRows must have " + height + " rows");
        List<String> tileRows = new ArrayList<>();
        for (JsonValue row : rows) {
            String tiles = string(row, file, field + ".tileRows[" + tileRows.size() + "]");
            if (tiles.length() != width || !tiles.matches("[ #_]*"))
                throw invalid(file, field + ".tileRows[" + tileRows.size() + "] has invalid width or tile");
            tileRows.add(tiles);
        }

        JsonValue doorsNode = node.get("doors");
        if (doorsNode == null || !doorsNode.isArray() || doorsNode.size == 0 || doorsNode.size > 4)
            throw invalid(file, field + ".doors must contain 1 to 4 doors");
        List<Door> doors = new ArrayList<>();
        Set<String> sides = new LinkedHashSet<>();
        for (JsonValue doorNode : doorsNode) {
            String doorField = field + ".doors[" + doors.size() + "]";
            if (!doorNode.isObject()) throw invalid(file, doorField + " must be an object");
            String side = side(doorNode.get("side"), file, doorField + ".side");
            if (!sides.add(side)) throw invalid(file, doorField + " repeats side " + side);
            int offset = integer(doorNode.get("offset"), file, doorField + ".offset");
            int span = integer(doorNode.get("span"), file, doorField + ".span");
            int edgeLength = side.equals("left") || side.equals("right") ? height : width;
            if (offset < 1 || span < 2 || offset + span > edgeLength - 1)
                throw invalid(file, doorField + " must leave a corner and span at least two tiles");
            for (int i = offset; i < offset + span; i++) {
                int x = side.equals("left") ? 0 : side.equals("right") ? width - 1 : i;
                int y = side.equals("bottom") ? 0 : side.equals("top") ? height - 1 : i;
                int insideX = x + (side.equals("left") ? 1 : side.equals("right") ? -1 : 0);
                int insideY = y + (side.equals("bottom") ? 1 : side.equals("top") ? -1 : 0);
                if (tileRows.get(height - 1 - y).charAt(x) != ' ' ||
                    tileRows.get(height - 1 - insideY).charAt(insideX) != ' ')
                    throw invalid(file, doorField + " is blocked by a tile");
            }
            doors.add(new Door(side, offset, span));
        }
        return new RoomTemplate(id, patternId, role, biome, encounterRole, eventId,
            width, height, tileRows, doors);
    }

    private static boolean reserveRouteConnection(RoomTemplate a, RoomTemplate b,
                                                  Map<String, Set<String>> usedSockets) {
        Set<String> aUsed = usedSockets.computeIfAbsent(a.id, id -> new LinkedHashSet<>());
        Set<String> bUsed = usedSockets.computeIfAbsent(b.id, id -> new LinkedHashSet<>());
        for (Door door : a.doors) {
            String bSide = opposite(door.side);
            if (b.door(bSide) == null || aUsed.contains(door.side) || bUsed.contains(bSide)) continue;
            aUsed.add(door.side);
            bUsed.add(bSide);
            return true;
        }
        return false;
    }

    private static String opposite(String side) {
        switch (side) {
            case "left": return "right";
            case "right": return "left";
            case "top": return "bottom";
            default: return "top";
        }
    }

    private static String side(JsonValue node, FileHandle file, String field) {
        String side = string(node, file, field);
        if (!side.equals("left") && !side.equals("right") && !side.equals("top") && !side.equals("bottom"))
            throw invalid(file, field + " must be left, right, top, or bottom");
        return side;
    }

    private static List<String> identifiers(JsonValue node, FileHandle file, String field, int min, int max) {
        if (node == null || !node.isArray() || node.size < min || node.size > max)
            throw invalid(file, field + " must contain " + min + " to " + max + " IDs");
        List<String> ids = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (JsonValue entry : node) {
            String id = identifier(entry, file, field + " entry");
            if (!seen.add(id)) throw invalid(file, field + " repeats " + id);
            ids.add(id);
        }
        return ids;
    }

    private static String identifier(JsonValue node, FileHandle file, String field) {
        String value = string(node, file, field);
        if (!value.matches("[A-Za-z0-9_-]{1,64}"))
            throw invalid(file, field + " must be a 1 to 64 character identifier");
        return value;
    }

    private static String string(JsonValue node, FileHandle file, String field) {
        if (node == null || !node.isString()) throw invalid(file, field + " must be a string");
        return node.asString();
    }

    private static int integer(JsonValue node, FileHandle file, String field) {
        if (node == null || !node.isNumber()) throw invalid(file, field + " must be an integer");
        double value = node.asDouble();
        if (!Double.isFinite(value) || value != Math.rint(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)
            throw invalid(file, field + " must be an integer");
        return (int) value;
    }

    private static IllegalArgumentException invalid(FileHandle file, String detail) {
        return new IllegalArgumentException(file.path() + ": " + detail);
    }
}
