# AbyssFall 内容参考手册

> **这是 `HANDOFF.md` 的配套文件，不是独立文档。**
>
> 分工：`HANDOFF.md` 讲「你必须先知道什么才不会做错事」——角色约定、两块地基（San / 配置）、发布流程、血泪教训、当前状态。
> 本文件讲「已经做了什么、当初为什么这么做」——逐个功能的实现细节与设计依据。
>
> **什么时候读这里**：要动某个已有功能之前，先在这里找它那一节，读完再动手。不要通读。
>
> 维护规矩与 `HANDOFF.md` 相同：每条要能说清「已验证 / 未验证 / 推断」；会过时的东西标注获取方法而不是写死；
> 改之前先在项目里核一遍，别凭印象判断文档写错了。

## 已实现的内容与机制

### 目录结构
```
src/main/java/com/abyssfall/
├── AbyssFall.java                      主入口，MOD_ID + LOGGER + id(String) 工具方法
├── advancement/AbyssFallAdvancements.java
├── agreement/AgreementText.java        ← 测试协议文案（双语，硬编码），第四次交接新增
├── agreement/TestAgreement.java        ← preLaunch 入口点，见「测试协议系统」一节
├── block/AbyssDirtBlock.java
├── block/AbyssFallBlocks.java
├── block/AbyssFallBoneMealHandler.java
├── config/AbyssFallConfig.java         ← 配置加载/保存，见 HANDOFF 「配置系统」
├── config/AbyssFallConfigData.java     ← 配置根记录
├── config/DeveloperSettings.java
├── config/HudSettings.java             ← 第四次交接新增
├── config/LootSettings.java
├── config/VisualSettings.java
├── core/AbyssFallCoreSystem.java       ← San 系统，见 HANDOFF 「核心系统：San 值」
├── core/AbyssFallSanCommand.java
├── core/SanChangedCallback.java
├── core/SanState.java
├── effect/AbyssExplorerEffect.java
├── effect/AbyssFallEffects.java
├── item/AbyssFallDevInventory.java     ← 开发者物品栏（条件注册）
├── item/AbyssFallItemGroups.java
├── item/AbyssFallItems.java
├── item/SanCounterItem.java            ← 理智计数器（debug 工具）
├── loot/AbyssFallLootTables.java
└── mixin/WitherRoseBlockMixin.java
src/client/java/com/abyssfall/client/
├── AbyssFallClient.java                                ← 不再是空实现
├── hud/AbyssFallSanHud.java                            ← San HUD 注册
├── hud/SanIconHudElement.java                          ← 图标行渲染（当前使用的 HUD）
├── hud/SanBarHudElement.java                           ← 进度条渲染，**保留不注册**，留给以后的道具
└── mixin/HudStatusBarHeightRegistryImplMixin.java      ← 第二个 Mixin，见下
```

`AbyssFall.onInitialize()` 的调用顺序（有依赖关系，勿随意调整）：
```java
AbyssFallConfig.load();               // 最先！注册与否取决于配置，注册后无法回头
AbyssFallCoreSystem.initialize();     // San 最先，它是其他一切要移动的值
AbyssFallSanCommand.initialize();      // 条件注册（dev_command）
AbyssFallEffects.initialize();
AbyssFallItems.initialize();
AbyssFallBlocks.initialize();
AbyssFallItemGroups.initialize();     // 依赖 Items 和 Blocks
AbyssFallLootTables.initialize();     // 读配置，必须在 load() 之后
AbyssFallBoneMealHandler.initialize();
AbyssFallDevInventory.initialize();   // 最后，条件注册
```

**`preLaunch` 比这一切都早**：`agreement/TestAgreement` 是 `preLaunch` 入口点，在 Mixin bootstrap 之后、`onInitialize()` 之前**几秒**执行。它不属于上面的顺序，也读不到配置（`AbyssFallConfig.load()` 还没跑）。见「测试协议系统」一节。

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
`AbyssFallBoneMealHandler` 用 **`ItemEvents.USE_ON`**（fabric-events-interaction-v0）。

**为什么不用 Mixin**：`FlowerBlock` 不实现 `BonemealableBlock`，vanilla 骨粉点凋零玫瑰完全无反应。用这个事件即可拦截，返回非 null 表示接管、返回 null 交还 vanilla。

事件同时接管「点玫瑰」和「点污泥」两种点击（把点击位置统一解析到污泥坐标），这样两条路径行为一致，也避免 vanilla 的 `levelEvent(1505)` 绿色粒子和自定义特效叠在一起。

`stack.causeUseVibration(...)` 仅在 `context.getPlayer() != null` 时触发（`AbyssFallBoneMealHandler:84-85`），兼容发射器（无玩家）路径。

**这是 mod 唯一获得深渊之花的「制作」途径。原版无任何办法用骨粉催熟凋零玫瑰，所以这是唯一路径**——这个事实是成就设计的基础。

### 6. 催熟特效（灵魂主题）
`AbyssDirtBlock.playBloomEffects()`。刻意避开骨粉默认的绿色欢快粒子，因为这是「献祭」而非「施肥」：

| 粒子 | 基准数量 |
|---|---|
| `SOUL` | 12 |
| `SCULK_SOUL` | 8 |
| `REVERSE_PORTAL` | 20 |
| `SMOKE` | 6 |

音效：`SOUL_ESCAPE.value()`（基准音量 0.7 / 音调 0.6）+ `SCULK_CATALYST_BLOOM`（0.5 / 1.4）。一低一高，读作「付出 → 到来」。

注意 `SOUL_ESCAPE` 是 `Holder.Reference<SoundEvent>` 需 `.value()`，`SCULK_CATALYST_BLOOM` 本身是 `SoundEvent`。

**第三次交接时接入了配置倍率**：上表数量与音量都会乘 `visuals.bloom_particle_scale` / `bloom_sound_volume`（默认 1.0，即上表原值）。**音调不受配置影响**，理由见 `HANDOFF.md` 「配置系统」一节。骨粉催熟机制本身**没有开关**——用户明确说骨粉是核心机制，不给其他 mod 让路，只有特效可调。

### 7. 药水效果：深渊探索者 `abyssfall:abyss_explorer`
`MobEffectCategory.BENEFICIAL`，颜色 `0x9B6BC9`。**纯标记效果，不覆盖任何 tick 方法**，逻辑全在战利品侧。18×18 图标由 `make-effect-icon.ps1` 生成。

注册用 `Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, ...)`。

**注意**：全项目**没有任何地方给玩家上这个效果**，只有战利品侧读取它。获取途径尚未设计。


### 8. 战利品表注入
`AbyssFallLootTables` 用 `LootTableEvents.MODIFY`（`net.fabricmc.fabric.api.loot.v3`），回调签名 `(key, tableBuilder, source, registries)`。

**第三次交接时已改为读配置**：注入哪些表、概率多少都来自 `LootSettings`，见 `HANDOFF.md` 「配置系统」一节。默认仍是原来那 18 张高价值结构宝箱表（沙漠神殿、丛林神庙、末地城宝藏、林地府邸、要塞×3、堡垒残骸×4、沉船宝藏、远古城市、试炼密室 unique×2、下界要塞、掠夺者前哨站、埋藏的宝藏），**行为与改动前一致**。

**两个独立池**：
1. 基础概率池：由 `flower_chance` 决定（默认 5%）
2. 带「深渊探索者」效果时**必定**额外给 1 个

第 2 个池的条件是 `LootItemEntityPropertyCondition.hasProperties(EntityTarget.THIS, EntityPredicate.effects(...))`。**能这样做的依据**：`RandomizableContainer.unpackLootTable()` 在玩家开箱时会 `withParameter(LootContextParams.THIS_ENTITY, player)`，所以战利品表知道开箱者是谁、身上有什么效果。破坏箱子不走这条路径，符合「开启宝箱」语义。

#### `isBuiltin()` 门槛已被刻意移除（用户明确决定，别加回去）

原先有 `if (!source.isBuiltin()) return;`，意图是「尊重数据包作者的重写」。**第三次交接时用户明确要求去掉**，原话：

> 「我没那么礼貌，我们的mod保留自己的权益的同时尊重他人即可，而不是牺牲自己的权益去尊重他人。」

用户同时明确表示，**「保持现状（继续拦）」这个选项以后不要再提**。

技术澄清（我上一轮判断错过一次，别重犯）：`LootTableSource` 四个值是 `VANILLA(true)` / `MOD(true)` / `DATA_PACK(false)` / `REPLACED(false)`。**其他 mod 自带的表是 `MOD`、`isBuiltin()` 为 true**，所以那个检查从来没拦住别的 mod，它实际只拦「外部数据包」和「被 REPLACE 替换过的表」。移除它是正当的：我们用 `withPool` **追加独立池**，不编辑既有池、不改任何既有条目权重，别人写的东西一字不动。现在改为遇到非 builtin 来源打 INFO，**冲突可见但不改变行为**。

#### 未命中的表会 WARN

配置里写了但整个加载周期从未出现的表 ID，会在 `LootTableEvents.ALL_LOADED` 时逐个 WARN。

**为什么只能这样检测**：`LootTableEvents.Modify` 返回 `void`，`withPool` 也无返回值或异常，**回调内部压根不存在「注入失败」这个状态**。真正的失败形态只有「回调从未被调用」（表不存在），而这只能等全部加载完才知道。用户原本要求「API 状态导致无法注入就 WARN」，我核实后报告了这一点并改成现在的形态。

用 `ConcurrentHashMap.newKeySet()` 跟踪待命中集合，因为 Fabric 自己的 loot 实现注释写明「due to possible cross-thread handling」。`ALL_LOADED` 里会重新填充集合以支持 `/reload` 后再次检测。

**`target_tables` 不限箱子、不限原版**：用的是 `ResourceKey.codec(Registries.LOOT_TABLE)`，任何命名空间的任何战利品表都能写，含钓鱼 `gameplay/fishing/treasure`、猪灵交易、生物掉落 `entities/*`。

**一个事实**：用户最初要求「加入所有含 EPIC 物品的原版箱子」，但实测 vanilla 只有 2 张表含真正 EPIC 物品（试炼密室 `reward_ominous_unique` 的沉重核心、远古城市的静默盔甲纹饰模板），**沙漠神殿并不含 EPIC**（附魔金苹果是 `RARE`）。用户据此改为「高价值结构宝箱」口径。

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

**已知结构限制**：用户想要 `abyss_fall + abyssdirt = abyss_gardeners`（两个父节点汇聚），但 `Advancement` 的 parent 字段类型是 `Optional<Identifier>` —— **单亲树，做不到多父节点**。故实现为链式。已向用户说明，用户接受。曾提出的备选方案 C（把 `abyss_gardeners` 的 criteria 扩为四条 AND）用户暂未采纳，可按需提起。

### 10. 资源与元数据
- `fabric.mod.json`：含 `icon`、`license: GPL-3.0-or-later`、`fabric-api: >=0.141.6`
- `assets/abyssfall/icon.png`（128×128，桃花，由 `make-icon.ps1` 生成）
- `assets/abyssfall/textures/gui/sprites/hud/san_{empty,full,half,full_blinking,half_blinking}.png`（9×9，由 `make-san-icon.ps1` 生成）。**放进这个目录就自动进 GUI 图集**，无需注册代码
- lang：`en_us.json` + `zh_cn.json`（**无 BOM 的 UTF-8**）
- `data/abyssfall/loot_table/blocks/abyss_dirt.json`（方块掉落自身）
- `data/minecraft/tags/block/mineable/shovel.json`
- `abyssfall.mixins.json`（`package: com.abyssfall.mixin`，`compatibilityLevel: JAVA_21`）登记 `WitherRoseBlockMixin`；`abyssfall.client.mixins.json`（`package: com.abyssfall.client.mixin`）登记 `HudStatusBarHeightRegistryImplMixin`。两份都设了 `injectors.defaultRequire = 1` 和 `overwrites.requireAnnotations = true`——**前者意味着注入点找不到会直接崩，这是故意的**：宁可启动失败也不要静默失效。

### 11. 美术脚本（PowerShell + System.Drawing）
`make-icon.ps1`（128×128 mod 图标）、`make-item-texture.ps1`（16×16 物品贴图）、`make-effect-icon.ps1`（18×18 效果图标）、`make-dev-icon.ps1`（16×16 DEV 字样）、`make-san-icon.ps1`（9×9 San HUD 图标 ×5，见 15d）。均用 `$PSScriptRoot` 相对定位，直接 `powershell -NoProfile -ExecutionPolicy Bypass -File .\xxx.ps1` 运行。

**含中文注释的脚本必须存成带 BOM 的 UTF-8**（只有 `make-san-icon.ps1` 有中文）——无 BOM 时 PowerShell 按 GBK 解，乱码会吃掉换行导致语法错误。

**9×9 / 16×16 这类小尺寸一律逐像素 `SetPixel`，不要用矢量+降采样**：任何抗锯齿都会把 1px 笔画糊成灰。大尺寸（icon.png、效果图标）才用超采样+双三次缩小。

### 12. 开发者物品栏 `abyssfall:abyssfall_dev_inventory`（第三次交接时新增）

第二个创造模式标签，**仅当 `developer.dev_tools = true` 时才注册**，默认 false。（配置键第四次交接时从 `dev_inventory` 改名，但**注册 ID 仍是 `abyssfall:abyssfall_dev_inventory` 没动**——那是存档相关的，别顺手改。）

标题是**三色**：「深渊」DARK_GRAY 粗 + 「浮现」GRAY 粗 + 「开发者物品栏」血红 `0xB01030` 粗斜。前两段**复用主标签的 lang key**（`itemGroup.abyssfall.head` / `.tail`），第三段是新增的 `itemGroup.abyssfall.dev`。用户已实测三色标题显示正常、tooltip 不变蓝。

血红用 `TextColor.fromRgb(0xB01030)` 而非 `ChatFormatting.DARK_RED`，因为创造界面背景偏亮，原版暗红读起来发棕。

en_us 的 `.dev` 值是 `" Dev Inventory"`（**有前导空格**），因为英文三段拼接不加空格会变成 `AbyssFallDev Inventory`；中文不需要空格。

**关键实现约束**：`AbyssFallDevInventory` 里的物品与标签**都不是 `static final`**，而是在 `initialize()` 里创建、用普通 static 字段持有。原因是 `static final` 在类被触碰的瞬间就完成注册，开关根本没机会起作用。`getSanCounter()` 在关闭时返回 null，javadoc 里说明了这是刻意的（项目里没有 `@Nullable` 依赖，所以只写在文档里）。

**后果要知道**：关掉开关后，存档里已有的这些物品会在加载时被当作未知物品**丢弃**。这是「真的没注册」的必然结果，不是 bug。用户当初的要求原话就是「物品也不会被注册」。若哪天想改成「物品仍存在、只是标签不显示」，那是另一套语义（只把标签注册设为条件性）。

### 13. 理智计数器 `abyssfall:san_counter`（第三次交接时新增）

开发者专用 debug 物品，放在开发者物品栏里，`stacksTo(1)`。图标暂用原版时钟贴图（`assets/abyssfall/items/san_counter.json` 直接指向 `minecraft:item/clock_00`）。

**作用**：主手右键，在生命/饱食度条上方显示 `理智值：当前 / 最大`，3 秒后淡出，再按重新计时。

**「3 秒 + 淡出 + 重按续期」全部是原版行为，一行计时器都没写。** 依据（1.21.11 mojmap 源码实测）：`Gui.setOverlayMessage` 把 `overlayMessageTime` **无条件**设为 60 ticks（= 精确 3 秒，无条件赋值所以重按即重置）；alpha 算式 `(overlayMessageTime - partialTick) * 255 / 20` 上限 255，所以最后 20 ticks 线性淡出；渲染位置 `translate(guiWidth/2, guiHeight - 68)` 正是生命/饱食度上方。传输链是 `ServerPlayer.displayClientMessage(c, true)` → `ClientboundSystemChatPacket(overlay=true)` → 客户端 `ChatListener.handleSystemMessage` → `gui.setOverlayMessage`。

**刻意只在服务端读值**（`player instanceof ServerPlayer`）：客户端那份 attachment 只是服务端推送的镜像，debug 工具必须报告权威值，否则它验证不了任何东西。返回 `InteractionResult.SUCCESS`（`SwingSource.CLIENT`）让挥手动画立刻播放，不等往返。

### 14. DEV 图标 `abyssfall:abyss_dev_icon`（第四次交接新增）

**纯工具物品，只为做开发者物品栏的标签图标而生。** 不命名（保持键值 `item.abyssfall.abyss_dev_icon`）、无任何行为、**不放入该物品栏的内容中**，`Properties` 全默认。用专门物品而非借用已有工具做 icon，是为了让标签图标不随工具外观变化而变。

**图标**：16×16 纯黑 `DEV`，字母外零像素、无抗锯齿。`make-dev-icon.ps1` 逐像素 `SetPixel` 绘制而不用 `System.Drawing` 的画字功能——16×16 上任何抗锯齿都会把 1px 笔画糊成灰色。**V 必须 5 列宽才有单像素尖底**（偶数宽度末行必然两像素 → 平底 → 读作 U），这是像素网格的硬约束，不是审美选择。

### 15. HUD 系统（第四次交接新增，客户端侧初次活跃）

#### 15a. 位置系统（两个公共 API + 一个 Mixin）

**API**（`fabric-rendering-v1` 16.2.10）：
- `HudElementRegistry.attachElementAfter(FOOD_BAR, SAN_BAR_ID, element)` → 层序从下到上：**快捷栏 → 饱食度 → 理智值 → 其他 mod**
- `HudStatusBarHeightRegistry.addRight(SAN_BAR_ID, provider)` → 报告占用高度，vanilla 的氧气条、手持物品名、overlay 文本自动上移让位
- 渲染坐标向 `getHeight(SAN_BAR_ID)` **询问**而非固定像素

**`getHeight(id)` 语义**：返回**顶边 Y**，且求和时**遇到自身即返回**（不含自己的高度）。正确写法 `guiHeight - getHeight(id)`，零额外偏移。这个坑我踩了两次，详见 `HANDOFF.md` 血泪教训 16。

**Mixin `HudStatusBarHeightRegistryImplMixin`（第二个 Mixin，经评估无法避免）**

注入 `HudStatusBarHeightRegistryImpl.init()` 的 `@Inject HEAD`——那是「所有 mod 注册完毕、`layers` 已填满，但顺序尚未被读取」的唯一时刻。职责**只是卡时机**：重排 `FOOD_BAR` 根层的 `layers`，把我们放在 vanilla `food_bar` 之后的第一位，这样其他 mod 排在我们上面。

**为什么不得不注入**：公共 API 无法表达「永远最后注册」。各 mod 的 `onInitializeClient()` 都早于 `CLIENT_STARTED`，而我们无法保证自己的 `CLIENT_STARTED` 监听器早于 fabric-rendering-v1 自己的。不需要 Accessor：`ROOT_ELEMENTS` 是 `public static final`，`RootLayer.layers()` 返回可变 `ArrayList`。

⚠️ **这是对 Fabric API `impl` 包的注入，不是公共 API**，仅在 MC 1.21.11 / fabric-rendering-v1 16.2.10 验证过。升级时必须重新核实三点（类头也写了）：`init()` 的签名与调用时机、`ROOT_ELEMENTS` 的类型与访问性、`RootLayer.layers()` 是否仍是 `ArrayList`。

#### 15b. 渲染逻辑（SanIconHudElement，第五次交接改成图标式）

**十个 9×9 图标排成一行，和原版饱食度并列**，右对齐到饱食行右边缘（`guiWidth/2 + 91`）。参数全部照 `Gui.renderFood` 实测：10 格、`blitSprite(..., 9, 9)`、步距 8、`ROW_WIDTH = 81`、先画 empty 再叠 full/half。

**San 连续 → 20 个半格的量化只存在于这个类里**，core 一无所知，`SanState` 仍是连续值。且**任何大于 0 的 San 至少亮半格**（`max(1, round(ratio*20))`），不能把「快没了」画成「没了」。

**三种动效，全部实测过时序**：

| 触发 | 效果 | 参数 |
|---|---|---|
| San ≤ 20% | 持续抖动，越低越快 | 逐图标 `y += random.nextInt(3)-1`，周期 `halves*3+1`，到 0 时每 tick |
| San 下降 | 全排抖一下 | `SHUDDER_TICKS = 4` |
| San 上升 | 从左到右行波 + 慢闪 1 次 | 每格上抬 2px、2 tick/格；闪 5 tick/相 × 1 |
| San 回满 | 快闪 4 次（**取代**上面的慢闪） | 2 tick/相 × 4 |

抖动与行波都是照抄 vanilla：抖动来自 `Gui.renderFood`（`tickCount % (foodLevel*3+1)`），行波来自 `Gui.renderHearts`（再生效果把心的 Y 减 2，**只位移不改色**）。回满快闪没有 vanilla 对应物，是补的——回满恰好是这一排即将淡出的时刻，不给信号的话「恢复了」和「HUD 被关了」看起来一样。

**回满不叠加普通闪而是取代它**：把玩家补满是一个事件，该给一个信号。慢闪进行中收到能补满的恢复会被立刻打断换成快闪，不排队。

**变化检测用轮询同步值（`lastSeen` 字段），没用 `SanChangedCallback`**：那个事件在服务端触发，而动效的正确时机是**客户端看到数值变化的那一刻**（attachment 同步到达时）。用事件在多人/延迟下会和画面不同步。首帧只记录不反应，所以登录时 San 不满不会误触发。

**⚠️ 闪光必须用第二套贴图，不能靠代码提亮**（第五次交接踩过，详见 `HANDOFF.md` 血泪教训 20）：`blitSprite` 的 tint 是**乘算**的，只能变暗；且这里传的 `ARGB.white(alpha)` 其 RGB 本身就是 `0xFFFFFF`。vanilla 也是这么做的——心有 `hud/heart/full_blinking`，同一张图把 `FF1313` 提成 `FFA1A1`。

**可见性与淡出逻辑一行未改**（沿用 15b 原有设计）：满 100% 不显示且不占垂直空间，低于 `show_below_percent` 常显，回满后约 1 秒淡出、高度随 alpha 同步收缩。淡出用 `Util.getMillis()`，抖动/行波/闪光用 `player.tickCount`——**后者刻意不用帧计数**，否则 200fps 下会抖得比原版快十倍且暂停不停。

#### 15c. 进度条 HUD（`SanBarHudElement`）保留但不注册

紫色进度条 + `San: 90.00%` 文本那个类**完整保留着，一行逻辑没删，但没有注册到 HUD**。

**这不是死代码，别删、别和图标行合并。** 用户明确要求：「保留原来的那个进度条HUD的代码，以后写一个道具会显示细节，细节就是那个紫色进度条的代码」。它是**详细读数**，将来给一个道具用。类注释顶部有警示段。

复用时要注意一处：它的 `render` 仍向 `HudStatusBarHeightRegistry` 按 `SAN_BAR_ID` 问 Y 坐标，而那个 ID 现在属于图标行。要画到状态栏区域以外，得直接给它坐标而不是让它去问。

**元素 ID 仍是 `abyssfall:san_bar`**（没改成 `san_icons`）：Mixin 靠它查图层，改名等于同时改 Mixin，收益为零。它标识的是「本 mod 的 San 读数」，不是某种具体长相。

#### 15d. San 图标美术（`make-san-icon.ps1`）

五张 9×9：`san_empty` / `san_full` / `san_half` / `san_full_blinking` / `san_half_blinking`。放在 `assets/abyssfall/textures/gui/sprites/hud/`，**零注册代码**（`DirectoryLister` 自动收进 GUI 图集，玩家也能用资源包替换）。

图案是**用户亲手定稿的**（我出了 A~E 五个方向 + B 的多轮改法，用户逐像素指了改法）。主体是一坨正在失去形状、甩出一滴的东西；**滴落那一点故意和主体断开**——连着主体的 1px 细流会被读成一根针。

脚本设计成给用户改的：开头就是 9 行字符画 `$pattern` + 三张写死的十六进制调色板，改完直接跑，还会生成一张带行列坐标和棋盘格背景的放大预览图（`build/san-icon-preview.png`）。

**⚠️ 文件必须存成带 BOM 的 UTF-8**。无 BOM 时 PowerShell 按 GBK 解中文注释，乱码会吃掉换行直接语法错误。

**⚠️ 半格方向：保留左半、右半透明。**「从右往左掏空」，和一条从左往右缩短的进度条一致。证据是原版**心**的逐像素比对（`heart/full` 第 2 行 x=1..7 有料，`heart/half` 是 x=1..4）。**千万别拿 `food_half.png` 反推**——鸡腿图案倾斜不对称，它的半格看着像「变瘦」而不是「切一半」，我第一版就是这么读反的，用户实测才发现（详见 `HANDOFF.md` 血泪教训 21）。

配色：主体 `9B6BC9`（沿用项目 San 紫）、高光 `C4A2E3`、暗部 `6E4A96`、滴落 `4A2E68`；亮版主体 `E4CDF4`；空槽只有纯黑轮廓 + `282828` 内部，这两个值是从原版 `food_empty.png` 逐像素读出来的。**轮廓在亮版里不提亮**——原版 `full_blinking` 的深边也只提了一点，全提会让图标失去形状。

### 16. 测试协议系统（第四次交接新增）

唯一的 `preLaunch` 入口点，在 Mixin bootstrap 之后、`onInitialize()` 之前几秒执行。

用户的定位：「算是和玩家的一些小约定，它们二次分发我们的 mod 我们也没办法……咱们这个项目本来就是开源项目。」所以这是**告知机制而非安全措施**——jar 是 GPL 的、检查可被轻易移除，这正是设计意图。

| 环境 | 行为 |
|---|---|
| 开发环境（`isDevelopmentEnvironment()`） | 静默通过，`runClient` 不受干扰 |
| 客户端（有显示） | Swing 对话框：双语文案 + 输入框 + 「复制仓库链接」按钮 |
| 客户端（无头） | WARN 后**降级为服务器行为**（已实测，无 `HeadlessException`） |
| 服务器 | WARN 协议文案 + INFO Server 说明 + INFO 链接，**永不阻止启动** |

**接受**：`accept`（不区分大小写、去首尾空格）→ INFO → 继续加载。
**拒绝**（输错/空/取消/关窗）：ERROR → 抛 `RuntimeException`。Loader 的 `handleFormattedException` 会自动记日志 + 弹错误窗 + `System.exit(1)`，**所以不要自己调 `System.exit()`**，那会绕过日志和错误窗。

**三个约束**：
- **文案必须硬编码在 Java 里**（`agreement/AgreementText`）：该阶段无资源管理器、无翻译系统，lang 文件到不了这里。
- **必须是独立类**，不引用任何其他 mod 类（含 `AbyssFall.LOGGER`），否则会过早触发静态初始化器——这是 `PreLaunchEntrypoint` javadoc 自己的警告。所以它有自己的 Logger。
- **配置系统此时尚未加载**，记不住接受状态。正好符合「每次都问」的要求（用户明确要每次问，理由是记住的答案就是不再被阅读的答案）。

## Git / 发布流程（第一次交接时新建，以后由你负责）

用户明确授权：**「以后 github 构建都由你来」**。

### 仓库状态

远端 `https://github.com/Kainy030/AbyssFall-Fabric.git`，分支 `main`，凭据已缓存（`credential.helper=manager`，push 无需交互），git 用户 Kainy / 1747110555@qq.com。

tag：`0.1-Dev`、`v0.2-Dev`、`v0.3-Dev`、`v0.4-Dev`。**提交历史和哈希一律现场核实**（`git --no-pager log --oneline -5; git status --short; git --no-pager tag`），别信文档里写死的。

### tag 命名规则（重要）

`0.1-Dev` 无前缀，`v0.2-Dev` 起改为**带 `v` 前缀**。workflow 触发器同时接受两种形状：
```yaml
tags:
  - '[0-9]+.[0-9]+*'      # 匹配 0.1-Dev
  - 'v[0-9]+.[0-9]+*'     # 匹配 v0.2-Dev
```
**别简化成只留一种**——旧 tag 还在，两种都要能构建。

tag 名与 `gradle.properties` 的 `version` **不必一致**：`version=0.4-Dev`（jar 内的版本号，无 `v`），tag 是 `v0.4-Dev`。

### 三个发布产物

`build.gradle` 加了两个自定义任务：`javadocJar`（打包 API 文档，项目原本只有 `javadoc` 无打包任务）和 `releaseJars`（把三个 jar 按短名汇总到 `build/release/`）。

产出：`abyssfall.jar`（来自 **`remapJar`**，不是 `jar`——后者 dev-mapped、真实环境跑不了）、`abyssfall-doc.jar`、`abyssfall-source.jar`。`build/libs/` 同时保留 Gradle 标准命名版，内容字节一致。

**Javadoc 必须显式设 UTF-8**（`options.encoding` / `docEncoding` / `charSet`）：源码含中文注释和排印破折号，doclet 默认用平台编码（本机 GBK）会读失败。同时关了 doclint，因为 MC / Fabric API 无外链文档。

### GitHub Actions

`.github/workflows/release.yml`，tag push 或手动 `workflow_dispatch` 触发。action 版本（当时查证过的最新版，将来需再查）：`checkout@v7` / `setup-java@v5` / `setup-gradle@v6` / `action-gh-release@v3` / `upload-artifact@v7`。

**三个关键设计点**：
1. **不加 `--offline`** —— 本地能 offline 是因为缓存完整；CI 空缓存必须联网拉 Minecraft / Loom / Fabric API，首次约 3–8 分钟。
2. **直接用 `setup-java` 提供 JDK 21，不靠 Gradle toolchain** —— `gradle.properties` 里 `auto-download=false` 且硬编码了本机 JDK 路径，CI 上不存在。
3. **`gradlew` 必须是 `100755`** —— Windows 建仓时它是 `100644`，Linux runner 上会 `Permission denied`。已用 `git update-index --chmod=+x gradlew` 修好。

**已验证的 CI 结果**（0.1-Dev 那次，用户回报 SHA256 比对）：`abyssfall.jar` 和 `abyssfall-source.jar` 字节级一致；`abyssfall-doc.jar` 不一致，因为 Javadoc HTML 内嵌时间戳/JDK 版本（内容等价，是推断，未逐字节 diff 证明）。

**四次运行全部成功**（第六次交接用 `gh run list` 实测，推翻了前几次「没等 CI 跑完、无法查询」的记载）：`0.1-Dev` / `v0.2-Dev` / `v0.3-Dev` / `v0.4-Dev` 四个 tag 的 Release workflow 都是 `completed success`，耗时 1m25s ~ 1m49s。`v0.4-Dev` 的 Release 资产实测含 `abyssfall.jar` / `abyssfall-doc.jar` / `abyssfall-source.jar` 三个。**以后自己用 gh 查，别再留成未验证项**（用法见下面「gh CLI」一节）。

⚠️ 一个命名不一致，别踩：`0.1-Dev` 那次的 **Release 标题是 `Fabric-v0.1-Dev`**，而 tag 是 `0.1-Dev`。后三次 Release 名与 tag 一致。查 Release 时按 tag 查不会出错，按名字查会。

### 发布流程

```powershell
git add -A; git commit -m "..."; git push origin main
git tag -a '<版本>' -m 'AbyssFall <版本>'; git push origin '<版本>'   # CI 自动构建并附加三个 jar
```
也可在 Actions 页面手动 `workflow_dispatch` 选任意已有 tag 触发，不必重新打 tag。

**两个坑**：
- push 时偶发 `schannel: failed to receive handshake`。**读操作（`ls-remote`）正常、写操作失败 → 是网络不稳定，不是权限问题**，重试即可（有次连失败 3 次、第 4 次成功）。重试循环的 `Start-Sleep` 总时长要控制在 30 秒内。
- push 成功时 git 会把进度写到 stderr，**PowerShell 会因此报非零退出码**。看输出里有没有 `<old>..<new> main -> main` 才是判断依据，别信退出码。

### gh CLI（第六次交接装好，现在可用）

`gh` 已装在 `C:\Program Files\GitHub CLI\gh.exe`（第六次交接实测版本 2.98.0）。**当前会话的 PATH 里可能还没有它**（装完没重开终端），保险起见用全路径调。

**登录方式有个坑**：`gh auth login --with-token` 会**拒绝** git 凭据里那个 token，报 `missing required scope 'read:org'`。但 `GH_TOKEN` 环境变量走的是另一条路径、不做这个 scope 校验，**能正常调 API**。所以这样用：

```powershell
$gh='C:\Program Files\GitHub CLI\gh.exe'
$env:GH_TOKEN=(("protocol=https`nhost=github.com`n`n" | git credential fill) |
    Where-Object { $_ -like 'password=*' }) -replace '^password=',''
& $gh run list --limit 5          # 查 CI
& $gh release view v0.4-Dev --json assets --jq '.assets[].name'
```

token 从 git 的凭据管理器现取，**不要写进任何文件**。`GH_TOKEN` 只在当次 `run_commands` 调用内有效，每次都要重新设。

### GitHub 的一个认知点

用户曾疑惑「手动用 tag 构建版本，Assets 里只有 Source code」。**这是正常的**：那两个源码包由 GitHub 按 tag 自动生成、无法删除；编译产物 GitHub 永远不会自己生成，必须上传或用 CI。「用 tag 创建 Release」只是建了个条目，没有发生任何编译。

