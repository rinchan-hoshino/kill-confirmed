# Changelog

## 2.0.2

### Fixed
- **1.20.1 Forge `pack.mcmeta`**: Added `pack.mcmeta` (`pack_format: 15`) exclusively to the `1.20.1` Forge JAR to eliminate the client `ResourcePackInfo` startup warning, with strict per-target JAR assertions in `scripts/build-matrix.py`.

### Added
- **Minecraft 26.3 Support**: Added `26.3` (`Fabric` & `NeoForge`, Java 25) to `profiles.json`, expanding the release matrix to 10 targets.

## 2.0.1

### Fixed
- **Dedicated Server Isolation**: Stripped client `Screen` / `Minecraft` constant-pool references from `DogTagCaseItem` so dedicated servers never load or reference client-only UI classes.

## 2.0.0

### Highlights
- **108-Slot Vanilla Dog Tag Case (`kill_confirmed:dog_tag_case`)**:
  - Pure vanilla `#C6C6C6` stone dual-section container GUI (`9×6` visible grid + vertical scrollbar over `9×12` = 108 slots + locked-slot player inventory/hotbar).
  - Compact header search bar with real-time match highlighting/dimming, Auto-Pickup toggle, One-Click Sort (*Newest* / *By Victim* / *By Level*), and Casualty Stats plaque (Top 5 victims; Shift tooltip shows Top 3).
  - Bundle-style right-click quick insert & pop in any inventory screen.
  - Configurable crafting recipe (`enable_dog_tag_case_recipe`), 8% structure chest loot (`enable_case_loot`), and 20-emerald Wandering Trader offer (`enable_wanderer_trade`).
- **Enhanced Dog Tags (`kill_confirmed:dog_tag`)**:
  - Fire & lava resistant by default (`fire_resistant = true`).
  - Anti-farm per-victim cooldown (`cooldown_seconds = 0`).
  - Built-in `location` placeholder detecting death-site structures with automatic biome fallback.
  - Restrained 3-line default epitaph lore (`death_message`, `⌖ location · (x, y, z)`, `✦ Lv.<level> · Day <day>`).
  - Right-click held dog tag inspection with subtle metallic chain sound (`SoundEvents.CHAIN_STEP`).
- **Zero Dependencies & 8-Target Matrix**:
  - Standalone single JAR with zero external dependencies.
  - Expanded build matrix to **8 targets**: `1.20.1` (Fabric, Forge), `1.21.1` (Fabric, NeoForge), `26.1.2` (Fabric, NeoForge), `26.2` (Fabric, NeoForge).
- **Native Config Screen & 4 Languages**:
  - Added zero-dependency `KillConfirmedConfigScreen` integrated with Mod Menu (Fabric), Forge, and NeoForge, hot-saving `config/kill_confirmed.toml`.
  - Cleaned obsolete translation keys and completed `en_us`, `zh_cn`, `zh_tw`, and `ja_jp` localizations.
- **Refreshed Artwork**:
  - Adopted refined `16×16` item textures for `dog_tag` and `dog_tag_case`.
  - Generated new `64×64` -> `512×512` military-steel riveted badge `icon.png` via `scripts/draw_icon.py`.

## 1.2.0
- Unified TOML configuration and native lore component templates.

## 1.1.0
- Unified supported versions and fixed dog tag callback execution.

## 1.0.0
- Initial release.
