# Kill Confirmed

<img src="docs/icon.png" alt="Kill Confirmed" width="200" height="200">

Kill Confirmed creates a dog tag when a player dies, for records or keepsakes.

![Dog tag with the default information](docs/images/default-dog-tag.png)

![Dog tag with a custom layout and extra information](docs/images/custom-dog-tag.png)

## How it works

- Every real player death creates one dog tag with the player's name, the cause of death, the position and dimension, and the experience level.
- By default the tag drops where you died. Set `drop = "respawn_inventory"` to receive it on respawn instead.
- Each line of the tag's lore is a JSON text component. A line whose value is missing is hidden.
- Tags already created keep their text when the config changes.

## Configuration

The file is `config/kill_confirmed.toml`, created on first launch. Edit it with the game or server stopped, then restart. On a server only the server's file is used.

```toml
drop = "death_position"
player_kills_only = false
function = ""
lore = [
  '{"placeholder":"death_message","color":"gray","italic":false}',
  '{"text":"","color":"gray","italic":false,"extra":[{"placeholder":"x"},{"text":", "},{"placeholder":"y"},{"text":", "},{"placeholder":"z"},{"text":"  "},{"placeholder":"dimension","color":"#808080"}]}',
  '{"translate":"kill_confirmed.lore.level_day","color":"#808080","italic":false,"with":[{"placeholder":"level","color":"#E2C88C"},{"placeholder":"day","color":"#E2C88C"}]}',
]
```

| Key | Default | Effect |
| --- | --- | --- |
| `drop` | `"death_position"` | `death_position` drops the tag where you died; `respawn_inventory` gives it to you on respawn. |
| `player_kills_only` | `false` | When `true`, only deaths caused by another player create a tag. |
| `function` | `""` | Optional function ID. It runs as the dead player at the death position (permission level 2, output silenced) before the tag is built. |
| `lore` | Three lines: death message, coordinates and dimension, level and day | One JSON text component per line. `{"placeholder":"name"}` inserts a value. |

Placeholders for `lore`:

| Placeholder | Value |
| --- | --- |
| `player` | Deceased player's name |
| `killer` | Killer's display name; absent without a killer |
| `weapon` | Killer's main-hand item name; absent with an empty hand or no killer |
| `death_message` | Vanilla death message |
| `x`, `y`, `z` | Integer death coordinates |
| `dimension` | Dimension name |
| `level`, `day` | Experience level and world day |

## Compatibility

- Minecraft 1.21.1, 26.1.2 and 26.2
- Fabric and NeoForge
- RinLib is bundled; no separate installation is needed.

## Download

- [GitHub releases](https://github.com/rinchan-hoshino/kill-confirmed/releases)
