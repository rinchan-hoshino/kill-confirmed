# Development and validation

Kill Confirmed uses one gameplay implementation for Minecraft 1.21.1, 26.1.2 and 26.2, on Fabric and NeoForge. The supported targets and pinned loader versions are in `profiles.json`.

## Source layout

- `common`: death capture, delivery, snapshots, placeholders and the public API.
- `portable`: Minecraft-independent template and placeholder rendering, with unit tests. Its tests also compile the shared TOML configuration loader.
- RinLib 2.0 owns player save hooks and loader events. The saved key remains `KillConfirmedPendingDogTag`.
- `fabric` and `neoforge`: loader event registration and networking.
- RinLib supplies registry-aware item/text serialization, player command sources and synchronous function execution.

The profile build generates Java sources for upstream API renames in 26.x: identifiers, Fabric payload registration, `getDayTime()` → `getOverworldClockTime()`, and `ComponentUtils.updateForEntity(...)` → `ComponentUtils.resolve(ResolutionContext.create(source), ...)`. The resolution context retains the deceased source entity for score selectors; the overworld clock preserves the old shared world-day semantics across dimensions. Generated sources stay under `build/`.

## Build

Use Java 21 for 1.21.1 and Java 25 for 26.x. From the repository root:

```sh
./gradlew :portable:test
python3 scripts/build-matrix.py
# Or select one supported target:
python3 scripts/build-matrix.py --minecraft 26.2 --loader neoforge
```

Release artifacts and their hashes are written to `build/matrix/`. The wrapper isolates Gradle state for each target. Set `JAVA_21_HOME` and `JAVA_25_HOME` to select installed JDKs. A local RinLib release matrix can be supplied with `--rinlib-dir=/absolute/path/to/rinlib/build/matrix`. RinLib 2.0 is bundled in every artifact; the dependency metadata is satisfied by that nested jar.

## Runtime regression suite

`runtime-tests/run.py` launches an isolated dedicated server with the real loader events. It requires an existing accepted `eula.txt` and a matching RinLib jar for compilation:

```sh
python3 runtime-tests/run.py --minecraft 1.21.1 --loader fabric \
  --strategy ON_RESPAWN_INVENTORY --rinlib /absolute/path/to/rinlib.jar \
  --eula /absolute/path/to/eula.txt --output /absolute/path/to/test-instances
```

Run both `AT_DEATH_POSITION` and `ON_RESPAWN_INVENTORY` for every target. The suite applies lethal zombie, player, void and lava damage through vanilla and the real loader events. It writes `zombie.json`, `player.json`, `void.json`, `lava.json` and `death-cases.json` with the item name/lore components and English plain text. Hazard items are ticked through vanilla physics to record whether they survive; exact-position delivery has no safe-position or respawn fallback. Additional checks cover synchronous function callbacks during ordinary and function-triggered deaths, score/NBT/selector text, optional weapons/killers, malformed-line isolation, Java providers, player-only filtering, command-context restoration, real respawn events, full inventories and pending-state disk serialization. A successful Gradle exit alone is insufficient: the runner also requires the probe's completion marker and assertions.

The test mod is a separate source set and is never included in a release jar.

Add `--packaged=/absolute/path/to/kill_confirmed.jar` to test the release artifact instead of development classes, using its bundled RinLib at runtime. Fabric dedicated-server checks also need `--server-dir=/absolute/path/to/installed-fabric-server`, including the matching Fabric API jar in its `mods` directory. The runner builds a separate remapped probe and copies only that probe, the release jar and Fabric API into the isolated server; no separate RinLib is loaded. NeoForge verifies that RinLib was discovered inside the release jar. Add `--client` for client startup and item resource checks. Client startup checks do not replace the server behavior suite.

## Configuration implementation decisions (1.2.0)

- Reuse NightConfig 3.8.3 instead of maintaining a TOML subset parser. Fabric bundles its core/TOML jars; NeoForge uses the NightConfig already supplied in its boot layer, avoiding duplicate Java modules. Neither needs a separate installation.
- Keep the shared schema-one snapshot intact rather than add a save migration. New `weapon`/`day` values are frozen in the generated lore; existing add-on APIs and saved tags keep their contracts. Legacy translation keys remain for the text components already stored on old tags.
- Retain exact-position delivery instead of adding a new safe-drop policy. Void/lava loss is recorded by the runtime suite and documented; `respawn_inventory` remains the existing alternative.
- Parse each lore line independently, then substitute components and apply the vanilla resolver. An unavailable placeholder skips only its own line; a malformed line warns once per startup. No scratch storage, function tag, output parser or `when` condition participates.
