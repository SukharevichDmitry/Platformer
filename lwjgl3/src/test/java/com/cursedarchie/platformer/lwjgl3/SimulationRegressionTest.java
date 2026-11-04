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
        checkEnemyHeadBounce();
        checkRenderRateConsistency();
        checkUnifiedAttackModel();
        checkHeroAttackTimingAndDirection();
        checkCombatExtensions();
        checkLockedAttackDirection();
        checkSpecialEnemyRoles();
        checkScavengerActionPriority();
        checkRaiderBlockAndDash();
        checkHeroBlockAndHeavyRules();
        checkNewEnemyRoles();
        checkGroupAttackSlots();
        checkRollCounterAndJumpAttack();
        checkArrowArcPhysics();
        checkAttackRhythm();
        checkBreakthroughDash();
        checkArcherMemoryAndMirroredTurn();
        checkArrowCeilingAndSightLoss();
        checkMeleeReachMatchesHitbox();
        checkRapidTurnAndAwareness();
        checkEnemyLungeRecovery();
        checkEnemyAttackAndDamageCooldown();
        checkLocationTitleTiming();
        checkInteractionInput();
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
                interactionWorld.getHero().meleeDamageMultiplier(true, 0) <= 1.3f)
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
                    resumed.getHero().meleeDamageMultiplier(true, 0) <= 1.3f)
                    throw new AssertionError("Relic synergy did not survive a checkpoint restore");
            } finally {
                resumed.dispose();
            }
        } finally {
            interactionWorld.dispose();
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

            for (int i = 0; i < 100 && !attacker.isCanAttackHero(); i++)
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

    private void checkEnemyHeadBounce() {
        GameWorld headWorld = combatFixture();
        try {
            Enemy platformEnemy = null;
            for (Enemy enemy : headWorld.getEnemies()) {
                if (enemy.getSize() < 2f) platformEnemy = enemy;
                else enemy.setAlive(false);
            }
            if (platformEnemy == null) throw new AssertionError("No enemy for head-jump check");
            platformEnemy.setActive(false);
            platformEnemy.getBody().setTransform(10f, 2.5f, 0f);
            platformEnemy.getBody().setLinearVelocity(0f, 0f);
            Hero hero = headWorld.getHero();
            hero.getBody().setTransform(10f, 5.2f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            boolean landedOnEnemy = false;
            for (int i = 0; i < 40; i++) {
                headWorld.update(GameWorld.FIXED_STEP);
                if (hero.isGrounded() && hero.getFootContacts() == 0 &&
                    hero.getPosition().y > platformEnemy.getPosition().y + 0.8f) {
                    landedOnEnemy = true;
                    break;
                }
            }
            if (landedOnEnemy) throw new AssertionError("Enemy head must not act as a stable platform");
            // Landing on a head bounces the hero off sideways with a hop and staggers the enemy briefly.
            platformEnemy.getBody().setTransform(10f, 2.5f, 0f);
            platformEnemy.getBody().setLinearVelocity(0f, 0f);
            hero.getBody().setTransform(10.05f, 5.2f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            float enemyHealthBefore = platformEnemy.getHealth();
            boolean bounced = false;
            boolean enemyStaggered = false;
            for (int i = 0; i < 60 && !bounced; i++) {
                headWorld.update(GameWorld.FIXED_STEP);
                if (platformEnemy.isStaggered()) enemyStaggered = true;
                if (Math.abs(hero.getBody().getLinearVelocity().x) > 2f && hero.getBody().getLinearVelocity().y > 1f)
                    bounced = true;
            }
            if (!bounced) throw new AssertionError("Landing on an enemy head did not bounce the hero off");
            if (!enemyStaggered && !platformEnemy.isStaggered())
                throw new AssertionError("Enemy showed no 'hit from above' reaction");
            closeTo(platformEnemy.getHealth(), enemyHealthBefore, "A plain landing must not damage the enemy");
            if (hero.getInvulnerabilityRemaining() > 0f)
                throw new AssertionError("Landing on an enemy granted the hero invulnerability");
            for (int i = 0; i < 30; i++) headWorld.update(GameWorld.FIXED_STEP);
            if (hero.isGrounded() && hero.getFootContacts() == 0)
                throw new AssertionError("Hero is still being supported by an enemy head");
            platformEnemy.getBody().setTransform(14f, 6f, 0f);
            platformEnemy.getBody().setLinearVelocity(0f, 0f);
            hero.getBody().setTransform(13f, 6f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            headWorld.update(GameWorld.FIXED_STEP);
            if (hero.isGrounded() || hero.getFootContacts() != 0)
                throw new AssertionError("Enemy side contact incorrectly granted a jump");
        } finally {
            headWorld.dispose();
        }
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
            for (int i = 0; i < 6; i++) combatWorld.update(GameWorld.FIXED_STEP);
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
        AttackDefinition first = sampleHero.attackDefinition(false, 1);
        closeTo(first.startupDuration, .12f, "first attack startup setting");
        closeTo(first.activeDuration, .12f, "first attack active setting");
        closeTo(first.recoveryDuration, .12f, "first attack recovery setting");
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
        if (runtime.phase() != AttackRuntime.Phase.RECOVERY || !runtime.acceptsNextInput())
            throw new AssertionError("Attack recovery or combo input window is incorrect");

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
            // The follow-up is accepted only during the recovery of strike one.
            for (int i = 0; i < 5; i++) fight.update(GameWorld.FIXED_STEP);
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
            closeTo(hero.meleeDamageMultiplier(true, 0), 1.55f,
                "two relics did not synergize on a heavy strike");
            target.getBody().setTransform(hero.getPosition().x + 1.2f, hero.getPosition().y, 0f);
            target.getBody().setLinearVelocity(0f, 0f);
            for (int i = 0; i < 20; i++) fight.update(GameWorld.FIXED_STEP);
            float before = target.getHealth();
            fight.getHeroController().keyDown(Input.Keys.Z);
            fight.update(GameWorld.FIXED_STEP);
            fight.getHeroController().keyDown(Input.Keys.RIGHT);
            fight.update(GameWorld.FIXED_STEP);
            if (hero.getBody().getLinearVelocity().x > Hero.WALK_SPEED * .35f + .1f)
                throw new AssertionError("Heavy windup did not restrict movement");
            fight.getHeroController().keyUp(Input.Keys.RIGHT);
            for (int i = 2; i < 28; i++) fight.update(GameWorld.FIXED_STEP);
            closeTo(target.getHealth(), before, "heavy attack hit before windup");
            for (int i = 0; i < 45; i++) fight.update(GameWorld.FIXED_STEP);
            if (target.getHealth() >= before - 2f)
                throw new AssertionError("Heavy strike missed or ignored the relic synergy");
            target.getBody().setTransform(30f, 2.5f, 0f);
            hero.getCombatStateMachine().changeState(
                new com.cursedarchie.platformer.actors.hero.states.combat.AttackState(true, 0));
            for (int i = 0; i < 100; i++) fight.update(GameWorld.FIXED_STEP);
            if (!(hero.getCombatStateMachine().getCurrentState() instanceof
                com.cursedarchie.platformer.actors.hero.states.combat.AttackState))
                throw new AssertionError("Missed heavy strike recovered too early");
            for (int i = 0; i < 12; i++) fight.update(GameWorld.FIXED_STEP);
            if (hero.getCombatStateMachine().getCurrentState() instanceof
                com.cursedarchie.platformer.actors.hero.states.combat.AttackState)
                throw new AssertionError("Missed heavy strike recovery never ended");
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
            // Roll: brief invulnerability, then vulnerable again; cooldown blocks immediate re-roll.
            if (!hero.tryRoll(1f)) throw new AssertionError("Grounded idle hero could not roll");
            float hp = hero.getHealth();
            hero.receiveHit(new CombatHit(CombatHit.Type.MELEE_LIGHT, 1f, 9f, 1f, 0f, null));
            closeTo(hero.getHealth(), hp, "roll i-frames did not avoid the hit");
            hero.update(Hero.ROLL_INVULNERABLE + .02f);
            if (hero.isRollInvulnerable()) throw new AssertionError("Roll invulnerability lasted too long");
            hero.update(Hero.ROLL_DURATION);
            if (hero.tryRoll(1f)) throw new AssertionError("Roll could be repeated during its cooldown");
            hero.update(Hero.ROLL_COOLDOWN);
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
            if (hero.attackDefinition(false, 3).missRecovery <= 0f) throw new AssertionError("Counter has no miss penalty");
            if (hero.attackDefinition(false, 4).missRecovery < .4f) throw new AssertionError("Jump attack landing recovery too short");
            if (hero.attackDefinition(false, 3).staggerDuration <= hero.attackDefinition(false, 1).staggerDuration)
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

        // A press that is too early makes the strike hurried, breaks the combo and does not cancel the strike.
        hurried[0] = 0f;
        java.util.List<String> early = rhythmRun(.17f, 60, 0, hurried);
        if (hurried[0] != 1f) throw new AssertionError("An early press did not make the strike hurried");
        if (early.contains("hero_light_2") || early.size() != 1)
            throw new AssertionError("An early press must not continue or restart the combo: " + early);

        // Spamming right after the early press is not queued and cannot extend or restart anything.
        java.util.List<String> spam = rhythmRun(.17f, 90, 6, hurried);
        if (spam.contains("hero_light_2") || spam.size() != 1)
            throw new AssertionError("Spam produced extra or chained strikes: " + spam);

        // Hurried strikes are weaker and slower to recover; correct ones are untouched.
        com.cursedarchie.platformer.combat.AttackRhythmConfig cfg =
            com.cursedarchie.platformer.combat.AttackRhythmConfig.get();
        GameWorld w = combatFixture();
        try {
            Hero hero = w.getHero();
            com.cursedarchie.platformer.combat.AttackDefinition def = hero.attackDefinition(false, 1);
            com.cursedarchie.platformer.combat.AttackRuntime normal =
                new com.cursedarchie.platformer.combat.AttackRuntime(def, false);
            com.cursedarchie.platformer.combat.AttackRuntime rushed =
                new com.cursedarchie.platformer.combat.AttackRuntime(def, false);
            normal.advance(.01f);
            rushed.advance(def.nextInputTime - .02f);
            if (rushed.registerAttackPress(true) != com.cursedarchie.platformer.combat.InputResult.EARLY_INPUT)
                throw new AssertionError("Press just before the window was not classified as early");
            if (rushed.registerAttackPress(true) != com.cursedarchie.platformer.combat.InputResult.NO_INPUT)
                throw new AssertionError("A second press of the same strike must be ignored");
            if (normal.registerAttackPress(true) != com.cursedarchie.platformer.combat.InputResult.EARLY_INPUT)
                throw new AssertionError("A press while the swing has barely begun must be a hurried early press");
            normal.advance(def.nextInputTime);   // later pressing, even in the window, never rescues the combo
            if (normal.registerAttackPress(true) != com.cursedarchie.platformer.combat.InputResult.NO_INPUT || !normal.isHurried())
                throw new AssertionError("Mashing before the window must not reach the combo");
            com.cursedarchie.platformer.combat.AttackRuntime clean =
                new com.cursedarchie.platformer.combat.AttackRuntime(def, false);
            float expectedExtra = def.recoveryDuration * (cfg.hurriedRecoveryMultiplier - 1f) + cfg.hurriedRecoveryExtra;
            closeTo(rushed.duration() - clean.duration(), expectedExtra, "Hurried recovery must be longer by the configured amount");
            if (!(rushed.duration() > clean.duration() + .15f))
                throw new AssertionError("Hurried strike did not get a longer recovery");
            if (cfg.hurriedDamageMultiplier > .85f || cfg.hurriedKnockbackMultiplier > .7f || cfg.hurriedStaggerMultiplier > .7f)
                throw new AssertionError("Hurried strike is not weak enough");
            com.badlogic.gdx.math.Rectangle a = new com.badlogic.gdx.math.Rectangle();
            com.badlogic.gdx.math.Rectangle b = new com.badlogic.gdx.math.Rectangle();
            com.badlogic.gdx.math.Vector2 origin = new com.badlogic.gdx.math.Vector2(10f, 2.5f);
            clean.hitbox(origin, a);
            rushed.hitbox(origin, b);
            if (!(b.width < a.width)) throw new AssertionError("Hurried strike did not lose reach");
            closeTo(rushed.definition().damageMultiplier, normal.definition().damageMultiplier,
                "Hurried strike must keep the same base definition");
        } finally { w.dispose(); }

        // A late press (after a shortened window) starts a fresh first strike, not strike two.
        com.cursedarchie.platformer.combat.AttackRhythmConfig saved = cfg;
        com.cursedarchie.platformer.combat.AttackRhythmConfig tight =
            new com.cursedarchie.platformer.combat.AttackRhythmConfig();
        tight.comboWindowLength = .03f;
        com.cursedarchie.platformer.combat.AttackRhythmConfig.set(tight);
        try {
            java.util.List<String> late = rhythmRun(.33f, 90, 0, hurried);
            if (late.size() < 2 || !"hero_light_1".equals(late.get(1)))
                throw new AssertionError("A late press did not begin a fresh first strike: " + late);
        } finally { com.cursedarchie.platformer.combat.AttackRhythmConfig.set(saved); }

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

    private static boolean stepUntil(GameWorld w, Hero hero, com.cursedarchie.platformer.combat.HeavyState state, int max) {
        for (int i = 0; i < max; i++) {
            if (hero.getHeavyState() == state) return true;
            w.update(GameWorld.FIXED_STEP);
        }
        return hero.getHeavyState() == state;
    }

    private void checkBreakthroughDash() {
        com.cursedarchie.platformer.combat.HeavyState NONE = com.cursedarchie.platformer.combat.HeavyState.NONE;
        com.cursedarchie.platformer.combat.HeavyState CHARGE = com.cursedarchie.platformer.combat.HeavyState.HEAVY_CHARGE;
        com.cursedarchie.platformer.combat.HeavyState DASH = com.cursedarchie.platformer.combat.HeavyState.HEAVY_DASH;
        com.cursedarchie.platformer.combat.HeavyState RECOVER = com.cursedarchie.platformer.combat.HeavyState.HEAVY_RECOVERY;
        closeTo(com.cursedarchie.platformer.combat.BreakthroughConfig.parse("{\"dashDistance\":2.0}").dashDistance, 2f,
            "breakthrough config value was not read");
        closeTo(com.cursedarchie.platformer.combat.BreakthroughConfig.parse("{}").chargeTime, .75f,
            "breakthrough defaults missing");

        // 1) States, timings, distance, restricted charge, direction lock, cooldown.
        GameWorld w = combatFixture();
        try {
            parkEnemies(w);
            Hero hero = w.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            hero.setFacingLeft(false);
            w.update(GameWorld.FIXED_STEP);
            w.getHeroController().keyDown(Input.Keys.Z);
            w.getHeroController().keyUp(Input.Keys.Z);
            int charge = 0, dash = 0, recover = 0;
            float plannedDistance = Float.NaN, lockedX = Float.NaN;
            float dashStartX = Float.NaN, dashEndX = Float.NaN, maxChargeSpeed = 0f;
            boolean sawRecoveryAttackIgnored = true;
            Object recoveryState = null;
            for (int i = 1; i <= 120; i++) {
                if (i == 2) w.getHeroController().keyDown(Input.Keys.LEFT);   // retreat input must change nothing
                if (i == 15) { w.getHeroController().keyUp(Input.Keys.LEFT); w.getHeroController().keyDown(Input.Keys.RIGHT); }
                if (i == 40) { w.getHeroController().keyUp(Input.Keys.RIGHT); w.getHeroController().keyDown(Input.Keys.LEFT); }
                w.update(GameWorld.FIXED_STEP);
                com.cursedarchie.platformer.combat.HeavyState st = hero.getHeavyState();
                if (st == CHARGE) {
                    charge++;
                    maxChargeSpeed = Math.max(maxChargeSpeed, Math.abs(hero.getBody().getLinearVelocity().x));
                    if (Float.isNaN(lockedX)) lockedX = hero.getPosition().x;
                    if (hero.isFacingLeft()) throw new AssertionError("The hero turned while charging");
                    plannedDistance = ((com.cursedarchie.platformer.actors.hero.states.combat.AttackState)
                        hero.getCombatStateMachine().getCurrentState()).getPlannedDistance();
                    if (Math.abs(hero.getPosition().x - lockedX) > .001f)
                        throw new AssertionError("The charging hero slid: " + (hero.getPosition().x - lockedX));
                } else if (st == DASH) {
                    dash++;
                    if (maxChargeSpeed > 0.001f) throw new AssertionError("Charge moved the hero: " + maxChargeSpeed);
                    if (Float.isNaN(dashStartX)) dashStartX = hero.getPosition().x;
                    dashEndX = hero.getPosition().x;
                } else if (st == RECOVER) {
                    recover++;
                    if (recoveryState == null) {
                        recoveryState = hero.getCombatStateMachine().getCurrentState();
                        w.getHeroController().keyDown(Input.Keys.X);
                        w.getHeroController().keyUp(Input.Keys.X);
                        w.getHeroController().keyDown(Input.Keys.Z);
                        w.getHeroController().keyUp(Input.Keys.Z);
                    } else if (hero.getCombatStateMachine().getCurrentState() != recoveryState) sawRecoveryAttackIgnored = false;
                }
            }
            w.getHeroController().keyUp(Input.Keys.LEFT);
            com.cursedarchie.platformer.combat.BreakthroughConfig bc = com.cursedarchie.platformer.combat.BreakthroughConfig.get();
            int dashLimit = Math.round(bc.dashDuration / GameWorld.FIXED_STEP) + 2;
            if (Math.abs(charge - Math.round(bc.chargeTime / GameWorld.FIXED_STEP)) > 3 || dash > dashLimit ||
                Math.abs(recover - Math.round(bc.recoveryTime / GameWorld.FIXED_STEP)) > 4)
                throw new AssertionError("Breakthrough phase lengths off: " + charge + "/" + dash + "/" + recover);
            if (Math.abs(plannedDistance - bc.dashDistance) > .01f)
                throw new AssertionError("Open floor must plan the full distance: " + plannedDistance);
            float travelled = dashEndX - dashStartX;
            if (Math.abs(travelled - plannedDistance) > .6f)   // dashStartX is sampled one frame in
                throw new AssertionError("Dash covered " + travelled + " but the arrow showed " + plannedDistance);
            if (hero.isFacingLeft() && hero.getPosition().x < 10f)
                throw new AssertionError("The dash did not go right (direction was locked at the start)");
            if (!sawRecoveryAttackIgnored) throw new AssertionError("Recovery was cancelled by an attack press");
            if (hero.getHeavyState() != NONE) throw new AssertionError("Breakthrough never returned to idle");
        } finally { w.dispose(); }

        // 2) Three enemies in a row: 100% / 50% / 50%, each only once, and the dash passes through them.
        GameWorld g = combatFixture();
        try {
            parkEnemies(g);
            Hero hero = g.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            hero.setFacingLeft(false);
            Enemy e1 = staticFoe(g, 11.0f), e2 = staticFoe(g, 11.8f), e3 = staticFoe(g, 12.5f);
            float h1 = e1.getHealth(), h2 = e2.getHealth(), h3 = e3.getHealth();
            g.update(GameWorld.FIXED_STEP);
            g.getHeroController().keyDown(Input.Keys.Z);
            g.getHeroController().keyUp(Input.Keys.Z);
            for (int i = 0; i < 110; i++) g.update(GameWorld.FIXED_STEP);
            float l1 = h1 - e1.getHealth(), l2 = h2 - e2.getHealth(), l3 = h3 - e3.getHealth();
            if (l1 <= 0f) throw new AssertionError("The first enemy on the path took no damage");
            closeTo(l2, l1 * .5f, "second enemy must take half damage, once");
            closeTo(l3, l1 * .5f, "third enemy must take half damage, once");
            if (hero.getPosition().x < 12.3f)
                throw new AssertionError("Enemies blocked the dash: hero ended at " + hero.getPosition().x);
        } finally { g.dispose(); }

        // 3) A wall stops the dash, shows an impact, starts recovery early and shields enemies behind it.
        GameWorld wall = combatFixture();
        try {
            parkEnemies(wall);
            Hero hero = wall.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            hero.setFacingLeft(false);
            addLevelWall(wall, 11.8f, 2.5f, .1f, 1.5f);
            Enemy behind = staticFoe(wall, 12.7f);
            float behindHealth = behind.getHealth();
            int impacts = hero.getBreakthroughImpactId();
            wall.update(GameWorld.FIXED_STEP);
            wall.getHeroController().keyDown(Input.Keys.Z);
            wall.getHeroController().keyUp(Input.Keys.Z);
            int dashSteps = 0;
            for (int i = 0; i < 110; i++) {
                wall.update(GameWorld.FIXED_STEP);
                if (hero.getHeavyState() == DASH) dashSteps++;
            }
            if (hero.getPosition().x > 11.7f) throw new AssertionError("Dash went through a wall: " + hero.getPosition().x);
            if (hero.getPosition().x < 11.7f - hero.getSize() - .2f)
                throw new AssertionError("Dash stopped far short of the wall: " + hero.getPosition().x);
            if (hero.getBreakthroughImpactId() == impacts) throw new AssertionError("Wall hit produced no impact");
            if (dashSteps > 16) throw new AssertionError("Dash kept going after hitting the wall: " + dashSteps);
            closeTo(behind.getHealth(), behindHealth, "Dash hit an enemy through a wall");
        } finally { wall.dispose(); }

        // 4) Interruptions: a hit breaks the charge and the recovery, but a dash in full flight carries on.
        for (int phase = 0; phase < 3; phase++) {
            GameWorld hurt = combatFixture();
            try {
                parkEnemies(hurt);
                Hero hero = hurt.getHero();
                hero.getBody().setTransform(10f, 2.5f, 0f);
                hero.getBody().setLinearVelocity(0f, 0f);
                hurt.update(GameWorld.FIXED_STEP);
                hurt.getHeroController().keyDown(Input.Keys.Z);
                hurt.getHeroController().keyUp(Input.Keys.Z);
                if (!stepUntil(hurt, hero, phase == 0 ? CHARGE : phase == 1 ? DASH : RECOVER, 120))
                    throw new AssertionError("Could not reach breakthrough phase " + phase);
                for (int i = 0; i < 3; i++) hurt.update(GameWorld.FIXED_STEP);
                Enemy src = hurt.getEnemies().first();
                float hp = hero.getHealth();
                hero.receiveHit(new CombatHit(CombatHit.Type.MELEE_LIGHT, 1f, src, hero, 1f, 1f, 3f, .3f, true));
                hurt.update(GameWorld.FIXED_STEP);
                if (hero.getHealth() >= hp) throw new AssertionError("Hero took no damage in breakthrough phase " + phase);
                boolean still = hero.getHeavyState() != NONE;
                if (phase == 1 && !still) throw new AssertionError("A hit cancelled a dash in full flight");
                if (phase != 1 && still) throw new AssertionError("A hit did not interrupt breakthrough phase " + phase);
                if (!hero.isStaggered()) throw new AssertionError("Hit during breakthrough phase " + phase + " gave no stagger");
            } finally { hurt.dispose(); }
        }

        // 5) Cooldown counts from the end of the recovery.
        GameWorld cd = combatFixture();
        try {
            parkEnemies(cd);
            Hero hero = cd.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            cd.update(GameWorld.FIXED_STEP);
            cd.getHeroController().keyDown(Input.Keys.Z);
            cd.getHeroController().keyUp(Input.Keys.Z);
            if (!stepUntil(cd, hero, RECOVER, 120)) throw new AssertionError("no recovery");
            for (int i = 0; i < 60 && hero.getHeavyState() != NONE; i++) cd.update(GameWorld.FIXED_STEP);
            if (hero.canHeavyAttack()) throw new AssertionError("Cooldown did not start after the recovery");
            for (int i = 0; i < 110; i++) cd.update(GameWorld.FIXED_STEP);
            if (!hero.canHeavyAttack()) throw new AssertionError("Cooldown never ended");
        } finally { cd.dispose(); }

        // 6) A guarding enemy is not simply cut down: the first blow breaks the guard and deals reduced damage.
        GameWorld guard = combatFixture();
        try {
            parkEnemies(guard);
            Hero hero = guard.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            hero.setFacingLeft(false);
            Enemy raider = staticFoe(guard, 11.2f);
            raider.setCanSeeHero(true);
            raider.forceBlockForTest();
            float full = hero.getDamage() * 2f;
            float before = raider.getHealth();
            guard.update(GameWorld.FIXED_STEP);
            guard.getHeroController().keyDown(Input.Keys.Z);
            guard.getHeroController().keyUp(Input.Keys.Z);
            for (int i = 0; i < 110; i++) guard.update(GameWorld.FIXED_STEP);
            float loss = before - raider.getHealth();
            if (loss <= 0f || loss >= full - .05f)
                throw new AssertionError("Guarding enemy took " + loss + " of a full " + full + " blow");
            if (raider.isBlocking()) throw new AssertionError("The dash did not break the guard");
        } finally { guard.dispose(); }

        // 7) A heavy enemy slows the dash but never cancels it.
        GameWorld heavy = combatFixture();
        try {
            parkEnemies(heavy);
            Hero hero = heavy.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            hero.setFacingLeft(false);
            Enemy brute = EnemyFactory.createHeavyHunter(heavy.getWorld(), new Vector2(11.2f, 2.5f));
            brute.setActive(false);
            heavy.addEnemy(brute);
            brute.getBody().setType(com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody);
            float bruteHealth = brute.getHealth();
            heavy.update(GameWorld.FIXED_STEP);
            heavy.getHeroController().keyDown(Input.Keys.Z);
            heavy.getHeroController().keyUp(Input.Keys.Z);
            float startX = Float.NaN, endX = Float.NaN;
            boolean reachedRecovery = false;
            for (int i = 0; i < 120; i++) {
                heavy.update(GameWorld.FIXED_STEP);
                if (hero.getHeavyState() == DASH) {
                    if (Float.isNaN(startX)) startX = hero.getPosition().x;
                    endX = hero.getPosition().x;
                }
                if (hero.getHeavyState() == RECOVER) reachedRecovery = true;
            }
            if (!reachedRecovery) throw new AssertionError("A heavy enemy cancelled the dash");
            if (brute.getHealth() >= bruteHealth) throw new AssertionError("The dash did not hurt the heavy enemy");
            if (brute.getSize() >= 2f && endX - startX > 2.9f)
                throw new AssertionError("A heavy enemy did not slow the dash: " + (endX - startX));
        } finally { heavy.dispose(); }

        // 8) The arrow is shortened by a wall, ignores a wall closer than the minimum, and a weak hit keeps the charge.
        GameWorld arrow = combatFixture();
        try {
            parkEnemies(arrow);
            Hero hero = arrow.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            hero.setFacingLeft(false);
            addLevelWall(arrow, 12.6f, 2.5f, .1f, 1.5f);
            arrow.update(GameWorld.FIXED_STEP);
            arrow.getHeroController().keyDown(Input.Keys.Z);
            arrow.getHeroController().keyUp(Input.Keys.Z);
            for (int i = 0; i < 3; i++) arrow.update(GameWorld.FIXED_STEP);
            com.cursedarchie.platformer.actors.hero.states.combat.AttackState charging =
                (com.cursedarchie.platformer.actors.hero.states.combat.AttackState) hero.getCombatStateMachine().getCurrentState();
            if (!(charging.getPlannedDistance() < 2.5f) || !charging.isWallLimited())
                throw new AssertionError("A wall ahead did not shorten the arrow: " + charging.getPlannedDistance());
            Enemy src = arrow.getEnemies().first();
            hero.receiveHit(new CombatHit(CombatHit.Type.MELEE_LIGHT, 0.1f, src, hero, 1f, 1f, 3f, .08f, true));
            arrow.update(GameWorld.FIXED_STEP);
            if (hero.getHeavyState() != CHARGE) throw new AssertionError("A light hit interrupted the charge");
            if (Math.abs(hero.getBody().getLinearVelocity().x) > .001f) throw new AssertionError("A light hit pushed the charging hero");
            hero.receiveHit(new CombatHit(CombatHit.Type.MELEE_HEAVY, 0.1f, src, hero, 1f, 1f, 3f, .24f, true));
            arrow.update(GameWorld.FIXED_STEP);
            if (hero.getHeavyState() != NONE) throw new AssertionError("A heavy hit did not interrupt the charge");
        } finally { arrow.dispose(); }

        // 9) A hero who dies mid-charge does not go on to dash.
        GameWorld dead = combatFixture();
        try {
            parkEnemies(dead);
            Hero hero = dead.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            dead.update(GameWorld.FIXED_STEP);
            dead.getHeroController().keyDown(Input.Keys.Z);
            dead.getHeroController().keyUp(Input.Keys.Z);
            for (int i = 0; i < 10; i++) dead.update(GameWorld.FIXED_STEP);
            hero.setAlive(false);
            for (int i = 0; i < 90; i++) dead.update(GameWorld.FIXED_STEP);
            if (hero.getHeavyState() == DASH || hero.isBreakthroughDashing())
                throw new AssertionError("A dead hero kept charging or dashing");
        } finally { dead.dispose(); }

        // 10) A rushed strike leaves a real recovery: no block, no queued strike, mashing does not shorten it.
        GameWorld rush = combatFixture();
        try {
            parkEnemies(rush);
            Hero hero = rush.getHero();
            hero.getBody().setTransform(10f, 2.5f, 0f);
            hero.setFacingLeft(false);
            rush.update(GameWorld.FIXED_STEP);
            rush.getHeroController().keyDown(Input.Keys.X);
            rush.getHeroController().keyUp(Input.Keys.X);
            for (int i = 0; i < 4; i++) rush.update(GameWorld.FIXED_STEP);
            rush.getHeroController().keyDown(Input.Keys.X);   // far too early
            rush.getHeroController().keyUp(Input.Keys.X);
            com.cursedarchie.platformer.actors.hero.states.combat.AttackState first = heroAttack(hero);
            if (first == null || !first.getRuntime().isHurried()) throw new AssertionError("An early press did not hurry the strike");
            float expected = first.getRuntime().duration();
            rush.getHeroController().keyDown(Input.Keys.B);   // guard held through the recovery
            boolean blockedInRecovery = false, otherStrike = false;
            int steps = 4;
            for (int i = 0; i < 120 && heroAttack(hero) == first; i++) {
                rush.getHeroController().keyDown(Input.Keys.X);   // mashing during the whole strike
                rush.getHeroController().keyUp(Input.Keys.X);
                rush.update(GameWorld.FIXED_STEP);
                steps++;
                com.cursedarchie.platformer.actors.hero.states.combat.AttackState a = heroAttack(hero);
                if (a != null && a != first) otherStrike = true;
                if (a == first && a.isInRecovery(hero) && hero.isBlocking()) blockedInRecovery = true;
            }
            rush.getHeroController().keyUp(Input.Keys.B);
            if (blockedInRecovery) throw new AssertionError("The hero could guard during a rushed recovery");
            if (otherStrike) throw new AssertionError("Mashing during a rushed strike started another strike");
            if (steps * GameWorld.FIXED_STEP < expected - .05f)
                throw new AssertionError("Mashing shortened a rushed recovery: " + steps * GameWorld.FIXED_STEP + " < " + expected);
        } finally { rush.dispose(); }
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
            hero.getBody().setTransform(10f, 2.5f, 0f);
            for (int i = 0; i < 4; i++) {
                Enemy e = EnemyFactory.createDefaultEnemy(fight.getWorld(), new Vector2(12f + i * 1.5f, 2.5f));
                e.setBrain(null);
                e.setIntent(Enemy.Intent.IDLE);
                fight.addEnemy(e);
            }
            fight.update(GameWorld.FIXED_STEP);
            int permitted = 0;
            for (Enemy e : fight.getAliveEnemies()) if (e.hasAttackPermit()) permitted++;
            if (permitted > 2) throw new AssertionError("More than two melee enemies hold attack slots: " + permitted);
            if (permitted < 1) throw new AssertionError("No melee enemy received an attack slot");
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

    private void checkHeroBlockAndHeavyRules() {
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
            if (hero.canHeavyAttack() != true) throw new AssertionError("Heavy should start off cooldown");
            hero.beginHeavyCooldown();
            if (hero.canHeavyAttack()) throw new AssertionError("Heavy attack has no cooldown");
            hero.update(com.cursedarchie.platformer.combat.BreakthroughConfig.get().cooldown + .01f);
            if (!hero.canHeavyAttack()) throw new AssertionError("Heavy cooldown never ends");
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
        for (int variant = 0; variant < 3; variant++) {
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
                    new com.cursedarchie.platformer.actors.hero.states.combat.AttackState(
                        variant == 1, variant == 2 ? 2 : 1));
                // Light strikes lock their direction at once. The breakthrough charge may still be steered
                // until its final lock-in, so its retreat input arrives only after the direction is final.
                if (variant != 1) fight.getHeroController().keyDown(Input.Keys.LEFT);
                int beforeActive = variant == 1 ? 40 : variant == 2 ? 4 : 7;
                for (int i = 0; i < beforeActive; i++) fight.update(GameWorld.FIXED_STEP);
                if (variant == 1) fight.getHeroController().keyDown(Input.Keys.LEFT);
                target.getBody().setTransform(hero.getPosition().x + 1.2f,
                    hero.getPosition().y, 0f);
                for (int i = beforeActive; i < (variant == 1 ? 70 : 20); i++)
                    fight.update(GameWorld.FIXED_STEP);
                if (target.getHealth() >= 20f || hero.isFacingLeft())
                    throw new AssertionError("Attack turned with retreat input, variant=" + variant);
                for (int i = 0; i < (variant == 1 ? 50 : 22); i++) fight.update(GameWorld.FIXED_STEP);
                if (!hero.isFacingLeft())
                    throw new AssertionError("Hero did not face retreat direction after attack");
                hero.getBody().setTransform(20f, 2.5f, 0f);
                hero.setFacingLeft(false);
                target.getBody().setTransform(25f, 2.5f, 0f);
                target.setHealth(20f);
                hero.getCombatStateMachine().changeState(
                    new com.cursedarchie.platformer.actors.hero.states.combat.AttackState(
                        variant == 1, variant == 2 ? 2 : 1));
                for (int i = 0; i < (variant == 1 ? 70 : 20); i++)
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
                route.getHero().getBody().setTransform(next.getWorldX() + 2f, 3f, 0f);
                route.update(GameWorld.FIXED_STEP);
                if (route.getCurrentRoom().getRoomId() != roomId)
                    throw new AssertionError("Alley boundary did not advance to room " + roomId);
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
                throw new AssertionError("Alley exit did not return to the same street doorway");
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
