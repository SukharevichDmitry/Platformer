package com.cursedarchie.platformer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.cursedarchie.platformer.levels.wrappers.InteractionData;
import com.cursedarchie.platformer.world.WorldRenderer;

/** Short 2.5D camera turn used only between a ruins street and the laundry alley. */
final class AlleyTurnTransition implements Disposable {
    static final float DURATION = .72f;
    private static final float SWITCH_PROGRESS = .5f;
    private static final float PLANE_HEIGHT = 9f;
    private static final float CORNER_WIDTH = .34f;

    private final PerspectiveCamera camera = new PerspectiveCamera(52f, 1f, 1f);
    private final ShaderProgram shader;
    private Mesh streetPlane;
    private Mesh alleyPlane;
    private Mesh cornerPlane;
    private FrameBuffer source;
    private FrameBuffer target;
    private int width = 1;
    private int height = 1;
    private float progress;
    private boolean reverse;
    private boolean active;
    private boolean switched;
    private boolean failed;

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
                "void main(){ gl_FragColor=texture2D(u_texture,v_uv)*u_tint; }"
        );
        if (!shader.isCompiled())
            throw new IllegalStateException("Could not compile alley transition shader: " + shader.getLog());
        resize(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
    }

    static boolean supports(String location, InteractionData portal) {
        if (portal == null || !"portal".equals(portal.kind)) return false;
        return "alley_door".equals(portal.visualId) && "laundry_alley".equals(portal.targetLocation) ||
            "laundry_alley".equals(location) && "street_exit".equals(portal.visualId) && portal.returnToOrigin;
    }

    void begin(boolean reverse, WorldRenderer renderer) {
        if (active) return;
        this.reverse = reverse;
        progress = 0f;
        switched = false;
        failed = false;
        active = true;
        recreateBuffers();
        capture(source, renderer);
    }

    /** Returns true once the backend should change rooms behind the building corner. */
    boolean update(float delta) {
        if (!active) return false;
        progress = Math.min(1f, progress + Math.max(0f, delta) / DURATION);
        return !switched && progress >= SWITCH_PROGRESS;
    }

    void switched(boolean success, WorldRenderer renderer) {
        if (!active || switched) return;
        switched = true;
        failed = !success;
        if (success) capture(target, renderer);
    }

    boolean isComplete() { return active && progress >= 1f; }
    boolean isActive() { return active; }
    boolean failed() { return failed; }

    void finish() {
        active = false;
        switched = false;
        progress = 0f;
        disposeBuffers();
    }

    void resize(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        camera.viewportWidth = this.width;
        camera.viewportHeight = this.height;
        rebuildPlanes();
        // Active captures stay valid and are scaled to the new viewport. Recreate
        // them before the next transition, after both old views are no longer needed.
    }

    void render() {
        if (!active || source == null) return;
        float visualProgress = failed && progress > SWITCH_PROGRESS ? 1f - progress : progress;
        float eased = Interpolation.smooth.apply(visualProgress);
        float angle = reverse ? 90f * (1f - eased) : 90f * eased;
        float radians = angle * MathUtils.degreesToRadians;
        float aspect = width / (float) height;
        float planeWidth = PLANE_HEIGHT * aspect;
        // Move the focus along the visible walls, through their shared edge.
        // A direct diagonal interpolation cuts through the empty quadrant between
        // the planes and exposes the clear colour in the middle of the turn.
        float hingeX = planeWidth * .5f;
        float targetX = eased < .5f ? hingeX * eased * 2f : hingeX;
        float targetZ = eased > .5f ? planeWidth * .5f * (eased * 2f - 1f) : 0f;
        float distance = PLANE_HEIGHT * .5f /
            (float) Math.tan(camera.fieldOfView * .5f * MathUtils.degreesToRadians);
        camera.position.set(targetX + MathUtils.sin(radians) * distance, 0f,
            targetZ + MathUtils.cos(radians) * distance);
        camera.up.set(0f, 1f, 0f);
        camera.lookAt(targetX, 0f, targetZ);
        camera.near = .1f;
        camera.far = distance + planeWidth * 3f;
        camera.update();

        ScreenUtils.clear(.025f, .022f, .035f, 1f);
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

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
    }

    private void capture(FrameBuffer buffer, WorldRenderer renderer) {
        buffer.begin();
        ScreenUtils.clear(.1f, .1f, .1f, 1f);
        renderer.renderWorldOnly(0f);
        buffer.end();
    }

    private void recreateBuffers() {
        disposeBuffers();
        source = new FrameBuffer(com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888,
            Math.max(1, width), Math.max(1, height), true);
        target = new FrameBuffer(com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888,
            Math.max(1, width), Math.max(1, height), true);
        source.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        target.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    private void rebuildPlanes() {
        disposeMeshes();
        float planeWidth = PLANE_HEIGHT * width / (float) height;
        float halfHeight = PLANE_HEIGHT * .5f;
        streetPlane = plane(new float[] {
            -planeWidth * .5f, -halfHeight, 0f, 0f, 0f,
            planeWidth * .5f, -halfHeight, 0f, 1f, 0f,
            planeWidth * .5f, halfHeight, 0f, 1f, 1f,
            -planeWidth * .5f, halfHeight, 0f, 0f, 1f
        });
        alleyPlane = plane(new float[] {
            planeWidth * .5f, -halfHeight, 0f, 0f, 0f,
            planeWidth * .5f, -halfHeight, planeWidth, 1f, 0f,
            planeWidth * .5f, halfHeight, planeWidth, 1f, 1f,
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
        shader.dispose();
    }
}
