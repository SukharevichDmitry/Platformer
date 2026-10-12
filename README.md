# Platformer

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

This project was generated with a template including simple application launchers and an `ApplicationAdapter` extension that draws libGDX logo.

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
It also crosses between the `start` and `forest` locations 20 times, checks
room/enemy body counts and texture counts, then checks disposal after a death.

For a visible check, launch the extracted build, repeat those actions, and
resize both menu and pause screens. Location transitions now unload old room
and enemy bodies before activating the destination. The game screen disposes
its Box2D world, atlas, sprite batch and shape renderer when the run ends.
To return from the forest, move left through its open edge. You reappear inside
`start` room 2, clear of the exit that leads back to the forest. The packaged
smoke test checks both border crossings repeatedly.

The camera follows the hero until any side of the view reaches one
world block past the outermost room in the current location. Run
`./gradlew.bat lwjgl3:cameraBoundaryTest` to check the limit in both `start`
and `forest`; it also checks that ordinary following continues between the edges.

## Simulation and room checks

Gameplay advances in fixed 1/60-second steps, with at most six steps processed
after a long frame. The hero motion and combat machines each advance once per
step. Room JSON is validated before any room tiles or physics are built.

```powershell
./gradlew.bat core:check
./gradlew.bat lwjgl3:simulationRegressionTest
```

`core:check` validates the authored rooms and checks malformed fixtures for
useful file, row and neighbor errors. The simulation test uses the packaged
game and a hidden graphics window to check split frames, a capped stall, and
held movement state timing. The GitHub Actions validation workflow runs both
checks on Ubuntu with a virtual display.

The procedural ruins pipeline generates a seeded graph, room layout, tile rows,
and encounter spawn data. Walk right through forest to enter ruins; walk left
through its outermost room to return. The layout is generated once per game run.
Run `./gradlew.bat core:validateRoomEncounters` to check 1,000 seeds for stable
spawns, allowed enemy types, difficulty limits, walkable terrain, and clear
entries and story space. Run `./gradlew.bat lwjgl3:generatedLocationTest` to
check packaged border travel and repeated room and enemy cleanup.

Before week 1 changes, a source snapshot, working/index patches and Git index
copy were saved in the ignored `backups/` directory. Keep that directory when
cleaning build outputs. Existing gameplay edits are separate from the week 1
commit; the packaged smoke check exercises the current working tree.

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
