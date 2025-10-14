package com.cursedarchie.platformer.levels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.tiles.HalfOfBlock;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.tiles.DefaultBlock;
import com.cursedarchie.platformer.actors.enemies.Boss;
import com.cursedarchie.platformer.actors.enemies.DefaultEnemy;
import com.cursedarchie.platformer.actors.Enemy;

import java.util.Arrays;
import java.util.Iterator;

public class Level {

    private int width = 10;
    private int height = 7;
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

    public Array<Enemy> getAliveEnemies() {
        for (Iterator<Enemy> it = enemies.iterator(); it.hasNext(); ) {
            Enemy enemy = it.next();
            if (!enemy.isAlive()) {
                it.remove();
            }
        }
        return enemies;
    }
    public Tile[][] getTiles() {
        return tiles;
    }
    public Tile getTile(int x, int y) {
        return tiles[x][y];
    }
    public void setTiles(Tile[][] tiles) {
        this.tiles = tiles;
    }

    public Level() {
        Gdx.app.log("Level", "Creating Level");
        this.enemies = new Array<Enemy>();
        this.tiles = new Tile[width][height];
    }

    public void loadLevel1() {
        this.enemies = new Array<Enemy>();

        this.tiles = new Tile[width][height];
        fillBlocks(this.tiles);
        loadTopWalls(this.tiles);
        loadBottomWalls(this.tiles);
        loadLeftWalls(this.tiles);

        this.tiles[2][2] = new DefaultBlock(new Vector2(2, 2));
        this.tiles[3][2] = new DefaultBlock(new Vector2(3, 2));
        this.tiles[3][3] = new DefaultBlock(new Vector2(3, 3));
        this.tiles[4][2] = new HalfOfBlock(new Vector2(4,2));

        this.enemies.add(new DefaultEnemy(new Vector2(7.25f, 4)));
    }

    public void loadLevel2() {
        this.enemies = new Array<Enemy>();

        this.tiles = new Tile[width][height];
        fillBlocks(this.tiles);
        loadTopWalls(this.tiles);
        loadBottomWalls(this.tiles);

        this.tiles[6][3] = new DefaultBlock(new Vector2(6, 3));
        this.tiles[6][4] = new DefaultBlock(new Vector2(6, 4));
        this.tiles[6][5] = new DefaultBlock(new Vector2(6, 5));

        for (int col = 0; col < this.tiles.length; col++) {
            if (col > 2) {
                this.tiles[col][1] = new DefaultBlock(new Vector2(col, 1));
            }
        }

        this.enemies.add(new DefaultEnemy(new Vector2(3.25f, 2)));
        this.enemies.add(new DefaultEnemy(new Vector2(9.25f, 2)));
    }

    public void loadLevel3() {
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
