//package com.cursedarchie.platformer.actors;
//import com.badlogic.gdx.math.Vector2;
//
//public class Hero extends Actor<Hero.HeroState>{
//
//    public enum HeroState {
//        IDLE,
//        WALK,
//        JUMP,
//        ATTACK,
//        DIE
//    }
//
//    /**
//     * Hero attributes... (open to details)
//     *
//     * @param HeroState enum of states that hero can have
//     */
//    public static final float SIZE = 0.5f;
//    public static final float MAX_HEALTH = 3;
//
//    public Hero(Vector2 pos) {
//        super();
//        this.position.set(pos);
//        this.bounds.x = pos.x;
//        this.bounds.y = pos.y;
//        this.bounds.width = SIZE;
//        this.bounds.height = SIZE;
//        this.state = HeroState.IDLE;
//        this.stateTime = 0f;
//        this.alive = true;
//        this.health = MAX_HEALTH;
//        this.damage = 1f;
//    }
//
//    @Override
//    public void takeDamage(float damage) {
//        this.health -= damage;
//    }
//
//    /**
//     * @return maximum health of actor
//     */
//    @Override
//    public float getMaxHealth() {
//        return MAX_HEALTH;
//    }
//}
