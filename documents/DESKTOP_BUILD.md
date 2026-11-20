# Windows desktop build

Use JDK 17 on Windows, then run:

```powershell
.\gradlew.bat core:check lwjgl3:simulationRegressionTest lwjgl3:packagedSmokeTest lwjgl3:desktopZip
```

The shareable archive is `lwjgl3/build/distributions/Platformer-1.0.0-windows-x64.zip`.
Unzip the entire archive and run `Platformer/Platformer.exe`. Keep the `app` and
`runtime` directories beside the executable; the build includes its own Java
runtime. The executable and game window use the Platformer icon.

For a local build without making the archive, run `lwjgl3:desktopAppImage` and
launch `lwjgl3/build/desktop/windows-x64/Platformer/Platformer.exe`.

The app image is built with JDK 17 `jpackage`, so this task runs on Windows.
Build outputs stay under the ignored `lwjgl3/build` directory.
