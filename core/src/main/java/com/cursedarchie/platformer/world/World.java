package com.cursedarchie.platformer.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.tiles.Tile;

import com.cursedarchie.platformer.actors.Hero;
import com.cursedarchie.platformer.levels.Level;

import java.util.ArrayList;
import java.util.List;


public class World {

    Hero hero;
    Level level;
    private int currentLevel;

    Array<Rectangle> heroCollisionRects = new Array<Rectangle>();
    Array<Rectangle> enemyCollisionRects = new Array<Rectangle>();



    public Array<Rectangle> getHeroCollisionRects() {
        return heroCollisionRects;
    }
    public Array<Rectangle> getEnemyCollisionRects() {
        return  enemyCollisionRects;
    }
    public Hero getHero() {
        return hero;
    }
    public Level getLevel() {
        return level;
    }

    public List<Tile> getDrawableTiles(int width, int height) {
        int x = (int)hero.getPosition().x - width;
        int y = (int)hero.getPosition().y - height;
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        int x2 = x + 2 * width;
        int y2 = y + 2 * height;
        if (x2 > level.getWidth()) {
            x2 = level.getWidth() - 1;
        }
        if (y2 > level.getHeight()) {
            y2 = level.getHeight() - 1;
        }

        List<Tile> tiles = new ArrayList<Tile>();
        Tile tile;
        for (int col = x; col <= x2; col++) {
            for (int row = y; row <= y2; row++) {
                tile = level.getTiles()[col][row];
                if (tile != null) {
                    tiles.add(tile);
                }
            }
        }
        return tiles;
    }

    public World() {
        hero = new Hero(new Vector2(1, 1));
        currentLevel = 0;
        level = createLevel(currentLevel);
    }

    private Level createLevel(int levelIndex) {
        Level newLevel = new Level();

        switch (levelIndex) {
            case 0:
                newLevel.loadLevel1();
                break;
            case 1:
                newLevel.loadLevel2();
                break;
            case 2:
                newLevel.loadLevel3();
                break;
            case 3:
                newLevel.loadLevel4();
                break;
            case 4:
                newLevel.loadLevel5();
                break;
        }
        return newLevel;
    }

    public void goToNextLevel() {
        if (currentLevel < 4) {
            currentLevel++;
            level = createLevel(currentLevel);
            hero.setPosition(new Vector2(0.5f, hero.getPosition().y));
        }
    }

    public void goToPreviousLevel() {
        if (currentLevel > 0) {
            currentLevel--;
            level = createLevel(currentLevel);
            hero.setPosition(new Vector2(level.getWidth() - 1, hero.getPosition().y));
        }
    }

}
