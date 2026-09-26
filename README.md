# AbyssFall

一个以**理智**与**感知**为核心的 Fabric 模组。

深渊不只是危险：随着 San 值下降，熟悉的方块、物品与环境会逐渐失真，而没有两个玩家会
看到同一个深渊。San 衡量的不是你还能撑多久，而是这个世界你还有多少能够相信。

**为普通玩家添加有限的内容，为整合包作者夯实足够坚实的地基。** 本模组自己只做一小撮
经过打磨的内容，但每一项机制同时都是一个对外开放的接口——**AbyssFall 的下限由本模组
负责，上限由整合包作者决定。**

| 项 | 值 |
|---|---|
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.158.0+26.2 |
| Loom | 1.17.19（`net.fabricmc.fabric-loom`） |
| Java | 25 |
| 许可 | GPL-3.0-or-later |

## 设计信条

这个 mod 只有一条架构纪律：**每个子系统都不知道自己被谁使用。** San core 只管数值，
HUD、药水效果、物品各自直连它、互不认识；新增系统不改旧系统。加行为时的判断顺序是
自有数据结构 → Fabric API → Mixin，全项目 10 个 Mixin（主端 7 + 客户端 3）都是确认
API 无缝可借之后的决定，理由写在各自的类文档里。

这条纪律对使用者的意义很实际：**几乎所有机制的门都开在 vanilla 原生面上**——tag、
数据包、效果、命令、Fabric 事件——而不是开在我们自造的私有格式上。你会在下面的章节
反复看到这一点。

## 写给玩家：这个 mod 里有什么

本模组对「直接玩法内容」刻意保持克制。以下是全部。

### San 值系统

San 是贯穿整个模组的变量，其他一切都围绕它运转。每位玩家携带一个理智读数与一个属于
自己的上限。这个值由服务端权威管理，随存档持久化，死亡重生后不会被洗白，并且只同步
给玩家本人。

- **San 是连续参数，不是分档状态机。** 0%～100% 之间每一点都是不同的处境：从 80%
  掉到 79.9% 也是一次真实的变化，足以触发对应的反应。系统不预设任何「阶段」，每个
  功能自己决定是平滑缩放还是有自己的界线。
- **内部连续，外部模糊。** 玩家在游戏内永远只能得知百分比，不会看到底层数值。这是
  刻意的玩法设计而非技术限制：「能知道多精确」本身就是可以争取的游玩内容。
- **深渊需要被唤醒。** 新玩家的 San 系统是未激活的：数值在，但世界不能动它、HUD 也
  不显示。第一次吃下深渊之花，深渊才开始注视你。

### 理智的显示

- **两种 HUD 读数**：十格图标行（默认，与饱食度并列）与紫色百分比进度条。满值时
  完全不显示、也不占用垂直空间，掉落后出现，回满约一秒后淡出。
- 完整的动态反馈：低理智时持续抖动（越低越快）、失去时全排颤动一下、恢复时从左到右
  的行波、回满时闪光。抖动与行波的节奏取自原版饱食度与生命值的行为，玩家不必重新
  学习。
- **认知窥镜** —— 手持右键在两种读数间切换，并短暂强制显示读数。

### 理智的变化

- **精神崩溃 / 精神饱满** —— 一对方向相反的药水效果，每 10 秒按上限的百分比扣除
  或回复理智。五个等级分别是 1% / 2% / 4% / 8% / 12.5%。
- 和平难度会拦下崩溃，但不拦饱满：选择和平的玩家表明了不想承受消耗战，而恢复不属于
  消耗。

### 深渊之花线

- **深渊污泥** —— 行为与泥土完全一致，只是凋零玫瑰可以种在上面。自然生成于丛林、
  沼泽与红树沼泽的地下（Y≤48）。
- **深渊之花** —— 史诗品质。用骨粉催熟种在深渊污泥上的凋零玫瑰，即献祭玫瑰、结出
  此花（原版无法用骨粉催熟凋零玫瑰，这是唯一制作途径）。催熟特效刻意避开骨粉的绿色
  粒子，改用灵魂主题，一低一高两声音效读作「付出 → 到来」。
- **吃花是深渊的入口。** 第一次吃唤醒 San 系统（不增减数值），之后每一朵使 San 上限
  +0.7。
- **金透镜** —— 合成材料。**染色玻璃板** —— 不透光的紫色玻璃板，可合成。

### 深渊元素与终焉死兆

- **深渊元素（Abyssdium）** —— 毕业材料定位，当前没有获取途径。它是「不毁」的天然
  持有者：任何死法——伤害、爆炸、`/kill`、五分钟消失、坠入虚空——都不能销毁它，
  虚空会把它还给丢下它的人。
- **终焉死兆（Final Death Omen）** —— 毕业武器。同样不毁；它的攻击不由伤害管道
  结算，而是一击即死的深渊裁决，受害者得到三句随机讣告之一。拔出它的玩家会使天空
  变暗——同图所有人都能看见死兆。
- 深渊质物品拥有独特的「深渊」稀有度：物品名呈灰色逐字波浪。

### 其他

- **战利品注入** —— 深渊之花会出现在 18 张高价值结构宝箱表中，概率可配。追加独立
  奖池，不改动任何既有条目，因此不会破坏数据包或其他模组写的内容。
- **深渊探索者** —— 持有此效果的玩家开箱时，必定额外获得一朵深渊之花。
- **四个成就**，链式解锁：初获深渊之花 → 挖到深渊污泥 → 催熟第一朵花 → 第一次品尝。
- **启动协议** —— 首次加载时的双语告知弹窗。
- **开发者工具** —— `/san` 调试指令与开发者物品栏，默认不注册，需在配置中显式开启。

## 写给整合包作者：不写代码的扩展面

这一章的每样东西都可以直接抄进你的数据包或配置。所有 id 都真实存在于当前构建。

### 用 tag 给任何物品装上深渊的机制

本 mod 的机制判定读的是 tag，不是物品名单。你的数据包放一个同名 tag 文件即可并入
（vanilla 默认合并多个数据包的同名 tag）：

```json
// data/abyssfall/tags/item/abyss_gazing.json
{
  "values": [
    "mymod:ancient_relic"
  ]
}
```

这一行 JSON 让 `mymod:ancient_relic` 立即获得「不毁」：火烧、爆炸、`/kill`、五分钟
消失都杀不掉它，掉进虚空会被送回主人的物品栏。可用的 tag 全集：

| Tag | 类型 | 装上之后的效果 | 默认成员 |
|---|---|---|---|
| `abyssfall:abyss_gazing` | item | 不毁（全死法豁免，虚空回栏） | 深渊元素、终焉死兆 |
| `abyssfall:abyss_striking` | item | 攻击变成即死裁决，受害者读你可配置的通用讣告 | 终焉死兆 |
| `abyssfall:abyssdium_tool_materials` | item | 深渊质装备的修复材料（默认为空 = 不可修复；填入即生效） | 空 |
| `abyssfall:incorrect_for_abyssdium_tool` | block | 深渊质工具挖它不产生掉落（默认为空；填入即生效） | 空 |
| `minecraft:supports_wither_rose` | block | 允许种凋零玫瑰，即可建深渊之花生产线 | 深渊污泥 |
| `minecraft:mineable/pickaxe`、`.../shovel` | block | 本 mod 方块的采掘工具归属 | 染色玻璃板、深渊污泥 |
| `minecraft:bypasses_*`（共 8 个） | damage_type | `death_omen` 伤害穿透护甲/护盾/附魔/抗性/无敌帧等 | `abyssfall:death_omen` |

两条组合规则：

- 挖掘资格 = 在 `abyss_gazing` 里 ∧ 在 vanilla 的镐/铲/斧/锄类 tag 里。两者都满足
  的工具可以缓慢掘进基岩类方块（生存模式），基岩被破坏后掉落自身。给你的自定义镐
  打上这两个 tag，它就是一把深渊质工具。
- 绝杀的讣告分两套：终焉死兆自己杀人用它专属的三句台词；其他 `abyss_striking` 成员
  杀人用配置里的通用池（见配置章），不会冒死兆的名。

### 伤害类型可以在任何 JSON 里引用

`abyssfall:death_omen` 是普通数据文件，`/damage`、自定义战利品表、附魔效果等任何
需要伤害类型的地方都能直接用。它在 8 个 `bypasses_*` tag 里的成员资格也可以用你的
数据包增删。

### 战利品：往哪张表塞花，你说了算

`loot.target_tables` 接受任意战利品表 id——vanilla 的、其他 mod 的、你自己新建的、
钓鱼或以物易物这类非宝箱表都行。注入永远以新的独立奖池追加，不动表里已有的任何条
目，所以和你的数据包、其他 mod 的改写不冲突。配置了但不存在的表会在加载结束时写进
日志警告，不会静默失败。

### 配置文件：`config/abyssfall/abyssfall.json`

| 键 | 默认 | 含义 |
|---|---|---|
| `developer.dev_tools` | `false` | 注册开发者创造标签及其内容物 |
| `developer.dev_command` | `false` | 注册 `/san` 命令树 |
| `hud.show_below_percent` | `100.0` | San 百分比低于该值才显示读数；`0` = 永不显示 |
| `loot.flower_chance` | `0.05` | 深渊之花在目标表中的概率，`[0, 1]` |
| `loot.target_tables` | 18 张高价值宝箱表 | 注入目标，任意战利品表 id |
| `visuals.bloom_particle_scale` | `1.0` | 开花粒子数量倍率，`[0, 2]` |
| `visuals.bloom_sound_volume` | `1.0` | 开花音量倍率，`[0, 2]` |
| `san.peaceful_prevents_loss` | `true` | 和平难度阻止世界侵蚀 San |
| `striking.death_message_N` | 一条 | 绝杀通用讣告池。`N` 从 1 起没有上限，击杀时随机取一；`%1$s` = 受害者，`%2$s` = 行刑者 |

容错行为：文件整个解析失败 → 原文件改名备份为 `abyssfall.json.broken-<时间戳>` 并
写入默认值；单个块解析失败 → 只有那个块回落默认，其余键照常生效，文件原样保留。
缺字段一律回落默认值，旧版本写的文件永远能继续用。

### 用效果驱动 San：数据包侧的正确杠杆

三个药水效果就是三根现成的 San 杠杆，任何能发效果的机制（任务奖励、区域诅咒、BOSS
光环、自定义道具）都能直接驱动 San，不用写 Java：

```
/effect give @p abyssfall:san_breakdown 30 2
```

这条命令给玩家 30 秒 III 级精神崩溃：每 10 秒扣除上限的 4%，共 3 次，合计约 12%。
等级与速率的对应是 I~V 级 = 每次扣上限的 1% / 2% / 4% / 8% / 12.5%。反向恢复用
`abyssfall:san_spirited`（同一张速率表），`abyssfall:abyss_explorer` 则是开箱必得
深渊之花的增益。

### `/san` 命令：先说清谁能用

`/san` 有 `query` / `set` / `add` / `max set` / `max add` / `restore` / `reset` /
`on` / `off` 共九类操作，两道门：**`dev_command=true` 才注册**，且全树要求 **3 级
权限（`LEVEL_ADMINS`）**。这意味着：

- 可以用：服务器控制台、RCON、以服务器身份执行命令的脚本引擎（4 级）。
- **不可以用：命令方块、命令方块矿车、数据包函数**——它们在 vanilla 里一律只有 2
  级权限（`GAMEMASTER`），且 `/function` 会把调用方的权限压到不超过 2 级，管理员
  手动跑也一样。数据包侧请用上面那三根效果杠杆。

### 激活开关是留给你的进度门

新玩家的 San 系统默认**未激活**：数值在，但一切写入（包括效果）都会被拒绝，HUD 也
不显示。默认流程是吃下第一朵深渊之花唤醒。你可以用 `/san on` / `/san off`（控制台
或服务端脚本）自己决定深渊何时开始注视某个玩家——`off` 是冻结并保留读数，不是重置。

## 写给 mod 作者：为 AbyssFall 写扩展

**我们欢迎并鼓励社区为 AbyssFall 写扩展 mod、联动 mod、魔改 mod。** 如果你在做这样
的东西，这一章是给你的；卡住了或者有想要的能力，来
[Issues](https://github.com/Kainy030/AbyssFall-Fabric/issues) 说一声。

### 为什么成本比看起来低

- **26.2 没有混淆。** Minecraft 从 26.1 起不再混淆，Fabric 也不再维护第三方映射。
  你引用 AbyssFall 的类和引用 vanilla 的类一样，没有映射层，没有重映射，名字就是
  最终名字。
- **入口全是 `public static`。** San 系统的全部能力在
  `com.abyssfall.core.AbyssFallCoreSystem` 一个类里，不用找服务加载、不用翻注册表。
- **Javadoc 就是 API 文档。** 每个公开类和公开方法都有完整的行为契约，Releases 里
  的 `abyssfall-doc.jar` 是完整的文档站。源码本身（GPL）也随时可读。

### 依赖方式

目前没有发布到 maven（见「诚实的现状」）。从
[Releases](https://github.com/Kainy030/AbyssFall-Fabric/releases) 下载 jar 放进
你项目的 `libs/`，然后：

```groovy
implementation files("libs/abyssfall.jar")
```

（26.2 的 Loom 没有 `modImplementation`——没有映射就没有重映射，那个配置不复存在。
）运行时依赖照常在 `fabric.mod.json` 里声明 `"abyssfall": ">=2.5"`。

### 五个能直接抄的例子

监听 San 变化，在跌破 20% 的那一刻做一次事（事件在服务端、值已落库后派发）：

```java
SanChangedCallback.EVENT.register(change -> {
    if (change.crossedDown(0.20F)) {
        ServerPlayer player = change.player();
        // 你的逻辑：施加诅咒、刷怪、放音效……
    }
});
```

让世界侵蚀 San（统一入口，自动遵守「和平难度不侵蚀」配置）：

```java
AbyssFallCoreSystem.erode(serverPlayer, 5.0F);
```

把你 mod 的物品变成「不毁」（火烧、爆炸、`/kill`、消失、虚空全部豁免）：

```java
NeverDestroyed.INSTANCE.grant(stack -> stack.is(MyItems.ANCIENT_RELIC));
```

让你的物品名带上深渊的灰色逐字波浪：

```java
AbyssFallRarity.assign(MyItems.ANCIENT_RELIC, AbyssFallRarity.ABYSSAL);
```

客户端读本地玩家的 San（已自动同步到本人客户端），驱动你自己的渲染：

```java
Player self = Minecraft.getInstance().player;
SanState state = self == null ? null : self.getAttached(AbyssFallCoreSystem.SAN);
if (state != null) {
    float ratio = state.ratio();
}
```

写操作只收 `ServerPlayer`，且玩家未激活时一律拒绝——如果你的 mod 想掌管某个玩家的
唤醒时机，自己调 `AbyssFallCoreSystem.activate(serverPlayer)` 即可，之后一切正常。

完整能力清单：读（`getCurrent` / `getMax` / `getRatio` / `getPercent` /
`getSilently`）、写（`setCurrent` / `addCurrent` / `setMax` / `addMax` / `restore`
/ `reset`）、规则化写（`erode` / `canErode`）、激活（`isActivated` / `activate` /
`deactivate`）、事件（`SanChangedCallback` 带 `crossedDown` / `crossedUp` /
`ratioDelta` 等工具；`SanAccessedCallback` 在玩家查看自己 San 时派发）。

### 硬边界：几件不跟我们合作就做不到的事

这些是设计决定，不是遗漏。列出来免得你白试：

- **读 attachment 需要编译期依赖。** Fabric 的 attachment 注册表没有按 id 反查的
  API，所以「零依赖读 San」不存在——按上面的方式依赖 jar，或者用你自己的包发数据。
- **客户端看不到其他玩家的 San。** 同步只发本人（刻意的隐私设计）。服务端代码不受
  此限，想看谁看谁。
- **不能往我们的物品机制清单里注册新机制。** 「不毁」可以授予任何物品，但机制列表
  本身（`ItemMechanics`）是封闭的。你想要自己的机制，按同样的形状自己写一个即可，
  我们的实现（`itemmechanismruntime` 包）可以当范本。
- **`/san` 命令有 3 级权限门槛**（见整合包章），你的 mod 要替玩家调 San，走
  `AbyssFallCoreSystem` 而不是命令。

### 许可与扩展的分发

AbyssFall 是 GPL-3.0-or-later。分发与它静态链接的扩展 mod 时遵守 GPL 即可；只用
数据包/tag/命令面交互的作品不受任何约束。

## 诚实的现状

不写场面话，几件你迟早会发现的事先放在这里：

- **没有 maven 仓库**，发布只有 GitHub Releases。扩展 mod 的编译期依赖目前只能本地
  jar。有实际需求就到 Issues 里说，人多就发布。
- **版本号还是 Dev 阶段**（当前 `2.5-Dev-Fix`）。San core 的 API 自引入起保持向后
  兼容、行为契约写死在 javadoc 里，但公开面整体尚未冻结——2.2 曾整套移除 shader
  系统。跨版本升级前看一眼 Release Notes 是明智的。
- **26.2 上目前没有 KubeJS**（Modrinth 上其 Fabric 构建没有 26.2 版本）。所以本文档
  通篇不假设任何脚本引擎存在：数据包 + 命令 + 效果就是今天可用的全部魔改面。哪天有
  引擎登陆 26.2，tag/效果/命令这些面即插即用；想自己写脚本绑定的人，上一章的 Java
  API 就是现成的绑定层。
- 这个 mod 的「内容少」是产品决定，不是烂尾。玩家体验的下限由我们负责，上限由整合
  包作者负责——我们把精力花在地基上，也接受因此显得「小」。

## 构建

```bash
./gradlew build          # 编译并打包
./gradlew releaseJars    # 汇总三个发布产物到 build/release/
```

需要 JDK 25。产出 `abyssfall.jar`、`abyssfall-source.jar`、`abyssfall-doc.jar`。

Minecraft 从 26.1 起不再混淆代码，Fabric 也随之停止维护第三方映射，因此本项目不声明
任何 mappings，构建也没有重映射步骤——产出的 jar 就是能直接运行的那个。

## 下载

见 [Releases](../../releases)。

## 分支

`main` 是唯一在开发的分支。[`1.21.11`](../../tree/1.21.11) 保存着本项目在 Minecraft
1.21.11 上的最终状态，已停止维护，仅作存档。

## 许可

GPL-3.0-or-later，详见 [LICENSE](LICENSE)。
