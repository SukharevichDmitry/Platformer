package com.cursedarchie.platformer.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.enemies.states.combat.AttackState;
import com.cursedarchie.platformer.actors.enemies.states.motion.MotionIdleState;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.actors.hero.states.HeroState;
import com.cursedarchie.platformer.actors.hero.states.motion.MoveState;
import com.cursedarchie.platformer.world.GameWorld;

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
        world = new GameWorld();
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
        checkHeroMovementAndContacts();
        checkJumpBuffer();
        checkEnemyHeadJump();
        checkRenderRateConsistency();
        checkHeroAttackTimingAndDirection();
        checkEnemyAttackAndDamageCooldown();
        passed = true;
        Gdx.app.exit();
    }

    private void checkEnemyAwareness() {
        GameWorld attackWorld = new GameWorld();
        try {
            Enemy enemy = null;
            for (Enemy candidate : attackWorld.getEnemies()) {
                if (candidate.getSize() < 1f) enemy = candidate;
                else candidate.setAlive(false);
            }
            if (enemy == null) throw new AssertionError("No regular enemy to test");

            Hero target = attackWorld.getHero();
            enemy.setFacingLeft(true);
            enemy.getBody().setTransform(9f, 2f, 0);
            target.getBody().setTransform(9.5f, 2f, 0);
            attackWorld.update(GameWorld.FIXED_STEP);
            if (!enemy.getAttackTargets().contains(target))
                throw new AssertionError("Behind approach did not enter the attack range sensor");
            if (enemy.isCanSeeHero() || enemy.isCanAttackHero() ||
                enemy.getCombatStateMachine().getCurrentState() instanceof AttackState ||
                !(enemy.getMotionStateMachine().getCurrentState() instanceof MotionIdleState))
                throw new AssertionError("Enemy attacked or animated walking without seeing the hero behind it");

            target.getBody().setTransform(8.5f, 2f, 0);
            target.getBody().setLinearVelocity(0, 0);
            for (int i = 0; i < 3; i++) attackWorld.update(GameWorld.FIXED_STEP);
            if (!enemy.isCanSeeHero() || !enemy.isCanAttackHero() ||
                !(enemy.getCombatStateMachine().getCurrentState() instanceof AttackState))
                throw new AssertionError("Enemy did not attack the hero inside its front range");
        } finally {
            attackWorld.dispose();
        }
    }

    private void checkHeroMovementAndContacts() {
        GameWorld movementWorld = new GameWorld();
        try {
            movementWorld.changeLocation("forest", 0);
            Hero hero = movementWorld.getHero();
            hero.getBody().setTransform(2f, 1.35f, 0f);
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
            if (!landed || apex - startY < 2.8f || apex - startY > 3.5f)
                throw new AssertionError("Jump arc is outside the intended platform range: " + (apex - startY));
            movementWorld.getHeroController().keyUp(Input.Keys.SPACE);

            hero.getBody().setTransform(1.5f, 1.25f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            for (int i = 0; i < 5; i++) movementWorld.update(GameWorld.FIXED_STEP);
            movementWorld.getHeroController().keyDown(Input.Keys.RIGHT);
            for (int i = 0; i < 10; i++) movementWorld.update(GameWorld.FIXED_STEP);
            movementWorld.getHeroController().keyUp(Input.Keys.RIGHT);
            float releasedX = hero.getPosition().x;
            for (int i = 0; i < 12; i++) movementWorld.update(GameWorld.FIXED_STEP);
            if (hero.getPosition().x - releasedX > 0.35f)
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

        GameWorld contactWorld = new GameWorld();
        try {
            Enemy regular = null;
            for (Enemy enemy : contactWorld.getEnemies()) {
                if (enemy.getSize() < 1f) regular = enemy;
                else enemy.setAlive(false);
            }
            if (regular == null) throw new AssertionError("No enemy available for body contact check");
            regular.setActive(false);
            regular.getBody().setTransform(6f, 1.25f, 0f);
            regular.getBody().setLinearVelocity(0f, 0f);
            Hero hero = contactWorld.getHero();
            hero.getBody().setTransform(5f, 1.25f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            contactWorld.getHeroController().keyDown(Input.Keys.RIGHT);
            for (int i = 0; i < 90; i++) contactWorld.update(GameWorld.FIXED_STEP);
            contactWorld.getHeroController().keyUp(Input.Keys.RIGHT);
            if (hero.getPosition().x > 5.6f || Math.abs(regular.getPosition().x - 6f) > 0.05f)
                throw new AssertionError("Hero passed through or shoved an enemy: hero=" +
                    hero.getPosition().x + " enemy=" + regular.getPosition().x);
            hero.getBody().setTransform(4f, 1.25f, 0f);
            regular.setActive(true);
            regular.setBrain(null);
            regular.setIntent(Enemy.Intent.IDLE);
            for (int i = 0; i < 8; i++) contactWorld.update(GameWorld.FIXED_STEP);
            float restingX = regular.getPosition().x;
            for (int i = 0; i < 60; i++) contactWorld.update(GameWorld.FIXED_STEP);
            if (Math.abs(regular.getPosition().x - restingX) > 0.02f ||
                Math.abs(regular.getBody().getLinearVelocity().x) > 0.01f)
                throw new AssertionError("Idle enemy retained push velocity");
        } finally {
            contactWorld.dispose();
        }
    }

    private void checkJumpBuffer() {
        GameWorld bufferedWorld = new GameWorld();
        try {
            bufferedWorld.changeLocation("forest", 0);
            Hero hero = bufferedWorld.getHero();
            hero.getBody().setTransform(2f, 1.6f, 0f);
            hero.getBody().setLinearVelocity(0f, -4f);
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

        GameWorld expiredWorld = new GameWorld();
        try {
            expiredWorld.changeLocation("forest", 0);
            Hero hero = expiredWorld.getHero();
            hero.getBody().setTransform(2f, 4f, 0f);
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
        GameWorld headWorld = new GameWorld();
        try {
            Enemy platformEnemy = null;
            for (Enemy enemy : headWorld.getEnemies()) {
                if (enemy.getSize() < 1f) platformEnemy = enemy;
                else enemy.setAlive(false);
            }
            if (platformEnemy == null) throw new AssertionError("No enemy for head-jump check");
            platformEnemy.setActive(false);
            platformEnemy.getBody().setTransform(5f, 1.25f, 0f);
            platformEnemy.getBody().setLinearVelocity(0f, 0f);
            Hero hero = headWorld.getHero();
            hero.getBody().setTransform(5f, 2.6f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            boolean landedOnEnemy = false;
            for (int i = 0; i < 40; i++) {
                headWorld.update(GameWorld.FIXED_STEP);
                if (hero.isGrounded() && hero.getFootContacts() == 0 &&
                    hero.getPosition().y > platformEnemy.getPosition().y + 0.4f) {
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
            platformEnemy.getBody().setTransform(7f, 3f, 0f);
            platformEnemy.getBody().setLinearVelocity(0f, 0f);
            hero.getBody().setTransform(6.5f, 3f, 0f);
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
            if (Math.abs(sampled[0] - baseline[0]) > 0.03f ||
                Math.abs(sampled[1] - baseline[1]) > 0.03f)
                throw new AssertionError("Hero moved differently at " + fps + " FPS");
        }
    }

    private float[] simulateOneSecondAt(int fps) {
        GameWorld sampledWorld = new GameWorld(42L);
        try {
            sampledWorld.changeLocation("forest", 0);
            Hero hero = sampledWorld.getHero();
            hero.getBody().setTransform(2f, 1.35f, 0f);
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
        GameWorld combatWorld = new GameWorld();
        try {
            Enemy regular = null;
            Enemy boss = null;
            for (Enemy enemy : combatWorld.getEnemies()) {
                enemy.setActive(false);
                if (enemy.getSize() < 1f) regular = enemy;
                else boss = enemy;
            }
            if (regular == null || boss == null)
                throw new AssertionError("Combat fixtures need a regular enemy and boss");
            Hero hero = combatWorld.getHero();
            hero.getBody().setTransform(5f, 1.25f, 0f);
            hero.getBody().setLinearVelocity(0f, 0f);
            hero.setFacingLeft(false);
            regular.getBody().setTransform(5.6f, 1.25f, 0f);
            regular.getBody().setLinearVelocity(0f, 0f);
            boss.getBody().setTransform(4.3f, 1.75f, 0f);
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
            for (int i = 0; i < 35; i++) combatWorld.update(GameWorld.FIXED_STEP);
            if (regular.getHealth() != regularHealth - hero.getDamage())
                throw new AssertionError("Held attack damaged a target twice");
            combatWorld.getHeroController().keyUp(Input.Keys.X);
        } finally {
            combatWorld.dispose();
        }
    }

    private void checkEnemyAttackAndDamageCooldown() {
        GameWorld combatWorld = new GameWorld();
        try {
            Enemy attacker = null;
            for (Enemy enemy : combatWorld.getEnemies()) {
                if (enemy.getSize() < 1f) attacker = enemy;
                else enemy.setAlive(false);
            }
            if (attacker == null) throw new AssertionError("No regular enemy to check attack timing");
            attacker.setBrain(null);
            attacker.setIntent(Enemy.Intent.IDLE);
            attacker.setFacingLeft(true);
            attacker.getBody().setTransform(5.6f, 1.25f, 0f);
            attacker.getBody().setLinearVelocity(0f, 0f);
            Hero hero = combatWorld.getHero();
            hero.getBody().setTransform(5f, 1.25f, 0f);
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
            attacker.getCombatStateMachine().changeState(new AttackState());
            for (int i = 0; i < 18; i++) combatWorld.update(GameWorld.FIXED_STEP);
            if (hero.getHealth() != health - attacker.getDamage())
                throw new AssertionError("Hero took a second hit during the damage cooldown");
        } finally {
            combatWorld.dispose();
        }
    }

    private static void closeTo(float actual, float expected, String failure) {
        if (Math.abs(actual - expected) > 0.0001f)
            throw new AssertionError(failure + ": expected " + expected + ", got " + actual);
    }

    @Override public void dispose() {
        if (world != null) world.dispose();
    }
}
