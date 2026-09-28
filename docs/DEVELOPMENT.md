# Development and validation

Kill Confirmed uses one gameplay implementation for Minecraft 1.21.1, 26.1.2 and 26.2, on Fabric and NeoForge. The supported targets and pinned loader versions are in `profiles.json`.

## Source layout

- `common`: death capture, delivery, snapshots, placeholders and the public API.
- `portable`: Minecraft-independent template and placeholder parsing, with unit tests.
- `adapters/1.21` and `adapters/26`: the two Minecraft player save-hook signatures. The saved key remains `KillConfirmedPendingDogTag`.
- `fabric` and `neoforge`: loader event registration and networking.
- RinLib supplies registry-aware item/text serialization, player command sources and synchronous function execution.

The profile build generates Java sources for the three upstream API renames in 26.x: `ResourceLocation` → `Identifier`, `ResourceKey.location()` → `identifier()`, and Fabric's `PayloadTypeRegistry.playS2C()` → `clientboundPlay()`. It does not rewrite gameplay behavior. Generated sources stay under `build/`.

## Build

Use Java 21 for 1.21.1 and Java 25 for 26.x. From the repository root:

```sh
./gradlew :portable:test
python3 scripts/build-matrix.py
# Or select one supported target:
python3 scripts/build-matrix.py --minecraft 26.2 --loader neoforge
```

Release artifacts and their hashes are written to `build/matrix/`. The wrapper isolates Gradle state for each target. Set `JAVA_21_HOME` and `JAVA_25_HOME` to select installed JDKs. A local RinLib release matrix can be supplied with `--rinlib-dir=/absolute/path/to/rinlib/build/matrix`. The metadata still requires RinLib 1.1.0 or newer.

## Runtime regression suite

`runtime-tests/run.py` launches an isolated dedicated server with the real loader events. It requires an existing accepted `eula.txt` and a matching RinLib jar:

```sh
python3 runtime-tests/run.py --minecraft 1.21.1 --loader fabric \
  --strategy ON_RESPAWN_INVENTORY --rinlib /absolute/path/to/rinlib.jar \
  --eula /absolute/path/to/eula.txt --output /absolute/path/to/test-instances
```

Run both `AT_DEATH_POSITION` and `ON_RESPAWN_INVENTORY` for every target. The suite checks natural and function-triggered deaths, nested datapack functions, restoring command contexts after exceptions, real player respawn events, full-inventory delivery, pending-state disk serialization, snapshot validation, copied public text components, Java placeholders, and recovery from invalid datapack output. A successful Gradle exit alone is insufficient: the runner also requires the probe's completion marker and assertions.

The test mod is a separate source set and is never included in a release jar.

Add `--packaged=/absolute/path/to/kill_confirmed.jar` to test the release artifact instead of development classes. Add `--client` for client startup and item resource checks. Client startup checks do not replace the server behavior suite.
