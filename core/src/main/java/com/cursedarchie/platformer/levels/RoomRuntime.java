package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;

public class RoomRuntime {

    private Body levelBody;

    public Body getLevelBody() {
        return levelBody;
    }

    public void build(World world) {

        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.StaticBody;

        levelBody = world.createBody(def);
        levelBody.setUserData("level");
    }

    public void destroy(World world) {

        if (levelBody != null) {
            world.destroyBody(levelBody);
            levelBody = null;
        }
    }
}
