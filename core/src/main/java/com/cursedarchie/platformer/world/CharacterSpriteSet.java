package com.cursedarchie.platformer.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Generated character frames (rigs in {@code tools/sprites}) and their metadata, e.g.
 * {@code config/hero_animations.json}. Frames face right; the anchor is the middle of the feet, which sits on the
 * bottom of the collision box. Attack frames are chosen per phase so the drawing always follows the
 * startup / active / recovery timing of the running attack. See documents/ART_ASSET_GUIDE.md.
 */
final class CharacterSpriteSet implements Disposable {
    static final String CONFIG = "config/art.json";

    static final class Anim {
        final Array<TextureRegion> frames;
        final float frameDuration;
        final boolean loop;
        final int[][] tips;
        final ObjectMap<String, int[]> phases = new ObjectMap<>();

        Anim(Array<TextureRegion> frames, JsonValue meta) {
            this.frames = frames;
            frameDuration = meta.getFloat("frameDuration", .083f);
            loop = meta.getBoolean("loop", false);
            JsonValue tipList = meta.get("weaponTip");
            tips = new int[frames.size][];
            for (int i = 0; i < frames.size; i++) {
                JsonValue tip = tipList == null ? null : tipList.get(i);
                tips[i] = tip == null ? new int[] {0, 0} : tip.asIntArray();
            }
            JsonValue phaseList = meta.get("phases");
            if (phaseList != null)
                for (JsonValue phase = phaseList.child; phase != null; phase = phase.next)
                    phases.put(phase.name, phase.asIntArray());
        }

        int frameAt(float time) {
            int index = (int) (time / frameDuration);
            return loop ? index % frames.size : Math.min(index, frames.size - 1);
        }

        /** Frame inside one phase ("startup", "active", "recovery") at progress 0..1 through that phase. */
        int phaseFrame(String phase, float progress) {
            int[] range = phases.get(phase);
            if (range == null) return frameAt(progress);
            int span = range[1] - range[0] + 1;
            return range[0] + Math.min(span - 1, (int) (Math.max(0f, progress) * span));
        }
    }

    private final TextureAtlas atlas;
    private final ObjectMap<String, Anim> animations = new ObjectMap<>();
    /** World size of a frame and the anchor's offset from the frame's bottom-left corner, in world units. */
    final float width, height, anchorX, anchorY, pixelsPerUnit;
    private final int frameHeightPx;

    private CharacterSpriteSet(String atlasPath, JsonValue meta) {
        atlas = new TextureAtlas(Gdx.files.internal(atlasPath));
        pixelsPerUnit = meta.getFloat("pixelsPerUnit", 64f);
        int[] frame = meta.get("frame").asIntArray();
        int[] anchor = meta.get("anchor").asIntArray();
        frameHeightPx = frame[1];
        width = frame[0] / pixelsPerUnit;
        height = frame[1] / pixelsPerUnit;
        anchorX = anchor[0] / pixelsPerUnit;
        // Pixel rows count down from the top; the anchor row is the lowest row of the feet.
        anchorY = (frame[1] - 1 - anchor[1]) / pixelsPerUnit;
        for (JsonValue anim = meta.get("animations").child; anim != null; anim = anim.next) {
            Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions(anim.name);
            if (regions.size != anim.getInt("frames"))
                throw new IllegalArgumentException(atlasPath + " is missing frames of " + anim.name);
            animations.put(anim.name, new Anim(new Array<TextureRegion>(regions), anim));
        }
    }

    /** True when {@code config/art.json} selects the generated set under {@code key} (default "v2"). */
    static boolean selected(String key) {
        try {
            if (!Gdx.files.internal(CONFIG).exists()) return true;
            return "v2".equals(new JsonReader().parse(Gdx.files.internal(CONFIG)).getString(key, "v2"));
        } catch (RuntimeException e) {
            return true;
        }
    }

    /** The set, or null when it is not selected in config/art.json or cannot be read (legacy art is used then). */
    static CharacterSpriteSet load(String configKey, String atlasPath, String metadataPath) {
        if (!selected(configKey)) return null;
        try {
            return new CharacterSpriteSet(atlasPath, new JsonReader().parse(Gdx.files.internal(metadataPath)));
        } catch (RuntimeException e) {
            Gdx.app.error("CharacterSpriteSet", "Falling back to legacy frames for " + atlasPath, e);
            return null;
        }
    }

    Anim get(String name) {
        Anim anim = animations.get(name);
        if (anim == null) throw new IllegalArgumentException("Unknown animation: " + name);
        return anim;
    }

    /** Weapon tip of a frame relative to the anchor, in world units, for a right-facing frame. */
    float tipX(Anim anim, int frame) { return anim.tips[frame][0] / pixelsPerUnit - anchorX; }
    float tipY(Anim anim, int frame) {
        return (frameHeightPx - 1 - anim.tips[frame][1]) / pixelsPerUnit - anchorY;
    }

    @Override public void dispose() { atlas.dispose(); }
}
