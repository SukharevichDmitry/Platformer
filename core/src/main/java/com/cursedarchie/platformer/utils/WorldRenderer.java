package com.cursedarchie.platformer.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.actors.enemies.Boss;
import com.cursedarchie.platformer.actors.Enemy;
import com.cursedarchie.platformer.actors.Hero;
import com.cursedarchie.platformer.actors.Hero.HeroState;
import com.cursedarchie.platformer.actors.Enemy.EnemyState;
import com.cursedarchie.platformer.world.World;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;


public class WorldRenderer {

    private static final float CAMERA_WIDTH = 10f;
    private static final float CAMERA_HEIGHT = 7f;
    public static final float RUNNING_FRAME_DURATION = 0.06f;
    public static final float ATTACK_FRAME_DURATION = 0.15f;

    private World world;
    private OrthographicCamera cam;

    ShapeRenderer debugRenderer = new ShapeRenderer();

    private TextureRegion heroIdleLeft;
    private TextureRegion heroIdleRight;
    private TextureRegion heroJumpLeft;
    private TextureRegion heroJumpRight;
    private TextureRegion heroFallLeft;
    private TextureRegion heroFallRight;

    private Animation<TextureRegion> heroWalkLeftAnimation;
    private Animation<TextureRegion> heroWalkRightAnimation;
    private Animation<TextureRegion> heroAttackLeftAnimation;
    private Animation<TextureRegion> heroAttackRightAnimation;

    private Animation<TextureRegion> enemyWalkLeftAnimation;
    private Animation<TextureRegion> enemyWalkRightAnimation;
    private Animation<TextureRegion> enemyAttackLeftAnimation;
    private Animation<TextureRegion> enemyAttackRightAnimation;


    private TextureRegion enemyIdleLeft;
    private TextureRegion enemyIdleRight;
    private TextureRegion enemyJumpLeft;
    private TextureRegion enemyJumpRight;
    private TextureRegion enemyFallLeft;
    private TextureRegion enemyFallRight;

    private TextureRegion bossIdleLeft;
    private TextureRegion bossIdleRight;

    private SpriteBatch spriteBatch;
    private boolean debug = false;
    private int width;
    private int height;
    private float ppuX;	// pixels per unit on the X axis
    private float ppuY;	// pixels per unit on the Y axis

    public void setSize (int w, int h) {
        this.width = w;
        this.height = h;
        ppuX = (float)width / CAMERA_WIDTH;
        ppuY = (float)height / CAMERA_HEIGHT;
    }
    public boolean isDebug() {
        return debug;
    }
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public WorldRenderer(World world, boolean debug) {
        this.world = world;
        this.cam = new OrthographicCamera(CAMERA_WIDTH, CAMERA_HEIGHT);
        this.cam.position.set(CAMERA_WIDTH / 2f, CAMERA_HEIGHT / 2f, 0);
        this.cam.update();
        this.debug = debug;
        spriteBatch = new SpriteBatch();
        loadTextures();
    }

    private void loadTextures() {
        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("atlases/game.atlas"));
        loadHeroTextures(atlas);
        loadEnemyTextures(atlas);
    }

    private void loadHeroTextures(TextureAtlas atlas) {
        heroIdleLeft = atlas.findRegion("hero");
        heroIdleRight = new TextureRegion(heroIdleLeft);
        heroIdleRight.flip(true, false);

        TextureRegion[] walkLeftFrames = new TextureRegion[3];
        for (int i = 0; i < walkLeftFrames.length; i++) {
            walkLeftFrames[i] = atlas.findRegion("hero" + (i + 1));
        }
        heroWalkLeftAnimation = new Animation<>(RUNNING_FRAME_DURATION, walkLeftFrames);

        TextureRegion[] walkRightFrames = new TextureRegion[3];
        for (int i = 0; i < walkRightFrames.length; i++) {
            walkRightFrames[i] = new TextureRegion(walkLeftFrames[i]);
            walkRightFrames[i].flip(true, false);
        }
        heroWalkRightAnimation = new Animation<>(RUNNING_FRAME_DURATION, walkRightFrames);

        heroJumpLeft = atlas.findRegion("hero");
        heroJumpRight = new TextureRegion(heroJumpLeft);
        heroJumpRight.flip(true, false);
        heroFallLeft = atlas.findRegion("hero2");
        heroFallRight = new TextureRegion(heroFallLeft);
        heroFallRight.flip(true, false);

        TextureRegion[] attackLeftFrames = new TextureRegion[3];
        for (int i = 0; i < attackLeftFrames.length; i++) {
            attackLeftFrames[i] = atlas.findRegion("hero_attack" + (i + 1));
        }
        heroAttackLeftAnimation = new Animation<>(ATTACK_FRAME_DURATION, attackLeftFrames);

        TextureRegion[] attackRightFrames = new TextureRegion[3];
        for (int i = 0; i < attackRightFrames.length; i++) {
            attackRightFrames[i] = new TextureRegion(attackLeftFrames[i]);
            attackRightFrames[i].flip(true, false);
        }
        heroAttackRightAnimation = new Animation<>(ATTACK_FRAME_DURATION, attackRightFrames);

    }

    private void loadEnemyTextures(TextureAtlas atlas) {

        enemyIdleLeft = atlas.findRegion("enemy");
        enemyIdleRight = new TextureRegion(enemyIdleLeft);
        enemyIdleRight.flip(true, false);

        TextureRegion[] walkLeftFrames = new TextureRegion[3];
        for (int i = 0; i < walkLeftFrames.length; i++) {
            walkLeftFrames[i] = atlas.findRegion("enemy" + (i + 1));
        }
        enemyWalkLeftAnimation = new Animation<>(RUNNING_FRAME_DURATION, walkLeftFrames);

        TextureRegion[] walkRightFrames = new TextureRegion[3];
        for (int i = 0; i < walkRightFrames.length; i++) {
            walkRightFrames[i] = new TextureRegion(walkLeftFrames[i]);
            walkRightFrames[i].flip(true, false);
        }
        enemyWalkRightAnimation = new Animation<>(RUNNING_FRAME_DURATION, walkRightFrames);

        enemyJumpLeft = atlas.findRegion("enemy");
        enemyJumpRight = new TextureRegion(enemyJumpLeft);
        enemyJumpRight.flip(true, false);
        enemyFallLeft = atlas.findRegion("enemy2");
        enemyFallRight = new TextureRegion(enemyFallLeft);
        enemyFallRight.flip(true, false);

        TextureRegion[] attackLeftFrames = new TextureRegion[2];
        for (int i = 0; i < attackLeftFrames.length; i++) {
            attackLeftFrames[i] = atlas.findRegion("enemy_attack" + (i + 1));
        }
        enemyAttackLeftAnimation = new Animation<>(ATTACK_FRAME_DURATION, attackLeftFrames);

        TextureRegion[] attackRightFrames = new TextureRegion[2];
        for (int i = 0; i < attackRightFrames.length; i++) {
            attackRightFrames[i] = new TextureRegion(attackLeftFrames[i]);
            attackRightFrames[i].flip(true, false);
        }
        enemyAttackRightAnimation = new Animation<>(ATTACK_FRAME_DURATION, attackRightFrames);


        bossIdleLeft = atlas.findRegion("boss");
        bossIdleRight = new TextureRegion(bossIdleLeft);
        bossIdleRight.flip(true, false);

    }

    public void render() {
        spriteBatch.begin();
            drawTiles();
            drawHero();
            drawEnemy();
        spriteBatch.end();
        drawHitBar();
        if (debug) {
            drawDebug();
            drawCollisionBlocks();
            drawViewCones();
        }
    }

    private void drawTiles() {
        for (Tile tile : world.getDrawableTiles((int)CAMERA_WIDTH, (int)CAMERA_HEIGHT)) {
            spriteBatch.draw(
                tile.getTextureRegion(),
                tile.getPosition().x * ppuX,
                tile.getPosition().y * ppuY,
                tile.getSize() * ppuX,
                tile.getSize() * ppuY
            );
        }
    }

    private void drawHero() {
        Hero hero = world.getHero();

        TextureRegion heroFrame = hero.isFacingLeft() ? heroIdleLeft : heroIdleRight;
        if (hero.getState().equals(HeroState.WALKING)) {
            heroFrame = hero.isFacingLeft() ?
                heroWalkLeftAnimation.getKeyFrame(hero.getStateTime(), true) :
                heroWalkRightAnimation.getKeyFrame(hero.getStateTime(), true);
        } else if (hero.getState().equals(HeroState.JUMPING)) {
            if (hero.getVelocity().y > 0) {
                heroFrame = hero.isFacingLeft() ? heroJumpLeft : heroJumpRight;
            } else {
                heroFrame = hero.isFacingLeft() ? heroFallLeft : heroFallRight;
            }
        } else if (hero.getState().equals(HeroState.ATTACKING)) {
            heroFrame = hero.isFacingLeft() ?
                heroAttackLeftAnimation.getKeyFrame(hero.getStateTime(), false) :
                heroAttackRightAnimation.getKeyFrame(hero.getStateTime(), false);

            if (heroAttackLeftAnimation.isAnimationFinished(hero.getStateTime())) {
                hero.setState(HeroState.IDLE);
            }
        }

        spriteBatch.draw(
            heroFrame,
            hero.getPosition().x * ppuX,
            hero.getPosition().y * ppuY,
            Hero.SIZE * ppuX,
            Hero.SIZE * ppuY);
    }

    private void drawHitBar() {
        Hero hero = world.getHero();

        float maxHealth = hero.getMaxHealth();  // максимальное здоровье героя
        float currentHealth = hero.getHealth(); // текущее здоровье героя

        float healthPercent = currentHealth / maxHealth;

        // Размеры полоски
        float barWidth = 200;  // ширина полоски в пикселях
        float barHeight = 20;  // высота полоски в пикселях

        // Позиция полоски — отступы от верхнего левого угла окна
        float marginLeft = 20;
        float marginTop = 20;

        // Высота экрана в пикселях - нужно учесть, что y=0 в ShapeRenderer — внизу,
        // поэтому позицию по Y считаем от низа:
        float y = height - marginTop - barHeight;
        float x = marginLeft;

        debugRenderer.setProjectionMatrix(spriteBatch.getProjectionMatrix());  // Используем проекцию SpriteBatch — пиксели экрана
        debugRenderer.begin(ShapeType.Filled);

        // Рисуем фон полоски (серый)
        debugRenderer.setColor(Color.DARK_GRAY);
        debugRenderer.rect(x, y, barWidth, barHeight);

        // Рисуем текущий уровень здоровья (зелёный)
        debugRenderer.setColor(Color.GREEN);
        debugRenderer.rect(x, y, barWidth * healthPercent, barHeight);

        debugRenderer.end();
    }



    private void drawEnemy() {
        Array<Enemy> enemies = world.getLevel().getEnemies();

        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                TextureRegion enemyFrame;

                if (enemy instanceof Boss) {
                    // Это босс — используем его текстуры
                    enemyFrame = enemy.isFacingLeft() ? bossIdleLeft : bossIdleRight;
                } else {
                    enemyFrame = enemy.isFacingLeft() ? enemyIdleLeft : enemyIdleRight;
                    if (enemy.getState().equals(EnemyState.WALKING)) {
                        enemyFrame = enemy.isFacingLeft() ?
                            enemyWalkLeftAnimation.getKeyFrame(enemy.getStateTime(), true) :
                            enemyWalkRightAnimation.getKeyFrame(enemy.getStateTime(), true);
                    } else if (enemy.getState().equals(EnemyState.JUMPING)) {
                        if (enemy.getVelocity().y > 0) {
                            enemyFrame = enemy.isFacingLeft() ? enemyJumpLeft : enemyJumpRight;
                        } else {
                            enemyFrame = enemy.isFacingLeft() ? enemyFallLeft : enemyFallRight;
                        }
                    } else if (enemy.getState().equals(EnemyState.ATTACKING)) {
                        enemyFrame = enemy.isFacingLeft() ?
                            enemyAttackLeftAnimation.getKeyFrame(enemy.getStateTime(), false) :
                            enemyAttackRightAnimation.getKeyFrame(enemy.getStateTime(), false);

                        if (enemyAttackLeftAnimation.isAnimationFinished(enemy.getStateTime())) {
                            enemy.setState(EnemyState.IDLE);
                        }
                    }
                }

                spriteBatch.draw(
                    enemyFrame,
                    enemy.getPosition().x * ppuX,
                    enemy.getPosition().y * ppuY,
                    enemy.getSize() * ppuX,
                    enemy.getSize() * ppuY
                );
            }
        }
    }

    private void drawViewCones() {
        Array<Enemy> enemies = world.getLevel().getEnemies();

        debugRenderer.setProjectionMatrix(cam.combined);
        debugRenderer.begin(ShapeType.Line);
        debugRenderer.setColor(new Color(1, 1, 0, 0.7f)); // ярко-желтый, чуть прозрачный


        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) continue;

            Vector2 pos = enemy.getPosition();
            float viewDistance = enemy.getViewDistance();
            float viewAngle = enemy.getViewAngle();

            // Направление взгляда — влево или вправо
            Vector2 direction = enemy.isFacingLeft() ? new Vector2(-1, 0) : new Vector2(1, 0);
            float baseAngle = direction.angleDeg();

            // Центр обзора — в центре спрайта (если нужно)
            Vector2 center = new Vector2(pos.x + enemy.getSize() / 2f, pos.y + enemy.getSize() / 2f);

            int segments = 20; // число сегментов для дуги
            float angleStep = viewAngle / segments;

            // Рисуем линии от центра к краям конуса
            Vector2 leftEdge = new Vector2(viewDistance, 0).setAngleDeg(baseAngle - viewAngle / 2f).add(center);
            Vector2 rightEdge = new Vector2(viewDistance, 0).setAngleDeg(baseAngle + viewAngle / 2f).add(center);

            // Линии крайних лучей конуса
            debugRenderer.line(center.x, center.y, leftEdge.x, leftEdge.y);
            debugRenderer.line(center.x, center.y, rightEdge.x, rightEdge.y);

            // Рисуем дугу между левым и правым краем — соединяем сегменты
            Vector2 prevPoint = leftEdge;
            for (int i = 1; i <= segments; i++) {
                float angle = baseAngle - viewAngle / 2f + i * angleStep;
                Vector2 nextPoint = new Vector2(viewDistance, 0).setAngleDeg(angle).add(center);
                debugRenderer.line(prevPoint.x, prevPoint.y, nextPoint.x, nextPoint.y);
                prevPoint = nextPoint;
            }
        }

        debugRenderer.end();
    }

    private void drawDebug() {
        debugRenderer.setProjectionMatrix(cam.combined);
        debugRenderer.begin(ShapeType.Line);
        for (Tile tile : world.getDrawableTiles((int)CAMERA_WIDTH, (int)CAMERA_HEIGHT)) {
            Rectangle rect = tile.getBounds();
            debugRenderer.setColor(new Color(1, 0, 0, 1));
            debugRenderer.rect(rect.x, rect.y, rect.width, rect.height);
        }

        Hero hero = world.getHero();
        Rectangle heroRect = hero.getBounds();
        debugRenderer.setColor(new Color(0, 1, 0, 1));
        debugRenderer.rect(heroRect.x, heroRect.y,   heroRect.width, heroRect.height);

        Array<Enemy> enemies = world.getLevel().getEnemies();
        for (Enemy enemy: enemies) {
            Rectangle enemyRect = enemy.getBounds();
            debugRenderer.setColor(new Color(1, 1, 0, 1));
            debugRenderer.rect(enemyRect.x, enemyRect.y, enemyRect.width, enemyRect.height);
        }

        debugRenderer.end();
    }

    private void drawCollisionBlocks() {
        debugRenderer.setProjectionMatrix(cam.combined);
        debugRenderer.begin(ShapeType.Filled);
        debugRenderer.setColor(new Color(1, 1, 1, 1));
        for (Rectangle rect : world.getHeroCollisionRects()) {
            debugRenderer.rect(rect.x, rect.y, rect.width, rect.height);
        }
        for (Rectangle rect : world.getEnemyCollisionRects()) {
            debugRenderer.rect(rect.x, rect.y, rect.width, rect.height);
        }
        debugRenderer.end();

    }

}
