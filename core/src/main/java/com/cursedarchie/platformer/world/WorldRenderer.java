package com.cursedarchie.platformer.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
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
import com.cursedarchie.platformer.levels.wrappers.InteractionData;
import com.cursedarchie.platformer.tiles.Tile;
import com.cursedarchie.platformer.tiles.TileMetrics;
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

    private static final float CAMERA_WIDTH = 20f;
    private static final float CAMERA_HEIGHT = 14f;
    private static final float BACKDROP_HEIGHT = 10.4f;
    private static final float BACKDROP_OVERLAP = .27f;
    private static final float EDGE_MARGIN = 2f;
    private static final float RUINS_FLOOR_CAMERA_LIFT = 1.4f;
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
    private final Texture bandageSprite;
    private final Texture noteSprite;
    private final Texture keeperSprite;
    private final Texture sceneryPixel;
    private final Texture shelterBackdrop;
    private final Texture redBackdrop;
    private final Texture inkBackdrop;
    private final Texture mechanicalBackdrop;
    private final TextureRegion[][] sceneryRegions;
    private final Texture wardenSprite;
    private final Texture scavengerSprite;
    private final Texture redCollectorSprite;
    private final RoadSurfaceRenderer roadSurface;
    private final ItemLabelFont itemLabelFont;
    private static final Color BANDAGE_LABEL = new Color(0.90f, 0.85f, 0.70f, 1f);
    private static final Color NOTE_LABEL = new Color(0.94f, 0.76f, 0.46f, 1f);

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
    private final BitmapFont damageFont;
    private final GlyphLayout damageLayout = new GlyphLayout();
    private final boolean debug;
    private int width;
    private int height;

    public void setSize (int w, int h) {
        this.width = w;
        this.height = h;
        if (w > 0 && h > 0) {
            cam.viewportWidth = CAMERA_HEIGHT * w / (float) h;
            cam.viewportHeight = CAMERA_HEIGHT;
            updateCamera(0f);
        }
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
        damageFont = new BitmapFont();
        damageFont.getData().setScale(0.05f);
        // World-space glyphs are much smaller than one world unit. Integer vertex
        // snapping can collapse them into zero-sized quads at this scale.
        damageFont.setUseIntegerPositions(false);
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
        bandageSprite = new Texture(Gdx.files.internal("interactions/field-bandage.png"));
        noteSprite = new Texture(Gdx.files.internal("interactions/sealed-note.png"));
        keeperSprite = createKeeperSprite();
        shelterBackdrop = fadedBackdrop("scenery/shelter.png");
        redBackdrop = fadedBackdrop("scenery/red-street.png");
        inkBackdrop = fadedBackdrop("scenery/ink-street.png");
        mechanicalBackdrop = fadedBackdrop("scenery/mechanical-street.png");
        sceneryRegions = new TextureRegion[][] {
            sceneryVariants(shelterBackdrop), sceneryVariants(redBackdrop),
            sceneryVariants(inkBackdrop), sceneryVariants(mechanicalBackdrop)
        };
        wardenSprite = pixelTexture("characters/shelter-warden.png");
        scavengerSprite = pixelTexture("characters/scavenger.png");
        redCollectorSprite = pixelTexture("characters/red-collector.png");
        roadSurface = new RoadSurfaceRenderer();
        Pixmap scenery = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        scenery.setColor(Color.WHITE);
        scenery.fill();
        sceneryPixel = new Texture(scenery);
        scenery.dispose();
        bandageSprite.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        noteSprite.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        itemLabelFont = new ItemLabelFont();
        loadTextures();
    }

    private static TextureRegion requiredRegion(TextureAtlas atlas, String name) {
        TextureRegion region = atlas.findRegion(name);
        if (region == null) throw new IllegalArgumentException("Missing ruins atlas region: " + name);
        return region;
    }

    private static Texture pixelTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    private static Texture fadedBackdrop(String path) {
        Pixmap pixels = new Pixmap(Gdx.files.internal(path));
        int width = pixels.getWidth();
        int height = pixels.getHeight();
        float sideFade = width * .18f;
        float topFade = height * .20f;
        for (int y = 0; y < height; y++) {
            float vertical = Math.min(1f, y / topFade);
            for (int x = 0; x < width; x++) {
                float horizontal = Math.min(1f,
                    Math.min(x, width - 1 - x) / sideFade);
                int pixel = pixels.getPixel(x, y);
                int alpha = Math.round((pixel & 255) * horizontal * vertical);
                pixels.drawPixel(x, y, (pixel & 0xffffff00) | alpha);
            }
        }
        Texture texture = new Texture(pixels);
        pixels.dispose();
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    private static TextureRegion[] sceneryVariants(Texture texture) {
        TextureRegion[] variants = new TextureRegion[2];
        // The source paintings include a road. It is now drawn by macro modules,
        // so keep only their buildings, shelter and sky above the ground line.
        int sceneryHeight = Math.round(texture.getHeight() * .76f);
        variants[0] = new TextureRegion(texture, 0, 0, texture.getWidth(), sceneryHeight);
        variants[1] = new TextureRegion(texture, 0, 0, texture.getWidth(), sceneryHeight);
        variants[1].flip(true, false);
        return variants;
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
            if ("start".equals(gameWorld.getLocation().getLocation()) ||
                "ruins".equals(gameWorld.getLocation().getLocation())) drawOpeningScenery();
            else drawBackgroundMasonry();
            drawTiles();
            roadSurface.draw(gameWorld.getLocation(), spriteBatch,
                getVisibleLeftEdge(), getVisibleRightEdge());
            drawClimbShafts();
            drawInteractions();
            drawHero();
            drawEnemy();
            drawDefeatEffects();
            drawDamageIndicators();
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
            float left = shaft.centerX - 0.56f;
            float bottom = shaft.bottomY - 0.9f;
            float top = shaft.topY + 0.7f;
            for (float y = bottom; y < top - 0.02f; y += 1.5f)
                spriteBatch.draw(ladderSegment, left, y, 1.12f, Math.min(1.5f, top - y));
            spriteBatch.draw(ladderCap, left, bottom - 0.1f, 1.12f, 0.7f);
            spriteBatch.draw(ladderCap, left, top - 0.56f, 1.12f, 0.7f);
        }
    }

    private void drawInteractions() {
        RoomInstance room = gameWorld.getCurrentRoom();
        if (room == null) return;
        InteractionData nearby = gameWorld.getNearbyInteraction();
        for (InteractionData interaction : room.getInteractions()) {
            if (gameWorld.isInteractionConsumed(room, interaction)) continue;
            float x = room.getWorldX() + interaction.position.x;
            float y = room.getWorldY() + interaction.position.y;
            if ("npc".equals(interaction.kind)) {
                spriteBatch.draw("warden".equals(interaction.visualId) ? wardenSprite : keeperSprite,
                    x - .7f, y - 1f, 1.4f, 2.4f);
                spriteBatch.setColor(Color.WHITE);
                if (interaction == nearby)
                    itemLabelFont.drawCentered(spriteBatch, "E", x, y + 1.96f, NOTE_LABEL);
                continue;
            }
            boolean bandage = "field_bandage".equals(interaction.visualId);
            spriteBatch.draw(bandage ? bandageSprite : noteSprite,
                x - 0.4f, y - 0.36f, 0.8f, 0.8f);
            if (interaction == nearby) {
                Color labelColor = bandage ? BANDAGE_LABEL : NOTE_LABEL;
                itemLabelFont.drawCentered(spriteBatch, "E", x, y + 1.04f, labelColor);
                itemLabelFont.drawCentered(spriteBatch, bandage ? "BANDAGE" : "NOTE",
                    x, y + 0.60f, labelColor);
            }
        }
    }

    private static Texture createKeeperSprite() {
        Pixmap pixels = new Pixmap(16, 24, Pixmap.Format.RGBA8888);
        pixels.setColor(.13f, .18f, .22f, 1f);
        pixels.fillRectangle(4, 5, 8, 19);
        pixels.setColor(.33f, .44f, .46f, 1f);
        pixels.fillRectangle(3, 3, 10, 10);
        pixels.setColor(.76f, .65f, .49f, 1f);
        pixels.fillRectangle(5, 6, 6, 6);
        pixels.setColor(.13f, .18f, .22f, 1f);
        pixels.fillRectangle(4, 2, 8, 4);
        pixels.setColor(.94f, .75f, .40f, 1f);
        pixels.drawPixel(6, 9);
        pixels.drawPixel(9, 9);
        Texture texture = new Texture(pixels);
        pixels.dispose();
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    /** Story-themed scenery is visual only and stays behind the physical tile layer. */
    private void drawOpeningScenery() {
        float left = getVisibleLeftEdge() - 2f;
        float bottom = getVisibleBottomEdge() - 2f;
        float viewWidth = getVisibleRightEdge() - left + 2f;
        float viewHeight = getVisibleTopEdge() - bottom + 2f;
        fillScenery(left, bottom, viewWidth, viewHeight, .17f, .19f, .23f);
        for (RoomInstance room : gameWorld.getLocation().getRooms()) {
            int theme = backdropIndex(room);
            float panelWidth = BACKDROP_HEIGHT * sceneryRegions[theme][0].getRegionWidth() /
                sceneryRegions[theme][0].getRegionHeight();
            float stride = panelWidth * (1f - BACKDROP_OVERLAP);
            int panelCount = Math.max(1, (int) Math.ceil((room.getWidth() + panelWidth) / stride));
            spriteBatch.setColor(.58f, .59f, .65f, 1f);
            for (int panel = 0; panel < panelCount; panel++) {
                float panelX = room.getWorldX() - panelWidth * .5f + panel * stride;
                if (panelX > getVisibleRightEdge() || panelX + panelWidth < getVisibleLeftEdge()) continue;
                int variant = Math.floorMod(room.getRoomId() * 3 + panel,
                    sceneryRegions[theme].length);
                spriteBatch.draw(sceneryRegions[theme][variant], panelX,
                    room.getWorldY() + 2f, panelWidth, BACKDROP_HEIGHT);
            }
        }
        spriteBatch.setColor(Color.WHITE);
    }

    private int backdropIndex(RoomInstance room) {
        String theme = room.getSceneryTheme();
        if ("shelter".equals(theme)) return 0;
        if ("red".equals(theme)) return 1;
        if ("ink".equals(theme)) return 2;
        if ("mechanical".equals(theme)) return 3;
        switch (Math.floorMod(room.getRoomId(), 3)) {
            case 0: return 1;
            case 1: return 2;
            default: return 3;
        }
    }

    private void fillScenery(float x, float y, float width, float height,
                             float red, float green, float blue) {
        spriteBatch.setColor(red, green, blue, 1f);
        spriteBatch.draw(sceneryPixel, x, y, width, height);
    }

    /** Dark, non-solid infill hides the blank void behind room walls and doors. */
    private void drawBackgroundMasonry() {
        int left = (int) Math.floor(getVisibleLeftEdge()) - 2;
        int right = (int) Math.ceil(getVisibleRightEdge()) + 2;
        int bottom = (int) Math.floor(getVisibleBottomEdge()) - 2;
        int top = (int) Math.ceil(getVisibleTopEdge()) + 2;
        for (int x = left; x < right; x++) {
            for (int y = bottom; y < top; y++) {
                float centerX = (x + 0.5f) * TileMetrics.SIZE;
                float centerY = (y + 0.5f) * TileMetrics.SIZE;
                boolean withinRoom = false;
                float nearestRoom = Float.POSITIVE_INFINITY;
                for (RoomInstance room : gameWorld.getLocation().getRooms()) {
                    float distanceX = Math.max(room.getWorldX() - centerX,
                        Math.max(centerX - (room.getWorldX() + room.getWidth()), 0f));
                    float distanceY = Math.max(room.getWorldY() - centerY,
                        Math.max(centerY - (room.getWorldY() + room.getHeight()), 0f));
                    nearestRoom = Math.min(nearestRoom, (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY));
                    if (distanceX == 0f && distanceY == 0f) {
                        withinRoom = true;
                        break;
                    }
                }
                // Outside the playable rooms, masonry continues visually into
                // darkness. It remains purely decorative, never a hidden wall.
                float shade = withinRoom ? 0.25f : nearestRoom < 3f ? 0.58f :
                    nearestRoom < 7f ? 0.43f : 0.28f;
                spriteBatch.setColor(shade, shade * 0.94f, shade, 1f);
                spriteBatch.draw(backgroundStone, x * TileMetrics.SIZE, y * TileMetrics.SIZE,
                    TileMetrics.SIZE, TileMetrics.SIZE);
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
            String locationId = gameWorld.getLocation().getLocation();
            float floorLift = "start".equals(locationId) || "ruins".equals(locationId)
                ? RUINS_FLOOR_CAMERA_LIFT : 0f;
            cameraY = clampCamera(heroPos.y,
                levelBottom - EDGE_MARGIN + floorLift + halfHeight,
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
                    if (RoadSurfaceRenderer.covers(room, x, y)) continue;

                    Rectangle bounds = tile.getBounds();

                    float worldX = roomX + bounds.x;
                    float worldY = roomY + bounds.y;

                    if (tile instanceof OneWayPlatform) {
                        // The wood strip occupies the top 12/64 of its image cell,
                        // matching the fixture's top 0.18 world units.
                        spriteBatch.draw(platformOneWay, worldX, roomY + y * TileMetrics.SIZE,
                            TileMetrics.SIZE, TileMetrics.SIZE);
                    } else if (tile instanceof HalfOfBlock) {
                        spriteBatch.draw(stoneHalf, worldX, roomY + y * TileMetrics.SIZE,
                            TileMetrics.SIZE, TileMetrics.SIZE);
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

        if (gameWorld.hasRecentDamage(hero)) {
            spriteBatch.setColor(1f, 0.35f, 0.35f, 1f);
        } else if (hero.getInvulnerabilityRemaining() > 0f) {
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

                if (gameWorld.hasRecentDamage(enemy)) {
                    spriteBatch.setColor(1f, 0.35f, 0.35f, 1f);
                } else if (enemy.getCombatStateMachine().getCurrentState() instanceof
                    com.cursedarchie.platformer.actors.enemies.states.combat.AttackState &&
                    enemy.getCombatStateTime() < enemy.getAttackStartup()) {
                    spriteBatch.setColor(1f, 0.6f, 0.6f, 1f);
                }
                Texture character = "scavenger".equals(enemy.getArchetypeId()) ? scavengerSprite :
                    "red_collector".equals(enemy.getArchetypeId()) ? redCollectorSprite : null;
                if (character != null) {
                    float visualWidth = "scavenger".equals(enemy.getArchetypeId()) ? 1.32f : 1.64f;
                    float visualHeight = "scavenger".equals(enemy.getArchetypeId()) ? 1.44f : 2.06f;
                    float left = enemy.getPosition().x - visualWidth / 2f;
                    float bottom = enemy.getPosition().y - enemy.getSize() / 2f;
                    spriteBatch.draw(character, enemy.isFacingLeft() ? left + visualWidth : left,
                        bottom, enemy.isFacingLeft() ? -visualWidth : visualWidth, visualHeight);
                } else {
                    spriteBatch.draw(enemyFrame, enemy.getPosition().x - enemy.getSize()/2f,
                        enemy.getPosition().y - enemy.getSize()/2f, enemy.getSize(), enemy.getSize());
                }
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
                effect.y - effect.size / 2f + 0.3f * progress, effect.size, effect.size);
        }
        spriteBatch.setColor(Color.WHITE);
    }

    private void drawDamageIndicators() {
        for (GameWorld.DamageIndicator indicator : gameWorld.getDamageIndicators()) {
            float progress = indicator.getProgress();
            float rise = 0.84f * (1f - (1f - progress) * (1f - progress));
            float alpha = 1f - progress;
            damageLayout.setText(damageFont, indicator.label);
            float x = indicator.x - damageLayout.width / 2f;
            float y = indicator.y + rise;
            damageFont.setColor(0f, 0f, 0f, 0.75f * alpha);
            damageFont.draw(spriteBatch, indicator.label, x + 0.03f, y - 0.03f);
            damageFont.setColor(1f,
                indicator.hero ? 0.38f : 0.86f,
                indicator.hero ? 0.38f : 0.5f, alpha);
            damageFont.draw(spriteBatch, indicator.label, x, y);
        }
        damageFont.setColor(Color.WHITE);
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

        return dx * dx + dy * dy <= 36f;
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
        damageFont.dispose();
        itemLabelFont.dispose();
        bandageSprite.dispose();
        noteSprite.dispose();
        keeperSprite.dispose();
        sceneryPixel.dispose();
        shelterBackdrop.dispose();
        redBackdrop.dispose();
        inkBackdrop.dispose();
        mechanicalBackdrop.dispose();
        wardenSprite.dispose();
        scavengerSprite.dispose();
        redCollectorSprite.dispose();
        roadSurface.dispose();
        spriteBatch.dispose();
        debugRenderer.dispose();
        ruinsAtlas.dispose();
        atlas.dispose();
    }
}
