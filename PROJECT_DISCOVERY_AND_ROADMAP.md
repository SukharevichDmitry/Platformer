# Platformer: discovery and route to a Steam release

Assessment date: 2026-09-28. This is a planning document, not a claim that the game has been playtested. Estimates are **person-hours of focused work**, including integration and ordinary rework. They assume one developer, with art and audio made by that developer or supplied to the same specification. A large, polished game in the spirit of *Blasphemous* is much bigger than the launch scope proposed here.

## Product direction and scope decision

The strongest direction is a compact, atmospheric action platformer built around deliberate melee combat, traversal, exploration, and memorable places. Keep the inspiration in mood and quality bar; establish original characters, iconography, lore, art, music, and combat identity. Write a one-page creative brief answering: who the protagonist is, what the player does every minute, what makes traversal and combat distinct, and why a player returns to an earlier area.

**Recommended first commercial scope (a planning assumption):** a 6–10 hour complete game, 3 distinct biomes, 10–15 enemy types, 3 bosses, 6–10 NPCs, 2–3 movement/combat upgrades, and one ending. Build one 20–30 minute polished slice before committing to those counts. A smaller 3–5 hour game is a valid release if playtesting shows the larger scope exceeds capacity. Defer online services, multiplayer, sprawling branching dialogue, and fully procedural maps until the core game is proven.

The world should initially use **hand-authored rooms plus controlled variation**: authored landmark/combat rooms, a room graph, optional seeded room selection, and decorative tile rules. Pure random tile generation creates a heavy obligation to prove reachability, combat readability, pacing, save compatibility, and visual quality. Build deterministic generation only after a hand-authored slice is fun.

## What exists today

- Java/libGDX desktop project with LWJGL3 and Box2D; Gradle wrapper and runnable-jar packaging are configured.
- A hero with movement, jump, melee attack, health and motion/combat state machines; a basic enemy and a boss config using shared AI; collision sensors and merged solid-tile fixtures.
- Two locations represented by room JSON: `start` has three rooms and two spawns; `forest` has one room. Room-neighbor placement and an exit from `start/2` to `forest/0` are underway.
- A following camera, tile/actor rendering, debug overlays, a menu and pause screen.
- A small placeholder atlas and sprites. No audio assets, save implementation, dialogue/NPC system, automated tests, or shipping UI assets were found.

These observations describe the **current working tree**, which has uncommitted room/world/actor changes. The sample JSON files parse and their declared row widths/heights match; gameplay and packaging were not validated by a successful build.

## Highest-priority findings

| Priority | Evidence in current tree | Consequence and recommended fix |
|---|---|---|
| Blocker | `MenuScreen` and `PauseScreen` load `uiskin.json`, absent from `assets`. | The menu is likely to fail at launch. Add a valid skin/fonts/atlas or replace the placeholder UI, then smoke-test the packaged build. |
| Blocker | `GameWorld.changeLocation()` assigns the new `location` before `rebuildLocation()` calls `clearLevelBodies()`. | The old location's Box2D bodies remain; old enemies also remain in the world. Destroy/unload the old runtime first, then load and activate the new one, with a repeat-transition test. |
| High | `GameScreen.dispose()` is empty; `WorldRenderer` allocates a batch, shape renderer and atlas; `GameWorld` allocates a Box2D world; `Tile` keeps a static atlas. | Native memory is retained across starts/deaths/menu switches. Give the game/asset service clear ownership and implement disposal; avoid an unmanaged static atlas. [libGDX resource guidance](https://libgdx.com/wiki/articles/memory-management). |
| High | `GameWorld.update()` calls hero controller (which calls `hero.update`) and then updates the hero motion machine again. Hero state changes can also be re-entered from held movement input. | Animation/state timing and transitions may be inconsistent. Update each state machine once per simulation tick and avoid re-entering an unchanged state. |
| High | `world.step(min(delta, 1/30), ...)` has no accumulator; enemy death is removed before a death presentation can run. | Frame stalls lose game time, and death feedback is abrupt. Use a capped fixed-step accumulator; separate defeated/death animation/body-removal phases. |
| High | `RoomManager` assumes room `0` and every referenced neighbor exists; `RoomInstance.buildTiles()` indexes rows directly. | A bad authored/generated room can crash during load. Add schema checks and a pre-build content validator with useful file/row errors. |
| Medium | Room state is keyed only by numeric room ID and cleared on location change; no persistent save schema exists. | Revisiting a location cannot reliably preserve enemies, doors, quests, or pickups. Use stable `locationId/roomId/entityId` keys and versioned save data. |
| Medium | Camera snaps to hero at a fixed 10×7 world view; resize only records window size. All loaded rooms are built and enemies are spawned at once. | Wide windows, large locations, and vertical movement will expose framing and performance issues. Add a viewport, bounded/look-ahead camera and room activation/streaming after the lifecycle is stable. |
| Medium | Root Gradle declares libGDX 1.10.0 while modules use `gdxVersion=1.12.1`; several repositories and packaging paths are configured. | Dependency resolution may mask version conflicts. Use one version catalog/property, trim repositories, and validate a clean packaged build before updating dependencies. |

The first two items deserve attention before adding content. Avoid a large engine rewrite; keep the current Java/libGDX foundation and separate **simulation, content data, rendering, input, audio, and persistence** at their ownership boundaries.

## Work discovery and estimates

| Phase / exit gate | Development and design | Art and audio | QA, delivery, security | Hours |
|---|---|---|---|---:|
| **0. Stabilize prototype** — fresh install launches, start/pause/death/restart and repeated location transitions work | Fix listed blockers; fixed timestep; screen/resource ownership; validated room format; basic game settings and input map | Temporary licensed or original UI placeholders; audio event list | Build smoke test, 3–5 focused regression tests, repeat-transition memory check | 120–250 |
| **1. Vertical slice** — 20–30 minutes worth replaying | One connected micro-biome, 2–3 enemies with distinct behavior, one miniboss, one traversal upgrade, checkpoint/save, one NPC and short dialogue, bounded dynamic camera, controller support | Final style guide, final hero animation set, one environment kit, enemy silhouettes/VFX, combat/ambient/music pass | External playtest with observed completion, accessibility pass, frame-time/memory budgets | 350–700 |
| **2. Content pipeline** — new room/enemy/quest can be added predictably | Data-driven enemy definitions, room/entity IDs and validation, room editor/export path, event/quest/dialogue data, save migrations, seeded generation prototype with reachability tests | Atlas export/naming rules, palette/animation templates, sound naming/mix rules | CI build and asset validation, content review checklist, dependency inventory | 200–400 |
| **3. Full content** — feature complete at agreed scope | Finish 3-biome world, transitions, encounters, bosses, NPCs, quest and progression loops, map/fast travel, difficulty tuning | Biome tiles/backgrounds/parallax, character and boss sprites, VFX, UI, full SFX/ambience/music and mix | Iterative playtests, controller/keyboard rebinding, localization-ready text, copyright/license ledger | 1,000–2,200 |
| **4. Beta and release candidate** — no known progress-loss or launch blockers | Balancing, save migration/failure recovery, performance and loading, credits/options, achievements only if they serve the game | Final readability/color/accessibility pass; trailer captures and store graphics | Full-path QA, clean OS install tests, hardware/resolution matrix, backups, Steam depot/beta testing | 400–850 |
| **5. Steam launch and first support window** — shippable build and support process | Fix launch issues, versioning, patch notes, rollback-ready builds | Final store trailer/screenshots/capsules; postlaunch fixes | Steam page/build review, pricing/localization metadata, support channel and crash triage | 180–350 |

**Planning total:** about **2,250–4,750 hours**, plus 20–30% contingency for unfamiliar art/audio work, rework, and test feedback: roughly **2,700–6,200 hours**. At 25 productive hours/week, that is approximately **2–5 years**; at 40 productive hours/week, **1.3–3 years**. These are scenario estimates, not a release promise. Hiring an artist/composer can shorten elapsed time but adds budget, direction, integration, and rights-clearance work. Re-estimate after the vertical slice using measured hours per room, enemy, animation set, track, and bug.

## The systems to build, in dependency order

1. **Core feel:** fixed simulation; buffered jump and coyote time if playtests need them; acceleration/friction, variable jump, hit reactions, invulnerability window, readable enemy telegraphs and fair checkpoints. Write down target timings so combat remains consistent as content grows.
2. **Room and world lifecycle:** explicit load → validate → build → activate → deactivate → persist → unload flow. Keep authored room coordinates and transition entry points in data; scope `RoomState` by location and stable entity IDs. For smooth biome borders, use authored connective rooms, progressively changing palette/tiles/weather/parallax, prefetch the next biome's assets, and crossfade ambience/music. Add this presentation only after exits are safe and reversible.
3. **Camera and pixel presentation:** choose a virtual resolution and pixels-per-world-unit rule, use an aspect-aware viewport, make camera bounds/vertical zones/look-ahead configurable per room, and test pixel snapping against smooth movement at multiple window sizes.
4. **Content architecture:** separate enemy configuration/animation/audio IDs from behavior code; favor a small behavior vocabulary (patrol, investigate, chase, wind-up, attack, recover, retreat). Bosses get purpose-built phases. Create a content validator before creating hundreds of JSON files.
5. **Map generation:** represent the world as a graph of room templates with doors/biome tags and a stored seed. Verify connectivity, progression gates, spawn safety, room overlap, exit alignment, and save/reload of generated layouts. First ship a fixed authored route if generator quality remains uncertain.
6. **Narrative and UI:** data-driven conversations with IDs, conditions, effects, localization keys, journal/map prompts, skip/advance behavior, readable fonts, remappable keyboard and controller actions, separate game and UI input handling.
7. **Persistence:** schema version, stable IDs, checkpoint and inventory/progression state, safe atomic file replacement and backup, corruption recovery, and migration tests across builds. Store display settings separately from save progression. Consider Steam Auto-Cloud after local saves are reliable; it syncs configured files without requiring a Steam API integration. [Steam Cloud guidance](https://partner.steamgames.com/doc/features/cloud).

## Art and audio production plan

Create a **visual bible** before mass production: internal pixel resolution, palette per biome, silhouette hierarchy, animation frame/timing rules, light/VFX rules, parallax layers, tile edges/corners, UI type scale, and contrast requirements. Prototype one complete hero action and one complete enemy at final quality to measure actual production speed. Maintain original editable source files, export scripts, asset IDs, and a rights ledger. Avoid promising that placeholder sprites will be upgraded later without budgeting every animation, hit effect, portrait, environment prop, and marketing image.

Build an **audio event map** early: footsteps by surface, jump/land, swing/hit/guard, enemy wind-up, damage/death, UI, weather, room ambience, boss states, and biome music layers. Use short sound effects as `Sound` and streamed long tracks as `Music`; libGDX supports both. [libGDX audio guide](https://libgdx.com/wiki/audio/audio). Add master/music/SFX/ambience sliders, pause/focus behavior, ducking for key moments, and loudness checks. A silent prototype hides timing and feedback problems, so add rough but licensed/original sound in the slice before commissioning a final score.

## Quality, security, and business work

- **Tests:** automate room JSON shape/neighbor/transition checks, seeded graph reachability, save round-trip/migration, combat timing and collision regressions. Add a manual one-page checklist for start, pause, death, checkpoint, return travel, resize, controller disconnect, fresh install, and upgrade from an old save. Keep an FPS/frame-time and memory budget for the lowest supported PC.
- **Security proportional to an offline game:** validate untrusted/corrupt save and room data before allocating objects or indexing arrays; bound file sizes, map dimensions, coordinates and IDs; use a safe per-user save directory and atomic writes. Pin and verify Gradle dependencies, remove unnecessary repositories, scan build dependencies, never commit Steam credentials, restrict partner account access, and keep release artifacts/backups. [Gradle locking](https://docs.gradle.org/current/userguide/dependency_locking.html) and [dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html) explain reproducible and integrity-checked dependencies. DRM/anti-cheat/backend architecture is low priority for this offline scope.
- **Legal/business:** record author, source, license, territory and commercial-use terms for every sprite, font, sound, music track and library; use written assignment or license terms for contractors. Select final title and logo only after checking availability and likely conflicts. Budget localization, tax/account onboarding, age/content disclosures, customer support, refunds and postlaunch patches with jurisdiction-specific professional advice where needed.
- **Steam delivery:** start with Windows as the first supported build; add Linux/macOS only after testing those packages on those systems. Prepare store page, actual-gameplay screenshots/trailer, current-size capsule/library art, description, supported languages/features, system requirements, pricing, depot/launch options, and a private beta branch. Valve requires review of both the store page and build; a public Coming Soon page must be live for at least two weeks before release. New partners also face a 30-day wait after paying the app fee. The Steam Direct fee is currently **US$100 per app**, recoupable after the documented revenue threshold. Verify current terms when onboarding. [Release process](https://partner.steamgames.com/doc/store/releasing), [Steam Direct](https://partner.steamgames.com/steamdirect/), [app fee](https://partner.steamgames.com/doc/gettingstarted/appfee), [art specifications](https://partner.steamgames.com/doc/store/assets), [SteamPipe upload](https://partner.steamgames.com/doc/sdk/uploading).
- **Marketing:** collect honest clips and playtest quotes once the slice looks representative. Put the Coming Soon page up when the title, identity and gameplay screenshots are credible; maintain a small cadence of playable updates, a demo only if it helps discovery, and a short list of press/creator contacts. Measure wishlists, demo completion, feedback themes and conversion cautiously; no sales target is guaranteed.

## Next four weeks (about 80–120 focused hours)

| Week | Concrete deliverable | Acceptance check |
|---|---|---|
| 1 | Launchable menu and packaged build; fix missing skin and version configuration | Fresh extracted build reaches gameplay, pause and restart without missing asset errors |
| 2 | Correct location unload/reload and enemy cleanup; explicit disposal ownership | Travel between `start` and `forest` repeatedly; body/resource counts return to baseline |
| 3 | Fixed-step update, one update per state machine, room validator and regression tests | No duplicated motion timing; malformed room reports a clear error; tests run in CI/local build |
| 4 | One-page creative brief, visual/audio bible draft, vertical-slice room and enemy specification | A 20–30 minute slice has an asset list, interaction map, effort estimate and playtest goal |

After week 4, make one decision: whether the current combat and traversal are compelling enough to justify the 3-biome production plan. If they are not, iterate the slice before multiplying content.
