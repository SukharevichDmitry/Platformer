package com.cursedarchie.platformer.run;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/** Small, validated gameplay catalog. Item IDs do not depend on texture names. */
public final class ItemCatalog {
    public static final String FIELD_BANDAGE = "field_bandage";
    public static final String SEALED_NOTE = "sealed_note";
    private static final String PATH = "items/field_bandage.json";
    private static ItemCatalog instance;

    public final String id;
    public final int maxStack;
    public final float heal;

    private ItemCatalog(String id, int maxStack, float heal) {
        this.id = id;
        this.maxStack = maxStack;
        this.heal = heal;
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
            instance = new ItemCatalog(id, maxStack, heal);
        }
        return instance;
    }

    public static int maxStack(String id) {
        if (FIELD_BANDAGE.equals(id)) return bandage().maxStack;
        if (SEALED_NOTE.equals(id)) return 1;
        throw new IllegalArgumentException("Unknown item: " + id);
    }

    public static boolean supports(String id) {
        return FIELD_BANDAGE.equals(id) || SEALED_NOTE.equals(id);
    }
}
