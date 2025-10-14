package com.cursedarchie.platformer.actors.hero;

import com.cursedarchie.platformer.actors.Hero;

public interface HeroState {
    void enter(Hero hero);
    void update (Hero hero, float delta);
    void exit (Hero hero);
}
