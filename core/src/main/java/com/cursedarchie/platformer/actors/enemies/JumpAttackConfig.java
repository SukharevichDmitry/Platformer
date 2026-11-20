package com.cursedarchie.platformer.actors.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Tunable numbers of the regular melee enemy's jump attack, loaded from {@code config/enemy_jump_attack.json};
 * missing keys keep the defaults below. See the file's note for the phase order.
 */
public final class JumpAttackConfig {
    public final Array<String> archetypes = new Array<>(new String[] {"default"});
    public float jumpStartup = .45f, jumpDistance = 3.8f, minDistance = 2.3f, jumpHeight = 1.7f;
    public float jumpActiveTime = .2f, jumpRecovery = .45f, missRecoveryExtra = .4f, jumpCooldown = 3.2f;
    /** The arc lands this far short of the hero's centre, so the enemy never comes down on top of the hero. */
    public float landingGap = 1f;
    /** The landing floor must lie within this height of the take-off floor: no jumping into pits or onto ledges. */
    public float maxLevelDifference = .6f;
    public float hitboxWidth = 1.1f, hitboxHeight = 1.3f, hitboxReach = .8f, hitboxOffsetY = -.25f;
    public float damageMultiplier = 1f, knockback = 4f, staggerDuration = .25f, stabilityDamage = 1.5f;

    private static JumpAttackConfig current;

    public static synchronized JumpAttackConfig get() {
        if (current == null) current = load();
        return current;
    }

    public static synchronized void set(JumpAttackConfig config) { current = config; }

    public static JumpAttackConfig load() {
        try {
            if (Gdx.files != null && Gdx.files.internal("config/enemy_jump_attack.json").exists())
                return parse(Gdx.files.internal("config/enemy_jump_attack.json").readString("UTF-8"));
        } catch (RuntimeException ignored) {
            // A broken file must never break the game: fall through to the defaults.
        }
        return new JumpAttackConfig();
    }

    public static JumpAttackConfig parse(String json) {
        JsonValue r = new JsonReader().parse(json);
        JumpAttackConfig d = new JumpAttackConfig();
        if (r.has("archetypes")) {
            d.archetypes.clear();
            for (JsonValue id : r.get("archetypes")) d.archetypes.add(id.asString());
        }
        d.jumpStartup = Math.max(.15f, r.getFloat("jumpStartup", d.jumpStartup));
        d.jumpDistance = Math.max(1f, r.getFloat("jumpDistance", d.jumpDistance));
        d.minDistance = Math.max(.5f, Math.min(d.jumpDistance, r.getFloat("minDistance", d.minDistance)));
        d.jumpHeight = Math.max(.4f, r.getFloat("jumpHeight", d.jumpHeight));
        d.jumpActiveTime = Math.max(.05f, r.getFloat("jumpActiveTime", d.jumpActiveTime));
        d.jumpRecovery = Math.max(0f, r.getFloat("jumpRecovery", d.jumpRecovery));
        d.missRecoveryExtra = Math.max(0f, r.getFloat("missRecoveryExtra", d.missRecoveryExtra));
        d.jumpCooldown = Math.max(0f, r.getFloat("jumpCooldown", d.jumpCooldown));
        d.landingGap = Math.max(.5f, r.getFloat("landingGap", d.landingGap));
        d.maxLevelDifference = Math.max(.1f, r.getFloat("maxLevelDifference", d.maxLevelDifference));
        d.hitboxWidth = Math.max(.2f, r.getFloat("hitboxWidth", d.hitboxWidth));
        d.hitboxHeight = Math.max(.2f, r.getFloat("hitboxHeight", d.hitboxHeight));
        d.hitboxReach = Math.max(d.hitboxWidth / 2f, r.getFloat("hitboxReach", d.hitboxReach));
        d.hitboxOffsetY = r.getFloat("hitboxOffsetY", d.hitboxOffsetY);
        d.damageMultiplier = Math.max(0f, r.getFloat("damageMultiplier", d.damageMultiplier));
        d.knockback = Math.max(0f, r.getFloat("knockback", d.knockback));
        d.staggerDuration = Math.max(0f, r.getFloat("staggerDuration", d.staggerDuration));
        d.stabilityDamage = Math.max(0f, r.getFloat("stabilityDamage", d.stabilityDamage));
        return d;
    }
}
