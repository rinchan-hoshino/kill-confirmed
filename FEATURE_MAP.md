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
- `fabric/`, `forge/`, `neoforge/` — thin loader entrypoints, ModMenu compile-time stub, and `MenuScreensMixin` (zero Fabric API)
- `common/src/test/java/dev/rinchan/killconfirmed/portable/` — JUnit 5 test suite (7 test classes)
