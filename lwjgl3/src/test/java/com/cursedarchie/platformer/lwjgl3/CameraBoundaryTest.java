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
        System.out.println("CAMERA BOUNDARY PASSED: start and forest stop one block past the right edge");
    }

    @Override public void create() {
        world = new GameWorld();
        renderer = new WorldRenderer(world, false);
        renderer.setSize(640, 480);
        checkRightEdge("start", 2);
        checkRightEdge("forest", 0);
        checkRightEdge("start", 2);
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
        world.getHero().getBody().setTransform(roomLeft + 2f, 2, 0);
        renderer.render();
        if (Math.abs(renderer.getVisibleRightEdge() - (roomLeft + 7f)) > 0.001f) {
            throw new AssertionError(location + " camera stopped following before the boundary");
        }
        world.getHero().getBody().setTransform(levelRight - 0.1f, 2, 0);
        renderer.render();
        assertRightEdge(location, levelRight + 1f);
        world.getHero().getBody().setTransform(levelRight + 3f, 2, 0);
        renderer.render();
        assertRightEdge(location, levelRight + 1f);
    }

    private void assertRightEdge(String location, float expectedRight) {
        float actualRight = renderer.getVisibleRightEdge();
        if (Math.abs(actualRight - expectedRight) > 0.001f)
            throw new AssertionError(location + " right edge: expected " + expectedRight + ", got " + actualRight);
    }

    @Override public void dispose() {
        if (renderer != null) renderer.dispose();
        if (world != null) world.dispose();
    }
}
