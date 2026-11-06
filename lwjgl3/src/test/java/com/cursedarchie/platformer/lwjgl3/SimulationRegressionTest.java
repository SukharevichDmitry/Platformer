package com.cursedarchie.platformer.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.RangedAttackConfig;
import com.cursedarchie.platformer.actors.enemies.EnemyFactory;
import com.cursedarchie.platformer.actors.enemies.logic.SimpleEnemyBrain;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.enemies.states.combat.AttackState;
import com.cursedarchie.platformer.actors.enemies.states.motion.MotionIdleState;
import com.cursedarchie.platformer.actors.enemies.states.motion.ChaseState;
import com.cursedarchie.platformer.actors.enemies.states.motion.PatrolState;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroState;
import com.cursedarchie.platformer.actors.hero.states.motion.MoveState;
import com.cursedarchie.platformer.run.ItemCatalog;
import com.cursedarchie.platformer.physics.CombatHit;
import com.cursedarchie.platformer.combat.AttackDefinition;
import com.cursedarchie.platformer.combat.AttackRuntime;
import com.cursedarchie.platformer.world.GameWorld;
import com.cursedarchie.platformer.world.LocationTitleOverlay;

/** Integration check for fixed simulation steps and state-machine timing. */
public final class SimulationRegressionTest extends ApplicationAdapter {
    private GameWorld world;
    private boolean passed;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setInitialVisible(false);
        SimulationRegressionTest test = new SimulationRegressionTest();
        new Lwjgl3Application(test, config);
        if (!test.passed) throw new AssertionError("Simulation regression test did not complete");
        System.out.println("SIMULATION PASSED: fixed steps, stable held state and enemy awareness");
    }

    @Override public void create() {
        world = combatFixture();
        Hero hero = world.getHero();
        world.getHeroController().keyDown(Input.Keys.X);
        world.update(GameWorld.FIXED_STEP / 2f);
        closeTo(hero.getCombatStateTime(), 0f, "half step advanced combat");
        world.update(GameWorld.FIXED_STEP / 2f);
        closeTo(hero.getCombatStateTime(), GameWorld.FIXED_STEP, "combat did not advance once");
        world.update(GameWorld.FIXED_STEP * 4f);
        closeTo(hero.getCombatStateTime(), GameWorld.FIXED_STEP * 5f, "four steps were lost");
        world.update(1f);
        closeTo(hero.getCombatStateTime(), GameWorld.FIXED_STEP * 11f, "stall exceeded six steps");

        hero.getMotionStateMachine().changeState(new MoveState());
        HeroState walking = hero.getMotionStateMachine().getCurrentState();
        world.getHeroController().keyDown(Input.Keys.RIGHT);
        world.update(GameWorld.FIXED_STEP);
        if (hero.getMotionStateMachine().getCurrentState() != walking)
            throw new AssertionError("Held movement re-entered the same motion state");
        closeTo(hero.getMotionStateTime(), GameWorld.FIXED_STEP, "motion advanced more than once");
        world.update(GameWorld.FIXED_STEP);
        if (hero.getMotionStateMachine().getCurrentState() != walking)
            throw new AssertionError("Held movement reset the motion state");
        closeTo(hero.getMotionStateTime(), GameWorld.FIXED_STEP * 2f, "motion timing is inconsistent");
        checkEnemyAwareness();
        checkEnemySearchMemory();
        checkEnemyStopsAtAttackRange();
        checkHeroMovementAndContacts();
        checkDropThroughPlatform();
        checkJumpBuffer();
        checkHeadLandingFall();
        checkHeadJumpOnPushback();
        checkLightStrikeSingleTarget();
        checkRenderRateConsistency();
        checkUnifiedAttackModel();
        checkHeroAttackTimingAndDirection();
        checkCombatExtensions();
        checkLockedAttackDirection();
        checkSpecialEnemyRoles();
        checkScavengerActionPriority();
        checkRaiderBlockAndDash();
        checkHeroBlockRules();
        checkNewEnemyRoles();
        checkGroupAttackSlots();
        checkGroupPositioning();
        checkRollCounterAndJumpAttack();
        checkArrowArcPhysics();
        checkAttackRhythm();
        checkThreeHitFinisherRules();
        checkDodgeRoll();
        checkFlowTweaks();
        checkArcherMemoryAndMirroredTurn();
        checkArrowCeilingAndSightLoss();
        checkMeleeReachMatchesHitbox();
        checkRapidTurnAndAwareness();
        checkEnemyLungeRecovery();
        checkEnemyAttackAndDamageCooldown();
        checkLocationTitleTiming();
        checkInteractionInput();
        checkCombatTestRoom();
        checkAlleyPortalsAndChoices();
        checkSliceItemsAndGuard();
        checkArrowCover();
        checkArcherActionReadability();
        passed = true;
        Gdx.app.exit();
    }

    /** Combat fixtures stay in the regression test, not in the hero's home. */
    private static GameWorld combatFixture() {
        GameWorld result = new GameWorld();
        Enemy regular = EnemyFactory.createDefaultEnemy(result.getWorld(), new Vector2(18f, 4f));
        Enemy boss = EnemyFactory.createBoss(result.getWorld(), new Vector2(14f, 4f));
        regular.setBrain(new SimpleEnemyBrain(result));
        boss.setBrain(new SimpleEnemyBrain(result));
        result.addEnemy(regular);
        result.addEnemy(boss);
        return result;
    }

    private void checkLocationTitleTiming() {
        GameWorld titleWorld = new GameWorld(42L);
        LocationTitleOverlay title = new LocationTitleOverlay(titleWorld);
        try {
            title.update(0f);
            if (!"Start".equals(title.getTitle()))
                throw new AssertionError("Initial location title is missing");
            closeTo(title.getOpacity(), 0f, "location title did not start transparent");
            title.update(0.2f);
            closeTo(title.getOpacity(), 0.5f, "location title did not fade in");
            title.update(0.2f);
            closeTo(title.getOpacity(), 1f, "location title did not become fully visible");
            title.update(2f);
            closeTo(title.getOpacity(), 1f, "location title did not hold");
            title.update(0.4f);
            closeTo(title.getOpacity(), 0.5f, "location title did not fade out");
            title.update(0.4f);
            closeTo(title.getOpacity(), 0f, "location title did not disappear");

            com.cursedarchie.platformer.levels.RoomInstance nextRoom = titleWorld.getLocation().getRoom(1);
            titleWorld.getHero().getBody().setTransform(nextRoom.getWorldX() + 4f,
                nextRoom.getWorldY() + 4f, 0f);
            titleWorld.update(GameWorld.FIXED_STEP);
            if (titleWorld.getCurrentRoom() != nextRoom)
                throw new AssertionError("Location-title room-change check did not enter room 1");
            title.update(GameWorld.FIXED_STEP);
            closeTo(title.getOpacity(), 0f, "room movement replayed the location title");

            titleWorld.changeLocation("ruins", 0);
            title.update(0f);
            if (!"Ruins".equals(title.getTitle()))
                throw new AssertionError("New location title is missing");
            closeTo(title.getOpacity(), 0f, "new location title did not restart its fade");
            title.update(0.2f);
            closeTo(title.getOpacity(), 0.5f, "new location title did not fade in");
        } finally {
            title.dispose();
            titleWorld.dispose();
        }
    }

    private void checkInteractionInput() {
        GameWorld interactionWorld = new GameWorld(31L);
        try {
            for (int i = 0; i < 30; i++) interactionWorld.update(GameWorld.FIXED_STEP);
            if (interactionWorld.getNearbyInteraction() == null ||
                !"first_bandage".equals(interactionWorld.getNearbyInteraction().id))
                throw new AssertionError("Bandage is not reachable from the initial spawn");
            interactionWorld.getHero().getBody().setTransform(8f, 9f, 0f);
            interactionWorld.getHeroController().keyDown(Input.Keys.E);
            interactionWorld.update(GameWorld.FIXED_STEP);
            if (!interactionWorld.hasRunFlag("intro_note_read"))
                throw new AssertionError("E did not activate nearby evidence");
            long revision = interactionWorld.getPersistenceRevision();
            interactionWorld.getHeroController().keyDown(Input.Keys.E);
            interactionWorld.update(GameWorld.FIXED_STEP);
            interactionWorld.getHeroController().keyUp(Input.Keys.E);
            interactionWorld.getHeroController().keyDown(Input.Keys.E);
            interactionWorld.update(GameWorld.FIXED_STEP);
            if (interactionWorld.getPersistenceRevision() != revision)
                throw new AssertionError("Consumed evidence activated again");
            interactionWorld.getHero().getBody().setTransform(11f, 9f, 0f);
            interactionWorld.getHeroController().keyUp(Input.Keys.E);
            interactionWorld.getHeroController().keyDown(Input.Keys.E);
            interactionWorld.update(GameWorld.FIXED_STEP);
            if (interactionWorld.getFieldBandages() != 1)
                throw new AssertionError("E did not collect the bandage");
            interactionWorld.getHero().setHealth(2f);
            interactionWorld.getHeroController().keyDown(Input.Keys.C);
            interactionWorld.update(GameWorld.FIXED_STEP);
            if (interactionWorld.getFieldBandages() != 1 || interactionWorld.getHero().getHealth() != 2f)
                throw new AssertionError("Bag bandage was used without a pocket");
            interactionWorld.getHeroController().keyUp(Input.Keys.C);
            if (!interactionWorld.moveToPocket(1, 0))
                throw new AssertionError("Bandage did not move into pocket 1");
            interactionWorld.getHeroController().keyDown(Input.Keys.NUM_1);
            interactionWorld.update(GameWorld.FIXED_STEP);
            if (interactionWorld.getFieldBandages() != 0 || interactionWorld.getHero().getHealth() != 3f)
                throw new AssertionError("Pocket 1 did not use the bandage once");
            interactionWorld.getHeroController().keyUp(Input.Keys.E);
            interactionWorld.changeLocation("start", 1);
            interactionWorld.getHero().getBody().setTransform(
                interactionWorld.getCurrentRoom().getWorldX() + 19f, 3f, 0f);
            interactionWorld.getHeroController().keyDown(Input.Keys.E);
            interactionWorld.update(GameWorld.FIXED_STEP);
            if (interactionWorld.getInventory().count(ItemCatalog.BLADE_SHARD) != 1)
                throw new AssertionError("Blade shard pickup did not enter the bag");
            interactionWorld.getHeroController().keyUp(Input.Keys.E);
            interactionWorld.changeLocation("start", 3);
            interactionWorld.getHero().getBody().setTransform(
                interactionWorld.getCurrentRoom().getWorldX() + 22f, 3f, 0f);
            interactionWorld.getHeroController().keyDown(Input.Keys.E);
            interactionWorld.update(GameWorld.FIXED_STEP);
            int bladeSlot = inventorySlot(interactionWorld, ItemCatalog.BLADE_SHARD);
            int impactSlot = inventorySlot(interactionWorld, ItemCatalog.IMPACT_CORE);
            if (interactionWorld.combineItems(bladeSlot, bladeSlot) ||
                interactionWorld.getInventory().count(ItemCatalog.BLADE_SHARD) != 1 ||
                interactionWorld.getInventory().count(ItemCatalog.IMPACT_CORE) != 1)
                throw new AssertionError("Invalid fusion consumed an ingredient");
            if (interactionWorld.getInventory().count(ItemCatalog.IMPACT_CORE) != 1 ||
                !interactionWorld.combineItems(bladeSlot, impactSlot) ||
                interactionWorld.getInventory().count(ItemCatalog.FUSED_RELIC) != 1 ||
                interactionWorld.getHero().meleeDamageMultiplier(3) <= 1.3f)
                throw new AssertionError("Relics did not combine into the upgraded item");
            if (!interactionWorld.moveInventorySlot(0, 1) ||
                !ItemCatalog.SEALED_NOTE.equals(interactionWorld.getInventory().itemId(1)) ||
                !ItemCatalog.FUSED_RELIC.equals(interactionWorld.getInventory().itemId(0)))
                throw new AssertionError("Bag cell exchange lost item identities");
            if (!interactionWorld.moveInventorySlot(0, 7) ||
                !interactionWorld.moveInventorySlot(7, 2) ||
                !ItemCatalog.FUSED_RELIC.equals(interactionWorld.getInventory().itemId(2)))
                throw new AssertionError("Pocket transfer or return lost the relic");
            com.cursedarchie.platformer.run.RunCheckpoint saved = interactionWorld.checkpoint();
            saved.formatVersion = 7;
            GameWorld resumed = new GameWorld(saved, null);
            try {
                if (resumed.getInventory().count(ItemCatalog.BLADE_SHARD) != 0 ||
                    resumed.getInventory().count(ItemCatalog.IMPACT_CORE) != 0 ||
                    resumed.getInventory().count(ItemCatalog.FUSED_RELIC) != 1 ||
                    !ItemCatalog.SEALED_NOTE.equals(resumed.getInventory().itemId(1)) ||
                    !ItemCatalog.FUSED_RELIC.equals(resumed.getInventory().itemId(2)) ||
                    resumed.getHero().meleeDamageMultiplier(3) <= 1.3f)
                    throw new AssertionError("Relic synergy did not survive a checkpoint restore");
            } finally {
                resumed.dispose();
            }
        } finally {
            interactionWorld.dispose();
        }
    }

    private void checkCombatTestRoom() {
        GameWorld testWorld = new GameWorld(734L);
        try {
            Vector2 origin = new Vector2(testWorld.getHero().getPosition());
            String originLocation = testWorld.getLocation().getLocation();
            testWorld.toggleCombatTestRoom();
            if (!testWorld.isCombatTestLocation() ||
                !"combat_test".equals(testWorld.getLocation().getLocation()))
                throw new AssertionError("F8 combat room entry did not change location");
            closeTo(testWorld.getHero().getPosition().x, 6f, "combat room entry x");

            boolean raider = false, archer = false, infiltrator = false;
            boolean breaker = false, overseer = false;
            for (Enemy enemy : testWorld.getEnemies()) {
                raider |= "default".equals(enemy.getArchetypeId());
                archer |= "scavenger".equals(enemy.getArchetypeId());
                infiltrator |= "infiltrator".equals(enemy.getArchetypeId());
                breaker |= "heavy_hunter".equals(enemy.getArchetypeId());
                overseer |= "overseer".equals(enemy.getArchetypeId());
            }
            if (testWorld.getEnemies().size != 5 || !raider || !archer || !infiltrator ||
                !breaker || !overseer)
                throw new AssertionError("Combat room is missing one of the five test roles");

            com.cursedarchie.platformer.levels.RoomInstance room = testWorld.getCurrentRoom();
            if (room.getTiles()[34][3] != null || room.getTiles()[34][1] == null ||
                room.getTiles()[0][8] == null || room.getTiles()[71][8] == null)
                throw new AssertionError("Combat room pit, catch floor, or side walls are missing");

            Enemy source = testWorld.getEnemies().first();
            testWorld.fireArrows(source, new Vector2(-1f, 0f), RangedAttackConfig.NORMAL);
            testWorld.getHero().setHealth(1f);
            source.takeDamage(source.getHealth());
            testWorld.update(GameWorld.FIXED_STEP);
            testWorld.resetCombatTestRoom();
            if (testWorld.getArrows().size != 0 || testWorld.getEnemies().size != 5 ||
                testWorld.getHero().getHealth() != testWorld.getHero().getMaxHealth() ||
                !testWorld.getHero().isAlive())
                throw new AssertionError("F9 did not fully reset the combat room");

            testWorld.spawnRoleShowcase();
            if (testWorld.getEnemies().size != 5)
                throw new AssertionError("F7 did not replace the lineup with the mixed-group test wave");
            testWorld.spawnCombatTestPair();
            if (testWorld.getEnemies().size != 2)
                throw new AssertionError("F6 did not replace the wave with the Raider/Archer pair");

            testWorld.toggleCombatTestRoom();
            if (!originLocation.equals(testWorld.getLocation().getLocation()) ||
                !testWorld.getHero().getPosition().epsilonEquals(origin, .01f))
                throw new AssertionError("F8 did not return from the combat room to its origin");
        } finally {
            testWorld.dispose();
        }
    }

    private void checkEnemyAwareness() {
        GameWorld attackWorld = combatFixture();
        try {
            Enemy enemy = null;
            for (Enemy candidate : attackWorld.getEnemies()) {
                if (candidate.getSize() < 2f) enemy = candidate;
                else candidate.setAlive(false);
            }
            if (enemy == null) throw new AssertionError("No regular enemy to test");

            Hero target = attackWorld.getHero();
            enemy.setFacingLeft(true);
            enemy.getBody().setTransform(18f, 4f, 0);
            target.getBody().setTransform(19f, 4f, 0);
            attackWorld.update(GameWorld.FIXED_STEP);
            if (!enemy.getAttackTargets().contains(target))
                throw new AssertionError("Behind approach did not enter the attack range sensor");
            if (!enemy.isCanSeeHero())
                throw new AssertionError("Enemy lost the nearby hero solely because it was behind him");

            target.getBody().setTransform(17f, 4f, 0);
            target.getBody().setLinearVelocity(0, 0);
            for (int i = 0; i < 35; i++) attackWorld.update(GameWorld.FIXED_STEP);
            if (!enemy.isCanSeeHero() || !enemy.isCanAttackHero() ||
                !(enemy.getCombatStateMachine().getCurrentState() instanceof AttackState))
                throw new AssertionError("Enemy did not attack the hero inside its front range");
        } finally {
            attackWorld.dispose();
        }
    }

    private void checkEnemySearchMemory() {
        GameWorld search = new GameWorld();
        try {
            Enemy enemy = EnemyFactory.createDefaultEnemy(search.getWorld(), new Vector2(17f, 2.5f));
            enemy.setBrain(new SimpleEnemyBrain(search));
            search.addEnemy(enemy);
            Hero hero = search.getHero();
            hero.getBody().setTransform(11f, 2.5f, 0f);
            enemy.setFacingLeft(true);
            for (int i = 0; i < 3; i++) search.update(GameWorld.FIXED_STEP);
            if (!enemy.isCanSeeHero()) throw new AssertionError("Enemy never acquired search target");
            hero.getBody().setTransform(32f, 2.5f, 0f);
            search.update(GameWorld.FIXED_STEP);
            if (!(enemy.getMotionStateMachine().getCurrentState() instanceof ChaseState))
                throw new AssertionError("Enemy forgot the hero immediately after losing sight");
            for (int i = 0; i < 120; i++) search.update(GameWorld.FIXED_STEP);
            if (!(enemy.getMotionStateMachine().getCurrentState() instanceof PatrolState))
                throw new AssertionError("Enemy did not resume patrol after searching");
        } finally {
            search.dispose();
        }
    }

    private void checkEnemyStopsAtAttackRange() {
        checkEnemyStopsAtAttackRange(false);
        checkEnemyStopsAtAttackRange(true);
    }

    private void checkEnemyStopsAtAttackRange(boolean boss) {
        GameWorld chaseWorld = combatFixture();
        try {
            Enemy attacker = null;
            for (Enemy enemy : chaseWorld.getEnemies()) {
                if ((enemy.getSize() > 2f) == boss) attacker = enemy;
                else enemy.setAlive(false);
            }
            if (attacker == null) throw new AssertionError("Missing enemy for chase range check");
            Hero hero = chaseWorld.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            attacker.setFacingLeft(true);
            attacker.getBody().setTransform(16f, boss ? 3.5f : 2.5f, 0f);
            attacker.getBody().setLinearVelocity(0f, 0f);

            // The enemy walks to its combat slot (just inside its reach) and stops there.
            for (int i = 0; i < 200 && !(attacker.isCanAttackHero() &&
                Math.abs(attacker.getBody().getLinearVelocity().x) < .01f); i++)
                chaseWorld.update(GameWorld.FIXED_STEP);
            if (!attacker.isCanAttackHero())
                throw new AssertionError("Enemy never reached its attack range: boss=" + boss);
            float heroX = hero.getPosition().x;
            float attackerX = attacker.getPosition().x;
            for (int i = 0; i < 20; i++) chaseWorld.update(GameWorld.FIXED_STEP);
            if (!(attacker.getMotionStateMachine().getCurrentState() instanceof MotionIdleState) ||
                Math.abs(attacker.getBody().getLinearVelocity().x) > 0.01f)
                throw new AssertionError("Enemy kept chasing inside attack range: boss=" + boss);
            if (Math.abs(hero.getPosition().x - heroX) > 0.1f ||
                Math.abs(attacker.getPosition().x - attackerX) > 0.1f)
                throw new AssertionError("Enemy pushed the hero while attacking: boss=" + boss);
            if (boss) {
                attacker.setBrain(null);
                attacker.setIntent(Enemy.Intent.IDLE);
                attacker.getBody().setTransform(14f, 3.5f, 0f);
                attacker.getBody().setLinearVelocity(0f, 0f);
                chaseWorld.update(GameWorld.FIXED_STEP);
                if (attacker.getAttackTargets().contains(hero))
                    throw new AssertionError("Boss attack sensor still reaches two units from its body");
            }
        } finally {
            chaseWorld.dispose();
        }
    }

    private void checkDropThroughPlatform() {
        GameWorld dropWorld = combatFixture();
        try {
            dropWorld.changeLocation("forest", 0);
            com.badlogic.gdx.physics.box2d.BodyDef bodyDef = new com.badlogic.gdx.physics.box2d.BodyDef();
            bodyDef.type = com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody;
            bodyDef.position.set(4f, 8.9f);
            com.badlogic.gdx.physics.box2d.Body ledge = dropWorld.getWorld().createBody(bodyDef);
            ledge.setUserData("level");
            com.badlogic.gdx.physics.box2d.PolygonShape shape = new com.badlogic.gdx.physics.box2d.PolygonShape();
            shape.setAsBox(2f, .1f);
            com.badlogic.gdx.physics.box2d.Fixture fixture = ledge.createFixture(shape, 0f);
            com.badlogic.gdx.physics.box2d.Filter filter = fixture.getFilterData();
            filter.categoryBits = com.cursedarchie.platformer.physics.PhysicsCategories.GROUND;
            filter.maskBits = com.cursedarchie.platformer.physics.PhysicsCategories.HERO;
            fixture.setFilterData(filter);
            fixture.setUserData(new com.cursedarchie.platformer.physics.OneWaySurface(9f));
            shape.dispose();
            Hero hero = dropWorld.getHero();
            hero.getBody().setTransform(4f, 10.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            for (int i = 0; i < 30; i++) dropWorld.update(GameWorld.FIXED_STEP);
            if (!dropWorld.isHeroOnOneWaySurface())
                throw new AssertionError("Hero did not settle on a one-way platform");
            dropWorld.getHeroController().keyDown(Input.Keys.DOWN);
            for (int i = 0; i < 15; i++) dropWorld.update(GameWorld.FIXED_STEP);
            if (hero.getPosition().y >= 9f || dropWorld.isHeroOnOneWaySurface())
                throw new AssertionError("Down did not drop through the one-way platform");
        } finally {
            dropWorld.dispose();
        }
    }

    private void checkHeroMovementAndContacts() {
        GameWorld movementWorld = combatFixture();
        try {
            movementWorld.changeLocation("forest", 0);
            Hero hero = movementWorld.getHero();
            hero.getBody().setTransform(4f, 2.7f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            for (int i = 0; i < 15; i++) movementWorld.update(GameWorld.FIXED_STEP);
            if (!hero.isGrounded()) throw new AssertionError("Hero did not settle on the forest floor");

            float startY = hero.getPosition().y;
            movementWorld.getHeroController().keyDown(Input.Keys.SPACE);
            movementWorld.update(GameWorld.FIXED_STEP);
            if (hero.getBody().getLinearVelocity().y <= 0f)
                throw new AssertionError("Jump did not respond during the next physics step");
            float apex = hero.getPosition().y;
            boolean landed = false;
            for (int i = 0; i < 70; i++) {
                movementWorld.update(GameWorld.FIXED_STEP);
                apex = Math.max(apex, hero.getPosition().y);
                if (hero.isGrounded() && hero.getBody().getLinearVelocity().y <= 0.1f) landed = true;
                if (landed && hero.getBody().getLinearVelocity().y > 0.5f)
                    throw new AssertionError("Holding Space caused an automatic second jump");
            }
            if (!landed || apex - startY < 5.6f || apex - startY > 7f)
                throw new AssertionError("Jump arc is outside the intended platform range: " + (apex - startY));
            movementWorld.getHeroController().keyUp(Input.Keys.SPACE);

            hero.getBody().setTransform(3f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            for (int i = 0; i < 5; i++) movementWorld.update(GameWorld.FIXED_STEP);
            movementWorld.getHeroController().keyDown(Input.Keys.RIGHT);
            movementWorld.update(GameWorld.FIXED_STEP);
            closeTo(hero.getBody().getLinearVelocity().x, Hero.WALK_SPEED,
                "ground movement did not start immediately");
            movementWorld.getHeroController().keyUp(Input.Keys.RIGHT);
            movementWorld.getHeroController().keyDown(Input.Keys.LEFT);
            movementWorld.update(GameWorld.FIXED_STEP);
            closeTo(hero.getBody().getLinearVelocity().x, -Hero.WALK_SPEED,
                "ground movement did not reverse immediately");
            movementWorld.getHeroController().keyUp(Input.Keys.LEFT);
            movementWorld.getHeroController().keyDown(Input.Keys.RIGHT);
            for (int i = 0; i < 10; i++) movementWorld.update(GameWorld.FIXED_STEP);
            movementWorld.getHeroController().keyUp(Input.Keys.RIGHT);
            float releasedX = hero.getPosition().x;
            for (int i = 0; i < 12; i++) movementWorld.update(GameWorld.FIXED_STEP);
            if (hero.getPosition().x - releasedX > 0.7f)
                throw new AssertionError("Hero drifted too far after releasing movement");
            movementWorld.getHeroController().keyDown(Input.Keys.RIGHT);
            movementWorld.getHeroController().keyDown(Input.Keys.SPACE);
            movementWorld.getHeroController().keyDown(Input.Keys.X);
            movementWorld.getHeroController().clearInput();
            movementWorld.update(GameWorld.FIXED_STEP);
            if (hero.getBody().getLinearVelocity().y > 0.5f ||
                hero.getCombatStateMachine().getCurrentState() instanceof
                    com.cursedarchie.platformer.actors.hero.states.combat.AttackState)
                throw new AssertionError("Cleared input still triggered a queued action");
            movementWorld.changeLocation("start", 0);
            if (hero.isGrounded() || hero.getFootContacts() != 0)
                throw new AssertionError("Location transition retained stale ground contacts");
        } finally {
            movementWorld.dispose();
        }

        GameWorld contactWorld = combatFixture();
        try {
            Enemy regular = null;
            for (Enemy enemy : contactWorld.getEnemies()) {
                if (enemy.getSize() < 2f) regular = enemy;
                else enemy.setAlive(false);
            }
            if (regular == null) throw new AssertionError("No enemy available for body contact check");
            regular.setActive(false);
            regular.getBody().setTransform(12f, 2.5f, 0f);
            regular.getBody().setLinearVelocity(0f, 0f);
            Hero hero = contactWorld.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            contactWorld.getHeroController().keyDown(Input.Keys.RIGHT);
            for (int i = 0; i < 90; i++) contactWorld.update(GameWorld.FIXED_STEP);
            contactWorld.getHeroController().keyUp(Input.Keys.RIGHT);
            if (hero.getPosition().x > 11.2f || Math.abs(regular.getPosition().x - 12f) > 0.1f)
                throw new AssertionError("Hero passed through or shoved an enemy: hero=" +
                    hero.getPosition().x + " enemy=" + regular.getPosition().x);
            hero.getBody().setTransform(8f, 2.5f, 0f);
            regular.setActive(true);
            regular.setBrain(null);
            regular.setIntent(Enemy.Intent.IDLE);
            for (int i = 0; i < 8; i++) contactWorld.update(GameWorld.FIXED_STEP);
            float restingX = regular.getPosition().x;
            for (int i = 0; i < 60; i++) contactWorld.update(GameWorld.FIXED_STEP);
            if (Math.abs(regular.getPosition().x - restingX) > 0.04f ||
                Math.abs(regular.getBody().getLinearVelocity().x) > 0.02f)
                throw new AssertionError("Idle enemy retained push velocity");
        } finally {
            contactWorld.dispose();
        }
    }

    private void checkJumpBuffer() {
        GameWorld bufferedWorld = combatFixture();
        try {
            bufferedWorld.changeLocation("forest", 0);
            Hero hero = bufferedWorld.getHero();
            hero.getBody().setTransform(4f, 3.2f, 0f);
            hero.getBody().setLinearVelocity(0f, -8f);
            bufferedWorld.getHeroController().keyDown(Input.Keys.SPACE);
            bufferedWorld.getHeroController().keyUp(Input.Keys.SPACE);
            boolean landed = false;
            boolean jumped = false;
            for (int i = 0; i < 12; i++) {
                bufferedWorld.update(GameWorld.FIXED_STEP);
                if (hero.isGrounded()) landed = true;
                if (hero.getBody().getLinearVelocity().y > 1f) {
                    if (!landed) throw new AssertionError("Buffered jump fired before landing");
                    jumped = true;
                    break;
                }
            }
            if (!jumped) throw new AssertionError("Tap shortly before landing was not buffered");
        } finally {
            bufferedWorld.dispose();
        }

        GameWorld expiredWorld = combatFixture();
        try {
            expiredWorld.changeLocation("forest", 0);
            Hero hero = expiredWorld.getHero();
            hero.getBody().setTransform(4f, 8f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            expiredWorld.getHeroController().keyDown(Input.Keys.SPACE);
            boolean landed = false;
            for (int i = 0; i < 90; i++) {
                expiredWorld.update(GameWorld.FIXED_STEP);
                if (hero.isGrounded()) landed = true;
                if (landed && hero.getBody().getLinearVelocity().y > 1f)
                    throw new AssertionError("Expired jump press triggered after landing");
            }
            if (!landed) throw new AssertionError("Expired-buffer test did not land");
        } finally {
            expiredWorld.dispose();
        }
    }

    private static Enemy headFoe(GameWorld w, float x, boolean ai) {
        Enemy e = EnemyFactory.createDefaultEnemy(w.getWorld(), new Vector2(x, 2.5f));
        e.setFacingLeft(!ai);
        if (ai) e.setBrain(new SimpleEnemyBrain(w));
        e.setActive(false);   // an AI enemy is switched on by the test once it has been knocked down
        w.addEnemy(e);
        if (!ai) e.getBody().setType(com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody);
        return e;
    }

    private static void placeHero(Hero hero, float x, float y, float vy) {
        hero.getBody().setTransform(x, y, 0f);
        hero.getBody().setLinearVelocity(0f, vy);
    }

    private static int headCollisions(GameWorld w, java.util.Set<Object> seen) {
        for (GameWorld.CombatEvent ev : w.getCombatEvents())
            if (ev.kind == GameWorld.CombatEvent.Kind.HEAD_COLLISION) seen.add(ev);
        return seen.size();
    }

    /**
     * Landing on a head: hero and a standard enemy lose their balance, fall side by side, recover and carry on. No
     * damage, no standing on heads, no repeat triggers, heavy enemies stay up.
     */
    /** The test room has a one-way platform over x 6..18, so the fall tests use the open floor to its right. */
    private static final float X0 = 24f;

    private void checkHeadLandingFall() {
        com.cursedarchie.platformer.combat.HeadCollisionConfig cfg =
            com.cursedarchie.platformer.combat.HeadCollisionConfig.get();
        float minGap = Hero.BODY_SIZE / 2f + .5f + .03f;
        java.util.Set<Object> collisions = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object, Boolean>());

        // ---- 1. A standard enemy and the hero fall together, recover, and can be landed on again later.
        GameWorld w = combatFixture();
        try {
            parkEnemies(w);
            Hero hero = w.getHero();
            Enemy foe = headFoe(w, X0, false);
            float foeHealth = foe.getHealth(), heroHealth = hero.getHealth();
            placeHero(hero, (X0 + .1f), 10.5f, 0f);
            int started = -1, heroDown = 0, foeDown = 0;
            for (int i = 0; i < 170; i++) {
                w.update(GameWorld.FIXED_STEP);
                headCollisions(w, collisions);
                if (hero.isFalling() && started < 0) {
                    started = i;
                    if (hero.getFallPhase() != Hero.FallPhase.FALL_COLLISION)
                        throw new AssertionError("A standard enemy must put the hero in FALL_COLLISION");
                    if (foe.getKnockdownPhase() != Enemy.KnockdownPhase.KNOCKDOWN_FROM_ABOVE)
                        throw new AssertionError("The enemy did not enter KNOCKDOWN_FROM_ABOVE with the hero");
                    if (hero.getPosition().x <= foe.getPosition().x)
                        throw new AssertionError("Hero landed right of the head but fell left");
                    if (foe.getPosition().x - X0 < cfg.enemyShift * .75f)
                        throw new AssertionError("The enemy did not shift the way the hero fell");
                }
                if (hero.isFalling()) {
                    heroDown++;
                    int sinceStart = i - started;
                    if (sinceStart == 3) w.getHeroController().keyDown(Input.Keys.B);
                    if (sinceStart == 5) { w.getHeroController().keyDown(Input.Keys.X); w.getHeroController().keyUp(Input.Keys.X); }
                    if (sinceStart == 6) { w.getHeroController().keyDown(Input.Keys.Z); w.getHeroController().keyUp(Input.Keys.Z); }
                    if (sinceStart == 7) { w.getHeroController().keyDown(Input.Keys.SPACE); w.getHeroController().keyUp(Input.Keys.SPACE); }
                    if (sinceStart == 8) { w.getHeroController().keyDown(Input.Keys.RIGHT); }
                    if (heroAttack(hero) != null) throw new AssertionError("Hero attacked while falling");
                    if (hero.isRolling()) throw new AssertionError("Hero rolled while falling");
                    if (hero.isBlocking()) throw new AssertionError("Hero blocked while falling");
                    if (Math.abs(hero.getBody().getLinearVelocity().x) > .01f)
                        throw new AssertionError("Hero kept horizontal speed while falling: " + hero.getBody().getLinearVelocity().x);
                    if (hero.getPosition().y - .5f < 2f - .06f)
                        throw new AssertionError("Hero sank into the floor during the fall");
                    if (hero.getPosition().y - .5f > 2f + .3f)
                        throw new AssertionError("Hero is still on top of the enemy during the fall");
                }
                if (foe.isKnockedDown()) {
                    foeDown++;
                    if (foe.isAttacking() || foe.isBlocking()) throw new AssertionError("Fallen enemy attacked or guarded");
                    if (foe.getPosition().y - .5f < 2f - .06f) throw new AssertionError("Enemy sank into the floor");
                }
                if (hero.isFalling() || foe.isKnockedDown()) {
                    float dx = Math.abs(hero.getPosition().x - foe.getPosition().x);
                    if (hero.isFalling() && foe.isKnockedDown() && dx < minGap - .01f)
                        throw new AssertionError("Fallen pair overlap or are closer than the minimum gap: " + dx);
                    if (dx > minGap + .35f && hero.isFalling() && foe.isKnockedDown())
                        throw new AssertionError("Fallen pair drifted apart: " + dx);
                }
                if (started >= 0 && i - started == 12) w.getHeroController().keyUp(Input.Keys.B);
                if (started >= 0 && i - started == 14) w.getHeroController().keyUp(Input.Keys.RIGHT);
            }
            if (started < 0) throw new AssertionError("Landing on a head from above did not start a fall");
            if (collisions.size() != 1) throw new AssertionError("Head collision fired " + collisions.size() + " times");
            float heroSteps = (cfg.heroFallDuration + cfg.heroFallRecovery) / GameWorld.FIXED_STEP;
            float foeSteps = (cfg.enemyKnockdownDuration + cfg.enemyKnockdownRecovery) / GameWorld.FIXED_STEP;
            if (Math.abs(heroDown - heroSteps) > 3f) throw new AssertionError("Hero was down " + heroDown + " steps, expected " + heroSteps);
            if (Math.abs(foeDown - foeSteps) > 3f) throw new AssertionError("Enemy was down " + foeDown + " steps, expected " + foeSteps);
            if (foeDown <= heroDown) throw new AssertionError("The enemy must stay down longer than the hero");
            if (hero.isFalling() || hero.getFallPartner() != null || foe.isKnockedDown())
                throw new AssertionError("Both must be back to normal after the recovery");
            if (heroAttack(hero) != null) throw new AssertionError("A button pressed during the fall was replayed afterwards");
            closeTo(hero.getHealth(), heroHealth, "The head landing damaged the hero");
            closeTo(foe.getHealth(), foeHealth, "The head landing damaged the enemy");
            if (hero.getInvulnerabilityRemaining() > 0f) throw new AssertionError("The head landing granted invulnerability");

            // Collisions are solid again: walking into the (immovable) enemy stops the hero at its side.
            w.getHeroController().keyUp(Input.Keys.RIGHT);
            placeHero(hero, (X0 - 1.5f), 2.5f, 0f);
            for (int i = 0; i < 20; i++) w.update(GameWorld.FIXED_STEP);
            w.getHeroController().keyDown(Input.Keys.RIGHT);
            for (int i = 0; i < 50; i++) w.update(GameWorld.FIXED_STEP);
            w.getHeroController().keyUp(Input.Keys.RIGHT);
            if (hero.getPosition().x >= foe.getPosition().x - .5f)
                throw new AssertionError("Hero walked through the enemy after recovery: collisions were not restored");

            // A new jump and a new landing triggers a new fall, this time to the other side.
            placeHero(hero, foe.getPosition().x - .1f, 10.5f, 0f);
            boolean second = false;
            for (int i = 0; i < 60 && !second; i++) {
                w.update(GameWorld.FIXED_STEP);
                headCollisions(w, collisions);
                second = hero.isFalling();
            }
            if (!second || collisions.size() != 2) throw new AssertionError("A new landing did not trigger a new fall");
            if (hero.getPosition().x >= foe.getPosition().x) throw new AssertionError("Hero landed left of the head but fell right");
        } finally { w.dispose(); }

        // ---- 2. No repeat while the enemy is still down: the hero is moved off, the fall does not restart.
        GameWorld again = combatFixture();
        try {
            parkEnemies(again);
            Hero hero = again.getHero();
            Enemy foe = headFoe(again, X0, false);
            collisions.clear();
            placeHero(hero, (X0 + .1f), 10.5f, 0f);
            boolean began = false, window = false;
            for (int i = 0; i < 200 && !window; i++) {
                again.update(GameWorld.FIXED_STEP);
                headCollisions(again, collisions);
                if (hero.isFalling()) began = true;
                window = began && !hero.isFalling() && foe.isKnockedDown();
            }
            if (!window)
                throw new AssertionError("Expected a window where the hero is up and the enemy is still down");
            placeHero(hero, foe.getPosition().x, 3.7f, -3f);
            boolean sawRecovery = false;
            for (int i = 0; i < 90; i++) {
                again.update(GameWorld.FIXED_STEP);
                headCollisions(again, collisions);
                if (foe.getKnockdownPhase() == Enemy.KnockdownPhase.KNOCKDOWN_RECOVERY) sawRecovery = true;
                if (sawRecovery && foe.getKnockdownPhase() == Enemy.KnockdownPhase.KNOCKDOWN_FROM_ABOVE)
                    throw new AssertionError("Landing on the same enemy again restarted its knockdown");
            }
            if (collisions.size() != 1) throw new AssertionError("A repeat landing during recovery fired a new collision");
            if (hero.getPosition().y - .5f > 2f + .3f) throw new AssertionError("Hero ended up standing on the fallen enemy");
        } finally { again.dispose(); }

        // ---- 3. Only a downward landing triggers; a rest on the head slides the hero off; the side never triggers.
        GameWorld rest = combatFixture();
        try {
            parkEnemies(rest);
            Hero hero = rest.getHero();
            Enemy foe = headFoe(rest, X0, false);
            placeHero(hero, X0, 3.45f, 6f);
            for (int i = 0; i < 4; i++) rest.update(GameWorld.FIXED_STEP);
            if (hero.isFalling() || foe.isKnockedDown()) throw new AssertionError("A rising hero triggered a head fall");
            placeHero(hero, X0, 3.5f, 0f);
            for (int i = 0; i < 3; i++) rest.update(GameWorld.FIXED_STEP);
            if (hero.isFalling() || foe.isKnockedDown()) throw new AssertionError("Touching a head without moving down triggered a fall");
            boolean slid = false;
            for (int i = 0; i < 60; i++) {
                rest.update(GameWorld.FIXED_STEP);
                if (hero.isFalling()) slid = true;
            }
            if (!slid) throw new AssertionError("The hero was allowed to stand on a head");
            if (foe.isKnockedDown()) throw new AssertionError("A mere rest on a head knocked the enemy down");
            if (hero.getPosition().y - .5f > 2f + .3f) throw new AssertionError("Hero is still standing on the enemy's head");
        } finally { rest.dispose(); }

        GameWorld flank = combatFixture();
        try {
            parkEnemies(flank);
            Hero hero = flank.getHero();
            Enemy foe = headFoe(flank, X0, false);
            placeHero(hero, (X0 - 2.5f), 2.5f, 0f);
            for (int i = 0; i < 20; i++) flank.update(GameWorld.FIXED_STEP);
            flank.getHeroController().keyDown(Input.Keys.RIGHT);
            for (int i = 0; i < 70; i++) {
                flank.update(GameWorld.FIXED_STEP);
                if (hero.isFalling() || foe.isKnockedDown()) throw new AssertionError("A side collision triggered a head fall");
            }
            flank.getHeroController().keyUp(Input.Keys.RIGHT);
        } finally { flank.dispose(); }

        // ---- 4. A heavy enemy does not fall and takes no damage; only the hero recovers, off to the side.
        GameWorld heavyWorld = combatFixture();
        try {
            parkEnemies(heavyWorld);
            Hero hero = heavyWorld.getHero();
            Enemy heavy = EnemyFactory.createHeavyHunter(heavyWorld.getWorld(), new Vector2(X0, 2.85f));
            heavy.setActive(false);
            heavyWorld.addEnemy(heavy);
            heavy.getBody().setType(com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody);
            if (heavy.canBeKnockedDownFromAbove()) throw new AssertionError("The heavy hunter must not be topplable");
            float heavyHealth = heavy.getHealth(), heroHealth = hero.getHealth();
            placeHero(hero, (X0 + .2f), 11f, 0f);
            boolean sawFall = false;
            for (int i = 0; i < 100; i++) {
                heavyWorld.update(GameWorld.FIXED_STEP);
                if (heavy.isKnockedDown()) throw new AssertionError("A heavy enemy was knocked down");
                if (hero.isFalling()) {
                    sawFall = true;
                    if (hero.getFallPhase() != Hero.FallPhase.FALL_RECOVERY || hero.getFallPartner() != null)
                        throw new AssertionError("Landing on a heavy enemy must only put the hero into FALL_RECOVERY");
                    if (hero.getPosition().x - heavy.getPosition().x < Hero.BODY_SIZE / 2f + .85f + .02f)
                        throw new AssertionError("Hero was left inside the heavy enemy");
                }
            }
            if (!sawFall) throw new AssertionError("Landing on a heavy enemy did nothing: the hero can stand on it");
            closeTo(heavy.getPosition().x, X0, "The heavy enemy was pushed");
            closeTo(heavy.getHealth(), heavyHealth, "The head landing damaged the heavy enemy");
            closeTo(hero.getHealth(), heroHealth, "The head landing damaged the hero");
            if (hero.getPosition().y - .5f > 2f + .3f) throw new AssertionError("Hero is standing on the heavy enemy");
        } finally { heavyWorld.dispose(); }

        // ---- 5. Walls: the pair is placed in open space, never into the wall.
        GameWorld walled = combatFixture();
        try {
            parkEnemies(walled);
            Hero hero = walled.getHero();
            addLevelWall(walled, (X0 + 2.2f), 3.5f, 1f, 2f);        // its left face is at x = 11.2
            Enemy foe = headFoe(walled, X0, false);
            placeHero(hero, (X0 + .1f), 10.5f, 0f);
            boolean fell = false;
            for (int i = 0; i < 80; i++) {
                walled.update(GameWorld.FIXED_STEP);
                if (hero.isFalling() && foe.isKnockedDown()) {
                    fell = true;
                    if (hero.getPosition().x + .5f > (X0 + 1.2f) + .02f || foe.getPosition().x + .5f > (X0 + 1.2f) + .02f)
                        throw new AssertionError("The fallen pair were pushed into the wall");
                    if (hero.getPosition().x >= foe.getPosition().x)
                        throw new AssertionError("The hero should have fallen to the open side");
                    if (Math.abs(hero.getPosition().x - foe.getPosition().x) < minGap - .01f)
                        throw new AssertionError("The pair overlap next to a wall");
                }
            }
            if (!fell) throw new AssertionError("No fall happened beside a wall");
        } finally { walled.dispose(); }

        GameWorld corridor = combatFixture();
        try {
            parkEnemies(corridor);
            Hero hero = corridor.getHero();
            addLevelWall(corridor, (X0 - 1.8f), 3.5f, 1f, 2f);       // right face at x = 9.2
            addLevelWall(corridor, (X0 + 2.2f), 3.5f, 1f, 2f);      // left face at x = 11.2
            Enemy foe = headFoe(corridor, (X0 + .2f), false);
            placeHero(hero, (X0 + .3f), 10.5f, 0f);
            for (int i = 0; i < 80; i++) {
                corridor.update(GameWorld.FIXED_STEP);
                if (hero.getPosition().x - .5f < (X0 - .8f) - .06f || hero.getPosition().x + .5f > (X0 + 1.2f) + .06f)
                    throw new AssertionError("Hero was pushed into a wall with no room for the pair");
                if (foe.isKnockedDown()) throw new AssertionError("The pair fell although there was no room");
            }
        } finally { corridor.dispose(); }

        // ---- 6. The enemy's AI is back after the recovery.
        GameWorld brainy = combatFixture();
        try {
            parkEnemies(brainy);
            Hero hero = brainy.getHero();
            Enemy foe = headFoe(brainy, X0, true);
            placeHero(hero, (X0 + .1f), 10.5f, 0f);
            boolean wasDown = false, resumed = false;
            for (int i = 0; i < 260; i++) {
                brainy.update(GameWorld.FIXED_STEP);
                if (foe.isKnockedDown()) {
                    if (!wasDown) foe.setActive(true);   // from now on its AI is running and must still stay quiet
                    wasDown = true;
                    if (foe.isAttacking() || foe.isBlocking() || foe.isDashPhase())
                        throw new AssertionError("A knocked-down enemy acted");
                    if (foe.getCurrentIntent() != Enemy.Intent.IDLE)
                        throw new AssertionError("A knocked-down enemy kept an AI intent: " + foe.getCurrentIntent());
                } else if (wasDown && (foe.isAttacking() || foe.getCurrentIntent() != Enemy.Intent.IDLE)) {
                    resumed = true;
                    break;
                }
            }
            if (!wasDown) throw new AssertionError("The AI enemy was never knocked down");
            if (!resumed) throw new AssertionError("The enemy's AI did not resume after the recovery");
        } finally { brainy.dispose(); }
    }


    /**
     * A jump onto a head (a short drop) only pushes the hero back mildly; only a fall of at least ~0.9 of the maximum
     * jump height topples anybody. Landings are caught even right at an enemy's edge at full fall speed.
     */
    private void checkHeadJumpOnPushback() {
        com.cursedarchie.platformer.combat.HeadCollisionConfig cfg =
            com.cursedarchie.platformer.combat.HeadCollisionConfig.get();
        float maxJump = Hero.JUMP_SPEED * Hero.JUMP_SPEED / (2f * GameWorld.GRAVITY);
        float landingCenter = 3.5f;   // a standard enemy's head (top at 3.0) plus half the hero

        // A plain jump (peak a full jump above the floor) onto a head: pushed away, nobody falls, nobody is hurt.
        for (float dx : new float[]{-.6f, .0f, .5f}) {
            GameWorld w = combatFixture();
            try {
                parkEnemies(w);
                Hero hero = w.getHero();
                Enemy foe = headFoe(w, X0, false);
                float heroHealth = hero.getHealth(), foeHealth = foe.getHealth();
                placeHero(hero, X0 + dx, 2.5f + maxJump * .98f, 0f);
                boolean pushed = false;
                for (int i = 0; i < 90; i++) {
                    w.update(GameWorld.FIXED_STEP);
                    if (hero.isFalling() || foe.isKnockedDown())
                        throw new AssertionError("A plain jump onto a head caused a fall (dx " + dx + ")");
                    if (hero.isPushedBack()) {
                        pushed = true;
                        if (Math.abs(hero.getBody().getLinearVelocity().x) > cfg.pushbackSpeed * 1.2f)
                            throw new AssertionError("The push-back is not mild: " + hero.getBody().getLinearVelocity().x);
                    }
                }
                if (!pushed) throw new AssertionError("A jump onto a head did not push the hero back (dx " + dx + ")");
                float away = dx < 0f ? foe.getPosition().x - hero.getPosition().x : hero.getPosition().x - foe.getPosition().x;
                if (away < .9f) throw new AssertionError("The hero was not pushed clear of the enemy (dx " + dx + ", away " + away + ")");
                if (hero.getPosition().y - .5f > 2.3f) throw new AssertionError("The hero is standing on the head after a jump onto it");
                closeTo(hero.getHealth(), heroHealth, "A jump onto a head hurt the hero");
                closeTo(foe.getHealth(), foeHealth, "A jump onto a head hurt the enemy");
            } finally { w.dispose(); }
        }

        // The threshold: just under it only pushes, just over it topples both.
        float threshold = maxJump * cfg.minFallHeightRatio;
        float[] drops = {threshold - .3f, threshold + .3f};
        for (int k = 0; k < 2; k++) {
            GameWorld w = combatFixture();
            try {
                parkEnemies(w);
                Hero hero = w.getHero();
                Enemy foe = headFoe(w, X0, false);
                placeHero(hero, (X0 + .1f), landingCenter + drops[k], 0f);
                boolean fell = false;
                for (int i = 0; i < 90; i++) {
                    w.update(GameWorld.FIXED_STEP);
                    if (hero.isFalling() && foe.isKnockedDown()) fell = true;
                }
                if (k == 0 && fell) throw new AssertionError("A drop just under the threshold toppled the enemy");
                if (k == 1 && !fell) throw new AssertionError("A drop just over the threshold did not topple the enemy");
            } finally { w.dispose(); }
        }

        // Landings at full fall speed are never missed, also when only a corner of the hero is over the head.
        for (float dx : new float[]{-.85f, -.6f, -.3f, .0f, .3f, .6f, .85f}) {
            GameWorld w = combatFixture();
            try {
                parkEnemies(w);
                Hero hero = w.getHero();
                Enemy foe = headFoe(w, X0, false);
                placeHero(hero, X0 + dx, 12f, 0f);
                boolean fell = false;
                for (int i = 0; i < 90; i++) {
                    w.update(GameWorld.FIXED_STEP);
                    if (hero.isFalling() && foe.isKnockedDown()) fell = true;
                    if (hero.isFalling() && hero.getPosition().y - .5f > 2.3f)
                        throw new AssertionError("The hero hovers above the enemy during the fall (dx " + dx + ")");
                }
                if (!fell) throw new AssertionError("A fast landing at dx " + dx + " did not start the fall");
            } finally { w.dispose(); }
        }

        // Never standing on a head, even in the recovery after being deflected from one.
        GameWorld heavyWorld = combatFixture();
        try {
            parkEnemies(heavyWorld);
            Hero hero = heavyWorld.getHero();
            Enemy heavy = EnemyFactory.createHeavyHunter(heavyWorld.getWorld(), new Vector2(X0, 2.85f));
            heavy.setActive(false);
            heavyWorld.addEnemy(heavy);
            heavy.getBody().setType(com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody);
            placeHero(hero, (X0 + .3f), 13f, 0f);
            for (int i = 0; i < 120; i++) {
                heavyWorld.update(GameWorld.FIXED_STEP);
                if (hero.isFalling() && hero.getPosition().y - .5f > 3.7f - .1f)
                    throw new AssertionError("The hero stood on the heavy enemy's head while recovering");
            }
        } finally { heavyWorld.dispose(); }
    }

    /** Light strikes 1 and 2 hit one opponent only, and keep hitting that one while it stays in the strike zone. */
    private void checkLightStrikeSingleTarget() {
        GameWorld w = combatFixture();
        try {
            parkEnemies(w);
            Hero hero = w.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            hero.setFacingLeft(false);
            Enemy near = staticFoe(w, 11.0f), far = staticFoe(w, 11.3f);
            near.setMaxHealth(500f); near.setHealth(500f);
            far.setMaxHealth(500f); far.setHealth(500f);
            for (int i = 0; i < 20; i++) w.update(GameWorld.FIXED_STEP);
            com.cursedarchie.platformer.actors.hero.states.combat.AttackState first =
                new com.cursedarchie.platformer.actors.hero.states.combat.AttackState(
                    com.cursedarchie.platformer.actors.hero.states.combat.AttackState.COMBO_FIRST);
            hero.getCombatStateMachine().changeState(first);
            for (int i = 0; i < 40; i++) w.update(GameWorld.FIXED_STEP);
            if (near.getHealth() >= 500f)
                throw new AssertionError("Light strike 1 did not hit the nearest enemy");
            if (far.getHealth() < 500f)
                throw new AssertionError("Light strike 1 dealt splash damage to a second enemy");
            float nearAfterFirst = near.getHealth();
            // The other enemy is now the nearer one; the second strike must stay on the locked opponent.
            near.getBody().setTransform(11.3f, 2.5f, 0f);
            far.getBody().setTransform(11.0f, 2.5f, 0f);
            for (int i = 0; i < 3; i++) w.update(GameWorld.FIXED_STEP);
            com.cursedarchie.platformer.actors.hero.states.combat.AttackState second =
                new com.cursedarchie.platformer.actors.hero.states.combat.AttackState(
                    com.cursedarchie.platformer.actors.hero.states.combat.AttackState.COMBO_SECOND);
            hero.getCombatStateMachine().changeState(second);
            for (int i = 0; i < 40; i++) w.update(GameWorld.FIXED_STEP);
            if (near.getHealth() >= nearAfterFirst)
                throw new AssertionError("Light strike 2 did not stay on the locked enemy");
            if (far.getHealth() < 500f)
                throw new AssertionError("Light strike 2 hit a different enemy than the locked one");
        } finally { w.dispose(); }
    }

    private void checkRenderRateConsistency() {
        float[] baseline = simulateOneSecondAt(60);
        for (int fps : new int[]{30, 120}) {
            float[] sampled = simulateOneSecondAt(fps);
            if (Math.abs(sampled[0] - baseline[0]) > 0.06f ||
                Math.abs(sampled[1] - baseline[1]) > 0.06f)
                throw new AssertionError("Hero moved differently at " + fps + " FPS");
        }
    }

    private float[] simulateOneSecondAt(int fps) {
        GameWorld sampledWorld = new GameWorld(42L);
        try {
            sampledWorld.changeLocation("forest", 0);
            Hero hero = sampledWorld.getHero();
            hero.getBody().setTransform(4f, 2.7f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            for (int i = 0; i < 20; i++) sampledWorld.update(GameWorld.FIXED_STEP);
            if (!hero.isGrounded()) throw new AssertionError("Render-rate sample did not settle");
            sampledWorld.getHeroController().keyDown(Input.Keys.RIGHT);
            sampledWorld.getHeroController().keyDown(Input.Keys.SPACE);
            for (int i = 0; i < fps; i++) sampledWorld.update(1f / fps);
            return new float[]{hero.getPosition().x, hero.getPosition().y};
        } finally {
            sampledWorld.dispose();
        }
    }

    private void checkHeroAttackTimingAndDirection() {
        GameWorld combatWorld = combatFixture();
        try {
            Enemy regular = null;
            Enemy boss = null;
            for (Enemy enemy : combatWorld.getEnemies()) {
                enemy.setActive(false);
                if (enemy.getSize() < 2f) regular = enemy;
                else boss = enemy;
            }
            if (regular == null || boss == null)
                throw new AssertionError("Combat fixtures need a regular enemy and boss");
            Hero hero = combatWorld.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            hero.setFacingLeft(false);
            regular.getBody().setTransform(11.2f, 2.5f, 0f);
            regular.getBody().setLinearVelocity(0f, 0f);
            boss.getBody().setTransform(8.6f, 3.5f, 0f);
            boss.getBody().setLinearVelocity(0f, 0f);
            combatWorld.update(GameWorld.FIXED_STEP);
            float regularHealth = regular.getHealth();
            float bossHealth = boss.getHealth();

            combatWorld.getHeroController().keyDown(Input.Keys.X);
            for (int i = 0; i < 4; i++) combatWorld.update(GameWorld.FIXED_STEP);
            if (regular.getHealth() != regularHealth)
                throw new AssertionError("Hero dealt damage before the shown contact frame");
            for (int i = 0; i < 3; i++) combatWorld.update(GameWorld.FIXED_STEP);
            if (regular.getHealth() != regularHealth - hero.getDamage() || boss.getHealth() != bossHealth)
                throw new AssertionError("Hero strike missed the front target or hit behind");
            GameWorld.DamageIndicator enemyHit = indicatorFor(combatWorld, regular);
            if (enemyHit == null || enemyHit.amount != hero.getDamage() ||
                indicatorFor(combatWorld, boss) != null || !combatWorld.hasRecentDamage(regular))
                throw new AssertionError("Damage cue did not match the hero's real hit");
            for (int i = 0; i < 35; i++) combatWorld.update(GameWorld.FIXED_STEP);
            if (regular.getHealth() != regularHealth - hero.getDamage())
                throw new AssertionError("Held attack damaged a target twice");
            combatWorld.getHeroController().keyUp(Input.Keys.X);
            int cuesBeforeLethalHit = combatWorld.getDamageIndicators().size;
            regular.takeDamage(regular.getHealth());
            combatWorld.update(GameWorld.FIXED_STEP);
            if (combatWorld.getDamageIndicators().size <= cuesBeforeLethalHit ||
                combatWorld.getEnemies().contains(regular, true))
                throw new AssertionError("Lethal enemy hit lost its damage cue during cleanup");
        } finally {
            combatWorld.dispose();
        }
    }

    private void checkUnifiedAttackModel() {
        Hero sampleHero = world.getHero();
        AttackDefinition first = sampleHero.attackDefinition(1);
        closeTo(first.startupDuration, .10f, "balanced first attack startup");
        closeTo(first.activeDuration, .09f, "balanced first attack active phase");
        closeTo(first.recoveryDuration, .20f, "balanced first attack recovery");
        closeTo(first.duration, .39f, "balanced first attack total duration");
        closeTo(com.cursedarchie.platformer.combat.AttackRhythmConfig.get().comboWindowLength, .15f,
            "combo window keeps its original absolute duration");
        closeTo(com.cursedarchie.platformer.combat.AttackRhythmConfig.get().highlightIntensity(100), 1f,
            "COMBO_READY highlight must not fade after successful combos");
        AttackDefinition second = sampleHero.attackDefinition(
            com.cursedarchie.platformer.actors.hero.states.combat.AttackState.COMBO_SECOND);
        AttackDefinition finisher = sampleHero.attackDefinition(
            com.cursedarchie.platformer.actors.hero.states.combat.AttackState.COMBO_FINISHER);
        closeTo(second.startupDuration, .07f, "balanced second attack startup");
        closeTo(second.activeDuration, .12f, "balanced second attack active phase");
        closeTo(second.recoveryDuration, .29f, "balanced second attack recovery");
        closeTo(second.duration, .48f, "balanced second attack total duration");
        closeTo(finisher.startupDuration, .10f, "balanced finisher startup");
        closeTo(finisher.activeDuration, .15f, "balanced finisher active phase");
        closeTo(finisher.recoveryDuration, .68f, "finisher mandatory recovery remains unchanged");
        closeTo(first.duration + second.duration + finisher.duration, 1.80f,
            "ideal three-hit combo duration");
        closeTo(first.hitboxWidth, .9f, "first attack hitbox width");
        closeTo(GameWorld.Arrow.WIDTH, .48f, "arrow width setting");
        AttackRuntime runtime = new AttackRuntime(first, false);
        if (runtime.direction() != 1 || runtime.phase() != AttackRuntime.Phase.STARTUP)
            throw new AssertionError("Attack runtime did not lock its initial direction");
        runtime.advance(first.startupDuration);
        if (runtime.phase() != AttackRuntime.Phase.ACTIVE)
            throw new AssertionError("Attack did not enter its active phase at startup end");
        runtime.advance(first.activeDuration);
        runtime.advance(.01f);
        if (runtime.phase() != AttackRuntime.Phase.RECOVERY || runtime.acceptsNextInput())
            throw new AssertionError("Combo window opened before its configured recovery time");
        runtime.advance(first.nextInputTime - runtime.elapsed());
        if (!runtime.acceptsNextInput())
            throw new AssertionError("Attack combo window did not open at nextInputTime");
        com.cursedarchie.platformer.actors.hero.states.combat.AttackState readyState =
            new com.cursedarchie.platformer.actors.hero.states.combat.AttackState(com.cursedarchie.platformer.actors.hero.states.combat.AttackState.COMBO_FIRST);
        readyState.enter(sampleHero);
        readyState.getRuntime().advance(readyState.getRuntime().comboWindowStart());
        if (readyState.getPhase(sampleHero) !=
            com.cursedarchie.platformer.actors.hero.states.combat.AttackState.Phase.COMBO_READY)
            throw new AssertionError("The first strike did not expose its COMBO_READY phase");
        readyState.exit(sampleHero);

        GameWorld fight = combatFixture();
        try {
            Enemy target = null;
            for (Enemy enemy : fight.getEnemies()) {
                enemy.setActive(false);
                if (enemy.getSize() < 2f) target = enemy;
            }
            Hero hero = fight.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            target.getBody().setTransform(11.2f, 2.5f, 0f);
            fight.update(GameWorld.FIXED_STEP);
            float health = target.getHealth();
            fight.getHeroController().keyDown(Input.Keys.X);
            for (int i = 0; i < 4; i++) fight.update(GameWorld.FIXED_STEP);
            target.getBody().setTransform(15f, 2.5f, 0f);
            for (int i = 0; i < 18; i++) fight.update(GameWorld.FIXED_STEP);
            closeTo(target.getHealth(), health, "target that left before active phase was hit");
            fight.getHeroController().keyUp(Input.Keys.X);

            for (int i = 0; i < 20; i++) fight.update(GameWorld.FIXED_STEP);
            hero.setFacingLeft(false);
            target.getBody().setTransform(hero.getPosition().x + 1.2f, hero.getPosition().y, 0f);
            target.getBody().setLinearVelocity(0f, 0f);
            fight.getHeroController().keyDown(Input.Keys.X);
            for (int i = 0; i < 12; i++) fight.update(GameWorld.FIXED_STEP);
            if (fight.getCombatEvents().size == 0)
                throw new AssertionError("Successful hit did not create a gameplay combat event");
            CombatHit hit = fight.getCombatEvents().first().hit;
            if (hit.source != hero || hit.target != target || hit.direction != 1f ||
                hit.damage <= 0f || hit.recoil <= 0f || hit.staggerDuration <= 0f || !hit.blockable)
                throw new AssertionError("Combat event omitted source, target, force, stagger, or blockability");
            fight.getHeroController().keyUp(Input.Keys.X);
        } finally { fight.dispose(); }
    }

    private void checkEnemyAttackAndDamageCooldown() {
        GameWorld combatWorld = combatFixture();
        try {
            Enemy attacker = null;
            for (Enemy enemy : combatWorld.getEnemies()) {
                if (enemy.getSize() < 2f) attacker = enemy;
                else enemy.setAlive(false);
            }
            if (attacker == null) throw new AssertionError("No regular enemy to check attack timing");
            attacker.setBrain(null);
            attacker.setIntent(Enemy.Intent.IDLE);
            attacker.setFacingLeft(true);
            attacker.getBody().setTransform(11.2f, 2.5f, 0f);
            attacker.getBody().setLinearVelocity(0f, 0f);
            Hero hero = combatWorld.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            combatWorld.update(GameWorld.FIXED_STEP);
            if (!attacker.getAttackTargets().contains(hero))
                throw new AssertionError("Enemy attack sensor did not see the hero");
            float health = hero.getHealth();
            attacker.getCombatStateMachine().changeState(new AttackState());
            for (int i = 0; i < 12; i++) combatWorld.update(GameWorld.FIXED_STEP);
            if (hero.getHealth() != health)
                throw new AssertionError("Enemy hit before its visible windup completed");
            for (int i = 0; i < 6; i++) combatWorld.update(GameWorld.FIXED_STEP);
            if (hero.getHealth() != health - attacker.getDamage())
                throw new AssertionError("Enemy attack missed its active window");
            GameWorld.DamageIndicator heroHit = indicatorFor(combatWorld, hero);
            if (heroHit == null || heroHit.amount != attacker.getDamage() ||
                !combatWorld.hasRecentDamage(hero))
                throw new AssertionError("Hero damage cue did not match the enemy's real hit");
            int indicatorCount = combatWorld.getDamageIndicators().size;
            attacker.getCombatStateMachine().changeState(new AttackState());
            for (int i = 0; i < 18; i++) combatWorld.update(GameWorld.FIXED_STEP);
            if (hero.getHealth() != health - attacker.getDamage())
                throw new AssertionError("Hero took a second hit during the damage cooldown");
            if (combatWorld.getDamageIndicators().size != indicatorCount)
                throw new AssertionError("Invulnerable hero displayed a false damage cue");
            attacker.setActive(false);
            for (int i = 0; i < 45; i++) combatWorld.update(GameWorld.FIXED_STEP);
            if (combatWorld.hasRecentDamage(hero) || combatWorld.getDamageIndicators().size != 0)
                throw new AssertionError("Damage cue did not fade and expire");
        } finally {
            combatWorld.dispose();
        }
    }

    private void checkCombatExtensions() {
        GameWorld fight = combatFixture();
        try {
            Enemy target = null;
            for (Enemy enemy : fight.getEnemies()) {
                if (enemy.getSize() < 2f) target = enemy;
                else enemy.setAlive(false);
            }
            if (target == null) throw new AssertionError("Missing combo target");
            target.setActive(false);
            target.setMaxHealth(10f);
            target.setHealth(10f);
            Hero hero = fight.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            target.getBody().setTransform(11.2f, 2.5f, 0f);
            fight.update(GameWorld.FIXED_STEP);
            fight.getHeroController().keyDown(Input.Keys.X);
            for (int i = 0; i < 10; i++) fight.update(GameWorld.FIXED_STEP);
            closeTo(target.getHealth(), 9f, "first strike damage");
            fight.getHeroController().keyUp(Input.Keys.X);
            // The follow-up is accepted only inside strike one's configured recovery window.
            for (int i = 0; i < 11; i++) fight.update(GameWorld.FIXED_STEP);
            fight.getHeroController().keyDown(Input.Keys.X);
            target.getBody().setTransform(hero.getPosition().x + 1.55f, hero.getPosition().y, 0f);
            target.getBody().setLinearVelocity(0f, 0f);
            for (int i = 0; i < 24; i++) fight.update(GameWorld.FIXED_STEP);
            if (target.getHealth() >= 9f)
                throw new AssertionError("Recovery-queued second combo strike did not land at its extended range");
            fight.getHeroController().keyUp(Input.Keys.X);

            fight.getInventory().add(ItemCatalog.BLADE_SHARD);
            fight.getInventory().add(ItemCatalog.IMPACT_CORE);
            if (!fight.combineItems(inventorySlot(fight, ItemCatalog.BLADE_SHARD),
                inventorySlot(fight, ItemCatalog.IMPACT_CORE)))
                throw new AssertionError("Relic fusion failed");
            closeTo(hero.meleeDamageMultiplier(com.cursedarchie.platformer.actors.hero.states.combat.AttackState.COMBO_FINISHER), 1.55f,
                "two relics did not synergize on the finisher");
        } finally {
            fight.dispose();
        }
    }

    private void checkSpecialEnemyRoles() {
        GameWorld fight = new GameWorld();
        try {
            Hero hero = fight.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            Enemy ranged = EnemyFactory.createScavenger(fight.getWorld(), new Vector2(15f, 2.5f));
            ranged.setBrain(new SimpleEnemyBrain(fight));
            fight.addEnemy(ranged);
            for (int i = 0; i < 70 && !ranged.isRangedWindingUp(); i++)
                fight.update(GameWorld.FIXED_STEP);
            if (!ranged.isRangedWindingUp())
                throw new AssertionError("Scavenger did not telegraph a distant shot");
            float health = hero.getHealth();
            for (int i = 0; i < 75; i++) fight.update(GameWorld.FIXED_STEP);
            if (hero.getHealth() >= health)
                throw new AssertionError("Telegraphed ranged shot did not deal damage");
            hero.getBody().setTransform(ranged.getPosition().x - 2f, 2.5f, 0f);
            fight.update(GameWorld.FIXED_STEP);
            if (ranged.getCurrentIntent() != Enemy.Intent.RETREAT)
                throw new AssertionError("Scavenger did not retreat when approached");

            Enemy support = EnemyFactory.createRedCollector(fight.getWorld(), new Vector2(16f, 2.5f));
            support.setBrain(new SimpleEnemyBrain(fight));
            ranged.setBrain(null);
            ranged.setIntent(Enemy.Intent.IDLE);
            ranged.getBody().setTransform(15f, 2.5f, 0f);
            ranged.getBody().setLinearVelocity(0f, 0f);
            ranged.setHealth(1f);
            fight.addEnemy(support);
            fight.update(GameWorld.FIXED_STEP);
            if (ranged.getHealth() <= 1f || support.getSupportFlashRemaining() <= 0f)
                throw new AssertionError("Collector did not heal a wounded nearby ally");
            support.getBody().setTransform(hero.getPosition().x + 1.5f, 2.5f, 0f);
            support.setFacingLeft(true);
            support.setCanSeeHero(true);
            for (int i = 0; i < 9; i++)
                support.updateSpecialActions(hero, fight.getAliveEnemies(), .1f);
            if (!support.isBlocking()) throw new AssertionError("Collector did not raise its guard");
            float guardHealth = support.getHealth();
            support.takeMeleeHit(2f, hero.getPosition().x, 0f, false);
            closeTo(support.getHealth(), guardHealth - 1f, "raised guard did not reduce frontal light hit");
            if (support.isBlocking()) throw new AssertionError("Guard did not react to a light hit");
            Enemy bypass = EnemyFactory.createRedCollector(fight.getWorld(),
                new Vector2(hero.getPosition().x + 2f, 2.5f));
            bypass.setFacingLeft(true);
            bypass.setCanSeeHero(true);
            fight.addEnemy(bypass);
            for (int i = 0; i < 9; i++)
                bypass.updateSpecialActions(hero, fight.getAliveEnemies(), .1f);
            if (!bypass.isBlocking()) throw new AssertionError("Second collector did not raise its guard");
            bypass.takeMeleeHit(2f, hero.getPosition().x, 0f, true);
            closeTo(bypass.getHealth(), bypass.getMaxHealth() - 2f,
                "Heavy strike did not break frontal guard");
            Enemy flank = EnemyFactory.createRedCollector(fight.getWorld(),
                new Vector2(hero.getPosition().x + 2.3f, 2.5f));
            flank.setFacingLeft(true);
            flank.setCanSeeHero(true);
            fight.addEnemy(flank);
            for (int i = 0; i < 9; i++)
                flank.updateSpecialActions(hero, fight.getAliveEnemies(), .1f);
            if (!flank.isBlocking()) throw new AssertionError("Flank target did not raise its guard");
            flank.takeMeleeHit(2f, flank.getPosition().x + 1f, 0f, false);
            closeTo(flank.getHealth(), flank.getMaxHealth() - 2f,
                "Rear light strike did not bypass guard");
        } finally {
            fight.dispose();
        }
    }

    private void checkRollCounterAndJumpAttack() {
        GameWorld fight = new GameWorld();
        try {
            Hero hero = fight.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            for (int i = 0; i < 60; i++) fight.update(GameWorld.FIXED_STEP);   // settle on the ground
            // Roll: brief invulnerability, then vulnerable again; no cooldown.
            if (!hero.tryRoll(1f)) throw new AssertionError("Grounded idle hero could not roll");
            float hp = hero.getHealth();
            hero.receiveHit(new CombatHit(CombatHit.Type.MELEE_LIGHT, 1f, 9f, 1f, 0f, null));
            closeTo(hero.getHealth(), hp, "roll i-frames did not avoid the hit");
            hero.update(Hero.ROLL_INVULNERABLE + .02f);
            if (hero.isRollInvulnerable()) throw new AssertionError("Roll invulnerability lasted too long");
            hero.update(Hero.ROLL_DURATION);
            if (!hero.tryRoll(1f)) throw new AssertionError("A roll has no cooldown: it must be repeatable once it ends");
            hero.update(Hero.ROLL_DURATION);
            // Perfect block: a hit right after raising the guard costs nothing and opens a counter window.
            hero.setFacingLeft(false);
            hero.getBody().setTransform(10f, 2.5f, 0f);
            for (int i = 0; i < 30; i++) fight.update(GameWorld.FIXED_STEP);
            hero.setBlockHeld(true);
            hp = hero.getHealth();
            float stability = hero.getBlockStability();
            hero.receiveHit(new CombatHit(CombatHit.Type.MELEE_HEAVY, 1f, 11f, 2f, 0f, null));
            closeTo(hero.getHealth(), hp, "perfect block must negate even a heavy hit");
            closeTo(hero.getBlockStability(), stability, "perfect block must not cost stability");
            if (!hero.isCounterReady()) throw new AssertionError("Perfect block did not open a counter window");
            hero.update(Hero.COUNTER_WINDOW + .05f);
            if (hero.isCounterReady()) throw new AssertionError("Counter window never closed");
            hero.setBlockHeld(false);
            // Counter and jump-attack definitions: both are punishable when they miss.
            int counter = com.cursedarchie.platformer.actors.hero.states.combat.AttackState.COUNTER_ATTACK;
            int jumpAttack = com.cursedarchie.platformer.actors.hero.states.combat.AttackState.JUMP_ATTACK;
            if (hero.attackDefinition(counter).missRecovery <= 0f)
                throw new AssertionError("Counter has no miss penalty");
            if (hero.attackDefinition(jumpAttack).missRecovery < .4f)
                throw new AssertionError("Jump attack landing recovery too short");
            if (hero.attackDefinition(counter).staggerDuration <= hero.attackDefinition(1).staggerDuration)
                throw new AssertionError("Counter must stagger more than a light strike");
        } finally {
            fight.dispose();
        }
    }

    /** Every melee archetype must stop where its real hitbox reaches the hero, whatever its body size. */
    private void checkMeleeReachMatchesHitbox() {
        GameWorld fight = new GameWorld();
        try {
            Hero hero = fight.getHero();
            String[] types = {"default", "scavenger", "infiltrator", "overseer", "red_collector", "heavy_hunter"};
            for (String type : types) {
                Enemy e;
                switch (type) {
                    case "scavenger": e = EnemyFactory.createScavenger(fight.getWorld(), new Vector2(20f, 2.5f)); break;
                    case "infiltrator": e = EnemyFactory.createInfiltrator(fight.getWorld(), new Vector2(20f, 2.5f)); break;
                    case "overseer": e = EnemyFactory.createOverseer(fight.getWorld(), new Vector2(20f, 2.5f)); break;
                    case "red_collector": e = EnemyFactory.createRedCollector(fight.getWorld(), new Vector2(20f, 2.5f)); break;
                    case "heavy_hunter": e = EnemyFactory.createHeavyHunter(fight.getWorld(), new Vector2(20f, 2.5f)); break;
                    default: e = EnemyFactory.createDefaultEnemy(fight.getWorld(), new Vector2(20f, 2.5f));
                }
                e.setFacingLeft(false);
                float approach = e.getAttackApproachDistance(hero);
                if (approach <= 0.3f) throw new AssertionError(type + ": approach distance is not positive");
                com.badlogic.gdx.math.Rectangle box = new com.badlogic.gdx.math.Rectangle();
                com.badlogic.gdx.math.Rectangle hurt = new com.badlogic.gdx.math.Rectangle();
                // Just inside the approach distance: reach says yes and the real strike geometry agrees.
                hero.getBody().setTransform(20f + approach - .06f, 2.5f, 0f);
                if (!e.isTargetInStandardReach(hero))
                    throw new AssertionError(type + ": hero inside approach distance is out of reach");
                com.cursedarchie.platformer.combat.AttackRuntime strike =
                    new com.cursedarchie.platformer.combat.AttackRuntime(e.attackDefinition(false, false), false);
                if (!strike.canHit(e, hero, box, hurt))
                    throw new AssertionError(type + ": reach check and strike hitbox disagree (would whiff)");
                // Clearly outside: no attack may start; the enemy has to step closer first.
                hero.getBody().setTransform(20f + approach + .3f, 2.5f, 0f);
                if (e.isTargetInStandardReach(hero))
                    throw new AssertionError(type + ": enemy would attack from outside its reach");
                // Facing away: no attack even when the hero is in front of the weapon's back.
                hero.getBody().setTransform(20f + approach - .06f, 2.5f, 0f);
                e.setFacingLeft(true);
                if (e.isTargetInStandardReach(hero))
                    throw new AssertionError(type + ": enemy reaches the hero behind it");
            }
        } finally {
            fight.dispose();
        }
    }

    private void checkRapidTurnAndAwareness() {
        GameWorld fight = new GameWorld();
        try {
            Hero hero = fight.getHero();
            hero.getBody().setTransform(18.5f, 2.5f, 0f);
            Enemy foe = EnemyFactory.createDefaultEnemy(fight.getWorld(), new Vector2(20f, 2.5f));
            foe.setBrain(new SimpleEnemyBrain(fight));
            foe.setFacingLeft(false);   // looking away from the hero standing on its left
            fight.addEnemy(foe);
            boolean turned = false;
            boolean attackedWithoutStartup = false;
            for (int i = 0; i < 30 && !turned; i++) {
                fight.update(GameWorld.FIXED_STEP);
                if (foe.isFacingLeft()) turned = true;
            }
            if (!turned) throw new AssertionError("Enemy did not turn to the hero at once");
            if (foe.isAttacking() && foe.getCombatStateTime() <= 0f) attackedWithoutStartup = true;
            if (attackedWithoutStartup) throw new AssertionError("Enemy attacked on the frame it turned");
            if (foe.getAwareness() == Enemy.Awareness.UNAWARE)
                throw new AssertionError("Enemy never registered the hero");
            // Hero jumps over: the enemy swings round to the last known position without a patrol-style turn.
            hero.getBody().setTransform(21.5f, 2.5f, 0f);
            boolean flipped = false;
            for (int i = 0; i < 30 && !flipped; i++) {
                fight.update(GameWorld.FIXED_STEP);
                if (!foe.isFacingLeft()) flipped = true;
            }
            if (!flipped) throw new AssertionError("Enemy kept facing away after the hero crossed over it");
        } finally {
            fight.dispose();
        }
    }

    private void checkArcherMemoryAndMirroredTurn() {
        GameWorld w = new GameWorld();
        try {
            Hero hero = w.getHero();
            hero.getBody().setTransform(14f, 2.5f, 0f);
            Enemy archer = EnemyFactory.createScavenger(w.getWorld(), new Vector2(18f, 2.5f));
            archer.setBrain(new SimpleEnemyBrain(w));
            archer.setFacingLeft(true);
            w.addEnemy(archer);
            for (int i = 0; i < 20; i++) w.update(GameWorld.FIXED_STEP);
            if (archer.getAwareness() == Enemy.Awareness.UNAWARE) throw new AssertionError("Archer never noticed the hero");
            // Hero vanishes far behind the archer: it must still remember for a good while.
            hero.getBody().setTransform(40f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            for (int i = 0; i < 150; i++) w.update(GameWorld.FIXED_STEP);
            if (archer.getAwareness() == Enemy.Awareness.UNAWARE)
                throw new AssertionError("Archer forgot the hero within 2.5 s of losing sight");
            for (int i = 0; i < 360; i++) w.update(GameWorld.FIXED_STEP);
            if (archer.getAwareness() != Enemy.Awareness.UNAWARE)
                throw new AssertionError("Archer never returned to patrol after the search timer");
        } finally { w.dispose(); }
        // Mirrored jump-over: hero starts on the enemy's right, ends on its left.
        GameWorld m = new GameWorld();
        try {
            Hero hero = m.getHero();
            hero.getBody().setTransform(21.5f, 2.5f, 0f);
            Enemy foe = EnemyFactory.createDefaultEnemy(m.getWorld(), new Vector2(20f, 2.5f));
            foe.setBrain(new SimpleEnemyBrain(m));
            foe.setFacingLeft(true);
            m.addEnemy(foe);
            boolean turned = false;
            for (int i = 0; i < 30 && !turned; i++) { m.update(GameWorld.FIXED_STEP); turned = !foe.isFacingLeft(); }
            if (!turned) throw new AssertionError("Enemy did not turn right toward the hero");
            hero.getBody().setTransform(18.5f, 2.5f, 0f);
            boolean flipped = false;
            for (int i = 0; i < 30 && !flipped; i++) { m.update(GameWorld.FIXED_STEP); flipped = foe.isFacingLeft(); }
            if (!flipped) throw new AssertionError("Enemy kept facing away after a right-to-left jump-over");
        } finally { m.dispose(); }
    }

    private void checkArrowCeilingAndSightLoss() {
        GameWorld w = new GameWorld();
        try {
            Hero hero = w.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            Enemy shooter = EnemyFactory.createScavenger(w.getWorld(), new Vector2(15f, 2.5f));
            shooter.setActive(false);
            com.badlogic.gdx.physics.box2d.BodyDef def = new com.badlogic.gdx.physics.box2d.BodyDef();
            def.type = com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody;
            def.position.set(13f, 4.2f);
            com.badlogic.gdx.physics.box2d.Body ceiling = w.getWorld().createBody(def);
            ceiling.setUserData("level");
            com.badlogic.gdx.physics.box2d.PolygonShape box = new com.badlogic.gdx.physics.box2d.PolygonShape();
            box.setAsBox(3f, .05f);
            ceiling.createFixture(box, 0f);
            box.dispose();
            w.fireArrows(shooter, new Vector2(-.4f, .9f), RangedAttackConfig.NORMAL);
            float maxY = 0f;
            for (int i = 0; i < 90; i++) {
                for (GameWorld.Arrow a : w.getArrows()) maxY = Math.max(maxY, a.position.y);
                w.update(GameWorld.FIXED_STEP);
            }
            if (maxY > 4.4f) throw new AssertionError("Arrow passed through the ceiling: " + maxY);
            if (w.getArrows().size != 0) throw new AssertionError("Arrow was not removed after hitting the ceiling");
        } finally { w.dispose(); }
        // An archer already drawing its bow finishes the shot even if the hero briefly leaves view.
        GameWorld v = new GameWorld();
        try {
            Hero hero = v.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            Enemy archer = EnemyFactory.createScavenger(v.getWorld(), new Vector2(15f, 2.5f));
            archer.setBrain(new SimpleEnemyBrain(v));
            archer.setFacingLeft(true);
            v.addEnemy(archer);
            boolean drawing = false;
            for (int i = 0; i < 240 && !drawing; i++) {
                v.update(GameWorld.FIXED_STEP);
                drawing = archer.isRangedBusy();
            }
            if (!drawing) throw new AssertionError("Archer never began drawing at a clear target");
            // The hero slips out of view mid-draw: the shot must still be released.
            hero.getBody().setTransform(40f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            boolean fired = false;
            for (int i = 0; i < 120 && !fired; i++) {
                v.update(GameWorld.FIXED_STEP);
                fired = v.getArrows().size > 0;
            }
            if (!fired) throw new AssertionError("Archer dropped its shot as soon as sight was lost");
        } finally { v.dispose(); }
    }

    private static com.cursedarchie.platformer.actors.hero.states.combat.AttackState heroAttack(Hero hero) {
        Object state = hero.getCombatStateMachine().getCurrentState();
        return state instanceof com.cursedarchie.platformer.actors.hero.states.combat.AttackState ?
            (com.cursedarchie.platformer.actors.hero.states.combat.AttackState) state : null;
    }

    /** Presses X at {@code pressAt} seconds into a strike, releases at once, and returns the ids of every strike that started. */
    private java.util.List<String> rhythmRun(float pressAt, int totalSteps, int extraTaps, float[] hurriedFlags) {
        GameWorld w = combatFixture();
        try {
            for (Enemy e : w.getEnemies()) { e.setActive(false); e.getBody().setTransform(30f, 2.5f, 0f); }
            Hero hero = w.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            hero.setFacingLeft(false);
            w.update(GameWorld.FIXED_STEP);
            java.util.List<String> started = new java.util.ArrayList<String>();
            Object last = null;
            w.getHeroController().keyDown(Input.Keys.X);
            w.getHeroController().keyUp(Input.Keys.X);
            int pressStep = Math.round(pressAt / GameWorld.FIXED_STEP);
            for (int i = 1; i <= totalSteps; i++) {
                if (i == pressStep) {
                    for (int tap = 0; tap <= extraTaps; tap++) {
                        w.getHeroController().keyDown(Input.Keys.X);
                        w.getHeroController().keyUp(Input.Keys.X);
                    }
                }
                w.update(GameWorld.FIXED_STEP);
                com.cursedarchie.platformer.actors.hero.states.combat.AttackState a = heroAttack(hero);
                if (a != null && a != last) { started.add(a.getDefinition().id); last = a; }
                if (a != null && a.getRuntime().isHurried() && hurriedFlags != null) hurriedFlags[0] = 1f;
            }
            return started;
        } finally { w.dispose(); }
    }

    /** Drives both recovery presses relative to the strike that owns each combo window. */
    private java.util.List<String> threeHitRhythmRun(float secondAt, float finisherAt, int totalSteps) {
        GameWorld w = combatFixture();
        try {
            for (Enemy e : w.getEnemies()) { e.setActive(false); e.getBody().setTransform(30f, 2.5f, 0f); }
            Hero hero = w.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            w.update(GameWorld.FIXED_STEP);
            java.util.List<String> started = new java.util.ArrayList<String>();
            Object last = null;
            boolean secondPressed = false, finisherPressed = false;
            w.getHeroController().keyDown(Input.Keys.X);
            w.getHeroController().keyUp(Input.Keys.X);
            for (int i = 0; i < totalSteps; i++) {
                com.cursedarchie.platformer.actors.hero.states.combat.AttackState current = heroAttack(hero);
                if (current != null && current.getComboStep() ==
                    com.cursedarchie.platformer.actors.hero.states.combat.AttackState.COMBO_FIRST &&
                    !secondPressed && current.getRuntime().elapsed() >= secondAt) {
                    w.getHeroController().keyDown(Input.Keys.X);
                    w.getHeroController().keyUp(Input.Keys.X);
                    secondPressed = true;
                } else if (current != null && current.getComboStep() ==
                    com.cursedarchie.platformer.actors.hero.states.combat.AttackState.COMBO_SECOND &&
                    !finisherPressed && current.getRuntime().elapsed() >= finisherAt) {
                    w.getHeroController().keyDown(Input.Keys.X);
                    w.getHeroController().keyUp(Input.Keys.X);
                    finisherPressed = true;
                }
                w.update(GameWorld.FIXED_STEP);
                com.cursedarchie.platformer.actors.hero.states.combat.AttackState after = heroAttack(hero);
                if (after != null && after != last) { started.add(after.getDefinition().id); last = after; }
            }
            return started;
        } finally { w.dispose(); }
    }

    private void checkAttackRhythm() {
        // Defaults load from the config file shape.
        com.cursedarchie.platformer.combat.AttackRhythmConfig parsed =
            com.cursedarchie.platformer.combat.AttackRhythmConfig.parse("{\"hurriedDamageMultiplier\":0.7}");
        closeTo(parsed.hurriedDamageMultiplier, .7f, "config value was not read");
        closeTo(parsed.hurriedRecoveryMultiplier, 1.3f, "missing config keys must keep their defaults");

        // A single press is a full, normal strike and never hurried.
        float[] hurried = new float[1];
        java.util.List<String> single = rhythmRun(99f, 60, 0, hurried);
        if (single.size() != 1 || !"hero_light_1".equals(single.get(0)) || hurried[0] != 0f)
            throw new AssertionError("A single press must be one normal first strike: " + single);

        // Pressing inside the combo window continues into the second strike.
        java.util.List<String> calm = rhythmRun(.27f, 60, 0, hurried);
        if (calm.size() < 2 || !"hero_light_2".equals(calm.get(1)) || hurried[0] != 0f)
            throw new AssertionError("A well-timed press did not chain into strike two: " + calm);
        java.util.List<String> windowOpening = rhythmRun(.25f, 60, 0, hurried);
        if (windowOpening.size() < 2 || !"hero_light_2".equals(windowOpening.get(1)))
            throw new AssertionError("A press at COMBO_READY expired before strike two: " + windowOpening);

        // Strike two owns a real input window and a correctly timed third press starts the finisher.
        AttackDefinition second = world.getHero().attackDefinition(
            com.cursedarchie.platformer.actors.hero.states.combat.AttackState.COMBO_SECOND);
        AttackRuntime secondRuntime = new AttackRuntime(second, false);
        closeTo(secondRuntime.comboWindowEnd() - secondRuntime.comboWindowStart(), .20f,
            "strike two finisher input window");
        java.util.List<String> fullCombo = threeHitRhythmRun(.24f, .32f, 110);
        if (fullCombo.size() != 3 || !"hero_light_1".equals(fullCombo.get(0)) ||
            !"hero_light_2".equals(fullCombo.get(1)) || !"hero_light_3".equals(fullCombo.get(2)))
            throw new AssertionError("A timed three-hit combo did not reach the finisher: " + fullCombo);

        // Strike two deliberately has an early-input penalty: the finisher must be timed.
        java.util.List<String> earlyFinisher = threeHitRhythmRun(.24f, .15f, 110);
        if (earlyFinisher.contains("hero_light_3") || earlyFinisher.size() != 2)
            throw new AssertionError("An early third press incorrectly reached the finisher: " + earlyFinisher);

        // A press close to the end of strike two still means finisher, not a new first strike.
        java.util.List<String> lateFinisher = threeHitRhythmRun(.24f, .38f, 130);
        if (lateFinisher.size() != 3 || !"hero_light_3".equals(lateFinisher.get(2)))
            throw new AssertionError("A late-but-valid third press restarted the combo: " + lateFinisher);
        java.util.List<String> tooLateFinisher = threeHitRhythmRun(.24f, .47f, 130);
        if (tooLateFinisher.contains("hero_light_3"))
            throw new AssertionError("A press after strike two's window started the finisher: " + tooLateFinisher);

        // An early follow-up is consumed, does not weaken strike one, and cannot start strike two later.
        hurried[0] = 0f;
        java.util.List<String> early = rhythmRun(.1f, 60, 0, hurried);
        if (hurried[0] != 0f || early.size() != 1)
            throw new AssertionError("An early second-strike press was buffered or weakened strike one: " + early);

        // Repeated events after the first classified press cannot rescue or restart the combo.
        java.util.List<String> spam = rhythmRun(.1f, 90, 6, hurried);
        if (spam.size() != 1)
            throw new AssertionError("Spam rescued an early input or produced another strike: " + spam);

        // EARLY, WINDOW and LATE are mutually exclusive and only WINDOW stores a follow-up.
        com.cursedarchie.platformer.combat.AttackRhythmConfig cfg =
            com.cursedarchie.platformer.combat.AttackRhythmConfig.get();
        GameWorld w = combatFixture();
        try {
            Hero hero = w.getHero();
            com.cursedarchie.platformer.combat.AttackDefinition def = hero.attackDefinition(
                com.cursedarchie.platformer.actors.hero.states.combat.AttackState.COMBO_SECOND);
            com.cursedarchie.platformer.combat.AttackRuntime earlyRuntime =
                new com.cursedarchie.platformer.combat.AttackRuntime(def, false);
            earlyRuntime.advance(.1f);
            if (earlyRuntime.registerAttackPress(true) != com.cursedarchie.platformer.combat.InputResult.EARLY_INPUT ||
                earlyRuntime.isHurried() || earlyRuntime.duration() != def.duration)
                throw new AssertionError("EARLY changed or weakened the current strike");
            if (earlyRuntime.registerAttackPress(true) != com.cursedarchie.platformer.combat.InputResult.NO_INPUT)
                throw new AssertionError("A second press of the same strike was classified twice");
            com.cursedarchie.platformer.combat.AttackRuntime windowRuntime =
                new com.cursedarchie.platformer.combat.AttackRuntime(def, false);
            windowRuntime.advance(def.nextInputTime + .04f);
            if (windowRuntime.registerAttackPress(true) != com.cursedarchie.platformer.combat.InputResult.COMBO_BUFFERED)
                throw new AssertionError("A press inside the window was not buffered");
            com.cursedarchie.platformer.combat.AttackRuntime lateRuntime =
                new com.cursedarchie.platformer.combat.AttackRuntime(def, false);
            lateRuntime.advance(lateRuntime.comboWindowEnd() + .02f);
            if (lateRuntime.registerAttackPress(true) != com.cursedarchie.platformer.combat.InputResult.LATE_INPUT)
                throw new AssertionError("A press after the window was not classified as late");
        } finally { w.dispose(); }

        // Once the old strike has fully ended, a genuinely new press may start a fresh chain, never strike two.
        java.util.List<String> late = rhythmRun(.49f, 90, 0, hurried);
        if (late.size() != 2 || !"hero_light_1".equals(late.get(1)))
            throw new AssertionError("A post-recovery press did not start a clean first strike: " + late);

        // Holding the button is one strike, not a stream of them.
        GameWorld held = combatFixture();
        try {
            for (Enemy e : held.getEnemies()) { e.setActive(false); e.getBody().setTransform(30f, 2.5f, 0f); }
            Hero hero = held.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            held.update(GameWorld.FIXED_STEP);
            held.getHeroController().keyDown(Input.Keys.X);
            int strikes = 0;
            Object last = null;
            for (int i = 0; i < 180; i++) {
                held.update(GameWorld.FIXED_STEP);
                Object a = heroAttack(hero);
                if (a != null && a != last) { strikes++; last = a; }
            }
            held.getHeroController().keyUp(Input.Keys.X);
            if (strikes != 1) throw new AssertionError("Holding attack started " + strikes + " strikes");
        } finally { held.dispose(); }
    }

    private void checkThreeHitFinisherRules() {
        com.cursedarchie.platformer.combat.AttackRhythmConfig cfg =
            com.cursedarchie.platformer.combat.AttackRhythmConfig.get();
        if (cfg.inputBufferTime < .12f || cfg.inputBufferTime > .18f)
            throw new AssertionError("Combo buffer lifetime is outside 0.12-0.18 seconds");
        if (cfg.finisherStaggerDuration < .45f || cfg.finisherShieldDamageMultiplier < 2f ||
            cfg.finisherRecoveryDuration < .55f)
            throw new AssertionError("Finisher stagger, shield damage, or recovery is below the design floor");

        // Light one and two may damage an attacking enemy, but never cancel its attack state.
        GameWorld interrupt = combatFixture();
        try {
            parkEnemies(interrupt);
            Hero hero = interrupt.getHero();
            Enemy target = staticFoe(interrupt, 11.2f);
            target.setMaxHealth(20f);
            target.setHealth(20f);
            for (int step = 1; step <= 2; step++) {
                target.getCombatStateMachine().changeState(new AttackState());
                AttackDefinition def = hero.attackDefinition(step);
                CombatHit light = new CombatHit(def.type, def.damageMultiplier, hero, target, 1f,
                    def.stabilityDamage, def.knockback, def.staggerDuration, def.blockable);
                if (!target.takeMeleeHit(light) || !(target.getCombatStateMachine().getCurrentState() instanceof AttackState))
                    throw new AssertionError("LIGHT_" + step + " interrupted an enemy attack");
            }
            target.getCombatStateMachine().changeState(new AttackState());
            AttackDefinition finisher = hero.attackDefinition(
                com.cursedarchie.platformer.actors.hero.states.combat.AttackState.COMBO_FINISHER);
            CombatHit finalHit = new CombatHit(finisher.type, finisher.damageMultiplier, hero, target, 1f,
                finisher.stabilityDamage, finisher.knockback, finisher.staggerDuration, finisher.blockable);
            target.takeMeleeHit(finalHit);
            if (target.getCombatStateMachine().getCurrentState() instanceof AttackState || !target.isStaggered())
                throw new AssertionError("FINISHER_3 did not cancel the enemy attack and stagger it");
        } finally { interrupt.dispose(); }

        // The area finisher hits every visible target once and cannot reach through level geometry.
        GameWorld area = combatFixture();
        try {
            parkEnemies(area);
            Hero hero = area.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.setFacingLeft(false);
            Enemy[] targets = {staticFoe(area, 10.9f), staticFoe(area, 11.6f), staticFoe(area, 12.15f)};
            float[] before = new float[targets.length];
            for (int i = 0; i < targets.length; i++) {
                targets[i].setMaxHealth(20f);
                targets[i].setHealth(20f);
                before[i] = targets[i].getHealth();
                hero.getAttackTargets().add(targets[i]);
            }
            com.cursedarchie.platformer.actors.hero.states.combat.AttackState finisherState =
                new com.cursedarchie.platformer.actors.hero.states.combat.AttackState(com.cursedarchie.platformer.actors.hero.states.combat.AttackState.COMBO_FINISHER);
            hero.getCombatStateMachine().changeState(finisherState);
            for (int i = 0; i < 30; i++) area.update(GameWorld.FIXED_STEP);
            float expectedLoss = hero.getDamage() * cfg.finisherDamageMultiplier;
            for (int i = 0; i < targets.length; i++)
                closeTo(before[i] - targets[i].getHealth(), expectedLoss,
                    "finisher area target " + i + " was missed or hit more than once");

            if (heroAttack(hero) != finisherState || !finisherState.isInRecovery(hero))
                throw new AssertionError("FINISHER_3 did not enter its mandatory recovery");
            area.getHeroController().keyDown(Input.Keys.X);
            area.getHeroController().keyUp(Input.Keys.X);
            area.getHeroController().keyDown(Input.Keys.RIGHT);
            for (int i = 0; i < 12; i++) area.update(GameWorld.FIXED_STEP);
            area.getHeroController().keyUp(Input.Keys.RIGHT);
            if (heroAttack(hero) != finisherState)
                throw new AssertionError("Input cancelled FINISHER_RECOVERY");
            for (int i = 0; i < 80 && heroAttack(hero) != null; i++) area.update(GameWorld.FIXED_STEP);
            for (int i = 0; i < 20; i++) area.update(GameWorld.FIXED_STEP);
            if (heroAttack(hero) != null)
                throw new AssertionError("Buffered input started an attack after FINISHER_RECOVERY");
        } finally { area.dispose(); }

        GameWorld cover = combatFixture();
        try {
            parkEnemies(cover);
            Hero hero = cover.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.setFacingLeft(false);
            Enemy blocked = staticFoe(cover, 11.8f);
            blocked.setMaxHealth(20f);
            blocked.setHealth(20f);
            hero.getAttackTargets().add(blocked);
            addLevelWall(cover, 11.15f, 2.5f, .08f, 1.4f);
            hero.getCombatStateMachine().changeState(
                new com.cursedarchie.platformer.actors.hero.states.combat.AttackState(com.cursedarchie.platformer.actors.hero.states.combat.AttackState.COMBO_FINISHER));
            for (int i = 0; i < 30; i++) cover.update(GameWorld.FIXED_STEP);
            closeTo(blocked.getHealth(), 20f, "finisher passed through a wall");
        } finally { cover.dispose(); }

        // A frontal guard absorbs most damage but is broken and staggered by the finisher.
        GameWorld guardWorld = combatFixture();
        try {
            parkEnemies(guardWorld);
            Hero hero = guardWorld.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            Enemy guard = staticFoe(guardWorld, 11.2f);
            guard.forceBlockForTest();
            float health = guard.getHealth();
            AttackDefinition finisher = hero.attackDefinition(
                com.cursedarchie.platformer.actors.hero.states.combat.AttackState.COMBO_FINISHER);
            guard.takeMeleeHit(new CombatHit(finisher.type, hero.getDamage() * finisher.damageMultiplier,
                hero, guard, 1f, finisher.stabilityDamage, finisher.knockback,
                finisher.staggerDuration, finisher.blockable));
            float loss = health - guard.getHealth();
            if (guard.isBlocking() || !guard.isStaggered() || loss <= 0f || loss >= hero.getDamage() * finisher.damageMultiplier)
                throw new AssertionError("Finisher did not break the guard with reduced guarded damage");
        } finally { guardWorld.dispose(); }

        // Damage during a chain clears a previously accepted follow-up.
        GameWorld damaged = combatFixture();
        try {
            parkEnemies(damaged);
            Hero hero = damaged.getHero();
            damaged.update(GameWorld.FIXED_STEP);
            damaged.getHeroController().keyDown(Input.Keys.X);
            damaged.getHeroController().keyUp(Input.Keys.X);
            for (int i = 0; i < 22; i++) damaged.update(GameWorld.FIXED_STEP);
            damaged.getHeroController().keyDown(Input.Keys.X);
            damaged.getHeroController().keyUp(Input.Keys.X);
            Enemy source = damaged.getEnemies().first();
            hero.receiveHit(new CombatHit(CombatHit.Type.MELEE_LIGHT, .25f, source, hero, -1f,
                0f, 0f, .1f, true));
            for (int i = 0; i < 60; i++) damaged.update(GameWorld.FIXED_STEP);
            com.cursedarchie.platformer.actors.hero.states.combat.AttackState current = heroAttack(hero);
            if (current != null && current.getComboStep() ==
                com.cursedarchie.platformer.actors.hero.states.combat.AttackState.COMBO_SECOND)
                throw new AssertionError("Damage left a buffered LIGHT_2 alive");
        } finally { damaged.dispose(); }
    }

    private static void parkEnemies(GameWorld w) {
        for (Enemy e : w.getEnemies()) { e.setActive(false); e.getBody().setTransform(40f, 2.5f, 0f); }
    }

    private static void addLevelWall(GameWorld w, float x, float y, float halfWidth, float halfHeight) {
        com.badlogic.gdx.physics.box2d.BodyDef def = new com.badlogic.gdx.physics.box2d.BodyDef();
        def.type = com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody;
        def.position.set(x, y);
        com.badlogic.gdx.physics.box2d.Body body = w.getWorld().createBody(def);
        body.setUserData("level");
        com.badlogic.gdx.physics.box2d.PolygonShape shape = new com.badlogic.gdx.physics.box2d.PolygonShape();
        shape.setAsBox(halfWidth, halfHeight);
        com.badlogic.gdx.physics.box2d.FixtureDef fixtureDef = new com.badlogic.gdx.physics.box2d.FixtureDef();
        fixtureDef.shape = shape;
        // Same collision filter as real level walls, so the hero and enemies are stopped by it.
        fixtureDef.filter.categoryBits = com.cursedarchie.platformer.physics.PhysicsCategories.WALL;
        fixtureDef.filter.maskBits = (short) (com.cursedarchie.platformer.physics.PhysicsCategories.HERO |
            com.cursedarchie.platformer.physics.PhysicsCategories.ENEMY);
        body.createFixture(fixtureDef);
        shape.dispose();
    }

    private static Enemy staticFoe(GameWorld w, float x) {
        Enemy e = EnemyFactory.createDefaultEnemy(w.getWorld(), new Vector2(x, 2.5f));
        e.setActive(false);
        e.setFacingLeft(true);
        w.addEnemy(e);
        e.getBody().setType(com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody);
        return e;
    }

    /** Z is a dodge roll: no charge-up, no damage, no cooldown, 25% faster than walking, and it passes through enemies. */
    private void checkDodgeRoll() {
        if (Math.abs(Hero.ROLL_SPEED - Hero.WALK_SPEED * 1.25f) > .01f)
            throw new AssertionError("Roll speed must be 25% above walking speed: " + Hero.ROLL_SPEED);
        GameWorld w = combatFixture();
        try {
            parkEnemies(w);
            Hero hero = w.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            hero.setFacingLeft(false);
            Enemy foe = staticFoe(w, 11.4f);
            float foeHealth = foe.getHealth();
            for (int settle = 0; settle < 20; settle++) w.update(GameWorld.FIXED_STEP);
            float startX = hero.getPosition().x;
            // No wind-up: the roll begins within a few steps of the press and never becomes an attack.
            w.getHeroController().keyDown(Input.Keys.Z);
            w.getHeroController().keyUp(Input.Keys.Z);
            boolean rolling = false, sawAttack = false, sawSpeed = false;
            int steps = 0;
            while (steps < 60 && (!rolling || hero.isRolling())) {
                w.update(GameWorld.FIXED_STEP);
                steps++;
                if (hero.isRolling()) {
                    rolling = true;
                    if (Math.abs(hero.getBody().getLinearVelocity().x - Hero.ROLL_SPEED) < .5f) sawSpeed = true;
                }
                if (heroAttack(hero) != null) sawAttack = true;
                if (!rolling && steps > 4) throw new AssertionError("The roll had a wind-up (did not start within 4 steps)");
            }
            if (!rolling) throw new AssertionError("Z did not start a roll");
            if (sawAttack) throw new AssertionError("The roll started an attack");
            if (!sawSpeed) throw new AssertionError("The roll never reached its faster speed");
            if (hero.getPosition().x <= foe.getPosition().x)
                throw new AssertionError("The roll did not carry the hero through the enemy");
            if (hero.getPosition().x - startX < Hero.ROLL_SPEED * Hero.ROLL_DURATION * .6f)
                throw new AssertionError("The roll covered too little ground");
            closeTo(foe.getHealth(), foeHealth, "a roll must deal no damage");
            // No cooldown: the next press rolls again at once.
            w.getHeroController().keyDown(Input.Keys.Z);
            w.getHeroController().keyUp(Input.Keys.Z);
            boolean again = false;
            for (int i = 0; i < 4 && !again; i++) { w.update(GameWorld.FIXED_STEP); again = hero.isRolling(); }
            if (!again) throw new AssertionError("A second roll was blocked: the roll has no cooldown");
            // A held key never repeats the roll by itself.
            for (int i = 0; i < 40; i++) w.update(GameWorld.FIXED_STEP);
            w.getHeroController().keyDown(Input.Keys.Z);
            int starts = 0;
            boolean was = false;
            for (int i = 0; i < 90; i++) {
                w.update(GameWorld.FIXED_STEP);
                if (hero.isRolling() && !was) starts++;
                was = hero.isRolling();
            }
            w.getHeroController().keyUp(Input.Keys.Z);
            if (starts != 1) throw new AssertionError("A held roll key repeated the roll: " + starts);
        } finally { w.dispose(); }

        // A jump is possible during a roll and keeps the roll's momentum.
        GameWorld jr = combatFixture();
        try {
            parkEnemies(jr);
            Hero hero = jr.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.setFacingLeft(false);
            for (int settle = 0; settle < 20; settle++) jr.update(GameWorld.FIXED_STEP);
            jr.getHeroController().keyDown(Input.Keys.Z);
            jr.getHeroController().keyUp(Input.Keys.Z);
            for (int i = 0; i < 3; i++) jr.update(GameWorld.FIXED_STEP);
            if (!hero.isRolling()) throw new AssertionError("Roll did not start for the jump-out test");
            jr.getHeroController().keyDown(Input.Keys.SPACE);
            boolean jumped = false;
            for (int i = 0; i < 6 && !jumped; i++) {
                jr.update(GameWorld.FIXED_STEP);
                jumped = hero.getBody().getLinearVelocity().y > 1f;
            }
            jr.getHeroController().keyUp(Input.Keys.SPACE);
            if (!jumped) throw new AssertionError("The hero could not jump during a roll");
        } finally { jr.dispose(); }

        // Combos work in the air: a jump attack chains into strike two, and plain air strikes chain too.
        GameWorld air = combatFixture();
        try {
            parkEnemies(air);
            Hero hero = air.getHero();
            hero.setFacingLeft(false);
            java.util.List<String> started = new java.util.ArrayList<String>();
            Object last = null;
            int secondStart = -1;
            for (int i = 0; i < 140; i++) {
                hero.getBody().setTransform(10f, 9f, 0f);   // keep the hero in the air
                hero.getBody().setLinearVelocity(0f, 0f);
                if (i == 3 || i == 27 || i == 50) {
                    air.getHeroController().keyDown(Input.Keys.X);
                    air.getHeroController().keyUp(Input.Keys.X);
                }
                air.update(GameWorld.FIXED_STEP);
                com.cursedarchie.platformer.actors.hero.states.combat.AttackState a = heroAttack(hero);
                if (a != null && a != last) {
                    started.add(a.getDefinition().id);
                    last = a;
                    if (started.size() == 2) secondStart = i;
                }
            }
            if (started.size() < 3 || !"hero_jump_attack".equals(started.get(0)) || !"hero_light_2".equals(started.get(1)) ||
                !"hero_light_3".equals(started.get(2)))
                throw new AssertionError("The full combo was not possible in the air: " + started);
            if (secondStart > 40)
                throw new AssertionError("The second air strike waited for the jump attack's recovery: step " + secondStart);
        } finally { air.dispose(); }
    }

    /** Guard during attack recovery, arrow lifetime outside rooms, and enemies turning to a hero behind them. */
    private void checkFlowTweaks() {
        // Guard: raising it during a strike's recovery cuts the recovery short; holding it through the swing works too.
        for (int variant = 0; variant < 2; variant++) {
            GameWorld g = combatFixture();
            try {
                parkEnemies(g);
                Hero hero = g.getHero();
                hero.getBody().setTransform(10f, 2.5f, 0f);
                hero.setFacingLeft(false);
                for (int settle = 0; settle < 20; settle++) g.update(GameWorld.FIXED_STEP);
                g.getHeroController().keyDown(Input.Keys.X);
                g.getHeroController().keyUp(Input.Keys.X);
                if (variant == 1) { g.update(GameWorld.FIXED_STEP); g.getHeroController().keyDown(Input.Keys.B); }
                int guard = 0;
                com.cursedarchie.platformer.actors.hero.states.combat.AttackState a = null;
                while (guard++ < 80) {
                    g.update(GameWorld.FIXED_STEP);
                    a = heroAttack(hero);
                    if (variant == 1 ? a == null : a != null && a.isInRecovery(hero)) break;
                }
                if (variant == 0) {
                    if (a == null) throw new AssertionError("The strike never reached its recovery");
                    g.getHeroController().keyDown(Input.Keys.B);
                    g.update(GameWorld.FIXED_STEP);
                }
                if (heroAttack(hero) != null) throw new AssertionError("Guard did not cut the strike's recovery short (variant " + variant + ")");
                if (!hero.isBlocking()) throw new AssertionError("Hero could not block after a strike (variant " + variant + ")");
                g.getHeroController().keyUp(Input.Keys.B);
            } finally { g.dispose(); }
        }

        // Arrows: an arrow inside a room is never removed by age; outside every room it lasts 3 s.
        GameWorld a = combatFixture();
        try {
            parkEnemies(a);
            Enemy shooter = EnemyFactory.createScavenger(a.getWorld(), new Vector2(15f, 2.6f));
            shooter.setActive(false);
            a.getHero().getBody().setTransform(30f, 2.5f, 0f);
            a.fireArrows(shooter, new Vector2(0f, 1f), RangedAttackConfig.NORMAL);
            if (a.getArrows().size != 1) throw new AssertionError("Test arrow was not fired");
            a.getArrows().first().age = 100f;
            a.update(GameWorld.FIXED_STEP);
            if (a.getArrows().size != 1 && a.getArrowImpacts().size == 0)
                throw new AssertionError("An arrow inside a room vanished from age alone");
            a.getArrows().clear();
            Enemy far = EnemyFactory.createScavenger(a.getWorld(), new Vector2(-500f, -500f));
            far.setActive(false);
            a.fireArrows(far, new Vector2(-1f, 0f), RangedAttackConfig.NORMAL);
            for (int i = 0; i < 170; i++) a.update(GameWorld.FIXED_STEP);
            if (a.getArrows().size != 1) throw new AssertionError("An arrow outside the rooms vanished before 3 s");
            for (int i = 0; i < 30; i++) a.update(GameWorld.FIXED_STEP);
            if (a.getArrows().size != 0) throw new AssertionError("An arrow outside the rooms outlived 3 s");
        } finally { a.dispose(); }

        // An enemy already aware of the hero turns round to a hero that slips behind it a few units away.
        GameWorld t = new GameWorld();
        try {
            Hero hero = t.getHero();
            hero.getBody().setTransform(18f, 2.5f, 0f);
            Enemy foe = EnemyFactory.createDefaultEnemy(t.getWorld(), new Vector2(20f, 2.5f));
            foe.setBrain(new SimpleEnemyBrain(t));
            foe.setFacingLeft(true);
            t.addEnemy(foe);
            for (int i = 0; i < 20; i++) t.update(GameWorld.FIXED_STEP);
            if (foe.getAwareness() == Enemy.Awareness.UNAWARE) throw new AssertionError("Enemy never noticed the hero");
            hero.getBody().setTransform(foe.getPosition().x + 3.2f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            boolean turned = false;
            for (int i = 0; i < 8 && !turned; i++) { t.update(GameWorld.FIXED_STEP); turned = !foe.isFacingLeft(); }
            if (!turned) throw new AssertionError("An aware enemy did not turn round quickly to a hero behind it");
        } finally { t.dispose(); }
    }

    private void checkArrowArcPhysics() {
        GameWorld fight = new GameWorld();
        try {
            Hero hero = fight.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            Enemy shooter = EnemyFactory.createScavenger(fight.getWorld(), new Vector2(15f, 2.5f));
            shooter.setActive(false);
            // Gravity bends the flight: a horizontal shot loses height and its angle turns downward.
            fight.fireArrows(shooter, new Vector2(-1f, 0f), RangedAttackConfig.NORMAL);
            GameWorld.Arrow arrow = fight.getArrows().first();
            float startY = arrow.position.y;
            for (int i = 0; i < 12; i++) fight.update(GameWorld.FIXED_STEP);
            if (fight.getArrows().size == 0) throw new AssertionError("Arrow vanished before reaching the hero");
            if (!(arrow.velocity.y < 0f) || !(arrow.position.y < startY))
                throw new AssertionError("Arrow did not fall under gravity");
            // The empowered shot is flatter than the standard one over the same distance.
            if (!(RangedAttackConfig.CHARGED.gravity < RangedAttackConfig.NORMAL.gravity))
                throw new AssertionError("Empowered shot must arc less than the standard shot");
            // Aim compensation: the previewed flight ends on the hero, and a live arrow follows the same path.
            fight.getArrows().clear();
            Vector2 aim = RangedAttackConfig.NORMAL.launchDirection(15f, 2.6f, 10f, 2.5f);
            float[] xs = new float[96], ys = new float[96];
            int n = fight.predictArrowPath(15f + aim.x * .65f, 2.6f + aim.y * .65f, aim.x, aim.y,
                RangedAttackConfig.NORMAL, xs, ys);
            if (Math.abs(xs[n - 1] - 10f) > .8f || Math.abs(ys[n - 1] - 2.5f) > .8f)
                throw new AssertionError("Aim preview does not end on the target: " + xs[n - 1] + "," + ys[n - 1]);
            float health = hero.getHealth();
            fight.fireArrows(shooter, aim, RangedAttackConfig.NORMAL);
            for (int i = 0; i < 90 && fight.getArrows().size > 0; i++) fight.update(GameWorld.FIXED_STEP);
            if (hero.getHealth() >= health) throw new AssertionError("Arc-compensated arrow missed the hero");
            // Fast arrows cannot tunnel through a thin wall.
            com.badlogic.gdx.physics.box2d.BodyDef wallDef = new com.badlogic.gdx.physics.box2d.BodyDef();
            wallDef.type = com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody;
            wallDef.position.set(12.5f, 2.5f);
            com.badlogic.gdx.physics.box2d.Body wall = fight.getWorld().createBody(wallDef);
            wall.setUserData("level");
            com.badlogic.gdx.physics.box2d.PolygonShape shape = new com.badlogic.gdx.physics.box2d.PolygonShape();
            shape.setAsBox(.02f, 1.2f);
            wall.createFixture(shape, 0f);
            shape.dispose();
            health = hero.getHealth();
            fight.fireArrows(shooter, new Vector2(-1f, 0f), RangedAttackConfig.CHARGED);
            for (int i = 0; i < 40; i++) fight.update(GameWorld.FIXED_STEP);
            closeTo(hero.getHealth(), health, "A fast arrow tunnelled through a thin wall");
        } finally {
            fight.dispose();
        }
    }

    private void checkGroupAttackSlots() {
        GameWorld fight = new GameWorld();
        try {
            Hero hero = fight.getHero();
            hero.setMaxHealth(1000f);
            hero.setHealth(1000f);
            hero.getBody().setTransform(10f, 2.5f, 0f);
            for (int i = 0; i < 4; i++) {
                Enemy e = EnemyFactory.createDefaultEnemy(fight.getWorld(), new Vector2(13f + i * 1.5f, 2.5f));
                e.setBrain(new SimpleEnemyBrain(fight));
                fight.addEnemy(e);
            }
            for (int i = 0; i < 90; i++) fight.update(GameWorld.FIXED_STEP);
            // There is no global attacker cap any more: the slots themselves are the limit.
            com.cursedarchie.platformer.actors.enemies.group.CombatSlotManager slots =
                fight.getEnemyController().getSlotManager();
            int holders = 0;
            for (Enemy e : fight.getAliveEnemies()) if (e.hasAttackSlot()) holders++;
            if (holders != slots.countOccupied(com.cursedarchie.platformer.actors.enemies.group.CombatSlot.Type.MELEE))
                throw new AssertionError("Melee slot ownership and enemy slots disagree");
            if (holders < 2) throw new AssertionError("Fewer than two melee enemies hold an attack slot: " + holders);
            // An archer standing behind an ally may not shoot through it.
            Enemy archer = EnemyFactory.createScavenger(fight.getWorld(), new Vector2(20f, 2.5f));
            archer.setBrain(null);
            archer.setIntent(Enemy.Intent.IDLE);
            fight.addEnemy(archer);
            archer.updateSpecialActions(fight, hero, fight.getAliveEnemies(), .01f);
            if (!archer.isLineBlockedByAlly(hero.getPosition()))
                throw new AssertionError("Archer did not notice an ally blocking its line of fire");
        } finally {
            fight.dispose();
        }
    }

    /*=====================================GROUP POSITIONING AND SOFT PUSHING=========================================*/

    /** Watches a fight step by step and enforces the rules that must hold at every moment, not just at the end. */
    private static final class GroupWatch {
        final GameWorld world;
        boolean heroMoves;
        int maxAttackers;
        float maxStep;
        private final java.util.IdentityHashMap<Enemy, float[]> last = new java.util.IdentityHashMap<>();

        GroupWatch(GameWorld world) { this.world = world; }

        void run(int steps) { for (int i = 0; i < steps; i++) step(); }

        void step() {
            world.update(GameWorld.FIXED_STEP);
            Hero hero = world.getHero();
            float hx = hero.getPosition().x;
            int attackers = 0;
            for (Enemy e : world.getAliveEnemies()) {
                if (!e.isActive()) continue;
                boolean melee = e.getCombatRole().isMelee();
                boolean attacking = e.isAttacking();
                if (attacking && melee) attackers++;
                if (attacking && melee && e.isSlotManaged() && !e.hasAttackSlot())
                    throw new AssertionError("A melee enemy attacked without holding an attack slot");
                if (e.getCurrentSlot() != null && e.getCurrentSlot().getOwner() != e)
                    throw new AssertionError("Slot " + e.getCurrentSlot() + " is listed on an enemy that does not own it");
                float x = e.getPosition().x;
                float side = Math.signum(x - hx);
                float[] before = last.get(e);
                if (before != null) {
                    if (!e.isStaggered() && !e.isKnockedDown()) maxStep = Math.max(maxStep, Math.abs(x - before[0]));
                    // Allies never pass through the hero: changing sides needs a leap (which starts its cooldown).
                    if (!heroMoves && before[1] != 0f && side != 0f && before[1] != side &&
                        e.getLeapCooldownRemaining() <= 0f && !e.isLeaping())
                        throw new AssertionError("An enemy passed through the hero without leaping");
                    // A melee attack may only start from within the weapon's reach of the hero.
                    if (attacking && melee && before[2] == 0f && e.isSlotManaged() &&
                        Math.abs(x - hx) > e.getAttackApproachDistance(hero) + .25f)
                        throw new AssertionError("An enemy started an attack from outside its reach");
                }
                last.put(e, new float[]{x, side, attacking ? 1f : 0f});
            }
            maxAttackers = Math.max(maxAttackers, attackers);
            for (com.cursedarchie.platformer.actors.enemies.group.CombatSlot slot :
                world.getEnemyController().getSlotManager().getSlots())
                if (slot.getOwner() != null && slot.getOwner().getCurrentSlot() != slot)
                    throw new AssertionError("Slot " + slot + " names an owner that does not hold it");
        }
    }

    /** An open arena: the hero stands still (static) on the open floor at X0, with plenty of health. */
    private static GameWorld groupArena() {
        GameWorld w = new GameWorld();
        parkEnemies(w);
        Hero hero = w.getHero();
        hero.setMaxHealth(1000f);
        hero.setHealth(1000f);
        hero.getBody().setTransform(X0, 2.5f, 0f);
        hero.getBody().setLinearVelocity(0f, 0f);
        hero.setFacingLeft(false);
        hero.getBody().setType(com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody);
        return w;
    }

    private static Enemy groupFoe(GameWorld w, String type, float x) {
        Vector2 at = new Vector2(x, 2.5f);
        Enemy e;
        switch (type) {
            case "scavenger": e = EnemyFactory.createScavenger(w.getWorld(), at); break;
            case "infiltrator": e = EnemyFactory.createInfiltrator(w.getWorld(), at); break;
            case "overseer": e = EnemyFactory.createOverseer(w.getWorld(), at); break;
            case "red_collector": e = EnemyFactory.createRedCollector(w.getWorld(), at); break;
            case "heavy_hunter": e = EnemyFactory.createHeavyHunter(w.getWorld(), at); break;
            default: e = EnemyFactory.createDefaultEnemy(w.getWorld(), at);
        }
        e.getBody().setTransform(x, 2f + e.getSize() / 2f + .05f, 0f);
        e.setBrain(new SimpleEnemyBrain(w));
        e.setFacingLeft(x > X0);
        w.addEnemy(e);
        return e;
    }

    /** The enemies that really take part (parked or static fixtures are inactive). */
    private static Array<Enemy> activeFoes(GameWorld w) {
        Array<Enemy> result = new Array<>();
        for (Enemy e : w.getAliveEnemies()) if (e.isActive()) result.add(e);
        return result;
    }

    private static com.cursedarchie.platformer.actors.enemies.group.CombatSlotManager slotsOf(GameWorld w) {
        return w.getEnemyController().getSlotManager();
    }

    private static final com.cursedarchie.platformer.actors.enemies.group.CombatSlot.Type MELEE_SLOT =
        com.cursedarchie.platformer.actors.enemies.group.CombatSlot.Type.MELEE;

    private void checkGroupPositioning() {
        checkGroupDistribution();
        checkGroupSlotRelease();
        checkGroupRoles();
        checkGroupMovement();
        checkSoftPushing();
    }

    private void checkGroupDistribution() {
        GameWorld w = groupArena();
        try {
            GroupWatch watch = new GroupWatch(w);
            float[] xs = {2.6f, 3.4f, 4f, 4.5f, 5f};
            for (float x : xs) groupFoe(w, "default", X0 + x);
            int maxOccupied = 0;
            boolean sawWaiting = false;
            for (int i = 0; i < 480; i++) {
                watch.step();
                if (i >= 240) {
                    maxOccupied = Math.max(maxOccupied, slotsOf(w).countOccupied(MELEE_SLOT));
                    for (Enemy e : activeFoes(w))
                        if (e.getSlotState() == com.cursedarchie.platformer.actors.enemies.group.SlotState.SURROUNDING)
                            sawWaiting = true;
                }
            }
            float hx = w.getHero().getPosition().x;
            int left = 0, right = 0;
            for (Enemy e : activeFoes(w)) {
                if (e.getPosition().x < hx - .5f) left++;
                else if (e.getPosition().x > hx + .5f) right++;
            }
            if (left < 1 || right < 1)
                throw new AssertionError("Five enemies from one side did not spread around the hero: left=" + left + " right=" + right);
            if (maxOccupied < 3)
                throw new AssertionError("Fewer than three melee slots were filled at once: " + maxOccupied);
            if (!sawWaiting)
                throw new AssertionError("With more enemies than attack slots, one must wait on a surrounding position");
            if (watch.maxAttackers < 2)
                throw new AssertionError("Enemies never attacked simultaneously from different slots");
            if (watch.maxStep > .35f)
                throw new AssertionError("An enemy moved " + watch.maxStep + " m in one step (teleport-like)");
            // No total overlap: allies keep a distance even though they are not solid.
            Array<Enemy> all = activeFoes(w);
            for (int i = 0; i < all.size; i++)
                for (int j = i + 1; j < all.size; j++)
                    if (Math.abs(all.get(i).getPosition().x - all.get(j).getPosition().x) < .35f &&
                        Math.abs(all.get(i).getPosition().y - all.get(j).getPosition().y) < .9f)
                        throw new AssertionError("Two enemies stand on top of each other");
        } finally { w.dispose(); }
    }

    private void checkGroupSlotRelease() {
        GameWorld w = groupArena();
        try {
            GroupWatch watch = new GroupWatch(w);
            for (float x : new float[]{2.6f, 3.4f, 4f, 4.5f, 5f}) groupFoe(w, "default", X0 + x);
            watch.run(420);
            com.cursedarchie.platformer.actors.enemies.group.CombatSlot front = slotsOf(w).findSlot("FRONT_RIGHT");
            Enemy first = front.getOwner();
            if (first == null) throw new AssertionError("Nobody holds the front-right slot after the approach");
            // A dying attacker frees its slot and the nearest suitable waiting enemy takes it.
            first.setAlive(false);
            watch.run(240);
            if (front.getOwner() == null || front.getOwner() == first || !front.getOwner().isAlive())
                throw new AssertionError("The freed slot was not taken by another enemy");
            // A staggered enemy gives its slot up at once.
            Enemy holder = null;
            for (Enemy e : activeFoes(w))
                if (e.getSlotState() == com.cursedarchie.platformer.actors.enemies.group.SlotState.ATTACK_POSITION &&
                    !e.isAttacking()) { holder = e; break; }
            if (holder == null) throw new AssertionError("No enemy standing in an attack position to stagger");
            holder.applyStagger(.8f);
            watch.run(2);
            if (holder.getCurrentSlot() != null)
                throw new AssertionError("A staggered enemy kept its slot");
        } finally { w.dispose(); }
    }

    private void checkGroupRoles() {
        com.cursedarchie.platformer.actors.enemies.group.GroupPositioningConfig cfg =
            com.cursedarchie.platformer.actors.enemies.group.GroupPositioningConfig.get();
        // Ranged: keeps its distance, never takes a melee slot.
        GameWorld w = groupArena();
        try {
            GroupWatch watch = new GroupWatch(w);
            Enemy archer = groupFoe(w, "scavenger", X0 + 7f);
            groupFoe(w, "default", X0 + 3.5f);
            groupFoe(w, "default", X0 + 4.5f);
            for (int i = 0; i < 480; i++) {
                watch.step();
                if (archer.hasAttackSlot()) throw new AssertionError("An archer took a melee slot");
            }
            float d = Math.abs(archer.getPosition().x - X0);
            if (d < cfg.rangedMinDistance - .6f || d > cfg.rangedMaxDistance + .8f)
                throw new AssertionError("Archer stands " + d + " m from the hero, outside its firing band");
            if (archer.getSlotState() != com.cursedarchie.platformer.actors.enemies.group.SlotState.RANGED_POSITION)
                throw new AssertionError("Archer is not in its ranged position: " + archer.getSlotState());
            if (archer.isLineBlockedByAlly(w.getHero().getPosition()))
                throw new AssertionError("Archer still has an ally in its line of fire");
        } finally { w.dispose(); }

        // Ranged repositions when an ally blocks the line of fire.
        w = groupArena();
        try {
            GroupWatch watch = new GroupWatch(w);
            watch.heroMoves = false;
            Enemy blocker = EnemyFactory.createDefaultEnemy(w.getWorld(), new Vector2(X0 + 3f, 2.5f));
            blocker.setActive(false);
            w.addEnemy(blocker);
            blocker.getBody().setType(com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody);
            Enemy archer = groupFoe(w, "scavenger", X0 + 7f);
            watch.run(540);
            if (archer.getPosition().x >= X0 || archer.isLineBlockedByAlly(w.getHero().getPosition()))
                throw new AssertionError("Archer did not move to a side with a clear line of fire (x=" +
                    archer.getPosition().x + ")");
        } finally { w.dispose(); }

        // Support: behind the front line, no melee slot, safe distance.
        w = groupArena();
        try {
            GroupWatch watch = new GroupWatch(w);
            Enemy support = groupFoe(w, "overseer", X0 + 5.5f);
            groupFoe(w, "default", X0 + 3f);
            groupFoe(w, "default", X0 + 3.8f);
            groupFoe(w, "default", X0 + 4.5f);
            for (int i = 0; i < 480; i++) {
                watch.step();
                if (support.hasAttackSlot() || support.isAttacking())
                    throw new AssertionError("The support caster joined the melee");
            }
            float sd = Math.abs(support.getPosition().x - X0);
            float side = Math.signum(support.getPosition().x - X0);
            if (sd < cfg.supportMinDistance - .7f) throw new AssertionError("Support stands too close: " + sd);
            for (Enemy e : activeFoes(w)) {
                if (e == support || !e.getCombatRole().isMelee() || Math.signum(e.getPosition().x - X0) != side) continue;
                if (Math.abs(e.getPosition().x - X0) > sd + .2f)
                    throw new AssertionError("Support stands in front of a fighting ally");
            }
            if (support.getSlotState() != com.cursedarchie.platformer.actors.enemies.group.SlotState.SUPPORT_POSITION)
                throw new AssertionError("Support is not in its support position: " + support.getSlotState());
        } finally { w.dispose(); }

        // Fast: flanks (never the head-on slot on the side the hero faces) and ends on the far side.
        w = groupArena();
        try {
            GroupWatch watch = new GroupWatch(w);
            Enemy fox = groupFoe(w, "infiltrator", X0 + 6f);
            groupFoe(w, "default", X0 + 3.5f);
            groupFoe(w, "default", X0 + 4.5f);
            boolean flanked = false;
            for (int i = 0; i < 480; i++) {
                watch.step();
                if (fox.getCurrentSlot() != null && fox.getCurrentSlot().name.equals("FRONT_RIGHT"))
                    throw new AssertionError("A fast enemy took the head-on slot in front of the hero");
                if (fox.getCurrentSlot() != null && fox.getCurrentSlot().side == -1 &&
                    fox.getPosition().x < X0) flanked = true;
            }
            if (!flanked) throw new AssertionError("The fast enemy did not flank to the hero's back");
        } finally { w.dispose(); }

        // Heavy: takes more room than a standard enemy, and two heavies never share a slot.
        w = groupArena();
        try {
            GroupWatch watch = new GroupWatch(w);
            Enemy heavy = groupFoe(w, "heavy_hunter", X0 + 3.6f);
            Enemy other = groupFoe(w, "heavy_hunter", X0 + 4.6f);
            Enemy s1 = groupFoe(w, "default", X0 + 2.8f);
            Enemy s2 = groupFoe(w, "default", X0 + 4f);
            watch.run(900);
            for (Enemy e : activeFoes(w)) {
                if (e == heavy || Math.signum(e.getPosition().x - X0) != Math.signum(heavy.getPosition().x - X0)) continue;
                float gap = Math.abs(e.getPosition().x - heavy.getPosition().x);
                float need = e == other ? cfg.heavyHeavyMinDistance : cfg.heavyStandardMinDistance;
                if (gap < need * .6f)
                    throw new AssertionError("A heavy enemy shares its space with " + e.getArchetypeId() + ": gap " + gap);
            }
            com.cursedarchie.platformer.actors.enemies.group.CombatSlotManager m = slotsOf(w);
            if (m.minDistance(heavy, other) <= m.minDistance(heavy, s1) ||
                m.minDistance(heavy, s1) <= m.minDistance(s1, s2))
                throw new AssertionError("Minimum distances must grow: standard < heavy-standard < heavy-heavy");
        } finally { w.dispose(); }
    }

    private void checkGroupMovement() {
        // The formation follows a hero who walks away: slots move with the hero and no one teleports.
        GameWorld w = groupArena();
        try {
            GroupWatch watch = new GroupWatch(w);
            for (float x : new float[]{2.6f, 3.4f, 4f, 4.5f}) groupFoe(w, "default", X0 + x);
            watch.run(240);
            watch.heroMoves = true;
            for (int i = 1; i <= 80; i++) {
                w.getHero().getBody().setTransform(X0 - i * .05f, 2.5f, 0f);
                watch.step();
            }
            watch.run(420);
            float hx = w.getHero().getPosition().x;
            int holders = 0;
            for (Enemy e : activeFoes(w)) {
                if (Math.abs(e.getPosition().x - hx) > 6.2f)
                    throw new AssertionError("An enemy stayed behind when the hero moved away");
                if (e.hasAttackSlot()) holders++;
            }
            if (holders < 2) throw new AssertionError("The formation did not re-form around the moved hero");
            if (watch.maxStep > .35f) throw new AssertionError("An enemy teleported while the formation re-formed");
        } finally { w.dispose(); }

        // The hero leaps over the group: enemies turn round and re-form on the new side.
        w = groupArena();
        try {
            GroupWatch watch = new GroupWatch(w);
            watch.heroMoves = true;
            for (float x : new float[]{2f, 3f, 4f}) groupFoe(w, "default", X0 + x);
            watch.run(240);
            for (int i = 1; i <= 40; i++) {
                float arc = (float) Math.sin(Math.PI * i / 40f) * 2.5f;
                w.getHero().getBody().setTransform(X0 + i * .1375f, 2.5f + arc, 0f);
                watch.step();
            }
            w.getHero().getBody().setTransform(X0 + 5.5f, 2.5f, 0f);
            watch.run(420);
            float hx = w.getHero().getPosition().x;
            int holders = 0;
            for (Enemy e : activeFoes(w)) {
                if (Math.abs(e.getPosition().x - hx) > 6.5f)
                    throw new AssertionError("An enemy did not follow the hero who jumped over the group");
                if (e.getSlotState() == com.cursedarchie.platformer.actors.enemies.group.SlotState.ATTACK_POSITION) {
                    holders++;
                    if (e.getPosition().x < hx != !e.isFacingLeft())
                        throw new AssertionError("An enemy in position faces away from the hero");
                }
            }
            if (holders < 2) throw new AssertionError("The group did not re-form after the hero leapt over it");
        } finally { w.dispose(); }

        // Walls: a waiting spot behind a wall is never used, so nobody presses against it.
        w = groupArena();
        try {
            GroupWatch watch = new GroupWatch(w);
            addLevelWall(w, X0 + 5.5f, 3.5f, .5f, 2f);        // left face at X0 + 5
            for (float x : new float[]{2f, 2.8f, 3.4f, 4f, 4.5f}) groupFoe(w, "default", X0 + x);
            watch.run(480);
            for (Enemy e : activeFoes(w)) {
                if (e.getPosition().x > X0 + 4.6f)
                    throw new AssertionError("An enemy is jammed against the wall at x=" + e.getPosition().x);
                if (e.getSlotState() == com.cursedarchie.platformer.actors.enemies.group.SlotState.NONE)
                    throw new AssertionError("An enemy beside the wall lost the formation");
            }
        } finally { w.dispose(); }

        // Leaving the room (or the location) clears every slot and every occupant.
        w = groupArena();
        try {
            GroupWatch watch = new GroupWatch(w);
            for (float x : new float[]{3f, 3.8f, 4.4f}) groupFoe(w, "default", X0 + x);
            watch.run(200);
            if (slotsOf(w).countOccupied(MELEE_SLOT) == 0)
                throw new AssertionError("No slots were filled before the room change");
            w.changeLocation("start", 0);
            for (com.cursedarchie.platformer.actors.enemies.group.CombatSlot slot : slotsOf(w).getSlots())
                if (slot.isOccupied()) throw new AssertionError("Slot " + slot + " survived a room change");
            for (Enemy e : w.getEnemies())
                if (e.getCurrentSlot() != null) throw new AssertionError("An enemy kept its slot across a room change");
        } finally { w.dispose(); }
    }

    private void checkSoftPushing() {
        // Collision categories: allies are not solid to each other; the hero and the level still are.
        GameWorld w = groupArena();
        try {
            Enemy a = groupFoe(w, "default", X0 + 8f);
            a.setBrain(null);
            a.setIntent(Enemy.Intent.IDLE);
            Enemy b = groupFoe(w, "default", X0 + 8.2f);
            b.setBrain(null);
            b.setIntent(Enemy.Intent.IDLE);
            for (com.badlogic.gdx.physics.box2d.Fixture f : a.getBody().getFixtureList()) {
                if (f.isSensor()) continue;
                short mask = f.getFilterData().maskBits;
                if ((mask & com.cursedarchie.platformer.physics.PhysicsCategories.ENEMY) != 0)
                    throw new AssertionError("Enemy bodies still collide with each other");
                if ((mask & com.cursedarchie.platformer.physics.PhysicsCategories.HERO) == 0 ||
                    (mask & com.cursedarchie.platformer.physics.PhysicsCategories.WALL) == 0 ||
                    (mask & com.cursedarchie.platformer.physics.PhysicsCategories.GROUND) == 0)
                    throw new AssertionError("Enemies must still collide with the hero, walls and ground");
            }
            boolean heroSolidToEnemies = false;
            for (com.badlogic.gdx.physics.box2d.Fixture f : w.getHero().getBody().getFixtureList())
                if (!f.isSensor() && (f.getFilterData().maskBits & com.cursedarchie.platformer.physics.PhysicsCategories.ENEMY) != 0)
                    heroSolidToEnemies = true;
            if (!heroSolidToEnemies) throw new AssertionError("The hero must remain solid to enemies");
            // Soft pushing: two enemies on the same spot drift apart gently, they are not thrown apart.
            float peak = 0f;
            for (int i = 0; i < 90; i++) {
                w.update(GameWorld.FIXED_STEP);
                peak = Math.max(peak, Math.max(Math.abs(a.getBody().getLinearVelocity().x),
                    Math.abs(b.getBody().getLinearVelocity().x)));
            }
            float gap = Math.abs(a.getPosition().x - b.getPosition().x);
            if (gap < .5f) throw new AssertionError("Overlapping allies did not steer apart: gap " + gap);
            if (peak > slotsOf(w).getConfig().maxSeparationSpeed + .3f)
                throw new AssertionError("Soft pushing scattered the enemies (speed " + peak + ")");
        } finally { w.dispose(); }

        // An arrow flies through the shooter's allies and only ever hits the hero or the level.
        w = groupArena();
        try {
            Enemy ally = groupFoe(w, "default", X0 + 3f);
            ally.setBrain(null);
            ally.setIntent(Enemy.Intent.IDLE);
            ally.getBody().setType(com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody);
            Enemy archer = groupFoe(w, "scavenger", X0 + 6f);
            archer.setBrain(null);
            archer.setIntent(Enemy.Intent.IDLE);
            archer.getBody().setType(com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody);
            float allyHealth = ally.getHealth();
            Vector2 aim = RangedAttackConfig.NORMAL.launchDirection(archer.getPosition().x, archer.getPosition().y,
                ally.getPosition().x, ally.getPosition().y);
            w.fireArrows(archer, aim, RangedAttackConfig.NORMAL);
            com.badlogic.gdx.math.Rectangle box = new com.badlogic.gdx.math.Rectangle();
            boolean crossed = false;
            for (int i = 0; i < 120 && w.getArrows().size > 0; i++) {
                w.update(GameWorld.FIXED_STEP);
                for (GameWorld.Arrow arrow : w.getArrows()) {
                    ally.getHurtboxBounds(box);
                    if (box.contains(arrow.position)) crossed = true;
                }
            }
            if (!crossed) throw new AssertionError("The test arrow never crossed the ally");
            if (ally.getHealth() != allyHealth) throw new AssertionError("An arrow hurt the shooter's ally");
        } finally { w.dispose(); }
    }

    private void checkNewEnemyRoles() {
        GameWorld fight = new GameWorld();
        try {
            Hero hero = fight.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            // Breaker: tough, barely staggers from light hits, strong frontal sweep vs. slam definitions.
            Enemy breaker = EnemyFactory.createHeavyHunter(fight.getWorld(), new Vector2(12f, 2.5f));
            if (breaker.getMaxHealth() < 8f) throw new AssertionError("Breaker must have high health");
            breaker.takeMeleeHit(new CombatHit(CombatHit.Type.MELEE_LIGHT, 1f, hero, breaker, -1f, 1f, 5f, .14f, true));
            if (breaker.isStaggered() && breaker.getBody().getLinearVelocity().x < -2f)
                throw new AssertionError("Breaker was knocked back by a light hit");
            com.cursedarchie.platformer.combat.AttackDefinition sweep = breaker.attackDefinition(false, true, false);
            com.cursedarchie.platformer.combat.AttackDefinition slam = breaker.attackDefinition(false, true, true);
            if (slam.startupDuration <= sweep.startupDuration)
                throw new AssertionError("Ground slam must have a longer startup than the sweep");
            if (slam.knockback <= sweep.knockback) throw new AssertionError("Slam knockback must be stronger");
            if (slam.hitboxHeight > .8f) throw new AssertionError("Slam must be low enough to jump over");

            // Infiltrator: fragile, fast; light strike evaded during sidestep, heavy strike lands.
            Enemy fox = EnemyFactory.createInfiltrator(fight.getWorld(), new Vector2(12f, 2.5f));
            if (fox.getMaxHealth() > 2f) throw new AssertionError("Infiltrator must be fragile");
            if (fox.getVelocity() <= EnemyFactory.createDefaultEnemy(fight.getWorld(), new Vector2(30f, 2.5f)).getVelocity())
                throw new AssertionError("Infiltrator must be faster than the raider");

            // Overseer: casts a buff on a nearby ally, an interrupting hit cancels it and starts a cooldown.
            Enemy overseer = EnemyFactory.createOverseer(fight.getWorld(), new Vector2(14f, 2.5f));
            Enemy ally = EnemyFactory.createDefaultEnemy(fight.getWorld(), new Vector2(12f, 2.5f));
            fight.addEnemy(overseer);
            fight.addEnemy(ally);
            overseer.updateSpecialActions(hero, fight.getAliveEnemies(), .01f);
            if (!overseer.isCasting()) throw new AssertionError("Overseer did not start a buff cast");
            overseer.takeMeleeHit(new CombatHit(CombatHit.Type.MELEE_LIGHT, 1f, hero, overseer, 1f, 1f, 1f, .14f, true));
            if (overseer.isCasting()) throw new AssertionError("Hit did not interrupt the overseer cast");
            overseer.updateSpecialActions(hero, fight.getAliveEnemies(), .01f);
            if (overseer.isCasting()) throw new AssertionError("Overseer re-cast during its interrupted cooldown");
            if (ally.isBuffed()) throw new AssertionError("Interrupted cast must not buff");
            ally.receiveBuff(Enemy.BUFF_DURATION);
            if (!ally.isBuffed()) throw new AssertionError("Buff was not applied");
        } finally {
            fight.dispose();
        }
    }

    private void checkHeroBlockRules() {
        GameWorld fight = new GameWorld();
        try {
            Hero hero = fight.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.setFacingLeft(false);
            hero.setBlockHeld(true);
            if (!hero.isBlocking()) throw new AssertionError("Hero could not raise block while idle");
            Enemy foe = EnemyFactory.createDefaultEnemy(fight.getWorld(), new Vector2(11f, 2.5f));
            float hp = hero.getHealth();
            hero.receiveHit(new CombatHit(CombatHit.Type.MELEE_LIGHT, 1f, 11f, 1f, 3f, foe));
            closeTo(hero.getHealth(), hp, "blocked light melee must deal no damage");
            hero.receiveHit(new CombatHit(CombatHit.Type.PROJECTILE, .5f, 11f, 0f, 0f, foe));
            closeTo(hero.getHealth(), hp - .5f, "arrows must pass through the hero block");
        } finally {
            fight.dispose();
        }
    }

    private void checkRaiderBlockAndDash() {
        GameWorld fight = new GameWorld();
        try {
            Hero hero = fight.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            // Light hit on a guarding raider: zero damage, no stagger. Heavy: 35% damage, guard broken.
            Enemy raider = EnemyFactory.createDefaultEnemy(fight.getWorld(), new Vector2(11f, 2.5f));
            raider.setCanSeeHero(true);
            raider.setFacingLeft(true);
            raider.forceBlockForTest();
            float hp = raider.getHealth();
            CombatHit blockedHit = new CombatHit(CombatHit.Type.MELEE_LIGHT, 1f, hero, raider, 1f, 1f, 3f, .14f, true);
            raider.takeMeleeHit(blockedHit);
            hero.emitCombatHit(blockedHit);
            if (fight.getCombatEvents().size == 0 ||
                fight.getCombatEvents().peek().kind != GameWorld.CombatEvent.Kind.BLOCK_METAL)
                throw new AssertionError("Blocked hit did not produce a block-specific feedback event");
            closeTo(raider.getHealth(), hp, "light hit on raider block must deal no damage");
            if (raider.isStaggered()) throw new AssertionError("Blocked light hit staggered the raider");
            raider.takeMeleeHit(new CombatHit(CombatHit.Type.MELEE_HEAVY, 2f, 10f, 2f, 5f, hero));
            closeTo(raider.getHealth(), hp - .7f, "heavy hit on raider block must deal 35% damage");
            if (raider.isBlocking()) throw new AssertionError("Heavy hit did not break the raider block");
            // Attack from behind ignores the guard.
            Enemy back = EnemyFactory.createDefaultEnemy(fight.getWorld(), new Vector2(15f, 2.5f));
            back.setFacingLeft(false);
            back.forceBlockForTest();
            float backHp = back.getHealth();
            back.takeMeleeHit(new CombatHit(CombatHit.Type.MELEE_LIGHT, 1f, 14f, 1f, 3f, hero));
            closeTo(back.getHealth(), backHp - 1f, "attack from behind must ignore the raider block");
            if (raider.getCombatPhase() == com.cursedarchie.platformer.combat.CombatPhase.DEAD)
                throw new AssertionError("Raider phase wrong");
        } finally {
            fight.dispose();
        }
    }

    private void checkScavengerActionPriority() {
        GameWorld fight = new GameWorld();
        try {
            Hero hero = fight.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);

            Enemy shooter = EnemyFactory.createScavenger(fight.getWorld(), new Vector2(15f, 2.5f));
            shooter.setIntent(Enemy.Intent.IDLE);
            shooter.updateSpecialActions(fight, hero, fight.getAliveEnemies(), .81f);
            shooter.setCanSeeHero(true);
            if (!shooter.beginRangedAttack(RangedAttackConfig.NORMAL, hero))
                throw new AssertionError("Scavenger could not begin a valid normal shot");
            Vector2 firstAim = new Vector2(shooter.getRangedAim());
            hero.getBody().setTransform(20f, 2.5f, 0f);
            shooter.updateSpecialActions(fight, hero, fight.getAliveEnemies(), .05f);
            if (shooter.getRangedAim().x <= 0f || shooter.getRangedAim().epsilonEquals(firstAim, .0001f))
                throw new AssertionError("Aim did not follow the hero during the main preparation phase");
            for (int i = 0; i < 60 && shooter.getRangedWindupProgress() < Enemy.RANGED_AIM_LOCK_AT; i++)
                shooter.updateSpecialActions(fight, hero, fight.getAliveEnemies(), .02f);
            Vector2 lockedAim = new Vector2(shooter.getRangedAim());
            Vector2 lockedTarget = new Vector2(shooter.getRangedTargetPosition());
            hero.getBody().setTransform(5f, 2.5f, 0f);
            shooter.updateSpecialActions(fight, hero, fight.getAliveEnemies(),
                RangedAttackConfig.NORMAL.startup + .01f);
            if (fight.getArrows().size != 1)
                throw new AssertionError("Normal shot did not create exactly one arrow");
            GameWorld.Arrow lockedArrow = fight.getArrows().first();
            if (!lockedArrow.direction.epsilonEquals(lockedAim, .0001f) || lockedArrow.direction.x <= 0f)
                throw new AssertionError("Moving after the lock redirected the committed shot");
            if (!shooter.getRangedTargetPosition().epsilonEquals(lockedTarget, .0001f))
                throw new AssertionError("Moving after the lock changed the committed target position");
            closeTo(lockedArrow.damage, .5f, "normal arrow damage");
            closeTo(lockedArrow.speed, 15f, "normal arrow speed");

            fight.getArrows().clear();
            fight.fireArrows(shooter, new Vector2(-1f, 0f), RangedAttackConfig.CHARGED);
            if (fight.getArrows().size != 1 ||
                fight.getArrows().first().type != RangedAttackConfig.Type.CHARGED)
                throw new AssertionError("Charged shot did not create one charged arrow");
            closeTo(fight.getArrows().first().damage, 1f, "charged arrow damage");
            closeTo(fight.getArrows().first().speed, 18f, "charged arrow speed");

            fight.getArrows().clear();
            fight.fireArrows(shooter, new Vector2(-1f, 0f), RangedAttackConfig.VOLLEY);
            if (fight.getArrows().size != 3)
                throw new AssertionError("Volley did not create exactly three arrows");
            if (!(fight.getArrows().get(0).direction.y > fight.getArrows().get(1).direction.y &&
                fight.getArrows().get(1).direction.y > fight.getArrows().get(2).direction.y))
                throw new AssertionError("Volley arrows do not have vertical angular spread");
            fight.getArrows().clear();

            Enemy closeShooter = EnemyFactory.createScavenger(fight.getWorld(),
                new Vector2(12f, 2.5f));
            closeShooter.setIntent(Enemy.Intent.IDLE);
            closeShooter.updateSpecialActions(fight, hero, fight.getAliveEnemies(), .81f);
            hero.getBody().setTransform(10.5f, 2.5f, 0f);
            closeShooter.setCanSeeHero(true);
            if (closeShooter.beginRangedAttack(RangedAttackConfig.NORMAL, hero))
                throw new AssertionError("Scavenger started a ranged shot at melee distance");
            closeShooter.requestAttack();
            closeShooter.processCombatRequests();
            if (!closeShooter.isAttacking() || closeShooter.isRangedWindingUp())
                throw new AssertionError("Close scavenger did not choose an exclusive melee attack");

            Enemy dodger = EnemyFactory.createScavenger(fight.getWorld(), new Vector2(15f, 2.5f));
            dodger.setIntent(Enemy.Intent.IDLE);
            float health = dodger.getHealth();
            boolean lightApplied = dodger.takeMeleeHit(new CombatHit(CombatHit.Type.MELEE_LIGHT,
                1f, 14f, 1f, 2f, hero));
            if (lightApplied || dodger.getHealth() != health || !dodger.isDodging())
                throw new AssertionError("Safe light-hit dodge did not avoid damage");
            dodger.setCanSeeHero(true);
            dodger.requestAttack();
            dodger.processCombatRequests();
            if (dodger.beginRangedAttack(RangedAttackConfig.NORMAL, hero) || dodger.isAttacking())
                throw new AssertionError("Dodge allowed a simultaneous ranged or melee attack");

            Enemy heavyTarget = EnemyFactory.createScavenger(fight.getWorld(),
                new Vector2(17f, 2.5f));
            float heavyHealth = heavyTarget.getHealth();
            if (!heavyTarget.takeMeleeHit(new CombatHit(CombatHit.Type.MELEE_HEAVY,
                1f, 16f, 2f, 3f, hero)) || heavyTarget.getHealth() >= heavyHealth ||
                heavyTarget.isDodging())
                throw new AssertionError("Heavy hit was incorrectly treated as a light dodge");

            Enemy blocked = EnemyFactory.createScavenger(fight.getWorld(), new Vector2(22f, 2.5f));
            blocked.setIntent(Enemy.Intent.IDLE);
            com.badlogic.gdx.physics.box2d.BodyDef wallDef = new com.badlogic.gdx.physics.box2d.BodyDef();
            wallDef.type = com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody;
            wallDef.position.set(23f, 2.5f);
            com.badlogic.gdx.physics.box2d.Body wall = fight.getWorld().createBody(wallDef);
            wall.setUserData("level");
            com.badlogic.gdx.physics.box2d.PolygonShape wallShape =
                new com.badlogic.gdx.physics.box2d.PolygonShape();
            wallShape.setAsBox(.2f, 1f);
            wall.createFixture(wallShape, 0f);
            wallShape.dispose();
            if (blocked.hasClearRetreatPath(1f) || blocked.beginRetreat(1f))
                throw new AssertionError("Scavenger retreated into a wall");
            hero.getBody().setTransform(25f, 2.5f, 0f);
            blocked.updateSpecialActions(fight, hero, fight.getAliveEnemies(), .81f);
            blocked.setCanSeeHero(true);
            if (blocked.beginRangedAttack(RangedAttackConfig.NORMAL, hero))
                throw new AssertionError("Scavenger shot through blocked line of sight");
        } finally {
            fight.dispose();
        }
    }

    private void checkEnemyLungeRecovery() {
        GameWorld fight = combatFixture();
        try {
            Enemy enemy = EnemyFactory.createDefaultEnemy(fight.getWorld(), new Vector2(18f, 2.5f));
            enemy.setIntent(Enemy.Intent.IDLE);
            enemy.requestAttack();
            enemy.processCombatRequests();
            if (!enemy.isAttacking() || ((AttackState) enemy.getCombatStateMachine()
                .getCurrentState()).isLunge())
                throw new AssertionError("First regular enemy attack was not quick");
            for (int i = 0; i < 80; i++) enemy.update(GameWorld.FIXED_STEP);
            enemy.requestAttack();
            enemy.processCombatRequests();
            if (!enemy.isAttacking() || !((AttackState) enemy.getCombatStateMachine()
                .getCurrentState()).isLunge())
                throw new AssertionError("Regular enemy did not alternate to a lunge");
            for (int i = 0; i < 56; i++) enemy.update(GameWorld.FIXED_STEP);
            enemy.requestAttack();
            enemy.processCombatRequests();
            if (enemy.isAttacking())
                throw new AssertionError("Missed lunge had no punishable recovery");
        } finally {
            fight.dispose();
        }
    }

    private void checkLockedAttackDirection() {
        for (int variant = 0; variant < 2; variant++) {
            GameWorld fight = combatFixture();
            try {
                Enemy target = null;
                for (Enemy enemy : fight.getEnemies()) {
                    if (enemy.getSize() < 2f) target = enemy;
                    else enemy.setAlive(false);
                }
                target.setActive(false);
                target.setMaxHealth(20f);
                target.setHealth(20f);
                Hero hero = fight.getHero();
                hero.getBody().setTransform(20f, 2.5f, 0f);
                hero.getBody().setLinearVelocity(0f, 0f);
                hero.setFacingLeft(false);
                target.getBody().setTransform(21.2f, 2.5f, 0f);
                fight.update(GameWorld.FIXED_STEP);
                hero.getCombatStateMachine().changeState(
                    new com.cursedarchie.platformer.actors.hero.states.combat.AttackState(variant == 1 ? 2 : 1));
                // Every strike locks its direction: retreat input must not turn the swing.
                fight.getHeroController().keyDown(Input.Keys.LEFT);
                int beforeActive = variant == 1 ? 4 : 7;
                for (int i = 0; i < beforeActive; i++) fight.update(GameWorld.FIXED_STEP);
                target.getBody().setTransform(hero.getPosition().x + 1.2f,
                    hero.getPosition().y, 0f);
                for (int i = beforeActive; i < 20; i++) fight.update(GameWorld.FIXED_STEP);
                if (target.getHealth() >= 20f || hero.isFacingLeft())
                    throw new AssertionError("Attack turned with retreat input, variant=" + variant);
                for (int i = 0; i < 22; i++) fight.update(GameWorld.FIXED_STEP);
                if (!hero.isFacingLeft())
                    throw new AssertionError("Hero did not face retreat direction after attack");
                hero.getBody().setTransform(20f, 2.5f, 0f);
                hero.setFacingLeft(false);
                target.getBody().setTransform(25f, 2.5f, 0f);
                target.setHealth(20f);
                hero.getCombatStateMachine().changeState(
                    new com.cursedarchie.platformer.actors.hero.states.combat.AttackState(variant == 1 ? 2 : 1));
                for (int i = 0; i < 20; i++)
                    fight.update(GameWorld.FIXED_STEP);
                if (target.getHealth() != 20f)
                    throw new AssertionError("Distant target was hit, variant=" + variant);
            } finally {
                fight.dispose();
            }
        }
    }

    private static GameWorld.DamageIndicator indicatorFor(GameWorld world, Actor target) {
        for (GameWorld.DamageIndicator indicator : world.getDamageIndicators())
            if (indicator.target == target) return indicator;
        return null;
    }

    private static int inventorySlot(GameWorld world, String id) {
        for (int i = 0; i < com.cursedarchie.platformer.run.Inventory.TOTAL_SLOTS; i++)
            if (id.equals(world.getInventory().itemId(i))) return i;
        throw new AssertionError("Missing inventory item: " + id);
    }

    private void checkAlleyPortalsAndChoices() {
        GameWorld route = new GameWorld(412L);
        try {
            route.changeLocation("ruins", 0);
            com.cursedarchie.platformer.levels.wrappers.InteractionData entrance = null;
            for (com.cursedarchie.platformer.levels.RoomInstance room : route.getLocation().getRooms())
                for (com.cursedarchie.platformer.levels.wrappers.InteractionData interaction : room.getInteractions())
                    if ("laundry_alley_entrance".equals(interaction.id)) {
                        route.changeLocation("ruins", room.getRoomId());
                        entrance = interaction;
                    }
            if (entrance == null) throw new AssertionError("Generated street has no alley entrance");
            int streetRoom = route.getCurrentRoom().getRoomId();
            float streetX = route.getCurrentRoom().getWorldX() + entrance.position.x;
            route.getHero().getBody().setTransform(streetX,
                route.getCurrentRoom().getWorldY() + entrance.position.y, 0f);
            if (!route.enterPortal(entrance) || !"laundry_alley".equals(route.getLocation().getLocation()))
                throw new AssertionError("Alley portal did not enter its authored room");
            int lastRoom = route.getLocation().getRooms().size() - 1;
            if (lastRoom < 3) throw new AssertionError("Generated alley is too short");
            for (int roomId = 1; roomId <= lastRoom; roomId++) {
                com.cursedarchie.platformer.levels.RoomInstance next = route.getLocation().getRoom(roomId);
                route.getHero().getBody().setTransform(next.getWorldX() + 2f, next.getWorldY() + 3f, 0f);
                route.update(GameWorld.FIXED_STEP);
                if (route.getCurrentRoom().getRoomId() != roomId)
                    throw new AssertionError("Alley boundary did not advance to room " + roomId);
                // Cellars lie below the street's y = 0; only falling out of the location may kill.
                if (!route.getHero().isAlive())
                    throw new AssertionError("Hero died standing in alley room " + roomId);
            }
            route.getHero().getBody().setTransform(3.5f, 3f, 0f);
            route.update(GameWorld.FIXED_STEP);
            if (route.getCurrentRoom().getRoomId() != 0)
                throw new AssertionError("Alley return path did not reach its first room");
            com.cursedarchie.platformer.levels.wrappers.InteractionData exit =
                route.getCurrentRoom().getInteractions().first();
            route.getHero().getBody().setTransform(exit.position, 0f);
            if (!route.enterPortal(exit) || !"ruins".equals(route.getLocation().getLocation()) ||
                route.getCurrentRoom().getRoomId() != streetRoom ||
                Math.abs(route.getHero().getPosition().x - streetX) > .01f)
                throw new AssertionError("Alley exit did not return to the same street doorway: " +
                    route.getLocation().getLocation() + " room " + route.getCurrentRoom().getRoomId() + " (expected " +
                    streetRoom + "), x " + route.getHero().getPosition().x + " (expected " + streetX + ")");

            // The far exit is its own room and comes out further along the street, not at the entrance door.
            route.getHero().getBody().setTransform(streetX, route.getCurrentRoom().getWorldY() + entrance.position.y, 0f);
            if (!route.enterPortal(entrance)) throw new AssertionError("Alley could not be re-entered");
            com.cursedarchie.platformer.levels.RoomInstance exitRoom = null;
            com.cursedarchie.platformer.levels.wrappers.InteractionData farExit = null;
            for (com.cursedarchie.platformer.levels.RoomInstance room : route.getLocation().getRooms())
                for (com.cursedarchie.platformer.levels.wrappers.InteractionData interaction : room.getInteractions())
                    if ("main_street_exit".equals(interaction.id)) { exitRoom = room; farExit = interaction; }
            if (farExit == null || exitRoom.getRoomId() == 0) throw new AssertionError("Alley has no separate exit room");
            route.getHero().getBody().setTransform(exitRoom.getWorldX() + farExit.position.x,
                exitRoom.getWorldY() + farExit.position.y, 0f);
            route.update(GameWorld.FIXED_STEP);
            if (!route.enterPortal(farExit) || !"ruins".equals(route.getLocation().getLocation()) ||
                route.getCurrentRoom().getRoomId() == streetRoom ||
                route.getCurrentRoom().namedSpawn("alley_exit_return") == null)
                throw new AssertionError("Far alley exit did not come out at its own street spawn");
        } finally { route.dispose(); }
    }

    private void checkSliceItemsAndGuard() {
        GameWorld route = new GameWorld(424L);
        try {
            Hero hero = route.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.setFacingLeft(true);
            hero.setBlockHeld(true);
            hero.update(Hero.PERFECT_BLOCK_WINDOW + .05f);   // a late block is an ordinary block, not a perfect one
            float health = hero.getHealth();
            hero.receiveHit(new CombatHit(CombatHit.Type.MELEE_LIGHT, 1f, 9f, 1f, 0f, null));
            closeTo(hero.getHealth(), health, "front light hit passed through block");
            closeTo(hero.getBlockStability(), 2f, "block did not lose stability");
            hero.receiveHit(new CombatHit(CombatHit.Type.MELEE_HEAVY, 1f, 9f, 2f, 0f, null));
            closeTo(hero.getHealth(), health - .5f, "heavy block did not leak partial damage");
            if (!hero.isBlockBroken()) throw new AssertionError("Depleted block did not break");
            hero.setBlockHeld(false);
            for (int i = 0; i < 60; i++) route.update(GameWorld.FIXED_STEP);
            float beforeArrow = hero.getHealth();
            hero.receiveHit(new CombatHit(CombatHit.Type.PROJECTILE, .5f, 9f, 0f, 0f, null));
            closeTo(hero.getHealth(), beforeArrow - .5f, "Projectile was blocked");
            route.getInventory().add(ItemCatalog.RED_BRACER);
            closeTo(hero.getMaxBlockStability(), 3.75f, "damaged bracer stability bonus");
            route.getInventory().add(ItemCatalog.IMPACT_CORE);
            if (!route.combineItems(inventorySlot(route, ItemCatalog.RED_BRACER),
                inventorySlot(route, ItemCatalog.IMPACT_CORE)) ||
                route.getInventory().count(ItemCatalog.WEIGHTED_BRACER) != 1 ||
                route.getInventory().count(ItemCatalog.IMPACT_CORE) != 0)
                throw new AssertionError("Weighted bracer recipe did not consume one core");
            route.getInventory().add(ItemCatalog.SEWING_KIT);
            route.getInventory().add(ItemCatalog.SEWING_KIT);
            route.getInventory().add(ItemCatalog.SEWING_KIT);
            route.getInventory().add(ItemCatalog.FIELD_BANDAGE);
            if (!route.combineItems(inventorySlot(route, ItemCatalog.SEWING_KIT),
                inventorySlot(route, ItemCatalog.FIELD_BANDAGE)) ||
                route.getInventory().count(ItemCatalog.SEWING_KIT) != 2 ||
                route.getInventory().count(ItemCatalog.REINFORCED_BANDAGE) != 1)
                throw new AssertionError("Sewing recipe lost charges or output");
            if (!route.moveInventorySlot(inventorySlot(route, ItemCatalog.REINFORCED_BANDAGE), 7))
                throw new AssertionError("Reinforced bandage could not enter pocket");
            hero.setHealth(1f);
            if (!route.usePocketItem(7) || hero.getHealth() != hero.getMaxHealth())
                throw new AssertionError("Reinforced bandage did not heal two up to max");
        } finally { route.dispose(); }
    }

    private void checkArrowCover() {
        GameWorld route = new GameWorld(426L);
        try {
            Hero hero = route.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            Enemy shooter = EnemyFactory.createScavenger(route.getWorld(), new Vector2(15f, 2.5f));
            shooter.setActive(false);
            float health = hero.getHealth();
            route.fireArrows(shooter, new Vector2(-1f, 0f), RangedAttackConfig.NORMAL);
            boolean heroImpact = false;
            for (int i = 0; i < 35; i++) {
                route.update(GameWorld.FIXED_STEP);
                for (GameWorld.ArrowImpact impact : route.getArrowImpacts()) if (impact.hero) heroImpact = true;
            }
            if (hero.getHealth() >= health) throw new AssertionError("Flying arrow missed an exposed hero");
            if (route.getArrows().size != 0)
                throw new AssertionError("Arrow was not removed after hitting the hero");
            if (!heroImpact) throw new AssertionError("Arrow hit on the hero left no impact effect");
            hero.setHealth(health);
            com.badlogic.gdx.physics.box2d.BodyDef wallDef = new com.badlogic.gdx.physics.box2d.BodyDef();
            wallDef.type = com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody;
            wallDef.position.set(12.5f, 2.5f);
            com.badlogic.gdx.physics.box2d.Body wall = route.getWorld().createBody(wallDef);
            wall.setUserData("level");
            com.badlogic.gdx.physics.box2d.PolygonShape shape = new com.badlogic.gdx.physics.box2d.PolygonShape();
            shape.setAsBox(.15f, 1f);
            wall.createFixture(shape, 0f);
            shape.dispose();
            route.fireArrows(shooter, new Vector2(-1f, 0f), RangedAttackConfig.NORMAL);
            boolean wallImpact = false;
            for (int i = 0; i < 35; i++) {
                route.update(GameWorld.FIXED_STEP);
                for (GameWorld.ArrowImpact impact : route.getArrowImpacts()) if (!impact.hero) wallImpact = true;
            }
            closeTo(hero.getHealth(), health, "Arrow passed through cover");
            if (route.getArrows().size != 0)
                throw new AssertionError("Arrow was not removed after hitting a wall");
            if (!wallImpact) throw new AssertionError("Arrow hit on a wall left no impact effect");
        } finally { route.dispose(); }
    }

    /**
     * The archer's actions must stay distinguishable: a phased shot with a locked aim, distinct hit
     * feedback, a retreat that is telegraphed and skids rather than teleports, an evasion that deals no
     * damage, and a melee strike that clearly breaks the ranged stance.
     */
    private void checkArcherActionReadability() {
        // Standard shot: DRAW -> LOCKED -> CUE -> RECOVERY, with the aim frozen from the lock onward.
        GameWorld shot = new GameWorld();
        try {
            Hero hero = shot.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            Enemy archer = EnemyFactory.createScavenger(shot.getWorld(), new Vector2(15f, 2.5f));
            archer.setBrain(new SimpleEnemyBrain(shot));
            shot.addEnemy(archer);
            int last = 0;
            boolean[] seen = new boolean[5];
            Vector2 lockedAim = null;
            Vector2 cueAim = null;
            for (int i = 0; i < 300; i++) {
                shot.update(GameWorld.FIXED_STEP);
                Enemy.RangedPhase phase = archer.getRangedPhase();
                int rank = phase.ordinal();
                if (rank == 0) {
                    if (last == 4) break;
                    continue;
                }
                if (rank < last) throw new AssertionError("Archer shot phases ran out of order");
                last = rank;
                seen[rank] = true;
                if (phase == Enemy.RangedPhase.LOCKED && lockedAim == null) {
                    lockedAim = new Vector2(archer.getRangedAim());
                    hero.getBody().setTransform(10f, 3.3f, 0f);
                }
                if (phase == Enemy.RangedPhase.CUE && cueAim == null) cueAim = new Vector2(archer.getRangedAim());
            }
            if (!seen[1] || !seen[2] || !seen[3] || !seen[4] || lockedAim == null || cueAim == null)
                throw new AssertionError("Archer shot skipped a phase (draw, lock, release cue or recovery)");
            closeTo(cueAim.x, lockedAim.x, "Aim kept moving after it was locked");
            closeTo(cueAim.y, lockedAim.y, "Aim kept moving after it was locked");
            if (archer.getRangedShotMode() != 0) throw new AssertionError("First archer shot was not a standard shot");
            Enemy.Cue firstCue = archer.pollCue();
            while (firstCue == Enemy.Cue.ALERT) firstCue = archer.pollCue();
            if (firstCue != Enemy.Cue.BOW_DRAW_STANDARD)
                throw new AssertionError("Standard draw did not start with its own sound cue");
            boolean shotCue = false;
            for (Enemy.Cue cue = archer.pollCue(); cue != null; cue = archer.pollCue())
                if (cue == Enemy.Cue.SHOT_STANDARD) shotCue = true;
            if (!shotCue) throw new AssertionError("Standard release did not queue its sound cue");
        } finally { shot.dispose(); }

        // An arrow that lands: impact effect once, a hero reaction, and real damage.
        GameWorld hit = new GameWorld();
        try {
            Hero hero = hit.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            Enemy archer = EnemyFactory.createScavenger(hit.getWorld(), new Vector2(15f, 2.5f));
            archer.setBrain(new SimpleEnemyBrain(hit));
            hit.addEnemy(archer);
            float health = hero.getHealth();
            boolean impact = false;
            boolean reaction = false;
            for (int i = 0; i < 160 && !(impact && reaction); i++) {
                hit.update(GameWorld.FIXED_STEP);
                for (GameWorld.ArrowImpact effect : hit.getArrowImpacts()) {
                    if (!effect.hero) continue;
                    impact = true;
                    if (!effect.consumeSoundCue() || effect.consumeSoundCue())
                        throw new AssertionError("Impact sound cue must play exactly once");
                }
                if (hit.getHeroArrowReactionProgress() >= 0f) reaction = true;
            }
            if (!impact) throw new AssertionError("Arrow hit left no impact effect");
            if (!reaction) throw new AssertionError("Hero did not react to an arrow hit");
            if (hero.getHealth() >= health) throw new AssertionError("Arrow hit dealt no damage");
        } finally { hit.dispose(); }

        // Retreat (timers owned by the gameplay rules): lean-back tell, dash, settle back into stance; no teleport.
        GameWorld retreat = new GameWorld();
        try {
            Hero hero = retreat.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            Enemy archer = EnemyFactory.createScavenger(retreat.getWorld(), new Vector2(12.2f, 2.5f));
            archer.setBrain(new SimpleEnemyBrain(retreat));
            retreat.addEnemy(archer);
            boolean telegraph = false;
            boolean dash = false;
            boolean recover = false;
            float lastX = archer.getPosition().x;
            float biggestStep = 0f;
            for (int i = 0; i < 300; i++) {
                retreat.update(GameWorld.FIXED_STEP);
                Enemy.RetreatPhase phase = archer.getRetreatPhase();
                if (phase == Enemy.RetreatPhase.TELEGRAPH) telegraph = true;
                if (phase == Enemy.RetreatPhase.DASH) dash = true;
                if (phase == Enemy.RetreatPhase.RECOVER) recover = true;
                biggestStep = Math.max(biggestStep, Math.abs(archer.getPosition().x - lastX));
                lastX = archer.getPosition().x;
                if (recover && phase == Enemy.RetreatPhase.NONE) break;
            }
            if (!telegraph || !dash || !recover)
                throw new AssertionError("Retreat skipped its telegraph, dash or recovery");
            if (biggestStep > .35f) throw new AssertionError("Archer retreat teleported instead of dashing");
            if (archer.getPosition().x < 12.7f) throw new AssertionError("Retreat did not create distance");
            if (!archer.isFacingLeft()) throw new AssertionError("Archer did not return to face the hero");
            boolean dashCue = false;
            for (Enemy.Cue cue = archer.pollCue(); cue != null; cue = archer.pollCue())
                if (cue == Enemy.Cue.RETREAT_DASH) dashCue = true;
            if (!dashCue) throw new AssertionError("Retreat dash did not queue its sound cue");
        } finally { retreat.dispose(); }

        // Evasion after a light hit: no damage, an afterimage trail and a visible dodge effect.
        GameWorld dodge = new GameWorld();
        try {
            Hero hero = dodge.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            Enemy archer = EnemyFactory.createScavenger(dodge.getWorld(), new Vector2(11.2f, 2.5f));
            dodge.addEnemy(archer);
            archer.setBrain(null);
            archer.setIntent(Enemy.Intent.IDLE);
            float health = archer.getHealth();
            float startX = archer.getPosition().x;
            archer.takeMeleeHit(new CombatHit(CombatHit.Type.MELEE_LIGHT, 1f, hero.getPosition().x, 1f, 0f, null));
            closeTo(archer.getHealth(), health, "Dodged light hit still dealt damage");
            if (archer.getDodgeFlashRemaining() <= 0f || archer.getRetreatTrailRemaining() <= 0f ||
                !archer.isRetreatTrailDodge())
                throw new AssertionError("Dodge showed no evasion effect");
            if (archer.pollCue() != Enemy.Cue.DODGE) throw new AssertionError("Dodge did not queue its sound cue");
            for (int i = 0; i < 12; i++) dodge.update(GameWorld.FIXED_STEP);
            if (archer.getPosition().x <= startX + .3f) throw new AssertionError("Dodge did not move the archer away");
        } finally { dodge.dispose(); }

        // Melee: rushing an archer that is drawing ends the ranged stance and uses its own cues.
        GameWorld melee = new GameWorld();
        try {
            Hero hero = melee.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            Enemy archer = EnemyFactory.createScavenger(melee.getWorld(), new Vector2(15f, 2.5f));
            archer.setBrain(new SimpleEnemyBrain(melee));
            melee.addEnemy(archer);
            for (int i = 0; i < 120 && !archer.isRangedWindingUp(); i++) melee.update(GameWorld.FIXED_STEP);
            if (!archer.isRangedWindingUp()) throw new AssertionError("Archer never started drawing");
            hero.getBody().setTransform(14.2f, 2.5f, 0f);
            hero.setHealth(hero.getMaxHealth());
            for (int i = 0; i < 200 && !archer.isAttacking(); i++) melee.update(GameWorld.FIXED_STEP);
            if (!archer.isAttacking()) throw new AssertionError("Archer did not switch to a melee strike");
            if (archer.isRangedRecovering() || archer.getRangedPhase() != Enemy.RangedPhase.NONE)
                throw new AssertionError("Melee strike still showed the ranged stance");
            for (int i = 0; i < 20; i++) melee.update(GameWorld.FIXED_STEP);
            boolean windup = false;
            boolean strike = false;
            for (Enemy.Cue cue = archer.pollCue(); cue != null; cue = archer.pollCue()) {
                if (cue == Enemy.Cue.MELEE_WINDUP) windup = true;
                if (cue == Enemy.Cue.MELEE_STRIKE) strike = true;
            }
            if (!windup || !strike) throw new AssertionError("Melee strike lacked its own sound cues");
        } finally { melee.dispose(); }
    }

    private static void closeTo(float actual, float expected, String failure) {
        if (Math.abs(actual - expected) > 0.0001f)
            throw new AssertionError(failure + ": expected " + expected + ", got " + actual);
    }

    @Override public void dispose() {
        if (world != null) world.dispose();
    }
}
