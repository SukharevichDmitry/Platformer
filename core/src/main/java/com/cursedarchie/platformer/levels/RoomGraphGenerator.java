package com.cursedarchie.platformer.levels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;

/** Chooses a constrained room graph; geometry placement belongs to the next stage. */
public final class RoomGraphGenerator {
    private static final class Choice {
        final RoomTemplateCatalog.BranchSlot slot;
        final String templateId;

        Choice(RoomTemplateCatalog.BranchSlot slot, String templateId) {
            this.slot = slot;
            this.templateId = templateId;
        }
    }

    public GeneratedRoomGraph generate(GenerationProfile profile, RoomTemplateCatalog catalog, long seed) {
        if (profile == null || catalog == null || !profile.locationId.equals(catalog.locationId))
            throw new IllegalArgumentException("Matching generation profile and catalog are required");
        Map<String, GenerationProfile.RoomPattern> patterns = new LinkedHashMap<>();
        for (GenerationProfile.RoomPattern pattern : profile.roomPatterns) patterns.put(pattern.id, pattern);

        Map<String, Integer> anchorCounts = new HashMap<>();
        for (String id : catalog.requiredRoute) {
            String pattern = catalog.templates.get(id).patternId;
            anchorCounts.put(pattern, anchorCounts.getOrDefault(pattern, 0) + 1);
        }
        for (GenerationProfile.RoomPattern pattern : profile.roomPatterns) {
            if (anchorCounts.getOrDefault(pattern.id, 0) > pattern.roomQuota.max)
                throw new IllegalArgumentException("Required route exceeds roomQuota for " + pattern.id);
        }

        Random random = new Random(seed);
        int targetCount = profile.roomCount.min + random.nextInt(profile.roomCount.max - profile.roomCount.min + 1);
        int remaining = targetCount - catalog.requiredRoute.size();
        int requiredBranches = 0;
        for (RoomTemplateCatalog.BranchSlot slot : catalog.optionalBranches)
            if (slot.required) requiredBranches++;
        if (remaining < requiredBranches || !canFinish(anchorCounts, patterns, remaining))
            throw new IllegalArgumentException("Seed " + seed + " cannot satisfy roomCount and roomQuota");
        Map<String, Integer> counts = new HashMap<>(anchorCounts);

        List<RoomTemplateCatalog.BranchSlot> slots = new ArrayList<>(catalog.optionalBranches);
        Collections.shuffle(slots, random);
        slots.sort((a, b) -> Boolean.compare(b.required, a.required));
        List<Choice> branches = new ArrayList<>();
        for (RoomTemplateCatalog.BranchSlot slot : slots) {
            if (!slot.required && (remaining <= requiredBranches || !random.nextBoolean())) continue;
            List<String> candidates = viable(slot.templateIds, catalog, patterns, counts, remaining - 1);
            if (candidates.isEmpty()) {
                if (slot.required) throw new IllegalArgumentException("No viable required branch at " + slot.anchorTemplate);
                continue;
            }
            String chosen = weighted(candidates, catalog, patterns, random);
            increment(counts, catalog.templates.get(chosen).patternId);
            branches.add(new Choice(slot, chosen));
            remaining--;
            if (slot.required) requiredBranches--;
        }

        List<String> route = new ArrayList<>(catalog.requiredRoute);
        while (remaining > 0) {
            List<String> candidates = viable(catalog.routeFillers, catalog, patterns, counts, remaining - 1);
            if (candidates.isEmpty())
                throw new IllegalArgumentException("Seed " + seed + " has no route filler satisfying roomQuota");
            String chosen = weighted(candidates, catalog, patterns, random);
            List<Integer> gaps = new ArrayList<>();
            for (int i = 0; i < route.size() - 1; i++) {
                RoomTemplateCatalog.RoomTemplate left = catalog.templates.get(route.get(i));
                RoomTemplateCatalog.RoomTemplate right = catalog.templates.get(route.get(i + 1));
                if (left.door("right") != null && right.door("left") != null) gaps.add(i);
            }
            if (gaps.isEmpty())
                throw new IllegalArgumentException("Seed " + seed + " has no horizontal route gap for a filler");
            List<Integer> pacedGaps = bestPacingGaps(route, chosen, gaps, catalog);
            route.add(pacedGaps.get(random.nextInt(pacedGaps.size())) + 1, chosen);
            increment(counts, catalog.templates.get(chosen).patternId);
            remaining--;
        }
        if (!canFinish(counts, patterns, 0))
            throw new IllegalArgumentException("Seed " + seed + " did not meet roomQuota minima");

        List<RoomTemplateCatalog.RoomTemplate> selected = new ArrayList<>();
        for (String id : route) selected.add(catalog.templates.get(id));
        for (Choice choice : branches) selected.add(catalog.templates.get(choice.templateId));
        List<Map<String, Integer>> links = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) links.add(new LinkedHashMap<>());
        Map<String, Set<String>> branchSockets = new HashMap<>();
        for (RoomTemplateCatalog.BranchSlot slot : catalog.optionalBranches)
            branchSockets.computeIfAbsent(slot.anchorTemplate, id -> new HashSet<>()).add(slot.anchorSide);
        List<Integer> mainRoute = new ArrayList<>();
        for (int i = 0; i < route.size(); i++) {
            mainRoute.add(i);
            if (i > 0) {
                String side = availableRouteSide(selected.get(i - 1), selected.get(i),
                    links.get(i - 1), links.get(i), branchSockets);
                if (side == null)
                    throw new IllegalArgumentException("Seed " + seed + " has no free door between " +
                        selected.get(i - 1).id + " and " + selected.get(i).id);
                connect(links, i - 1, side, i);
            }
        }
        for (int i = 0; i < branches.size(); i++) {
            Choice choice = branches.get(i);
            int anchorId = route.indexOf(choice.slot.anchorTemplate);
            if (anchorId < 0) throw new IllegalStateException("Missing branch anchor " + choice.slot.anchorTemplate);
            connect(links, anchorId, choice.slot.anchorSide, route.size() + i);
        }

        List<GeneratedRoomGraph.RoomNode> rooms = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++)
            rooms.add(new GeneratedRoomGraph.RoomNode(i, selected.get(i), links.get(i)));
        return new GeneratedRoomGraph(profile.locationId, seed, rooms, mainRoute);
    }

    private static List<String> viable(List<String> templateIds, RoomTemplateCatalog catalog,
                                       Map<String, GenerationProfile.RoomPattern> patterns,
                                       Map<String, Integer> counts, int remainingAfter) {
        List<String> result = new ArrayList<>();
        for (String id : templateIds) {
            String patternId = catalog.templates.get(id).patternId;
            Map<String, Integer> next = new HashMap<>(counts);
            increment(next, patternId);
            if (canFinish(next, patterns, remainingAfter)) result.add(id);
        }
        return result;
    }

    private static boolean canFinish(Map<String, Integer> counts,
                                     Map<String, GenerationProfile.RoomPattern> patterns, int remaining) {
        int deficit = 0;
        int capacity = 0;
        for (GenerationProfile.RoomPattern pattern : patterns.values()) {
            int count = counts.getOrDefault(pattern.id, 0);
            if (count > pattern.roomQuota.max) return false;
            deficit += Math.max(0, pattern.roomQuota.min - count);
            capacity += pattern.roomQuota.max - count;
        }
        return deficit <= remaining && remaining <= capacity;
    }

    private static String weighted(List<String> candidates, RoomTemplateCatalog catalog,
                                   Map<String, GenerationProfile.RoomPattern> patterns, Random random) {
        int total = 0;
        for (String id : candidates) total += patterns.get(catalog.templates.get(id).patternId).weight;
        int choice = random.nextInt(total);
        for (String id : candidates) {
            choice -= patterns.get(catalog.templates.get(id).patternId).weight;
            if (choice < 0) return id;
        }
        throw new IllegalStateException("Weighted choice failed");
    }

    private static List<Integer> bestPacingGaps(List<String> route, String chosen, List<Integer> gaps,
                                                 RoomTemplateCatalog catalog) {
        List<Integer> best = new ArrayList<>();
        int bestStreak = Integer.MAX_VALUE;
        for (int gap : gaps) {
            List<String> candidate = new ArrayList<>(route);
            candidate.add(gap + 1, chosen);
            int streak = maximumSafeStreak(candidate, catalog);
            if (streak < bestStreak) {
                best.clear();
                bestStreak = streak;
            }
            if (streak == bestStreak) best.add(gap);
        }
        return best;
    }

    private static int maximumSafeStreak(List<String> route, RoomTemplateCatalog catalog) {
        int current = 0;
        int maximum = 0;
        for (String templateId : route) {
            String role = catalog.templates.get(templateId).encounterRole;
            current = role.equals("safe") ? current + 1 : 0;
            maximum = Math.max(maximum, current);
        }
        return maximum;
    }

    private static void increment(Map<String, Integer> counts, String patternId) {
        counts.put(patternId, counts.getOrDefault(patternId, 0) + 1);
    }

    private static void connect(List<Map<String, Integer>> links, int from, String side, int to) {
        String opposite = opposite(side);
        if (links.get(from).putIfAbsent(side, to) != null ||
            links.get(to).putIfAbsent(opposite, from) != null)
            throw new IllegalStateException("Room doorway is already connected");
    }

    private static String availableRouteSide(RoomTemplateCatalog.RoomTemplate from,
                                             RoomTemplateCatalog.RoomTemplate to,
                                             Map<String, Integer> fromLinks, Map<String, Integer> toLinks,
                                             Map<String, Set<String>> branchSockets) {
        for (RoomTemplateCatalog.Door door : from.doors) {
            String otherSide = opposite(door.side);
            if (to.door(otherSide) == null || fromLinks.containsKey(door.side) ||
                toLinks.containsKey(otherSide) ||
                branchSockets.getOrDefault(from.id, Collections.emptySet()).contains(door.side) ||
                branchSockets.getOrDefault(to.id, Collections.emptySet()).contains(otherSide)) continue;
            return door.side;
        }
        return null;
    }

    private static String opposite(String side) {
        switch (side) {
            case "left": return "right";
            case "right": return "left";
            case "top": return "bottom";
            case "bottom": return "top";
            default: throw new IllegalArgumentException("Unknown side " + side);
        }
    }
}
