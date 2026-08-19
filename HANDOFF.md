# AbyssFall 项目交接提示词

> 把下面全部内容作为新对话的第一条消息发出即可。

---

## 你的角色与协作方式

你接手一个 Minecraft 1.21.11 Fabric 模组项目「AbyssFall」。用户（Kainy）与你的分工已经磨合成形，请严格延续：

**用户负责**：提供设计思路、大方向、功能需求。
**你负责**：让想法运作起来。具体实现自由度很高。

**用户明确授予你的自主权**（不必询问，改完告知即可）：
- 颜色、粒子、音效等视觉/听觉细节。若某配色看不清或效果不好，直接换掉。
- 语言文件翻译。若用户的翻译有语法错误直接改正；同时必须避免机翻感和僵硬表达。

**验证强度约定**（用户明确要求，为省 token）：
- 用户自己跑 `runClient`，他跑得更快且能拿到更多信息。**不要自己跑 runClient**。
- 你需要 runClient 确认什么，就明确列出待确认项让用户去测。
- 大改动才跑 `gradlew build`；小改动只做代码验证（读文件确认改动正确 + MCP 核实 API），**不构建**。
- 回复尽量简洁，省 token。

**用户背景**：以前写游戏外挂，遇到问题的第一直觉是「不用 API，用注入解决」。他已认可你的判断顺序并要求延续：

> **自有数据结构 → Fabric API/事件 → Mixin**

只有前两者做不到时才注入，且注入前必须用 MCP 核实目标版本的精确签名。选择注入时要说明为什么必须注入。

**语言**：中文（zh-CN）回复。

---

## 项目基本信息（已验证）

| 项 | 值 |
|---|---|
| 路径 | `D:/MC1.21.11-AbyssFall-Fabric` |
| Minecraft | 1.21.11 |
| **映射** | **Mojang 官方映射（mojmap）**，不是 Yarn |
| Fabric Loader | 0.19.3 |
| Loom | 1.17.19（插件 id 是 `net.fabricmc.fabric-loom-remap`） |
| Fabric API | 0.141.6+1.21.11 |
| Gradle | 9.7.0 |
| modid / 包名 | `abyssfall` / `com.abyssfall` |
| 版本 | 0.1-Dev |
| 许可 | GPL-3.0-or-later（所有 .java 带 GPL 版权头） |
| JDK | Gradle 跑 JDK 25，编译 toolchain JDK 21 + `release = 21` |
| 源集 | `splitEnvironmentSourceSets()`：`src/main` + `src/client` |
| Git | **未初始化**（用户说现阶段无意义） |

**重要**：因为用 mojmap，类名是 `net.minecraft.world.item.Item`、`net.minecraft.resources.Identifier`（1.21.11 已从 `ResourceLocation` 改名）这类 Mojang 名，**不要写 Yarn 名**。

**常用命令**：
```powershell
cd D:/MC1.21.11-AbyssFall-Fabric
.\gradlew.bat build --console=plain --offline
.\gradlew.bat compileJava --console=plain --offline --rerun-tasks   # 强制重编译
```
注意：`build` 偶尔超过 30 秒被工具截断，此时重跑一次看 `UP-TO-DATE` 判断上次是否已成功。


---

## 已实现的内容

### 目录结构
```
src/main/java/com/abyssfall/
├── AbyssFall.java                      主入口，MOD_ID + LOGGER + id(String) 工具方法
├── advancement/AbyssFallAdvancements.java
├── block/AbyssDirtBlock.java
├── block/AbyssFallBlocks.java
├── block/AbyssFallBoneMealHandler.java
├── effect/AbyssExplorerEffect.java
├── effect/AbyssFallEffects.java
├── item/AbyssFallItemGroups.java
├── item/AbyssFallItems.java
├── loot/AbyssFallLootTables.java
└── mixin/WitherRoseBlockMixin.java
src/client/java/com/abyssfall/client/AbyssFallClient.java   （空实现）
```

`AbyssFall.onInitialize()` 的调用顺序（有依赖关系，勿随意调整）：
```java
AbyssFallEffects.initialize();
AbyssFallItems.initialize();
AbyssFallBlocks.initialize();
AbyssFallItemGroups.initialize();     // 依赖 Items 和 Blocks
AbyssFallLootTables.initialize();
AbyssFallBoneMealHandler.initialize();
```

### 1. 创造模式物品栏
`AbyssFallItemGroups`。标题是**双色加粗**：「深渊」DARK_GRAY + 「浮现」GRAY。

**关键技巧（勿破坏）**：标题以 `Component.empty()` 为根、两半都作为 sibling。原因是 `CreativeModeInventoryScreen:710` 对标签名执行 `.copy().withStyle(ChatFormatting.BLUE)`，只替换**根组件**的 style。空根让蓝色落在无文字处，两个 sibling 的显式颜色按 `Style.applyTo` 规则（子级非 null 字段优先）照常生效。这是不用 Mixin 解决 tooltip 变蓝的办法。

lang key：`itemGroup.abyssfall.head` / `.tail`。

### 2. 物品：深渊之花 `abyssfall:abyss_flower`
`Rarity.EPIC`（1.21.11 原版最高级，无 legendary——`Rarity` 枚举只有 COMMON/UNCOMMON/RARE/EPIC）。无行为，占位。贴图由 `make-item-texture.ps1` 生成（16×16 桃花）。

### 3. 方块：深渊污泥 `abyssfall:abyss_dirt`
`BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)`，实现 `BonemealableBlock`。

核心方法 `AbyssDirtBlock.bloom(ServerLevel, BlockPos dirtPos)` → `boolean`：摧毁上方凋零玫瑰（`destroyBlock(pos, false)` 不掉落原方块）+ `popResource` 吐出深渊之花 + 播放特效。返回是否真的消耗了玫瑰。

材质暂用原版 `minecraft:block/dirt`。tag 只加了 `mineable/shovel`。

### 4. Mixin：让凋零玫瑰能种在深渊污泥上
`WitherRoseBlockMixin` 注入 `WitherRoseBlock.mayPlaceOn`，`@At("RETURN")` + `cancellable`。

**为什么必须用 Mixin**（用户已认可这个判断）：`WitherRoseBlock.mayPlaceOn` 是
```java
super.mayPlaceOn(...) || is(NETHERRACK) || is(SOUL_SAND) || is(SOUL_SOIL)
```
后三个是硬编码方块判断，无扩展点。`super`（`VegetationBlock`）用的是 `BlockTags.DIRT`，但加入该 tag 会连带开放**所有**植物、甘蔗、瓜藤、苔藓替换——用户明确拒绝这个副作用。

Mixin 逻辑刻意**只做加法**（只把 false 翻成 true，从不反向覆盖），保证 vanilla 规则和其他 mod 的结果都不被破坏。

（题外话：`HoeItem.TILLABLES` 是 `Map<Block,...>` 硬编码白名单，不查 tag，所以深渊污泥从来都不能被锄头耕地。）

### 5. 骨粉催熟交互
`AbyssFallBoneMealHandler` 用 **`ItemEvents.USE_ON`**（fabric-events-interaction-v0 4.1.1）。

**为什么不用 Mixin**：`FlowerBlock` 不实现 `BonemealableBlock`，vanilla 骨粉点凋零玫瑰完全无反应。用这个事件即可拦截，返回非 null 表示接管、返回 null 交还 vanilla。

事件同时接管「点玫瑰」和「点污泥」两种点击（把点击位置统一解析到污泥坐标），这样两条路径行为一致，也避免 vanilla 的 `levelEvent(1505)` 绿色粒子和自定义特效叠在一起。

**这是 mod 唯一获得深渊之花的「制作」途径。原版无任何办法用骨粉催熟凋零玫瑰，所以这是唯一路径**——这个事实是成就设计的基础。

### 6. 催熟特效（灵魂主题）
`AbyssDirtBlock.playBloomEffects()`。刻意避开骨粉默认的绿色欢快粒子，因为这是「献祭」而非「施肥」：

| 粒子 | 数量 |
|---|---|
| `SOUL` | 12 |
| `SCULK_SOUL` | 8 |
| `REVERSE_PORTAL` | 20 |
| `SMOKE` | 6 |

音效：`SOUL_ESCAPE.value()`（音量 0.7 / 音调 0.6）+ `SCULK_CATALYST_BLOOM`（0.5 / 1.4）。一低一高，读作「付出 → 到来」。

注意 `SOUL_ESCAPE` 是 `Holder.Reference<SoundEvent>` 需 `.value()`，`SCULK_CATALYST_BLOOM` 本身是 `SoundEvent`。

### 7. 药水效果：深渊探索者 `abyssfall:abyss_explorer`
`MobEffectCategory.BENEFICIAL`，颜色 `0x9B6BC9`。**纯标记效果，不覆盖任何 tick 方法**，逻辑全在战利品侧。18×18 图标由 `make-effect-icon.ps1` 生成。

注册用 `Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, ...)`。

### 8. 战利品表注入
`AbyssFallLootTables` 用 `LootTableEvents.MODIFY`（`net.fabricmc.fabric.api.loot.v3`），回调签名 `(key, tableBuilder, source, registries)`，带 `source.isBuiltin()` 检查（尊重数据包作者的重写意图）。

覆盖 18 张高价值结构宝箱表（沙漠神殿、丛林神庙、末地城宝藏、林地府邸、要塞×3、堡垒残骸×4、沉船宝藏、远古城市、试炼密室 unique×2、下界要塞、掠夺者前哨站、埋藏的宝藏）。

**两个独立池**：
1. 基础概率：空条目权重 19 + 花权重 1 ≈ 5%
2. 带「深渊探索者」效果时**必定**额外给 1 个

第 2 个池的条件是 `LootItemEntityPropertyCondition.hasProperties(EntityTarget.THIS, EntityPredicate.effects(...))`。**能这样做的依据**：`RandomizableContainer.unpackLootTable()` 在玩家开箱时会 `withParameter(LootContextParams.THIS_ENTITY, player)`，所以战利品表知道开箱者是谁、身上有什么效果。破坏箱子不走这条路径，符合「开启宝箱」语义。

**重要事实**：用户最初要求「加入所有含 EPIC 物品的原版箱子」，但实测 vanilla 只有 2 张箱子表含真正 EPIC 物品（试炼密室 `reward_ominous_unique` 的沉重核心、远古城市的静默盔甲纹饰模板），**沙漠神殿并不含 EPIC**（附魔金苹果是 `RARE`）。用户据此改为「高价值结构宝箱」口径。

### 9. 成就系统（3 个，链式）

| 注册名 | 中文名 | 英文名 | 父节点 | 图标 | 触发 |
|---|---|---|---|---|---|
| `abyss_fall` | 深渊浮现 | AbyssFall | 无（根，末地背景） | 深渊之花 | 背包有深渊之花 |
| `abyssdirt` | 黏糊糊的烂泥巴 | It's so sticky... | `abyss_fall` | `minecraft:dirt` | 背包有深渊污泥 |
| `abyss_gardeners` | 深渊园艺师 | The Gardener of The Abyss | `abyssdirt` | `minecraft:sunflower` | 见下 |

`abyss_gardeners` 有两条 criteria，`requirements` 写成 `[["bloom_wither_rose"], ["obtain_abyss_flower"]]` —— **两个独立数组即 AND**，必须都满足：
- `bloom_wither_rose`：`minecraft:impossible` trigger，由 `AbyssFallAdvancements.awardBloom()` 在催熟成功瞬间显式授予
- `obtain_abyss_flower`：`inventory_changed`，纯数据

**为什么第一条必须用代码授予（踩过的坑，勿回退）**：曾试过 `item_used_on_block` + `location_check`，但 `ServerPlayerGameMode:375-378` 显示该 trigger 在 `stack.useOn()` **返回之后**才触发，而那时玫瑰已被摧毁，`ItemUsedOnLocationTrigger` 内部读取的是**当前** blockstate（空气），条件必然失败。这个 bug 曾导致成就完全不触发。

**已知结构限制**：用户想要 `abyss_fall + abyssdirt = abyss_gardeners`（两个父节点汇聚），但 `Advancement` 的 parent 字段类型是 `Optional<Identifier>` —— **单亲树，做不到多父节点**。故实现为链式。已向用户说明，用户接受。曾提出的备选方案 C（把 `abyss_gardeners` 的 criteria 扩为四条 AND，逻辑上真正要求前两个成就的条件都达成）用户暂未采纳，可按需提起。

### 10. 资源与元数据
- `fabric.mod.json`：含 `icon`、`license: GPL-3.0-or-later`、`fabric-api: >=0.141.6`
- `assets/abyssfall/icon.png`（128×128，桃花，由 `make-icon.ps1` 生成）
- lang：`en_us.json` + `zh_cn.json`（**无 BOM 的 UTF-8**）
- `data/abyssfall/loot_table/blocks/abyss_dirt.json`（方块掉落自身）
- `data/minecraft/tags/block/mineable/shovel.json`
- `abyssfall.mixins.json` 已登记 `WitherRoseBlockMixin`；`abyssfall.client.mixins.json` 的 `client` 数组仍为空

### 11. 美术脚本（PowerShell + System.Drawing）
`make-icon.ps1`（128×128 mod 图标）、`make-item-texture.ps1`（16×16 物品贴图）、`make-effect-icon.ps1`（18×18 效果图标）。均用 `$PSScriptRoot` 相对定位，直接 `powershell -NoProfile -ExecutionPolicy Bypass -File .\xxx.ps1` 运行。


---

## 血泪教训（务必避免重犯）

1. **路径必须用项目实际 vanilla jar 验证**，不能只信官方参考仓库。方块 tag 的正确路径是 `data/minecraft/tags/block/dirt.json`（带 registry 子目录），而 fabric-docs 参考里有个无 `block/` 的旧格式遗留文件会误导人。验证方法：
```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z=[System.IO.Compression.ZipFile]::OpenRead('run\.fabric\remappedJars\minecraft-1.21.11-0.19.3\client-intermediary.jar')
$z.Entries | Select-Object -ExpandProperty FullName | Where-Object { $_ -match 'tags/.*dirt' }
$z.Dispose()
```
1.21.11 的数据目录都是**单数**：`advancement/`、`loot_table/`、`tags/block/`。

2. **不要凭记忆断言 vanilla 行为**。我曾断言「加 dirt tag 后锄头能耕地」，实际 `HoeItem.TILLABLES` 是硬编码 Map，根本不查 tag。被用户要求查证后才发现是错的。**能验证就验证，不能验证就说不知道。**

3. **advancement 背景只有 5 个**：`husbandry` / `end` / `nether` / `stone` / `adventure`。我曾编了个不存在的 `soul_sandstone`。

4. **`ServerPlayer` 没有 `getServer()`**，用 `player.level().getServer()`。

5. **PowerShell 控制台显示中文乱码是编码显示问题**，不代表文件坏了。用 `[System.Text.Encoding]::UTF8.GetString([System.IO.File]::ReadAllBytes(path))` 验证真实内容。

6. **不要过度设计**。用户曾指出「你做的有点复杂了」——当时我为一个成就造了多余的 root 节点和代码授予层，而实际上两条件 AND 就够了。先想最简方案。

---

## 当前状态

- 最后一次 `gradlew build`：**BUILD SUCCESSFUL**（已验证）
- 所有功能用户已在 runClient 中**实测通过**：创造标签双色标题、tooltip 不再变蓝、深渊之花 EPIC 紫色、宝箱掉落、药水效果必定掉落、村民箱子不掉落、玫瑰可种深渊污泥、其他作物不可种、骨粉催熟出花且玫瑰被消耗、灵魂特效、三个成就正常触发
- 产物：`build/libs/abyssfall-0.1-Dev.jar`

## 未完成 / 可能的下一步

- 深渊污泥仍用原版泥土材质（用户说「暂时」，需要专属材质时会提）
- `abyss_gardeners` 图标是向日葵占位（用户说「暂时替换」）
- `abyssdirt` 成就图标是原版泥土（同上）
- 深渊之花无实际功能（纯注册占位）
- 「深渊探索者」效果目前只影响战利品表，无其他作用
- client mixin 配置为空，尚无客户端逻辑
- 无配方、无数据生成（datagen）、无自定义音效资源
- 未 git init

## 开工前请做

1. 读 `gradle.properties`、`fabric.mod.json`、`AbyssFall.java` 确认状态与本文档一致
2. 用 `Fabric-Knowledge` MCP 查 1.21.11 官方参考（`get_fabric_context` 返回 `version_match_only`，其 Fabric API 是 0.141.1 与本项目 0.141.6 略有差异，属正常）
3. 用 `minecraft-dev` MCP（`mapping: "mojmap"`, `version: "1.21.11"`）核实所有类/方法/字段签名，**不要凭记忆**
4. 涉及 Mixin 时用 `analyze_mixin` 校验

