package com.cursedarchie.platformer.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.levels.GeneratedRunSave;
import com.cursedarchie.platformer.levels.RoomInstance;
import com.cursedarchie.platformer.world.GameWorld;

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
            passed = true;
        } finally {
            preferences.clear();
            preferences.flush();
            Gdx.app.exit();
        }
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
