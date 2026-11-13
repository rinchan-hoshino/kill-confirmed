# Development

Kill Confirmed uses one gameplay implementation for Minecraft 1.21.1, 26.1.2 and 26.2, on Fabric and NeoForge. Supported targets and loader versions are listed in `profiles.json`.

## Source layout

- `common`: death capture, delivery, snapshots, placeholders and the public API.
- `portable`: Minecraft-independent template and placeholder rendering, with unit tests. Its tests also compile the shared TOML configuration loader.
- `fabric` and `neoforge`: loader event registration and networking.
- `profiles`: Gradle scripts for the per-target builds.
- RinLib 2.0 is bundled. It provides registry-aware item and text serialization, player command sources, synchronous function execution and the player save hooks. The saved key stays `KillConfirmedPendingDogTag`.

## Build

Use Java 21 for 1.21.1 and Java 25 for 26.x. From the repository root:

```sh
./gradlew :portable:test
python3 scripts/build-matrix.py
python3 scripts/build-matrix.py --minecraft 26.2 --loader neoforge
```

Release artifacts and their hashes are written to `build/matrix/`. Set `JAVA_21_HOME` and `JAVA_25_HOME` to select installed JDKs.

## Configuration decisions (1.2.0)

- NightConfig 3.8.3 reads the TOML file. Fabric bundles its core and TOML jars. NeoForge uses the copy supplied by its boot layer.
- The snapshot layout is unchanged, so no save migration is needed. `weapon` and `day` values are written into the lore when the tag is created. Legacy translation keys stay for text already stored on old tags.
- Delivery is at the exact death position by default. Void and lava losses are a known consequence; `respawn_inventory` avoids them.
- Each lore line is parsed on its own. A missing placeholder hides only its line. A malformed line logs one warning per startup.
