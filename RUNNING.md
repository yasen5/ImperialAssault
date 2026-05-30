# Running Multiplayer

## Command Line

Compile the root launchers:

```sh
javac *.java
```

Run the server:

```sh
java Server
```

Run the server with options:

```sh
java Server --single-client --fresh --no-ui
java Server 2 --fresh
```

Server options:

- `--single-client`, `single-client`, or `wait-one`: start with no required Rebel clients.
- `1`, `2`, `3`, or `4`: require that many Rebel clients.
- `--fresh`, `--no-load`, or `--new-game`: start without loading the previous saved game.
- `--no-ui` or `--headless`: run without the spectator UI.
- `--debug`: draw wall lines in red from their start point to their end point.

Run a client:

```sh
java Client
```

Run a client for a specific seat:

```sh
java Client REBEL_2
```

Client seats:

- `REBEL_1`
- `REBEL_2`
- `REBEL_3`
- `REBEL_4`
- `IMPERIAL`

Add `--debug` to draw wall lines in red from their start point to their end point.
