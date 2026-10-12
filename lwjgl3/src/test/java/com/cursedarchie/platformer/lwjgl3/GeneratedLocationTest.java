package com.cursedarchie.platformer.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.cursedarchie.platformer.levels.GenerationProfile;
import com.cursedarchie.platformer.levels.LocationInstance;
import com.cursedarchie.platformer.levels.PlacedRoomGraph;
import com.cursedarchie.platformer.levels.RoomInstance;
import com.cursedarchie.platformer.levels.RoomGraphGenerator;
import com.cursedarchie.platformer.levels.RoomLayoutPlacer;
import com.cursedarchie.platformer.levels.RoomTemplateCatalog;
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
        int startBodies = world.getWorld().getBodyCount();
        int startEnemies = world.getEnemies().size;
        int forestBodies = -1;
        int ruinsBodies = -1;
        int ruinsEnemies = -1;
        LocationInstance generated = null;
        for (int i = 0; i < 20; i++) {
            world.changeLocation("forest", 0);
            if (forestBodies < 0) forestBodies = world.getWorld().getBodyCount();
            expect("forest", forestBodies, 0);
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
                    !"forest".equals(entry.getLocationEntry().location))
                    throw new AssertionError("Generated entry does not open onto forest");
            } else if (world.getLocation() != generated) {
                throw new AssertionError("Ruins regenerated during the same run");
            }
            expect("ruins", ruinsBodies, ruinsEnemies);
            world.changeLocation("start", 0);
            expect("start", startBodies, startEnemies);
        }

        world.changeLocation("forest", 0);
        RoomInstance forest = world.getCurrentRoom();
        world.getHero().getBody().setTransform(forest.getWorldX() + forest.getWidth() - 0.1f,
            forest.getWorldY() + 2f, 0);
        world.update(GameWorld.FIXED_STEP);
        expect("ruins", ruinsBodies, ruinsEnemies);
        RoomInstance entry = world.getCurrentRoom();
        world.getHero().getBody().setTransform(entry.getWorldX() + 0.1f, entry.getWorldY() + 2f, 0);
        world.update(GameWorld.FIXED_STEP);
        expect("forest", forestBodies, 0);
        exerciseOptionalBranchEntry();
        passed = true;
        Gdx.app.exit();
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
            branchWorld.changeLocation("forest", 0);
            int forestBodies = branchWorld.getWorld().getBodyCount();
            branchWorld.changeLocation("ruins", 0);
            RoomInstance entry = branchWorld.getCurrentRoom();
            if (entry.getRoomId() == 0 ||
                entry.getRoomId() != branchWorld.getLocation().getEntryRoomId())
                throw new AssertionError("Transition missed left branch entry");
            branchWorld.getHero().getBody().setTransform(entry.getWorldX() + 0.1f,
                entry.getWorldY() + 2f, 0);
            branchWorld.update(GameWorld.FIXED_STEP);
            if (!"forest".equals(branchWorld.getLocation().getLocation()) ||
                branchWorld.getWorld().getBodyCount() != forestBodies)
                throw new AssertionError("Optional branch did not return cleanly to forest");
        } finally {
            branchWorld.dispose();
        }
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
