# Kill Confirmed

<p align="center">
  <img src="docs/icon.png" alt="Kill Confirmed" width="128" />
</p>

**Kill Confirmed** mints a fire-resistant **Dog Tag** (`kill_confirmed:dog_tag`) whenever a player falls, and includes a **108-slot vanilla Dog Tag Case** (`kill_confirmed:dog_tag_case`) to collect, search, sort, and inspect your trophies.

Zero library dependencies — no RinLib, no Fabric API required.

---

## Features

### Dog Tag (`kill_confirmed:dog_tag`)
- **Fire & Lava Resistant by Default** (`fire_resistant = true`): metal dog tags survive lava pools and fire deaths.
- **Restrained 3-Line Epitaph Lore**:
  1. Death narrative (`death_message`, `#CCCCCC`, non-italic)
  2. Automatic landmark & coordinates (`⌖ <structure or biome> · (x, y, z)`)
  3. Experience level & world day (`✦ Lv.<level> · Day <day>`)
- **Built-in Landmark Detection (`location`)**: detects the structure at the death site (e.g. Ancient City, Nether Fortress) and falls back to the biome name automatically.
- **Right-Click Inspection**: right-clicking a held dog tag plays a subtle metallic chain chime and prints its full epitaph to chat.
- **Anti-Farm Cooldown**: configurable per-player cooldown (`cooldown_seconds`, default `0`).
- **Extensible Template & Java API**: supports all 11 built-in placeholders (`player`, `killer`, `weapon`, `death_message`, `location`, `x`, `y`, `z`, `dimension`, `level`, `day`), optional `mcfunction` hooks, and `KillConfirmedApi.registerPlaceholder(...)`.

### Dog Tag Case (`kill_confirmed:dog_tag_case`)
- **Pure Vanilla Dual-Section Container GUI**: standard `#C6C6C6` stone container with a `9×6` visible grid, vanilla Creative/Stonecutter-style vertical scrollbar over **108 slots (`9×12`)**, and standard player inventory + hotbar below (with the open case slot locked).
- **Full Native Slot Controls**: left-click grab/place, right-click split, Shift+left-click `quickMoveStack` across all 108 slots, number keys `1–9` swap, and native dog tag tooltips.
- **Lightweight Vanilla Micro-Widgets**:
  - Compact recessed search bar that highlights matching tags and dims non-matches in real time.
  - **Auto-Pickup Toggle**: automatically routes picked-up ground dog tags straight into your carried case with `BUNDLE_INSERT` feedback.
  - **One-Click Sort**: cycles between *Newest*, *By Victim*, and *By Level* and compacts the case.
  - **Casualty Stats Plaque**: hover to view `Stored: N / 108` and the Top 5 most frequent victims (holding `Shift` on the item tooltip expands the Top 3).
- **Bundle-Style Quick Insert/Extract**: right-click a dog tag onto a case (or a case onto a dog tag) in any inventory screen to insert, or right-click an empty slot to pop the last tag.
- **Acquisition (All Configurable)**:
  - Crafting recipe: `name_tag` center, `iron_ingot` top/bottom, `leather` left/right, `iron_nugget` corners.
  - Rare structure chest loot (`8%`) in Ancient Cities, Strongholds, Bastion Remnants, Mineshafts, and Woodland Mansions.
  - Rare Wandering Trader offer (`20` emeralds).

---

## Supported Matrix (8 Targets)

| Minecraft | Loaders | Java |
| --- | --- | --- |
| `1.20.1` | Fabric, Forge | 17 |
| `1.21.1` | Fabric, NeoForge | 21 |
| `26.1.2` | Fabric, NeoForge | 25 |
| `26.2` | Fabric, NeoForge | 25 |

---

## Configuration

Stored at `config/kill_confirmed.toml` and editable in-game via the native **Kill Confirmed Settings** screen (Mod Menu on Fabric, Mods menu on Forge / NeoForge):

```toml
drop = "death_position"           # "death_position" or "respawn_inventory"
player_kills_only = false
fire_resistant = true
cooldown_seconds = 0
enable_dog_tag_case_recipe = true
enable_case_loot = true
enable_wanderer_trade = true
function = ""
```

---

## Building from Source

```bash
python3 scripts/build-matrix.py
```

All 8 verified JARs and `manifest.json` are written to `build/releases/`.

## License

[GPL-3.0-only](LICENSE)
