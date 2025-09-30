package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.tiles.HalfOfBlock;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.tiles.DefaultBlock;
import com.cursedarchie.platformer.actors.enemies.Boss;
import com.cursedarchie.platformer.actors.enemies.DefaultEnemy;
import com.cursedarchie.platformer.actors.Enemy;

import java.util.Arrays;

public class Level {

    private int width;
    private int height;
    private Tile[][] tiles;

    private Array<Enemy> enemies;


    public int getWidth() {
        return width;
    }
    public void setWidth(int width) {
        this.width = width;
    }
    public int getHeight() {
        return height;
    }
    public void setHeight(int height) {
        this.height = height;
    }
    public Array<Enemy> getEnemies() {
        return enemies;
    }
    public Tile[][] getTiles() {
        return tiles;
    }
    public Tile get(int x, int y) {
        return tiles[x][y];
    }
    public void setTiles(Tile[][] tiles) {
        this.tiles = tiles;
    }

    public Level() {
        this.enemies = new Array<>();
    }

    public void loadLevel1() {
        width = 10;
        height = 7;

        enemies = new Array<Enemy>();

        tiles = new Tile[width][height];
        fillBlocks(tiles);
        loadTopWalls(tiles);
        loadBottomWalls(tiles);
        loadLeftWalls(tiles);

        tiles[2][2] = new DefaultBlock(new Vector2(2, 2));
        tiles[3][2] = new DefaultBlock(new Vector2(3, 2));
        tiles[3][3] = new DefaultBlock(new Vector2(3, 3));
        tiles[4][2] = new HalfOfBlock(new Vector2(4,2));

        enemies.add(new DefaultEnemy(new Vector2(7.25f, 4)));
    }

    public void loadLevel2() {
        width = 10;
        height = 7;

        enemies = new Array<Enemy>();

        tiles = new Tile[width][height];
        fillBlocks(tiles);
        loadTopWalls(tiles);
        loadBottomWalls(tiles);

        tiles[6][3] = new DefaultBlock(new Vector2(6, 3));
        tiles[6][4] = new DefaultBlock(new Vector2(6, 4));
        tiles[6][5] = new DefaultBlock(new Vector2(6, 5));

        for (int col = 0; col < tiles.length; col++) {
            if (col > 2) {
                tiles[col][1] = new DefaultBlock(new Vector2(col, 1));
            }
        }

        enemies.add(new DefaultEnemy(new Vector2(3.25f, 2)));
        enemies.add(new DefaultEnemy(new Vector2(9.25f, 2)));
    }

    public void loadLevel3() {
        width = 10;
        height = 7;

        enemies = new Array<Enemy>();

        tiles = new Tile[width][height];
        fillBlocks(tiles);
        loadTopWalls(tiles);
        loadBottomWalls(tiles);

        tiles[0][1] = new DefaultBlock(new Vector2(0, 1));
        tiles[1][1] = new DefaultBlock(new Vector2(1, 1));

        tiles[8][1] = new DefaultBlock(new Vector2(8, 1));
        tiles[9][1] = new DefaultBlock(new Vector2(9, 1));
        tiles[9][2] = new DefaultBlock(new Vector2(9, 2));

        enemies.add(new DefaultEnemy(new Vector2(5.25f, 1)));
    }

    public void loadLevel4() {
        width = 10;
        height = 7;

        enemies = new Array<Enemy>();
        tiles = new Tile[width][height];
        fillBlocks(tiles);
        loadTopWalls(tiles);

        tiles[0][0] = new DefaultBlock(new Vector2(0, 0));
        tiles[1][0] = new DefaultBlock(new Vector2(1, 0));
        tiles[2][0] = new DefaultBlock(new Vector2(2, 0));
        tiles[3][0] = new DefaultBlock(new Vector2(3, 0));
        tiles[7][0] = new DefaultBlock(new Vector2(7, 0));
        tiles[8][0] = new DefaultBlock(new Vector2(8, 0));
        tiles[9][0] = new DefaultBlock(new Vector2(9, 0));

        tiles[0][1] = new DefaultBlock(new Vector2(0, 1));
        tiles[1][1] = new DefaultBlock(new Vector2(1, 1));

        tiles[0][2] =  new DefaultBlock(new Vector2(0, 2));
        tiles[1][2] = new DefaultBlock(new Vector2(1, 2));


    }

    public void loadLevel5() {
        width = 10;
        height = 7;

        enemies = new Array<>();
        tiles = new Tile[width][height];
        fillBlocks(tiles);
        loadBottomWalls(tiles);
        loadRightWalls(tiles);

        tiles[0][height - 1] = new DefaultBlock(new Vector2(0, height - 1));
        tiles[width - 2][height - 1] = new DefaultBlock(new Vector2(width - 2, height - 1));

        enemies.add(new Boss(new Vector2(3, 1)));
    }


    private void fillBlocks(Tile[][] tiles) {
        for (Tile[] tile : tiles) {
            Arrays.fill(tile, null);
        }
    }
    private void loadTopWalls(Tile[][] tiles) {
        for (int col = 0; col < tiles.length; col++) {
            tiles[col][tiles[col].length - 1] = new DefaultBlock(new Vector2(col, tiles[col].length - 1));
        }
    }
    private void loadBottomWalls(Tile[][] tiles) {
        for (int col = 0; col < tiles.length; col++) {
            tiles[col][0] = new DefaultBlock(new Vector2(col, 0));
        }
    }
    private void loadLeftWalls(Tile[][] tiles) {
        for (int row = 0; row < tiles[0].length; row++) {
            tiles[0][row] = new DefaultBlock(new Vector2(0, row));
        }
    }
    private void loadRightWalls(Tile[][] tiles) {
        for (int row = 0; row < tiles[0].length; row++) {
            tiles[tiles.length - 1][row] = new DefaultBlock(new Vector2(tiles.length-1, row));
        }
    }

}
