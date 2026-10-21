package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.levels.wrappers.Neighbors;
import com.cursedarchie.platformer.levels.wrappers.RoomData;
import com.cursedarchie.platformer.physics.GameContactListener;
import com.cursedarchie.platformer.physics.OneWaySurface;
import com.cursedarchie.platformer.tiles.types.BreakableBlock;
import com.cursedarchie.platformer.world.GameWorld;

/** Native Box2D regression for one-way support and per-cell breakable fixtures. */
public final class InteractiveTilePhysicsTest extends ApplicationAdapter {
    private boolean passed;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setInitialVisible(false);
        config.setWindowedMode(640, 480);
        InteractiveTilePhysicsTest test = new InteractiveTilePhysicsTest();
        new Lwjgl3Application(test, config);
        if (!test.passed) throw new AssertionError("Interactive tile test did not complete");
        System.out.println("INTERACTIVE TILES PASSED: merged ledges, jump-through, top landing, isolated destruction, saved destruction");
    }

    @Override public void create() {
        World physics = new World(new Vector2(0f, -GameWorld.GRAVITY), true);
        try {
            LocationInstance location = roomWithTiles();
            location.buildBodies(physics);
            if (fixtureCount(physics, 4.05f, 3.85f, 6.95f, 3.99f, true) != 1)
                throw new AssertionError("Adjacent wooden slabs did not share one fixture");
            if (fixtureCount(physics, 1.05f, 3.1f, 3.95f, 3.4f, false) != 1)
                throw new AssertionError("Adjacent stone half-blocks did not share one fixture");
            physics.setContactListener(new GameContactListener());
            Hero hero = new Hero(new Vector2(5.5f, 2.1f), physics);
            hero.getBody().setLinearVelocity(0f, Hero.JUMP_SPEED);
            boolean crossed = false;
            for (int i = 0; i < 40; i++) {
                physics.step(GameWorld.FIXED_STEP, 6, 2);
                if (hero.getPosition().y > 4.4f) crossed = true;
            }
            if (!crossed) throw new AssertionError("Hero could not pass upward through one-way ledge");
            for (int i = 0; i < 90; i++) physics.step(GameWorld.FIXED_STEP, 6, 2);
            if (Math.abs(hero.getPosition().y - 4.25f) > 0.12f)
                throw new AssertionError("Hero did not land on one-way ledge: " + hero.getPosition().y);

            hero.getBody().setTransform(4.3f, 4.25f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            for (int i = 0; i < 4; i++) physics.step(GameWorld.FIXED_STEP, 6, 2);
            for (int direction : new int[] {1, -1}) {
                for (int i = 0; i < 30; i++) {
                    hero.getBody().setLinearVelocity(direction * 5f, 0f);
                    physics.step(GameWorld.FIXED_STEP, 6, 2);
                    if (Math.abs(hero.getPosition().y - 4.25f) > 0.1f)
                        throw new AssertionError("Hero snagged or fell at a wooden slab seam");
                }
                if ((direction == 1 && hero.getPosition().x < 6.6f) ||
                    (direction == -1 && hero.getPosition().x > 4.5f))
                    throw new AssertionError("Hero could not cross the wooden slab seams");
            }

            if (!hasFixture(physics, 3.5f, 1.5f))
                throw new AssertionError("Breakable tile has no starting fixture");
            if (!location.breakTile(0, 3, 1) || hasFixture(physics, 3.5f, 1.5f) ||
                location.getRoom(0).getTiles()[3][1] != null)
                throw new AssertionError("Breaking tile did not remove its own art and fixture");
            if (!hasFixture(physics, 2.5f, 0.5f))
                throw new AssertionError("Breaking tile damaged unrelated terrain");

            location.destroyBodies(physics);
            GeneratedRunSave save = new GeneratedRunSave();
            save.brokenTileIds.add("0:3:1");
            LocationInstance restored = roomWithTiles();
            restored.setGeneration(save, 0);
            if (restored.getRoom(0).getTiles()[3][1] instanceof BreakableBlock)
                throw new AssertionError("Saved broken tile reappeared before terrain build");
            restored.buildBodies(physics);
            if (hasFixture(physics, 3.5f, 1.5f))
                throw new AssertionError("Saved broken tile regained a fixture");
            restored.destroyBodies(physics);
            physics.destroyBody(hero.getBody());
            verifyJoinedRoomBoundary(physics);
            verifySwordBreaksGeneratedTile();
            passed = true;
        } finally {
            physics.dispose();
            Gdx.app.exit();
        }
    }

    private static void verifySwordBreaksGeneratedTile() {
        GameWorld game = new GameWorld(42L);
        try {
            game.changeLocation("ruins", 0);
            for (com.cursedarchie.platformer.actors.enemies.Enemy enemy : game.getEnemies())
                enemy.getBody().setActive(false);
            RoomInstance targetRoom = null;
            int targetX = -1, targetY = -1;
            for (RoomInstance room : game.getLocation().getRooms()) {
                for (int x = 1; x < room.getWidth() - 1; x++) {
                    for (int y = 1; y < room.getHeight() - 1; y++) {
                        if (room.getTiles()[x][y] instanceof BreakableBlock &&
                            room.getTiles()[x - 1][y] == null) {
                            targetRoom = room;
                            targetX = x;
                            targetY = y;
                            break;
                        }
                    }
                    if (targetRoom != null) break;
                }
                if (targetRoom != null) break;
            }
            if (targetRoom == null) throw new AssertionError("Generated ruins contain no reachable breakable tile");
            float tileX = targetRoom.getWorldX() + targetX;
            float tileY = targetRoom.getWorldY() + targetY;
            game.getHero().getBody().setTransform(tileX - 0.55f, tileY + 0.5f, 0f);
            game.getHero().getBody().setLinearVelocity(0f, 0f);
            game.getHero().setFacingLeft(false);
            game.getHeroController().keyDown(Input.Keys.X);
            for (int i = 0; i < 16; i++) game.update(GameWorld.FIXED_STEP);
            game.getHeroController().keyUp(Input.Keys.X);
            if (targetRoom.getTiles()[targetX][targetY] != null ||
                !game.getLocation().getGeneratedRun().brokenTileIds.contains(
                    targetRoom.getRoomId() + ":" + targetX + ":" + targetY, false))
                throw new AssertionError("X attack did not break and record the generated tile");
        } finally {
            game.dispose();
        }
    }

    private static LocationInstance roomWithTiles() {
        RoomData data = new RoomData();
        data.location = "test";
        data.id = 0;
        data.width = 10;
        data.height = 8;
        data.tileRows = new String[] {
            "          ", "          ", "          ", "          ",
            " ___===   ", "          ", "   B      ", "##########"
        };
        data.neighbors = new Neighbors();
        RoomInstance room = new RoomInstance(data);
        room.buildTiles();
        LocationInstance location = new LocationInstance("test");
        location.addRoom(room);
        return location;
    }

    private static void verifyJoinedRoomBoundary(World physics) {
        LocationInstance location = new LocationInstance("seam-test");
        for (int id = 0; id < 2; id++) {
            RoomData data = new RoomData();
            data.location = "seam-test";
            data.id = id;
            data.width = 3;
            data.height = 3;
            data.worldX = id * 3;
            data.tileRows = id == 0 ?
                new String[] {"   ", "  =", "###"} :
                new String[] {"   ", "=  ", "###"};
            data.neighbors = new Neighbors();
            RoomInstance room = new RoomInstance(data);
            room.buildTiles();
            location.addRoom(room);
        }
        location.buildBodies(physics);
        if (fixtureCount(physics, 2.05f, 1.85f, 3.95f, 1.99f, true) != 1)
            throw new AssertionError("Wooden slabs across rooms retained a collision seam");
        location.destroyBodies(physics);
    }

    private static boolean hasFixture(World world, float x, float y) {
        boolean[] found = {false};
        world.QueryAABB(fixture -> {
            if (fixture.testPoint(x, y)) found[0] = true;
            return true;
        }, x - 0.01f, y - 0.01f, x + 0.01f, y + 0.01f);
        return found[0];
    }

    private static int fixtureCount(World world, float minX, float minY,
                                    float maxX, float maxY, boolean oneWay) {
        int[] count = {0};
        world.QueryAABB((Fixture fixture) -> {
            if ((fixture.getUserData() instanceof OneWaySurface) == oneWay) count[0]++;
            return true;
        }, minX, minY, maxX, maxY);
        return count[0];
    }
}
