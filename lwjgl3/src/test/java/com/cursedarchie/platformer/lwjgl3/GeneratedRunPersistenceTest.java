package com.cursedarchie.platformer.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.levels.GeneratedRunSave;
import com.cursedarchie.platformer.levels.RoomInstance;
import com.cursedarchie.platformer.levels.wrappers.InteractionData;
import com.cursedarchie.platformer.run.RunCheckpoint;
import com.cursedarchie.platformer.run.RunCheckpointStore;
import com.cursedarchie.platformer.run.Inventory;
import com.cursedarchie.platformer.run.ItemCatalog;
import com.cursedarchie.platformer.world.GameWorld;
import com.cursedarchie.platformer.controllers.ControlBindings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Verifies a generated run and defeated spawn survive a complete GameWorld reload. */
public final class GeneratedRunPersistenceTest extends ApplicationAdapter {
    private static final String PREFS = "com.cursedarchie.platformer.test.generated-run";
    private boolean passed;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setInitialVisible(false);
        config.setWindowedMode(640, 480);
        GeneratedRunPersistenceTest test = new GeneratedRunPersistenceTest();
        new Lwjgl3Application(test, config);
        if (!test.passed) throw new AssertionError("Generated run persistence test did not complete");
        System.out.println("GENERATED RUN SAVE PASSED: seed, graph, layout, narrative choices and defeated enemy restored");
    }

    @Override public void create() {
        Preferences preferences = Gdx.app.getPreferences(PREFS);
        preferences.clear();
        preferences.flush();
        try {
            GameWorld first = new GameWorld(42L, preferences);
            String rooms;
            String topology;
            String placement;
            String narratives;
            int enemiesBefore;
            String defeatedId;
            try {
                first.changeLocation("ruins", 0);
                GeneratedRunSave save = first.getLocation().getGeneratedRun();
                rooms = roomSignature(first);
                topology = save.topologySignature;
                placement = save.placementSignature;
                narratives = save.narrativeChoices.toString("|");
                enemiesBefore = first.getEnemies().size;
                if (enemiesBefore == 0) throw new AssertionError("Generated run has no enemy to persist");
                Enemy defeated = first.getEnemies().first();
                defeatedId = defeated.getPersistentSpawnId();
                defeated.takeDamage(10000f);
                first.update(GameWorld.FIXED_STEP);
                if (first.getEnemies().size != enemiesBefore - 1 ||
                    !save.defeatedSpawnIds.contains(defeatedId, false))
                    throw new AssertionError("Defeated enemy was not recorded");
                if (first.getDefeatEffects().size != 1)
                    throw new AssertionError("Defeated enemy had no short visual feedback");
                for (int i = 0; i < 20; i++) first.update(GameWorld.FIXED_STEP);
                if (first.getDefeatEffects().size != 0)
                    throw new AssertionError("Defeat feedback did not expire");
            } finally {
                first.dispose();
            }

            GameWorld restored = new GameWorld(9999L, preferences);
            try {
                restored.changeLocation("ruins", 0);
                GeneratedRunSave save = restored.getLocation().getGeneratedRun();
                if (save.seed != 42L || !topology.equals(save.topologySignature) ||
                    !placement.equals(save.placementSignature) ||
                    !narratives.equals(save.narrativeChoices.toString("|")) ||
                    !rooms.equals(roomSignature(restored)))
                    throw new AssertionError("Reload changed the generated graph, layout, exits, or narrative choices");
                if (!save.defeatedSpawnIds.contains(defeatedId, false) ||
                    restored.getEnemies().size != enemiesBefore - 1)
                    throw new AssertionError("Reload respawned a defeated generated enemy");
                for (Enemy enemy : restored.getEnemies())
                    if (defeatedId.equals(enemy.getPersistentSpawnId()))
                        throw new AssertionError("Defeated spawn ID returned");
            } finally {
                restored.dispose();
            }
            checkStartCreatesFreshRun();
            checkSuspendResume(preferences);
            checkKeeperChoice(preferences);
            checkSlicePersistence(preferences);
            checkGeneratedAlleyVariants();
            passed = true;
        } finally {
            preferences.clear();
            preferences.flush();
            Gdx.app.exit();
        }
    }

    private void checkStartCreatesFreshRun() {
        GameWorld first = new GameWorld();
        long firstSeed;
        try {
            first.changeLocation("ruins", 0);
            firstSeed = first.getLocation().getGenerationSeed();
            if (first.getEnemies().size == 0)
                throw new AssertionError("Fresh ruins run has no enemies");
            for (Enemy enemy : first.getEnemies()) enemy.takeDamage(10000f);
            first.update(GameWorld.FIXED_STEP);
            if (first.getEnemies().size != 0)
                throw new AssertionError("First run did not record defeated enemies");
        } finally {
            first.dispose();
        }

        GameWorld retry = new GameWorld();
        try {
            retry.changeLocation("ruins", 0);
            if (retry.getLocation().getGenerationSeed() == firstSeed || retry.getEnemies().size == 0)
                throw new AssertionError("START reused the previous ruins seed or defeated enemies");
        } finally {
            retry.dispose();
        }
    }

    private void checkSuspendResume(Preferences preferences) {
        RunCheckpointStore store = new RunCheckpointStore(preferences);
        store.clearRun();
        GameWorld first = new GameWorld(718L, preferences);
        String defeatedId;
        RunCheckpoint checkpoint;
        try {
            first.getHero().getBody().setTransform(8f, 9f, 0f);
            if (!first.interact() || !first.hasRunFlag("intro_note_read") || first.interact())
                throw new AssertionError("Evidence interaction did not consume exactly once");
            if (first.getInventory().count(ItemCatalog.SEALED_NOTE) != 1 ||
                !ItemCatalog.SEALED_NOTE.equals(first.getInventory().itemId(0)))
                throw new AssertionError("Collected note is absent from the bag");
            first.getHero().getBody().setTransform(11f, 9f, 0f);
            if (!first.interact() || first.getFieldBandages() != 1 || first.interact())
                throw new AssertionError("Bandage pickup did not consume exactly once");
            if (first.getInventory().count(1) != 1 ||
                !ItemCatalog.FIELD_BANDAGE.equals(first.getInventory().itemId(1)) ||
                first.getInventory().itemId(Inventory.CAPACITY - 1) != null)
                throw new AssertionError("Pickup was not stored in the 4x4 bag");
            first.changeLocation("ruins", 0);
            Enemy defeated = first.getEnemies().first();
            defeatedId = defeated.getPersistentSpawnId();
            defeated.takeDamage(10000f);
            first.update(GameWorld.FIXED_STEP);
            first.getHero().setHealth(2f);
            checkpoint = first.checkpoint();
            store.save(checkpoint);
        } finally {
            first.dispose();
        }

        RunCheckpoint loaded = store.load();
        if (loaded == null || loaded.seed != 718L || !"ruins".equals(loaded.locationId))
            throw new AssertionError("Active run checkpoint was not loaded");
        RunCheckpoint legacy = store.load();
        legacy.formatVersion = 6;
        legacy.inventory = new com.badlogic.gdx.utils.Array<>();
        for (int i = 0; i < 18; i++) legacy.inventory.add(new RunCheckpoint.InventorySlot());
        legacy.inventory.get(10).itemId = ItemCatalog.SEALED_NOTE;
        legacy.inventory.get(10).count = 1;
        legacy.inventory.get(16).itemId = ItemCatalog.FIELD_BANDAGE;
        legacy.inventory.get(16).count = 1;
        GameWorld migrated = new GameWorld(legacy, preferences);
        try {
            if (!ItemCatalog.SEALED_NOTE.equals(migrated.getInventory().itemId(0)) ||
                !ItemCatalog.FIELD_BANDAGE.equals(migrated.getInventory().itemId(Inventory.CAPACITY)))
                throw new AssertionError("Old bag and pocket contents were not migrated");
        } finally { migrated.dispose(); }
        GameWorld resumed = new GameWorld(loaded, preferences);
        try {
            if (!"ruins".equals(resumed.getLocation().getLocation()) ||
                resumed.getFieldBandages() != 1 ||
                resumed.getInventory().count(1) != 1 ||
                resumed.getInventory().count(ItemCatalog.SEALED_NOTE) != 1 ||
                resumed.getCurrentRoom().getRoomId() != checkpoint.roomId ||
                resumed.getHero().getHealth() != 2f ||
                Math.abs(resumed.getHero().getPosition().x - checkpoint.heroX) > 0.01f ||
                Math.abs(resumed.getHero().getPosition().y - checkpoint.heroY) > 0.01f ||
                !resumed.getLocation().getGeneratedRun().defeatedSpawnIds.contains(defeatedId, false))
                throw new AssertionError("Continue did not restore the run and generated mutations");
            resumed.changeLocation("start", 0);
            resumed.getHero().getBody().setTransform(8f, 9f, 0f);
            if (!resumed.hasRunFlag("intro_note_read") || resumed.interact())
                throw new AssertionError("Consumed evidence returned after Continue");
            resumed.getHero().getBody().setTransform(11f, 9f, 0f);
            if (resumed.interact() || !resumed.hasRunFlag("first_bandage_taken") ||
                resumed.useFieldBandage() ||
                !resumed.moveToPocket(1, 0) ||
                resumed.getInventory().count(Inventory.CAPACITY) != 1)
                throw new AssertionError("Bandage did not move to pocket 1");
            store.save(resumed.checkpoint());
            RunCheckpoint withPocket = store.load();
            if (withPocket == null) throw new AssertionError("Pocket checkpoint was not saved");
            GameWorld pocketReload = new GameWorld(withPocket, preferences);
            try {
                if (pocketReload.getInventory().count(Inventory.CAPACITY) != 1 ||
                    pocketReload.getInventory().itemId(1) != null)
                    throw new AssertionError("Pocket contents did not survive Continue");
            } finally {
                pocketReload.dispose();
            }
            if (
                !resumed.useFieldBandage(Inventory.CAPACITY) || resumed.getFieldBandages() != 0 ||
                resumed.getInventory().itemId(1) != null ||
                resumed.getInventory().count(ItemCatalog.SEALED_NOTE) != 1 ||
                resumed.getHero().getHealth() != 3f || resumed.useFieldBandage())
                throw new AssertionError("Bandage pickup/use did not survive Continue");
            resumed.getHero().takeDamage(10000f);
            if (resumed.getHero().isAlive()) throw new AssertionError("Lethal damage did not end the run");
            store.clearRun();
        } finally {
            resumed.dispose();
        }
        if (store.load() != null || preferences.contains("location.ruins"))
            throw new AssertionError("Death left a resumable run or generated location");
        GameWorld fresh = new GameWorld(719L, preferences);
        try {
            if (fresh.hasRunFlag("intro_note_read"))
                throw new AssertionError("New run inherited an old story flag");
            if (fresh.getFieldBandages() != 0 || fresh.hasRunFlag("first_bandage_taken"))
                throw new AssertionError("New run inherited inventory or pickup state");
            fresh.getHero().getBody().setTransform(8f, 9f, 0f);
            if (!fresh.interact()) throw new AssertionError("New run did not restore the evidence object");
            fresh.changeLocation("ruins", 0);
            if (fresh.getLocation().getGeneratedRun().defeatedSpawnIds.contains(defeatedId, false))
                throw new AssertionError("New run inherited an old enemy defeat");
        } finally {
            fresh.dispose();
        }
    }

    private void checkKeeperChoice(Preferences preferences) {
        RunCheckpointStore store = new RunCheckpointStore(preferences);
        store.clearRun();
        GameWorld first = new GameWorld(812L, preferences);
        try {
            first.changeLocation("ruins", 0);
            RoomInstance exit = first.getLocation().getRoom(first.getLocation().getExitRoomId());
            if (exit.getLocationExit() == null ||
                !"transfer_gate".equals(exit.getLocationExit().location))
                throw new AssertionError("Final ruins room has no story exit");
            first.getHero().getBody().setTransform(exit.getWorldX() + exit.getWidth() - .2f,
                exit.getWorldY() + 3f, 0f);
            first.update(GameWorld.FIXED_STEP);
            if (!"transfer_gate".equals(first.getLocation().getLocation()))
                throw new AssertionError("Ruins exit did not enter the transfer gate");
            for (Enemy enemy : first.getEnemies())
                if ("boss".equals(enemy.getArchetypeId())) enemy.setAlive(false);
            first.update(GameWorld.FIXED_STEP);
            if (!first.hasRunFlag(GameWorld.GATE_VICTORY))
                throw new AssertionError("Gate victory was not recorded");
            first.getHero().getBody().setTransform(first.getCurrentRoom().getWorldX() +
                first.getCurrentRoom().getWidth() - .2f, 3f, 0f);
            first.update(GameWorld.FIXED_STEP);
            if (!"keeper_chamber".equals(first.getLocation().getLocation()))
                throw new AssertionError("Won gate did not open the keeper chamber");
            first.getHero().getBody().setTransform(12f, 3f, 0f);
            if (first.getNearbyInteraction() == null ||
                !"keeper".equals(first.getNearbyInteraction().id) ||
                first.interact() || !first.acceptKeeperOffer() || first.acceptKeeperOffer() ||
                first.getHero().getMaxHealth() != 2f || first.getHero().getHealth() != 2f ||
                !first.hasEchoDash())
                throw new AssertionError("Keeper offer was not applied exactly once");
            store.save(first.checkpoint());
        } finally { first.dispose(); }

        // Version 10 changes the world-unit scale, so an older generated route must
        // never be combined with this checkpoint.
        String generated = preferences.getString("location.ruins");
        if (!generated.contains("\"profileVersion\":10"))
            throw new AssertionError("Missing generated ruins profile version");
        preferences.putString("location.ruins",
            generated.replace("\"profileVersion\":10", "\"profileVersion\":9")).flush();
        try {
            new GameWorld(store.load(), preferences);
            throw new AssertionError("Incompatible ruins geometry was resumed");
        } catch (IllegalArgumentException expected) {
            if (!expected.getMessage().contains("Generated location no longer matches checkpoint"))
                throw expected;
        }
        preferences.putString("location.ruins", generated).flush();
        GameWorld resumed = new GameWorld(store.load(), preferences);
        try {
            if (!"keeper_chamber".equals(resumed.getLocation().getLocation()) ||
                !resumed.hasEchoDash() || resumed.getHero().getMaxHealth() != 2f ||
                resumed.getHero().getHealth() != 2f || resumed.acceptKeeperOffer())
                throw new AssertionError("Keeper choice did not survive Continue");
            resumed.getHeroController().setBindings(new ControlBindings(null));
            resumed.getHeroController().keyDown(Input.Keys.SHIFT_LEFT);
            resumed.update(GameWorld.FIXED_STEP);
            if (resumed.getHero().getBody().getLinearVelocity().x <=
                resumed.getHero().getVelocity())
                throw new AssertionError("Granted dash did not accelerate the hero");
            resumed.getHeroController().keyUp(Input.Keys.SHIFT_LEFT);
            resumed.changeLocation("ruins", -1);
            if (resumed.getCurrentRoom().getRoomId() != resumed.getLocation().getExitRoomId())
                throw new AssertionError("Story room did not return to the ruins exit");
        } finally { resumed.dispose(); }

        store.clearRun();
        GameWorld refused = new GameWorld(813L, preferences);
        try {
            refused.changeLocation("keeper_chamber", 0);
            refused.getHero().getBody().setTransform(12f, 3f, 0f);
            if (!refused.refuseKeeperOffer() || refused.refuseKeeperOffer() ||
                refused.acceptKeeperOffer() || refused.hasEchoDash() ||
                refused.getHero().getMaxHealth() != 3f)
                throw new AssertionError("Refusal did not lock the offer for this run");
            store.save(refused.checkpoint());
        } finally { refused.dispose(); }
        GameWorld refusedReload = new GameWorld(store.load(), preferences);
        try {
            if (!refusedReload.hasRunFlag(GameWorld.KEEPER_REFUSAL) ||
                refusedReload.acceptKeeperOffer() || refusedReload.hasEchoDash())
                throw new AssertionError("Refusal did not survive Continue");
        } finally { refusedReload.dispose(); }
        store.clearRun();
        GameWorld fresh = new GameWorld(814L, preferences);
        try {
            if (fresh.hasRunFlag(GameWorld.KEEPER_REFUSAL) || fresh.hasEchoDash() ||
                fresh.getHero().getMaxHealth() != 3f)
                throw new AssertionError("New run inherited the keeper choice");
        } finally { fresh.dispose(); }
    }

    private void checkSlicePersistence(Preferences preferences) {
        RunCheckpointStore store = new RunCheckpointStore(preferences);
        store.clearRun();
        GameWorld first = new GameWorld(41L, preferences);
        int originRoom;
        try {
            first.changeLocation("ruins", 0);
            for (RoomInstance room : first.getLocation().getRooms()) {
                for (InteractionData candidate : room.getInteractions())
                    if ("laundry_alley_entrance".equals(candidate.id)) {
                        first.changeLocation("ruins", room.getRoomId());
                        break;
                    }
            }
            originRoom = first.getCurrentRoom().getRoomId();
            InteractionData entry = interaction(first, "laundry_alley_entrance");
            first.getHero().getBody().setTransform(first.getCurrentRoom().getWorldX() +
                entry.position.x, first.getCurrentRoom().getWorldY() + entry.position.y, 0f);
            if (!first.enterPortal(entry) || !"laundry_alley".equals(first.getLocation().getLocation()) ||
                !first.hasRunFlag("laundry_access_locked"))
                throw new AssertionError("Generated street portal or locked route failed");
            first.changeLocation("laundry_alley", roomWithInteraction(first, "locked_laundry_door"));
            InteractionData locked = interaction(first, "locked_laundry_door");
            first.getHero().getBody().setTransform(locked.position.x + first.getCurrentRoom().getWorldX(),
                locked.position.y, 0f);
            if (first.enterPortal(locked)) throw new AssertionError("Locked door opened without a key");
            first.changeLocation("laundry_alley", 1);
            InteractionData key = interaction(first, "alley_door_key");
            first.getHero().getBody().setTransform(key.position.x + first.getCurrentRoom().getWorldX(),
                key.position.y, 0f);
            if (!first.interact() || !first.hasRunFlag("alley_key_found"))
                throw new AssertionError("Alley key did not unlock the door");
            first.changeLocation("laundry_alley", roomWithInteraction(first, "locked_laundry_door"));
            locked = interaction(first, "locked_laundry_door");
            first.getHero().getBody().setTransform(locked.position.x + first.getCurrentRoom().getWorldX(),
                locked.position.y, 0f);
            if (!first.enterPortal(locked)) throw new AssertionError("Unlocked laundry door failed");
            Enemy collector = first.getEnemies().first();
            collector.takeDamage(10000f);
            first.update(GameWorld.FIXED_STEP);
            if (!first.hasRunFlag("defeated:laundry_house:0:0"))
                throw new AssertionError("Authored enemy defeat was not recorded");
            first.changeLocation("catchment_cellar", 0);
            InteractionData prisoner = interaction(first, "cellar_prisoner");
            first.getHero().getBody().setTransform(prisoner.position, 0f);
            if (!first.resolvePrisonerChoice(0) || !first.hasRunFlag("cellar_key_found") ||
                !first.hasRunFlag(GameWorld.PRISONER_ESCORTED))
                throw new AssertionError("Prisoner decision did not unlock the vault");
            store.save(first.checkpoint());
        } finally { first.dispose(); }
        GameWorld resumed = new GameWorld(store.load(), preferences);
        try {
            if (!"catchment_cellar".equals(resumed.getLocation().getLocation()) ||
                !resumed.hasRunFlag("alley_key_found") || !resumed.hasRunFlag("cellar_key_found") ||
                !resumed.hasRunFlag(GameWorld.PRISONER_ESCORTED))
                throw new AssertionError("Route or prisoner decision was lost on Continue");
            resumed.changeLocation("laundry_house", 0);
            if (resumed.getEnemies().size != 0)
                throw new AssertionError("Defeated collector respawned after Continue");
            resumed.changeLocation("laundry_alley", 0);
            InteractionData exit = interaction(resumed, "street_return");
            resumed.getHero().getBody().setTransform(exit.position, 0f);
            if (!resumed.enterPortal(exit) || !"ruins".equals(resumed.getLocation().getLocation()) ||
                resumed.getCurrentRoom().getRoomId() != originRoom)
                throw new AssertionError("Return portal lost its generated origin");
        } finally { resumed.dispose(); }
    }

    private InteractionData interaction(GameWorld world, String id) {
        for (InteractionData value : world.getCurrentRoom().getInteractions())
            if (id.equals(value.id)) return value;
        throw new AssertionError("Missing interaction: " + id);
    }

    private int roomWithInteraction(GameWorld world, String id) {
        for (RoomInstance room : world.getLocation().getRooms())
            for (InteractionData value : room.getInteractions())
                if (id.equals(value.id)) return room.getRoomId();
        throw new AssertionError("Missing interaction: " + id);
    }

    private void checkGeneratedAlleyVariants() {
        boolean[] lengths = new boolean[4];
        for (long seed = 0; seed < 24; seed++) {
            GameWorld world = new GameWorld(seed);
            try {
                world.changeLocation("laundry_alley", 0);
                int count = world.getLocation().getRooms().size();
                if (count < 4 || count > 7) throw new AssertionError("Invalid alley length: " + count);
                lengths[count - 4] = true;
                if (roomWithInteraction(world, "street_return") != 0 ||
                    roomWithInteraction(world, "locked_laundry_door") != count - 2 ||
                    roomWithInteraction(world, "alley_cellar_stairs") != count - 1)
                    throw new AssertionError("Required alley landmarks changed order");
                world.changeLocation("laundry_house", 0);
                if (interaction(world, "alley_return").targetRoomId != count - 2)
                    throw new AssertionError("Laundry return targets the wrong generated room");
                world.changeLocation("catchment_cellar", 0);
                if (interaction(world, "alley_return").targetRoomId != count - 1)
                    throw new AssertionError("Cellar return targets the wrong generated room");
            } finally { world.dispose(); }
        }
        for (boolean seen : lengths)
            if (!seen) throw new AssertionError("Alley room count does not vary across seeds");
    }

    private String roomSignature(GameWorld world) {
        List<RoomInstance> rooms = new ArrayList<>(world.getLocation().getRooms());
        rooms.sort(Comparator.comparingInt(RoomInstance::getRoomId));
        StringBuilder result = new StringBuilder();
        for (RoomInstance room : rooms) {
            result.append(room.getRoomId()).append('@').append(room.getWorldX()).append(',')
                .append(room.getWorldY()).append(':').append(room.getWidth()).append('x')
                .append(room.getHeight()).append(':').append(room.getLeftNeighbor()).append(',')
                .append(room.getRightNeighbor()).append(',').append(room.getUpNeighbor()).append(',')
                .append(room.getDownNeighbor()).append(':');
            if (room.getLocationEntry() != null)
                result.append("entry=").append(room.getLocationEntry().location).append('/')
                    .append(room.getLocationEntry().id);
            if (room.getLocationExit() != null)
                result.append("exit=").append(room.getLocationExit().location).append('/')
                    .append(room.getLocationExit().id);
            result.append(';');
        }
        return result.toString();
    }
}
