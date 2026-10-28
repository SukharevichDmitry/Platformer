package com.cursedarchie.platformer.controllers;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Preferences;

/** Game-wide keyboard settings, independent of the current run. */
public final class ControlBindings {
    public enum Action {
        LEFT("Move left", Keys.LEFT), RIGHT("Move right", Keys.RIGHT),
        UP("Move up", Keys.UP), DOWN("Move down", Keys.DOWN),
        JUMP("Jump", Keys.SPACE), ATTACK("Attack", Keys.X), DASH("Echo dash", Keys.SHIFT_LEFT),
        INTERACT("Interact", Keys.E), BAG("Inventory", Keys.I),
        POCKET_1("Left pocket", Keys.NUM_1), POCKET_2("Right pocket", Keys.NUM_2),
        PAUSE("Pause", Keys.ESCAPE);

        public final String label;
        public final int defaultKey;
        Action(String label, int defaultKey) { this.label = label; this.defaultKey = defaultKey; }
    }

    private final Preferences preferences;
    private final int[] keys = new int[Action.values().length];

    public ControlBindings(Preferences preferences) {
        this.preferences = preferences;
        resetToDefaults(false);
        if (preferences == null) return;
        int[] loaded = new int[keys.length];
        for (Action action : Action.values()) {
            int key = preferences.getInteger("control." + action.name(), action.defaultKey);
            if (key <= 0 || Keys.toString(key) == null) return;
            loaded[action.ordinal()] = key;
        }
        for (int i = 0; i < loaded.length; i++)
            for (int j = i + 1; j < loaded.length; j++)
                if (loaded[i] == loaded[j]) return;
        System.arraycopy(loaded, 0, keys, 0, keys.length);
    }

    public int key(Action action) { return keys[action.ordinal()]; }
    public boolean matches(Action action, int keycode) { return key(action) == keycode; }
    public String keyName(Action action) { return Keys.toString(key(action)); }

    /** Editable copy: changes are applied to the live bindings only on Save. */
    public ControlBindings draft() {
        ControlBindings copy = new ControlBindings(null);
        System.arraycopy(keys, 0, copy.keys, 0, keys.length);
        return copy;
    }

    public void apply(ControlBindings draft) {
        System.arraycopy(draft.keys, 0, keys, 0, keys.length);
        save();
    }

    /** Duplicate bindings are rejected so every action remains reachable. */
    public boolean rebind(Action action, int keycode) {
        if (keycode <= 0 || Keys.toString(keycode) == null) return false;
        for (Action other : Action.values())
            if (other != action && key(other) == keycode) return false;
        keys[action.ordinal()] = keycode;
        save();
        return true;
    }

    public void resetToDefaults() { resetToDefaults(true); }
    private void resetToDefaults(boolean persist) {
        for (Action action : Action.values()) keys[action.ordinal()] = action.defaultKey;
        if (persist) save();
    }

    private void save() {
        if (preferences == null) return;
        for (Action action : Action.values())
            preferences.putInteger("control." + action.name(), key(action));
        preferences.flush();
    }
}
