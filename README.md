# Platformer

A libGDX 1.12.1 / Box2D platformer prototype. The authored `start` location leads
to a seeded, generated `ruins` run. JDK 17 is used for development checks.

## Where to work

- `GameWorld` owns the fixed 1/60-second simulation, Box2D world, location
  transitions, enemies, and generated-run defeat state. `HeroController` stores
  input edges and applies movement/jump/attack on fixed steps, not render frames.
- `GameContactListener` tracks terrain-foot, vision, and attack-sensor overlaps.
  `GameWorld.refreshHeroGrounding` also checks post-step body-contact normals so
  the hero can stand on an enemy's head but not jump from its side. Never destroy
  bodies in a Box2D contact callback.
- `RoomManager` loads authored `assets/levels/start` JSON or generated ruins from
  `assets/generation/ruins.json` and its templates. `LocationInstance` builds
  terrain fixtures: full solid cells merge across room borders; partial tiles
  keep their own bounds.
- `WorldRenderer` draws the world; `GameScreen` owns its lifecycle and clears
  held/buffered input on pause or hide. `GeneratedRunStore` persists a generated
  run via libGDX Preferences. Changing generated output or save compatibility
  requires reviewing the generation profile version.
- `WorldRenderer` eases vertical camera movement while following horizontally;
  `MiniMapRenderer` draws a top-right room map using `GameWorld`'s per-location
  visit history. Room visits last for the current run, not across saved runs.
  `LocationTitleOverlay` briefly names each location on entry.

Start with [ROGUELIKE_FOUNDATION_PLAN.md](documents/ROGUELIKE_FOUNDATION_PLAN.md) for the current game direction and [FOUNDATION_POLISH_PLAN.md](documents/FOUNDATION_POLISH_PLAN.md) for polish status and remaining
manual feel checks. Its original audit is historical, not a description of all
current behavior. Check `git status` before editing: this workspace may contain
uncommitted gameplay work.

## Build and check the desktop release

Use JDK 17 to run Gradle (set `JAVA_HOME` to your JDK installation).
`gradle.properties` defines both `gdxVersion` and `projectVersion`; module
dependencies and release names use those values.

```powershell
./gradlew.bat clean lwjgl3:distZip
./gradlew.bat lwjgl3:packagedSmokeTest
```

The ZIP is `lwjgl3/build/distributions/lwjgl3-1.0.0.zip`. Extract it to a new
directory and launch `bin/lwjgl3.bat` on Windows. This distribution requires
Java 8 or newer installed; it does not bundle a Java runtime. You can also run
`java -jar lib/Platformer-1.0.0.jar` from the extracted directory. All game
assets and the prototype UI font are inside the JAR; no source checkout or
external `uiskin.json` is needed.

The smoke task extracts the release and runs it with a hidden desktop window,
so a working graphics driver/display is required. It renders the menu and
gameplay, clicks START, presses Escape, clicks Continue, returns to the menu,
starts a fresh game, and clicks EXIT. Failures produce a nonzero exit status.
It uses only the extracted release JAR and the test harness on its classpath.
It also crosses between `start` and generated `ruins` repeatedly, checks
room/enemy body counts and texture counts, then checks disposal after a death.

For a visible check, launch the extracted build, repeat those actions, and
resize both menu and pause screens. Location transitions now unload old room
and enemy bodies before activating the destination. The game screen disposes
its Box2D world, atlas, sprite batch and shape renderer when the run ends.
To return from ruins, move left through its entry edge. You reappear inside
`start` room 3, clear of the exit that leads back to ruins. The packaged smoke
test checks both border crossings repeatedly.

The camera follows the hero until any side of the view reaches one
world block past the outermost room in the current location. Run
`./gradlew.bat lwjgl3:cameraBoundaryTest` to check the limit in both `start`
and the legacy authored `forest` test fixture; it also checks that ordinary
following continues between the edges.

## Simulation and room checks

Gameplay advances in fixed 1/60-second steps, with at most six steps processed
after a long frame. The hero motion and combat machines each advance once per
step. Room JSON is validated before any room tiles or physics are built.

```powershell
./gradlew.bat core:check
./gradlew.bat lwjgl3:simulationRegressionTest
./gradlew.bat lwjgl3:packagedSmokeTest
```

`core:check` validates the authored rooms and checks malformed fixtures for
useful file, row and neighbor errors; it also runs the seeded generation
quality gate. The simulation test uses a hidden graphics window to check fixed
steps, movement/contact behavior, jump buffering, and attack timing. The
packaged smoke test checks the distributable in a hidden desktop window,
including repeated border travel and screen lifecycle. Desktop checks require
a working graphics driver/display.

The procedural ruins pipeline generates a seeded graph, room layout, tile rows,
and encounter spawn data. Walk right through `start` room 3 to enter ruins;
walk left through the ruins entry to return. The layout is generated once per game run.
The menu offers CONTINUE RUN after an orderly exit or a sudden process stop.
The game saves immediately on START and important world changes, every five
seconds during play, and on pause/shutdown. A forced stop can lose up to roughly
five seconds of movement. Continue restores the location, hero and generated
ruins state; START and death clear the previous run. In the opening room, press
E near the gold note marker to pick it up. The checkpoint stores the note and
consumable items. In the same opening room,
the bandage sprite is on the starting platform, near the hero;
the sealed note sprite is farther left. A small pixel-font `E` and item name appear
above the nearby object. Press E near the bandage to take it. Open the six-cell
bag with I, select the bandage with the movement keys, and press 1 or 2 to
exchange it with the corresponding pocket. During play, press that pocket's
key while injured to heal one health. Items cannot be used directly from the
bag. SETTINGS is available from the main and pause menus. Its Controls and
Display tabs let you change keys, the window resolution, and fullscreen mode. Press Enter to
save both tabs across game sessions, or Esc to leave without saving. Esc also
continues the game from the pause menu. These settings are separate from the run.
One map character now represents one 1 × 1 terrain tile and one world unit.
Room JSON rows directly define the collision grid; there is no runtime 2 × 2
expansion. Existing maps, actor dimensions, movement, gravity, and camera view
were doubled numerically to preserve their previous physical and visual scale.
Generated rooms use open streets, short supported ledges, and tile-sized stone shaping.
The ruins profile is version 10, with 11–14 rooms per run. Old run checkpoints
are reset when their world layout version differs; incompatible generated
layouts are rejected instead of being loaded into the new geometry.

### Map and world coordinates

Each character in `tileRows` occupies exactly one tile and one world unit.
`width` is the number of characters in each row; `height` is the number of
rows. Text rows run from top to bottom, while Box2D and room-local coordinates
start at the bottom left: character `(column, row)` occupies world rectangle
`[worldX + column, worldX + column + 1] ×
[worldY + height - row - 1, worldY + height - row]`.
`#` is one full solid tile, `_` is a lower half tile, `=` is a one-way ledge,
`B` is one breakable tile, and a space is empty. Interaction and enemy
positions, door offsets and spans, hero dimensions and movement, camera size,
and road module placement use the same world units. The 512 × 1024 road
image covers 2 × 4 tiles without stretching; its artwork is separate from
the solid `#` cells beneath it.
The opening starts in the mother's repaired shelter, passes through a lived-in
courtyard and a short transition, then reaches the broad street in `start` room 3.
The opening and ruins now use distinct pixel-art scenery for the family shelter,
Red streets, Ink quarter, and ruined mechanical workshop. Generated rooms choose
the scene from their story landmark or a deterministic street variant. Backdrops
keep their source aspect ratio, use smaller world-scale panels, and overlap with
soft edges so district transitions do not expose hard image seams. The gameplay
camera preserves square world units at windowed and fullscreen resolutions.
The shelter warden, scavenger, and Red collector have separate character sprites.
The shelter warden in the courtyard asks the hero to find the missing-neighbors
list. The list lies in the broad street; return it to her or keep it. This choice
is saved for the current run. The ruins currently feature two additional hostile
roles, a quick scavenger and a tougher Red collector. Their sprites are tinted
placeholders until the faction art is ready.
Open the inventory with I and press Tab for the Abilities page. It records
Echo Dash, its key, origin, and maximum-health cost after the Keeper's offer.
The final ruins room leads to the Keeper Chamber. Approach the hooded Keeper and
press E to open a dialogue panel. Accepting the offer reduces maximum health
from 3 to 2 and grants Echo Dash (Left Shift by default, remappable in Controls).
Refusing keeps maximum health and closes the offer for the current run. Either
choice survives Continue, while New Run and death clear it. Esc closes dialogue.
The new ruins route stays on one level under open sky. Wooden one-way ledges can be jumped through
from below and landed on from above. Heavily cracked blocks can be broken with
X in attack range; they stay broken when the generated run reloads. The muted
masonry behind rooms is decorative and has no collision.
Structural stone now uses one compatible nine-face masonry family across
connected walls and floors: center, side, top/bottom, and correctly oriented
corners are selected from world-neighbor occupancy, including room seams.
Exterior backing continues past room edges
and darkens with distance, without changing the room's physical boundaries.
Run `./gradlew.bat core:validateRoomEncounters` to check 1,000 seeds for stable
spawns, allowed enemy types, difficulty limits, walkable terrain, and clear
entries and story space. Run `./gradlew.bat lwjgl3:generatedLocationTest` to
check packaged border travel and repeated room and enemy cleanup.

Generated ruins persist through the platform's libGDX Preferences storage. The
save includes its profile version, seed, graph and layout signatures, narrative
room choices, defeated enemy spawn IDs, and broken tile IDs. Run
`./gradlew.bat lwjgl3:generatedRunPersistenceTest` to verify that a complete
game-world reload restores the same rooms, exits, choices, and defeated enemies.

Before week 1 changes, a source snapshot, working/index patches and Git index
copy were saved in the ignored `backups/` directory. Keep that directory when
cleaning build outputs. Existing gameplay edits are separate from the week 1
commit; the packaged smoke check exercises the current working tree.

Movement, tile-collision, or enemy-body changes also warrant
`lwjgl3:generatedLocationTest`; changes to generated content or save behavior
warrant `lwjgl3:generatedRunPersistenceTest` and a review of the ruins profile
version. Automated checks cannot certify how movement or combat feels, so pair
them with a visible playthrough of `start` and one generated ruins run.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
