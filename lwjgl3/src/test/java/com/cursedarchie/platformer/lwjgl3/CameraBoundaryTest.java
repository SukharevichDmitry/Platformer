package com.cursedarchie.platformer.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.cursedarchie.platformer.controllers.HeroController;
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
        config.setWindowedMode(640, 360);
        CameraBoundaryTest test = new CameraBoundaryTest();
        new Lwjgl3Application(test, config);
        if (!test.passed) throw new AssertionError("Camera boundary test did not complete");
        System.out.println("CAMERA BOUNDARY PASSED: raised opening floor framing, bounded location edges, smooth ladder floors, whole-pixel closer camera");
    }

    @Override public void create() {
        world = new GameWorld();
        renderer = new WorldRenderer(world, false);
        renderer.setSize(640, 360);
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
        checkLadderCamera();
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
        float expectedFollowEdge = roomLeft + 10f + WorldRenderer.CAMERA_WIDTH / 2f;
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

    /**
     * The ladder camera only engages while the hero climbs. Walking past a ladder keeps the room's framing; while
     * climbing between stacked floors the camera scrolls with the hero, as across a horizontal room edge, and no frame
     * jumps (grabbing on, crossing floors, stepping off at either end).
     */
    private void checkLadderCamera() {
        world.changeLocation("laundry_alley", 0);
        // Camera only: enemies standing in a shaft would shove the hero or take a head landing mid-climb.
        for (com.cursedarchie.platformer.actors.enemies.Enemy enemy : world.getEnemies()) enemy.setAlive(false);
        for (int i = 0; i < 5; i++) world.update(GameWorld.FIXED_STEP);
        com.cursedarchie.platformer.levels.VerticalConnection alley = null, cellar = null;
        for (com.cursedarchie.platformer.levels.VerticalConnection ladder : world.getLocation().getVerticalConnections()) {
            if ("alley_to_laundry".equals(ladder.id)) alley = ladder;
            if ("laundry_to_cellar".equals(ladder.id)) cellar = ladder;
        }
        if (alley == null || cellar == null) throw new AssertionError("Laundry alley has no stacked ladders");
        float alleyStand = world.getLocation().getRoom(alley.upperRoomId).getWorldY() + 2.515f;
        float laundryStand = world.getLocation().getRoom(alley.lowerRoomId).getWorldY() + 2.515f;
        walkPastLadder(alley.centerX, alleyStand, "alley mouth");
        walkPastLadder(cellar.centerX, laundryStand, "laundry floor");
        // A jump under the alley ladder rises into its shaft without grabbing it: the camera must do exactly what
        // it does for the same jump a few steps away from the ladder.
        float[] awayJump = jumpCamera(alley.centerX + 3f, laundryStand);
        float[] ladderJump = jumpCamera(alley.centerX, laundryStand);
        for (int i = 0; i < awayJump.length; i++)
            if (Math.abs(awayJump[i] - ladderJump[i]) > 0.01f)
                throw new AssertionError("Jumping under the ladder moved the camera like climbing, frame " + i +
                    ": " + awayJump[i] + " vs " + ladderJump[i]);

        HeroController controller = world.getHeroController();
        world.getHero().getBody().setTransform(alley.centerX, alleyStand, 0);
        controller.clearInput();
        world.update(GameWorld.FIXED_STEP);
        renderSettled();
        resetCameraMotion();
        controller.keyDown(Input.Keys.DOWN);
        int cellarRoom = cellar.lowerRoomId;
        float largest = 0f;
        for (int i = 0; i < 600 && !(world.getCurrentRoom().getRoomId() == cellarRoom &&
            world.getHero().getPosition().y <= cellar.bottomY + .01f); i++)
            largest = Math.max(largest, stepAndMeasure());
        controller.keyUp(Input.Keys.DOWN);
        if (world.getCurrentRoom().getRoomId() != cellarRoom)
            throw new AssertionError("Ladder camera check did not reach the cellar: " + world.getHero().getPosition());
        largest = Math.max(largest, stepOff());
        if (largest > 0.5f) throw new AssertionError("Ladder camera jumped " + largest + " in one frame going down");
        // Eased: the camera gathers speed gently as the climb starts instead of lurching to full speed.
        if (largestCameraJerk > 0.02f)
            throw new AssertionError("Ladder camera changed speed by " + largestCameraJerk + " in one frame going down");

        world.getHero().getBody().setTransform(cellar.centerX, world.getHero().getPosition().y, 0);
        controller.clearInput();
        world.update(GameWorld.FIXED_STEP);
        renderSettled();
        resetCameraMotion();
        controller.keyDown(Input.Keys.UP);
        largest = 0f;
        for (int i = 0; i < 600 && world.getHero().getPosition().y < alley.topY - .01f; i++)
            largest = Math.max(largest, stepAndMeasure());
        controller.keyUp(Input.Keys.UP);
        if (world.getCurrentRoom().getRoomId() != alley.upperRoomId)
            throw new AssertionError("Ladder camera check did not climb back to the alley: pos=" +
                world.getHero().getPosition() + ", room=" + world.getCurrentRoom().getRoomId() + ", climbing=" +
                controller.isClimbing() + ", ladders=" + cellar.bottomY + ".." + cellar.topY + "/" + alley.bottomY + ".." + alley.topY);
        largest = Math.max(largest, stepOff());
        if (largest > 0.5f) throw new AssertionError("Ladder camera jumped " + largest + " in one frame going up");
        // Eased: the camera gathers speed gently as the climb starts instead of lurching to full speed.
        if (largestCameraJerk > 0.02f)
            throw new AssertionError("Ladder camera changed speed by " + largestCameraJerk + " in one frame going up");
        float settled = renderer.getVisibleBottomEdge();
        renderSettled();
        if (Math.abs(renderer.getVisibleBottomEdge() - settled) > 0.05f)
            throw new AssertionError("Camera kept moving after stepping off the ladder");
    }

    /** Walks across the ladder without touching Up/Down: the camera must not move at all. */
    private void walkPastLadder(float centerX, float standY, String where) {
        HeroController controller = world.getHeroController();
        world.getHero().getBody().setTransform(centerX - 3f, standY, 0);
        controller.clearInput();
        for (int i = 0; i < 10; i++) world.update(GameWorld.FIXED_STEP);
        renderSettled();
        float framed = renderer.getVisibleBottomEdge();
        controller.keyDown(Input.Keys.RIGHT);
        boolean crossed = false;
        for (int i = 0; i < 120 && world.getHero().getPosition().x < centerX + 3f; i++) {
            world.update(GameWorld.FIXED_STEP);
            renderer.render(GameWorld.FIXED_STEP);
            crossed |= Math.abs(world.getHero().getPosition().x - centerX) < .2f;
            if (controller.isClimbing())
                throw new AssertionError("Walking past the ladder at the " + where + " grabbed it");
            if (Math.abs(renderer.getVisibleBottomEdge() - framed) > 0.01f)
                throw new AssertionError("Walking past the ladder at the " + where + " moved the camera: " +
                    framed + " -> " + renderer.getVisibleBottomEdge());
        }
        controller.keyUp(Input.Keys.RIGHT);
        if (!crossed) throw new AssertionError("Walk past the ladder at the " + where + " never crossed it");
    }

    private float[] jumpCamera(float x, float standY) {
        HeroController controller = world.getHeroController();
        world.getHero().getBody().setTransform(x, standY, 0);
        world.getHero().getBody().setLinearVelocity(0, 0);
        controller.clearInput();
        for (int i = 0; i < 10; i++) world.update(GameWorld.FIXED_STEP);
        renderSettled();
        float[] frames = new float[50];
        controller.keyDown(Input.Keys.SPACE);
        for (int i = 0; i < frames.length; i++) {
            world.update(GameWorld.FIXED_STEP);
            renderer.render(GameWorld.FIXED_STEP);
            if (controller.isClimbing()) throw new AssertionError("A plain jump grabbed the ladder");
            frames[i] = renderer.getVisibleBottomEdge();
        }
        controller.keyUp(Input.Keys.SPACE);
        for (int i = 0; i < 40; i++) world.update(GameWorld.FIXED_STEP);
        return frames;
    }

    /** Camera speed of the previous frame and the largest frame-to-frame change of speed since the last reset. */
    private float lastCameraStep, largestCameraJerk;

    private void resetCameraMotion() {
        lastCameraStep = 0f;
        largestCameraJerk = 0f;
    }

    private float stepAndMeasure() {
        float before = renderer.getVisibleBottomEdge();
        world.update(GameWorld.FIXED_STEP);
        renderer.render(GameWorld.FIXED_STEP);
        float step = renderer.getVisibleBottomEdge() - before;
        largestCameraJerk = Math.max(largestCameraJerk, Math.abs(step - lastCameraStep));
        lastCameraStep = step;
        return Math.abs(step);
    }

    private float stepOff() {
        HeroController controller = world.getHeroController();
        float largest = 0f;
        controller.keyDown(Input.Keys.RIGHT);
        for (int i = 0; i < 20; i++) largest = Math.max(largest, stepAndMeasure());
        controller.keyUp(Input.Keys.RIGHT);
        for (int i = 0; i < 60; i++) largest = Math.max(largest, stepAndMeasure());
        if (controller.isClimbing()) throw new AssertionError("Hero did not step off the ladder");
        return largest;
    }

    private void checkWideAspect() {
        renderer.setSize(1920, 1080);
        renderer.render(0f);
        float worldWidth = renderer.getVisibleRightEdge() - renderer.getVisibleLeftEdge();
        float worldHeight = renderer.getVisibleTopEdge() - renderer.getVisibleBottomEdge();
        if (Math.abs(worldWidth / worldHeight - 16f / 9f) > 0.001f)
            throw new AssertionError("World pixels stretch at 1920x1080");
        // Closer camera with whole-number pixel scale: 1080p shows 270 art rows at 4x, 720p 240 rows at 3x.
        int[][] sizes = {{1920, 1080, 4}, {1280, 720, 3}, {2560, 1440, 5}, {640, 360, 1}};
        for (int[] size : sizes) {
            renderer.setSize(size[0], size[1]);
            renderer.render(0f);
            float height = renderer.getVisibleTopEdge() - renderer.getVisibleBottomEdge();
            float screenPerArt = size[1] / (height * WorldRenderer.PIXELS_PER_UNIT);
            if (WorldRenderer.pixelScaleFor(size[1]) != size[2] || Math.abs(screenPerArt - size[2]) > 0.001f)
                throw new AssertionError("Art pixels are not " + size[2] + " screen pixels at " + size[0] + "x" +
                    size[1] + ": " + screenPerArt);
        }
        renderer.setSize(640, 360);
    }

    private void checkLeftEdge(String location, int roomId) {
        world.changeLocation(location, roomId);
        float levelLeft = Float.POSITIVE_INFINITY;
        for (RoomInstance room : world.getLocation().getRooms()) {
            levelLeft = Math.min(levelLeft, room.getWorldX());
        }
        world.getHero().getBody().setTransform(levelLeft + WorldRenderer.CAMERA_WIDTH / 2f, 4, 0);
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
        assertVerticalEdge(location, renderer.getVisibleBottomEdge(), middle - WorldRenderer.CAMERA_HEIGHT / 2f,
            "following");
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
        float viewHeight = renderer.getVisibleTopEdge() - renderer.getVisibleBottomEdge();
        // On the street the road's front face keeps its share of the view below the walk line (two floor rows up).
        float expectedBottom = "start".equals(location) || "ruins".equals(location)
            ? bottom + 2f - WorldRenderer.ROAD_VIEW_SHARE * viewHeight : bottom - 2f;
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
