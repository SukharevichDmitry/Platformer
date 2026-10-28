# Roguelike foundation

Status: agreed direction, 2026-10-26. This plan supersedes the first-release scope and ordering in `PROJECT_DISCOVERY_AND_ROADMAP.md`. The movement, collision, tile, and generation notes in the older plans remain technical history.

## Product contract

- Aim for a complete run of roughly **1–2 hours**. This is a pacing target, not a room-count constant; tune it after playable routes exist.
- Death ends the current run. A new run begins with a new seed and without its items, abilities, faction reputation, decisions, defeated enemies, or altered tiles.
- Quitting or pausing must permit **Continue** from the current run. A checkpoint is a suspend/resume record, not a death respawn.
- Narrative decisions affect only the current run. The city's districts, history, key places, and major story beats stay authored; routes, room variants, encounters, opportunities, and consequences vary per seed and choices.
- World rules and interactive content use stable IDs and data independent of texture names. Art can be replaced without changing collision, saves, or story logic. Audio follows the playable systems and visual pass.

## Runtime boundaries

1. **World definitions** are immutable assets: district connections, room templates, encounter and item definitions, dialogue graphs, and ability rules. Validators reject missing IDs, impossible sockets, inaccessible critical routes, and unsupported interactions before a run starts.
2. **Run state** owns the seed, current position and hero state, generated layouts, visited rooms, room/entity mutations, inventory, equipped items, abilities, faction reputation, quest/event flags, and decisions. It is versioned and belongs to one run. No narrative flag survives death.
3. **Room state** owns transient actors and physics bodies. Entering a room reconstructs them from world definitions plus run state. Stable IDs identify persistent enemies, pickups, breakable terrain, and interactables.
4. **Presentation** resolves visual IDs to atlases, animation, UI, and eventually audio. It does not decide collision, rewards, or narrative outcomes.

Current `GeneratedRunSave.narrativeChoices` contains generated event placements, not player choices. Rename/split this when real interaction choices are introduced; do not infer decisions from that field.

## Delivery order

| Stage | Playable result | Acceptance |
|---|---|---|
| 1. Run lifecycle | New Run, Continue after exit, death clears continuation | Seed, location, room, hero position/health and generated mutations survive an orderly exit; a dead run cannot Continue; starting anew never inherits old mutations |
| 2. Interaction and state | One room object can be used, consumed, and restored | Stable object ID, input range, event dispatch, persisted flag, no duplicate reward after reload |
| 3. Items and abilities | Pick up, equip/use, and persist one item and one traversal or combat ability | Definitions separate from sprites; inventory limits and save validation explicit; generation gates account for required abilities |
| 4. Story slice | One authored anchor, NPC dialogue choice, and later consequence | Choice changes only this run and is reproducible after reload; no critical route depends on an unavailable choice |
| 5. City route | Multiple districts connected by authored anchors and generated room routes | Every seed reaches required beats and an ending; branches offer meaningful rewards/risks; 5,000-seed gate and representative manual runs |
| 6. Content and visual pipeline | More enemies, hazards, items, skills and room motifs; replace placeholders with artist assets | New content uses validated IDs and clear art export contracts; existing saves and routes remain valid or migrate intentionally |
| 7. Release pass | Pacing, UI, accessibility, sound effects, ambience and music | Complete 1–2 hour runs, suspend/resume, death reset, Windows package, and visible QA pass |

Build vertical slices through these stages. Do not create a generic quest or skill framework before one concrete interaction exercises its save and gameplay rules.

## First slice details

- Keep a single active-run slot initially. Save format and generated-location profile versions are separate; invalid data must fail closed rather than silently mix two runs.
- Continue restores the current world seed, room, hero position/health and generated-location state. Save on New Run, important changes, pause, exit and application shutdown, plus a five-second timer during play. A forced stop may lose the most recent movement; a power loss during a Preferences flush has no atomic-write guarantee yet.
- New Run and death clear the active checkpoint and any generated-location persistence belonging to the previous run. Existing test constructors without Preferences remain isolated.
- Keep all world/physics mutation outside Box2D contact callbacks. Validate that a restored hero position belongs to a room and is not inside terrain before activating the world.
- Verify the lifecycle with a desktop round trip and the existing `core:check`, `lwjgl3:simulationRegressionTest`, `lwjgl3:generatedLocationTest`, `lwjgl3:generatedRunPersistenceTest`, and `lwjgl3:packagedSmokeTest` on JDK 17.

### Initial implementation

The active-run checkpoint now records seed, location, room, hero position/health, visited rooms, consumed interaction IDs, run flags, and all inventory cells. The menu can resume it after an orderly exit or a sudden process stop; death and New Run clear it together with generated-location records. An authored evidence object in the opening room exercises one-time use and persistence. The Keeper choice and ability are derived from a saved run flag. Authored-room enemy state and future equipment state still need to be added before those systems ship. Do not treat the checkpoint as a complete campaign save yet.

JDK 17 checks passed: `core:check` (including 5,000 generation seeds), `lwjgl3:simulationRegressionTest`, `lwjgl3:generatedLocationTest`, `lwjgl3:generatedRunPersistenceTest`, and `lwjgl3:packagedSmokeTest`. The persistence test covers hero health/position and a defeated generated enemy across world reconstruction; packaged smoke covers the menu Continue path and death reset.

Follow-up for unexpected exits: START writes the first checkpoint immediately; gameplay writes every five seconds and after room changes, hero damage, defeated generated enemies, broken blocks, and interactions. Window shutdown also saves through the application lifecycle. Tests verify a periodic save without visiting the pause/menu flow. A forced process stop can still lose movement since the most recent checkpoint, and an interrupted Preferences write is not yet protected by an atomic backup.

Stage 2 has its first concrete example: `start/0` contains an authored `evidence` interaction with a stable room-local ID and event ID. Pressing E within range consumes it once and records a run flag. Validation rejects duplicate IDs and placements inside solid tiles; a save/reload and a new run prove the flag's lifetime. The note and bandage have replaceable visual IDs, sprite assets and compact pixel-font interaction labels.

Stage 3 has a first item example: `field_bandage` is defined in an asset independently of art. A pickup in `start/0` grants one, within a three-item stack limit. A bandage in a pocket restores one health, up to the hero's current maximum; at full health, use does nothing. Pickup, count, and use are run state. Equipment and generation routes that require an ability remain later work.

The ordinary starting inventory has six bag cells in a 3×2 grid. Two separate pockets sit left and right below the bag at the marked screen positions and also appear on the play HUD. Open the bag, select a cell with the movement keys, and press a pocket key to swap its stack with that pocket. Only pocketed consumables can be used during play; bag contents cannot be used directly. The opening sealed note is a one-of-a-kind bag item. Picking it up consumes the room object only if a cell is available; its stable ID persists with other contents. The bag footer shows only the selected item's name, without control hints.

Checkpoint format 7 stores all eight cells. Formats 2 and 3 migrate their former bandage count; formats 2–4 restore a previously collected note from its old event flag; formats 4–6 compact older 16-cell bags, and format 6 retains its pocket contents. Authored runs currently have at most two distinct pickups. Settings has Controls and Display tabs from the main or pause menu. Key bindings, window resolution, and fullscreen mode are staged until Enter or Save, then persisted to application Preferences independently of run death and New Run. Esc leaves settings without applying unsaved changes, and Esc continues from pause. Duplicate keys are rejected; Reset Defaults stages the original keyboard layout. Faction slots and decoration remain future slices.

## Decisions still needed after the first slice

Новый рабочий дизайн начала игры и расширения руин описан в [RUINS_OPENING_DESIGN.md](RUINS_OPENING_DESIGN.md). Он заменяет нынешний тестовый старт с врагом и боссом на дом матери героя и открытый маршрут через обжитые руины. Размеры комнат и число переходов будут выбраны после проверки одной большой эталонной улицы; существующие сохранения потребуют контролируемой миграции или сброса профиля.

### First story choice and ability slice

The generated ruins exit now opens into the authored Keeper Chamber. A visible Keeper
opens a bottom portrait dialogue when approached with Interact. Accepting binds Echo
Dash for this run and permanently reduces maximum health by one (3 to 2); refusing
locks out the offer for this run. The choice, resulting health and ability survive
Continue and reset on New Run or death. Echo Dash uses a remappable Shift key and a
short cooldown. Version-7 ruins saves with matching generation signatures upgrade to
profile version 8, which opens the final room's exterior doorway. This is the first
stage-3 ability and stage-4 choice slice; later story consequences remain to be built.

- Exact inventory capacity, item stacking, and equipment slots.
- Whether abilities are found, taught, granted by faction offers, or some combination.
- Whether a district can be revisited after its critical beat and how choices alter later encounters.
- Art contract: virtual resolution, pixels per world unit, atlas naming, pivots, animation events, and layered foreground/background rules with the artists.
