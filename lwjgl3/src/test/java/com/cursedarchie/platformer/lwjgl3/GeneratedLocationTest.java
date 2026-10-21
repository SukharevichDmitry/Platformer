package com.cursedarchie.platformer.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.Input;
import com.cursedarchie.platformer.levels.GenerationProfile;
import com.cursedarchie.platformer.levels.LocationInstance;
import com.cursedarchie.platformer.levels.PlacedRoomGraph;
import com.cursedarchie.platformer.levels.RoomInstance;
import com.cursedarchie.platformer.levels.RoomGraphGenerator;
import com.cursedarchie.platformer.levels.RoomLayoutPlacer;
import com.cursedarchie.platformer.levels.RoomTemplateCatalog;
import com.cursedarchie.platformer.levels.VerticalConnection;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.tiles.types.HalfOfBlock;
import com.cursedarchie.platformer.world.GameWorld;

/** Exercises packaged generation, border transitions, and repeated unload/reload. */
public final class GeneratedLocationTest extends ApplicationAdapter {
    private GameWorld world;
    private boolean passed;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setInitialVisible(false);
        config.setWindowedMode(640, 480);
        GeneratedLocationTest test = new GeneratedLocationTest();
        new Lwjgl3Application(test, config);
        if (!test.passed) throw new AssertionError("Generated location test did not complete");
        System.out.println("GENERATED LOCATION PASSED: repeatable ruins, both border transitions, stable body and enemy counts");
    }

    @Override public void create() {
        world = new GameWorld(42L);
        if (!world.isRoomVisited(0) || world.isRoomVisited(1) || world.isRoomVisited(2))
            throw new AssertionError("Mini-map revealed unvisited start rooms");
        world.changeLocation("start", 1);
        if (!world.isRoomVisited(1) || world.isRoomVisited(2))
            throw new AssertionError("Entering a start room did not reveal only that room");
        world.changeLocation("start", 0);
        int startBodies = world.getWorld().getBodyCount();
        int startEnemies = world.getEnemies().size;
        int ruinsBodies = -1;
        int ruinsEnemies = -1;
        int visitedRuinsRoomId = -1;
        LocationInstance generated = null;
        for (int i = 0; i < 20; i++) {
            world.changeLocation("ruins", 0);
            if (generated == null) {
                generated = world.getLocation();
                ruinsBodies = world.getWorld().getBodyCount();
                ruinsEnemies = world.getEnemies().size;
                if (!generated.isGenerated() || generated.getGenerationSeed() != 42L)
                    throw new AssertionError("Requested seed was not retained");
                int left = Integer.MAX_VALUE;
                for (RoomInstance room : generated.getRooms())
                    left = Math.min(left, Math.round(room.getWorldX()));
                RoomInstance entry = generated.getRoom(generated.getEntryRoomId());
                if (entry.getWorldX() != left || entry.getLocationEntry() == null ||
                    !"start".equals(entry.getLocationEntry().location) ||
                    entry.getLocationEntry().id != 2)
                    throw new AssertionError("Generated entry does not open onto start room 2");
                checkPartialTileGeometry(generated);
                RoomInstance undiscovered = null;
                for (RoomInstance candidate : generated.getRooms()) {
                    if (candidate != entry) {
                        undiscovered = candidate;
                        break;
                    }
                }
                if (undiscovered == null || world.isRoomVisited(undiscovered.getRoomId()))
                    throw new AssertionError("Mini-map revealed an unvisited ruins room");
                world.getHero().getBody().setTransform(undiscovered.getWorldX() + 2f,
                    undiscovered.getWorldY() + 2f, 0f);
                world.update(GameWorld.FIXED_STEP);
                if (world.getCurrentRoom() != undiscovered || !world.isRoomVisited(undiscovered.getRoomId()))
                    throw new AssertionError("Entering a generated room did not reveal it");
                visitedRuinsRoomId = undiscovered.getRoomId();
            } else if (world.getLocation() != generated) {
                throw new AssertionError("Ruins regenerated during the same run");
            }
            if (!world.isRoomVisited(visitedRuinsRoomId))
                throw new AssertionError("Ruins discovery was lost after returning from start");
            expect("ruins", ruinsBodies, ruinsEnemies);
            world.changeLocation("start", 0);
            expect("start", startBodies, startEnemies);
            if (!world.isRoomVisited(1) || world.isRoomVisited(2))
                throw new AssertionError("Start-room discovery was lost or leaked across locations");
        }

        world.changeLocation("start", 2);
        RoomInstance startExit = world.getCurrentRoom();
        world.getHero().getBody().setTransform(startExit.getWorldX() + startExit.getWidth() - 0.1f,
            startExit.getWorldY() + 2f, 0);
        world.update(GameWorld.FIXED_STEP);
        expect("ruins", ruinsBodies, ruinsEnemies);
        RoomInstance entry = world.getCurrentRoom();
        world.getHero().getBody().setTransform(entry.getWorldX() + 0.1f, entry.getWorldY() + 2f, 0);
        world.update(GameWorld.FIXED_STEP);
        expect("start", startBodies, startEnemies);
        if (world.getCurrentRoom().getRoomId() != 2)
            throw new AssertionError("Generated return missed the authored exit room");
        exerciseOptionalBranchEntry();
        exerciseVerticalTraversal();
        exerciseLadderJumpWithEnemies();
        passed = true;
        Gdx.app.exit();
    }

    private void exerciseVerticalTraversal() {
        GameWorld climbWorld = new GameWorld(42L);
        try {
            climbWorld.changeLocation("ruins", 0);
            // Isolate traversal geometry from wandering enemies; combat keeps its
            // normal solid contacts in gameplay and in simulation regression.
            for (com.cursedarchie.platformer.actors.enemies.Enemy enemy : climbWorld.getEnemies())
                enemy.getBody().setActive(false);
            if (climbWorld.getLocation().getVerticalConnections().isEmpty())
                throw new AssertionError("Generated ruins have no vertical connection");
            VerticalConnection shaft = climbWorld.getLocation().getVerticalConnections().get(0);
            if (shaft.halfWidth != 0.5f ||
                shaft.contains(shaft.centerX - 0.35f, (shaft.bottomY + shaft.topY) / 2f,
                    com.cursedarchie.platformer.actors.hero.Hero.BODY_SIZE) ||
                shaft.contains(shaft.centerX + 0.35f, (shaft.bottomY + shaft.topY) / 2f,
                    com.cursedarchie.platformer.actors.hero.Hero.BODY_SIZE))
                throw new AssertionError("Ladder grip extends into a neighboring tile");
            if (climbWorld.isRoomVisited(shaft.upperRoomId))
                throw new AssertionError("Upper room revealed before entering it");
            float middleY = (shaft.bottomY + shaft.topY) / 2f;
            climbWorld.getHero().getBody().setTransform(shaft.centerX - 0.65f, middleY, 0f);
            climbWorld.getHero().getBody().setLinearVelocity(0f, 0f);
            climbWorld.getHeroController().keyDown(Input.Keys.UP);
            climbWorld.update(GameWorld.FIXED_STEP);
            if (climbWorld.getHero().getBody().getGravityScale() == 0f)
                throw new AssertionError("Hero grabbed ladder from the neighboring tile");
            climbWorld.getHeroController().keyUp(Input.Keys.UP);
            climbWorld.getHero().getBody().setTransform(shaft.centerX, shaft.bottomY, 0f);
            climbWorld.getHero().getBody().setLinearVelocity(0f, 0f);
            climbWorld.getHeroController().keyDown(Input.Keys.UP);
            for (int i = 0; i < 150; i++) climbWorld.update(GameWorld.FIXED_STEP);
            climbWorld.getHeroController().keyUp(Input.Keys.UP);
            if (climbWorld.getCurrentRoom().getRoomId() != shaft.upperRoomId ||
                !climbWorld.isRoomVisited(shaft.upperRoomId))
                throw new AssertionError("Hero could not climb into and reveal the upper room");
            // Releasing Up must keep the hero on the ladder; Right must actually
            // carry them across the floor edge instead of trapping them in the shaft.
            climbWorld.getHeroController().keyDown(Input.Keys.RIGHT);
            for (int i = 0; i < 24; i++) climbWorld.update(GameWorld.FIXED_STEP);
            climbWorld.getHeroController().keyUp(Input.Keys.RIGHT);
            if (climbWorld.getHero().getPosition().x < shaft.centerX + shaft.halfWidth + 0.3f ||
                climbWorld.getHero().getPosition().y < shaft.topY - 0.65f)
                throw new AssertionError("Hero could not step from the ladder onto the upper floor: x=" +
                    climbWorld.getHero().getPosition().x + " y=" + climbWorld.getHero().getPosition().y +
                    " ladder=" + shaft.centerX + "," + shaft.topY);

            climbWorld.getHero().getBody().setTransform(shaft.centerX, shaft.topY, 0f);
            climbWorld.getHero().getBody().setLinearVelocity(0f, 0f);
            climbWorld.getHeroController().keyDown(Input.Keys.DOWN);
            for (int i = 0; i < 150; i++) climbWorld.update(GameWorld.FIXED_STEP);
            climbWorld.getHeroController().keyUp(Input.Keys.DOWN);
            if (climbWorld.getCurrentRoom().getRoomId() != shaft.lowerRoomId)
                throw new AssertionError("Hero could not descend to the lower room");

            climbWorld.getHero().getBody().setTransform(shaft.centerX, middleY, 0f);
            climbWorld.getHero().getBody().setLinearVelocity(0f, 0f);
            climbWorld.getHeroController().keyDown(Input.Keys.UP);
            climbWorld.update(GameWorld.FIXED_STEP);
            climbWorld.getHeroController().keyUp(Input.Keys.UP);
            float beforeSideMove = climbWorld.getHero().getPosition().x;
            climbWorld.getHeroController().keyDown(Input.Keys.RIGHT);
            for (int i = 0; i < 3; i++) climbWorld.update(GameWorld.FIXED_STEP);
            if (climbWorld.getHero().getPosition().x < beforeSideMove + 0.15f)
                throw new AssertionError("Left/Right did not move the hero along the ladder");
            float beforeJump = climbWorld.getHero().getPosition().y;
            climbWorld.getHeroController().keyDown(Input.Keys.UP);
            climbWorld.getHeroController().keyDown(Input.Keys.SPACE);
            for (int i = 0; i < 4; i++) climbWorld.update(GameWorld.FIXED_STEP);
            if (climbWorld.getHero().getPosition().y < beforeJump + 0.5f ||
                climbWorld.getHero().getBody().getGravityScale() == 0f)
                throw new AssertionError("Space did not jump out of the ladder");
            climbWorld.getHeroController().clearInput();
            climbWorld.getHero().getBody().setTransform(shaft.centerX, middleY, 0f);
            climbWorld.getHero().getBody().setLinearVelocity(0f, 0f);
            climbWorld.getHeroController().keyDown(Input.Keys.UP);
            climbWorld.update(GameWorld.FIXED_STEP);
            climbWorld.getHeroController().keyUp(Input.Keys.UP);
            climbWorld.getHeroController().keyDown(Input.Keys.RIGHT);
            for (int i = 0; i < 12; i++) climbWorld.update(GameWorld.FIXED_STEP);
            if (climbWorld.getHero().getPosition().x <= shaft.centerX + 0.35f ||
                climbWorld.getHero().getBody().getGravityScale() == 0f)
                throw new AssertionError("Hero stayed attached after leaving ladder sideways");
            climbWorld.getHeroController().clearInput();
        } finally {
            climbWorld.dispose();
        }
    }

    private void exerciseLadderJumpWithEnemies() {
        GameWorld combatWorld = new GameWorld(42L);
        try {
            combatWorld.changeLocation("ruins", 0);
            VerticalConnection shaft = combatWorld.getLocation().getVerticalConnections().get(0);
            float middleY = (shaft.bottomY + shaft.topY) / 2f;
            combatWorld.getHero().getBody().setTransform(shaft.centerX, middleY, 0f);
            combatWorld.getHero().getBody().setLinearVelocity(0f, 0f);
            combatWorld.getHeroController().keyDown(Input.Keys.UP);
            combatWorld.update(GameWorld.FIXED_STEP);
            combatWorld.getHeroController().keyDown(Input.Keys.RIGHT);
            combatWorld.getHeroController().keyDown(Input.Keys.SPACE);
            combatWorld.update(GameWorld.FIXED_STEP);
            if (combatWorld.getHero().getBody().getLinearVelocity().y <= 0f ||
                combatWorld.getHero().getBody().getGravityScale() == 0f)
                throw new AssertionError("Space failed to detach from a ladder during an encounter");
        } finally {
            combatWorld.dispose();
        }
    }

    private void exerciseOptionalBranchEntry() {
        GenerationProfile profile = GenerationProfile.load(Gdx.files.internal("generation/ruins.json"));
        RoomTemplateCatalog catalog = RoomTemplateCatalog.load(
            Gdx.files.internal("generation/ruins.templates.json"), profile);
        long branchSeed = -1;
        for (long seed = 0; seed < 100; seed++) {
            PlacedRoomGraph layout = new RoomLayoutPlacer().place(
                new RoomGraphGenerator().generate(profile, catalog, seed), catalog, profile);
            if (layout.minX < 0) { branchSeed = seed; break; }
        }
        if (branchSeed < 0) throw new AssertionError("No optional left branch found");
        GameWorld branchWorld = new GameWorld(branchSeed);
        try {
            int startBodies = branchWorld.getWorld().getBodyCount();
            branchWorld.changeLocation("ruins", 0);
            RoomInstance entry = branchWorld.getCurrentRoom();
            if (entry.getRoomId() == 0 ||
                entry.getRoomId() != branchWorld.getLocation().getEntryRoomId())
                throw new AssertionError("Transition missed left branch entry");
            branchWorld.getHero().getBody().setTransform(entry.getWorldX() + 0.1f,
                entry.getWorldY() + 2f, 0);
            branchWorld.update(GameWorld.FIXED_STEP);
            if (!"start".equals(branchWorld.getLocation().getLocation()) ||
                branchWorld.getCurrentRoom().getRoomId() != 2 ||
                branchWorld.getWorld().getBodyCount() != startBodies)
                throw new AssertionError("Optional branch did not return cleanly to start");
        } finally {
            branchWorld.dispose();
        }
    }

    private void checkPartialTileGeometry(LocationInstance generated) {
        for (RoomInstance room : generated.getRooms()) {
            Tile[][] tiles = room.getTiles();
            for (int x = 0; x < tiles.length; x++) {
                for (int y = 0; y < tiles[x].length; y++) {
                    Tile tile = tiles[x][y];
                    if (!(tile instanceof HalfOfBlock)) continue;
                    Rectangle bounds = tile.getBounds();
                    if (bounds.width != 1f || bounds.height != 0.5f)
                        throw new AssertionError("Half tile visual bounds are not 1 x 0.5");
                    float worldX = room.getWorldX() + bounds.x + 0.5f;
                    float worldY = room.getWorldY() + bounds.y;
                    if (hasTerrainAt(worldX, worldY + 0.25f) == false ||
                        hasTerrainAt(worldX, worldY + 0.75f))
                        throw new AssertionError("Half tile has an invisible full-cell fixture");
                    return;
                }
            }
        }
        throw new AssertionError("Generated seed has no half tile to check");
    }

    private boolean hasTerrainAt(float x, float y) {
        boolean[] found = {false};
        world.getWorld().QueryAABB(fixture -> {
            if ("level".equals(fixture.getUserData())) found[0] = true;
            return true;
        }, x - 0.01f, y - 0.01f, x + 0.01f, y + 0.01f);
        return found[0];
    }

    private void expect(String location, int bodies, int enemies) {
        if (!location.equals(world.getLocation().getLocation()) ||
            world.getWorld().getBodyCount() != bodies || world.getEnemies().size != enemies)
            throw new AssertionError(location + " leaked or lost bodies/enemies: bodies=" +
                world.getWorld().getBodyCount() + " enemies=" + world.getEnemies().size);
    }

    @Override public void dispose() {
        if (world != null) world.dispose();
    }
}
