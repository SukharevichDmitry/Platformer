package com.cursedarchie.platformer.actors.hero.states;

import com.cursedarchie.platformer.actors.hero.Hero;

public interface HeroState {
    void enter(Hero hero);
    void update (Hero hero, float delta);
    void exit (Hero hero);
}
