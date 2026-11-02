package com.cursedarchie.platformer.run;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/** Small, validated gameplay catalog. Item IDs do not depend on texture names. */
public final class ItemCatalog {
    public static final String FIELD_BANDAGE = "field_bandage";
    public static final String SEALED_NOTE = "sealed_note";
    public static final String BLADE_SHARD = "blade_shard";
    public static final String IMPACT_CORE = "impact_core";
    public static final String FUSED_RELIC = "fused_relic";
    private static final String PATH = "items/field_bandage.json";
    private static ItemCatalog instance;
    private static ItemCatalog bladeShard;
    private static ItemCatalog impactCore;
    private static ItemCatalog fusedRelic;

    public final String id;
    public final int maxStack;
    public final float heal;
    public final float bonus;

    private ItemCatalog(String id, int maxStack, float heal, float bonus) {
        this.id = id;
        this.maxStack = maxStack;
        this.heal = heal;
        this.bonus = bonus;
    }

    public static ItemCatalog bandage() {
        if (instance == null) {
            JsonValue data = new JsonReader().parse(Gdx.files.internal(PATH));
            String id = data.getString("id");
            String effect = data.getString("effect");
            int maxStack = data.getInt("maxStack");
            float heal = data.getFloat("heal");
            if (!FIELD_BANDAGE.equals(id) || !"heal".equals(effect) ||
                maxStack < 1 || maxStack > 99 || !Float.isFinite(heal) || heal <= 0f)
                throw new IllegalArgumentException("Invalid item definition: " + PATH);
            instance = new ItemCatalog(id, maxStack, heal, 0f);
        }
        return instance;
    }

    public static ItemCatalog bladeShard() {
        if (bladeShard == null)
            bladeShard = relic(BLADE_SHARD, "melee_damage");
        return bladeShard;
    }

    public static ItemCatalog impactCore() {
        if (impactCore == null)
            impactCore = relic(IMPACT_CORE, "melee_recoil");
        return impactCore;
    }
    public static ItemCatalog fusedRelic() {
        if (fusedRelic == null) fusedRelic = relic(FUSED_RELIC, "melee_combo");
        return fusedRelic;
    }

    private static ItemCatalog relic(String id, String expectedEffect) {
        String path = "items/" + id + ".json";
        JsonValue data = new JsonReader().parse(Gdx.files.internal(path));
        float bonus = data.getFloat("bonus");
        if (!id.equals(data.getString("id")) ||
            !expectedEffect.equals(data.getString("effect")) ||
            data.getInt("maxStack") != 1 || !Float.isFinite(bonus) || bonus <= 0f)
            throw new IllegalArgumentException("Invalid item definition: " + path);
        return new ItemCatalog(id, 1, 0f, bonus);
    }

    public static int maxStack(String id) {
        if (FIELD_BANDAGE.equals(id)) return bandage().maxStack;
        if (SEALED_NOTE.equals(id)) return 1;
        if (BLADE_SHARD.equals(id)) return bladeShard().maxStack;
        if (IMPACT_CORE.equals(id)) return impactCore().maxStack;
        if (FUSED_RELIC.equals(id)) return fusedRelic().maxStack;
        throw new IllegalArgumentException("Unknown item: " + id);
    }

    public static boolean supports(String id) {
        return FIELD_BANDAGE.equals(id) || SEALED_NOTE.equals(id) ||
            BLADE_SHARD.equals(id) || IMPACT_CORE.equals(id) || FUSED_RELIC.equals(id);
    }
}
