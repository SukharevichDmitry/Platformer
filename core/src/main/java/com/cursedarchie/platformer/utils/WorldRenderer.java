package com.cursedarchie.platformer.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.enemies.logic.states.ChaseState;
import com.cursedarchie.platformer.actors.enemies.logic.states.IdleState;
import com.cursedarchie.platformer.actors.enemies.logic.states.PatrolState;
import com.cursedarchie.platformer.actors.hero.states.combat.AttackState;
import com.cursedarchie.platformer.actors.hero.states.motion.JumpState;
import com.cursedarchie.platformer.actors.hero.states.motion.MoveState;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.actors.enemies.Boss;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.world.GameWorld;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.List;


public class WorldRenderer {

    private static final float CAMERA_WIDTH = 10f;
    private static final float CAMERA_HEIGHT = 7f;
    public static final float RUNNING_FRAME_DURATION = 0.06f;
    public static final float ATTACK_FRAME_DURATION = 0.15f;

    private GameWorld gameWorld;
    private Hero hero;

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

    public WorldRenderer(GameWorld gameWorld, boolean debug) {
        Gdx.app.log("GameWorld Renderer", "Initializing GameWorld Renderer");
        this.gameWorld = gameWorld;
        this.hero = gameWorld.getHero();
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
            drawHeroSensors();
            drawViewCones();
            drawChainDebug();
        }
    }

    private void drawTiles() {
        for (Tile tile : getDrawableTiles((int)CAMERA_WIDTH, (int)CAMERA_HEIGHT)) {
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

        TextureRegion heroFrame = this.hero.isFacingLeft() ? heroIdleLeft : heroIdleRight;
        if (this.hero.getMotionStateMachine().getCurrentState() instanceof MoveState) {
            heroFrame = this.hero.isFacingLeft() ?
                heroWalkLeftAnimation.getKeyFrame(this.hero.getMotionStateTime(), true) :
                heroWalkRightAnimation.getKeyFrame(this.hero.getMotionStateTime(), true);
        } else if (this.hero.getMotionStateMachine().getCurrentState() instanceof JumpState) {
            if (this.hero.getBody().getLinearVelocity().y > 0) {
                heroFrame = this.hero.isFacingLeft() ? heroJumpLeft : heroJumpRight;
            } else {
                heroFrame = this.hero.isFacingLeft() ? heroFallLeft : heroFallRight;
            }
        } else if (this.hero.getCombatStateMachine().getCurrentState() instanceof AttackState) {
            heroFrame = this.hero.isFacingLeft() ?
                heroAttackLeftAnimation.getKeyFrame(this.hero.getCombatStateTime(), false) :
                heroAttackRightAnimation.getKeyFrame(this.hero.getCombatStateTime(), false);
        }

        spriteBatch.draw(
            heroFrame,
            (this.hero.getPosition().x - this.hero.getSize()/2f) * ppuX,
            (this.hero.getPosition().y - this.hero.getSize()/2f)* ppuY,
            this.hero.getSize() * ppuX,
            this.hero.getSize() * ppuY);
    }

    private void drawHeroSensors() {

        debugRenderer.setProjectionMatrix(cam.combined);
        debugRenderer.begin(ShapeType.Line);

        for (Fixture fixture : hero.getBody().getFixtureList()) {

            if ("hero_foot".equals(fixture.getUserData()) ||
                "hero_attack".equals(fixture.getUserData())) {

                if (fixture.isSensor()) {
                    debugRenderer.setColor(0.5f, 0f, 0f, 1f);
                } else {
                    debugRenderer.setColor(Color.RED);
                }

                if (fixture.getShape() instanceof PolygonShape) {
                    PolygonShape polygon = (PolygonShape) fixture.getShape();

                    Vector2[] vertices = new Vector2[4];
                    for (int i = 0; i < 4; i++) vertices[i] = new Vector2();

                    for (int i = 0; i < polygon.getVertexCount(); i++) {
                        polygon.getVertex(i, vertices[i]);

                        vertices[i].rotateRad(hero.getBody().getAngle());
                        vertices[i].add(hero.getBody().getPosition());
                    }

                    for (int i = 0; i < polygon.getVertexCount(); i++) {
                        Vector2 v1 = vertices[i];
                        Vector2 v2 = vertices[(i + 1) % polygon.getVertexCount()];
                        debugRenderer.line(v1.x, v1.y, v2.x, v2.y);
                    }
                }
            }
        }

        debugRenderer.end();
    }

    private void drawHitBar() {

        float maxHealth = this.hero.getMaxHealth();  // максимальное здоровье героя
        float currentHealth = this.hero.getHealth(); // текущее здоровье героя

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
        Array<Enemy> enemies = gameWorld.getLevel().getAliveEnemies();

        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                TextureRegion enemyFrame;

                if (enemy instanceof Boss) {
                    enemyFrame = enemy.isFacingLeft() ? bossIdleLeft : bossIdleRight;
                } else {
                    enemyFrame = enemy.isFacingLeft() ? enemyIdleLeft : enemyIdleRight;
                    if (enemy.getStateMachine().getCurrentState() instanceof com.cursedarchie.platformer.actors.enemies.logic.states.AttackState) {
                        enemyFrame = enemy.isFacingLeft() ?
                            enemyAttackLeftAnimation.getKeyFrame(enemy.getAttackTime(), false) :
                            enemyAttackRightAnimation.getKeyFrame(enemy.getAttackTime(), false);

                        if (enemyAttackLeftAnimation.isAnimationFinished(enemy.getAttackTime())) {
                            enemy.getStateMachine().changeState(new IdleState());
                        }
                    }
                    else if (enemy.getStateMachine().getCurrentState() instanceof ChaseState ||
                        enemy.getStateMachine().getCurrentState() instanceof PatrolState) {
                        enemyFrame = enemy.isFacingLeft() ?
                            enemyWalkLeftAnimation.getKeyFrame(enemy.getMotionStateTime(), true) :
                            enemyWalkRightAnimation.getKeyFrame(enemy.getMotionStateTime(), true);
                    }
                    else if (enemy.getStateMachine().getCurrentState() instanceof IdleState) {
                        enemyFrame = enemy.isFacingLeft() ? enemyIdleLeft : enemyIdleRight;
                    }
                    else if (!enemy.isGrounded()) {
                        if (enemy.getBody().getLinearVelocity().y > 0) {
                            enemyFrame = enemy.isFacingLeft() ? enemyJumpLeft : enemyJumpRight;
                        } else {
                            enemyFrame = enemy.isFacingLeft() ? enemyFallLeft : enemyFallRight;
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
        Array<Enemy> enemies = gameWorld.getLevel().getAliveEnemies();

        debugRenderer.setProjectionMatrix(cam.combined);
        debugRenderer.begin(ShapeType.Line);
        debugRenderer.setColor(new Color(1, 1, 0, 0.7f)); // ярко-желтый, чуть прозрачный


        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) continue;

            Vector2 pos = enemy.getPosition();
            float viewDistance = enemy.getViewDistance();
            float viewAngle = enemy.getViewAngle();

            Vector2 direction = enemy.isFacingLeft() ? new Vector2(-1, 0) : new Vector2(1, 0);
            float baseAngle = direction.angleDeg();

            Vector2 center = new Vector2(pos.x + enemy.getSize() / 2f, pos.y + enemy.getSize() / 2f);

            int segments = 20;
            float angleStep = viewAngle / segments;

            Vector2 leftEdge = new Vector2(viewDistance, 0).setAngleDeg(baseAngle - viewAngle / 2f).add(center);
            Vector2 rightEdge = new Vector2(viewDistance, 0).setAngleDeg(baseAngle + viewAngle / 2f).add(center);

            debugRenderer.line(center.x, center.y, leftEdge.x, leftEdge.y);
            debugRenderer.line(center.x, center.y, rightEdge.x, rightEdge.y);

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
        for (Tile tile : getDrawableTiles((int)CAMERA_WIDTH, (int)CAMERA_HEIGHT)) {
            Rectangle rect = tile.getBounds();
            debugRenderer.setColor(new Color(1, 0, 0, 1));
            debugRenderer.rect(rect.x, rect.y, rect.width, rect.height);
        }

        Rectangle heroRect = this.hero.getBounds();
        debugRenderer.setColor(new Color(0, 1, 0, 1));
        debugRenderer.rect(heroRect.x, heroRect.y,   heroRect.width, heroRect.height);

        Array<Enemy> enemies = gameWorld.getLevel().getAliveEnemies();
        for (Enemy enemy: enemies) {
            Rectangle enemyRect = enemy.getBounds();
            debugRenderer.setColor(new Color(1, 1, 0, 1));
            debugRenderer.rect(enemyRect.x, enemyRect.y, enemyRect.width, enemyRect.height);
        }

        debugRenderer.end();
    }

    public List<Tile> getDrawableTiles(int width, int height) {
        int x = (int) this.hero.getPosition().x - width;
        int y = (int) this.hero.getPosition().y - height;
        if (x < 0) x = 0;
        if (y < 0) y = 0;

        int x2 = x + 2 * width;
        int y2 = y + 2 * height;

        Tile[][] tiles = gameWorld.getTiles();
        int maxX = tiles.length - 1;
        int maxY = tiles[0].length - 1;

        if (x2 > maxX) x2 = maxX;
        if (y2 > maxY) y2 = maxY;

        List<Tile> drawableTiles = new ArrayList<>();
        for (int col = x; col <= x2; col++) {
            for (int row = y; row <= y2; row++) {
                Tile tile = tiles[col][row];
                if (tile != null) {
                    drawableTiles.add(tile);
                }
            }
        }
        return drawableTiles;
    }

    private void drawCollisionBlocks() {
        debugRenderer.setProjectionMatrix(cam.combined);
        debugRenderer.begin(ShapeType.Filled);
        debugRenderer.setColor(new Color(1, 1, 1, 1));
        for (Rectangle rect : this.hero.getCollisionRects()) {
            debugRenderer.rect(rect.x, rect.y, rect.width, rect.height);
        }
        Array<Enemy> enemies = this.gameWorld.getLevel().getAliveEnemies();
        for (Enemy enemy : enemies) {
            for (Rectangle rect : enemy.getCollisionRects()) {
                debugRenderer.rect(rect.x, rect.y, rect.width, rect.height);
            }
        }

        debugRenderer.end();
    }

    private void drawChainDebug() {
        debugRenderer.setProjectionMatrix(cam.combined);
        debugRenderer.begin(ShapeType.Line);
        debugRenderer.setColor(Color.RED);

        Array<Body> bodies = new Array<>();
        gameWorld.getWorld().getBodies(bodies);

        for (Body body : bodies) {
            Object data = body.getUserData();
            if (!(data instanceof Vector2[])) continue;

            Vector2[] vertices = (Vector2[]) data;

            for (int i = 0; i < vertices.length - 1; i++) {
                Vector2 v1 = vertices[i];
                Vector2 v2 = vertices[i + 1];
                debugRenderer.line(v1.x, v1.y, v2.x, v2.y);
            }

            // замкнутые цепи
            if (vertices.length > 1) {
                Vector2 v1 = vertices[vertices.length - 1];
                Vector2 v2 = vertices[0];
                debugRenderer.line(v1.x, v1.y, v2.x, v2.y);
            }
        }

        debugRenderer.end();
    }
}
