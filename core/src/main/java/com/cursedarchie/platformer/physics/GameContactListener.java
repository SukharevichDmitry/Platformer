package com.cursedarchie.platformer.physics;

import com.badlogic.gdx.physics.box2d.*;
import com.cursedarchie.platformer.actors.Actor;

/** Maintains sensor overlap sets; never creates or destroys Box2D bodies in callbacks. */
public class GameContactListener implements ContactListener {

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
        handleFootSensor(fixtureA, fixtureB, begin);
        handleVision(fixtureA, fixtureB, begin);

        handleAttack(fixtureA, fixtureB, PhysicsTags.HERO_ATTACK_SENSOR, begin);
        handleAttack(fixtureA, fixtureB, PhysicsTags.ENEMY_ATTACK_SENSOR, begin);

    }

    private void handleFootSensor(Fixture a, Fixture b, boolean begin) {

        Fixture foot = findFixture(a, b, PhysicsTags.FOOT_SENSOR);

        if (foot == null) return;

        Fixture other = foot == a ? b : a;

        Object actorData = foot.getBody().getUserData();

        if (!(actorData instanceof Actor)) return;

        Actor actor = (Actor) actorData;

        // Actor bodies are handled by GameWorld's post-step manifold check, not here.
        boolean isGround = "level".equals(other.getBody().getUserData());
        if (isGround && !other.isSensor()) {

            int contacts = actor.getFootContacts();

            if (begin) {
                contacts++;
            } else {
                contacts--;
            }

            if (contacts < 0) contacts = 0;

            actor.setFootContacts(contacts);
            actor.setGrounded(contacts > 0);
        }
    }

    private void handleVision(Fixture a, Fixture b, boolean begin) {

        Fixture vision = findFixture(a, b, PhysicsTags.VISION_SENSOR);

        if (vision == null) return;

        Fixture other = (vision == a) ? b : a;
        if (other.isSensor()) return;

        Object ownerObj = vision.getBody().getUserData();
        Object targetObj = other.getBody().getUserData();

        if (!(ownerObj instanceof Actor)) return;
        if (!(targetObj instanceof Actor)) return;

        Actor owner = (Actor) ownerObj;
        Actor target = (Actor) targetObj;

        if (begin) {
            owner.addVisibleActor(target);
        } else {
            owner.removeVisibleActor(target);
        }
    }

    private void handleAttack(Fixture a, Fixture b, String tag, boolean begin) {

        Fixture attack = findFixture(a, b, tag);
        if (attack == null) return;

        Fixture other = (attack == a) ? b : a;

        if (other.isSensor()) return;

        Object attackerObj = attack.getBody().getUserData();
        Object targetObj = other.getBody().getUserData();

        if (!(attackerObj instanceof Actor)) return;
        if (!(targetObj instanceof Actor)) return;

        Actor attacker = (Actor) attackerObj;
        Actor target = (Actor) targetObj;

        if (begin) {
            attacker.addAttackTarget(target);
        } else {
            attacker.removeAttackTarget(target);
        }
    }

    private Fixture findFixture(Fixture a, Fixture b, String tag) {

        if (tag.equals(a.getUserData())) return a;
        if (tag.equals(b.getUserData())) return b;

        return null;
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) { }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) { }
}
