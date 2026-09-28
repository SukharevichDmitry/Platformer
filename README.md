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

For a visible check, launch the extracted build, repeat those actions, and
resize both menu and pause screens. Full world/resource disposal and repeated
location travel remain the roadmap's week 2 work.

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
