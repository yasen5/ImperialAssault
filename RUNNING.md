# Running Multiplayer

## VS Code

Use **Run Task** for the Gradle commands:

- `gradle: runServer`
- `gradle: runClient`
- `gradle: smokeTutorialNetwork`

Each task prompts for the supported options before it starts.

Use **Run and Debug** when you want breakpoints:

- `Run Server (Gradle)`
- `Run Client (Gradle)`
- `Run Smoke Tutorial Network (Gradle)`

These launch entries run the Gradle commands in an integrated terminal. They do not require the VS Code Java debugger extension.

For Java breakpoints, install the VS Code Java debugger extension. The workspace recommends `vscjava.vscode-java-debug` and the Java extension pack.

Then use **Run and Debug**:

- `Debug Server (Java Launch)`
- `Debug Client (Java Launch)`
- `Debug Server (Gradle Attach)`
- `Debug Client (Gradle Attach)`

The Java Launch configs are the most reliable breakpoint path because VS Code launches the app from the imported Gradle project and owns the classpath/source mapping.

The Gradle Attach configs run the matching Gradle task with `--debug-jvm`, wait for the JVM to listen on port `5005`, and attach the Java debugger. They declare `src/main/java` explicitly for source lookup.

You can also start the suspended JVM manually from **Tasks: Run Task** with `gradle: debugServer` or `gradle: debugClient`, then attach a Java debugger to `localhost:5005`.

Gradle is not configured to optimize away debug information. `JavaCompile` emits debug metadata by default, and this build only sets UTF-8 encoding.
The workspace settings also tell VS Code to import Gradle automatically and rebuild Java metadata when the build changes.

## Command Line

Run the server:

```sh
./gradlew runServer
```

Run the server with options:

```sh
./gradlew runServer -Pargs="--single-client --fresh --no-ui"
./gradlew runServer -Pargs="2 --fresh"
```

Server options:

- `--single-client`, `single-client`, or `wait-one`: start with no required Rebel clients.
- `1`, `2`, `3`, or `4`: require that many Rebel clients.
- `--fresh`, `--no-load`, or `--new-game`: start without loading the previous saved game.
- `--no-ui` or `--headless`: run without the spectator UI.
- `--debug`: draw wall lines in red from their start point to their end point.

Run a client:

```sh
./gradlew runClient
```

Run a client for a specific seat:

```sh
./gradlew runClient -Pargs="REBEL_2"
```

Client seats:

- `REBEL_1`
- `REBEL_2`
- `REBEL_3`
- `REBEL_4`
- `IMPERIAL`

Add `--debug` to draw wall lines in red from their start point to their end point.
