# Procedural world generation summary

## Runtime flow

The authored `start` room 2 exit loads the `ruins` generation profile through `RoomManager`; the generated entry returns to `start` room 2. `forest` remains in the assets as legacy authored content but is not on the normal route. Authored and generated locations both become `LocationInstance` and `RoomInstance` objects, so rendering, collision, enemies, camera bounds, and transitions use the same runtime path.

Generation runs before the current location is unloaded and outside a locked Box2D step:

1. `GenerationProfile` validates global room, enemy, budget, production-tuning, and return-transition constraints.
2. `RoomTemplateCatalog` validates reusable room geometry, doorway sockets, required story anchors, optional branches, and narrative IDs.
3. `RoomGraphGenerator` chooses a seeded room count, preserves the required arrival → bell → shrine → exit order, adds a guaranteed upper watchtower above the bell hall, weighted fillers, and optionally a left-side branch.
4. `RoomLayoutPlacer` chooses allowed sizes, aligns reciprocal doorways, and rejects overlap or exposed connections.
5. `RoomTileGenerator` adds bounded platforms and cover while `RoomRouteReachability` preserves horizontal walking paths and a clear vertical climb shaft.
6. `RoomEncounterGenerator` places supported enemies within room and location budgets, keeping entries and narrative areas clear.
7. `GeneratedLocationFactory` opens the leftmost exterior entry, validates it, and converts the result to normal room data.
8. `GeneratedRunStore` saves the profile version, seed, topology, placement, narrative choices, and defeated enemy IDs in libGDX Preferences.

The current production profile is `assets/generation/ruins.json`, version 5. Version 5 narrows the guaranteed watchtower shaft and adds interactive platform/breakable tiles; older saved ruins runs are incompatible and start fresh. A generated seed is shown in the upper-left corner while the player is inside a generated location. In the bell hall, stand within the narrow visible ladder and hold Up or Down to traverse it; Left/Right moves along or off it, and Space jumps away. The upper release point clears the floor lip so walking out works.

## Production tuning

The production profile is checked over 5,000 seeds. The accepted mix is:

- 7–8 rooms on average, within a hard 5–10 room range.
- 4–6 enemies on average.
- 50–55% combat rooms (including the guaranteed watchtower encounter).
- An additional, nonrequired left-side branch in 40–60% of runs; the upper room is present in every run.
- No more than four consecutive safe rooms on the main route.

The arena weight is 3 and the arena quota requires at least three rooms. Filler insertion minimizes long safe-room streaks while retaining deterministic seed variation.

Representative manual playtest seeds are written to `core/build/reports/generation/ruins.json`. Use that report for current shortest, longest, side-branched, and no-side-branch seeds; all include the upper room.

## Updating generation content

When changing the ruins profile or templates:

1. Increase the profile `version` if the change can alter an existing generated run. Old incompatible saves will be discarded safely.
2. Keep required story templates in `requiredRoute` order and retain valid reciprocal doorway sockets.
3. Register new enemy IDs in `EnemyFactory` before adding them to a profile palette.
4. Update hard quotas and `tuningTargets` deliberately; do not widen targets only to hide an unintended distribution change.
5. Run the 5,000-seed gate and inspect the JSON report.
6. Play the representative shortest, longest, branched, and unbranched seeds shown in the report.
7. Run the generated transition, persistence, simulation, and packaged smoke tests before release.

Commands:

```powershell
./gradlew.bat core:qualityGateGeneratedWorld --offline
./gradlew.bat core:qualityGateGeneratedWorld -PgenerationSeed=12345 --offline
./gradlew.bat core:check lwjgl3:generatedLocationTest lwjgl3:generatedRunPersistenceTest lwjgl3:simulationRegressionTest lwjgl3:packagedSmokeTest --offline
```

## Bugs found during the final audit

- The transition quality rule previously chose the leftmost room and then only checked that no room was farther left. It now runs the same exterior-entry opening code as runtime generation and validates the opened route and floor clearance.
- Aggregate combat percentage allowed some seeds to contain six consecutive safe rooms. The production profile now requires a combat filler, and seeded filler placement minimizes safe streaks. The verified maximum is four.

## Current limitations

- Only `default` and `boss` enemy types are registered; `enemy1` and `enemy2` need definitions before profiles may use them.
- Hazards do not exist yet. Generation can emit full stone, lower-half steps, one-way ledges, breakable blocks, and empty space; masonry recesses behind the rooms are visual-only.
- Narrative event IDs and choices are generated and persisted, but there is no interaction or dialogue system consuming them yet.
- The story-critical route remains horizontal; the watchtower is an upper combat branch reached by a climb shaft. Other vertical room templates and multi-floor critical routes are not yet authored.
- Persistence stores one generated run per location rather than multiple save slots or named campaigns.
