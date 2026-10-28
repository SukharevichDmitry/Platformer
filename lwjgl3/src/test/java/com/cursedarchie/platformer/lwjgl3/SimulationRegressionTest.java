package com.cursedarchie.platformer.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.EnemyFactory;
import com.cursedarchie.platformer.actors.enemies.logic.SimpleEnemyBrain;
import com.badlogic.gdx.math.Vector2;
import com.cursedarchie.platformer.actors.Actor;
import com.cursedarchie.platformer.actors.enemies.states.combat.AttackState;
import com.cursedarchie.platformer.actors.enemies.states.motion.MotionIdleState;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroState;
import com.cursedarchie.platformer.actors.hero.states.motion.MoveState;
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
        checkEnemyStopsAtAttackRange();
        checkHeroMovementAndContacts();
        checkJumpBuffer();
        checkEnemyHeadJump();
        checkRenderRateConsistency();
        checkHeroAttackTimingAndDirection();
        checkEnemyAttackAndDamageCooldown();
        checkLocationTitleTiming();
        checkInteractionInput();
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
            if (enemy.isCanSeeHero() || enemy.isCanAttackHero() ||
                enemy.getCombatStateMachine().getCurrentState() instanceof AttackState ||
                !(enemy.getMotionStateMachine().getCurrentState() instanceof MotionIdleState))
                throw new AssertionError("Enemy attacked or animated walking without seeing the hero behind it");

            target.getBody().setTransform(17f, 4f, 0);
            target.getBody().setLinearVelocity(0, 0);
            for (int i = 0; i < 3; i++) attackWorld.update(GameWorld.FIXED_STEP);
            if (!enemy.isCanSeeHero() || !enemy.isCanAttackHero() ||
                !(enemy.getCombatStateMachine().getCurrentState() instanceof AttackState))
                throw new AssertionError("Enemy did not attack the hero inside its front range");
        } finally {
            attackWorld.dispose();
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

    private void checkEnemyHeadJump() {
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
            if (!landedOnEnemy) throw new AssertionError("Enemy head did not support the hero");
            headWorld.getHeroController().keyDown(Input.Keys.SPACE);
            headWorld.update(GameWorld.FIXED_STEP);
            if (hero.getBody().getLinearVelocity().y <= 0f)
                throw new AssertionError("Hero could not jump from the enemy head");
            headWorld.getHeroController().keyUp(Input.Keys.SPACE);
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

    private static GameWorld.DamageIndicator indicatorFor(GameWorld world, Actor target) {
        for (GameWorld.DamageIndicator indicator : world.getDamageIndicators())
            if (indicator.target == target) return indicator;
        return null;
    }

    private static void closeTo(float actual, float expected, String failure) {
        if (Math.abs(actual - expected) > 0.0001f)
            throw new AssertionError(failure + ": expected " + expected + ", got " + actual);
    }

    @Override public void dispose() {
        if (world != null) world.dispose();
    }
}
