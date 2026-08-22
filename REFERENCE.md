# AbyssFall 内容参考手册

> **`HANDOFF.md` 的配套文件**：那份讲「必须先知道什么才不会做错事」，本文件讲「已经做了什么、有哪些禁忌」。
>
> **读法**：动某个已有功能前先读它那一节。**不要通读。**
>
> **🔴 本文件不再保留「当年为什么这么做」的举例论证**——那些验证都已经做过了（依据见 `HANDOFF.md` 开头的写法约定）。留下的是结论、禁忌、数值、API 事实。看到「勿改 / 别统一 / 刻意如此」就当已验证的既成事实执行；要推翻先自己重做验证并报告。
>
> 章节序号是历史编号，**不要重排**（多处交叉引用）。

## 目录结构

```
src/main/java/com/abyssfall/
├── AbyssFall.java                      主入口：MOD_ID + LOGGER + id(String)
├── advancement/AbyssFallAdvancements.java
├── agreement/AgreementText.java        测试协议文案（双语，硬编码）
├── agreement/TestAgreement.java        preLaunch 入口点，见 16
├── block/AbyssDirtBlock.java  AbyssFallBlocks.java  AbyssFallBoneMealHandler.java
├── config/  (7 个，见 HANDOFF 4)
├── core/    (6 个，见 HANDOFF 3)
├── effect/AbyssExplorerEffect  AbyssFallEffects  SanBreakdownEffect  SanSpiritedEffect
├── item/AbyssFallDevInventory  AbyssFallItemGroups  AbyssFallItems
│        SanCounterItem  SanLensItem
└── loot/AbyssFallLootTables.java
src/client/java/com/abyssfall/client/
├── AbyssFallClient.java
├── hud/AbyssFallSanHud.java          HUD 注册
├── hud/SanHudDispatchElement.java    按模式转发（唯一注册的元素）
├── hud/SanIconHudElement.java        图标行
├── hud/SanBarHudElement.java         进度条
└── mixin/HudStatusBarHeightRegistryImplMixin.java   项目唯一 Mixin，见 15a
```

**`src/main` 下已无 `mixin/` 包**（`WitherRoseBlockMixin` 与 `abyssfall.mixins.json` 在 26.2 迁移时删除，见 4）。

`onInitialize()` 调用顺序**有依赖关系，勿随意调整**：
```java
AbyssFallConfig.load();               // 最先！注册与否取决于配置，注册后无法回头
AbyssFallCoreSystem.initialize();     // San 最先，它是其他一切要移动的值
AbyssFallSanCommand.initialize();     // 条件注册（dev_command）
AbyssFallEffects → Items → Blocks → ItemGroups（依赖前两者）
AbyssFallLootTables.initialize();     // 读配置，必须在 load() 之后
AbyssFallBoneMealHandler.initialize();
AbyssFallDevInventory.initialize();   // 最后，条件注册
```
**`preLaunch` 比这一切都早**：`TestAgreement` 在 Mixin bootstrap 之后、`onInitialize()` 之前几秒执行，读不到配置。

## 1. 创造模式物品栏

`AbyssFallItemGroups`。标题**双色加粗**：「深渊」DARK_GRAY + 「浮现」GRAY。lang key `itemGroup.abyssfall.head` / `.tail`。

**26.2 用 `fabric-creative-tab-api-v1`**：`FabricCreativeModeTab.builder()` 建标签、`CreativeModeTabEvents.modifyOutputEvent(key)` 填内容（回调收到的 `FabricCreativeModeTabOutput` 实现 `CreativeModeTab.Output`，`accept(...)` 写法与旧版一致）。

**🔴 勿破坏：标题以 `Component.empty()` 为根、两半作为 sibling。** 创造界面对标签名执行 `.copy().withStyle(ChatFormatting.BLUE)`、**只替换根组件的 style**，空根让蓝色落在无文字处。**这是不用 Mixin 解决 tooltip 变蓝的办法。**

## 2. 物品：深渊之花 `abyssfall:abyss_flower`

`Rarity.EPIC`（`Rarity` 只有 COMMON/UNCOMMON/RARE/EPIC，无 legendary）。无行为，占位。贴图 `make-item-texture.ps1`（16×16 桃花）。

## 3. 方块：深渊污泥 `abyssfall:abyss_dirt`

`Properties.ofFullCopy(Blocks.DIRT)`，实现 `BonemealableBlock`。材质暂用原版 `minecraft:block/dirt`。tag：`mineable/shovel` + `supports_wither_rose`。

`bloom(ServerLevel, BlockPos dirtPos)` → `boolean`：摧毁上方凋零玫瑰（`destroyBlock(pos, false)` 不掉落原方块）+ `popResource` 吐出深渊之花 + 播放特效，返回是否真的消耗了玫瑰。

## 4. 凋零玫瑰能种在深渊污泥上（26.2 起是数据文件）

`data/minecraft/tags/block/supports_wither_rose.json` 追加 `abyssfall:abyss_dirt`，`"replace": false`（**仍然只做加法**）。26.2 的 `WitherRoseBlock.mayPlaceOn` 就是 `return state.is(BlockTags.SUPPORTS_WITHER_ROSE);`，该 tag 是凋零玫瑰专用（全仓库仅三处引用），**零副作用**。

⚠️ **1.21.11 时用 tag 是不行的**（当时的判断没错）：那时要么硬编码无扩展点、要么 `BlockTags.DIRT` 会连带开放所有植物，用户明确拒绝那个副作用。**别以为当初的 Mixin 是多余的。**

（`HoeItem.TILLABLES` 是硬编码 Map、不查 tag ⇒ 深渊污泥从来不能被锄头耕地，26.2 仍如此。）

## 5. 骨粉催熟交互

`AbyssFallBoneMealHandler` 用 **`ItemEvents.USE_ON`**（fabric-events-interaction-v0）。`FlowerBlock` 不实现 `BonemealableBlock`，vanilla 骨粉点凋零玫瑰无反应，这个事件即可拦截（返回非 null 表示接管、null 交还 vanilla）⇒ **无需 Mixin**。

事件同时接管「点玫瑰」和「点污泥」（位置统一解析到污泥坐标），两条路径行为一致，也避免 vanilla 的 `levelEvent(1505)` 绿色粒子与自定义特效叠加。`stack.causeUseVibration(...)` 仅在 `getPlayer() != null` 时触发（兼容发射器）。

**🔴 这是 mod 唯一获得深渊之花的「制作」途径**——这个事实是成就设计的基础。

## 6. 催熟特效（灵魂主题）

`AbyssDirtBlock.playBloomEffects()`。刻意避开骨粉默认的绿色粒子（这是「献祭」不是「施肥」）。

粒子基准量 `SOUL` 12 / `SCULK_SOUL` 8 / `REVERSE_PORTAL` 20 / `SMOKE` 6。音效 `SOUL_ESCAPE.value()`（0.7 / 音调 0.6）+ `SCULK_CATALYST_BLOOM`（0.5 / 1.4）——一低一高读作「付出 → 到来」。

⚠️ `SOUL_ESCAPE` 是 `Holder.Reference<SoundEvent>` 需 `.value()`，`SCULK_CATALYST_BLOOM` 本身就是 `SoundEvent`。

数量与音量乘 `visuals.*`，**音调不受配置影响**。**骨粉催熟机制本身没有开关**（用户明确说骨粉是核心机制、不给其他 mod 让路，只有特效可调）。

---


## 7. 药水效果：深渊探索者 `abyssfall:abyss_explorer`

`BENEFICIAL`，颜色 `0x9B6BC9`。**纯标记效果，不覆盖任何 tick 方法**，逻辑全在战利品侧。18×18 图标 `make-effect-icon.ps1`。注册用 `Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, ...)`。**全项目没有任何地方给玩家上这个效果**，获取途径尚未设计。

## 7b. 药水效果：精神崩溃 / 精神饱满

| | 精神崩溃 `san_breakdown` | 精神饱满 `san_spirited` |
|---|---|---|
| 类别 / 颜色 | `HARMFUL` / `0x6B4A73` | `BENEFICIAL` / `0x7FD4C8` |
| 中/英 | 精神崩溃 / Mental Breakdown | 精神饱满 / Spirited |
| 每周期 | **扣** San，走 `erode()` | **回** San，走 `addCurrent()` |
| 和平模式 | **被拦住**（用户实测） | **照常生效**（问过用户，明确要求不受限） |

**共同参数**：周期 200 tick（10 秒）；五级 **1% / 2% / 4% / 8% / 12.5%**，`MAX_AMPLIFIER = 4` 封顶（超过按 V 级算，不越界）。

- **🔴 扣/回的是「上限的百分比」不是「当前值的百分比」**（用户确认过的语义，`HANDOFF.md` 教训 23 出自这里）。实测耗时 I=1000s / II=500s / III=250s / IV=130s / V=80s。
- **V 级故意打破倍增**（不是 16 而是 12.5），用户定的。**数值表是数组而非公式**（公式要特判这级，还会藏住意图）。
- **精神饱满不重写数值，直接调 `SanBreakdownEffect.drainFractionFor()`**（周期与封顶级也引用同一常量）⇒ 改一边另一边自动跟着。
- **「无粒子」的实现**：效果**无权**声明自己不可见（由施加者在 `MobEffectInstance` 上决定），但能决定产出哪个粒子 ⇒ 三参构造传入 **alpha=0 的 `ColorParticleOption`**（`SpellParticle.MobEffectProvider` 直接取粒子颜色的 alpha）。**零 Mixin。** 副作用：粒子仍被创建、只是看不见。
- **图标**都是占位（`make-breakdown-icon.ps1` / `make-spirited-icon.ps1`，视觉对偶）。⚠️ 这两个脚本是我自作主张画的，**用户批评过**（`HANDOFF.md` 教训 24）。

**已记录、未实现**：反精神崩溃**魔咒**。用户给的数值——I 减 0.5%、II 减 1.5%、III 减 3%、IV 减 6%、V 减 12%、**VI 级完全豁免**。`drainFractionFor(int)` 特意开成 public，实现时直接读它、别重新推导。**他说了「先记录，不要实现」。**

## 8. 战利品表注入

`AbyssFallLootTables` 用 `LootTableEvents.MODIFY`（`fabric.api.loot.v3`），回调签名 `(key, tableBuilder, source, registries)`。注入哪些表、概率多少来自 `LootSettings`（`HANDOFF.md` 4.5）。默认是 18 张高价值结构宝箱表（沙漠神殿、丛林神庙、末地城宝藏、林地府邸、要塞×3、堡垒残骸×4、沉船宝藏、远古城市、试炼密室 unique×2、下界要塞、掠夺者前哨站、埋藏的宝藏）。

**两个独立池**：①基础概率池（`flower_chance`，默认 5%）；②带「深渊探索者」时**必定**额外给 1 个。

池 2 条件 `LootItemEntityPropertyCondition.hasProperties(EntityTarget.THIS, EntityPredicate.effects(...))`。⚠️ **26.2 搬了家**：`EntityPredicate` → `advancements.predicates.entity`，`MobEffectsPredicate` → `advancements.predicates`（方法未变）。可行依据：`RandomizableContainer.unpackLootTable()` 开箱时 `withParameter(THIS_ENTITY, player)`，所以表知道开箱者是谁。**破坏箱子不走这条路径**，符合「开启宝箱」语义。

**🔴 `isBuiltin()` 门槛已被刻意移除，别加回去。** 用户原话：

> 「我没那么礼貌，我们的mod保留自己的权益的同时尊重他人即可，而不是牺牲自己的权益去尊重他人。」

他同时明确表示 **「保持现状（继续拦）」这个选项以后不要再提**。事实澄清：`LootTableSource` = `VANILLA(true)`/`MOD(true)`/`DATA_PACK(false)`/`REPLACED(false)` ⇒ **其他 mod 自带的表是 `MOD`、`isBuiltin()` 为 true**，那检查从来没拦住别的 mod。我们用 `withPool` **追加独立池**、不编辑既有池、不改任何既有权重。现在遇到非 builtin 来源打 INFO：**冲突可见但不改变行为。**

**未命中的表会 WARN**：配置里写了但整个加载周期从未出现的表 ID，在 `ALL_LOADED` 时逐个 WARN。**只能这样检测**：`Modify` 返回 `void`、`withPool` 无返回值或异常 ⇒ **回调内部不存在「注入失败」这个状态**，真正的失败形态只有「回调从未被调用」。用 `ConcurrentHashMap.newKeySet()` 跟踪（Fabric 自己的 loot 实现注释写明可能跨线程），`ALL_LOADED` 里重填集合以支持 `/reload` 后再检测。

**`target_tables` 不限箱子、不限原版**：`ResourceKey.codec(Registries.LOOT_TABLE)`，任何命名空间的任何表都能写（含钓鱼 `gameplay/fishing/treasure`、猪灵交易、`entities/*`）。

**一个事实**：vanilla 只有 2 张表含真正 EPIC 物品（试炼密室 `reward_ominous_unique` 的沉重核心、远古城市的静默盔甲纹饰模板），**沙漠神殿并不含 EPIC**（附魔金苹果是 `RARE`）⇒ 用户把口径从「所有含 EPIC 的箱子」改为「高价值结构宝箱」。

## 9. 成就系统（3 个，链式）

| 注册名 | 中文 / 英文 | 父节点 | 图标 | 触发 |
|---|---|---|---|---|
| `abyss_fall` | 深渊浮现 / AbyssFall | 无（根，末地背景） | 深渊之花 | 背包有深渊之花 |
| `abyssdirt` | 黏糊糊的烂泥巴 / It's so sticky... | `abyss_fall` | `minecraft:dirt` | 背包有深渊污泥 |
| `abyss_gardeners` | 深渊园艺师 / The Gardener of The Abyss | `abyssdirt` | `minecraft:sunflower` | 见下 |

`abyss_gardeners` 两条 criteria，`requirements` 写成 `[["bloom_wither_rose"], ["obtain_abyss_flower"]]`（**两个独立数组即 AND**）：`bloom_wither_rose` 用 `minecraft:impossible` trigger、由 `AbyssFallAdvancements.awardBloom()` 在催熟成功瞬间显式授予；`obtain_abyss_flower` 是 `inventory_changed` 纯数据。

**🔴 第一条必须用代码授予，勿回退成 `item_used_on_block` + `location_check`**：那个 trigger 在 `stack.useOn()` **返回之后**才触发，那时玫瑰已被摧毁，`ItemUsedOnLocationTrigger` 读的是当前 blockstate（空气），条件必然失败。曾导致成就完全不触发。

**已知结构限制**：用户想要两父汇聚，但 `Advancement` 的 parent 是 `Optional<Identifier>` ⇒ **单亲树，做不到**。已说明、他接受。备选方案 C（`abyss_gardeners` 扩为四条 AND）他暂未采纳，可按需提起。

## 10. 资源与元数据

- `fabric.mod.json`：`icon`、`license: GPL-3.0-or-later`、`minecraft: ~26.2`、`java: >=25`、`fabric-api: >=0.158.0`
- `assets/abyssfall/icon.png`（128×128 桃花，`make-icon.ps1`）
- `textures/gui/sprites/hud/san_{empty,full,half,full_blinking,half_blinking}.png`（9×9，`make-san-icon.ps1`）。**放进这个目录就自动进 GUI 图集**，零注册代码
- lang：`en_us.json` + `zh_cn.json`（**无 BOM 的 UTF-8**）
- `data/abyssfall/loot_table/blocks/abyss_dirt.json`、`data/minecraft/tags/block/mineable/shovel.json`
- **`abyssfall.client.mixins.json`**（`package: com.abyssfall.client.mixin`，`compatibilityLevel: JAVA_25`）——**现在唯一的 mixin 配置**。`injectors.defaultRequire = 1` + `overwrites.requireAnnotations = true`：**注入点找不到会直接崩，这是故意的**（宁可启动失败也不要静默失效）。⚠️ 改完必须验 JSON（`HANDOFF.md` 教训 26）

## 11. 美术脚本（PowerShell + System.Drawing）

`make-icon.ps1`（128×128）、`make-item-texture.ps1`（16×16）、`make-effect-icon.ps1`（18×18）、`make-dev-icon.ps1`（16×16 DEV）、`make-san-icon.ps1`（9×9 ×5，见 15d）、`make-breakdown-icon.ps1` / `make-spirited-icon.ps1`（18×18，**占位**）。均用 `$PSScriptRoot` 相对定位。

⚠️ **本机执行策略禁止直接跑**：必须 `powershell -ExecutionPolicy Bypass -File .\xxx.ps1`。
⚠️ **含中文注释的脚本必须存成带 BOM 的 UTF-8**（只有 `make-san-icon.ps1` 有中文）。
⚠️ **别再为「不分发 Mojang 美术资源」自己画占位图**（`HANDOFF.md` 教训 24）。
**9×9 / 16×16 一律逐像素 `SetPixel`**（任何抗锯齿都会把 1px 笔画糊成灰）；大尺寸才用超采样+双三次缩小。

---


## 12. 开发者物品栏 `abyssfall:abyssfall_dev_inventory`

第二个创造标签，**仅当 `developer.dev_tools = true` 时才注册**，默认 false。（配置键从 `dev_inventory` 改过名，但**注册 ID 没动**——那是存档相关的，别顺手改。）

标题**三色**：「深渊」DARK_GRAY 粗 + 「浮现」GRAY 粗 + 「开发者物品栏」血红 `0xB01030` 粗斜。前两段**复用主标签的 lang key**，第三段是 `itemGroup.abyssfall.dev`。血红用 `TextColor.fromRgb(0xB01030)` 而非 `ChatFormatting.DARK_RED`（创造界面背景偏亮，原版暗红发棕）。en_us 的 `.dev` 值是 `" Dev Inventory"`（**有前导空格**，否则拼成 `AbyssFallDev Inventory`）；中文不需要。

**🔴 里面的物品与标签都不是 `static final`**，而是在 `initialize()` 里创建、用普通 static 字段持有——`static final` 在类被触碰的瞬间就完成注册，开关根本没机会起作用。`getSanCounter()` 在关闭时返回 null（javadoc 说明这是刻意的，项目没有 `@Nullable` 依赖）。

**后果要知道**：关掉开关后，存档里已有的这些物品会在加载时被当作未知物品**丢弃**——这是「真的没注册」的必然结果、不是 bug（用户原话就是「物品也不会被注册」）。若想改成「物品仍存在、只是标签不显示」，那是另一套语义（只把标签注册设为条件性）。

## 13. 理智计数器 `abyssfall:san_counter`

开发者专用 debug 物品，在开发者物品栏，`stacksTo(1)`，图标占位 `minecraft:item/clock_00`。**作用**：主手右键，在生命/饱食度上方显示 `理智值：当前 / 最大`，3 秒后淡出，再按重新计时。

**🔴「3 秒 + 淡出 + 重按续期」全是原版行为，一行计时器都没写。** `Hud.setOverlayMessage` 把 `overlayMessageTime` **无条件**设为 60 ticks（= 3 秒，无条件赋值所以重按即重置），alpha 算式让最后 20 ticks 线性淡出。链路 `ServerPlayer.sendOverlayMessage(c)` → `sendSystemMessage(c, true)` → `ClientboundSystemChatPacket(overlay=true)` → `ChatListener` → `Hud.setOverlayMessage`。⚠️ 26.2 移除了 `displayClientMessage(Component, boolean)`，替代品落点相同，**这套依据继续成立**。

**刻意只在服务端读值**（`player instanceof ServerPlayer`）：客户端那份 attachment 只是镜像，debug 工具必须报告权威值。返回 `InteractionResult.SUCCESS`（`SwingSource.CLIENT`）让挥手动画立刻播放。

## 13b. 认知窥镜 `abyssfall:san_lens`

**玩家向内容，不是 debug 工具**，所以注册在 `AbyssFallItems` / 默认创造栏而非开发者栏。`stacksTo(1)`，图标同为占位 `clock_00`。**作用**：右键在两种 San 读数间切换 + 快捷栏上方提示（机制见 15c）。

**🔴 与理智计数器端相反**：

| | 理智计数器 | 认知窥镜 |
|---|---|---|
| 判定 | `player instanceof ServerPlayer` | **`level.isClientSide()`** |
| 在哪侧干活 | **服务端**（读权威值） | **客户端**（改的是纯屏幕状态） |
| 消息 | `ServerPlayer.sendOverlayMessage` 走系统聊天包 | 本地 `player.sendOverlayMessage`（`LocalPlayer` 覆写为 `chatListener().handleOverlay`） |

⚠️ **必须判 `isClientSide()` 而不是 `instanceof ServerPlayer`**：`use()` 两侧都跑，单人世界两端同进程，不判会切两次、自己抵消。

**lang 键**：`item.abyssfall.san_lens`、`.switched`（一个 `%s`）、`.mode.icons` / `.mode.percent`。模式名是「**具象 / 量化**」（Figurative / Quantified）而非「图标/百分比」——窥镜给的不是另一种界面，而是另一种认知方式。提示语「视界已切换：量化」。

## 14. DEV 图标 `abyssfall:abyss_dev_icon`

**纯工具物品，只为做开发者物品栏的标签图标。** 不命名（保持键值 `item.abyssfall.abyss_dev_icon`）、无行为、**不放入该物品栏的内容中**，`Properties` 全默认。用专门物品而非借用已有工具，是为了让标签图标不随工具外观变化。

**图标** 16×16 纯黑 `DEV`，逐像素 `SetPixel`、无抗锯齿。**V 必须 5 列宽才有单像素尖底**（偶数宽度末行必然两像素 → 平底 → 读作 U）——像素网格的硬约束，不是审美选择。

## 15. HUD 系统

### 15a. 位置系统（两个公共 API + 一个 Mixin）

**API**（`fabric-rendering-v1` 25.3.2）：
- `HudElementRegistry.attachElementAfter(FOOD_BAR, SAN_BAR_ID, element)` → 层序自下而上：**快捷栏 → 饱食度 → 理智值 → 其他 mod**
- `HudStatusBarHeightRegistry.addRight(SAN_BAR_ID, provider)` → 报告占用高度，vanilla 的氧气条、手持物品名、overlay 文本自动上移让位
- 渲染坐标向 `getHeight(SAN_BAR_ID)` **询问**而非固定像素

**🔴 `getHeight(id)` 返回顶边 Y，且求和时遇到自身即返回（不含自己的高度）。正确写法 `guiHeight - getHeight(id)`，零额外偏移。**（26.2 已用字节码复核；官方 javadoc 的代码片段与项目写法一致。这个坑踩过两次，`HANDOFF.md` 教训 16。）

**Mixin `HudStatusBarHeightRegistryImplMixin`（项目唯一的 Mixin，经评估无法避免）**

`@Inject HEAD` 到 `HudStatusBarHeightRegistryImpl.init()`——「所有 mod 注册完毕、`layers` 已填满、顺序尚未被读取」的唯一时刻。职责**只是卡时机**：重排 `FOOD_BAR` 根层的 `layers`，把我们放到 vanilla `food_bar` 之后的第一位，其他 mod 排在我们上面。

**不得不注入的原因**：公共 API 无法表达「永远最后注册」——两个注册表**无任何优先级参数**、都在 `CLIENT_STARTED` 冻结（之后 `addRight` 会抛 `IllegalStateException("Height provider registry already frozen!")`），而我们无法保证自己的 `CLIENT_STARTED` 监听器早于 fabric-rendering-v1 自己的。26.2 已复核缺口仍在。无需 Accessor（`ROOT_ELEMENTS` 是 `public static final`，`RootLayer.layers()` 返回可变 `ArrayList`）。

**✅「总是紧贴饱食度」已实测**（9 种注册顺序、30 项断言、0 失败）⇒ **位置不因任何 mod 的注册顺序而改变。**

⚠️ **这是对 `impl` 包的注入**，仅在 **MC 26.2 / fabric-rendering-v1 25.3.2+515ac5339e** 验证过。**升级时必须重新核实三点**（类头也写了）：`init()` 的签名与调用时机、`ROOT_ELEMENTS` 的类型与访问性、`RootLayer.layers()` 是否仍是 `ArrayList`。`analyze_mixin` 不认 `impl` 类，只能靠 `javap` + 编译通过证明。

### 15b. 图标行（SanIconHudElement）

**26.2 接口是 `extractRenderState(GuiGraphicsExtractor, DeltaTracker)`**（三个元素类都按这签名）。**下面所有参数、阈值、偏移都是 1.21.11 时期原样搬来的，一个数字没动。**

**十个 9×9 图标一行，与原版饱食度并列**，右对齐到 `guiWidth/2 + 91`。参数照 vanilla 饱食度：10 格、`blitSprite(..., 9, 9)`、步距 8、`ROW_WIDTH = 81`、先画 empty 再叠 full/half。

**San 连续 → 20 个半格的量化只存在于这个类里**（core 一无所知）。**任何大于 0 的 San 至少亮半格**（`max(1, round(ratio*20))`）——不能把「快没了」画成「没了」。

| 触发 | 效果 | 参数 |
|---|---|---|
| San ≤ 20% | 持续抖动，越低越快 | 逐图标 `y += random.nextInt(3)-1`，周期 `halves*3+1`，到 0 时每 tick |
| San 下降 | 全排抖一下 | `SHUDDER_TICKS = 4` |
| San 上升 | 从左到右行波 + 慢闪 1 次 | 每格上抬 2px、2 tick/格；闪 5 tick/相 × 1 |
| San 回满 | 快闪 4 次（**取代**慢闪，不排队） | 2 tick/相 × 4 |

抖动与行波照抄 vanilla（饱食度抖动 `tickCount % (foodLevel*3+1)`；行波来自再生效果把心的 Y 减 2，**只位移不改色**）。回满快闪是补的：回满恰是这排即将淡出的时刻，不给信号则「恢复了」和「HUD 被关了」看起来一样。

**变化检测用轮询同步值（`lastSeen` 字段），不用 `SanChangedCallback`**：事件在服务端触发，而动效的正确时机是**客户端看到数值变化的那一刻**。首帧只记录不反应。

**⚠️ 闪光必须用第二套贴图，不能靠代码提亮**（`HANDOFF.md` 教训 20）。vanilla 同样做法（`hud/heart/full_blinking`）。

**可见性**：满 100% 不显示且不占垂直空间，低于 `show_below_percent` 常显，回满后约 1 秒淡出、高度随 alpha 同步收缩。淡出用 `Util.getMillis()`，抖动/行波/闪光用 `player.tickCount`——**后者刻意不用帧计数**（否则 200fps 下抖得比原版快十倍且暂停不停）。

---


### 15c. 两种读数与切换

图标行与进度条都会被画，由玩家用**认知窥镜**（13b）切换。

**🔴 只注册一个元素：`SanHudDispatchElement`**（持有两个实现，`extractRenderState` 与 `occupiedHeight` 都按当前模式转发）。**别改成注册两个**：
- 两个 HUD 注册表在客户端启动完成后**冻结**，元素数量在玩家能按下窥镜之前就定死
- 注册两个 → 布局里有两条图层各自声明高度、各自要知道「对方在显示时我报 0」，多一处能对不上的地方
- **那个 Mixin 只认 `SAN_BAR_ID` 一个 id**，注册两个的话另一条会飘走、不再紧贴饱食度
- 两个 delegate **常驻不重建**（各自持有抖动/行波/闪光/淡出状态，重建会清空 ⇒ 来回切一次就看到冷启动跳变）

**模式状态在 `core/SanHudModeState`（main 源集）**而非 client 包，因为物品是双端代码、够不到 client 源集。它是 `static` 单值：**不做 attachment**（偏好哪种读数是「关于屏幕」不是「关于角色」的事，做成 attachment 会同步、进存档、让服务端有意见，而同世界两个玩家应能各看各的）；**不跨重启保留**（存配置会让每次切换都写盘，还会把显示开关拖进「明确不做热加载」的文件——要保留是另一个决定，别顺手做）。

**切换后强制显示 `REVEAL_MILLIS = 500`**（用户定的；我给 2000 → 1000 → 他改成 500）。实现只有一行，在两个元素的 `alphaFor` 里：

```java
long from = Math.max(this.lastShownAt, SanHudModeState.revealEndsAt());
```

淡出起算点取「San 上次值得显示的时刻」与「reveal 结束时刻」**较晚者** ⇒ reveal 不是「显示完就消失」而是**把淡出推迟**，之后照走原本 1 秒曲线（总可见 ≈1.5 秒）。**`Math.max` 同时解决两个方向**：淡出途中切换能重新显示；reveal 期间 San 掉下去窗口自然延长。**两种模式都有 reveal**（满 San 下切到图标模式同样得让人看见，否则「按了没反应」）。

### 15c-2. 进度条的视觉效果

参数与判定逻辑和 `SanIconHudElement` 逐字一致（阈值 0.3、周期 `halves*3+1`、`SHUDDER_TICKS=4`、闪光 2t×4 / 5t×1、首帧不反应、时钟倒流即清除）。三处**形态决定的**差异：

| 效果 | 图标行 | 进度条 | 原因 |
|---|---|---|---|
| 低 San 抖动 | 逐图标 roll | **整条一个偏移** | 分块 roll 会把 bar 撕开 |
| 回 San 行波 | 逐格上抬扫过 10 格 | **整条上抬 2px，同样持续 20 tick** | 单个形状没有可「扫过」的对象 |
| 高亮闪烁 | 换第二套亮版贴图 | **RGB 往白色插值 45%** | 见下 |

**⚠️ 别把这两处高亮「统一」了**：图标行走 `blitSprite`、tint 是**乘算**只能变暗 ⇒ 必须备亮版贴图；进度条走 `context.fill()`、**颜色直接就是填充色** ⇒ 算个更亮的 RGB 即可。

`HIGHLIGHT_STRENGTH = 0.45F` 而非 1.0（纯白会丢掉紫色、且所有 San 值都高亮成同一个白）。实测 `9B6BC9 → C8ADE1`、`7A1030 → B57B8D`。插值走**平直 sRGB**（和 `fillColor` 一致）不走线性光。**只提亮填充部分，边框与背景保持原色**（一起提亮会糊成一片，反而看不出在闪）。

**元素 ID 仍是 `abyssfall:san_bar`**：Mixin 靠它查图层，两种读数共用这一条。改名等于同时改 Mixin，收益为零。

### 15d. San 图标美术（`make-san-icon.ps1`）

五张 9×9：`san_empty` / `san_full` / `san_half` / `san_full_blinking` / `san_half_blinking`，放在 `assets/abyssfall/textures/gui/sprites/hud/`，**零注册代码**（`DirectoryLister` 自动收进 GUI 图集，玩家也能用资源包替换）。

图案是**用户亲手定稿的**（我出了 A~E 五个方向 + B 的多轮改法，他逐像素指了改法）。主体是一坨正在失去形状、甩出一滴的东西；**滴落那一点故意和主体断开**（连着的 1px 细流会被读成一根针）。

脚本设计成给用户改的：开头是 9 行字符画 `$pattern` + 三张写死的调色板，改完直接跑，还会生成带行列坐标和棋盘格背景的放大预览图（`build/san-icon-preview.png`）。⚠️ **必须存成带 BOM 的 UTF-8**（见 11）。

**⚠️ 半格方向：保留左半、右半透明**（「从右往左掏空」，和一条从左往右缩短的进度条一致）。**别拿 `food_half.png` 反推**（`HANDOFF.md` 教训 21）。

配色：主体 `9B6BC9`、高光 `C4A2E3`、暗部 `6E4A96`、滴落 `4A2E68`；亮版主体 `E4CDF4`；空槽只有纯黑轮廓 + `282828` 内部（这两值从原版 `food_empty.png` 读出）。**轮廓在亮版里不提亮**（全提会让图标失去形状）。

## 16. 测试协议系统

唯一的 `preLaunch` 入口点。用户定位：「算是和玩家的一些小约定，它们二次分发我们的 mod 我们也没办法……咱们这个项目本来就是开源项目。」⇒ 这是**告知机制而非安全措施**，检查可被轻易移除，**这正是设计意图**。

| 环境 | 行为 |
|---|---|
| 开发环境（`isDevelopmentEnvironment()`） | 静默通过，`runClient` 不受干扰 |
| 客户端（有显示） | Swing 对话框：双语文案 + 输入框 + 「复制仓库链接」按钮 |
| 客户端（无头） | WARN 后**降级为服务器行为**（已实测，无 `HeadlessException`） |
| 服务器 | WARN 协议文案 + INFO 说明与链接，**永不阻止启动** |

**接受**：`accept`（不区分大小写、去首尾空格）→ INFO → 继续加载。
**拒绝**（输错/空/取消/关窗）：ERROR → 抛 `RuntimeException`。Loader 的 `handleFormattedException` 会自动记日志 + 弹错误窗 + `System.exit(1)`，**所以不要自己调 `System.exit()`**（会绕过日志和错误窗）。

**三个约束**：
- **文案必须硬编码在 Java 里**（`agreement/AgreementText`）：该阶段无资源管理器、无翻译系统
- **必须是独立类**，不引用任何其他 mod 类（含 `AbyssFall.LOGGER`），否则过早触发静态初始化器——`PreLaunchEntrypoint` javadoc 自己的警告。所以它有自己的 Logger
- **配置系统此时尚未加载**，记不住接受状态。正好符合用户「每次都问」的要求（记住的答案就是不再被阅读的答案）

---


## Git / 发布流程（由你负责）

用户明确授权：**「以后 github 构建都由你来」**。

**仓库**：远端 `https://github.com/Kainy030/AbyssFall-Fabric.git`，分支 `main`（开发）+ `1.21.11`（存档，已冻结）。凭据已缓存（`credential.helper=manager`，push 无需交互），git 用户 Kainy / 1747110555@qq.com。**提交历史、哈希、tag 列表一律现场核实**：`git --no-pager log --oneline -5; git status --short; git --no-pager tag`。

### tag 命名（重要）

`0.1-Dev` 无前缀，`v0.2-Dev` 起**带 `v`**（`v1.1-Dev` 沿用）。workflow 同时接受两种形状，**别简化成只留一种**（旧 tag 还在）：
```yaml
tags:
  - '[0-9]+.[0-9]+*'      # 0.1-Dev
  - 'v[0-9]+.[0-9]+*'     # v0.2-Dev
```
tag 名与 `gradle.properties` 的 `version` **不必一致**（`version=1.1-Dev` 无 `v`，tag 是 `v1.1-Dev`）。

### 三个发布产物

自定义任务 `javadocJar` + `releaseJars`（把三个 jar 按短名汇总到 `build/release/`）。产出 `abyssfall.jar`（来自 **`jar`**）、`abyssfall-doc.jar`、`abyssfall-source.jar`；`build/libs/` 同时保留 Gradle 标准命名版，内容字节一致。

⚠️ **26.2 起不再有 `remapJar` / `remapSourcesJar`**（非重映射 Loom 不注册这两个任务）⇒ **现在 `jar` 产出的就是能直接运行的那个。**

**Javadoc 必须显式设 UTF-8**（`options.encoding`/`docEncoding`/`charSet`）：源码含中文，doclet 默认用平台编码（本机 GBK）会读失败。同时关了 doclint（MC / Fabric API 无外链文档）。

### GitHub Actions

`.github/workflows/release.yml`，**只有 tag push 或手动 `workflow_dispatch` 会触发**（推 `main` 不跑 CI）。action 版本（当时查证的最新版，将来需再查）：`checkout@v7` / `setup-java@v5` / `setup-gradle@v6` / `action-gh-release@v3` / `upload-artifact@v7`。

**三个关键点**：
1. **不加 `--offline`**（CI 空缓存必须联网拉，首次约 3–8 分钟）
2. **直接用 `setup-java` 提供 JDK 25，不靠 Gradle toolchain**（`gradle.properties` 里 `auto-download=false` 且硬编码了本机 JDK 路径，CI 上不存在）。⚠️ 迁移时这里曾错留 JDK 21，**由于只有 tag push 触发，这类错误会静默正确直到下次发布才炸**——已修
3. **`gradlew` 必须是 `100755`**（Windows 建仓时是 `100644`，Linux runner 上会 `Permission denied`）。已用 `git update-index --chmod=+x gradlew` 修好

**已验证的 CI 结果**（v0.5-Dev 那次，SHA256 逐个比对）：`abyssfall.jar` 与 `abyssfall-source.jar` **与本地 clean 构建字节级一致**；`abyssfall-doc.jar` 不一致（Javadoc HTML 内嵌时间戳/JDK 版本，内容等价属推断）。

⚠️ **比对 source jar 前必须先让工作区行尾归一化**：`.gitattributes` 是 `* text=auto eol=lf`，编辑器改过的文件在工作区里仍是 CRLF、而 git 提交时已转 LF ⇒ 哈希必然不同。**这不是仓库问题，别以为构建坏了。** 要复现 CI 哈希：`git rm --cached -r . ; git reset --hard` 后再 `gradlew clean releaseJars`。

**每次 tag push 的 Release workflow 至今全部 `completed success`**（1m25s ~ 1m49s，资产为三个 jar）。**以后自己用 `gh` 查。**

⚠️ 一个命名不一致：`0.1-Dev` 那次的 **Release 标题是 `Fabric-v0.1-Dev`** 而 tag 是 `0.1-Dev`。**查 Release 按 tag 查不会出错，按名字查会。**

### 发布流程

```powershell
git add -A; git commit -m "..."; git push origin main
git tag -a '<版本>' -m 'AbyssFall <版本>'; git push origin '<版本>'   # CI 自动构建并附加三个 jar
```
也可在 Actions 页面手动 `workflow_dispatch` 选任意已有 tag，不必重新打 tag。

**两个坑**：
- push 偶发 `schannel: failed to receive handshake`。**读操作正常、写失败 ⇒ 网络不稳定不是权限问题**，重试即可（曾连失败 3 次、第 4 次成功）。重试循环的 `Start-Sleep` 总时长控制在 30 秒内
- push 成功时 git 把进度写到 stderr，**PowerShell 会因此报非零退出码**。看有没有 `<old>..<new> main -> main` 才是判断依据，**别信退出码**

### gh CLI

`gh` 在 `C:\Program Files\GitHub CLI\gh.exe`（实测 2.98.0）。**当前会话 PATH 里可能没有它**，用全路径。

⚠️ `gh auth login --with-token` 会**拒绝** git 凭据里那个 token（报 `missing required scope 'read:org'`），但 `GH_TOKEN` 环境变量走另一条路径、不做这个校验，**能正常调 API**：

```powershell
$gh='C:\Program Files\GitHub CLI\gh.exe'
$env:GH_TOKEN=(("protocol=https`nhost=github.com`n`n" | git credential fill) |
    Where-Object { $_ -like 'password=*' }) -replace '^password=',''
& $gh run list --limit 5
& $gh release view v1.1-Dev --json assets --jq '.assets[].name'
```

token 从 git 凭据管理器现取，**不要写进任何文件**。`GH_TOKEN` 只在当次 `run_commands` 调用内有效。

### 一个认知点

用户曾疑惑「手动用 tag 构建版本，Assets 里只有 Source code」。**这是正常的**：那两个源码包由 GitHub 按 tag 自动生成、无法删除；编译产物必须上传或用 CI。**「用 tag 创建 Release」只是建了个条目，没有发生任何编译。**
