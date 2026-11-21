# 更新日志

## 2.0.2

- **修复 `1.20.1` Forge `pack.mcmeta`**：仅在 `1.20.1` Forge JAR 中内置 `pack_format: 15` 的 `pack.mcmeta`，消除客户端启动 `ResourcePackInfo` 警告，并在 `scripts/build-matrix.py` 增加构建产物硬断言。
- **新增 Minecraft `26.3` 支持**：新增 `26.3`（`Fabric` 与 `NeoForge`，Java 25）构建目标，扩展至 10 目标全量发布矩阵。

## 1.2.0

- 配置合并为带注释的 `config/kill_confirmed.toml`，统一设置掉落方式、仅玩家击杀、可选函数和 lore。旧 JSON 启动警告一次并忽略，不自动迁移。
- 删除数据包函数标签、scratch storage 进出协议和 `when` 条件，改为可选 function 配合以死者身份解析的原版 score、nbt、selector 文本组件。
- 新狗牌使用金色无斜体本地化名称；默认三行文本为原版死亡信息、坐标与维度、等级与天数。
- 新增 `day`、`weapon` 占位符，内建字段使用短名称，坐标拆为 x/y/z。任一值缺失或解析失败均隐藏整行，解析失败按行号只警告一次。
- 内置 RinLib 2.0，保留旧狗牌快照、待发玩家存档、客户端协议和右键行为。

更早版本见 [English changelog](CHANGELOG.md)。
