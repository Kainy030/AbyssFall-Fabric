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
├── AbyssFall.java                      主入口：MOD_ID + LOGGER + id(String) + configPath(String)
├── advancement/AbyssFallAdvancements.java
├── agreement/AgreementText.java        测试协议文案（双语，硬编码）
├── agreement/TestAgreement.java        preLaunch 入口点，见 16
├── block/AbyssDirtBlock.java  AbyssFallBlocks.java  AbyssFallBoneMealHandler.java
│        TintedGlassPaneBlock.java
├── config/  (7 个，见 HANDOFF 4)
├── core/    (6 个，见 HANDOFF 3)
├── damage/AbyssFallDamageTypes  DeathOmenDamageSource                见 17
├── effect/AbyssExplorerEffect  AbyssFallEffects  SanBreakdownEffect  SanSpiritedEffect
├── item/AbyssFallDevInventory  AbyssFallItemGroups  AbyssFallItems
│        SanCounterItem  SanLensItem  FinalDeathOmen                  见 17
├── item/AbyssFallRarity.java            自有稀有度（两级），见 19
├── loot/AbyssFallLootTables.java
├── shadercore/  (11 个：AbyssFallShaderCore  AbyssFallShaderConfig  ShaderConfigData
│                 ShaderConfigProvider  ShaderEffect  ShaderEffectProvider
│                 ShaderEffectType  ShaderEffectTypes  ShaderColorSource
│                 ShaderRenderContext  ShaderGeometrySource  ShaderQuad
│                 ShaderVertex)                                见 18 / HANDOFF 4b
├── shadercore/color/FixedColorSource.java      ⚠️ 占位实现，见 18d
├── shadercore/color/ColorDerivation.java       四种推导，见 18g
├── shadercore/color/DerivedColorSource.java    从物品贴图推色，见 18g
├── shadercore/color/ShaderColorSources.java    dispatch codec，见 18g
├── shadercore/geometry/ItemHullGeometry.java   跟随物品外壳，见 18h
├── shadercore/geometry/ItemFacesGeometry.java  只 ±Z 两面+基础层，见 18h / 18j
├── shadercore/effect/MaskedPulseEffect.java    第一个效果种类，见 18e
├── shadercore/effect/CosmicEffect.java         旧移植星空（寰宇支配之剑），见 18j
├── shadercore/effect/AbyssEffect.java          cosmic 的独立副本（死兆将至），见 18j
└── mixin/PlayerAttackMixin.java        毕业武器接管点，见 17
src/client/java/com/abyssfall/client/
├── AbyssFallClient.java
├── hud/AbyssFallSanHud.java          HUD 注册
├── hud/SanHudDispatchElement.java    按模式转发（唯一注册的元素）
├── hud/SanIconHudElement.java        图标行
├── hud/SanBarHudElement.java         进度条
├── tooltip/AbyssFallTooltips.java    tooltip 逐字波浪染色 + 物品名，见 17g / 19
├── tooltip/SwordOfTheCosmosTribute.java  致敬碑文（Shift 展开），见 19b
├── render/ShaderLayerItemModel.java      包装物品模型 + 每帧决策，见 18c
├── render/ShaderLayerModelPlugin.java    装到所有物品 + 取几何，见 18c / 18h
├── render/ShaderLayerRenderer.java       画 source 给的几何，见 18c
├── shader/AbyssFallPipelines.java        effect → RenderType，见 18b
├── mixin/RenderTypeInvoker.java          取 package-private 的 create，见 18b
├── mixin/HudSelectedItemNameMixin.java   手持提示的物品名上色，见 19a
└── mixin/HudStatusBarHeightRegistryImplMixin.java   见 15a

src/main/resources/assets/abyssfall/shaders/core/
├── masked_pulse.vsh / .fsh            见 18e / 18g
├── cosmic.vsh / .fsh                  旧移植「星空」，跑 18j 说的旧算法
└── abyss.vsh / .fsh                   同上的独立副本（深渊效果），见 18j

src/main/resources/assets/abyssfall/textures/shader/
├── cosmic/cosmic_0..9.png (+.mcmeta)  旧算法素材，18j
└── abyss/abyss_0..9.png (+.mcmeta)    旧算法素材，18j
```

**美术脚本**（见 11）：`make-death-omen-texture.ps1`（alpha 二值化，见 18i）+ `make-death-omen-mask.ps1`（debug 遮罩，见 18i）。

### 🔴 贴图目录约定：材质多的物品各占一个同名子目录（v1.9-Dev-Fix）

```
textures/item/
├── abyss_flower.png                     单张贴图的物品：留在 item/ 根下，不动
├── san_lens.png + .mcmeta               两份也算少，同样留在根下
├── final_death_omen/                    ← 四份文件的物品分出来
│   ├── final_death_omen.png / .mcmeta
│   └── final_death_omen_mask.png / .mcmeta
└── fake_infinity_sword/
    ├── fake_infinity_sword.png / .mcmeta
    └── fake_infinity_sword_mask.png / .mcmeta
```

**目录名 = 物品名，文件名保持全名不缩写**（用户选定的方案 A）：这样在 IDE 里按全名搜索仍能命中，代价是名字重复一次。

🔴 **子目录会进 sprite 名，一并进图集**：`item/final_death_omen/final_death_omen_mask` 就是完整的 sprite id。这不是巧合而是 vanilla 的既有能力 —— `DirectoryLister` 经 `PathPackResources.listPath` 的 `Files.find(path, Integer.MAX_VALUE, ...)` **递归**列举，jar 侧的 `FilePackResources.listResources` 也只做前缀判断（26.2 源码实证）。vanilla 自己就依赖这点：`atlases/gui.json` 的 source 是 `gui/sprites`，而其下有 207 个深度 ≥2 的文件（`hud/heart/*`）。

⇒ **遮罩/素材作为图集精灵才能播动画、才能被递归收录**这条在子目录下依然成立（机制见 18j 的「落地框架」），无需给 `items.json` 加任何 source。

⚠️ **只能是 `.png`**：`DirectoryLister` 的 converter 扩展名写死 `".png"`（不是 png 根本不进图集），且 `NativeImage.read` 会 `PngInfo.validateHeader` 后抛 `Bad PNG Signature`。**jpg 在这两道门上都过不去**，也没有 alpha 通道可供 18i 那条二值化约定使用。

⚠️ **`.mcmeta` 必须与 `.png` 同目录同名**。它不是 `.png` 故不会被当成精灵收录，只作为伴生元数据被 `resource.metadata()` 读到。

**改这类路径要同步四类引用**（v1.9-Dev-Fix 迁移两把剑时实际动了这些）：
1. `models/item/*.json` 的 `layer0`
2. `ShaderConfigData.DEFAULT` 里的 mask sprite 名
3. 写这些文件的美术脚本（`make-death-omen-texture.ps1` / `make-death-omen-mask.ps1`）
4. 🔴 **玩家本机已生成的 `config/abyssfall/AbyssFallShader.json`** —— 那是**存量文件**，不热加载也不自动迁移，仍写着旧 sprite 名 ⇒ `resolve` 报 WARN、`forEffect` 返回 null、**星空完全不画**。改完必须提醒用户删掉重新生成（同族问题见 `HANDOFF.md` 教训 19）

⚠️ 改名会让精灵在图集里换位置 ⇒ 编译进 shader 的 `SPRITE_n_U0..V1` / `MASK_*` 常量全部变化。功能上无害（每次重载本来就重算），但**必须进游戏复看渲染**，编译通过不能作为证据。

**Mixin 现在有四个**（`main` 一个 + `client` 三个），配置两份。`src/main` 下的 `mixin/` 包在 26.2 迁移时曾被删除（`WitherRoseBlockMixin` 改成数据文件，见 4），v1.3-Dev 为毕业武器**重新建立**——那次删除是因为不再需要，不是因为禁止。

`onInitialize()` 调用顺序**有依赖关系，勿随意调整**：
```java
AbyssFallConfig.load();               // 最先！注册与否取决于配置，注册后无法回头
AbyssFallShaderCore.initialize();     // 必须在下一行之前：解析 entry 需要 effect 类型已注册
AbyssFallShaderConfig.load();
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

`Properties.ofFullCopy(Blocks.DIRT)`，实现 `BonemealableBlock`。材质 `abyssfall:block/abyss_dirt`。tag：`mineable/shovel` + `supports_wither_rose`。

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
- **`abyssfall.client.mixins.json`**（`package: com.abyssfall.client.mixin`，`compatibilityLevel: JAVA_25`）与 **`abyssfall.mixins.json`**（`package: com.abyssfall.mixin`，无 `environment` ⇒ 两端加载，见 17）——**两份配置都在 `fabric.mod.json` 的 `mixins` 数组里注册**。两者都用 `injectors.defaultRequire = 1` + `overwrites.requireAnnotations = true`：**注入点找不到会直接崩，这是故意的**（宁可启动失败也不要静默失效）。⚠️ 改完必须验 JSON（`HANDOFF.md` 教训 26）

## 11. 美术脚本（PowerShell + System.Drawing）

`make-icon.ps1`（128×128）、`make-item-texture.ps1`（16×16）、`make-effect-icon.ps1`（18×18）、`make-dev-icon.ps1`（16×16 DEV）、`make-san-icon.ps1`（9×9 ×5，见 15d）、`make-lens-icon.ps1`（两面镜子，见 13d）、`make-breakdown-icon.ps1` / `make-spirited-icon.ps1`（18×18，**占位**）、`make-death-omen-texture.ps1`（alpha 二值化，见 18i）、`make-death-omen-mask.ps1`（debug 遮罩，见 18i-2）。均用 `$PSScriptRoot` 相对定位。

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

**玩家向内容，不是 debug 工具**，所以注册在 `AbyssFallItems` / 默认创造栏而非开发者栏。`stacksTo(1)`、`Rarity.EPIC`。贴图见 13d。**作用**：右键在两种 San 读数间切换 + 快捷栏上方提示（机制见 15c）。

**🔴 与理智计数器端相反**：

| | 理智计数器 | 认知窥镜 |
|---|---|---|
| 判定 | `player instanceof ServerPlayer` | **`level.isClientSide()`** |
| 在哪侧干活 | **服务端**（读权威值） | **客户端**（改的是纯屏幕状态） |
| 消息 | `ServerPlayer.sendOverlayMessage` 走系统聊天包 | 本地 `player.sendOverlayMessage`（`LocalPlayer` 覆写为 `chatListener().handleOverlay`） |

⚠️ **必须判 `isClientSide()` 而不是 `instanceof ServerPlayer`**：`use()` 两侧都跑，单人世界两端同进程，不判会切两次、自己抵消。

**lang 键**：`item.abyssfall.san_lens`、`.switched`（一个 `%s`）、`.mode.icons` / `.mode.percent`。模式名是「**具象 / 量化**」（Figurative / Quantified）而非「图标/百分比」——窥镜给的不是另一种界面，而是另一种认知方式。提示语「视界已切换：量化」。

## 13c. 金镜 `abyssfall:gold_lens`

**纯占位，无任何行为**，`Item::new` + `stacksTo(1)`（品质仍是默认 COMMON）。放常规创造栏，紧随认知窥镜。

**它就是认知窥镜去掉眼睛的那一版**——同一面镜子，在「有东西透过它看你」之前或之后。堆叠限 1 不是因为多拿一个没用（它本来就没用），而是这个物品的气质不适合成叠。

**配方**（`data/abyssfall/recipe/gold_lens.json`）：金锭四角 + 黑曜石四边 + 遮光玻璃板居中，产出 1。`category: "misc"`。

**解锁**：`advancement/recipes/gold_lens.json`，条件是**已获得原版成就 `minecraft:story/form_obsidian`**（「冰桶挑战」，即获得黑曜石）。**刻意复用原版成就而非自建**——配方本身就要黑曜石，玩家做出黑曜石那一刻正好解锁。该 advancement **无 `display`**，玩家看不到条目，它只是配方解锁器（原版所有 `recipes/` 下的条目都这样）。

## 13d. 两面镜子的美术（`make-lens-icon.ps1`）

**一个脚本产出两个物品的贴图**，刻意不拆：两者共用金框、镜面与整套配色，拆开就会各改一半然后飘。差异只在有没有眼睛。

产物四件：`san_lens.png`（16×48 动画条）、`san_lens.png.mcmeta`、`gold_lens.png`（16×16 静态、**无 mcmeta**）、`build/lens-icon-preview.png`（带行列号的放大预览，不进仓库）。

### 🔴 16px 画眼睛的四条硬约束（照原版 `ender_eye.png` 逐像素提取得来）

当年画这只眼睛失败了十几轮，全是甜甜圈/字母 O/H/M/金币。**这四条缺一条就不成立**：

1. **瞳孔必须 1px 宽 × 3 行高的竖缝**。2×2 方块瞳孔被虹膜包围，任何尺度下都读作甜甜圈
2. **瞳孔与虹膜之间必须有一层暗环**（本项目 `2E3348`，原版 `1E4835`）。**这是最后让它成立的那一笔**——缺这层中间明度，瞳孔与虹膜糊成一个形状
3. **虹膜左上要有高光点**（`F4F7FF`，原版 `CBFCDD`）。它打破对称，否则读作字母
4. **虹膜色相必须与镜框对立**。虹膜也用金色 ⇒ 整枚读作一枚金币。所以虹膜是冷蓝灰 `BFC8DE`

另加：**虹膜与金框之间必须留一圈暗镜面**，否则两者粘连。

⚠️ **别去「优化」瞳孔尺寸或删掉暗环层**，那是唯一让它读作眼睛的结构。

### 动画

只存 3 张唯一帧（正视/左视/右视，`$gazeFrames = @(0, -1, 1)`），mcmeta 用 `{index, time}` 逐帧指定时长来引用它们——所以文件是 16×48 而不是「一步一格」。

**节奏刻意不均匀**（`$gazeSequence`）：正视停 70/45/85/38 tick，瞥向两侧只 12~22 tick。**均匀节奏读作机器扫描，高傲的注视是长久平视 + 偶尔屈尊一瞥**。整循环 304 tick ≈ 15.2 秒。

**瞳孔只能左右各移 1px**：虹膜仅 col 5–10，移 2px 暗环就出到镜面上、眼睛散架。

### ⚠️ PowerShell 陷阱（踩过）

**哈希表键大小写不敏感**——用 `'d'` / `'D'` 区分明暗会直接 `Duplicate keys` 解析失败，脚本压根跑不起来。所以调色板全用互不相同的字符。

## 13e. 方块：遮光玻璃板 `abyssfall:tinted_glass_pane`

**原版缺的那一块**：原版有遮光玻璃（整块）和其他所有玻璃的板，却没有遮光玻璃板。

`TintedGlassPaneBlock extends IronBarsBlock`。**必须建子类**，两个原因：`IronBarsBlock` 构造器是 `protected`（跨包引用不到，`IronBarsBlock::new` 不成立），且遮光覆写得有地方放。

**遮光靠这两个方法**（照抄 `TintedGlassBlock`，26.2 只有它这么做）：
```java
propagatesSkylightDown → false   // 阻断天光直下
getLightDampening      → 15      // 光照衰减打满
```
⚠️ **方法名是 `getLightDampening`，不是 `getLightBlock`。**

✅ **已实测真遮光**（用户在游戏内确认）。⇒ **`getLightDampening` 不看方块实际体积，2px 薄片也照满值算。** 这条以后可直接复用。

**属性**：显式写出原版 `GLASS_PANE` 那一行（`instrument(HAT).strength(0.3F).sound(GLASS).noOcclusion()`）+ `mapColor(COLOR_GRAY)`。**不用 `ofFullCopy`**，因为那会连 mapColor 一起抄来再覆盖，读起来像事后补的。`noOcclusion` 与遮光无关，别因为「它遮光了」就去掉——occlusion 管的是渲染剔除，板子不是满方块不能宣称遮挡。

**材质 1:1 复制的关键**：模型继承原版 `template_glass_pane_*`（5 个变体），texture 只需两个键、**都要包 `force_translucent`**：
```json
"edge": { "force_translucent": true, "sprite": "minecraft:block/glass_pane_top" },
"pane": { "force_translucent": true, "sprite": "minecraft:block/tinted_glass" }
```
**没有自画任何贴图**，直接引用原版路径——这才是真 1:1。`edge` 沿用通用顶边（染色玻璃板全都复用它）。

**blockstate** 照抄原版 9 条 multipart。**战利品表**照抄玻璃板：**只有精准采集才掉落**。

**未加 `mineable` tag**：原版玻璃板不属于任何工具 tag（徒手/任意工具速度一致），保持一致。

**配方**：遮光玻璃 6 个（3×2）→ 16 个板，与原版玻璃板同比例。解锁条件是拿到遮光玻璃。

### 🔴 进两个创造栏，其中一个是原版的

自己的栏直接 `accept`。原版**染色方块**栏（`itemGroup.coloredBlocks`）用：
```java
entries.insertAfter(Items.GLASS_PANE, AbyssFallBlocks.TINTED_GLASS_PANE)
```
**用 `insertAfter` 不用 `accept`**：`accept` 会追加到整个标签最末尾（横幅之后），离玻璃十万八千里。

原版该栏顺序（已验证）：`GLASS → TINTED_GLASS → 16色STAINED_GLASS → GLASS_PANE → 16色STAINED_GLASS_PANE`，所以插在 `GLASS_PANE` 后正是原版会放它的位置。

⚠️ **`CreativeModeTabs` 的 14 个 tab key 全是 `private`**，引用不到。自建等价 key 即可（`createKey` 的实现就是这个）：
```java
ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("colored_blocks"))
```
指向的是**同一个标签**，不是副本。

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

## 17. 毕业武器：死兆将至 `abyssfall:final_death_omen`（v1.3-Dev 新增）

用户定位：**「不讲道理的秒杀所有生物——我说你死了，那你便是死了」**。核心宗旨：**AbyssFall 不相信 mod 或原版任何关于「能否造成伤害」的判定结果**。

### 17a. 为什么不能用物品钩子或 Fabric 事件

所有扩展点都在它要跳过的判定**内部**：`Item.hurtEnemy` 在 `hurtServer` 返回之后才跑（目标拒绝了这一击就到不了）；`ServerLivingEntityEvents.ALLOW_DAMAGE` 本身就是注入 `hurtServer` 的，属于被绕过的意见之一；`damage_type` 组件只改来源、伤害仍要过流水线。

⇒ **`Player#attack` 是唯一确定早于全部判定的点**。

### 17b. 接管方式：`@WrapMethod`，不是 `@Inject` / `@Redirect` / `@Overwrite`

`mixin/PlayerAttackMixin`，`@Mixin(value = Player.class, priority = Integer.MAX_VALUE)`。

- **`@WrapMethod`**（MixinExtras 0.5.4，**Loader 0.19.3 内嵌**，零新依赖）：vanilla 方法变成一个可调可不调的 `Operation`，「武器替换攻击」成为结构事实而非取消标志的副作用。**可叠加**，别的 mod 包裹同方法会形成嵌套链
- ❌ **`@Overwrite` 曾被评估并否决**：`hurtServer` 全仓 **55 处覆写** + `actuallyHurt` 7 处，覆盖不全；且会**静默删掉** Fabric API 自己的 `ALLOW_DAMAGE`/`AFTER_DAMAGE` 注入点
- ❌ `@Redirect` **独占**，第二个 mod 直接冲突
- ⚠️ **`priority` 不保证「运行时先执行」**，只决定 mixin 合并顺序。真正让它说了算的是**包裹层在方法体之外**这个结构位置。写 `Integer.MAX_VALUE` 无害（默认 1000，`readPriority` 无上限校验），但别把它当正确性保证

🔴 **必须判 `ServerLevel`**：`Player#attack` **两端都跑**（客户端 `MultiPlayerGameMode.attack:418` 发包后本地也调、服务端 `handleAttack:1797`），单人同进程会执行两次。不是服务端就 `original.call(target)` 交还 vanilla ⇒ 挥手动画照常本地播放。**注意这与认知窥镜刻意相反**（13b 判 `isClientSide()` 只在客户端做），同一个坑两个功能答案不同。

### 17c. 秒杀四步（`item/FinalDeathOmen.strike`，顺序与每步都不可省）

因为 vanilla 流水线**没在跑**，这些平时由它做的事必须自己做：

1. **`setLastHurtByPlayer(attacker, 100)`** —— `lastHurtByPlayerMemoryTime` 平时由 `hurtServer` 内的 `resolvePlayerResponsibleForDamage` 设置。**不设则经验为 0、战利品走「非玩家击杀」分支**，很多怪几乎不掉东西
2. **`getCombatTracker().recordDamage(source, getHealth())`** —— 死亡消息取 `CombatTracker` 最后一条记录，而 entries **只由 `actuallyHurt` 里的 `recordDamage` 写入**。无记录 ⇒ 退化成 `death.attack.generic`（「某某死了」），专属文案全丢。记录值＝目标此刻剩余生命值
3. **`setHealth(0)`** —— 直接置零而非减伤害，因为减法那一步才是吸收等减伤生效的地方
4. **`die(source)`** —— 掉落、经验、计分板、消息都在这里。**幂等**（开头判 `!isRemoved() && !dead`，`handleKillingBlow` 置 `dead=true`），重复调用安全

**非 `LivingEntity`** 走 `entity.kill(level)`（船/矿车/盔甲架/末影水晶各有覆写，比 `remove()` 更尊重其语义）。**`EnderDragonPart` 转 `parentMob`**——vanilla 自己的 `KineticWeapon` 也这么处理 ⇒ 打翅膀等于打龙，非头部 `/4` 衰减不存在。

### 17d. 覆盖范围（已核实的边界）

不进入 `hurtServer` ⇒ 以下**一次都不执行**：凋灵无敌窗口/`WITHER_IMMUNE_TO`、末影龙 DYING 阶段与相位减伤、52 个子类的提前 return、PvP 开关、创造模式/`invulnerable`/abilities、护甲/抗性/附魔/盾牌/吸收、不朽图腾、无敌帧、其它 mod 的 `hurtServer` 覆写与 `ALLOW_DAMAGE`。

- **武器是剑**（不挂 `PIERCING_WEAPON`/`KINETIC_WEAPON`）⇒ 只走 `attack`，`Player.stabAttack` 那条路**永不触发**，无需注入
- ⚠️ **掉落物与经验球秒不了**：`handleAttack:1788` 在调 `attack` 之前就判定它们不可攻击并**直接踢玩家下线**。这是 vanilla 前置校验，绕不过。「所有实体」实际＝「所有能被合法左键攻击的实体」
- ⚠️ 横扫附带目标走 `doSweepAttack` → `nearby.hurtServer(...)`，**不经过 `attack`**，目前不秒杀
### 17e. 伤害类型 `abyssfall:death_omen`

**唯一不能纯 Mixin 的一环**：`DamageType` 是注册表内容，必须是数据文件 `data/abyssfall/damage_type/death_omen.json`（vanilla 自己所有伤害类型也都是数据文件）。`damage/AbyssFallDamageTypes` 只持 key + 在**每次调用时**从 level 的注册表解析（不缓存静态字段：datapack 内容，`/reload` 后旧 holder 会失效）。

**8 个 `bypasses_*` tag 全加**（`data/minecraft/tags/damage_type/*.json`，`"replace": false` 只做加法）：`armor` `cooldown` `effects` `enchantments` `invulnerability` `resistance` `shield` `wolf_armor`。

⚠️ **这些 tag 对当前实现严格来说是冗余的**（不进 `hurtServer` ⇒ 没有减伤步骤被执行到）。保留的理由：伤害类型的语义应当自洽（万一将来有东西走常规流水线造成这个伤害，不该被护甲减免），且 datapack 作者读 tag 就能知道它声称什么。**曾一度多写了 `is_player_attack`/`no_knockback`/`no_impact` 三个——那不属于「穿透减伤」语义，已删，别再加回去。**

**三条随机死亡消息**：vanilla 从 `message_id` 推导消息键、不提供变体机制 ⇒ `DeathOmenDamageSource extends DamageSource` **覆写 `getLocalizedDeathMessage`**（所有死亡消息的唯一出口）。**零额外 Mixin**。

🔴 **变体在构造 source 时抽取，不在读消息时抽取**：一次死亡会调用 `getLocalizedDeathMessage` **三次**（玩家自己的战斗包、广播给其他人、命名实体的日志行），每次抽会让同一次死亡出现三种说法。

lang key `death.attack.death_omen.1/2/3`，数量由 `DEATH_MESSAGE_VARIANTS` 定义。

### 17f. 物品属性

`sword(ToolMaterial.NETHERITE, 3.0F, -2.4F)` 打底（耐久、横扫、蛛网挖掘、下界合金修复），然后**替换两处**：

- **`ATTACK_DAMAGE = Float.MAX_VALUE`**：原版 `LivingEntity#kill` 用的正是这个常量，且 `hurtServer` 会把 `Infinity` 钳到它 ⇒ 这是游戏能表达的最大伤害。传 `Float.POSITIVE_INFINITY` 无意义（会被钳成同一个值）。**实际伤害与它无关**（＝目标剩余生命值）
- **无 `ATTACK_SPEED` modifier**（整条不加，不是设 0）：攻速对别的武器有意义是因为冷却决定多少伤害能打进去；这把打的是「还剩多少」，蓄力只影响下令频率。不加 ⇒ 保持玩家基础攻速，**无后摇**
- **`component(DataComponents.ENCHANTABLE, null)` 移除附魔能力**：已追到底层验证 `component(type, null)` → `Initializer.add` → `DataComponentMap.Builder.setUnchecked` → **`map.remove(type)`**，而 `ItemStack.isEnchantable()` 第一行就是 `if (!has(ENCHANTABLE)) return false` ⇒ 附魔台与铁砧都走这个判断，**零 Mixin**。理由：值得上剑的附魔全都修改被跳过的流水线，留着是兑不了的承诺

⚠️ 曾计划用 Mixin 替换铁砧「过于昂贵」提示，**已放弃**：`TOO_EXPENSIVE_TEXT` 是 `AnvilScreen` 的 `private static final` 全局字段、不区分物品，替换会影响所有物品。

**贴图 `abyssfall:item/final_death_omen/final_death_omen`**（16×16，`parent: item/handheld`）。进常规创造栏，无 config 门禁。⚠️ **贴图与遮罩住在同名子目录里**，见「目录结构」末尾那条约定。

### 17g. tooltip 逐字波浪染色（`client/tooltip/AbyssFallTooltips`）

那一行是 `+深渊 攻击伤害` / `+Abyss Attack Damage`，用 vanilla 自己的 **`ItemAttributeModifiers.Display.override(Component)`** 替换掉数字（否则会显示 39 位数字墙）。**只改显示，attribute 实际值不动。**

**分四段拼**：前导空格、`+`、深渊、属性名。`+` 与属性名用**原版色**（`Attributes.ATTACK_DAMAGE.value().getStyle(true)` → `POSITIVE` + 增益 ⇒ `BLUE`），只有「深渊」是我们的。属性名从 `getDescriptionId()` 取 ⇒ 跟着语言走。

`+` 是**正确的 ASCII `U+002B`**（已验码点）。看起来像「十」是 MC 默认字体在该字号下的字形，**不是编码错误，别去改成别的字符**。

**波浪**：`Language.getInstance().getOrDefault(key)` 取当前语言实际文字 → **按 code point 拆成单字**（中文 2 段、英文 5 段，自动适配，不硬编码），每字相位偏移 `PHASE_STEP_PER_CHARACTER = -0.125`（负值 ⇒ 波从左往右跑）。`CYCLE_MILLIS = 3500`。灰阶范围 `#1E1E1E`–`#767676`（最亮处仍低于 vanilla `GRAY`）。余弦驱动 ⇒ 两端有停顿、无锯齿跳变，且**相位无需取模**（余弦本身周期）。用 `Util.getMillis()` ⇒ **游戏暂停时仍在动**（看 tooltip 时常是暂停的）。

物品组件里仍存静态 `#4A4A4A`（正好是动画中点），供截图/纯服务端/拿不到回调的场合。

依赖 **`fabric-item-api-v1`**（`ItemTooltipCallback`）——**fabric-api 传递依赖，无需在 `build.gradle` 声明**。

🔴 **两个已修 bug，写法刻意如此，不要"简化"回去**（类注释里也标了）：

1. **不能用 `setStyle` 改样式** —— `MutableComponent` 把渲染结果缓存在 `visualOrderText`，**只在语言变化时才重算** ⇒ 改样式不会上屏，颜色永远停在第一帧。**必须重建 Component。**
2. **更不能就地改** —— `Component.copy()` 只复制 sibling **列表**、sibling 本身是**共享引用**。创造界面用 `tab.getDisplayName().copy()` 给每个物品加标签名行（`CreativeModeInventoryScreen:718`），而本项目标签名是「深渊」「浮现」两个共享 Component 拼的 ⇒ 就地改样式会**穿过 copy 污染那两个原始实例**，导致游戏内**所有**物品的该行一起闪。

现在的写法：命中则 `MutableComponent.create(contents)` 造新的，未命中返回 `null` 让那一行保持原对象（不分配、不触碰），sibling 列表惰性复制。**新对象无旧缓存 ⇒ 颜色真的变；不写既有实例 ⇒ 不可能再污染。**


- **用户明确要求不打日志**：「我们的武器不需要跟任何人解释」


## 18. Shader 渲染系统（v1.4-Dev 新增，地基三）

**架构与禁忌读 `HANDOFF.md` 4b**，这里只记「怎么实现的」。

用户定位：**「SanCore 负责规则框架，Shader 负责物品渲染框架」**。🔴 **它不是死兆将至的专属系统**，那把剑只是第一个消费者。

### 18a. 26.2 渲染管线的四个事实（决定了整套设计）

全部用 `javap` / 源码实测，**不要凭 1.21.x 的记忆改**：

1. **无 `ItemRenderer`、无 `ShaderInstance`、无 `AbstractUniform`** —— 26.2 整套换成 `RenderPipeline` + UBO。
2. **`CuboidItemModelWrapper.validateAtlasUsage` 拒绝非图集 quad**（全库仅 2 处引用，都在该类）⇒ 高分辨率/程序化贴图**不能走普通 `layer0`**。
3. **逃生口是 `SpecialModelRenderer`** —— vanilla 自己的盾牌/三叉戟/箱子走这条路，纹理是裸 `Identifier` 而非图集精灵。`LayerRenderState.setupSpecialModel` 是 public。
4. **`SubmitNodeCollector.submitCustomGeometry(PoseStack, RenderType, CustomGeometryRenderer)`** 可提交任意 RenderType 的几何。GUI 与世界渲染都有 vanilla 用例（`GuiProfilerChartRenderer`、`BeaconRenderer` 等 15 处）。

⇒ 结论：**包装物品模型 + 追加一个 special 图层 + 自定义 RenderType**。那个 atlas 校验没有被绕过，它只是不适用于这种图层。

### 18b. 自定义 RenderType：唯一必须的 Mixin

`client/mixin/RenderTypeInvoker`，`@Invoker` 取 `RenderType.create`。

**为什么不可避免**（每一环都实测过）：
- `RenderPipeline.builder()` / `withVertexShader(Identifier)` / `withFragmentShader(Identifier)` —— **public**，接受自定义命名空间
- `RenderSetup.builder(RenderPipeline)` 及其 builder 全部方法 —— **public**
- **`RenderType.create(String, RenderSetup)` —— package-private**，descriptor 从字节码读得：
  `(Ljava/lang/String;Lnet/minecraft/client/renderer/rendertype/RenderSetup;)Lnet/minecraft/client/renderer/rendertype/RenderType;`
- `RenderTypes` 里全部 public 工厂**只产 vanilla 自己的 pipeline**
- **Fabric API 没有替代** —— `fabric-rendering-v1 25.3.2` 与 `fabric-renderer-api-v1 14.1.3` 的 `api` 包已逐个扫过，只有 `FabricRenderPipeline`（仅一个 GUI draw-mode 开关）

**曾评估的替代方案**：把类放进 `net.minecraft.client.renderer.rendertype` 包借 package 访问权。可行，但会往 vanilla 包里塞我们的文件；三行 invoker 侵入更小。用户选了 Mixin（原话「我们要保证自己的架构干净」）。

**GLSL 加载路径**：`ShaderManager.prepare` 用 `manager.listResources("shaders", ...)` 扫**全部命名空间** ⇒ `assets/abyssfall/shaders/core/*.vsh/.fsh` 会被加载，`#moj_import <minecraft:xxx.glsl>` 也可用。

**pipeline 无需注册**：`RenderPipelines.register` 是 private，但不需要它——`GlRenderPass:76` 走 `getOrCompilePipeline` **懒编译**。

🔴 **代价：静默失败。** `ShaderManager.apply` 只预编译 `getStaticPipelines()`、只为它们报错。自定义 pipeline 编译失败**不抛异常、不打日志、什么都不画**。排查「效果没出现」时先怀疑这个。

**pipeline location 必须按 effect 唯一**（用 `effect.hashCode()`）：两个只差一个 define 的 effect 是不同程序，共用 location 会让 GPU 缓存把第二个当成第一个。




### 18c. 渲染路径（`client/render/` 三个类）

```
ShaderLayerModelPlugin   modifyItemModelAfterBake 装到【所有】物品上 + bake 时取几何与图集
        ↓
ShaderLayerItemModel     每帧：先委托原模型，再问 core 要 effect
        ↓
ShaderLayerRenderer      submitCustomGeometry 画【几何源给的】quad 集合
```

**为什么装到所有物品**（不是只装配置里那几个）：provider 可能在任意一帧声明任意物品，按配置筛选会把答案固化在 bake 时。不命中时不加图层、结果等同原版。无 provider 时完全不安装。**别"优化"成预筛。**

🔴 **两个坐标系陷阱**（都踩过，见 HANDOFF 教训 34）：

1. **模型空间是 `0..1`，中心在 `(0.5,0.5)`**，不是以原点为中心。平面物品 z 在 `7.5/16 ~ 8.5/16`。~~故 `Z_PLANE = 8.5/16 + 0.002`~~ ⚠️ **`Z_PLANE` 已于 v1.5-Dev 删除**，几何改为跟随物品真实外壳，见 18h。
2. **每个图层要自己 `setItemTransform`** —— `ItemStackRenderState.submit` 逐图层套变换，新图层默认 `NO_TRANSFORM`，不设就不跟着物品转（手持时最明显）。变换取自 `ResolvedModel.getTopTransforms()`（沿父链解析，所以 `handheld` 的值是继承来的）。

**顶点必须写满 `DefaultVertexFormat.ENTITY` 的六个属性**（position/color/UV0/UV1/light/normal），顺序照 vanilla 的 `submitCustomGeometry` 调用方（如 `ExperienceOrbRenderer`）。绑定是位置相关的，少一个后面全错位。⚠️ **`UV1` 自 v1.5-Dev 起装遮罩 UV，不再是 overlay**，见 18h-2。

**GUI 缓存**：`output.setAnimated()` + `output.appendModelIdentityElement(effect)`。第二个传 effect 本身 ⇒ effect 变了就是 cache miss（教训 29 同族）。

### 18d. 🔴 颜色来源是刻意留空的接缝

用户明确要求：**「让 Shader System 不绑定任何一种颜色来源，避免以后选择方案时需要重做底层渲染系统」**。

`ShaderColorSource` 接口 + ~~**唯一占位实现**~~ `FixedColorSource`。⚠️ **v1.5-Dev 起有两个实现**：`FixedColorSource`（占位 / 现为 debug 工具）与 `DerivedColorSource`（从物品自己的贴图推色，**这条接缝的第一次真正兑现**，见 18g）。

**`FixedColorSource` 不是设计决定，是占位。** 它的限制（编译期常量、整块同色、不读原贴图）写在自己 javadoc 里并标注「这是占位的限制，不是系统的限制」。⚠️ **第三条限制已被 `DerivedColorSource` 打破** —— 它读原贴图，证明那确实是占位的限制而非系统的。

**已实测**：从外部定义一个性质完全不同的 source（贡献 `HUE_START`/`HUE_SPAN` + `COLOR_FROM_GRADIENT` 标志），零系统改动即生效，且 `COLOR_A_*` 那组 define 完全消失 —— 证明系统没有任何地方假设「颜色是两个 RGB 常量」。

**接口当前边界**：source 只能贡献编译期 define。若要「每帧变色」，改的是**这一个接口文件**（扩成也能贡献 uniform），不是渲染代码。⚠️ **但 `HANDOFF.md` 4d 实测证明不该走 uniform 而该走顶点属性** —— 动之前先读那节。

**未解决**：绿/蓝共用一个颜色 —— `opacity = continuous + sampled` 那步就把来源信息丢了，到着色时已分不清。修它必然涉及颜色方案设计，故未修。

#### 🔴 18d-2. 死兆将至的颜色是 debug 产物，遮罩定稿后连带删除

**用户原话**：「现在死兆将至还没有画出来 mask，同时当时 shader 系统刚刚解耦，必须要有一个道具用来测试，我就用的这种办法，**死兆将至相关的色彩系统压根就是 debug 下的产物**，后面遮罩画好之后这部分记得删除即可，届时也是颜色系统构思出来的时候。」

⇒ 配置文件里 `final_death_omen` 那条 entry **不写 `color` 字段**，回落到 `FixedColorSource.DEFAULT` = 红(`0xFF0000`)→蓝(`0x0000FF`)渐变。**这是调试配色，不是美术决定。**

**遮罩定稿时要一起清掉的东西**：`FixedColorSource`、以及 `masked_pulse.fsh` 里 `COLOR_A_*`/`COLOR_B_*` 那套（那行 xmap 已于 v1.5-Dev 换成 dispatch codec，不必再等）。

⚠️ **v1.5-Dev 状态更新（用户明确决定）**：`FixedColorSource` **暂不删除**。理由是它仍是 `MaskedPulseEffect` 的默认值（两处），删了没有回落值；且用户原话「**既然确定是材质问题，那么我画完新材质之后红蓝还是对比色最强的两个颜色，可以看得更清晰**」⇒ 红蓝现在是 **debug 工具**，等新美术定稿再删。

#### ✅ 18d-3. 那行 `xmap` 的写路径会 ClassCastException（**v1.5-Dev 已修**）

**已修，改成了 dispatch codec，详见 18g-3。** 下面保留原始记录，因为它解释了为什么「只有一个实现时也要用 dispatch」。

原来的 `MaskedPulseEffect.MAP_CODEC`：

```java
FixedColorSource.CODEC.optionalFieldOf("color", FixedColorSource.DEFAULT)
    .xmap(source -> (ShaderColorSource) source,   // 读：向上转，安全
          source -> (FixedColorSource) source)     // 写：向下强转，危险
```

**当时的实测结论**（JDK 25 + DFU 10.0.21 单文件验证）：

| 路径 | 结果 |
|---|---|
| 运行时用非 Fixed 的 source | ✅ 正常，defines/flags 都对，`COLOR_A_R` 确实消失 |
| 读文件 | ✅ 但**永远只能读出 `FixedColorSource`**（`optionalFieldOf` 不认的字段回落默认） |
| **写文件（`save()`）** | ❌ **`ClassCastException`** |

当时不会炸，是因为读出来的永远是 `FixedColorSource`，写回去强转必然成功 —— 那条路是死循环，走不出去。**v1.5-Dev 引入 `DerivedColorSource` 时这个循环被打破，所以先改了 codec。**

#### 18d-4. 解耦程度盘点（1.4-Dev 建立，v1.5-Dev 复测仍全部成立）

| 边界 | 状态 |
|---|---|
| `shadercore` → 任何具体物品 | ✅ 零引用（`ShaderConfigData.DEFAULT` 提到那把剑，但那是**默认值/消费方**，不是系统层） |
| `shadercore` → `client` / `net.minecraft.client` | ✅ **零引用**（v1.5-Dev 新增校验：`ShaderQuad`/`ShaderVertex` 刻意自带类型，不用 `BakedQuad`） |
| 渲染层四个类 → 效果种类 / 颜色 / 几何实现 | ✅ **零引用** `MaskedPulse`/`FixedColor`/`DerivedColor`/`ColorDerivation`/`ItemHull`/`COLOR_*`/`DERIVE_*` |
| 效果种类 → 渲染层 | ✅ 零引用 `client/` |
| `core` ↔ `shadercore` | ✅ **互不相识**（两向 grep 均为空） |

**已知的两处非缺陷**：
- `ShaderEffect.mask()` 是接口级强制 —— 每个效果都必须有遮罩。对 `masked_pulse` 合理，但**星空类程序化效果不需要遮罩**，届时要么给占位白图、要么改接口。这是设计取舍不是 bug。⚠️ **`ShaderEffect.geometry()` 刻意做成 `default` 方法就是为了不再重犯这个** —— 新效果不想管几何就不用管
- `AbyssFallPipelines.clear()` **无人调用** —— 资源重载场景需要它，加 reload 支持时记得接上


### 18e. `abyssfall:masked_pulse`（第一个效果种类）

遮罩**按通道**分工，通道**值**即不透明度（所以美术可以做渐隐）：

| 通道 | 行为 |
|---|---|
| **G** | 常驻显示 |
| **B** | 随机抽样：每轮随机选一批，持续一轮后换一批 |
| **R** | 空着，可作第三种行为 |

**时基全部挂在 vanilla 的 `GameTime` 上**（`globals.glsl` 自带，免费）。⚠️ 它是 `((gameTime % 24000) + partialTick) / 24000.0`，即 **0..1 归一化、一个 MC 日一圈**，不是秒也不是 tick。

🔴 **必须在归一化域里直接乘，不要先还原成 tick**：`floor(GameTime * 24000 / 10)` 实测 2400 个边界里有 **138 个错位**（float32 把 `110/24000*24000` 算成 `109.999992`）。正确写法 `floor(GameTime * 2400.0)`。

**实测的抽样时基质量**（0.5 秒 = 10 tick）：2400 桶全部访问、**零跳桶**、每桶 9~11 tick。抖动无害（只让切换早/晚一帧）。⚠️ 每 MC 日归零时会多一次不规则切换，20 分钟一次、持续一帧，判断可忽略。

**哈希抽样是纯函数**，无 CPU 侧状态：键 = `(像素坐标, 轮号)` ⇒ 同轮结果恒定、换轮全新一批。实测阈值 0.15 → 实际点亮率 0.1505。**像素坐标必须用 `floor(texCoord0 * MASK_RESOLUTION)`**，直接用 UV 会让单个物品像素内部出现噪点。

**`MASK_RESOLUTION` 必须可配** —— 项目里已有 16×48 的物品贴图（`san_lens.png`），写死 16 就错。

**采样器必须 `FilterMode.NEAREST`** —— 遮罩是数据不是图片，线性插值会在绿蓝边界混出中间值，造出属于任何效果的假像素。

**参数走编译期 define 的原因与代价**见 HANDOFF 4b.5。⚠️ `withShaderDefine` 只有 `int`/`float`/flag 三个重载（教训 32）。

### 18g. 🔴 颜色来源之二：`DerivedColorSource`（从物品自己的贴图推色，v1.5-Dev）

**这条是 18d 那道接缝第一次被真正用上**，也是「以后不必逐物品画遮罩」的技术前提。

用户原话：**「这个丁很重要，丁的出现可以让我们以后不用每个物品都要自己画遮罩」**。

`FixedColorSource` 把颜色**画在**物品上 ⇒ 每个要变异的物品都得有人手画遮罩。而本项目的前提是「**普通**物品随 San 下降开始变得不对」，主角是玩家背了几小时的石镐 —— 没人会给全游戏物品画遮罩。`DerivedColorSource` 读**被覆盖的那个像素**再推导出颜色，物品自己提供细节。

**四种推导**（`ColorDerivation` 枚举，`derivation` 字段）：

| 值 | 观感 | 做法 |
|---|---|---|
| `tinted`（默认） | 物品还认得出是自己，但颜色不再属于它 | 保留明暗，色相推向目标 |
| `drained` | 像被抽干了 | 先去饱和成灰再染色 |
| `inverted` | 负片 / 异界 | 亮度反相 |
| `glowing` | 物品几乎不变，只是在发光 | 原色 + 叠加辉光 |

**做成一个枚举而非四个 source**：四者输入相同、都由同一个 shader 的一个分支回答，拆开会把采样/参数/混合复制四遍去改一个表达式。三个参数 `derivation` / `color` / `strength`（`strength` 0~1 可部分应用，**这是留给 San 驱动的口子**）。

**shader 侧**：`#ifdef COLOR_FROM_TEXTURE` 大分支 + 每种推导各自 `#ifdef DERIVE_*`。**未选中的分支不进编译产物**，原来的 `COLOR_A/B` 分支一字未动。

⚠️ **`glowing` 有一个已知缺陷（v1.5-Dev 未修）**：公式是 `original + target * luminance(original)`，**发光强度与底层亮度成正比**。对近黑物品等于不发光 —— 实测死兆将至剑柄底层 luminance 仅 0.024，发光增量只有 **+3.5/255**，而人眼在深色背景上约需 +15~20 才能分辨。真要用它照亮暗色物品，得给公式加亮度底线，那会改变它对所有物品的行为，**属于数值语义、动前必须问用户**。

#### 🔴 18g-2. `Sampler0` 改绑物品图集（不再是遮罩）

**这是本轮唯一影响既有渲染的改动。**

| | 旧 | 新 |
|---|---|---|
| `Sampler0` | 绑遮罩，**从不读**（源码注释写着 bound but unread） | **物品自己的贴图**（图集） |
| `Sampler1` | 遮罩 | 遮罩（未变） |

⚠️ **物品贴图是图集不是单图** ⇒ 采样必须用**图集坐标**，喂 `0..1` 会读到整张图集（全游戏物品一起）。故 `ShaderVertex` 同时携带两套 UV，见 18h。

⚠️ **图集来源必须从 sprite 现取**（`sprite.atlasLocation()`）：多数物品在 item atlas，但**方块物品的 quad 带的是 block atlas**，绑错整个读错。且 `TextureAtlas.LOCATION_*` 三个常量在 26.2 **已 `@Deprecated`**（javap 实证），别去用它们——问 sprite 就绕开了。

⚠️ **pipeline 缓存 key 因此变成 `(effect, atlas)` 二元组**：同一 effect 贴在方块物品和普通物品上是两个 RenderType。

**采样器仍是 `NEAREST`**：物品是像素画，线性插值会造出美术从未用过的中间色，而推导会忠实地把它放大。

#### 🔴 18g-3. 那行危险的 xmap 已换成 dispatch codec

18d-3 记的那个「引入第二种颜色来源前必须先改，否则 `save()` 会 ClassCastException」的待办 —— **本轮做了**，因为第二种来源就是它等的那个。

新增 `ShaderColorSources`，与 effect 的 dispatch **同构**（`Codec.STRING.partialDispatch("type", ...)`）。⚠️ `partialDispatch` **直接返回 `Codec`，后面不要再 `.codec()`**（编译不过，本轮踩过）。

**旧文件兼容**：`LENIENT_CODEC = Codec.either(CODEC, FixedColorSource.CODEC)`，无 `"type"` 字段的旧 `color` 对象**仍按 `fixed` 读**。已实测四种推导往返 + fixed 往返 + 旧文件读取全部通过，**写路径不再抛 CCE**。

**`FixedColorSource` 保留未删** —— 它仍是 `MaskedPulseEffect` 的默认值（两处），删了那两处就没有回落值。用户明确要求「删除留到遮罩真正定稿时」，且**红蓝是对比最强的两色，debug 时看得最清**。

### 18h. 🔴 几何来源：`ShaderGeometrySource`（v1.5-Dev，取代单平面 quad）

**旧实现的错误必须记下来，因为它极易重犯：单平面 quad 假设「物品是平的」，而 Minecraft 里没有一个物品是平的。**

`ItemModelGenerator.bakeExtrudedSprite`（26.2 源码原文）给**每个**生成型物品造：

```java
Vector3f from = new Vector3f(0.0F, 0.0F, 7.5F);
Vector3f to   = new Vector3f(16.0F, 16.0F, 8.5F);
addUnculledFace(... SOUTH ...);   // 前面 @ z=8.5/16
addUnculledFace(... NORTH ...);   // 后面 @ z=7.5/16
bakeSideFaces(...);               // 逐像素侧壁，即物品的 1/16 厚度
```

⇒ **前面 + 后面 + 一圈逐像素侧壁**。旧的单平面只盖住三者之一，从任何非正视角度看都像「物品被压成一片、效果浮在旁边」——用户报的正是这个。

⚠️ **HANDOFF 4b.8 旧表述「`Z_PLANE` 假设平面物品 ⇒ 3D 物品会偏」低估了范围**：受影响的不是 3D 物品这个子集，而是**全部物品**。那条已在本轮修正。

**现在的形状**：

```
ShaderEffect.geometry()      效果说「我贴在什么形状上」（default → 物品外壳）
        ↓
ShaderGeometrySource         接口：收物品真实 quads，产出要画的几何
        ↓
ItemHullGeometry             唯一实现：跟随全部面，沿【各自法线】外推 0.002
```

**为什么是接缝而不是直接写死跟随外壳**：程序化「无限空间」那类效果**不贴合物品**（思想见 18j），它要的是投影面。若把「总是跟随外壳」写进渲染器，那种效果来时又得改渲染器。**加种类 = 加实现，不动渲染器。**

**沿各自法线外推，不沿固定轴**：固定轴对前面对、对侧壁全错（侧壁朝侧向，往镜头推等于沿物品滑动而非离开表面）。退化面（侧壁可能塌成线）法线为零 ⇒ 原地不动，**这是正确的省略**，无面积的面本来什么都不画。

**几何在 bake 时解析一次并持有**，不是每帧：几何属于模型，模型重烘焙时 wrapper 本来就会重建。每帧解析等于每秒 60 次相同工作 + 每次一个 list 分配，在渲染路径上、对屏幕上每个物品。

**取几何的途径**：`resolved.bakeTopGeometry(resolved.getTopTextureSlots(), baker, BlockModelRotation.IDENTITY).getAll()`。⚠️ 用 `IDENTITY` 是因为**显示变换在 submit 时逐图层套**（来自 `ItemTransforms`），这里再烘一次旋转会**套两遍**。

#### 18h-2. ⚠️ 顶点格式只有一个浮点 UV 槽，遮罩 UV 挤在 `UV1`

`DefaultVertexFormat.ENTITY` 的 `UV0` 是 `RG32_FLOAT`，而 `UV1`/`UV2` 是 **`RG16_SINT`**（26.2 源码实证）。现在要两套坐标，只能有一套走整数：

| 属性 | 装什么 | 理由 |
|---|---|---|
| `UV0`（float） | **图集坐标** | 必须精确，差一点就读到隔壁物品的像素 |
| `UV1`（16bit int） | **遮罩坐标** | 量化无害，见下 |

**已实测精度**：`FIXED_POINT_SCALE = 32767`，16px 遮罩每像素 **2048 个量化级**、128px 仍有 256 级，最坏往返误差 **0.00024 个像素**。

⚠️ **`FIXED_POINT_SCALE` 在 Java 与 `.vsh` 里各写一份，改一个必须改另一个。**

**用 `setUv1` 而非 `setOverlay`**：后者把一个 int 拆进两半，无法独立设两个分量。`UV1` 本来就闲置（这一层一直写 `NO_OVERLAY`、shader 从不读）——**又一次「格式声明了 ≠ 有人在用」**（HANDOFF 教训 36 同族）。

### 18i. 🔴 物品贴图的 alpha 必须只有 0 或 255（v1.5-Dev，血泪）

**这是「厚度看不见」问题的真正原因，与 shader、颜色、几何全都无关。**

`SpriteContents.isTransparent` 是 **`ARGB.alpha(pixel) == 0`**（26.2 源码原文，严格判断）。**alpha ≥ 1 就算实体**，参与建几何。

⇒ 一个 `alpha = 1` 的像素（0.4% 不透明，肉眼绝对看不见）会**长出侧壁**。后果：

| | 实际 |
|---|---|
| 玩家**看见**的轮廓 | 由 alpha 高的像素构成 |
| vanilla **建几何**的轮廓 | 由 alpha ≥ 1 的像素构成，**大一圈** |

**旧贴图实测**：83 个 alpha 1~254 的像素 ⇒ 116 个侧壁里 **76 个（65.5%）长在 alpha 均值仅 6.6/255 的像素上**。那圈厚度无论给什么颜色、多亮的光，乘上 2.6% 不透明度之后都是看不见的。

⇒ **原版的那把剑本身就没有可见厚度。** 之前几轮一直在给一圈本来就不可见的几何上色。

**解法只有一个，且不在代码里**：`make-death-omen-texture.ps1` 把 alpha 二值化（`≥128 → 255` 保留原色，`<128 → 全透明`）。处理后：

| | 前 | 后 |
|---|---|---|
| 幽灵像素 | 76 | **0** |
| 侧壁总数 | 116 | 98 |
| **侧壁长在可见像素上** | 40（34.5%） | **98（100%）** |

**代价**：边缘变成硬像素阶梯。**但那就是原版画风** —— vanilla 物品贴图从不用半透明像素，REFERENCE 11 早就记了「任何抗锯齿都会把 1px 笔画糊成灰」。

🔴 **以后新增任何物品贴图，先验 alpha 分布。** 一条命令的事：

```powershell
Add-Type -AssemblyName System.Drawing
$b=New-Object System.Drawing.Bitmap((Resolve-Path 'xxx.png').Path)
$semi=0; for($y=0;$y -lt $b.Height;$y++){for($x=0;$x -lt $b.Width;$x++){
  $a=$b.GetPixel($x,$y).A; if($a -gt 0 -and $a -lt 255){$semi++} }}
"半透明像素 = $semi （应为 0）"; $b.Dispose()
```

#### 18i-2. debug 遮罩按分区画，不按亮度猜

`make-death-omen-mask.ps1` 现在按**几何分区**：`rows 0..4` = 剑刃 → 蓝（抽样闪烁）；`rows 5..15` = 护手与柄 → 绿（常驻）。轮廓覆盖 **57/57**。

**为什么不按亮度**：上一版按「亮度 ≥ 68 = 剑身」分，那对旧贴图成立（亮剑身 + 近黑剑柄，双峰分明）。新贴图**亮度中位数只有 16、无第二个峰**，任何阈值都会把 89 个像素归一边、14 个归另一边 —— 这个办法失效了。**剑的形状是可靠信号，亮度不是。**

⚠️ **这张遮罩是 debug 工具不是美术**，目的是让「厚度有没有出现 / 哪个通道驱动哪里 / 抽样是否真在闪」一眼可辨。用户明确说「你自己随便画一张，目的是 debug」。

⚠️ **debug 时把 `sample_density` 调到 0.85、`sample_period_ticks` 调到 20**（默认 0.1 / 10）。**10% 可见率下根本看不出抽样规律** —— 任一瞬间只有一两个像素在亮，形不成可判断的图案。

🔴 **v1.6 起 18i-2 描述的分区遮罩已不存在**。当前遮罩是「线稿内部纯红」，G/B 归零 ⇒ `masked_pulse` 在它上面完全透明。要回到分区遮罩得改回生成脚本。



## 18j. 「有限平面上的无限空间」——下一版 Shader 的核心思想（v2.0-Dev 重写）

> 🔴 **这一节取代了旧的「星空效果」全部内容。** 旧的 18j-1～18j-20（移植自 Avaritia `cosmic.frag` 的那套）**已从本文档删除**。原因见 `HANDOFF.md` 教训 50：那段东西是把一具 14 年前的尸体移植到 26.2，我们要的不是尸体，是它的思想。

### 我们真正继承的东西：一个思想，不是一套代码

参考实现唯一不可替代的贡献，是这个问题本身：**「如何在一个有限的二维平面上，渲染出看似无限大的三维空间。」**

答案是一道数学题：**把每个 fragment 当作一条从观察者射出的射线（球面射线），用它去模拟一个无限空间。** 步骤——

1. 把 fragment 的屏幕坐标当作一条射线的方向；
2. 随观察者朝向旋转，映射到一个**球面**上（球面自封闭，永远走不到边）；
3. 把球面划成网格，用伪随机决定哪些格子里有东西；
4. 多层网格沿各自轴堆叠，制造**视差**（近处层掠过得快、远处层慢）。

⇒ **没有贴图要拼接、不会重复、没有接缝、任意分辨率代价相同。** 这就是「无限感」的全部来源，也是下一版效果必须实现的核心。**这几条数学关系不可替代（教训 50 的第 2 问）。**

### 🔴 参考「尸体」前必须回答的七个问题（教训 50）

1. 这个实现最终解决什么问题？
2. 哪些数学关系是不可替代的？（= 上面那几条射线/球面/网格/多层）
3. 哪些代码只是历史 API 适配？（1.12.2 固定管线光照、`ShaderInstance`/`AbstractUniform` 等——26.2 全没了，见教训 33/48）
4. 哪些参数只是视觉设计？（`lightmix=0.2`、16 层、101 格测试、十张素材——都可调可丢）
5. 哪些数据结构是旧架构遗产？（独立纹理遮罩、每帧上传 uniform 矩形等）
6. 哪些地方有明显 bug？（旧散列 `rand2d` 对 π 取模、`pow` 溢出 float32、层步长碰撞 35.2% 等——**全是尸体自带的，不是我们的 bug**）
7. 完全不管原工程结构，现代环境该怎么重写？

### 26.2 下这套思想的落地框架（已建好，照用）

旧实现能跑，已验证下面这些**框架事实**——它们与旧算法无关，下一版直接用：

- **效果 = 一个 record + 一个 GLSL + 注册**（见 18a–18c）；每帧决策、几何来源、颜色来源、遮罩机制全部现成。
- **每帧变化的值走顶点属性，不走 define/uniform**（`HANDOFF.md` 4d / 教训 35）；配置态常量才走 define。
- **遮罩当图集精灵**而非独立纹理，才能播动画、才能被递归收录。
- **几何取 ±Z 两面 + 只取基础层**（`ItemFacesGeometry`，v1.9 加了图层去重，见 `ShaderQuad#BASE_LAYER`），不取侧壁。
- **深度偏移用正数**（coplanar 几何）；**资源重载窗口期不许建 pipeline**。
- **只有遮罩红通道决定不透明度**（`mask.r <= 0 → discard`）。

### 🔴 素材分辨率不再是限制（v2.0 用户实测）

旧文档里「素材建议 64×64、警惕图集爆掉」**作废**。用户实测：**素材多大都无所谓，2048×2048 × 10 张一起渲染，帧率代价不到 10 fps。** 旧实现用低分辨率纯粹因为 2012 年那是极限。⚠️ 仍要守的硬约束只有：**`.png`、正方形帧、总高是帧高整数倍、数量与 shader 分支数一致**；图集 `GL_MAX_TEXTURE_SIZE` 仍是物理上限（现代卡 16384+）。

### 现状与下一步

- **`abysseffect`（死兆将至）与 `cosmic`（寰宇支配之剑）现在都跑旧移植算法。它们留在仓库里不动**——能跑、无害、bug 全继承自尸体，不挡路。
- **下一版效果**：只带「球面射线模拟无限空间」这条思想，用 26.2 框架**从零写新 shader**，不看旧工程结构。**不是现在的任务，等立项。**


## 19. 自有稀有度：Abyssal / Infinity（v1.8-Dev 新增）


`item/AbyssFallRarity`，两级：**`ABYSSAL`**（物品名灰阶逐字波浪）+ **`INFINITY`**（固定 `§c` 红）。**当前只改物品名颜色，无其它作用** —— 用户明确要求本轮只做这个，别自行加掉率/排序/tooltip 行等语义。

🔴 **不是新增 `Rarity` 枚举值，做不到**：vanilla `Rarity` 是 enum、四值、构造器 private（26.2 源码实证）；且它是网络与存档格式（`STREAM_CODEC` 传 ordinal、`CODEC` 读 name），加值会让不认识它的客户端/存档误读每个物品栈。

⇒ **旁表 + 覆盖显示**：`IdentityHashMap<Item, AbyssFallRarity>`，`assign()` 声明 / `of()` 查询。**不做 data component**（那会让稀有度进每个栈、上网络、写存档，而它只是给名字上色且同物品恒定）。物品**仍声明 vanilla rarity**，那是回落色。

**颜色常量都在 `AbyssFallTooltips`**，与「深渊」那行的四个常量**完全独立**（用户明确要求以后能分开调）：`RARITY_CYCLE_MILLIS=3500` / `RARITY_STEP_PER_CHARACTER=-0.045` / `RARITY_DARKEST=0x1F1F1F` / `RARITY_LIGHTEST=0xB4B4B4` / `INFINITY_NAME_COLOR=ChatFormatting.RED`。

- **步长比属性词小得多**（-0.045 vs -0.2）：物品名可长达二十字，用属性词的步长会绕好几圈、碎成不相干的色块。
- **灰阶比 `DARKEST`/`LIGHTEST` 宽**：那两个刻意压在 vanilla `GRAY` 以下以免和名字抢，而这**就是**名字。最亮 180 已略高于 `GRAY`(170)。
- ⚠️ **`ABYSSAL` 曾叫 `ABYSS`**（v1.8-Dev 当轮改名）。改名时只动了枚举值，`ABYSS_WORD_KEY` / `ABYSS_FLOWER` / `abyss_dirt` 是无关的同名物，**别一起改**。

**当前归属**：死兆将至 = `ABYSSAL`，寰宇支配之剑 = `INFINITY`。

### 19a. 🔴 手持提示要 Mixin，tooltip 不要（第四个 Mixin）

**同一个需求在两处的答案不同，这是关键。**

| | tooltip | 快捷栏上方手持提示 |
|---|---|---|
| 入口 | `ItemTooltipCallback` 交出 `List<Component>` | **无** |
| 做法 | 改 line 0（`getStyledHoverName()` 就是它） | `client/mixin/HudSelectedItemNameMixin` |

**为什么非注入不可**：`Hud.extractSelectedItemName` 是 **private**、组件建在局部变量、读两个 private 字段（`toolHighlightTimer`/`lastToolHighlight`）、算完直接送 `textWithBackdrop`，全程零暴露。`fabric-rendering-v1` 只能加/换**整个** HUD 元素，而这段文字属于 vanilla 元素 ⇒ 换掉它等于为一个颜色重写 vanilla 的计时、淡出与定位。

**注入点是字节码选的**（26.2 实证）：
```
24: ItemStack.getHoverName()      <- @WrapOperation 包这里
27: MutableComponent.append(...)      名字成为 sibling
40: MutableComponent.withStyle(...)   rarity 颜色只作用于【根】
```
⇒ **只需让 `getHoverName()` 返回带色组件，sibling 的颜色胜过根**。这正是创造标签双色标题依赖的既有机制（见 1），复用而非重新发现。**vanilla 的计时/淡出/宽度/定位一行未改。**

- `@WrapOperation` 包**一个调用点**，不是整个方法 ⇒ 全游戏其它成千处 `getHoverName()` 不受影响。
- 两处共用 `AbyssFallTooltips.rarityName()` + `nameClock()`（都 public），**波浪在两处永不失步**。
- `analyze_mixin` 报 `isValid: true`、零 error 零 warning。

### 19b. 致敬碑文（`SwordOfTheCosmosTribute`）

寰宇支配之剑的 tooltip 追加用户亲笔的十五行致敬（Avaritia / Morpheus1101）。🔴 **文案是用户的，一字不能改**；这个类只管排版与配色。

**默认收起**，只显示一行 `按住 Shift 阅读碑文`（lang key `...tribute.hint`）；按住 Shift 铺开 25 行。**`Minecraft.hasShiftDown()`** 直接查两个 Shift 键的物理状态（340/344），不存状态、下一帧即生效，vanilla 自己在 tooltip 期间也读它（`ExtendedView`）。

**排版**：空行分五段；`§7` 灰=叙述、`§8` 斜体=引语、**`§c` 红只给三句**（两个转折 + 收束句，用剑名同一个红）、Avaritia 引文缩进两格（tooltip 无边距，缩进只能进文本）。

🔴 **排版不写进 lang 文件**：`§` 代码能用，但那会把排版塞进可翻译文本 —— 翻译者得原样保留 `§7`/`§o`，漏一个就静默变样。**lang 只承载文字。**

⚠️ **没用 `appendHoverText`**（26.2 **已 `@Deprecated`**）、**没用 `ItemLore`**（它强制套 `DARK_PURPLE`+斜体，见 `ItemLore.LORE_STYLE`）⇒ 走已有的 `ItemTooltipCallback`，**零新增 Mixin**。

**追加在染色 pass 之后**：碑文样式已终、无可被匹配的词，放前面等于让全游戏每个 tooltip 白走二十多行。

## 20. 寰宇支配之剑 `abyssfall:fake_infinity_sword`（v1.8-Dev 新增）

**Shader 系统的第二个消费者，纯外观剑。** 渲染与死兆将至同构（配置里第二条 effect entry），但用的是 `cosmic` 效果种类（旧移植星空算法，见 18j）。

**属性只有一条**：`ATTACK_DAMAGE` modifier = **0.0**（tooltip 显示 `1`，即玩家空手值）；**`ATTACK_SPEED` 整条不加**（不是设 0 —— 设 0 会显示「4 攻击速度」，比 1.6 更难看）。与死兆将至同一做法（见 17f）。

🔴 **属性必须显式替换，不能靠 `sword()` 的 baseline 凑**：`sword(material, dmg, spd)` 会把材质的 `attackDamageBonus` 加到伤害 baseline 上（NETHERITE = +4.0）⇒ 传 `0.0F` 实测得 modifier 4.0、tooltip 显示 5.0。**实测过**（真实 classpath）：显式写 modifier 才得 1.0/1.6，而原版下界合金剑同法算得 8.0/1.6（对照正确）。

⚠️ **`sword(...)` 那两个参数因此是死的**，留着只为它顺带给的耐久/修复/附魔/横扫/蛛网规则 —— 没有不带 baseline 的重载。

**tooltip 那行是 `+无限 攻击伤害`**，机制照抄 17g（`Display.override` + 逐字波浪），只有两处不同：
- 走 `INFINITY_WORD_KEY`，客户端按 key 分派到**彩虹**调色板（`Mth.hsvToRgb`）
- 🔴 **彩虹的步长与周期都不能与灰阶共用**：灰阶由余弦驱动**会折返**，相位差一整圈看起来只是「波过去了」；**色相不折返，它绕回自身** ⇒ `-0.2 × 8` 字母跨 1.4 圈，实测第 6 个字母与第 1 个**字节完全相同**。故 `HUE_STEP_PER_CHARACTER = -0.1`（跨 0.7 圈、最近一对 RGB 距 91）、`RAINBOW_CYCLE_MILLIS = 500`（**用户实测后自己定的值**）。

🔴 **`Mth.hsvToArgb` 对负 hue 会抛异常**（首行 `(int)(hue*6)%6` 得负数 → 落进 `default` → throw，已实测）。而负相位是**常态**（步长为负）⇒ 必须 `Mth.positiveModulo(phase, 1.0F)`，**这不是防御性代码**。

**贴图与遮罩是死兆将至的独立副本**（同像素、两套文件）：effect 身份含 mask，共享会把两把剑的美术永久绑死。代价是多编译一条 pipeline。⚠️ **v1.9-Dev-Fix 起两套各住自己的子目录**（`item/fake_infinity_sword/…`），见「目录结构」末尾那条约定。



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
