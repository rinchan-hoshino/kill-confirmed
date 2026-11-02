# 狗牌自定义

## 配置

首次启动生成 `config/kill_confirmed.toml`。关闭游戏或服务器后修改，重启生效。多人游戏只读服务端的配置。修改只影响新狗牌，已有狗牌保留生成时的文本。

```toml
# Where the dog tag goes: "death_position" (drops where you died) or "respawn_inventory" (given to you on respawn)
drop = "death_position"
# Only create a dog tag when another player is the killer
player_kills_only = false
# Optional mcfunction id, run as the dead player at the death position (permission level 2, output silenced) right before the tag is built.
# Use it to fill scoreboards/storage, then read them in lore with vanilla "score"/"nbt" text components.
function = ""
# One entry per lore line, each a JSON text component. {"placeholder":"<name>"} inserts a value.
# A line that uses a placeholder with no value (e.g. killer on an environmental death) is hidden.
lore = [
  '{"placeholder":"death_message","color":"gray","italic":false}',
  '{"text":"","color":"gray","italic":false,"extra":[{"placeholder":"x"},{"text":", "},{"placeholder":"y"},{"text":", "},{"placeholder":"z"},{"text":"  "},{"placeholder":"dimension","color":"#808080"}]}',
  '{"translate":"kill_confirmed.lore.level_day","color":"#808080","italic":false,"with":[{"placeholder":"level","color":"#E2C88C"},{"placeholder":"day","color":"#E2C88C"}]}',
]
```

`drop` 可选 `death_position`（默认，死亡原地掉落）和 `respawn_inventory`（复活后放入物品栏）。`player_kills_only = true` 时，只在被另一名玩家杀死时生成狗牌。原地掉落的狗牌遵循普通物品物理规则，可能被岩浆或虚空销毁；需要避开这些风险时可选 `respawn_inventory`。

`lore` 中每个字符串是一行原版 JSON 文本组件。`{"placeholder":"name"}` 会递归插入组件，可放在 `extra`、`with` 等位置；占位符对象上的样式覆盖插入值的同名样式。任一占位符缺值时隐藏整行。JSON 或组件解析失败也隐藏该行，并按行号只警告一次。

| 占位符 | 内容 |
| --- | --- |
| `player` | 死者名 |
| `killer` | 凶手显示名；没有凶手时缺失 |
| `weapon` | 凶手主手物品名；空手或没有凶手时缺失 |
| `death_message` | 保留原样的原版死亡信息组件 |
| `x`、`y`、`z` | 死亡方块坐标整数 |
| `dimension` | 本地化维度名，没有翻译时显示维度 ID |
| `level` | 死亡时经验等级 |
| `day` | 世界天数：`level.getDayTime() / 24000 + 1` |

原版 `score`、`nbt`、`selector` 组件以死者身份、死亡位置解析。可选 `function` 在构建狗牌前运行，权限等级为 2，输出静默；由另一个函数触发死亡时也会先执行完。旧 JSON 配置与数据包占位符协议已移除；发现旧配置时启动警告一次，不自动迁移。

### 用 function 加自定义行

以 Minecraft 1.21.1 为例，新建 `world/datapacks/tag_stats/pack.mcmeta`：

```json
{"pack":{"pack_format":34,"description":"Dog tag statistics"}}
```

新建 `world/datapacks/tag_stats/data/example/function/dog_tag.mcfunction`：

```mcfunction
scoreboard players set @s kc_kills 7
```

先执行一次 `/scoreboard objectives add kc_kills dummy`，再执行 `/reload`。将 TOML 中的 `function` 改为 `"example:dog_tag"`，并在 `lore` 数组中追加：

```toml
  '{"text":"击杀数：","color":"#808080","italic":false,"extra":[{"score":{"name":"@s","objective":"kc_kills"},"color":"#E2C88C"}]}'
```

重启后，新狗牌多一行「击杀数：7」。函数中的 `7` 可以替换成你需要的计算。26.x 使用对应 Minecraft 版本的数据包格式，见 `profiles.json`。仓库附有[完整配置与数据包](examples/showcase/)。
