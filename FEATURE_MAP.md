# Kill Confirmed 2.0.0 — Feature & Architecture Map

## Build Matrix (8 Targets)

| Target | API Layer | Java | Output JAR |
| --- | --- | --- | --- |
| `1.20.1` Fabric | `v120` | 17 | `kill-confirmed-2.0.0+1.20.1-fabric.jar` |
| `1.20.1` Forge | `v120` | 17 | `kill-confirmed-2.0.0+1.20.1-forge.jar` |
| `1.21.1` Fabric | `v121` | 21 | `kill-confirmed-2.0.0+1.21.1-fabric.jar` |
| `1.21.1` NeoForge | `v121` | 21 | `kill-confirmed-2.0.0+1.21.1-neoforge.jar` |
| `26.1.2` Fabric | `v26` | 25 | `kill-confirmed-2.0.0+26.1.2-fabric.jar` |
| `26.1.2` NeoForge | `v26` | 25 | `kill-confirmed-2.0.0+26.1.2-neoforge.jar` |
| `26.2` Fabric | `v26` | 25 | `kill-confirmed-2.0.0+26.2-fabric.jar` |
| `26.2` NeoForge | `v26` | 25 | `kill-confirmed-2.0.0+26.2-neoforge.jar` |

## Source Layout

- `common/src/main/java/dev/rinchan/killconfirmed/`
  - `KillConfirmed.java` — shared mod state, config lifecycle, registry bindings, cooldown tracker
  - `KillConfirmedConfig.java` — zero-dependency TOML config with hot save & v1.2.0 migration
  - `PendingDogTagHolder.java` — duck interface for pending respawn dog tags
  - `portable/` — pure-Java rules, template engine, `SimpleToml`, `BuiltInFields`, `DeathCooldownTracker`, `DogTagCaseModel`
- `common/src/{v120,v121,v26}/java/dev/rinchan/killconfirmed/`
  - `api/` — `KillConfirmedApi`, `DogTagSnapshot`, `EntityIdentity`, `PlaceholderContext`, `PlaceholderProvider`
  - `DogTagLandmarks.java` — structure-first, biome-fallback death landmark resolver
  - `DogTagSnapshotCodec.java` — schema-1 NBT codec (backwards-compatible with v1.2.0 tags)
  - `PlaceholderResolver.java` — 11 built-in placeholders + external `KillConfirmedApi` providers
  - `DogTagItemFactory.java` & `DogTagLifecycle.java` — death capture, cooldown gating, mcfunction hook, drop/respawn delivery
  - `DogTagItem.java` — right-click inspect with metallic chain chime
  - `DogTagCaseStorage.java`, `DogTagCaseItem.java`, `DogTagCaseMenu.java` — 108-slot case persistence, Bundle interactions, vanilla container menu (zero custom packets via `clickMenuButton` + `ContainerData`)
  - `client/DogTagCaseScreen.java` — 100% vanilla `#C6C6C6` stone dual-section GUI, scrollbar, search bar, auto-pickup & sort micro-buttons, Top-5 casualty stats plaque
  - `client/KillConfirmedConfigScreen.java` — native zero-dependency settings screen
  - `mixin/` — `ServerPlayerMixin`, `PlayerListRespawnMixin`, `ItemEntityMixin`, `ShapedRecipeMixin`, `LootContainerMixin`, `WanderingTraderMixin`
- `fabric/`, `forge/`, `neoforge/` — thin loader entrypoints, isolated `.client` packages, ModMenu compile-time stub, and `MenuScreensMixin` (zero Fabric API)
- `common/src/test/java/dev/rinchan/killconfirmed/portable/` — JUnit 5 test suite (9 test classes including `DogTagCasePropertyTest` and `ArchitectureContractTest`)
- `neoforge/src/testV121/java/dev/rinchan/killconfirmed/client/ScreenLifecycleTest.java` — headless client Screen lifecycle verification (`DogTagCaseScreen` & `KillConfirmedConfigScreen`)
- `neoforge/src/gametest/java/dev/rinchan/killconfirmed/e2e/KillConfirmedPackagedJarGameTests.java` — Packaged-JAR black-box GameTest E2E suite on real Minecraft `1.21.1` NeoForge

## Verification Gates

Install the local `pre-push` hook and run all three verification gates via `./scripts/verify.sh` (native or `--container` using `Dockerfile.verify`):

```bash
./scripts/setup-hooks.sh
./scripts/verify.sh              # Native (or auto-fallback to container)
./scripts/verify.sh --container  # Reproducible Ubuntu 24.04 + Temurin 17/21/25 + xvfb-run
```

Individual gate commands:

1. **Gate 1 — Packaged-JAR E2E GameTest + Client Screen Lifecycle (`1.21.1` NeoForge)**:
   - Stages the real built `kill_confirmed-neoforge-2.0.0+1.21.1.jar` into `run-gametest/mods/` and verifies real `ServerPlayer.die()` 3-line epitaph + `location` landmark, lava/fire `fire_resistant` ticks, anti-farm cooldown, Bundle interactions, `DogTagCaseMenu` `9×6` → `108`-slot scroll/auto-pickup/sort/`quickMoveStack` stress, plus headless `DogTagCaseScreen` & `KillConfirmedConfigScreen` init.
   ```bash
   ./gradlew :neoforge:runGameTestServer -Pminecraft_version=1.21.1 -Ploader=neoforge --no-configuration-cache
   ./gradlew :neoforge:test --tests "dev.rinchan.killconfirmed.client.ScreenLifecycleTest" -Pminecraft_version=1.21.1 -Ploader=neoforge --no-configuration-cache
   ```
2. **Gate 2 — Property Invariant Bombardment (`DogTagCasePropertyTest`)**:
   - Fixed-seed randomized 2,000-step (+ 4×1,000-step + 1,500-iteration boundary fuzz) bombardment of the 108-slot `DogTagCaseModel` asserting strict dog tag count & NBT conservation.
   ```bash
   ./gradlew :neoforge:test --tests "dev.rinchan.killconfirmed.portable.DogTagCasePropertyTest" -Pminecraft_version=1.21.1 -Ploader=neoforge --no-configuration-cache
   ```
3. **Gate 3 — Architecture Contract Guardrails (`ArchitectureContractTest`)**:
   - Bytecode constant-pool audit of real packaged `.jar`s for Dedicated Server client-class isolation, bidirectional `*.mixins.json` ↔ `.jar` ↔ `@Mixin` closure, and zero `rinlib` / `com/terraformersmc/modmenu` stub leaks.
   ```bash
   ./gradlew :neoforge:test --tests "dev.rinchan.killconfirmed.portable.ArchitectureContractTest" -Pminecraft_version=1.21.1 -Ploader=neoforge -Dkillconfirmed.scanAllReleases=true --no-configuration-cache
   ```
