# Platformer: discovery and route to a Steam release

Assessment date: 2026-10-14. This document reflects completion of the first constrained procedural-generation profile and the subsequent gameplay-foundation audit in [FOUNDATION_POLISH_PLAN.md](FOUNDATION_POLISH_PLAN.md). It is a planning document, not a claim that the game has received sufficient human playtesting. Earlier hour totals were broad scenario guesses; they should be re-estimated after a mechanics proof and one representative finished room. The project is a first attempt at game development, so keep the first release small.

## Product direction and scope decision

The strongest direction is a compact, atmospheric action platformer built around responsive movement, clear melee combat, traversal, exploration, and memorable places. Keep the inspiration in mood and quality bar; establish original characters, iconography, lore, art, music, and combat identity. The creative brief can become specific after the controls, basic attack, and a representative room are satisfying.

**First-release target:** a complete **3–6 hour game** with one coherent traversal/combat loop, a small number of distinct enemy behaviors, a compact connected world, and a clear ending. Counts of biomes, enemies, bosses, upgrades, and NPCs are **not committed**. Choose them after measuring the cost of one finished room and the value of each mechanic. A first release can omit NPCs if the story is communicated better through places and events. Defer online services, multiplayer, and sprawling branching dialogue.

The project has a **hand-authored rooms plus controlled variation** architecture: authored anchors and templates form a deterministic, validated room graph with bounded tile and encounter variation. Keep this small. The next development gate is foundational: movement, body contacts, tile shapes, attack timing, and a repeatable visual pipeline. More generated biomes or story systems should wait until those basics are reliable and understandable.

## What exists today

- Java/libGDX 1.12.1 desktop project with LWJGL3 and Box2D; the Gradle wrapper, runnable-jar packaging, and extracted-distribution smoke testing are configured.
- A hero with movement, jump, melee attack, health and motion/combat state machines; `default` and `boss` enemy types using shared AI; collision sensors and merged solid-tile fixtures.
- Authored opening `start` location with a direct reversible border transition to generated `ruins`, validated room JSON, stable fixed-step simulation, bounded camera movement, and explicit world/resource disposal. Legacy authored `forest` files remain in assets but are not on the normal route.
- A generated `ruins` location using a versioned production profile, reusable templates, four ordered story anchors, optional branches, seeded graph/layout/tile/encounter generation, and a visible reproduction seed.
- Generated-run persistence through libGDX Preferences, including profile version, seed, topology/layout signatures, narrative choices, and defeated generated-enemy IDs.
- Automated profile, template, graph, layout, reachability, encounter, room, transition, persistence, simulation, camera, and packaged-build checks. The production quality gate currently passes 5,000 seeds.
- A functional generated UI skin, menu, pause flow, and debug overlays. The atlas has prototype hero, enemy, boss, and block art, but no coherent environment kit or audio; there is no NPC, dialogue, checkpoint, inventory, map, or progression system.

These observations describe the **current working tree**, which still contains uncommitted room/world/actor and regression-test changes. On 2026-10-14, `core:check`, the 5,000-seed quality gate, simulation regression, packaged smoke, generated-location, and generated-run persistence checks all passed with JDK 17.

Player feedback on 2026-10-15 changed the movement target: enemies should block without being pushed, and visible platform ledges should be jump-reachable. The hero/enemy body rule and jump/ruins ledge heights were retuned accordingly; ruins generation is now profile version 3. Normal-play debug geometry is disabled while first-10-second startup stutter awaits a repeat manual check. Details and remaining gates are in [FOUNDATION_POLISH_PLAN.md](FOUNDATION_POLISH_PLAN.md).

Follow-up feedback led to faster horizontal movement for hero and enemies, explicit idle stopping/damping for pushed enemies, and jumping from enemy heads via contact-normal support. Automated regressions pass, but the player still needs to judge the new relative speeds and contact feel.

## Highest-priority findings

This table records the original foundation audit. Movement, half-tile, and basic combat timing have since received an automated-check pass; the remaining hands-on and presentation work is tracked in [FOUNDATION_POLISH_PLAN.md](FOUNDATION_POLISH_PLAN.md).

| Priority | Evidence in current tree | Consequence and recommended fix |
|---|---|---|
| Immediate | Hero motion uses a fixed 5-unit/s horizontal velocity, gravity 10, and one jump impulse; the hero can shove dynamic enemies. | Measure and tune movement as a system, define actor collision rules, and test grounded state and jump behavior before adding traversal content. |
| Immediate | `_` is drawn as a 0.5 × 0.5 square while location-wide terrain merging creates a full 1 × 1 fixture for it. | Define the tile's intended shape and rebuild rendering, bounds, collision, and generator validation around the same geometry. |
| Immediate | Hero damage occurs halfway through a 0.6-second attack; both directional sensors exist continuously and attack animation loses priority to movement/jump. | Define an explicit strike timeline, directional active hitbox, buffered input, and render priority; verify the shown strike and actual hit coincide. |
| High | The atlas mixes prototype assets, and audio is absent. Sprite and collision sizes are tightly coupled. | Choose art scale and source/export conventions; complete one coherent character, enemy, and tile kit with basic event sounds before producing many rooms. |
| High | Current tests cover simulation steps, transitions, generation constraints, and cleanup, but not jump feel, partial-tile geometry, pushing, or strike timing. | Add focused regression checks as each rule is fixed, plus a repeatable hands-on mechanics checklist. |
| Later | Generated-run persistence covers ruins and defeated enemies; NPC/dialogue, progression, and full checkpoint saves are absent. | Design these after the foundation and a short mechanics proof establish what the 3–6 hour game actually needs. |

The earlier menu, unload/disposal, fixed-step, room-validation, camera-boundary, dependency-version, and first-profile procedural-generation risks are closed by implementation and automated checks. Their tests remain required when physics and tiles change. The detailed evidence and exit gates for the new priorities are in [FOUNDATION_POLISH_PLAN.md](FOUNDATION_POLISH_PLAN.md).

## Development gates for a 3–6 hour game

| Gate | Status | Deliverable | Exit condition |
|---|---|---|---|
| **0. Technical baseline** | Complete, with active room-runtime changes to settle | Fixed-step simulation, validated authored/generated rooms, packaged checks, reversible transitions, saved ruins run | Preserve the existing green checks and isolate the current terrain-fixture refactor |
| **1. Movement and collision** | Next | Responsive walking/jumping, stable landings, intentional hero/enemy contact rule | Measured jump and stopping behavior matches chosen targets; no enemy shoving or false grounding |
| **2. Tile geometry** | Next | Full and partial tile definitions shared by renderer, Box2D, and generation | Visible and physical edges match; seams, half-tile sides/top, and generated routes pass regression checks |
| **3. Combat foundation** | Next | A basic hero strike and one enemy attack with explicit timing, reach, feedback, and death behavior | Hits match shown contact poses, are directional and fair, and behave consistently while moving or airborne |
| **4. Visual and audio sample** | After mechanics settle | One representative room, hero, enemy, tile kit, and event-sound pass with source/export rules | The room is readable without debug overlays and can be rebuilt from source assets |
| **5. Mechanics proof** | After gates 1–4 | A replayable 10–15 minute path using the existing world and ruins | Complete it repeatedly without movement, collision, combat, or presentation blockers; use it to set the game's actual feature list |
| **6. Small complete game** | Scope after proof | Enough distinct areas, encounters, progression, saving, story delivery, and ending for 3–6 hours | Complete path works from start to ending and survives reload, packaging, and release QA |

The previous 2,400–5,700 hour projection assumed a larger 6–10 hour game and should not be used for this scope. Estimate each gate after its test room and asset samples are built. Record actual time for one polished tile set, character animation, enemy behavior, room, and save feature before projecting the full game.

## The systems to build, in dependency order

1. **Hero feel and contacts:** measure the existing fixed-step motion; tune acceleration, stopping, air control, jump/fall arcs, and grounding. Decide how actor bodies interact. Make the basic move set feel reliable before creating more traversal challenges.
2. **Terrain contract:** make each tile's appearance, collision, support, and line-of-sight behavior explicit. Repair the half tile and keep merged full-tile floors seam-free. Update authored and procedural validation together.
3. **Combat:** define attack startup, active, and recovery windows; link damage to the visible strike, honor facing and range, and make damage/death readable. Build new enemies after one enemy can fight fairly.
4. **Presentation pipeline:** select a pixel/world scale, camera/viewport behavior, asset naming and pivots; create a small coherent art and sound kit. Separate sprite draw dimensions from physics boxes.
5. **Room and world lifecycle:** keep load → validate → build → activate → deactivate → persist → unload working across the current refactor. Add authored state, checkpoints, and save recovery when the mechanics proof establishes the required state.
6. **Map generation:** preserve the versioned ruins profile, deterministic graph, reachability and encounter gates. Recheck them after movement or tile changes. Expand generated content only when the finished tile and combat vocabulary supports it.
7. **Game scope and narrative:** decide the 3–6 hour route, progression, ending, and needed story presentation after the mechanics proof. Implement dialogue/NPCs only if that design calls for them. UI, controller, and accessibility work should support the chosen move and combat set.

## Art and audio production plan

Start with a **one-room visual guide**: virtual resolution, pixels per world unit, a small palette, hero/enemy silhouettes, sprite pivots, animation frame/timing rules, full/half-tile edges, and readable contrast. Make one complete hero action set and one enemy at the intended quality to measure production speed. Keep original editable source files, export rules, asset IDs, and a rights ledger. Expand the guide only when the sample room works; every new animation, hit effect, prop, and UI image still needs a real budget.

Begin with jump, land, swing, hit, enemy tell, damage, and UI sounds. Tie each cue to the actual simulation event so it helps judge timing. Use original or appropriately licensed temporary assets, then expand to ambience/music and mixing controls when the core interaction is stable. [libGDX audio guide](https://libgdx.com/wiki/audio/audio).

## Quality now and release work later

- **Tests:** the repository automates room/profile/template validation, seeded graph/layout/tile/reachability/encounter checks, generated transition and persistence round trips, simulation timing, camera boundaries, and packaged start/pause/death/restart travel. The immediate gaps are hero jump/landing behavior, actor pushing, partial-tile collision, attack timing/direction, and animation state selection. Add focused checks as those mechanics are repaired. Later, add save migration/corruption, checkpoint, controller, and full-path QA. Keep an FPS/frame-time and memory budget for the lowest supported PC.
- **Data and builds:** validate room and save data, keep the packaged build reproducible, and retain backups. Add save corruption/migration work when the full save schema is designed. Review dependencies and Gradle deprecations before a toolchain upgrade.
- **Asset rights:** record source and permitted use for sprites, fonts, sounds, music, and libraries as they enter the project. Keep editable originals and written terms for any contractor work.
- **Release preparation:** after the complete 3–6 hour path exists, test a clean Windows package, then prepare store media, localization and accessibility claims, support, and rollback-ready builds. Verify current [Steam release requirements](https://partner.steamgames.com/doc/store/releasing) at onboarding time rather than treating old fee, timing, or asset rules as fixed.

## Next four weeks: foundation work

Treat these as ordered work packages, not calendar promises. Finish a package only when its acceptance check passes; carry unfinished work forward instead of starting NPCs or new content on schedule.

| Order | Concrete deliverable | Acceptance check |
|---|---|---|
| 1 | Settle the active terrain-fixture refactor and capture movement/contact/attack baselines in a small test room | Current prescribed checks pass; jump arc, stop distance, enemy push, half-tile mismatch, and attack delay are recorded reproducibly |
| 2 | Tune hero movement and choose hero/enemy body interaction | Responsive rise/fall and braking match written targets; hero cannot shove an enemy unexpectedly; grounding and transitions remain reliable |
| 3 | Repair the half tile and define terrain geometry shared by drawing and collision | No invisible borders; all intended tile surfaces, full-tile seams, and generated routes behave as specified |
| 4 | Rebuild the basic attack timeline and animation priority | The visible strike, directional hit window, damage event, and recovery agree in idle, moving, and airborne cases |

The next package after these four is one coherent art/audio sample room, followed by a short mechanics proof. Use that proof to decide the 3–6 hour game structure and whether NPCs, quests, additional generated content, or other systems earn their implementation cost.
