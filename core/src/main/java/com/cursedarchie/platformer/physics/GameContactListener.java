package com.cursedarchie.platformer.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.*;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.hero.Hero;

public class GameContactListener implements ContactListener {

    private final Hero hero;
    private int footContacts = 0;

    public GameContactListener(Hero hero) {
        this.hero = hero;
    }

    @Override
    public void beginContact(Contact contact) {
        handleContact(contact, true);
    }

    @Override
    public void endContact(Contact contact) {
        handleContact(contact, false);
    }

    private void handleContact(Contact contact, boolean begin) {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();

        //FOOT_SENSOR=================================================================================
        if (isHeroFoot(fixtureA) || isHeroFoot(fixtureB)) {

            Fixture other = isHeroFoot(fixtureA) ? fixtureB : fixtureA;
            System.out.println("Hero foot contact with: " + other.getUserData() + " begin: " + begin);

            if (other.getUserData() != null && other.getUserData().equals("level")) {
                if (begin) {
                    footContacts++;
                } else {
                    footContacts--;
                    if (footContacts < 0) footContacts = 0;
                }
                hero.setGrounded(footContacts > 0);
            }
        }

        //ATTACK_SENSOR===============================================================================

        if (begin && (isHeroAttack(fixtureA) || isHeroAttack(fixtureB))) {
            Fixture other = isHeroAttack(fixtureA) ? fixtureB : fixtureA;

            Object data = other.getBody().getUserData();

            if (data instanceof Enemy) {
                Enemy enemy = (Enemy) data;

                if (!hero.isAttackHandled()) {
                    enemy.takeDamage(hero.getDamage());

                    hero.setAttackHandled(true);

                    Gdx.app.log("HERO", "Hero hit enemy!");
                }
            }
        }
    }

    private boolean isHeroFoot(Fixture fixture) {
        return fixture.getUserData() != null && fixture.getUserData().equals("hero_foot");
    }

    private boolean isHeroAttack(Fixture fixture) {
        return "hero_attack".equals(fixture.getUserData());
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) { }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) { }
}
