package com.cursedarchie.platformer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.levels.TransitionProfile;
import com.cursedarchie.platformer.world.WorldRenderer;

/**
 * Stairs, breach, drop or hatch between rooms of one location ({@link TransitionProfile#isDescent()}). The backend
 * has already moved the hero to the landing spawn; while the world is paused (control locked) the hero's body is
 * carried along the profile's path from the doorway to the landing, so the ordinary camera follows it vertically.
 * The hero is hidden while it passes through the floor between the rooms. Control returns once the camera has
 * settled on the landing (or after {@code settleMax}). Unlike the alley turn, nothing is captured or rotated.
 */
final class DescentTransition implements Disposable {
    private final ObjectMap<String, Sound> sounds = new ObjectMap<>();
    private final Vector2 from = new Vector2(), to = new Vector2(), at = new Vector2();
    private TransitionProfile profile;
    private float time, settled;
    private boolean active;

    void begin(TransitionProfile profile, Vector2 doorway, Vector2 landing) {
        this.profile = profile;
        from.set(doorway);
        to.set(landing);
        time = settled = 0f;
        active = true;
        if (profile.sound != null) sound(profile.sound).play(profile.soundVolume);
    }

    boolean isActive() { return active; }

    /** Advances the move; returns true on the frame control is handed back. */
    boolean update(float delta, Hero hero, WorldRenderer renderer) {
        if (!active) return false;
        time += Math.max(0f, delta);
        float p = Math.min(1f, time / profile.duration);
        point(p, at);
        hero.getBody().setTransform(at, 0f);
        hero.getBody().setLinearVelocity(0f, 0f);
        boolean down = to.y < from.y;
        String anim = "drop".equals(profile.path) && down && p > .3f ? "fall" : "walk";
        // Fully hidden inside the floor, with a short fade at both edges of that window.
        float inside = Math.min(p - profile.fadeFrom, profile.fadeTo - p);
        float alpha = inside < 0f ? 1f : Math.max(0f, 1f - inside / .05f);
        boolean left = Math.abs(to.x - from.x) > .2f ? to.x < from.x : hero.isFacingLeft();
        if (p < 1f) {
            renderer.setHeroOverride(anim, time, alpha, left);
            return false;
        }
        hero.getBody().setTransform(to, 0f);
        renderer.setHeroOverride(null, 0f, 1f, left);
        hero.setFacingLeft(left);
        if (settled == 0f && profile.dustCount > 0) renderer.addLandingDust(to.x, to.y - Hero.BODY_SIZE / 2f, profile.dustCount);
        settled += Math.max(0f, delta);
        if (renderer.isCameraSettled() || settled >= profile.settleMax) {
            active = false;
            return true;
        }
        return false;
    }

    /** Stairs: a steady diagonal. Drop: a step into the opening, then an accelerating fall. */
    private void point(float p, Vector2 out) {
        if ("drop".equals(profile.path)) {
            float step = MathUtils.clamp(p / .3f, 0f, 1f), fall = MathUtils.clamp((p - .3f) / .7f, 0f, 1f);
            boolean down = to.y < from.y;
            out.set(MathUtils.lerp(from.x, to.x, Interpolation.smooth.apply(down ? step : fall)),
                MathUtils.lerp(from.y, to.y, down ? fall * fall : Interpolation.smooth.apply(step)));
            return;
        }
        out.set(MathUtils.lerp(from.x, to.x, Interpolation.smooth.apply(p)), MathUtils.lerp(from.y, to.y, p));
    }

    private Sound sound(String path) {
        Sound sound = sounds.get(path);
        if (sound == null) {
            sound = Gdx.audio.newSound(Gdx.files.internal(path));
            sounds.put(path, sound);
        }
        return sound;
    }

    @Override public void dispose() {
        for (Sound sound : sounds.values()) sound.dispose();
    }
}
