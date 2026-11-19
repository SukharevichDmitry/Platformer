package com.cursedarchie.platformer.world;

import com.badlogic.gdx.Input;

/** Every world label and every bindable key name must be drawn with real glyphs, never gaps or placeholder boxes. */
public final class ItemLabelFontCheck {
    private static final String[] LABELS = {
        "ENTER ALLEY", "RETURN", "ENTER", "BANDAGE", "BLADE SHARD", "RED BRACER", "SMOKE", "SEWING KIT",
        "IMPACT CORE", "E: ENTER ALLEY", "Space: Enter", "L-Shift: Return"
    };

    public static void main(String[] args) {
        for (String label : LABELS) check(label, "label");
        // Prompts start with the interact key's name, which the player can rebind to any key.
        for (int key = 0; key <= Input.Keys.MAX_KEYCODE; key++) {
            String name = Input.Keys.toString(key);
            if (name != null && name.chars().allMatch(c -> c < 128)) check(name + ": ENTER", "key name");
        }
        if (ItemLabelFont.glyphIndex(' ') != -1) throw new AssertionError("A space must leave a gap");
        if (ItemLabelFont.glyphIndex('e') != ItemLabelFont.glyphIndex('E'))
            throw new AssertionError("Lower case must draw as capitals");
        System.out.println("ITEM LABEL FONT PASSED: item, doorway and key-name labels use real glyphs");
    }

    private static void check(String text, String what) {
        for (char c : text.toCharArray())
            if (c != ' ' && ItemLabelFont.glyphIndex(c) == ItemLabelFont.CHARACTERS.length())
                throw new AssertionError("The label font has no glyph for '" + c + "' in " + what + " \"" + text + "\"");
    }
}
