package com.cursedarchie.platformer.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Json;
import com.cursedarchie.platformer.levels.LocationInstance;
import com.cursedarchie.platformer.levels.RoomInstance;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class RuinsStreetRenderer implements Disposable {
    private static final String DIRECTORY = "backgrounds/ruins/";
    private static final float PX = 1f / WorldRenderer.PIXELS_PER_UNIT;
    private static final int FACADE_COUNT = 12;
    /**
     * The road's paving runs WALK_LINE_INSET past the walk line (the street's depth behind the hero's feet), so a
     * facade standing on the walk line had its doorsteps and the bottom of its doors hidden behind the paving. The
     * whole street stands on the paving's back edge instead, rounded down to whole art pixels so no gap opens
     * between the paving and the facades.
     */
    static final float GROUND_LIFT = (float) Math.floor(RoadSurfaceRenderer.WALK_LINE_INSET / PX) * PX;

    private static final class Catalog {
        public Route[] routes;
    }

    private static final class Route {
        public int left, right, overlap;
        public String front, leftEdge, rightEdge;
        public boolean optional;
    }

    private static final class Piece {
        final TextureRegion region;
        final int x;

        Piece(TextureRegion region, int x) {
            this.region = region;
            this.x = x;
        }
    }

    private static final class Strip {
        final List<Piece> rear = new ArrayList<>();
        final List<Piece> front = new ArrayList<>();
        int width;
    }

    private final TextureAtlas atlas = new TextureAtlas(Gdx.files.internal(DIRECTORY + "street.atlas"));
    private final TextureRegion alleyFront = atlas.findRegion("alley_entry");
    private final TextureRegion alleyRear = atlas.findRegion("alley_entry_rear");
    private final TextureRegion sky = loadBackdrop("sky");
    private final TextureRegion far = loadBackdrop("far");
    private final TextureRegion rooftops = loadBackdrop("rooftops");
    private final Catalog catalog = new Json().fromJson(Catalog.class, Gdx.files.internal(DIRECTORY + "street.json"));
    private final TextureRegion clipped = new TextureRegion();
    private final Map<Float, Strip> strips = new HashMap<>();
    private LocationInstance current;

    private static TextureRegion loadBackdrop(String name) {
        Texture texture = new Texture(Gdx.files.internal(DIRECTORY + name + ".png"));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return new TextureRegion(texture);
    }

    void draw(LocationInstance location, SpriteBatch batch, float visibleLeft, float visibleRight, float cameraX) {
        if (current != location) {
            strips.clear();
            current = location;
        }
        Long generatedSeed = location.getGenerationSeed();
        long seed = generatedSeed == null ? 0L : generatedSeed;
        for (RoomInstance room : location.getRooms()) {
            float left = Math.max(visibleLeft, room.getWorldX());
            float right = Math.min(visibleRight, room.getWorldX() + room.getWidth());
            if (right <= left) continue;
            float floor = room.getWorldY() + 2f + GROUND_LIFT;
            drawBackdrop(batch, sky, left, right, floor - 13 * PX, cameraX * .85f);
            drawBackdrop(batch, far, left, right, floor + 41 * PX, cameraX * .63f);
            drawBackdrop(batch, rooftops, left, right, floor - 39 * PX, cameraX * .38f);
        }
        for (RoomInstance room : location.getRooms()) {
            float left = Math.max(visibleLeft, room.getWorldX());
            float right = Math.min(visibleRight, room.getWorldX() + room.getWidth());
            if (right <= left) continue;
            float row = room.getWorldY();
            float floor = row + 2f + GROUND_LIFT;
            Strip strip = strips.computeIfAbsent(row, y -> buildStrip(seed ^ Float.floatToIntBits(y)));
            float width = strip.width * PX;
            float start = (float) Math.floor(left / width) * width;
            for (float x = start; x < right; x += width) {
                drawPieces(batch, strip.rear, x, floor, left, right);
                drawPieces(batch, strip.front, x, floor, left, right);
            }
        }
    }

    /** The alley entrance at a portal; {@code walkLine} is the room's floor, the visual stands on the paving's back edge. */
    void drawAlley(SpriteBatch batch, float x, float walkLine) {
        float floor = walkLine + GROUND_LIFT;
        float left = x - 34 * PX;
        batch.draw(alleyRear, left, floor, alleyRear.getRegionWidth() * PX, alleyRear.getRegionHeight() * PX);
        batch.draw(alleyFront, left, floor, alleyFront.getRegionWidth() * PX, alleyFront.getRegionHeight() * PX);
    }
    private void drawBackdrop(SpriteBatch batch, TextureRegion region, float left, float right,
                              float y, float shift) {
        float width = region.getRegionWidth() * PX;
        float offset = Math.round(shift / PX) * PX;
        float start = offset + (float) Math.floor((left - offset) / width) * width;
        for (float x = start; x < right; x += width) drawClipped(batch, region, x, y, left, right);
    }

    private void drawPieces(SpriteBatch batch, List<Piece> pieces, float origin, float y,
                            float left, float right) {
        for (Piece piece : pieces) drawClipped(batch, piece.region, origin + piece.x * PX, y, left, right);
    }

    private void drawClipped(SpriteBatch batch, TextureRegion region, float x, float y,
                             float left, float right) {
        float width = region.getRegionWidth() * PX;
        float from = Math.max(left, x);
        float to = Math.min(right, x + width);
        if (to <= from) return;
        clipped.setRegion(region);
        float uWidth = region.getU2() - region.getU();
        clipped.setU(region.getU() + uWidth * (from - x) / width);
        clipped.setU2(region.getU() + uWidth * (to - x) / width);
        batch.draw(clipped, from, y, to - from, region.getRegionHeight() * PX);
    }

    private Strip buildStrip(long seed) {
        Random random = new Random(seed);
        List<List<Route>> choices = new ArrayList<>();
        for (int f = 0; f <= FACADE_COUNT; f++) choices.add(new ArrayList<>());
        Route direct = null;
        int count = 0;
        for (Route route : catalog.routes) {
            if (route.optional) direct = route;
            else {
                choices.get(route.left).add(route);
                count++;
            }
        }
        for (List<Route> list : choices) Collections.shuffle(list, random);
        boolean includeDirect = direct != null && random.nextBoolean();
        int start = includeDirect ? direct.right : 1 + random.nextInt(FACADE_COUNT);
        List<Route> cycle = new ArrayList<>();
        collectCycle(start, choices, cycle);
        if (cycle.size() != count) throw new IllegalStateException("Disconnected ruins facade catalog");
        Collections.reverse(cycle);
        if (includeDirect) {
            cycle.add(0, direct);
            cycle.addAll(returnPath(direct.right, direct.left));
        }

        Strip strip = new Strip();
        Route incoming = cycle.get(cycle.size() - 1);
        for (Route outgoing : cycle) {
            if (incoming.right != outgoing.left) throw new IllegalStateException("Invalid ruins facade join");
            int x = strip.width;
            String base = outgoing.left < 10 ? "F0" + outgoing.left : "F" + outgoing.left;
            TextureRegion rear = atlas.findRegion(base + "_rear");
            if (rear != null) strip.rear.add(new Piece(rear, x));
            addFront(strip, incoming.rightEdge);
            addFront(strip, outgoing.leftEdge);
            if (outgoing.front != null) addFront(strip, outgoing.front);
            strip.width -= outgoing.overlap;
            incoming = outgoing;
        }
        return strip;
    }

    private void addFront(Strip strip, String name) {
        TextureRegion texture = atlas.findRegion(name);
        if (texture == null) throw new IllegalStateException("Missing ruins facade " + name);
        strip.front.add(new Piece(texture, strip.width));
        strip.width += texture.getRegionWidth();
    }

    private static void collectCycle(int facade, List<List<Route>> choices, List<Route> cycle) {
        List<Route> next = choices.get(facade);
        while (!next.isEmpty()) {
            Route route = next.remove(next.size() - 1);
            collectCycle(route.right, choices, cycle);
            cycle.add(route);
        }
    }

    private List<Route> returnPath(int start, int end) {
        Route[] incoming = new Route[FACADE_COUNT + 1];
        boolean[] seen = new boolean[FACADE_COUNT + 1];
        ArrayDeque<Integer> todo = new ArrayDeque<>();
        seen[start] = true;
        todo.add(start);
        while (!todo.isEmpty() && !seen[end]) {
            int left = todo.remove();
            for (Route route : catalog.routes) {
                if (route.optional || route.left != left || seen[route.right]) continue;
                incoming[route.right] = route;
                seen[route.right] = true;
                todo.add(route.right);
            }
        }
        if (!seen[end]) throw new IllegalStateException("Unable to close ruins street");
        List<Route> result = new ArrayList<>();
        for (int right = end; right != start; right = incoming[right].left) result.add(incoming[right]);
        Collections.reverse(result);
        return result;
    }

    @Override public void dispose() {
        atlas.dispose();
        sky.getTexture().dispose();
        far.getTexture().dispose();
        rooftops.getTexture().dispose();
    }
}
