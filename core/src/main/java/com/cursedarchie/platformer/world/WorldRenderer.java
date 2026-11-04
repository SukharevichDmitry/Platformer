package com.cursedarchie.platformer.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
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
import com.cursedarchie.platformer.actors.enemies.RangedAttackConfig;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.run.ItemCatalog;
import com.cursedarchie.platformer.controllers.ControlBindings;
import com.cursedarchie.platformer.controllers.ControlBindings.Action;
import com.cursedarchie.platformer.combat.AttackRuntime;

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
    private ControlBindings controlBindings;
    private InteractionData enteringPortal;
    private float portalProgress;

    public void setControlBindings(ControlBindings bindings) { controlBindings = bindings; }
    public void setPortalTransition(InteractionData portal, float progress) {
        enteringPortal = portal;
        portalProgress = Math.max(0f, Math.min(1f, progress));
    }
    private String interactKey() { return controlBindings == null ? "E" : controlBindings.keyName(Action.INTERACT); }

    private final OrthographicCamera cam;
    private LocationInstance cameraLocation;

    private final ShapeRenderer debugRenderer = new ShapeRenderer();
    private final TextureAtlas atlas;
    private final TextureAtlas ruinsAtlas;
    private final TextureRegion[] wallFaces = new TextureRegion[256];
    private final TextureRegion backgroundStone;
    private final TextureRegion stoneHalf;
    private final Texture collapsedWallSample;
    private final TextureRegion platformOneWay;
    private final TextureRegion blockBreakable;
    private final TextureRegion ladderSegment;
    private final TextureRegion ladderCap;
    private final Texture bandageSprite;
    private final Texture noteSprite;
    private final Texture keeperSprite;
    private final Texture sceneryPixel;
    private final TextureRegion arrowPixel;
    private final Sound firstSlash;
    private final Sound secondSlash;
    private final Sound heavySlash;
    private final Sound hurriedSlash;
    private final Sound chargeSound;
    private final Sound readySound;
    private boolean readyPlayed;
    private int impactSeen;
    private final Sound comboChime;
    private AttackState rhythmAttack;
    private com.cursedarchie.platformer.combat.InputResult rhythmResult =
        com.cursedarchie.platformer.combat.InputResult.NO_INPUT;
    private final Sound[] archerCueSounds = new Sound[Enemy.Cue.values().length];
    private final Sound arrowHitHeroSound;
    private final java.util.EnumMap<GameWorld.CombatEvent.Kind, Sound> eventSounds = new java.util.EnumMap<>(GameWorld.CombatEvent.Kind.class);
    private Sound staggerPopSound, deathSound;
    private final Sound arrowHitWallSound;
    private final Texture glowTexture;
    private final Texture ringTexture;
    /** Free-running clock (seconds) for flicker and orbit effects; advanced by the render delta. */
    private float effectClock;
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
    private boolean combatDebug;
    private AttackState displayedHeroAttack;
    private boolean displayedHeroSwing;
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
    public void toggleCombatDebug() { combatDebug = !combatDebug; }
    public boolean isCombatDebug() { return combatDebug; }

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
        collapsedWallSample = pixelTexture("scenery/collapsed-masonry-wall-sample.png");
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
        arrowPixel = new TextureRegion(sceneryPixel);
        scenery.dispose();
        firstSlash = Gdx.audio.newSound(Gdx.files.internal("sfx/slash_light_1.wav"));
        secondSlash = Gdx.audio.newSound(Gdx.files.internal("sfx/slash_light_2.wav"));
        heavySlash = Gdx.audio.newSound(Gdx.files.internal("sfx/slash_heavy.wav"));
        hurriedSlash = Gdx.audio.newSound(Gdx.files.internal("sfx/slash_hurried.wav"));
        chargeSound = Gdx.audio.newSound(Gdx.files.internal("sfx/breakthrough_charge.wav"));
        readySound = Gdx.audio.newSound(Gdx.files.internal("sfx/breakthrough_ready.wav"));
        comboChime = Gdx.audio.newSound(Gdx.files.internal("sfx/combo_chain.wav"));
        loadArcherCueSounds();
        arrowHitHeroSound = Gdx.audio.newSound(Gdx.files.internal("sfx/arrow_hit_hero.wav"));
        eventSounds.put(GameWorld.CombatEvent.Kind.HIT_LIGHT, Gdx.audio.newSound(Gdx.files.internal("sfx/hit_light.wav")));
        eventSounds.put(GameWorld.CombatEvent.Kind.HIT_HEAVY, Gdx.audio.newSound(Gdx.files.internal("sfx/hit_heavy.wav")));
        eventSounds.put(GameWorld.CombatEvent.Kind.BLOCK_METAL, Gdx.audio.newSound(Gdx.files.internal("sfx/block_metal.wav")));
        eventSounds.put(GameWorld.CombatEvent.Kind.BLOCK_BREAK, Gdx.audio.newSound(Gdx.files.internal("sfx/block_break.wav")));
        eventSounds.put(GameWorld.CombatEvent.Kind.BLOCK_HERO, Gdx.audio.newSound(Gdx.files.internal("sfx/block_hero.wav")));
        eventSounds.put(GameWorld.CombatEvent.Kind.BLOCK_HERO_HEAVY, eventSounds.get(GameWorld.CombatEvent.Kind.BLOCK_HERO));
        eventSounds.put(GameWorld.CombatEvent.Kind.WHIFF, Gdx.audio.newSound(Gdx.files.internal("sfx/whiff.wav")));
        eventSounds.put(GameWorld.CombatEvent.Kind.HEAD_STOMP, Gdx.audio.newSound(Gdx.files.internal("sfx/head_stomp.wav")));
        eventSounds.put(GameWorld.CombatEvent.Kind.WALL_BUMP, Gdx.audio.newSound(Gdx.files.internal("sfx/wall_bump.wav")));
        staggerPopSound = Gdx.audio.newSound(Gdx.files.internal("sfx/stagger_pop.wav"));
        deathSound = Gdx.audio.newSound(Gdx.files.internal("sfx/death.wav"));
        arrowHitWallSound = Gdx.audio.newSound(Gdx.files.internal("sfx/arrow_hit_wall.wav"));
        glowTexture = radialTexture(false);
        ringTexture = radialTexture(true);
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
        renderWorldOnly(delta);
        drawHitBar();
    }

    /** Draws only the world layer so transitions can capture it without HUD. */
    public void renderWorldOnly(float delta) {
        updateCamera(delta);
        effectClock += delta;

        spriteBatch.setProjectionMatrix(cam.combined);

        spriteBatch.begin();
            if ("start".equals(gameWorld.getLocation().getLocation()) ||
                "ruins".equals(gameWorld.getLocation().getLocation())) drawOpeningScenery();
            else drawBackgroundMasonry();
            drawTiles();
            drawCollapsedWallSample();
            roadSurface.draw(gameWorld.getLocation(), spriteBatch,
                getVisibleLeftEdge(), getVisibleRightEdge());
            drawClimbShafts();
            drawPortals(false);
            drawInteractions();
            drawSmoke();
            drawHero();
            drawEnemy();
            drawArrows();
            drawArrowImpacts();
            drawPortals(true);
            drawDefeatEffects();
            drawCombatEvents();
            drawDamageIndicators();
        spriteBatch.end();

        if (debug) {
            drawDebug();
            drawHeroSensors();
            drawEnemySensors();
            drawViewCones();
            drawChainDebug();
        }
        if (combatDebug) drawCombatDebug();

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
            if (interaction.requiresFlag != null && !gameWorld.hasRunFlag(interaction.requiresFlag)) continue;
            if ("portal".equals(interaction.kind)) continue;
            float x = room.getWorldX() + interaction.position.x;
            float y = room.getWorldY() + interaction.position.y;
            if ("npc".equals(interaction.kind)) {
                spriteBatch.draw("warden".equals(interaction.visualId) ? wardenSprite : keeperSprite,
                    x - .7f, y - 1f, 1.4f, 2.4f);
                spriteBatch.setColor(Color.WHITE);
                if (interaction == nearby)
                    itemLabelFont.drawCentered(spriteBatch, interactKey(), x, y + 1.96f, NOTE_LABEL);
                continue;
            }
            boolean bandage = ItemCatalog.FIELD_BANDAGE.equals(interaction.visualId);
            boolean blade = ItemCatalog.BLADE_SHARD.equals(interaction.visualId);
            boolean impact = ItemCatalog.IMPACT_CORE.equals(interaction.visualId);
            if (blade) spriteBatch.setColor(.65f, .85f, 1f, 1f);
            else if (impact) spriteBatch.setColor(1f, .65f, .45f, 1f);
            else if (ItemCatalog.RED_BRACER.equals(interaction.visualId))
                spriteBatch.setColor(1f, .3f, .3f, 1f);
            else if (ItemCatalog.SMOKE_INCENSE.equals(interaction.visualId))
                spriteBatch.setColor(.66f, .72f, .73f, 1f);
            else if (ItemCatalog.SEWING_KIT.equals(interaction.visualId))
                spriteBatch.setColor(.98f, .81f, .5f, 1f);
            spriteBatch.draw(bandage ? bandageSprite : noteSprite,
                x - 0.4f, y - 0.36f, 0.8f, 0.8f);
            spriteBatch.setColor(Color.WHITE);
            if (interaction == nearby) {
                Color labelColor = bandage ? BANDAGE_LABEL : NOTE_LABEL;
                itemLabelFont.drawCentered(spriteBatch, interactKey(), x, y + 1.04f, labelColor);
                itemLabelFont.drawCentered(spriteBatch, bandage ? "BANDAGE" :
                    blade ? "BLADE SHARD" : impact ? "IMPACT CORE" :
                        ItemCatalog.RED_BRACER.equals(interaction.visualId) ? "RED BRACER" :
                        ItemCatalog.SMOKE_INCENSE.equals(interaction.visualId) ? "SMOKE" :
                        ItemCatalog.SEWING_KIT.equals(interaction.visualId) ? "SEWING KIT" :
                        "CLUE",
                    x, y + 0.60f, labelColor);
            }
        }
    }

    private void drawPortals(boolean foreground) {
        RoomInstance room = gameWorld.getCurrentRoom();
        if (room == null) return;
        for (int i = 0; i < room.getInteractions().size; i++) {
            InteractionData portal = room.getInteractions().get(i);
            if (!"portal".equals(portal.kind)) continue;
            boolean locked = "locked_laundry_door".equals(portal.id) &&
                gameWorld.hasRunFlag("laundry_access_locked") &&
                !gameWorld.hasRunFlag("alley_key_found");
            if (portal.requiresFlag != null && !gameWorld.hasRunFlag(portal.requiresFlag) && !locked) continue;
            float x = room.getWorldX() + portal.position.x;
            float y = room.getWorldY() + portal.position.y;
            if (!foreground) {
                spriteBatch.setColor(.08f, .07f, .09f, 1f);
                spriteBatch.draw(sceneryPixel, x - .85f, y - .65f, 1.7f, 2.5f);
                spriteBatch.setColor(.21f, .20f, .23f, 1f);
                spriteBatch.draw(sceneryPixel, x - .48f, y - .65f, .96f, 2.1f);
                spriteBatch.setColor(.44f, .34f, .25f, 1f);
                spriteBatch.draw(sceneryPixel, x - 1.4f, y - .75f, 1.1f, .08f);
                spriteBatch.draw(sceneryPixel, x + .3f, y - .75f, 1.1f, .08f);
                if (locked && gameWorld.getHero().getPosition().dst(x, y) < 3f)
                    itemLabelFont.drawCentered(spriteBatch, "LOCKED: FIND THE KEY", x, y + 2.3f, NOTE_LABEL);
                else if (gameWorld.getNearbyInteraction() == portal)
                    itemLabelFont.drawCentered(spriteBatch, interactKey() + ": " +
                        ("alley_door".equals(portal.visualId) ? "ENTER ALLEY" :
                            "street_exit".equals(portal.visualId) ? "RETURN" : "ENTER"),
                        x, y + 2.3f, NOTE_LABEL);
            } else {
                spriteBatch.setColor(.36f, .27f, .20f, 1f);
                spriteBatch.draw(sceneryPixel, x - 1.08f, y - .74f, .31f, 2.7f);
                spriteBatch.draw(sceneryPixel, x + .77f, y - .74f, .31f, 2.7f);
                spriteBatch.draw(sceneryPixel, x - 1.08f, y + 1.77f, 2.16f, .25f);
                if (enteringPortal == portal) {
                    spriteBatch.setColor(.03f, .03f, .05f, portalProgress * .75f);
                    spriteBatch.draw(sceneryPixel, x - .77f, y - .65f, 1.54f, 2.42f);
                }
            }
            spriteBatch.setColor(Color.WHITE);
        }
    }

    private void drawSmoke() {
        if (gameWorld.getSmokeRemaining() <= 0f) return;
        Vector2 center = gameWorld.getSmokeCenter();
        spriteBatch.setColor(.48f, .5f, .54f,
            Math.min(.52f, gameWorld.getSmokeRemaining() * .26f));
        spriteBatch.draw(sceneryPixel, center.x - 2.2f, center.y - .6f, 4.4f, 2.3f);
        spriteBatch.setColor(Color.WHITE);
    }

    /*=========================================ARCHER PRESENTATION=====================================================*/
    // The scavenger sprite is one static pose without a bow, so each archer action is composed from the sprite
    // transform plus procedural bow, aim, flash and trail effects. Palette, kept apart so actions read at a glance
    // and never rely on colour alone (each also has its own silhouette): standard shot = amber, single thin line;
    // charged shot = orange with a white-hot core, thick line and ring; volley = violet, three fanned lines;
    // melee = crimson zone and steel slash; retreat/evade = teal/cyan chevrons and ghosts; hero hit = white, then red.

    /** The scavenger art natively faces left; the collector's flip rule in drawEnemy is the same idea. */
    private static final boolean SCAVENGER_SPRITE_FACES_LEFT = true;
    private static final float ARCHER_VISUAL_WIDTH = 1.32f;
    private static final float ARCHER_VISUAL_HEIGHT = 1.44f;
    private static final float AIM_LINE_LENGTH = 6f;
    private static final Color COLOR_SHOT_STANDARD = new Color(1f, .84f, .30f, 1f);
    private static final Color COLOR_SHOT_CHARGED = new Color(1f, .45f, .12f, 1f);
    private static final Color COLOR_SHOT_VOLLEY = new Color(.86f, .40f, 1f, 1f);
    private static final Color COLOR_MELEE = new Color(1f, .20f, .20f, 1f);
    private static final Color COLOR_STEEL = new Color(.92f, .95f, 1f, 1f);
    private static final Color COLOR_RETREAT = new Color(.45f, .95f, 1f, 1f);
    private static final Color COLOR_WOOD = new Color(.55f, .37f, .20f, 1f);
    private static final Color COLOR_STRING = new Color(.92f, .88f, .78f, 1f);

    private static Color shotColor(int mode) {
        return mode == 2 ? COLOR_SHOT_VOLLEY : mode == 1 ? COLOR_SHOT_CHARGED : COLOR_SHOT_STANDARD;
    }

    private static float clamp01(float value) {
        return value < 0f ? 0f : value > 1f ? 1f : value;
    }

    private void tint(Color color, float alpha) {
        spriteBatch.setColor(color.r, color.g, color.b, alpha);
    }

    /** Like {@link #tint} but blends the colour toward white (0 = unchanged, 1 = white). */
    private void tintMixed(Color color, float toWhite, float alpha) {
        spriteBatch.setColor(color.r + (1f - color.r) * toWhite, color.g + (1f - color.g) * toWhite,
            color.b + (1f - color.b) * toWhite, alpha);
    }

    /** Bar of the given size centred on a point and rotated about that centre. */
    private void barCentered(float cx, float cy, float length, float thickness, float degrees) {
        spriteBatch.draw(arrowPixel, cx - length / 2f, cy - thickness / 2f, length / 2f, thickness / 2f,
            length, thickness, 1f, 1f, degrees);
    }

    /** Bar starting at a point and extending along the given angle. */
    private void barFrom(float x, float y, float length, float thickness, float degrees) {
        spriteBatch.draw(arrowPixel, x, y - thickness / 2f, 0f, thickness / 2f, length, thickness, 1f, 1f, degrees);
    }

    private void segment(float x0, float y0, float x1, float y1, float thickness) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        barFrom(x0, y0, (float) Math.sqrt(dx * dx + dy * dy), thickness,
            (float) Math.toDegrees(Math.atan2(dy, dx)));
    }

    private void glow(float cx, float cy, float radius) {
        spriteBatch.draw(glowTexture, cx - radius, cy - radius, radius * 2f, radius * 2f);
    }

    private void ring(float cx, float cy, float radius) {
        spriteBatch.draw(ringTexture, cx - radius, cy - radius, radius * 2f, radius * 2f);
    }

    /** Soft white disc (or thin ring) used, tinted, for flashes, auras and shock rings. */
    private static Texture radialTexture(boolean ring) {
        final int size = 64;
        Pixmap pixels = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixels.setBlending(Pixmap.Blending.None);
        float centre = (size - 1) / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float d = (float) Math.sqrt((x - centre) * (x - centre) + (y - centre) * (y - centre))
                    / (size / 2f);
                float alpha;
                if (d > 1f) alpha = 0f;
                else if (ring) alpha = Math.max(0f, 1f - Math.abs(d - .82f) / .14f);
                else {
                    alpha = 1f - d;
                    alpha *= alpha;
                }
                pixels.drawPixel(x, y, Color.rgba8888(1f, 1f, 1f, alpha));
            }
        }
        Texture texture = new Texture(pixels);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixels.dispose();
        return texture;
    }

    private static String cueFile(Enemy.Cue cue) {
        switch (cue) {
            case BOW_DRAW_STANDARD: return "bow_draw_standard";
            case BOW_DRAW_CHARGED: return "bow_draw_charged";
            case BOW_DRAW_VOLLEY: return "bow_draw_volley";
            case SHOT_STANDARD: return "arrow_shot_standard";
            case SHOT_CHARGED: return "arrow_shot_charged";
            case SHOT_VOLLEY: return "arrow_shot_volley";
            case MELEE_WINDUP: return "archer_melee_windup";
            case MELEE_STRIKE: return "archer_melee_strike";
            case RETREAT_DASH: return "archer_retreat_dash";
            case RAIDER_DASH: return "raider_dash";
            case RAIDER_GUARD: return "raider_guard";
            case OVERSEER_CAST: return "overseer_cast";
            case OVERSEER_BUFF: return "overseer_buff";
            case BREAKER_WINDUP: return "breaker_windup";
            case BREAKER_SLAM: return "breaker_slam";
            case ALERT: return "enemy_alert";
            default: return "archer_dodge";
        }
    }

    private static float cueVolume(Enemy.Cue cue) {
        switch (cue) {
            case SHOT_CHARGED: return .65f;
            case SHOT_VOLLEY: return .6f;
            case SHOT_STANDARD: case MELEE_WINDUP: case MELEE_STRIKE: return .5f;
            case DODGE: return .4f;
            case RETREAT_DASH: return .3f;
            case RAIDER_DASH: return .45f;
            case RAIDER_GUARD: return .4f;
            case OVERSEER_CAST: case OVERSEER_BUFF: return .4f;
            case BREAKER_WINDUP: return .5f;
            case BREAKER_SLAM: return .85f;
            case ALERT: return .4f;
            default: return .35f;
        }
    }

    private void loadArcherCueSounds() {
        for (Enemy.Cue cue : Enemy.Cue.values())
            archerCueSounds[cue.ordinal()] =
                Gdx.audio.newSound(Gdx.files.internal("sfx/" + cueFile(cue) + ".wav"));
    }

    /** Sounds fade with distance from the hero so off-screen archers stay quiet. */
    private float soundVolume(float x, float y, float base) {
        return base * Math.max(.15f, 1f - hero.getPosition().dst(x, y) / 14f);
    }

    private void playEnemyCues(Enemy enemy) {
        Enemy.Cue cue = enemy.pollCue();
        while (cue != null) {
            archerCueSounds[cue.ordinal()].play(
                soundVolume(enemy.getPosition().x, enemy.getPosition().y, cueVolume(cue)));
            cue = enemy.pollCue();
        }
    }

    private void drawArcherSprite(float centreX, float bottomY, boolean facingLeft, float forwardLean,
                                  float scaleX, float scaleY) {
        float face = facingLeft ? -1f : 1f;
        spriteBatch.draw(scavengerSprite, centreX - ARCHER_VISUAL_WIDTH / 2f, bottomY,
            ARCHER_VISUAL_WIDTH / 2f, 0f, ARCHER_VISUAL_WIDTH, ARCHER_VISUAL_HEIGHT, scaleX, scaleY,
            -face * forwardLean, 0, 0, scavengerSprite.getWidth(), scavengerSprite.getHeight(),
            facingLeft != SCAVENGER_SPRITE_FACES_LEFT, false);
    }

    private static com.cursedarchie.platformer.actors.enemies.states.combat.AttackState meleeState(Enemy enemy) {
        Object state = enemy.getCombatStateMachine().getCurrentState();
        return state instanceof com.cursedarchie.platformer.actors.enemies.states.combat.AttackState ?
            (com.cursedarchie.platformer.actors.enemies.states.combat.AttackState) state : null;
    }

    /** Draws one archer: body pose first, then the effects of whichever single action it is performing. */
    private void drawArcher(Enemy enemy) {
        playEnemyCues(enemy);
        Vector2 pos = enemy.getPosition();
        boolean left = enemy.isFacingLeft();
        float face = left ? -1f : 1f;
        float feet = pos.y - enemy.getSize() / 2f;
        com.cursedarchie.platformer.actors.enemies.states.combat.AttackState melee = meleeState(enemy);
        // Only one action is shown at a time, so melee never reads as a shot and vice versa.
        Enemy.RangedPhase ranged = melee == null ? enemy.getRangedPhase() : Enemy.RangedPhase.NONE;
        Enemy.RetreatPhase retreat = melee == null && ranged == Enemy.RangedPhase.NONE ?
            enemy.getRetreatPhase() : Enemy.RetreatPhase.NONE;

        float offX = 0f;
        float offY = 0f;
        float lean = 0f;
        float scaleX = 1f;
        float scaleY = 1f;
        float r = 1f;
        float g = 1f;
        float b = 1f;
        float a = 1f;

        if (melee != null) {
            float t = enemy.getCombatStateTime();
            float startup = melee.startup(enemy);
            float activeEnd = melee.activeEnd(enemy);
            if (t < startup) {                       // wind-up: coil back, blade raised, flush crimson
                float p = t / startup;
                lean = -12f * p;
                offX = -face * .1f * p;
                offY = -.04f * p;
                scaleY = 1f - .05f * p;
                g = b = 1f - .5f * p;
            } else if (t < activeEnd) {              // strike: lunge through the zone
                lean = 18f;
                offX = face * .24f;
            } else {                                 // recovery: ease back to a neutral stance
                float total = Math.max(activeEnd + .01f, enemy.getAttackDuration());
                float q = clamp01((t - activeEnd) / (total - activeEnd));
                lean = 18f * (1f - q);
                offX = face * .24f * (1f - q);
            }
        } else if (ranged != Enemy.RangedPhase.NONE) {
            float p = enemy.getRangedWindupProgress();
            switch (ranged) {
                case DRAW:
                    lean = -4f * p;
                    offX = -face * .05f * p;
                    break;
                case LOCKED:
                    lean = 3f;
                    offX = -face * .05f;
                    break;
                case CUE:
                    lean = 5f;
                    offX = -face * .05f + (float) Math.sin(effectClock * 90f) * .012f;
                    break;
                case RECOVERY:
                    float q = enemy.getRangedRecoveryProgress();
                    lean = -9f * (1f - q);
                    offX = -face * .14f * (1f - q);
                    break;
                default:
                    break;
            }
        } else if (retreat != Enemy.RetreatPhase.NONE) {
            float p = enemy.getRetreatPhaseProgress();
            switch (retreat) {
                case TELEGRAPH:                      // coil away from the hero
                    lean = -11f * p;
                    offX = -face * .09f * p;
                    offY = -.07f * p;
                    scaleY = 1f - .06f * p;
                    r = 1f - .3f * p;
                    break;
                case DASH:                           // stretched and tinted while moving backwards
                    lean = -16f;
                    scaleX = 1.1f;
                    scaleY = .94f;
                    r = .75f;
                    break;
                case RECOVER:                        // skid, then settle back into the stance
                    lean = -10f * (1f - p);
                    offY = -.04f * (1f - p);
                    break;
                default:
                    break;
            }
        }

        float dodgeRemaining = enemy.getDodgeFlashRemaining();
        if (dodgeRemaining > Enemy.DODGE_EFFECT_DURATION - Enemy.DODGE_TINT_DURATION) {
            r = .4f;
            g = 1f;
            b = 1f;
            a = .8f;
            lean = -14f;
        } else if (gameWorld.hasRecentDamage(enemy)) {
            r = 1f;
            g = b = .35f;
        }

        drawArcherTrail(enemy, left, feet);
        spriteBatch.setColor(r, g, b, a);
        drawArcherSprite(pos.x + offX, feet + offY, left, lean, scaleX, scaleY);

        if (melee != null) drawArcherMelee(enemy, melee, face);
        else if (ranged != Enemy.RangedPhase.NONE) drawArcherRanged(enemy, ranged);
        else if (retreat != Enemy.RetreatPhase.NONE) drawArcherRetreat(enemy, retreat, face, feet);
        drawArcherDodge(enemy);
        spriteBatch.setColor(Color.WHITE);
    }

    /** Afterimages along the path of a retreat dash (teal) or a dodge (cyan), plus a puff of dust at the start. */
    private void drawArcherTrail(Enemy enemy, boolean left, float feet) {
        float remaining = enemy.getRetreatTrailRemaining();
        if (remaining <= 0f) return;
        boolean dodge = enemy.isRetreatTrailDodge();
        float fade = clamp01(remaining / Enemy.RETREAT_TRAIL_FADE);
        float from = enemy.getRetreatTrailFromX();
        float to = enemy.getRetreatTrailToX();
        if (Math.abs(to - from) > .3f) {
            int ghosts = 5;
            for (int i = 0; i < ghosts; i++) {
                float f = i / (float) ghosts;
                float alpha = (.12f + .3f * f) * fade * (dodge ? 1.25f : 1f);
                if (dodge) spriteBatch.setColor(.45f, 1f, 1f, alpha);
                else spriteBatch.setColor(.55f, .9f, 1f, alpha);
                drawArcherSprite(from + (to - from) * f, feet, left, dodge ? -10f : -14f, 1f, 1f);
            }
        }
        float total = (dodge ? .3f : Enemy.RETREAT_DASH_TIME + Enemy.RETREAT_RECOVER_TIME) +
            Enemy.RETREAT_TRAIL_FADE;
        float puff = (total - remaining) / .4f;
        if (puff < 1f) {
            float dir = enemy.getRetreatTrailDirection();
            for (int k = 0; k < 3; k++) {
                spriteBatch.setColor(.72f, .7f, .66f, .4f * (1f - puff));
                glow(from - dir * (.12f + .22f * puff) * (k + 1), feet + .1f + .12f * puff * k,
                    .12f + .1f * k + .12f * puff);
            }
        }
    }

    /** Melee: a crimson strike zone, "!" marker and blade glint to telegraph, then a steel slash. */
    private void drawArcherMelee(Enemy enemy,
                                 com.cursedarchie.platformer.actors.enemies.states.combat.AttackState melee,
                                 float face) {
        Vector2 pos = enemy.getPosition();
        float t = enemy.getCombatStateTime();
        float startup = melee.startup(enemy);
        float activeEnd = melee.activeEnd(enemy);
        if (t < startup) {
            float p = t / startup;
            // Danger zone: where the hero's body can be struck (centred .86 ahead, hero body 1 wide).
            tint(COLOR_MELEE, .10f + .28f * p);
            spriteBatch.draw(sceneryPixel, face > 0f ? pos.x + .4f : pos.x - 1.75f, pos.y - .5f, 1.35f, 1f);
            tint(COLOR_MELEE, .45f + .45f * p);
            spriteBatch.draw(sceneryPixel, face > 0f ? pos.x + 1.69f : pos.x - 1.75f, pos.y - .5f, .06f, 1f);
            // "!" marker above the head.
            float markY = pos.y + .95f + .03f * (float) Math.sin(effectClock * 40f);
            tint(COLOR_MELEE, .95f);
            spriteBatch.draw(sceneryPixel, pos.x - .05f, markY, .1f, .26f);
            spriteBatch.draw(sceneryPixel, pos.x - .05f, markY - .15f, .1f, .09f);
            // Glint on the raised blade.
            tint(COLOR_STEEL, .35f + .5f * p);
            glow(pos.x + face * .6f, pos.y - .06f, .12f + .2f * p);
        } else {
            float sweepEnd = activeEnd + .1f;
            if (t < sweepEnd)
                drawSlashArc(pos.x, pos.y, face, .95f, (t - startup) / (sweepEnd - startup), COLOR_STEEL);
        }
    }

    /** Crescent of short bars swept top-to-bottom in front of {@code dirX}; used for melee and whiffed strikes. */
    private void drawSlashArc(float cx, float cy, float dirX, float radius, float progress, Color color) {
        float alpha = clamp01(1f - progress * .85f);
        int pieces = 7;
        for (int i = 0; i < pieces; i++) {
            float f = i / (float) (pieces - 1);
            if (f > clamp01(progress * 1.6f + .25f)) break;
            float angle = 62f - 118f * f;
            if (dirX < 0f) angle = 180f - angle;
            float rad = (float) Math.toRadians(angle);
            tint(color, alpha * (.45f + .55f * f));
            barCentered(cx + (float) Math.cos(rad) * radius, cy + .05f + (float) Math.sin(rad) * radius,
                .3f, .09f, angle + 90f);
        }
    }

    /** Retreat: teal chevrons as the tell, speed lines on the dash, a ready ring when back in stance. */
    private void drawArcherRetreat(Enemy enemy, Enemy.RetreatPhase phase, float face, float feet) {
        Vector2 pos = enemy.getPosition();
        float p = enemy.getRetreatPhaseProgress();
        float away = -face;
        switch (phase) {
            case TELEGRAPH:
                for (int k = 0; k < 3; k++) {
                    tint(COLOR_RETREAT, clamp01(p * 1.5f - k * .15f) * .85f);
                    chevron(pos.x + away * (.75f + .26f * k), pos.y - .1f, away);
                }
                tint(COLOR_RETREAT, .35f * p);
                ring(pos.x, feet + .1f, .15f + .35f * p);
                break;
            case DASH:
                for (int k = 0; k < 3; k++) {
                    tint(COLOR_RETREAT, .55f * (1f - p));
                    barCentered(pos.x - away * (.6f + .35f * k), pos.y - .3f + .3f * k,
                        .55f - .1f * k, .05f, 0f);
                }
                break;
            case RECOVER:
                if (p > .55f) {
                    float f = (p - .55f) / .45f;
                    tint(COLOR_RETREAT, .6f * (1f - f));
                    ring(pos.x, pos.y + .1f, .5f + .4f * f);
                }
                break;
            default:
                break;
        }
    }

    private void chevron(float tipX, float tipY, float dir) {
        float base = dir > 0f ? 180f : 0f;
        barFrom(tipX, tipY, .26f, .06f, base - 40f);
        barFrom(tipX, tipY, .26f, .06f, base + 40f);
    }

    /** Evasion: the hero's blade whiffs through the archer's old position and a "DODGE" label rises. */
    private void drawArcherDodge(Enemy enemy) {
        float remaining = enemy.getDodgeFlashRemaining();
        if (remaining <= 0f) return;
        float p = 1f - remaining / Enemy.DODGE_EFFECT_DURATION;
        float away = enemy.getDodgeDirection();
        float originX = enemy.getDodgeOriginX();
        float originY = enemy.getDodgeOriginY();
        if (p < .6f) drawSlashArc(originX - away * .65f, originY, away, .65f, p / .6f, COLOR_RETREAT);
        tint(COLOR_RETREAT, .6f * (1f - p));
        ring(originX, originY, .2f + .6f * p);
        float labelY = enemy.getPosition().y + enemy.getSize() / 2f + .7f + .5f * p;
        damageLayout.setText(damageFont, "DODGE");
        float labelX = enemy.getPosition().x - damageLayout.width / 2f;
        damageFont.setColor(0f, 0f, 0f, .7f * (1f - p));
        damageFont.draw(spriteBatch, "DODGE", labelX + .03f, labelY - .03f);
        damageFont.setColor(.45f, 1f, 1f, 1f - p);
        damageFont.draw(spriteBatch, "DODGE", labelX, labelY);
        damageFont.setColor(Color.WHITE);
    }

    /** Ranged: bow, nocked arrows and aim lines per shot type, release cue, muzzle flash and recovery. */
    private void drawArcherRanged(Enemy enemy, Enemy.RangedPhase phase) {
        int mode = enemy.getRangedShotMode();
        Color c = shotColor(mode);
        Vector2 pos = enemy.getPosition();
        Vector2 aim = enemy.getRangedAim();
        float face = enemy.isRangedShotFacingLeft() ? -1f : 1f;
        float aimAngle = (float) Math.toDegrees(Math.atan2(aim.y, aim.x));
        float progress = enemy.getRangedWindupProgress();
        boolean drawing = phase == Enemy.RangedPhase.DRAW || phase == Enemy.RangedPhase.LOCKED ||
            phase == Enemy.RangedPhase.CUE;
        // The bow sits where the arrows spawn (see GameWorld.fireArrowVolley).
        float hx = pos.x + aim.x * .55f;
        float hy = pos.y + .1f + aim.y * .55f;
        float mx = pos.x + aim.x * .65f;
        float my = pos.y + .1f + aim.y * .65f;
        float drawDistance = mode == 1 ? .46f : mode == 2 ? .42f : .34f;
        float pull = drawing ? clamp01(progress / .65f) : 0f;
        float bowAngle = aimAngle;
        if (phase == Enemy.RangedPhase.RECOVERY)
            bowAngle = aimAngle - face * 70f * clamp01(enemy.getRangedRecoveryProgress() * 3f);
        float cue = enemy.getRangedReleaseCueProgress();

        if (drawing) {
            drawAimLines(enemy, phase, mode, c, aimAngle, progress, cue);
            float charge = clamp01(progress);
            if (mode == 1) {
                // Charged: a growing orange aura with white-hot sparks spiralling in.
                tint(c, .18f + .32f * charge);
                glow(hx, hy, .25f + .55f * charge);
                for (int k = 0; k < 4; k++) {
                    float angle = effectClock * 7f + k * 1.5708f;
                    float orbit = .75f - .4f * charge;
                    tintMixed(c, .7f, .9f);
                    barCentered(hx + (float) Math.cos(angle) * orbit, hy + (float) Math.sin(angle) * orbit,
                        .09f, .09f, 0f);
                }
            } else if (mode == 2) {
                tint(c, .12f + .22f * charge);
                glow(hx, hy, .3f + .25f * charge);
            } else {
                tint(c, .10f + .15f * charge);
                glow(hx, hy, .22f);
            }
        }

        drawBow(hx, hy, bowAngle, pull, drawDistance);
        if (drawing) drawNockedArrows(hx, hy, bowAngle, pull, drawDistance, mode, c);

        if (enemy.getRangedLockFlashRemaining() > 0f) {
            float f = enemy.getRangedLockFlashRemaining() / .14f;
            tintMixed(c, .5f, f);
            ring(mx, my, .15f + .35f * (1f - f));
        }
        if (phase == Enemy.RangedPhase.CUE) {
            // Just before release: pulsing white-hot flare and a tightening ring.
            float pulse = .6f + .4f * (float) Math.sin(effectClock * 100f);
            tintMixed(c, .7f, .85f * pulse);
            glow(mx, my, (mode == 1 ? .6f : mode == 2 ? .5f : .34f) * (.7f + .6f * cue));
            tintMixed(c, .8f, 1f - cue);
            ring(mx, my, .7f - .55f * cue);
        }
        if (phase == Enemy.RangedPhase.RECOVERY) {
            if (enemy.getRangedShotFlashRemaining() > 0f) drawReleaseFlash(enemy, mode, c, mx, my, aimAngle, pos);
            else if (enemy.getRangedFizzleRemaining() > 0f) {
                float f = enemy.getRangedFizzleRemaining() / .3f;
                spriteBatch.setColor(.65f, .62f, .6f, .5f * f);
                glow(hx, hy, .45f * (1.2f - f * .4f));
                tint(c, .9f * f);
                for (int k = 0; k < 4; k++) {
                    float angle = k * 1.7f + .6f;
                    float dist = .15f + .5f * (1f - f);
                    barCentered(hx + (float) Math.cos(angle) * dist, hy + (float) Math.sin(angle) * dist,
                        .1f, .05f, (float) Math.toDegrees(angle));
                }
            } else {
                float q = enemy.getRangedRecoveryProgress();
                spriteBatch.setColor(.7f, .7f, .72f, .28f * (1f - q));
                glow(mx, my, .22f + .3f * q);
            }
        }
    }

    private final float[] arcX = new float[96], arcY = new float[96];

    /**
     * Aim as world-space arcs: the exact flight the arrow will take (same ballistics and collisions as a live
     * arrow). Dashed and faint while the aim follows the hero, solid once locked, white-hot at the release cue.
     * A volley shows three arcs and three impact points; the empowered shot is brighter, thicker and flatter.
     */
    private void drawAimLines(Enemy enemy, Enemy.RangedPhase phase, int mode, Color c, float aimAngle,
                              float progress, float cue) {
        Vector2 pos = enemy.getPosition();
        RangedAttackConfig config = enemy.getRangedAttackConfig();
        int lines = config.projectileCount;
        float spreadDegrees = (float) Math.toDegrees(config.spreadRadians);
        boolean cueing = phase == Enemy.RangedPhase.CUE;
        boolean locked = phase != Enemy.RangedPhase.DRAW;
        float lockProgress = clamp01(progress / Enemy.RANGED_AIM_LOCK_AT);
        float thickness = mode == 1 ? .09f : mode == 2 ? .04f : .055f;
        for (int i = 0; i < lines; i++) {
            float angle = aimAngle + (i - (lines - 1) / 2f) * spreadDegrees;
            float rad = (float) Math.toRadians(angle);
            float cos = (float) Math.cos(rad), sin = (float) Math.sin(rad);
            int n = gameWorld.predictArrowPath(pos.x + cos * .65f, pos.y + .1f + sin * .65f, cos, sin,
                config, arcX, arcY);
            // Only the leading part of the arc is shown while tracking: it grows as the draw is pulled.
            int shown = locked ? n : Math.max(2, (int) (n * (.35f + .65f * lockProgress)));
            float flicker = cueing ? .8f + .2f * (float) Math.sin(effectClock * 100f) : 1f;
            for (int k = 1; k < shown; k++) {
                if (!locked && (k & 1) == 1) continue;   // dashed while the aim is still following the hero
                float fade = 1f - .55f * k / (float) n;     // fades toward the far end so it never blocks the view
                if (mode == 1) {
                    tint(c, (locked ? .2f : .1f) * flicker * fade);
                    segment(arcX[k - 1], arcY[k - 1], arcX[k], arcY[k], thickness * 2.6f);
                }
                if (locked) tintMixed(c, cueing ? .6f : 0f, (cueing ? .95f : .6f) * flicker * fade);
                else tint(c, (.2f + .2f * lockProgress) * fade);
                segment(arcX[k - 1], arcY[k - 1], arcX[k], arcY[k], cueing ? thickness * 1.8f : thickness);
            }
            // Intended point of impact, drawn where the arc actually ends (wall, floor or hero).
            if (n > 1 && (locked || lockProgress > .6f)) {
                float ix = arcX[n - 1], iy = arcY[n - 1];
                float size = (mode == 1 ? .26f : mode == 2 ? .16f : .2f) * (cueing ? 1.25f - .25f * cue : 1f);
                tintMixed(c, cueing ? .5f : 0f, locked ? .95f : .4f);
                ring(ix, iy, size);
                barCentered(ix, iy, size * 1.3f, .035f, 45f);
                barCentered(ix, iy, size * 1.3f, .035f, -45f);
            }
        }
    }

    private void drawBow(float hx, float hy, float angleDeg, float pull, float drawDistance) {
        float rad = (float) Math.toRadians(angleDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        final int segments = 6;
        float prevX = 0f;
        float prevY = 0f;
        for (int i = 0; i <= segments; i++) {
            float u = -1f + 2f * i / segments;
            // The limbs bulge toward the target and their tips bend back as the string is drawn.
            float lx = .15f * (1f - u * u) - .09f * pull * u * u;
            float ly = u * .52f;
            float wx = hx + lx * cos - ly * sin;
            float wy = hy + lx * sin + ly * cos;
            if (i > 0) {
                tint(COLOR_WOOD, 1f);
                segment(prevX, prevY, wx, wy, .07f);
            }
            prevX = wx;
            prevY = wy;
        }
        float tipX = -.09f * pull;
        float nockX = -drawDistance * pull;
        float topX = hx + tipX * cos - .52f * sin;
        float topY = hy + tipX * sin + .52f * cos;
        float bottomX = hx + tipX * cos + .52f * sin;
        float bottomY = hy + tipX * sin - .52f * cos;
        float nockWorldX = hx + nockX * cos;
        float nockWorldY = hy + nockX * sin;
        tint(COLOR_STRING, .9f);
        segment(topX, topY, nockWorldX, nockWorldY, .022f);
        segment(nockWorldX, nockWorldY, bottomX, bottomY, .022f);
    }

    /** One nocked arrow, or three with a slight vertical fan for the volley, sliding back as the bow is drawn. */
    private void drawNockedArrows(float hx, float hy, float bowAngle, float pull, float drawDistance,
                                  int mode, Color c) {
        float rad = (float) Math.toRadians(bowAngle);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        int count = mode == 2 ? 3 : 1;
        for (int i = 0; i < count; i++) {
            float phi = mode == 2 ? (i - 1) * RangedAttackConfig.VOLLEY.spreadRadians : 0f;
            float lateral = mode == 2 ? (i - 1) * .08f : 0f;
            float tailLx = -drawDistance * pull;
            float length = drawDistance * pull + .24f;
            float tipLx = tailLx + (float) Math.cos(phi) * length;
            float tipLy = lateral + (float) Math.sin(phi) * length;
            float tailX = hx + tailLx * cos - lateral * sin;
            float tailY = hy + tailLx * sin + lateral * cos;
            float tipX = hx + tipLx * cos - tipLy * sin;
            float tipY = hy + tipLx * sin + tipLy * cos;
            float arrowAngle = bowAngle + (float) Math.toDegrees(phi);
            tintMixed(c, .35f, 1f);
            segment(tailX, tailY, tipX, tipY, mode == 1 ? .06f : .04f);
            tint(COLOR_STEEL, 1f);
            barCentered(tipX, tipY, .12f, mode == 1 ? .1f : .07f, arrowAngle);
            tint(c, 1f);
            barFrom(tailX, tailY, .12f, .03f, arrowAngle + 150f);
            barFrom(tailX, tailY, .12f, .03f, arrowAngle - 150f);
            if (mode != 0) {
                tint(c, .5f * pull);
                glow(tipX, tipY, mode == 1 ? .3f : .2f);
            }
        }
    }

    /** Muzzle flash at release: bigger and brighter for the charged shot, widest for the volley. */
    private void drawReleaseFlash(Enemy enemy, int mode, Color c, float mx, float my, float aimAngle,
                                  Vector2 pos) {
        float f = enemy.getRangedShotFlashRemaining() / enemy.getRangedShotFlashDuration();
        float size = mode == 2 ? .75f : mode == 1 ? .7f : .38f;
        tintMixed(c, .75f, f);
        glow(mx, my, size * (.7f + .6f * (1f - f)));
        int rays = mode == 1 ? 8 : mode == 2 ? 6 : 4;
        for (int k = 0; k < rays; k++) {
            float angle = aimAngle + k * 360f / rays;
            tintMixed(c, .5f, f * .9f);
            barFrom(mx, my, size * 1.4f * (1.1f - .3f * f), mode == 1 ? .07f : .05f, angle);
        }
        int beams = mode == 2 ? 3 : 1;
        float spreadDegrees = (float) Math.toDegrees(RangedAttackConfig.VOLLEY.spreadRadians);
        for (int i = 0; i < beams; i++) {
            float angle = aimAngle + (mode == 2 ? (i - 1) * spreadDegrees : 0f);
            float rad = (float) Math.toRadians(angle);
            tintMixed(c, .6f, f * .8f);
            barFrom(pos.x + (float) Math.cos(rad) * .65f, pos.y + .1f + (float) Math.sin(rad) * .65f,
                AIM_LINE_LENGTH * (1f - .4f * (1f - f)), mode == 1 ? .2f : .1f, angle);
        }
    }

    /** Arrow aligned with its velocity; length and motion trail scale with speed, colour with shot type. */
    private void drawArrows() {
        for (GameWorld.Arrow arrow : gameWorld.getArrows()) {
            float angle = (float) Math.toDegrees(Math.atan2(arrow.velocity.y, arrow.velocity.x));
            float rad = (float) Math.toRadians(angle);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            Color c = shotColor(arrow.mode());
            float length = arrow.speed * .04f;
            float thickness = arrow.mode() == 1 ? .12f : .09f;
            float tailX = arrow.position.x - cos * length;
            float tailY = arrow.position.y - sin * length;
            float trail = arrow.speed * (arrow.mode() == 1 ? .1f : .07f);
            int steps = 4;
            for (int i = 0; i < steps; i++) {
                float f = i / (float) steps;
                float segmentLength = trail / steps;
                float back = f * trail + segmentLength;
                tint(c, (arrow.mode() == 1 ? .55f : .35f) * (1f - f));
                barFrom(tailX - cos * back, tailY - sin * back, segmentLength, thickness * (1f - .5f * f), angle);
            }
            if (arrow.mode() == 1) {
                tint(c, .55f);
                glow(arrow.position.x - cos * length * .5f, arrow.position.y - sin * length * .5f, .38f);
            }
            tintMixed(c, .25f, 1f);
            barFrom(tailX, tailY, length, thickness * .55f, angle);
            tint(COLOR_STEEL, 1f);
            barCentered(arrow.position.x, arrow.position.y, .16f, thickness * 1.4f, angle);
            tint(c, 1f);
            barFrom(tailX, tailY, .14f, .035f, angle + 150f);
            barFrom(tailX, tailY, .14f, .035f, angle - 150f);
        }
        spriteBatch.setColor(Color.WHITE);
    }

    /** Arrow impacts: hero hits flash white and burst; wall hits are small dry chips with a different sound. */
    private void drawArrowImpacts() {
        for (GameWorld.ArrowImpact impact : gameWorld.getArrowImpacts()) {
            if (impact.consumeSoundCue())
                (impact.hero ? arrowHitHeroSound : arrowHitWallSound)
                    .play(soundVolume(impact.x, impact.y, impact.hero ? .7f : .45f));
            float p = impact.getProgress();
            float fade = 1f - p;
            Color c = shotColor(impact.mode);
            if (impact.hero) {
                float scale = impact.mode == 1 ? 1.45f : 1f;
                spriteBatch.setColor(1f, 1f, 1f, fade * fade);
                glow(impact.x, impact.y, (.3f + .45f * p) * scale);
                tintMixed(c, .5f, fade);
                ring(impact.x, impact.y, (.12f + .65f * p) * scale);
                int sparks = impact.mode == 1 ? 9 : 6;
                for (int k = 0; k < sparks; k++) {
                    float angle = impact.normalAngle + (k / (float) (sparks - 1) - .5f) * 110f;
                    float rad = (float) Math.toRadians(angle);
                    float dist = (.1f + .55f * p) * scale * (.8f + .4f * ((k * 37) % 5) / 4f);
                    spriteBatch.setColor(1f, .38f + .4f * fade, .3f, fade);
                    barCentered(impact.x + (float) Math.cos(rad) * dist, impact.y + (float) Math.sin(rad) * dist,
                        .2f * fade * scale + .04f, .045f, angle);
                }
            } else {
                // Wall/floor hit: a short burst, then the arrow stays embedded in the surface and fades out.
                float t = p * impact.getDuration();
                float embedFade = clamp01((impact.getDuration() - t) / .35f);
                float stuckAngle = impact.angle;
                float sr = (float) Math.toRadians(stuckAngle);
                tint(shotColor(impact.mode), embedFade);
                barFrom(impact.x - (float) Math.cos(sr) * .42f, impact.y - (float) Math.sin(sr) * .42f, .42f,
                    impact.mode == 1 ? .07f : .05f, stuckAngle);
                tint(COLOR_STEEL, embedFade);
                barCentered(impact.x, impact.y, .08f, .06f, stuckAngle);
                p = clamp01(t / GameWorld.ArrowImpact.WALL_BURST);
                fade = 1f - p;
                if (p >= 1f) continue;
                spriteBatch.setColor(.85f, .85f, .8f, .7f * fade);
                ring(impact.x, impact.y, .1f + .3f * p);
                for (int k = 0; k < 4; k++) {
                    float angle = impact.normalAngle + (k / 3f - .5f) * 110f;
                    float rad = (float) Math.toRadians(angle);
                    float dist = .06f + .4f * p;
                    spriteBatch.setColor(1f, .93f, .65f, fade);
                    barCentered(impact.x + (float) Math.cos(rad) * dist, impact.y + (float) Math.sin(rad) * dist,
                        .14f * fade + .03f, .035f, angle);
                }
                for (int k = 0; k < 2; k++) {
                    float angle = impact.normalAngle + (k == 0 ? -35f : 35f);
                    float rad = (float) Math.toRadians(angle);
                    spriteBatch.setColor(.6f, .58f, .55f, fade);
                    barCentered(impact.x + (float) Math.cos(rad) * (.1f + .3f * p),
                        impact.y + (float) Math.sin(rad) * (.1f + .3f * p) - 1.2f * p * p,
                        .06f, .06f, 0f);
                }
            }
        }
        spriteBatch.setColor(Color.WHITE);
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

    /** First authored wall sample; its solid silhouette is defined by start/0 tileRows. */
    private void drawCollapsedWallSample() {
        if (!"start".equals(gameWorld.getLocation().getLocation())) return;
        for (RoomInstance room : gameWorld.getLocation().getRooms()) {
            if (room.getRoomId() != 0 || !isRoomVisible(room)) continue;
            spriteBatch.setColor(.69f, .67f, .72f, 1f);
            spriteBatch.draw(collapsedWallSample,
                room.getWorldX(), room.getWorldY() + 2f, 6f, 4f);
            spriteBatch.setColor(Color.WHITE);
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
            AttackState attack = (AttackState) this.hero.getCombatStateMachine().getCurrentState();
            float time = hero.getCombatStateTime();
            // A hurried strike snaps through its pose faster: a sharp, jerky motion with a cut-short wind-up.
            if (attack.getRuntime().isHurried() && !attack.isHeavy()) time *= 1.35f;
            float pose = attack.isHeavy() ?
                // Braced stance while charging, a coiled crouch once locked, a full lunge in the dash,
                // then a slowly straightening recovery.
                time < attack.getStartup() * .7f ? 0f :
                    time < attack.getStartup() ? .13f :
                    time < attack.getActiveEnd() ? .26f :
                    time < attack.getActiveEnd() + .3f ? .13f : 0f :
                attack.getComboStep() == 2 ?
                    time < attack.getStartup() ? .26f :
                        time < attack.getActiveEnd() ? .13f : 0f : time;
            heroFrame = attack.isFacingLeft() ?
                heroAttackLeftAnimation.getKeyFrame(pose, false) :
                heroAttackRightAnimation.getKeyFrame(pose, false);
            if (displayedHeroAttack != attack) {
                displayedHeroAttack = attack;
                displayedHeroSwing = false;
                if (attack.isHeavy()) chargeSound.play(.5f);
            }
            if (attack.isHeavy()) {
                if (!readyPlayed && attack.getStartup() - time <= .1f && attack.getPhase(hero) == AttackState.Phase.STARTUP) {
                    readyPlayed = true;
                    readySound.play(.45f);
                }
            } else readyPlayed = false;
            if (rhythmAttack != attack) {
                if (attack.isHeavy()) readyPlayed = false;
                rhythmAttack = attack;
                rhythmResult = com.cursedarchie.platformer.combat.InputResult.NO_INPUT;
            }
            com.cursedarchie.platformer.combat.InputResult inputNow = attack.getRuntime().inputResult();
            if (inputNow != rhythmResult) {
                rhythmResult = inputNow;
                // Botched wind-up for a press that came too soon; a bright tick for a well-timed one.
                if (inputNow == com.cursedarchie.platformer.combat.InputResult.EARLY_INPUT) hurriedSlash.play(.5f);
                else if (inputNow == com.cursedarchie.platformer.combat.InputResult.COMBO_BUFFERED) comboChime.play(.4f);
            }
            if (!displayedHeroSwing && attack.getRuntime().phase() != AttackRuntime.Phase.STARTUP) {
                displayedHeroSwing = true;
                (attack.isHeavy() ? heavySlash : attack.getComboStep() == 2 ? secondSlash : firstSlash)
                    .play(attack.isHeavy() ? .55f : .35f);
            }
        } else if (this.hero.getMotionStateMachine().getCurrentState() instanceof MoveState) {
            displayedHeroAttack = null;
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
        } else if (hero.getHeavyState() == com.cursedarchie.platformer.combat.HeavyState.HEAVY_RECOVERY) {
            spriteBatch.setColor(1f, .82f, .78f, 1f);   // winded and exposed after the dash
        }
        float drawX = hero.getPosition().x - hero.getSize()/2f;
        float drawY = hero.getPosition().y - hero.getSize()/2f;
        float drawSize = hero.getSize();
        if (hero.getCombatStateMachine().getCurrentState() instanceof AttackState &&
            ((AttackState) hero.getCombatStateMachine().getCurrentState()).getRuntime().isHurried()) {
            // Hurried strike: the hero lurches forward off balance and wobbles through the extended recovery.
            AttackState slip = (AttackState) hero.getCombatStateMachine().getCurrentState();
            float lurch = slip.isFacingLeft() ? -1f : 1f;
            float settle = 1f - clamp01((hero.getCombatStateTime() - slip.getRuntime().comboWindowStart()) /
                Math.max(.05f, slip.getRuntime().duration() - slip.getRuntime().comboWindowStart()));
            drawX += lurch * .06f + (float) Math.sin(effectClock * 55f) * .035f * settle;
            drawY -= .035f * settle;
        }
        // Flinch when an arrow lands: shoved along the arrow's path with a quick shake that settles.
        float arrowReaction = gameWorld.getHeroArrowReactionProgress();
        if (arrowReaction >= 0f && enteringPortal == null) {
            float settle = 1f - arrowReaction;
            drawX += gameWorld.getHeroArrowReactionDirection() * .16f * settle +
                (float) Math.sin(arrowReaction * 60f) * .03f * settle;
        }
        if (enteringPortal != null) {
            RoomInstance room = gameWorld.getCurrentRoom();
            float targetX = room.getWorldX() + enteringPortal.position.x;
            float targetY = room.getWorldY() + enteringPortal.position.y;
            drawSize *= 1f - .62f * portalProgress;
            drawX = hero.getPosition().x + (targetX - hero.getPosition().x) * portalProgress - drawSize / 2f;
            drawY = hero.getPosition().y + (targetY - hero.getPosition().y + .8f) * portalProgress - drawSize / 2f;
        }
        if (hero.getHeavyState() == com.cursedarchie.platformer.combat.HeavyState.HEAVY_DASH) {
            // Afterimages trail behind the dash: the hero sweeps forward rather than teleporting.
            float back = ((AttackState) hero.getCombatStateMachine().getCurrentState()).isFacingLeft() ? 1f : -1f;
            for (int k = 4; k >= 1; k--) {
                spriteBatch.setColor(1f, .72f, .4f, .42f - k * .08f);
                spriteBatch.draw(heroFrame, drawX + back * k * .42f, drawY, drawSize, drawSize);
            }
            spriteBatch.setColor(Color.WHITE);
        }
        spriteBatch.draw(
            heroFrame,
            drawX, drawY, drawSize, drawSize
        );
        if (arrowReaction >= 0f && arrowReaction < .35f) {
            // Brief white flash over the hero before the red damage tint takes over.
            spriteBatch.setColor(1f, 1f, 1f, .75f * (1f - arrowReaction / .35f));
            glow(drawX + drawSize / 2f, drawY + drawSize / 2f, .75f);
            spriteBatch.setColor(Color.WHITE);
        }
        if (hero.isBlocking() || hero.isBlockBroken() || hero.getBlockFlashRemaining() > 0f) {
            float direction = hero.isFacingLeft() ? -1f : 1f;
            spriteBatch.setColor(hero.isBlockBroken() ? 1f :
                hero.getBlockFlashRemaining() > 0f ? .9f : .38f,
                hero.isBlockBroken() ? .28f : .75f, .95f, .9f);
            spriteBatch.draw(sceneryPixel, hero.getPosition().x + direction * .5f - .11f,
                hero.getPosition().y - .32f, .22f, .85f);
            spriteBatch.setColor(.28f, .83f, 1f, .85f);
            spriteBatch.draw(sceneryPixel, hero.getPosition().x - .48f,
                hero.getPosition().y + .75f,
                .96f * hero.getBlockStability() / hero.getMaxBlockStability(), .06f);
        }
        if (hero.isRolling()) {
            // Roll: squashed, low hero with fading afterimages and a dust puff; a faint white shimmer marks the i-frames.
            float dir = hero.getRollDirection();
            float hx = hero.getPosition().x, hy = hero.getPosition().y - hero.getSize() / 2f;
            spriteBatch.setColor(1f, 1f, 1f, hero.isRollInvulnerable() ? .5f : .2f);
            for (int i = 1; i <= 3; i++)
                spriteBatch.draw(sceneryPixel, hx - dir * i * .35f - .2f, hy + .05f, .4f, .5f - i * .08f);
            spriteBatch.setColor(.75f, .7f, .6f, .6f);
            spriteBatch.draw(sceneryPixel, hx - dir * .55f - .15f, hy, .3f, .12f);
            if (hero.isRollInvulnerable()) { spriteBatch.setColor(.8f, .95f, 1f, .5f); ring(hx, hy + .4f, .75f); }
        }
        if (hero.getPerfectFlashRemaining() > 0f) {
            // Perfect block: bright gold ring and cross flash on the guard.
            float t = hero.getPerfectFlashRemaining() / .35f;
            float dir = hero.isFacingLeft() ? -1f : 1f;
            float px = hero.getPosition().x + dir * .55f, py = hero.getPosition().y + .15f;
            spriteBatch.setColor(1f, .9f, .35f, t);
            ring(px, py, .25f + (1f - t) * .9f);
            barCentered(px, py, .9f * t + .2f, .06f, 0f);
            barCentered(px, py, .9f * t + .2f, .06f, 90f);
        }
        if (hero.isCounterReady()) {
            // Counter window open: pulsing gold edge on the blade side tells the player to strike now.
            float pulse = .5f + .4f * (float) Math.sin(effectClock * 20f);
            float dir = hero.isFacingLeft() ? -1f : 1f;
            spriteBatch.setColor(1f, .85f, .3f, pulse);
            spriteBatch.draw(sceneryPixel, hero.getPosition().x + dir * .5f - .04f, hero.getPosition().y - .35f, .08f, 1f);
        }
        if (hero.getBlockHeavyFlashRemaining() > 0f) {
            // Partial absorb of a heavy blow: orange shock ring on the guard, bigger than the light spark.
            float t = hero.getBlockHeavyFlashRemaining() / .3f;
            float dir = hero.isFacingLeft() ? -1f : 1f;
            spriteBatch.setColor(1f, .55f, .2f, t);
            ring(hero.getPosition().x + dir * .55f, hero.getPosition().y + .1f, .25f + (1f - t) * .7f);
        }
        if (hero.getBlockBreakFlashRemaining() > 0f) {
            // Guard shattered: red burst and a downward sag cue over the hero.
            float t = hero.getBlockBreakFlashRemaining() / .45f;
            float dir = hero.isFacingLeft() ? -1f : 1f;
            float bx = hero.getPosition().x + dir * .5f, by = hero.getPosition().y + .1f;
            spriteBatch.setColor(1f, .25f, .2f, t);
            for (int i = 0; i < 8; i++) barFrom(bx, by, .15f + .6f * (1f - t), .05f, i * 45f);
            ring(bx, by, .3f + (1f - t));
        }
        drawBreakthroughImpact();
        if (hero.getCombatStateMachine().getCurrentState() instanceof AttackState) {
            AttackState attack = (AttackState) hero.getCombatStateMachine().getCurrentState();
            if (attack.isHeavy() && attack.getPhase(hero) == AttackState.Phase.STARTUP) {
                // Charge-up: converging sparks, a swelling glow and a ground ring that tightens as the strike nears.
                float charge = clamp01(hero.getCombatStateTime() / attack.getStartup());
                float cx = hero.getPosition().x, cy = hero.getPosition().y + .2f;
                spriteBatch.setColor(1f, .5f, .15f, .25f + .5f * charge);
                glow(cx, cy, .5f + .5f * charge);
                spriteBatch.setColor(1f, .85f, .4f, .9f * charge);
                for (int i = 0; i < 6; i++) {
                    float ang = i * 60f + effectClock * 90f;
                    float r = 1.1f * (1f - charge) + .25f;
                    barCentered(cx + (float) Math.cos(Math.toRadians(ang)) * r,
                        cy + (float) Math.sin(Math.toRadians(ang)) * r, .22f, .05f, ang);
                }
                ring(cx, hero.getPosition().y - hero.getSize() / 2f + .1f, 1.3f - .6f * charge);
            }
            if (attack.isHeavy()) drawBreakthroughEffects(attack);
            else if (attack.getRuntime().isHurried() && attack.isInRecovery(hero)) {
                // A rushed strike leaves the hero visibly open: a flickering amber bar that drains with the recovery.
                float p = clamp01((hero.getCombatStateTime() - attack.getActiveEnd()) /
                    Math.max(.05f, attack.getRuntime().duration() - attack.getActiveEnd()));
                spriteBatch.setColor(1f, .55f, .25f, .8f * (.7f + .3f * (float) Math.sin(effectClock * 30f)));
                spriteBatch.draw(sceneryPixel, hero.getPosition().x - .4f * (1f - p),
                    hero.getPosition().y + hero.getSize() / 2f + .12f, .8f * (1f - p), .05f);
                spriteBatch.setColor(Color.WHITE);
            }
            if (attack.usesRhythm() && attack.getRuntime().isInComboWindow() && attack.chainsToNext()) {
                // Timing cue: a brief glint on the blade hand while a press would continue the combo.
                // Strong for new players, subtle once the rhythm has been mastered.
                float strength = com.cursedarchie.platformer.combat.AttackRhythmConfig.get()
                    .highlightIntensity(hero.getRhythmCombos());
                float dirH = attack.isFacingLeft() ? -1f : 1f;
                float pulse = .6f + .4f * (float) Math.sin(effectClock * 28f);
                spriteBatch.setColor(1f, .9f, .45f, .5f * strength * pulse);
                glow(hero.getPosition().x + dirH * .85f, hero.getPosition().y + .12f, .28f + .22f * strength);
                spriteBatch.setColor(1f, .97f, .75f, .8f * strength);
                barCentered(hero.getPosition().x + dirH * .85f, hero.getPosition().y + .12f, .22f, .04f, 0f);
                barCentered(hero.getPosition().x + dirH * .85f, hero.getPosition().y + .12f, .22f, .04f, 90f);
            }
            if (attack.getRuntime().isHurried()) {
                // Slip: scuffed dust at the feet and a grey flinch mark; no spark of a successful follow-up.
                float dirH = attack.isFacingLeft() ? -1f : 1f;
                float fade = 1f - clamp01((hero.getCombatStateTime() - attack.getRuntime().comboWindowStart()) /
                    Math.max(.05f, attack.getRuntime().duration() - attack.getRuntime().comboWindowStart()));
                float footY = hero.getPosition().y - hero.getSize() / 2f + .05f;
                spriteBatch.setColor(.72f, .68f, .6f, .6f * fade);
                for (int i = 0; i < 3; i++)
                    spriteBatch.draw(sceneryPixel, hero.getPosition().x + dirH * (.25f + i * .2f) - .1f,
                        footY + i * .035f * (1f - fade), .22f - i * .04f, .06f);
                spriteBatch.setColor(.6f, .62f, .68f, .7f * fade);
                barCentered(hero.getPosition().x - dirH * .1f, hero.getPosition().y + .62f, .3f, .04f, dirH * 25f);
            }
            if (attack.getPhase(hero) == AttackState.Phase.STARTUP) {
                float direction = attack.isFacingLeft() ? -1f : 1f;
                float windup = hero.getCombatStateTime() / attack.getStartup();
                float y = attack.getComboStep() == 2 ? .46f : .18f;
                spriteBatch.setColor(attack.isHeavy() ? 1f : attack.getComboStep() == 2 ? .38f : .76f,
                    attack.isHeavy() ? .48f : .82f, 1f, .28f + .46f * windup);
                spriteBatch.draw(sceneryPixel, hero.getPosition().x - direction * .22f,
                    hero.getPosition().y + y, direction * .42f, .08f + .12f * windup);
            }
        }
        if (hero.isLeftAttackActive() || hero.isRightAttackActive()) {
            AttackState attack = (AttackState) hero.getCombatStateMachine().getCurrentState();
            float direction = attack.isFacingLeft() ? -1f : 1f;
            if (attack.getRuntime().isHurried()) {
                // Hurried slash: a short, dull, wobbly smear with no bright leading edge.
                float wobble = (float) Math.sin(effectClock * 70f) * .05f;
                spriteBatch.setColor(.58f, .6f, .66f, .5f);
                spriteBatch.draw(sceneryPixel, hero.getPosition().x + direction * .55f - .2f,
                    hero.getPosition().y + .08f + wobble, .4f, .1f);
                spriteBatch.setColor(Color.WHITE);
                return;
            }
            spriteBatch.setColor(attack.isHeavy() ? .98f : attack.getComboStep() == 2 ? .38f : .76f,
                attack.isHeavy() ? .55f : .86f, 1f, .8f);
            float trailY = attack.getComboStep() == 2 ? .34f : .1f;
            spriteBatch.draw(sceneryPixel, hero.getPosition().x + direction * .65f - .35f,
                hero.getPosition().y + trailY, .7f, attack.isHeavy() ? .24f : .13f);
            // The bright leading edge exists only during the gameplay-active window.
            spriteBatch.setColor(1f, .96f, .78f, .95f);
            spriteBatch.draw(sceneryPixel, hero.getPosition().x + direction *
                (attack.getComboStep() == 2 ? 1.55f : 1.12f) - .05f,
                hero.getPosition().y + trailY, .10f, .10f);
            if (attack.getComboStep() == 2) {
                spriteBatch.draw(sceneryPixel, hero.getPosition().x + direction * .95f - .24f,
                    hero.getPosition().y + .12f, .48f, .12f);
                // Well-timed follow-up: gold crescent sparks fan out from the blade.
                float burst = clamp01(1f - (hero.getCombatStateTime() - attack.getStartup()) / .2f);
                spriteBatch.setColor(1f, .85f, .4f, .9f * burst);
                for (int i = -1; i <= 1; i++)
                    barFrom(hero.getPosition().x + direction * 1.45f, hero.getPosition().y + .16f,
                        .3f + .25f * (1f - burst), .05f, (direction < 0f ? 180f : 0f) + direction * (i * 28f));
            }
        }
        spriteBatch.setColor(Color.WHITE);
    }

    /** How far a dash from (x0,y0) would travel before solid geometry stops it. */
    private float dashReach(float x0, float y0, final float dir, final float length) {
        final float[] best = {length};
        gameWorld.getWorld().rayCast(new com.badlogic.gdx.physics.box2d.RayCastCallback() {
            @Override public float reportRayFixture(Fixture f, Vector2 p, Vector2 n, float fraction) {
                if (!f.isSensor() && "level".equals(f.getBody().getUserData())) {
                    best[0] = Math.min(best[0], fraction * length);
                    return fraction;
                }
                return -1f;
            }
        }, new Vector2(x0, y0), new Vector2(x0 + dir * length, y0));
        return best[0];
    }

    /** Range arrow, dash effects and the winded recovery of the breakthrough attack. */
    private void drawBreakthroughEffects(AttackState attack) {
        com.cursedarchie.platformer.combat.BreakthroughConfig cfg = com.cursedarchie.platformer.combat.BreakthroughConfig.get();
        float dir = attack.isFacingLeft() ? -1f : 1f;
        float hx = hero.getPosition().x, hy = hero.getPosition().y;
        float footY = hy - hero.getSize() / 2f + .12f;
        float t = hero.getCombatStateTime();
        float planned = attack.getPlannedDistance();
        float originX = attack.getDashOriginX();
        AttackState.Phase phase = attack.getPhase(hero);
        if (phase == AttackState.Phase.STARTUP) {
            float p = clamp01(t / Math.max(.05f, attack.getStartup()));
            float pulse = .6f + .4f * (float) Math.sin(effectClock * (14f + 20f * p));
            // The arrow is exactly as long as the real dash: it ends where the hero will stop (shortened by walls).
            drawDashArrow(hx, footY, dir, planned, p, pulse, 0f);
            // Braced body glow that tightens as the charge fills.
            spriteBatch.setColor(1f, .75f + .2f * p, .4f + .4f * p, .12f + .45f * p * pulse);
            glow(hx, hy, .6f + .5f * p);
            if (p > .7f) {
                spriteBatch.setColor(1f, .95f, .7f, (p - .7f) * 2f * (.6f + .4f * pulse));
                ring(hx, hy, .9f - .3f * p);
            }
        } else if (phase == AttackState.Phase.ACTIVE) {
            float sinceDash = Math.max(0f, t - attack.getStartup());
            float walked = Math.abs(hx - originX);
            // The arrow is erased as the hero runs over it: only the part still ahead is left.
            if (!attack.isDashEnded()) drawDashArrow(hx + dir * .4f, footY, dir, Math.max(0f, planned - walked), 1f, 1f, walked);
            // Acceleration burst at the start of the dash.
            if (sinceDash < .12f) {
                float k = sinceDash / .12f;
                spriteBatch.setColor(1f, .95f, .75f, 1f - k);
                for (int i = 0; i < 7; i++) barFrom(originX, hy + (i - 3) * .12f, .5f + 1.2f * k, .05f, dir < 0f ? 0f : 180f);
                ring(originX, hy, .4f + k * .9f);
            }
            // Slash line along the covered path and fading afterimages behind the hero.
            spriteBatch.setColor(1f, .92f, .7f, .75f);
            barCentered((originX + hx) / 2f, hy, Math.max(.05f, walked), .07f, 0f);
            spriteBatch.setColor(1f, .6f, .25f, .35f);
            barCentered((originX + hx) / 2f, hy + .12f, Math.max(.05f, walked), .14f, 0f);
            TextureRegion ghost = attack.isFacingLeft() ? heroAttackLeftAnimation.getKeyFrame(.26f, false) :
                heroAttackRightAnimation.getKeyFrame(.26f, false);
            for (int i = 1; i <= 4; i++) {
                float gx = hx - dir * Math.min(walked, i * .45f);
                spriteBatch.setColor(1f, .75f, .4f, .38f - i * .07f);
                spriteBatch.draw(ghost, gx - hero.getSize() / 2f, hy - hero.getSize() / 2f, hero.getSize(), hero.getSize());
            }
            // First enemy struck: a stronger, longer reaction.
            if (attack.getFirstHitTime() >= 0f) {
                float k = clamp01((t - attack.getFirstHitTime()) / .25f);
                if (k < 1f) {
                    spriteBatch.setColor(1f, .9f, .6f, 1f - k);
                    ring(attack.getFirstHitX(), attack.getFirstHitY(), .4f + k * 1.3f);
                    for (int i = 0; i < 8; i++) barFrom(attack.getFirstHitX(), attack.getFirstHitY(), .2f + .8f * k, .06f, i * 45f);
                }
            }
        } else {
            float since = t - attack.getActiveEnd();
            float p = clamp01(since / Math.max(.05f, attack.getRuntime().duration() - attack.getActiveEnd()));
            // Stop effect where the dash ended: dust, a ring and braking streaks.
            if (since < .3f && attack.isDashEnded()) {
                float k = since / .3f, ex = attack.getDashEndX();
                spriteBatch.setColor(.85f, .8f, .7f, .8f * (1f - k));
                ring(ex, footY + .1f, .3f + k * .8f);
                for (int i = 0; i < 4; i++)
                    spriteBatch.draw(sceneryPixel, ex - dir * (.2f + i * .22f * (1f + k)) - .1f, footY + k * .3f * (i % 2 + 1), .22f, .08f);
            }
            // The hero is spent and open until the amber bar runs out.
            float flicker = .7f + .3f * (float) Math.sin(effectClock * 30f);
            spriteBatch.setColor(1f, .55f, .25f, .8f * flicker);
            spriteBatch.draw(sceneryPixel, hx - .4f * (1f - p), hy + hero.getSize() / 2f + .12f, .8f * (1f - p), .05f);
        }
        spriteBatch.setColor(Color.WHITE);
    }

    /**
     * The dash-range arrow: {@code length} long from just in front of {@code x}. {@code fill} (0..1) is how much
     * of it is lit; its colour and thickness intensify with the charge.
     */
    private void drawDashArrow(float x, float y, float dir, float length, float fill, float pulse, float erased) {
        if (length < .05f) return;
        int segments = Math.max(1, (int) (length / .3f));
        float seg = length / segments, thick = .05f + .05f * fill;
        for (int i = 0; i < segments; i++) {
            float px = x + dir * (.5f + (i + .5f) * seg);
            boolean lit = (i + .5f) / segments <= fill;
            if (lit) spriteBatch.setColor(1f, .55f + .4f * fill, .2f + .5f * fill, .55f + .4f * fill);
            else spriteBatch.setColor(.6f, .8f, 1f, .18f + .12f * pulse);
            barCentered(px, y, seg * .8f, thick, 0f);
        }
        float tipX = x + dir * (.5f + length);
        if (fill >= .98f) spriteBatch.setColor(1f, 1f, .85f, .95f); else spriteBatch.setColor(1f, .7f, .35f, .35f + .5f * fill);
        for (int s = -1; s <= 1; s += 2) barFrom(tipX, y, .3f, thick * 1.2f, (dir < 0f ? 0f : 180f) + s * 35f);
    }

    private void drawBreakthroughImpact() {
        if (hero.getBreakthroughImpactId() != impactSeen) {
            impactSeen = hero.getBreakthroughImpactId();
            Sound bump = eventSounds.get(GameWorld.CombatEvent.Kind.WALL_BUMP);
            if (bump != null) bump.play(.8f);
        }
        float rem = hero.getBreakthroughImpactRemaining();
        if (rem <= 0f) return;
        float k = rem / .45f, x = hero.getBreakthroughImpactX(), y = hero.getBreakthroughImpactY();
        spriteBatch.setColor(1f, .9f, .6f, k);
        ring(x, y, .3f + (1f - k) * 1.1f);
        for (int i = 0; i < 8; i++) barFrom(x, y, .15f + .5f * (1f - k), .05f, i * 45f);
        spriteBatch.setColor(.75f, .7f, .62f, .7f * k);
        spriteBatch.draw(sceneryPixel, x - hero.getBreakthroughImpactDirection() * .3f - .15f, y - .5f, .3f, .1f + .2f * (1f - k));
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
                drawAwarenessMark(enemy);
                if ("scavenger".equals(enemy.getArchetypeId())) {
                    drawArcher(enemy);
                    continue;
                }
                TextureRegion enemyFrame;


                enemyFrame = enemy.isFacingLeft() ? enemyIdleLeft : enemyIdleRight;
                if (enemy.getCombatStateMachine().getCurrentState() instanceof com.cursedarchie.platformer.actors.enemies.states.combat.AttackState) {
                    com.cursedarchie.platformer.actors.enemies.states.combat.AttackState attack =
                        (com.cursedarchie.platformer.actors.enemies.states.combat.AttackState)
                            enemy.getCombatStateMachine().getCurrentState();
                    float shownFrame = enemy.getCombatStateTime() < attack.startup(enemy)
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

                if (enemy.getDodgeFlashRemaining() > 0f) {
                    spriteBatch.setColor(.4f, 1f, 1f, .8f);
                } else if (enemy.getStompFlashRemaining() > 0f) {
                    spriteBatch.setColor(.8f, .75f, .6f, 1f);
                } else if (enemy.isDashWindingUp()) {
                    spriteBatch.setColor(1f, .85f, .3f, 1f);   // crouched, amber: dash is coming
                } else if (enemy.isDashing()) {
                    spriteBatch.setColor(1f, 1f, 1f, .75f);
                } else if (enemy.getSupportFlashRemaining() > 0f) {
                    spriteBatch.setColor(.5f, 1f, .55f, 1f);
                } else if (gameWorld.hasRecentDamage(enemy)) {
                    spriteBatch.setColor(1f, 0.35f, 0.35f, 1f);
                } else if (enemy.getCombatStateMachine().getCurrentState() instanceof
                    com.cursedarchie.platformer.actors.enemies.states.combat.AttackState &&
                    enemy.getCombatStateTime() < ((com.cursedarchie.platformer.actors.enemies.states.combat.AttackState)
                        enemy.getCombatStateMachine().getCurrentState()).startup(enemy)) {
                    spriteBatch.setColor(1f, ((com.cursedarchie.platformer.actors.enemies.states.combat.AttackState)
                        enemy.getCombatStateMachine().getCurrentState()).isLunge() ? .36f : .68f,
                        .55f, 1f);
                }
                Texture character = "scavenger".equals(enemy.getArchetypeId()) ? scavengerSprite :
                    "red_collector".equals(enemy.getArchetypeId()) ? redCollectorSprite : null;
                if (character != null) {
                    float visualWidth = "scavenger".equals(enemy.getArchetypeId()) ? 1.32f : 1.64f;
                    float visualHeight = "scavenger".equals(enemy.getArchetypeId()) ? 1.44f : 2.06f;
                    float left = enemy.getPosition().x - visualWidth / 2f;
                    float bottom = enemy.getPosition().y - enemy.getSize() / 2f;
                    boolean flip = "red_collector".equals(enemy.getArchetypeId())
                        ? !enemy.isFacingLeft() : enemy.isFacingLeft();
                    spriteBatch.draw(character, flip ? left + visualWidth : left,
                        bottom, flip ? -visualWidth : visualWidth, visualHeight);
                } else {
                    if ("heavy_hunter".equals(enemy.getArchetypeId()))
                        spriteBatch.setColor(.62f, .45f, .36f, 1f);
                    else if ("infiltrator".equals(enemy.getArchetypeId()))
                        spriteBatch.setColor(.45f, .95f, .9f, enemy.isActionRecovering() ? .6f : 1f);
                    else if ("overseer".equals(enemy.getArchetypeId()))
                        spriteBatch.setColor(.78f, .55f, 1f, 1f);
                    else if ("boss".equals(enemy.getArchetypeId()))
                        spriteBatch.setColor(.85f, .32f, .3f, 1f);
                    spriteBatch.draw(enemyFrame, enemy.getPosition().x - enemy.getSize()/2f,
                        enemy.getPosition().y - enemy.getSize()/2f, enemy.getSize(), enemy.getSize());
                }
                if (enemy.getCombatStateMachine().getCurrentState() instanceof
                    com.cursedarchie.platformer.actors.enemies.states.combat.AttackState) {
                    com.cursedarchie.platformer.actors.enemies.states.combat.AttackState attack =
                        (com.cursedarchie.platformer.actors.enemies.states.combat.AttackState)
                            enemy.getCombatStateMachine().getCurrentState();
                    if (attack.getRuntime() != null && attack.getRuntime().isActive() && !attack.isArea()) {
                        // The weapon swing is drawn across the real hitbox, so what you see is what can hit you.
                        Rectangle hb = attack.getRuntime().hitbox(enemy.getPosition(), new Rectangle());
                        spriteBatch.setColor(1f, .92f, .7f, .85f);
                        barCentered(hb.x + hb.width / 2f, hb.y + hb.height * .55f, hb.width, .09f,
                            attack.isFacingLeft() ? 12f : -12f);
                        spriteBatch.setColor(1f, .5f, .3f, .25f);
                        spriteBatch.draw(sceneryPixel, hb.x, hb.y, hb.width, hb.height);
                    }
                    if (attack.isLunge() && enemy.getCombatStateTime() < attack.startup(enemy)) {
                        spriteBatch.setColor(1f, .5f, .15f, .8f);
                        spriteBatch.draw(sceneryPixel,
                            enemy.getPosition().x + (attack.isFacingLeft() ? -1.6f : .35f),
                            enemy.getPosition().y - .3f, 1.25f, .09f);
                    }
                    if (attack.isArea() && enemy.getCombatStateTime() < attack.startup(enemy)) {
                        float charge = enemy.getCombatStateTime() / attack.startup(enemy);
                        float ground = enemy.getPosition().y - enemy.getSize() / 2f + .03f;
                        float dirA = attack.isFacingLeft() ? -1f : 1f;
                        if (attack.isSlam()) {
                            // Ground slam: a low red band on BOTH sides that fills outward; jump over it.
                            float r = attack.getDefinition().hitboxWidth / 2f;
                            spriteBatch.setColor(1f, .2f, .12f, .35f + .45f * charge);
                            spriteBatch.draw(sceneryPixel, enemy.getPosition().x - r, ground, r * 2f, .16f);
                            spriteBatch.setColor(1f, .85f, .3f, .9f);
                            spriteBatch.draw(sceneryPixel, enemy.getPosition().x - r * charge, ground + .16f,
                                r * 2f * charge, .06f);
                        } else {
                            // Wide sweep: the whole frontal arc lights up, shoulder-high.
                            float reach = attack.getDefinition().range;
                            spriteBatch.setColor(1f, .55f, .15f, .3f + .45f * charge);
                            spriteBatch.draw(sceneryPixel, dirA > 0f ? enemy.getPosition().x : enemy.getPosition().x - reach,
                                enemy.getPosition().y - .55f, reach, 1.2f);
                        }
                    }
                }
                playEnemyCues(enemy);
                drawRoleProfile(enemy);
                if ("default".equals(enemy.getArchetypeId())) drawRaiderEffects(enemy);
                if (enemy.isBlocking() && !"default".equals(enemy.getArchetypeId())) {
                    float direction = enemy.isFacingLeft() ? -1f : 1f;
                    spriteBatch.setColor(.35f, .75f, 1f, .9f);
                    spriteBatch.draw(sceneryPixel, enemy.getPosition().x + direction * .68f - .09f,
                        enemy.getPosition().y - .4f, .18f, 1.1f);
                }
                spriteBatch.setColor(Color.WHITE);
            }
        }
    }

    /** Alert "!" on first sight, "?" while heading for the last known spot or searching, and a quick turn whip. */
    private void drawAwarenessMark(Enemy enemy) {
        Vector2 pos = enemy.getPosition();
        float top = pos.y + enemy.getSize() / 2f + .35f;
        float t = enemy.getAwarenessTime();
        Enemy.Awareness a = enemy.getAwareness();
        if (a == Enemy.Awareness.ALERT && t < .7f) {
            float hop = (float) Math.abs(Math.sin(t * 14f)) * Math.max(0f, 1f - t / .5f) * .2f;
            spriteBatch.setColor(1f, .35f, .2f, 1f - t / .9f);
            barCentered(pos.x, top + hop + .14f, .09f, .26f, 0f);
            barCentered(pos.x, top + hop - .08f, .09f, .09f, 0f);
        } else if (a == Enemy.Awareness.LOST || a == Enemy.Awareness.SEARCHING) {
            float sway = a == Enemy.Awareness.SEARCHING ? (float) Math.sin(effectClock * 6f) * .06f : 0f;
            spriteBatch.setColor(1f, .85f, .3f, .85f);
            barCentered(pos.x + sway, top + .18f, .2f, .06f, 0f);
            barCentered(pos.x + sway + .08f, top + .09f, .06f, .18f, 0f);
            barCentered(pos.x + sway, top - .02f, .12f, .06f, 0f);
            barCentered(pos.x + sway, top - .18f, .07f, .07f, 0f);
        }
        if (enemy.getTurnFlashRemaining() > 0f) {
            // Quick turn: two short whip lines trailing from the new facing side.
            float f = enemy.getTurnFlashRemaining() / Enemy.TURN_FLASH;
            float dir = enemy.isFacingLeft() ? -1f : 1f;
            spriteBatch.setColor(1f, 1f, 1f, .6f * f);
            barCentered(pos.x - dir * .4f, pos.y + .35f, .5f * f + .1f, .04f, dir * 18f);
            barCentered(pos.x - dir * .4f, pos.y + .15f, .4f * f + .1f, .04f, dir * -18f);
        }
        spriteBatch.setColor(Color.WHITE);
    }

    /** Role identity visible before any attack: shape, colour accents and ambient effects per archetype. */
    private void drawRoleProfile(Enemy enemy) {
        String id = enemy.getArchetypeId();
        float x = enemy.getPosition().x, y = enemy.getPosition().y, top = y + enemy.getSize() / 2f;
        float dir = enemy.isFacingLeft() ? -1f : 1f;
        if ("heavy_hunter".equals(id)) {
            // Breaker: broad armoured shoulders and a dark brow band; reads as slow and heavy.
            spriteBatch.setColor(.35f, .3f, .28f, 1f);
            spriteBatch.draw(sceneryPixel, x - .85f, top - .5f, 1.7f, .22f);
            spriteBatch.setColor(.9f, .5f, .2f, 1f);
            spriteBatch.draw(sceneryPixel, x - .85f, top - .52f, .18f, .26f);
            spriteBatch.draw(sceneryPixel, x + .67f, top - .52f, .18f, .26f);
        } else if ("infiltrator".equals(id)) {
            // Infiltrator: slim teal streaks behind it while moving; afterimage on sidestep.
            float speed = Math.abs(enemy.getBody().getLinearVelocity().x);
            if (speed > 2f) {
                spriteBatch.setColor(.4f, 1f, .9f, .4f);
                for (int i = 1; i <= 2; i++)
                    spriteBatch.draw(sceneryPixel, x - Math.signum(enemy.getBody().getLinearVelocity().x) * (.3f + i * .22f),
                        y - .15f + i * .08f, .32f, .05f);
            }
            if (enemy.isFlanking()) {
                spriteBatch.setColor(1f, .3f, .3f, .85f);   // red mark: a lunge is coming
                spriteBatch.draw(sceneryPixel, x - .08f, top + .12f, .16f, .3f);
                spriteBatch.draw(sceneryPixel, x - .08f, top + .02f, .16f, .06f);
            }
        } else if ("overseer".equals(id)) {
            // Overseer: floating violet diamond halo; a beam to the ally it is empowering.
            float bob = (float) Math.sin(effectClock * 3f) * .05f;
            spriteBatch.setColor(.8f, .5f, 1f, .9f);
            barCentered(x, top + .3f + bob, .26f, .26f, 45f);
            if (enemy.isCasting() && enemy.getCastTarget() != null) {
                Enemy t = enemy.getCastTarget();
                float p = enemy.getCastProgress();
                spriteBatch.setColor(.8f, .5f, 1f, .25f + .6f * p);
                segment(x, y + .2f, t.getPosition().x, t.getPosition().y + .2f, .03f + .05f * p);
                ring(x, y, .3f + .6f * p);
            }
        }
        if (enemy.isBuffed()) {
            // Buffed ally: gold ring and spark over the head so the player knows it is empowered.
            spriteBatch.setColor(1f, .85f, .3f, .5f + .2f * (float) Math.sin(effectClock * 8f));
            ring(x, y, enemy.getSize() * .75f);
            spriteBatch.setColor(1f, .9f, .4f, .9f);
            barCentered(x, top + .35f, .22f, .06f, 0f);
            barCentered(x, top + .35f, .22f, .06f, 90f);
        }
        spriteBatch.setColor(Color.WHITE);
    }

    /** Raider presentation: dash telegraph/trail, raised-weapon guard stance, block sparks and guard break. */
    private void drawRaiderEffects(Enemy enemy) {
        float dir = enemy.isFacingLeft() ? -1f : 1f;
        float x = enemy.getPosition().x, y = enemy.getPosition().y;
        float ground = y - enemy.getSize() / 2f;
        if (enemy.isDashWindingUp()) {
            // Amber ground line pointing where the raider will lunge to.
            spriteBatch.setColor(1f, .8f, .2f, .75f);
            spriteBatch.draw(sceneryPixel, dir > 0f ? x + .4f : x - 2.2f, ground + .03f, 1.8f, .08f);
        }
        if (enemy.isDashing()) {
            spriteBatch.setColor(1f, 1f, 1f, .45f);
            for (int i = 1; i <= 3; i++)
                spriteBatch.draw(sceneryPixel, x - dir * (.35f + i * .3f) - .12f, y - .2f, .24f, .5f - i * .1f);
        }
        if (enemy.isBlocking()) {
            // Weapon raised in front: bright steel plate, taller than the old marker, fading as the guard ends.
            float a = .55f + .4f * clamp01(enemy.getBlockProgress() * 3f);
            spriteBatch.setColor(.75f, .85f, 1f, a);
            spriteBatch.draw(sceneryPixel, x + dir * .62f - .07f, y - .45f, .14f, 1.25f);
            spriteBatch.setColor(.35f, .6f, 1f, a * .7f);
            spriteBatch.draw(sceneryPixel, x + dir * .62f - .16f, y + .78f, .32f, .1f);
        }
        if (enemy.getBlockHitFlashRemaining() > 0f) {
            float t = enemy.getBlockHitFlashRemaining() / .22f;
            spriteBatch.setColor(1f, .95f, .6f, t);
            float sx = x + dir * .8f, sy = y + .2f;
            for (int i = 0; i < 6; i++) {
                float ang = (i - 2.5f) * 28f + (dir < 0f ? 180f : 0f);
                barFrom(sx, sy, .1f + .45f * (1f - t), .045f, ang);
            }
            glow(sx, sy, .3f * t + .1f);
        }
        if (enemy.getBlockBreakFlashRemaining() > 0f) {
            float t = enemy.getBlockBreakFlashRemaining() / .4f;
            spriteBatch.setColor(1f, .45f, .2f, t);
            ring(x + dir * .7f, y + .2f, .3f + (1f - t) * .8f);
            barCentered(x + dir * .7f, y + .2f, .9f * t + .2f, .07f, 45f);
            barCentered(x + dir * .7f, y + .2f, .9f * t + .2f, .07f, -45f);
        }
        spriteBatch.setColor(Color.WHITE);
    }

    private void drawDefeatEffects() {
        for (GameWorld.DefeatEffect effect : gameWorld.getDefeatEffects()) {
            float progress = effect.getProgress();
            if (effect.consumeSound()) deathSound.play(soundVolume(effect.x, effect.y, .6f));
            TextureRegion frame = effect.boss
                ? (effect.facingLeft ? bossIdleLeft : bossIdleRight)
                : (effect.facingLeft ? enemyIdleLeft : enemyIdleRight);
            // Topples away from the killing blow: rotates about the feet, drifts, then fades on the ground.
            float fall = Math.min(1f, progress * 1.8f);
            fall = fall * fall;
            float fade = progress < .6f ? 1f : 1f - (progress - .6f) / .4f;
            spriteBatch.setColor(1f, 0.7f + 0.3f * progress, 0.7f + 0.3f * progress, fade);
            float half = effect.size / 2f;
            spriteBatch.draw(frame, effect.x - half + effect.fallDirection * .5f * fall, effect.y - half,
                half, 0f, effect.size, effect.size, 1f, 1f, -effect.fallDirection * 85f * fall);
        }
        spriteBatch.setColor(Color.WHITE);
    }

    private void drawDamageIndicators() {
        for (GameWorld.DamageIndicator indicator : gameWorld.getDamageIndicators()) {
            float progress = indicator.getProgress();
            float rise = 0.84f * (1f - (1f - progress) * (1f - progress));
            float alpha = 1f - progress;
            // Placeholder impact tick: it is intentionally short and separate
            // from the swing cue; artists can replace this binding later.
            // A short cross-shaped spark anchors the health loss to a world-space hit.
            if (progress < .35f) {
                float spark = .18f * (1f - progress / .35f);
                spriteBatch.setColor(1f, .86f, .42f, alpha);
                spriteBatch.draw(sceneryPixel, indicator.impactX - spark, indicator.impactY - .025f,
                    spark * 2f, .05f);
                spriteBatch.draw(sceneryPixel, indicator.impactX - .025f, indicator.impactY - spark,
                    .05f, spark * 2f);
            }
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
        spriteBatch.setColor(Color.WHITE);
        damageFont.setColor(Color.WHITE);
    }

    /** One look and one sound per event kind: hit, block, guard break, stagger, whiff and wall bump differ. */
    private void drawCombatEvents() {
        for (GameWorld.CombatEvent event : gameWorld.getCombatEvents()) {
            float p = event.progress(), inv = 1f - p;
            boolean heavyHit = event.kind == GameWorld.CombatEvent.Kind.HIT_HEAVY;
            if (event.consumeSound()) {
                Sound sound = eventSounds.get(event.kind);
                float vol = soundVolume(event.x, event.y, event.kind == GameWorld.CombatEvent.Kind.WHIFF ? .3f : .7f);
                if (sound != null) sound.play(vol);
                if (event.stagger) staggerPopSound.play(vol * .6f);
            }
            float x = event.x, y = event.y, d = event.direction == 0f ? 1f : event.direction;
            switch (event.kind) {
                case HIT_LIGHT: case HIT_HEAVY: {
                    // Impact burst thrown along the blow; heavy is larger, orange and adds a shock ring.
                    float len = (heavyHit ? .9f : .45f) * (.4f + p);
                    spriteBatch.setColor(1f, heavyHit ? .55f : .9f, heavyHit ? .2f : .4f, inv);
                    for (int i = -2; i <= 2; i++)
                        barFrom(x, y, len, heavyHit ? .08f : .045f, (d < 0f ? 180f : 0f) + i * (heavyHit ? 24f : 18f));
                    if (heavyHit) ring(x, y, .3f + p * .9f);
                    break;
                }
                case BLOCK_METAL: {
                    spriteBatch.setColor(.85f, .92f, 1f, inv);
                    for (int i = 0; i < 6; i++) barFrom(x + d * -.7f, y, .15f + .4f * p, .04f, i * 60f);
                    break;
                }
                case BLOCK_BREAK: {
                    spriteBatch.setColor(1f, .35f, .2f, inv);
                    ring(x, y, .35f + p * 1.1f);
                    for (int i = 0; i < 8; i++) barFrom(x, y, .2f + .8f * p, .05f, i * 45f);
                    break;
                }
                case BLOCK_HERO: case BLOCK_HERO_HEAVY: {
                    // Dull cyan pulse: the hero absorbs, nothing metallic.
                    spriteBatch.setColor(.4f, .8f, 1f, inv * .8f);
                    ring(x + d * -.5f, y, .25f + p * (event.kind == GameWorld.CombatEvent.Kind.BLOCK_HERO_HEAVY ? .9f : .5f));
                    break;
                }
                case WHIFF: {
                    // Pale thin arc where the swing found nothing.
                    spriteBatch.setColor(.85f, .9f, 1f, inv * .6f);
                    barCentered(x, y, .9f, .025f, d < 0f ? 200f : -20f);
                    break;
                }
                case HEAD_STOMP: {
                    // Dust ring spreading across the top of the head and two downward chevrons: "hit from above".
                    spriteBatch.setColor(.85f, .8f, .7f, inv);
                    barCentered(x, y, .4f + 1.0f * p, .05f, 0f);
                    ring(x, y, .2f + .5f * p);
                    spriteBatch.setColor(1f, .9f, .5f, inv);
                    barCentered(x - .15f, y + .45f - .15f * p, .3f, .05f, -35f);
                    barCentered(x + .15f, y + .45f - .15f * p, .3f, .05f, 35f);
                    break;
                }
                case WALL_BUMP: {
                    spriteBatch.setColor(.8f, .75f, .65f, inv);
                    for (int i = 0; i < 5; i++) barFrom(x + d * .4f, y - .2f, .12f + .3f * p, .05f, 90f + (i - 2) * 25f);
                    break;
                }
            }
            if (event.stagger) {
                // Stagger: small white ring above the head, separate from the damage burst.
                spriteBatch.setColor(1f, 1f, 1f, inv);
                ring(x, y + .8f, .15f + p * .35f);
            }
        }
        spriteBatch.setColor(Color.WHITE);
    }

    private void drawCombatDebug() {
        debugRenderer.setProjectionMatrix(cam.combined);
        debugRenderer.begin(ShapeType.Line);
        Rectangle box = new Rectangle();
        hero.getHurtboxBounds(box);
        debugRenderer.setColor(Color.CYAN);
        debugRenderer.rect(box.x, box.y, box.width, box.height);
        drawAttackRuntime(hero.getPosition(), hero.getCombatStateMachine().getCurrentState(), box);
        for (Enemy enemy : gameWorld.getAliveEnemies()) {
            enemy.getHurtboxBounds(box);
            debugRenderer.setColor(Color.GREEN);
            debugRenderer.rect(box.x, box.y, box.width, box.height);
            drawAttackRuntime(enemy.getPosition(), enemy.getCombatStateMachine().getCurrentState(), box);
        }
        debugRenderer.setColor(Color.MAGENTA);
        for (GameWorld.Arrow arrow : gameWorld.getArrows())
            debugRenderer.rect(arrow.position.x - GameWorld.Arrow.WIDTH / 2f,
                arrow.position.y - GameWorld.Arrow.HEIGHT / 2f,
                GameWorld.Arrow.WIDTH, GameWorld.Arrow.HEIGHT);
        debugRenderer.end();
    }

    private void drawAttackRuntime(Vector2 origin, Object state, Rectangle box) {
        AttackRuntime runtime = null;
        if (state instanceof AttackState) runtime = ((AttackState) state).getRuntime();
        else if (state instanceof com.cursedarchie.platformer.actors.enemies.states.combat.AttackState)
            runtime = ((com.cursedarchie.platformer.actors.enemies.states.combat.AttackState) state).getRuntime();
        if (runtime == null) return;
        runtime.hitbox(origin, box);
        debugRenderer.setColor(runtime.phase() == AttackRuntime.Phase.ACTIVE ? Color.RED :
            runtime.phase() == AttackRuntime.Phase.STARTUP ? Color.YELLOW : Color.GRAY);
        debugRenderer.rect(box.x, box.y, box.width, box.height);
        debugRenderer.line(origin.x, origin.y,
            origin.x + runtime.direction() * runtime.definition().range, origin.y);
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
        firstSlash.dispose();
        secondSlash.dispose();
        heavySlash.dispose();
        hurriedSlash.dispose();
        chargeSound.dispose();
        readySound.dispose();
        comboChime.dispose();
        for (Sound cue : archerCueSounds) if (cue != null) cue.dispose();
        arrowHitHeroSound.dispose();
        for (Sound sound : eventSounds.values()) if (sound != null) sound.dispose();
        staggerPopSound.dispose();
        deathSound.dispose();
        arrowHitWallSound.dispose();
        glowTexture.dispose();
        ringTexture.dispose();
        damageFont.dispose();
        itemLabelFont.dispose();
        bandageSprite.dispose();
        noteSprite.dispose();
        keeperSprite.dispose();
        collapsedWallSample.dispose();
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
