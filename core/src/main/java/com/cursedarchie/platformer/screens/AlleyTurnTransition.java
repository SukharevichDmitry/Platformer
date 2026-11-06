package com.cursedarchie.platformer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ScreenUtils;
import com.cursedarchie.platformer.levels.TransitionProfile;
import com.cursedarchie.platformer.levels.wrappers.InteractionData;
import com.cursedarchie.platformer.world.WorldRenderer;

/**
 * 2.5D camera turn between a street and an alley, timed by a {@link TransitionProfile}. The street and the alley
 * are captured as flat 2D frames <em>without the hero</em> and shown on two planes at a right angle; the camera
 * orbits the building corner (counter-clockwise into an alley, clockwise out of it). The hero is its own layer on
 * the plane it stands on and turns into the depth with the {@code alley_turn} frames. A foreground stone column
 * sweeps across the screen and fully hides the hero while the backend swaps the active location, so the jump to
 * the new spawn is never visible. After the turn the camera eases slightly right onto the gameplay view; control
 * stays locked (the world is not updated) until then. Box2D is never rotated.
 */
final class AlleyTurnTransition implements Disposable {
    private static final float PLANE_HEIGHT = 9f;
    private static final float CORNER_WIDTH = .34f;
    /** Hero layer sits this far in front of its plane so depth testing never hides it in the wall it stands on. */
    private static final float HERO_LIFT = .03f;
    /** Width of the hero's body in world units, used to size the column's full cover. */
    private static final float HERO_BODY_WIDTH = .8f;

    private final PerspectiveCamera camera = new PerspectiveCamera(52f, 1f, 1f);
    private final ShaderProgram shader;
    private final Mesh heroMesh;
    private final float[] heroVertices = new float[20];
    private final SpriteBatch overlay = new SpriteBatch();
    private final OrthographicCamera overlayCamera = new OrthographicCamera();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont debugFont = new BitmapFont();
    private final ObjectMap<String, Sound> sounds = new ObjectMap<>();
    private final Texture white;
    private Mesh streetPlane;
    private Mesh alleyPlane;
    private Mesh cornerPlane;
    private FrameBuffer source;
    private FrameBuffer target;
    private int width = 1;
    private int height = 1;
    private TransitionProfile profile;
    private float time;
    private boolean reverse;
    private boolean active;
    private boolean switched;
    private boolean failed;
    private boolean debug;
    private WorldRenderer.HeroLayer heroBefore;
    private WorldRenderer.HeroLayer heroAfter;
    private WorldRenderer renderer;
    private TextureRegion stone;

    AlleyTurnTransition() {
        ShaderProgram.pedantic = false;
        shader = new ShaderProgram(
            "attribute vec3 a_position;\n" +
                "attribute vec2 a_texCoord0;\n" +
                "uniform mat4 u_projTrans;\n" +
                "varying vec2 v_uv;\n" +
                "void main(){ v_uv=a_texCoord0; gl_Position=u_projTrans*vec4(a_position,1.0); }",
            "#ifdef GL_ES\nprecision mediump float;\n#endif\n" +
                "uniform sampler2D u_texture;\n" +
                "uniform vec4 u_tint;\n" +
                "varying vec2 v_uv;\n" +
                "void main(){ vec4 c=texture2D(u_texture,v_uv)*u_tint; if(c.a<0.02) discard; gl_FragColor=c; }"
        );
        if (!shader.isCompiled())
            throw new IllegalStateException("Could not compile alley transition shader: " + shader.getLog());
        heroMesh = new Mesh(false, 4, 6,
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"));
        heroMesh.setIndices(new short[] {0, 1, 2, 2, 3, 0});
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        white = new Texture(pixel);
        pixel.dispose();
        resize(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
    }

    /** Profile for a portal: its own {@code transition}, else the original laundry-alley rule. */
    static TransitionProfile profileFor(String location, InteractionData portal) {
        if (portal == null || !"portal".equals(portal.kind)) return null;
        if (portal.transition != null) return TransitionProfile.get(portal.transition);
        if ("alley_door".equals(portal.visualId) && "laundry_alley".equals(portal.targetLocation))
            return TransitionProfile.get("alley_enter");
        if ("laundry_alley".equals(location) && "street_exit".equals(portal.visualId) && portal.returnToOrigin)
            return TransitionProfile.get("alley_exit");
        return null;
    }

    void toggleDebug() { debug = !debug; }

    void begin(TransitionProfile profile, WorldRenderer renderer) {
        if (active || profile == null) return;
        this.profile = profile;
        this.renderer = renderer;
        stone = renderer.foregroundStone();
        reverse = profile.direction < 0;
        time = 0f;
        switched = false;
        failed = false;
        active = true;
        recreateBuffers();
        heroBefore = capture(source, renderer);
        heroAfter = null;
        if (profile.sound != null) sound(profile.sound).play(profile.soundVolume);
    }

    /** Returns true once the backend should change rooms behind the foreground column. */
    boolean update(float delta) {
        if (!active) return false;
        time = Math.min(profile.totalDuration(), time + Math.max(0f, delta) * (debug ? .25f : 1f));
        return !switched && turn() >= profile.switchAt;
    }

    void switched(boolean success, WorldRenderer renderer) {
        if (!active || switched) return;
        switched = true;
        failed = !success;
        if (success) heroAfter = capture(target, renderer);
    }

    boolean isComplete() { return active && time >= profile.totalDuration(); }
    boolean isActive() { return active; }
    boolean failed() { return failed; }

    void finish() {
        active = false;
        switched = false;
        time = 0f;
        disposeBuffers();
    }

    void resize(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        camera.viewportWidth = this.width;
        camera.viewportHeight = this.height;
        overlayCamera.setToOrtho(false, this.width, this.height);
        rebuildPlanes();
        // Active captures stay valid and are scaled to the new viewport. Recreate
        // them before the next transition, after both old views are no longer needed.
    }

    private float turn() { return Math.min(1f, time / profile.turnDuration); }

    private float settle() {
        return profile.settleDuration <= 0f ? 1f :
            MathUtils.clamp((time - profile.turnDuration) / profile.settleDuration, 0f, 1f);
    }

    void render() {
        if (!active || source == null) return;
        float turn = turn();
        float visualProgress = failed && turn > profile.switchAt ? 1f - turn : turn;
        float eased = Interpolation.smooth.apply(visualProgress);
        float angle = reverse ? 90f * (1f - eased) : 90f * eased;
        float radians = angle * MathUtils.degreesToRadians;
        float planeWidth = planeWidth();
        // The camera orbits the outside corner of a building: the focus slides along the street face to the
        // corner, then along the side face, so the view never leaves the walls.
        float along = reverse ? 1f - eased : eased;
        float hingeX = planeWidth * .5f;
        float targetX = along < .5f ? hingeX * along * 2f : hingeX;
        float targetZ = along > .5f ? -planeWidth * .5f * (along * 2f - 1f) : 0f;
        float distance = PLANE_HEIGHT * .5f /
            (float) Math.tan(camera.fieldOfView * .5f * MathUtils.degreesToRadians);
        camera.position.set(targetX + MathUtils.sin(radians) * distance, 0f,
            targetZ + MathUtils.cos(radians) * distance);
        camera.up.set(0f, 1f, 0f);
        camera.lookAt(targetX, 0f, targetZ);
        // The camera arrives a little to the left of the gameplay view and slides right onto it while settling.
        float shift = profile.cameraShift * planeWidth * offsetCurve(turn, settle());
        camera.position.add(new Vector3(camera.direction).crs(camera.up).nor().scl(-shift));
        camera.near = .1f;
        camera.far = distance + planeWidth * 3f;
        camera.update();

        // Depth must be cleared every frame: the planes are redrawn at the same depths.
        ScreenUtils.clear(.025f, .022f, .035f, 1f, true);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        shader.bind();
        shader.setUniformMatrix("u_projTrans", camera.combined);
        shader.setUniformi("u_texture", 0);

        Texture street = reverse && switched && !failed ? target.getColorBufferTexture() :
            source.getColorBufferTexture();
        Texture alley = !reverse && switched && !failed ? target.getColorBufferTexture() :
            source.getColorBufferTexture();
        draw(streetPlane, street, 1f, 1f, 1f, 1f);
        draw(alleyPlane, alley, .9f, .88f, .92f, 1f);
        draw(cornerPlane, source.getColorBufferTexture(), .34f, .27f, .24f, 1f);

        // Hero layer: on the plane it stands on, turning into the depth before the swap, side view after it.
        boolean after = switched && !failed;
        WorldRenderer.HeroLayer hero = after ? heroAfter : heroBefore;
        boolean onStreet = after == reverse;
        // Before the swap the hero turns into the depth; after it, it comes out of the depth back to side view
        // while the camera finishes the turn (the same 2.5D effect, reversed).
        float heroTurn = after ?
            1f - Interpolation.smooth.apply(MathUtils.clamp((turn - profile.switchAt) / Math.max(.01f, 1f - profile.switchAt), 0f, 1f)) :
            MathUtils.clamp(turn / Math.max(.01f, profile.heroTurnUntil), 0f, 1f);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        drawHero(hero, renderer.heroTurnFrame(heroTurn), onStreet);

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);

        float heroScreenX = projectHero(hero, onStreet);
        renderOverlay(turn, heroScreenX, hero);
        if (debug) renderDebug(turn, hero, onStreet, heroScreenX);
    }

    /** Left offset of the camera: grows after the swap, then returns to zero while settling. */
    private float offsetCurve(float turn, float settle) {
        float grow = MathUtils.clamp((turn - profile.switchAt) / Math.max(.01f, 1f - profile.switchAt), 0f, 1f);
        return Interpolation.smooth.apply(grow) * (1f - Interpolation.smooth.apply(settle));
    }

    private float planeWidth() { return PLANE_HEIGHT * width / (float) height; }

    /** A screen pixel of a captured frame, placed on the plane that frame is shown on. */
    private Vector3 planePoint(float sx, float sy, boolean street, float lift, Vector3 out) {
        float planeWidth = planeWidth();
        float y = (sy / height - .5f) * PLANE_HEIGHT;
        if (street) return out.set((sx / width - .5f) * planeWidth, y, lift);
        return out.set(planeWidth * .5f + lift, y, -sx / width * planeWidth);
    }

    private void drawHero(WorldRenderer.HeroLayer hero, TextureRegion frame, boolean street) {
        if (hero == null || frame == null) return;
        float ppu = hero.pixelsPerUnit;
        float anchor = hero.facingLeft ? hero.width - hero.anchorX : hero.anchorX;
        float left = hero.feetX - anchor * ppu, right = left + hero.width * ppu;
        float bottom = hero.feetY - hero.anchorY * ppu, top = bottom + hero.height * ppu;
        float u = hero.facingLeft ? frame.getU2() : frame.getU(), u2 = hero.facingLeft ? frame.getU() : frame.getU2();
        Vector3 p = new Vector3();
        put(0, planePoint(left, bottom, street, HERO_LIFT, p), u, frame.getV2());
        put(1, planePoint(right, bottom, street, HERO_LIFT, p), u2, frame.getV2());
        put(2, planePoint(right, top, street, HERO_LIFT, p), u2, frame.getV());
        put(3, planePoint(left, top, street, HERO_LIFT, p), u, frame.getV());
        heroMesh.setVertices(heroVertices);
        frame.getTexture().bind(0);
        shader.setUniformf("u_tint", 1f, 1f, 1f, 1f);
        heroMesh.render(shader, GL20.GL_TRIANGLES);
    }

    private void put(int corner, Vector3 p, float u, float v) {
        int i = corner * 5;
        heroVertices[i] = p.x;
        heroVertices[i + 1] = p.y;
        heroVertices[i + 2] = p.z;
        heroVertices[i + 3] = u;
        heroVertices[i + 4] = v;
    }

    private float projectHero(WorldRenderer.HeroLayer hero, boolean street) {
        if (hero == null) return width * .5f;
        Vector3 p = planePoint(hero.feetX, hero.feetY + hero.pixelsPerUnit * .5f, street, HERO_LIFT, new Vector3());
        camera.project(p, 0f, 0f, width, height);
        return p.x;
    }

    /** Fade, dust and the foreground column, in screen pixels over the 3D turn. */
    private void renderOverlay(float turn, float heroX, WorldRenderer.HeroLayer hero) {
        float ppu = hero == null ? height / 14f : hero.pixelsPerUnit;
        float cover = profile.coverTo - profile.coverFrom;
        float bump = Math.max(0f, 1f - Math.abs(turn - profile.switchAt) / cover);
        overlayCamera.update();
        overlay.setProjectionMatrix(overlayCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        overlay.begin();
        overlay.setColor(0f, 0f, 0f, profile.fadePeak * Interpolation.smooth.apply(bump));
        overlay.draw(white, 0f, 0f, width, height);

        // Dust kicked up along the floor, drifting with the column.
        float drift = -profile.direction * turn * width * .5f;
        float mote = Math.max(1f, ppu / 64f * 2f);
        for (int i = 0; i < profile.dustCount; i++) {
            float seed = MathUtils.sin(i * 12.9898f) * 43758.547f;
            float r1 = seed - MathUtils.floor(seed), r2 = (r1 * 7.31f) % 1f;
            float x = ((r1 * width + drift * (.6f + r2)) % width + width) % width;
            float y = height * (.06f + .22f * r2) + turn * ppu * (.2f + r2 * .4f);
            overlay.setColor(.72f, .66f, .6f, .55f * bump);
            overlay.draw(white, Math.round(x), Math.round(y), mote, mote);
        }

        // Foreground column: enters from the side the camera turns towards and covers the hero at the swap.
        float columnWidth = profile.occluderWidth * width;
        float heroWidth = HERO_BODY_WIDTH * ppu;
        float u = (turn - profile.coverFrom) / cover;
        // Across the hero at an even pace inside the cover window; outside it the column hurries in from the
        // screen edge and out past the other edge, so it is gone before the camera settles.
        float sweep = columnWidth + heroWidth;
        float travel = u;
        if (u < 0f) travel = u * (width + sweep) / (sweep * Math.max(.01f, profile.coverFrom / cover));
        else if (u > 1f) travel = 1f + (u - 1f) * (width + sweep) / (sweep * Math.max(.01f, (1f - profile.coverTo) / cover));
        float centre = heroX + profile.direction * (sweep * .5f - travel * sweep);
        float left = centre - columnWidth * .5f;
        if (left < width && left + columnWidth > 0f && stone != null) {
            float tile = ppu;
            overlay.setColor(.26f, .2f, .24f, 1f);
            for (float y = 0f; y < height; y += tile)
                for (float x = left; x < left + columnWidth; x += tile) {
                    float part = Math.min(1f, (left + columnWidth - x) / tile);
                    overlay.draw(stone.getTexture(), x, y, tile * part, tile,
                        stone.getU(), stone.getV2(), stone.getU() + (stone.getU2() - stone.getU()) * part,
                        stone.getV());
                }
            // Warm rim light on the edge facing the street light, dark core shadow on the other.
            float rim = Math.max(2f, ppu / 64f * 3f);
            float lit = profile.direction > 0 ? left : left + columnWidth - rim;
            float shade = profile.direction > 0 ? left + columnWidth - rim * 3f : left;
            overlay.setColor(.48f, .34f, .27f, .9f);
            overlay.draw(white, lit, 0f, rim, height);
            overlay.setColor(.05f, .03f, .06f, .55f);
            overlay.draw(white, shade, 0f, rim * 3f, height);
        }
        overlay.setColor(Color.WHITE);
        overlay.end();
    }

    /** F10: plane outlines and the drawing order, slowed to a quarter of the speed. */
    private void renderDebug(float turn, WorldRenderer.HeroLayer hero, boolean onStreet, float heroX) {
        float planeWidth = planeWidth(), half = PLANE_HEIGHT * .5f;
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.CYAN);
        shapes.rect(-planeWidth * .5f, -half, planeWidth, PLANE_HEIGHT);
        shapes.setColor(Color.MAGENTA);
        shapes.line(planeWidth * .5f, -half, 0f, planeWidth * .5f, -half, -planeWidth);
        shapes.line(planeWidth * .5f, half, 0f, planeWidth * .5f, half, -planeWidth);
        shapes.line(planeWidth * .5f, -half, -planeWidth, planeWidth * .5f, half, -planeWidth);
        shapes.setColor(Color.ORANGE);
        shapes.line(planeWidth * .5f, -half, 0f, planeWidth * .5f, half, 0f);
        shapes.end();
        shapes.setProjectionMatrix(overlayCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.YELLOW);
        if (hero != null) shapes.line(heroX, 0f, heroX, height);
        shapes.end();
        overlay.setProjectionMatrix(overlayCamera.combined);
        overlay.begin();
        float x = 12f, y = height - 12f, line = debugFont.getLineHeight();
        debugFont.setColor(Color.WHITE);
        debugFont.draw(overlay, String.format("%s  turn %.2f  switch %.2f  settle %.2f  %s",
            profile.id, turn, profile.switchAt, settle(),
            switched ? failed ? "SWAP FAILED" : "swapped" : "before swap"), x, y);
        debugFont.setColor(Color.CYAN);
        debugFont.draw(overlay, "1 street plane (z = 0)", x, y - line);
        debugFont.setColor(Color.MAGENTA);
        debugFont.draw(overlay, "2 alley plane (x = hinge)", x, y - line * 2);
        debugFont.setColor(Color.ORANGE);
        debugFont.draw(overlay, "3 corner chamfer", x, y - line * 3);
        debugFont.setColor(Color.YELLOW);
        debugFont.draw(overlay, "4 hero layer on the " + (onStreet ? "street" : "alley") + " plane", x, y - line * 4);
        debugFont.setColor(Color.RED);
        debugFont.draw(overlay, String.format("5 foreground column (screen space), covers %.2f..%.2f",
            profile.coverFrom, profile.coverTo), x, y - line * 5);
        overlay.end();
    }

    private WorldRenderer.HeroLayer capture(FrameBuffer buffer, WorldRenderer renderer) {
        buffer.begin();
        ScreenUtils.clear(.1f, .1f, .1f, 1f);
        renderer.setHeroHidden(true);
        try {
            renderer.renderWorldOnly(0f);
        } finally {
            renderer.setHeroHidden(false);
        }
        buffer.end();
        return renderer.heroLayer(width, height);
    }

    private Sound sound(String path) {
        Sound sound = sounds.get(path);
        if (sound == null) {
            sound = Gdx.audio.newSound(Gdx.files.internal(path));
            sounds.put(path, sound);
        }
        return sound;
    }

    private void recreateBuffers() {
        disposeBuffers();
        source = new FrameBuffer(Pixmap.Format.RGBA8888, Math.max(1, width), Math.max(1, height), true);
        target = new FrameBuffer(Pixmap.Format.RGBA8888, Math.max(1, width), Math.max(1, height), true);
        source.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        target.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    private void rebuildPlanes() {
        disposeMeshes();
        float planeWidth = planeWidth();
        float halfHeight = PLANE_HEIGHT * .5f;
        streetPlane = plane(new float[] {
            -planeWidth * .5f, -halfHeight, 0f, 0f, 0f,
            planeWidth * .5f, -halfHeight, 0f, 1f, 0f,
            planeWidth * .5f, halfHeight, 0f, 1f, 1f,
            -planeWidth * .5f, halfHeight, 0f, 0f, 1f
        });
        // The building's side wall runs back from the corner (-z), so both faces show mid-turn and,
        // seen from +x, the alley's left edge meets the street's right edge at the corner.
        alleyPlane = plane(new float[] {
            planeWidth * .5f, -halfHeight, 0f, 0f, 0f,
            planeWidth * .5f, -halfHeight, -planeWidth, 1f, 0f,
            planeWidth * .5f, halfHeight, -planeWidth, 1f, 1f,
            planeWidth * .5f, halfHeight, 0f, 0f, 1f
        });
        // A narrow chamfer makes the shared edge read as the building corner and
        // hides the exact frame at which the backend swaps the active location.
        cornerPlane = plane(new float[] {
            planeWidth * .5f - CORNER_WIDTH, -halfHeight, -CORNER_WIDTH, 0f, 0f,
            planeWidth * .5f + CORNER_WIDTH, -halfHeight, CORNER_WIDTH, 1f, 0f,
            planeWidth * .5f + CORNER_WIDTH, halfHeight, CORNER_WIDTH, 1f, 1f,
            planeWidth * .5f - CORNER_WIDTH, halfHeight, -CORNER_WIDTH, 0f, 1f
        });
    }

    private static Mesh plane(float[] vertices) {
        Mesh mesh = new Mesh(true, 4, 6,
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"));
        mesh.setVertices(vertices);
        mesh.setIndices(new short[] {0, 1, 2, 2, 3, 0});
        return mesh;
    }

    private void draw(Mesh mesh, Texture texture, float red, float green, float blue, float alpha) {
        texture.bind(0);
        shader.setUniformf("u_tint", red, green, blue, alpha);
        mesh.render(shader, GL20.GL_TRIANGLES);
    }

    private void disposeMeshes() {
        if (streetPlane != null) streetPlane.dispose();
        if (alleyPlane != null) alleyPlane.dispose();
        if (cornerPlane != null) cornerPlane.dispose();
        streetPlane = alleyPlane = cornerPlane = null;
    }

    private void disposeBuffers() {
        // Do not delete a managed FBO texture while it is still the active sampler.
        // Some desktop drivers keep that binding alive until another texture is bound.
        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0);
        if (source != null) source.dispose();
        if (target != null) target.dispose();
        source = target = null;
    }

    @Override public void dispose() {
        disposeBuffers();
        disposeMeshes();
        heroMesh.dispose();
        shader.dispose();
        overlay.dispose();
        shapes.dispose();
        debugFont.dispose();
        white.dispose();
        for (Sound sound : sounds.values()) sound.dispose();
    }
}
