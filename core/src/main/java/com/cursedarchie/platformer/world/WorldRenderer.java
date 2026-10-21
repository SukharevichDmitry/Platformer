package com.cursedarchie.platformer.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.enemies.states.motion.ChaseState;
import com.cursedarchie.platformer.actors.enemies.states.motion.MotionIdleState;
import com.cursedarchie.platformer.actors.enemies.states.motion.PatrolState;
import com.cursedarchie.platformer.actors.hero.states.combat.AttackState;
import com.cursedarchie.platformer.actors.hero.states.motion.JumpState;
import com.cursedarchie.platformer.actors.hero.states.motion.MoveState;
import com.cursedarchie.platformer.levels.RoomInstance;
import com.cursedarchie.platformer.levels.LocationInstance;
import com.cursedarchie.platformer.levels.VerticalConnection;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.tiles.types.HalfOfBlock;
import com.cursedarchie.platformer.tiles.types.OneWayPlatform;
import com.cursedarchie.platformer.tiles.types.BreakableBlock;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.hero.Hero;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.List;


public class WorldRenderer implements com.badlogic.gdx.utils.Disposable {

    private static final float CAMERA_WIDTH = 10f;
    private static final float CAMERA_HEIGHT = 7f;
    private static final float EDGE_MARGIN = 1f;
    private static final float VERTICAL_FOLLOW_SPEED = 8f;
    public static final float RUNNING_FRAME_DURATION = 0.06f;
    public static final float ATTACK_FRAME_DURATION = 0.12f;

    private final GameWorld gameWorld;
    private final Hero hero;

    private final OrthographicCamera cam;
    private LocationInstance cameraLocation;

    private final ShapeRenderer debugRenderer = new ShapeRenderer();
    private final TextureAtlas atlas;
    private final TextureAtlas ruinsAtlas;
    private final TextureRegion[] wallFaces = new TextureRegion[256];
    private final TextureRegion backgroundStone;
    private final TextureRegion stoneHalf;
    private final TextureRegion platformOneWay;
    private final TextureRegion blockBreakable;
    private final TextureRegion ladderSegment;
    private final TextureRegion ladderCap;

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

    private final SpriteBatch spriteBatch;
    private final boolean debug;
    private int width;
    private int height;

    public void setSize (int w, int h) {
        this.width = w;
        this.height = h;
    }

    public WorldRenderer(GameWorld gameWorld, boolean debug) {
        Gdx.app.log("GameWorld Renderer", "Initializing GameWorld Renderer");
        this.gameWorld = gameWorld;
        this.hero = gameWorld.getHero();
        Vector2 heroPos = hero.getPosition();
        this.cam = new OrthographicCamera(CAMERA_WIDTH, CAMERA_HEIGHT);
        this.cam.position.set(heroPos.x, heroPos.y, 0);
        updateCamera(0f);
        this.debug = debug;
        spriteBatch = new SpriteBatch();
        atlas = new TextureAtlas(Gdx.files.internal("atlases/game.atlas"));
        ruinsAtlas = new TextureAtlas(Gdx.files.internal("atlases/ruins-tiles.atlas"));
        for (int i = 0; i < wallFaces.length; i++) {
            String name = "wall_mask_" + (i & 15);
            if (i >= 16) name += "_corner_" + (i >> 4);
            wallFaces[i] = requiredRegion(ruinsAtlas, name);
        }
        backgroundStone = requiredRegion(ruinsAtlas, "wall_center");
        stoneHalf = ruinsAtlas.findRegion("stone_half");
        platformOneWay = ruinsAtlas.findRegion("platform_oneway");
        blockBreakable = ruinsAtlas.findRegion("block_breakable");
        ladderSegment = ruinsAtlas.findRegion("ladder_segment");
        ladderCap = ruinsAtlas.findRegion("ladder_cap");
        loadTextures();
    }

    private static TextureRegion requiredRegion(TextureAtlas atlas, String name) {
        TextureRegion region = atlas.findRegion(name);
        if (region == null) throw new IllegalArgumentException("Missing ruins atlas region: " + name);
        return region;
    }

    private void loadTextures() {
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

    public void render(float delta) {
        updateCamera(delta);

        spriteBatch.setProjectionMatrix(cam.combined);

        spriteBatch.begin();
            drawBackgroundMasonry();
            drawTiles();
            drawClimbShafts();
            drawHero();
            drawEnemy();
            drawDefeatEffects();
        spriteBatch.end();

        if (debug) {
            drawDebug();
            drawHeroSensors();
            drawEnemySensors();
            drawViewCones();
            drawChainDebug();
        }

        drawHitBar();
    }

    private void drawClimbShafts() {
        for (VerticalConnection shaft : gameWorld.getLocation().getVerticalConnections()) {
            // This is visual-only: the narrow climb grip is owned by VerticalConnection.
            float left = shaft.centerX - 0.28f;
            float bottom = shaft.bottomY - 0.45f;
            float top = shaft.topY + 0.35f;
            for (float y = bottom; y < top - 0.01f; y += 0.75f)
                spriteBatch.draw(ladderSegment, left, y, 0.56f, Math.min(0.75f, top - y));
            spriteBatch.draw(ladderCap, left, bottom - 0.05f, 0.56f, 0.35f);
            spriteBatch.draw(ladderCap, left, top - 0.28f, 0.56f, 0.35f);
        }
    }

    /** Dark, non-solid infill hides the blank void behind room walls and doors. */
    private void drawBackgroundMasonry() {
        int left = (int) Math.floor(getVisibleLeftEdge()) - 1;
        int right = (int) Math.ceil(getVisibleRightEdge()) + 1;
        int bottom = (int) Math.floor(getVisibleBottomEdge()) - 1;
        int top = (int) Math.ceil(getVisibleTopEdge()) + 1;
        for (int x = left; x < right; x++) {
            for (int y = bottom; y < top; y++) {
                boolean withinRoom = false;
                float nearestRoom = Float.POSITIVE_INFINITY;
                for (RoomInstance room : gameWorld.getLocation().getRooms()) {
                    float distanceX = Math.max(room.getWorldX() - (x + 0.5f),
                        Math.max((x + 0.5f) - (room.getWorldX() + room.getWidth()), 0f));
                    float distanceY = Math.max(room.getWorldY() - (y + 0.5f),
                        Math.max((y + 0.5f) - (room.getWorldY() + room.getHeight()), 0f));
                    nearestRoom = Math.min(nearestRoom, (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY));
                    if (distanceX == 0f && distanceY == 0f) {
                        withinRoom = true;
                        break;
                    }
                }
                // Outside the playable rooms, masonry continues visually into
                // darkness. It remains purely decorative, never a hidden wall.
                float shade = withinRoom ? 0.25f : nearestRoom < 1.5f ? 0.58f :
                    nearestRoom < 3.5f ? 0.43f : 0.28f;
                spriteBatch.setColor(shade, shade * 0.94f, shade, 1f);
                spriteBatch.draw(backgroundStone, x, y, 1f, 1f);
            }
        }
        spriteBatch.setColor(Color.WHITE);
    }

    private void updateCamera(float delta) {

        Vector2 heroPos = hero.getPosition();
        float levelLeft = Float.POSITIVE_INFINITY;
        float levelRight = Float.NEGATIVE_INFINITY;
        float levelBottom = Float.POSITIVE_INFINITY;
        float levelTop = Float.NEGATIVE_INFINITY;
        for (RoomInstance room : gameWorld.getLocation().getRooms()) {
            levelLeft = Math.min(levelLeft, room.getWorldX());
            levelRight = Math.max(levelRight, room.getWorldX() + room.getWidth());
            levelBottom = Math.min(levelBottom, room.getWorldY());
            levelTop = Math.max(levelTop, room.getWorldY() + room.getHeight());
        }
        float cameraX = heroPos.x;
        float cameraY = heroPos.y;
        if (levelRight != Float.NEGATIVE_INFINITY) {
            float halfWidth = cam.viewportWidth * cam.zoom / 2f;
            float halfHeight = cam.viewportHeight * cam.zoom / 2f;
            cameraX = clampCamera(heroPos.x,
                levelLeft - EDGE_MARGIN + halfWidth,
                levelRight + EDGE_MARGIN - halfWidth);
            cameraY = clampCamera(heroPos.y,
                levelBottom - EDGE_MARGIN + halfHeight,
                levelTop + EDGE_MARGIN - halfHeight);
        }

        // Never pan through empty space on a location transition. Only vertical
        // movement within one location is eased; horizontal follow stays responsive.
        boolean locationChanged = cameraLocation != gameWorld.getLocation();
        cameraLocation = gameWorld.getLocation();
        float smoothY = locationChanged ? cameraY : cam.position.y +
            (cameraY - cam.position.y) * (1f - (float) Math.exp(-VERTICAL_FOLLOW_SPEED * Math.max(0f, delta)));
        cam.position.set(cameraX, smoothY, 0);

        cam.update();
    }

    private static float clampCamera(float target, float min, float max) {
        return min <= max
            ? Math.max(min, Math.min(target, max))
            : (min + max) / 2f;
    }

    public float getVisibleRightEdge() {
        return cam.position.x + cam.viewportWidth * cam.zoom / 2f;
    }

    public float getVisibleLeftEdge() {
        return cam.position.x - cam.viewportWidth * cam.zoom / 2f;
    }

    public float getVisibleTopEdge() {
        return cam.position.y + cam.viewportHeight * cam.zoom / 2f;
    }

    public float getVisibleBottomEdge() {
        return cam.position.y - cam.viewportHeight * cam.zoom / 2f;
    }

    private void drawTiles() {

        for (RoomInstance room : gameWorld.getLocation().getRooms()) {

            if (!isRoomVisible(room)) continue;

            Tile[][] tiles = room.getTiles();

            float roomX = room.getWorldX();
            float roomY = room.getWorldY();

            for (int x = 0; x < tiles.length; x++) {
                for (int y = 0; y < tiles[x].length; y++) {

                    Tile tile = tiles[x][y];
                    if (tile == null) continue;

                    Rectangle bounds = tile.getBounds();

                    float worldX = roomX + bounds.x;
                    float worldY = roomY + bounds.y;

                    if (tile instanceof OneWayPlatform) {
                        // The wood strip occupies the top 12/64 of its image cell,
                        // matching the fixture's top 0.18 world units.
                        spriteBatch.draw(platformOneWay, worldX, roomY + y, 1f, 1f);
                    } else if (tile instanceof HalfOfBlock) {
                        spriteBatch.draw(stoneHalf, worldX, roomY + y, 1f, 1f);
                    } else if (tile instanceof BreakableBlock) {
                        spriteBatch.draw(blockBreakable, worldX, worldY, bounds.width, bounds.height);
                    } else {
                        drawDirectionalStone(room, x, y, worldX, worldY, bounds.width, bounds.height);
                    }
                }
            }
        }
    }

    private void drawDirectionalStone(RoomInstance room, int x, int y,
                                    float drawX, float drawY, float width, float height) {
        // Only empty space inside a room can illuminate a stone face.
        // World coordinates let adjacent rooms share the same stone edges.
        int worldX = Math.round(room.getWorldX()) + x;
        int worldY = Math.round(room.getWorldY()) + y;
        int lighting = StoneLighting.mask(gameWorld.getLocation().getRooms(), worldX, worldY);
        spriteBatch.draw(wallFaces[lighting], drawX, drawY, width, height);
    }

    private void drawHero() {

        TextureRegion heroFrame = this.hero.isFacingLeft() ? heroIdleLeft : heroIdleRight;
        if (this.hero.getCombatStateMachine().getCurrentState() instanceof AttackState) {
            heroFrame = this.hero.isFacingLeft() ?
                heroAttackLeftAnimation.getKeyFrame(this.hero.getCombatStateTime(), false) :
                heroAttackRightAnimation.getKeyFrame(this.hero.getCombatStateTime(), false);
        } else if (this.hero.getMotionStateMachine().getCurrentState() instanceof MoveState) {
            heroFrame = this.hero.isFacingLeft() ?
                heroWalkLeftAnimation.getKeyFrame(this.hero.getMotionStateTime(), true) :
                heroWalkRightAnimation.getKeyFrame(this.hero.getMotionStateTime(), true);
        } else if (this.hero.getMotionStateMachine().getCurrentState() instanceof JumpState) {
            if (this.hero.getBody().getLinearVelocity().y > 0) {
                heroFrame = this.hero.isFacingLeft() ? heroJumpLeft : heroJumpRight;
            } else {
                heroFrame = this.hero.isFacingLeft() ? heroFallLeft : heroFallRight;
            }
        }

        if (hero.getInvulnerabilityRemaining() > 0f) {
            spriteBatch.setColor(1f, 0.65f, 0.65f, 1f);
        }
        spriteBatch.draw(
            heroFrame,
            (this.hero.getPosition().x - this.hero.getSize()/2f),
            (this.hero.getPosition().y - this.hero.getSize()/2f),
            this.hero.getSize(),
            this.hero.getSize()
        );
        spriteBatch.setColor(Color.WHITE);
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

    private void drawEnemySensors() {

        Array<Enemy> enemies = gameWorld.getAliveEnemies();

        debugRenderer.setProjectionMatrix(cam.combined);
        debugRenderer.begin(ShapeType.Line);

        for (Enemy enemy : enemies) {
            for (Fixture fixture : enemy.getBody().getFixtureList()) {

                if ("enemy_foot".equals(fixture.getUserData()) ||
                    "enemy_attack".equals(fixture.getUserData())) {

                    if (fixture.isSensor()) {
                        debugRenderer.setColor(1, 1, 0, 1f);
                    } else {
                        debugRenderer.setColor(Color.RED);
                    }

                    if (fixture.getShape() instanceof PolygonShape) {
                        PolygonShape polygon = (PolygonShape) fixture.getShape();

                        Vector2[] vertices = new Vector2[polygon.getVertexCount()];
                        for (int i = 0; i < vertices.length; i++) {
                            vertices[i] = new Vector2();
                        }

                        for (int i = 0; i < polygon.getVertexCount(); i++) {
                            polygon.getVertex(i, vertices[i]);

                            // ВАЖНО: здесь должен быть enemy, а не hero
                            vertices[i].rotateRad(enemy.getBody().getAngle());
                            vertices[i].add(enemy.getBody().getPosition());
                        }

                        for (int i = 0; i < polygon.getVertexCount(); i++) {
                            Vector2 v1 = vertices[i];
                            Vector2 v2 = vertices[(i + 1) % polygon.getVertexCount()];
                            debugRenderer.line(v1.x, v1.y, v2.x, v2.y);
                        }
                    }
                }
            }
        }

        debugRenderer.end();
    }

    private void drawHitBar() {

        float maxHealth = hero.getMaxHealth();
        float currentHealth = hero.getHealth();
        float healthPercent = currentHealth / maxHealth;

        float barWidth = 200;
        float barHeight = 20;

        float marginLeft = 20;
        float marginTop = 20;

        float x = marginLeft;
        float y = height - marginTop - barHeight;

        debugRenderer.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, width, height));

        debugRenderer.begin(ShapeType.Filled);

        debugRenderer.setColor(Color.DARK_GRAY);
        debugRenderer.rect(x, y, barWidth, barHeight);

        debugRenderer.setColor(Color.GREEN);
        debugRenderer.rect(x, y, barWidth * healthPercent, barHeight);

        debugRenderer.end();
        debugRenderer.setProjectionMatrix(cam.combined);
    }



    private void drawEnemy() {
        Array<Enemy> enemies = gameWorld.getAliveEnemies();

        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                TextureRegion enemyFrame;


                enemyFrame = enemy.isFacingLeft() ? enemyIdleLeft : enemyIdleRight;
                if (enemy.getCombatStateMachine().getCurrentState() instanceof com.cursedarchie.platformer.actors.enemies.states.combat.AttackState) {
                    float shownFrame = enemy.getCombatStateTime() < enemy.getAttackStartup()
                        ? 0f : ATTACK_FRAME_DURATION;
                    enemyFrame = enemy.isFacingLeft() ?
                        enemyAttackLeftAnimation.getKeyFrame(shownFrame, false) :
                        enemyAttackRightAnimation.getKeyFrame(shownFrame, false);
                }
                else if ((enemy.getMotionStateMachine().getCurrentState() instanceof ChaseState ||
                    enemy.getMotionStateMachine().getCurrentState() instanceof PatrolState) &&
                    Math.abs(enemy.getBody().getLinearVelocity().x) > 0.1f) {
                    enemyFrame = enemy.isFacingLeft() ?
                        enemyWalkLeftAnimation.getKeyFrame(enemy.getMotionStateTime(), true) :
                        enemyWalkRightAnimation.getKeyFrame(enemy.getMotionStateTime(), true);
                }
                else if (enemy.getMotionStateMachine().getCurrentState() instanceof MotionIdleState) {
                    enemyFrame = enemy.isFacingLeft() ? enemyIdleLeft : enemyIdleRight;
                }
                else if (!enemy.isGrounded()) {
                    if (enemy.getBody().getLinearVelocity().y > 0) {
                        enemyFrame = enemy.isFacingLeft() ? enemyJumpLeft : enemyJumpRight;
                    } else {
                        enemyFrame = enemy.isFacingLeft() ? enemyFallLeft : enemyFallRight;
                    }
                }

                if (enemy.getCombatStateMachine().getCurrentState() instanceof
                    com.cursedarchie.platformer.actors.enemies.states.combat.AttackState &&
                    enemy.getCombatStateTime() < enemy.getAttackStartup()) {
                    spriteBatch.setColor(1f, 0.6f, 0.6f, 1f);
                }
                spriteBatch.draw(
                    enemyFrame,
                    (enemy.getPosition().x - enemy.getSize()/2f),
                    (enemy.getPosition().y - enemy.getSize()/2f),
                    enemy.getSize(),
                    enemy.getSize()
                );
                spriteBatch.setColor(Color.WHITE);
            }
        }
    }

    private void drawDefeatEffects() {
        for (GameWorld.DefeatEffect effect : gameWorld.getDefeatEffects()) {
            float progress = effect.getProgress();
            TextureRegion frame = effect.boss
                ? (effect.facingLeft ? bossIdleLeft : bossIdleRight)
                : (effect.facingLeft ? enemyIdleLeft : enemyIdleRight);
            spriteBatch.setColor(1f, 0.7f + 0.3f * progress, 0.7f + 0.3f * progress,
                1f - progress);
            spriteBatch.draw(frame, effect.x - effect.size / 2f,
                effect.y - effect.size / 2f + 0.15f * progress, effect.size, effect.size);
        }
        spriteBatch.setColor(Color.WHITE);
    }

    private void drawViewCones() {
        Array<Enemy> enemies = gameWorld.getAliveEnemies();

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

            Vector2 center = new Vector2(pos.x, pos.y);
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

        for (RoomInstance room : gameWorld.getLocation().getRooms()) {

            float roomX = room.getWorldX();
            float roomY = room.getWorldY();

            Tile[][] tiles = room.getTiles();

            for (int x = 0; x < tiles.length; x++) {
                for (int y = 0; y < tiles[x].length; y++) {

                    Tile tile = tiles[x][y];
                    if (tile == null) continue;

                    Rectangle bounds = tile.getBounds();

                    float worldX = roomX + bounds.x;
                    float worldY = roomY + bounds.y;

                    if (!inDebugRange(worldX, worldY)) continue;

                    debugRenderer.rect(
                        worldX,
                        worldY,
                        bounds.width,
                        bounds.height
                    );
                }
            }
        }

        debugRenderer.end();
    }

    private boolean inDebugRange(float x, float y) {
        Vector2 hp = hero.getPosition();

        float dx = hp.x - x;
        float dy = hp.y - y;

        return dx * dx + dy * dy <= 9f;
    }

    private boolean isRoomVisible(RoomInstance room) {

        float left =
            cam.position.x - cam.viewportWidth;

        float right =
            cam.position.x + cam.viewportWidth;

        float bottom =
            cam.position.y - cam.viewportHeight;

        float top =
            cam.position.y + cam.viewportHeight;

        return
            room.getWorldX() < right &&
                room.getWorldX() + room.getWidth() > left &&
                room.getWorldY() < top &&
                room.getWorldY() + room.getHeight() > bottom;
    }

    private void drawChainDebug() {
        debugRenderer.setProjectionMatrix(cam.combined);
        debugRenderer.begin(ShapeType.Line);
        debugRenderer.setColor(Color.RED);

        Array<Body> bodies = new Array<>();
        gameWorld.getWorld().getBodies(bodies);

        Vector2 center = new Vector2();

        for (Body body : bodies) {
            Object data = body.getUserData();
            if (!(data instanceof Vector2[])) continue;

            Vector2[] vertices = (Vector2[]) data;

            center.set(0, 0);
            for (Vector2 v : vertices) center.add(v);
            center.scl(1f / vertices.length);

            if (!inDebugRange(center.x, center.y)) continue;

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
    @Override
    public void dispose() {
        spriteBatch.dispose();
        debugRenderer.dispose();
        ruinsAtlas.dispose();
        atlas.dispose();
    }
}
