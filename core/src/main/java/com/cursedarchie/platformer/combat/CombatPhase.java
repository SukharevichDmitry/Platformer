package com.cursedarchie.platformer.combat;

/** Unified combat state vocabulary shared by hero and enemies (derived from each actor's own timers). */
public enum CombatPhase { IDLE, WINDUP, ACTIVE, RECOVERY, BLOCK, STAGGER, DODGE, DEAD }
