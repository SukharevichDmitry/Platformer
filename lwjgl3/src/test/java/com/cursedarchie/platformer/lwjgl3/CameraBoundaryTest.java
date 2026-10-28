package com.cursedarchie.platformer.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.cursedarchie.platformer.levels.RoomInstance;
import com.cursedarchie.platformer.world.GameWorld;
import com.cursedarchie.platformer.world.WorldRenderer;

/** Checks camera framing against the actual rooms in the packaged game. */
public final class CameraBoundaryTest extends ApplicationAdapter {
    private GameWorld world;
    private WorldRenderer renderer;
    private boolean passed;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setInitialVisible(false);
        config.setWindowedMode(640, 480);
        CameraBoundaryTest test = new CameraBoundaryTest();
        new Lwjgl3Application(test, config);
        if (!test.passed) throw new AssertionError("Camera boundary test did not complete");
        System.out.println("CAMERA BOUNDARY PASSED: raised opening floor framing and bounded location edges");
    }

    @Override public void create() {
        world = new GameWorld();
        renderer = new WorldRenderer(world, false);
        renderer.setSize(640, 480);
        checkRightEdge("start", 3);
        checkRightEdge("forest", 0);
        checkRightEdge("start", 3);
        checkLeftEdge("start", 0);
        checkLeftEdge("forest", 0);
        checkLeftEdge("start", 0);
        checkVerticalEdges("start");
        checkVerticalEdges("ruins");
        checkVerticalEdges("forest");
        checkVerticalEdges("start");
        checkWideAspect();
        passed = true;
        Gdx.app.exit();
    }

    private void checkRightEdge(String location, int roomId) {
        world.changeLocation(location, roomId);
        float levelRight = Float.NEGATIVE_INFINITY;
        for (RoomInstance room : world.getLocation().getRooms()) {
            levelRight = Math.max(levelRight, room.getWorldX() + room.getWidth());
        }
        float roomLeft = world.getCurrentRoom().getWorldX();
        world.getHero().getBody().setTransform(roomLeft + 10f, 4, 0);
        renderSettled();
        float expectedFollowEdge = roomLeft + 10f + 14f * (640f / 480f) / 2f;
        if (Math.abs(renderer.getVisibleRightEdge() - expectedFollowEdge) > 0.001f) {
            throw new AssertionError(location + " camera stopped following before the boundary");
        }
        world.getHero().getBody().setTransform(levelRight - 0.2f, 4, 0);
        renderSettled();
        assertRightEdge(location, levelRight + 2f);
        world.getHero().getBody().setTransform(levelRight + 6f, 4, 0);
        renderSettled();
        assertRightEdge(location, levelRight + 2f);
    }

    private void checkWideAspect() {
        renderer.setSize(1920, 1080);
        renderer.render(0f);
        float worldWidth = renderer.getVisibleRightEdge() - renderer.getVisibleLeftEdge();
        float worldHeight = renderer.getVisibleTopEdge() - renderer.getVisibleBottomEdge();
        if (Math.abs(worldWidth / worldHeight - 16f / 9f) > 0.001f)
            throw new AssertionError("World pixels stretch at 1920x1080");
        renderer.setSize(640, 480);
    }

    private void checkLeftEdge(String location, int roomId) {
        world.changeLocation(location, roomId);
        float levelLeft = Float.POSITIVE_INFINITY;
        for (RoomInstance room : world.getLocation().getRooms()) {
            levelLeft = Math.min(levelLeft, room.getWorldX());
        }
        world.getHero().getBody().setTransform(levelLeft + 14f * (640f / 480f) / 2f, 4, 0);
        renderSettled();
        assertLeftEdge(location, levelLeft);
        world.getHero().getBody().setTransform(levelLeft + 0.2f, 4, 0);
        renderSettled();
        assertLeftEdge(location, levelLeft - 2f);
        world.getHero().getBody().setTransform(levelLeft - 6f, 4, 0);
        renderSettled();
        assertLeftEdge(location, levelLeft - 2f);
    }

    private void assertLeftEdge(String location, float expectedLeft) {
        float actualLeft = renderer.getVisibleLeftEdge();
        if (Math.abs(actualLeft - expectedLeft) > 0.001f)
            throw new AssertionError(location + " left edge: expected " + expectedLeft + ", got " + actualLeft);
    }

    private void assertRightEdge(String location, float expectedRight) {
        float actualRight = renderer.getVisibleRightEdge();
        if (Math.abs(actualRight - expectedRight) > 0.001f)
            throw new AssertionError(location + " right edge: expected " + expectedRight + ", got " + actualRight);
    }

    private void checkVerticalEdges(String location) {
        world.changeLocation(location, 0);
        float bottom = Float.POSITIVE_INFINITY;
        float top = Float.NEGATIVE_INFINITY;
        for (RoomInstance room : world.getLocation().getRooms()) {
            bottom = Math.min(bottom, room.getWorldY());
            top = Math.max(top, room.getWorldY() + room.getHeight());
        }
        float middle = (bottom + top) / 2f;
        moveHeroVertically(middle);
        assertVerticalEdge(location, renderer.getVisibleBottomEdge(), middle - 7f, "following");
        float beforeJump = renderer.getVisibleBottomEdge();
        world.getHero().getBody().setTransform(10, middle + 4f, 0);
        renderer.render(GameWorld.FIXED_STEP);
        float firstJumpFrame = renderer.getVisibleBottomEdge();
        if (firstJumpFrame <= beforeJump || firstJumpFrame >= beforeJump + 2f)
            throw new AssertionError(location + " vertical camera did not ease toward a jump");
        float atSixty = followForOneSecond(middle, 60);
        float atThirty = followForOneSecond(middle, 30);
        float atOneTwenty = followForOneSecond(middle, 120);
        if (Math.abs(atSixty - atThirty) > 0.001f || Math.abs(atSixty - atOneTwenty) > 0.001f)
            throw new AssertionError(location + " vertical smoothing depends on render FPS");
        moveHeroVertically(bottom + 0.2f);
        float expectedBottom = bottom - ("start".equals(location) || "ruins".equals(location) ? .6f : 2f);
        assertVerticalEdge(location, renderer.getVisibleBottomEdge(), expectedBottom, "bottom");
        moveHeroVertically(bottom - 6f);
        assertVerticalEdge(location, renderer.getVisibleBottomEdge(), expectedBottom, "past bottom");
        moveHeroVertically(top - 0.2f);
        assertVerticalEdge(location, renderer.getVisibleTopEdge(), top + 2f, "top");
        moveHeroVertically(top + 6f);
        assertVerticalEdge(location, renderer.getVisibleTopEdge(), top + 2f, "past top");
    }

    private void moveHeroVertically(float y) {
        world.getHero().getBody().setTransform(10, y, 0);
        renderSettled();
    }

    private float followForOneSecond(float middle, int fps) {
        moveHeroVertically(middle);
        world.getHero().getBody().setTransform(10, middle + 4f, 0);
        for (int i = 0; i < fps; i++) renderer.render(1f / fps);
        return renderer.getVisibleBottomEdge();
    }

    private void renderSettled() {
        // A long frame reaches the clamped target while normal frames stay smoothed.
        renderer.render(2f);
    }

    private void assertVerticalEdge(String location, float actual, float expected, String edge) {
        if (Math.abs(actual - expected) > 0.001f)
            throw new AssertionError(location + " " + edge + ": expected " + expected + ", got " + actual);
    }

    @Override public void dispose() {
        if (renderer != null) renderer.dispose();
        if (world != null) world.dispose();
    }
}
