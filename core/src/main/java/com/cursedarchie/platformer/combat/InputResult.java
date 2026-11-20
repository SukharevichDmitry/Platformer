package com.cursedarchie.platformer.combat;

/** What the attack button did while a strike was already in progress. */
public enum InputResult {
    /** Nothing was pressed (or the press was ignored: only the first press per strike counts). */
    NO_INPUT,
    /** Pressed inside the combo window: the follow-up strike is buffered. */
    COMBO_BUFFERED,
    /** Pressed before the window opened: the press is discarded and the combo is lost. */
    EARLY_INPUT,
    /** Pressed after the window: the press is discarded; a later fresh press may start a new combo. */
    LATE_INPUT
}
