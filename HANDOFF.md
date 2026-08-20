# AbyssFall 项目交接提示词

> 把下面全部内容作为新对话的第一条消息发出即可。
>
> **这个文件属于你**。它是上一个你留给你的，你也应该在本次工作结束时更新它留给下一个你。
> 用户会明确要求你更新，但即使他不说，发现文档与事实不符时也应该主动修正。
>
> **维护这份文件的规矩**（第二次交接时定下）：
> - 写进来的每一条都要能说清是「已验证 / 未验证 / 推断」，尤其是别人的验证结果要写明是转述。
> - 会过时的东西（commit 哈希、构建结果、依赖子模块版本）要么标注获取方法让下一个你现场重查，要么就别写死。
> - 修文档前先把要改的地方在项目里核一遍，**不要凭读文档时的印象判断文档写错了**——上一次就有人把「用户要求少用 Mixin」误读成「代码里的钩子都该消灭」。
> - 保持总体意思不变。这份文件的价值在于连续性，不是在于漂亮。

---

## 你的角色与协作方式

你接手一个 Minecraft 1.21.11 Fabric 模组项目「AbyssFall」。用户（Kainy）与你的分工已经磨合成形，请严格延续：

**用户负责**：提供设计思路、大方向、功能需求。
**你负责**：让想法运作起来。具体实现自由度很高。

**用户明确授予你的自主权**（不必询问，改完告知即可）：
- 颜色、粒子、音效等视觉/听觉细节。若某配色看不清或效果不好，直接换掉。
- 语言文件翻译。若用户的翻译有语法错误直接改正；同时必须避免机翻感和僵硬表达。
- 代码注释、javadoc、命名、内部结构。
- **GitHub 相关操作全部交给你**（用户原话：「以后 github 构建都由你来」）。

**验证强度约定**（用户明确要求，为省 token）：
- 用户自己跑 `runClient`，他跑得更快且能拿到更多信息。**不要自己跑 runClient**。
- 你需要 runClient 确认什么，就明确列出待确认项让用户去测。
- 大改动才跑 `gradlew build`；小改动只做代码验证（读文件确认改动正确 + MCP 核实 API），**不构建**。
- 回复尽量简洁，省 token。**但结论必须分清「已验证 / 未验证 / 推断」**——用户重视这个，宁可说「我没验证」也不要含糊过去。

**用户背景**：以前写游戏外挂，遇到问题的第一直觉是「不用 API，用注入解决」。他已认可你的判断顺序并要求延续：

> **自有数据结构 → Fabric API/事件 → Mixin**

只有前两者做不到时才注入，且注入前必须用 MCP 核实目标版本的精确签名。选择注入时要说明为什么必须注入。

用户对这条原则的原话（第二次会话亲自澄清，照抄以免走形）：

> 「原则是尽量不使用 mixin，因为我以前是写外挂的，我的思考方式就是遇事不决用钩子，所以需要你来最大程度地不用 mixin，用 Fabric API 事件。但凡是有例外，有时候不得不用钩子的时候就要放心大胆地用钩子，**你在代码中看到的钩子就是不得不用的情况**。」

所以：**项目里现存的每一处 Mixin 和事件钩子都已经过评估、是不得不用的**（目前只有一个 Mixin：`WitherRoseBlockMixin`）。不要把它们当成待清理的技术债，也不要试图用 API 重写它们——那条路上一个你已经走过了。反过来，写新功能时该优先找 API 事件，找不到再注入，并说明理由。

**用户的提问风格**：他会问「这两个功能有什么区别，我测感觉差不多」这类问题。这通常不是抱怨，而是真的想搞清语义边界——直接回答区别、并说明什么情况下才看得出差异。他也会说「简单回复即可」，这时就别长篇大论。

**他也会用「本轮只做理论验证，不要写代码」来划定范围**（第三次会话用过）。遇到这句就老老实实只出方案、只收集证据，不要顺手改文件。方案里该问的问题一次问清，他会逐条回答甚至编号回应。

**关于设计取舍的立场（第三次会话确立）**：涉及与其他 mod / 数据包冲突时，用户的原则是「**保留自己的权益的同时尊重他人，而不是牺牲自己的权益去尊重他人**」。所以不要提「为了礼貌而放弃功能」的方案——他明确说过这类选项以后不要再提。可以提「照做 + 记日志让冲突可见」这种两全方案。

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
| Gradle | 9.7.0（`gradle/wrapper/gradle-wrapper.properties`，已核实） |
| modid / 包名 | `abyssfall` / `com.abyssfall` |
| 版本 | 0.1-Dev（`gradle.properties` 的 `version`） |
| 许可 | GPL-3.0-or-later（所有 .java 带 GPL 版权头，新文件必须照抄） |
| JDK | 本机 Gradle 跑 JDK 25，编译 toolchain JDK 21 + `release = 21` |
| 源集 | `splitEnvironmentSourceSets()`：`src/main` + `src/client` |
| **Git** | **已初始化**，远端 `https://github.com/Kainy030/AbyssFall-Fabric.git` |

**重要**：因为用 mojmap，类名是 `net.minecraft.world.item.Item`、`net.minecraft.resources.Identifier`（1.21.11 已从 `ResourceLocation` 改名）这类 Mojang 名，**不要写 Yarn 名**。

**常用命令**：
```powershell
cd D:/MC1.21.11-AbyssFall-Fabric
.\gradlew.bat build --console=plain --offline
.\gradlew.bat compileJava --console=plain --offline --rerun-tasks   # 强制重编译，能暴露警告
.\gradlew.bat releaseJars --console=plain --offline                 # 产出三个发布 jar
```
注意：工具有 **30 秒硬上限**，`build` 偶尔被截断，此时重跑一次看 `UP-TO-DATE` 判断上次是否已成功。`Start-Sleep` 超过 30 秒会直接失败，别用。

`build.gradle` 里还留着 `runProductionClient` / `runProductionServer` 两个任务（`net.fabricmc.loom.task.prod.*`，配套 `productionRuntimeMods` 依赖）。**用户明确说过这两个任务现在已无意义**，不要用它们做验证、也不要向用户推荐。留着不删是因为删它们属于与当前需求无关的改动。

---


## 核心系统：San 值（这是项目的地基，优先理解这一节）

**用户对项目的定位**：「这个系统贯穿始终，决定了咱们这个项目玩法的基础，咱们这个项目是随着 san 进行推进的，这是一个很重要的变量。」

### 设计理念（用户第二轮明确修正过，别改回去）

San 是**连续参数**，不是离散状态机。

第一版我做了 `SanStage` 枚举（STABLE / FRAYED<50% / SHATTERED<30%），**用户明确否决**：

> 「不要做成传统的线性分段状态……我希望 0%～100% 的整个百分比区间都可以拥有不同的行为规则……例如 San ≥ 80% 时保持正常，一旦从 80% 以下开始下降，即使只下降 0.1%，也可以触发一次对应的状态变化……所以 San 百分比应该被视为一个连续参数。」

`SanStage.java` 已删除。**不要再引入任何档位枚举或阈值常量到 core 里。**

阈值属于消费方：每个功能自己决定是平滑缩放还是有自己的线，core 不表态。

### 文件与职责

```
src/main/java/com/abyssfall/core/
├── AbyssFallCoreSystem.java   系统门面：attachment 注册 + 全部读写 API + 事件派发
├── SanState.java              不可变 record (current, max)，自带 Codec / StreamCodec
├── SanChangedCallback.java     变化事件，携带 previous→current
└── AbyssFallSanCommand.java    /san 调试命令
```

### 技术实现（零 Mixin，一个 API 事件钩子）

**attachment 注册名**：`abyssfall:core_system_san`（用户指定的名字，是存档 key，**改名会孤立所有存档**）

用 **Fabric Data Attachment API**（`fabric-data-attachment-api-v1:1.8.48+eed0806f3e`，版本已从项目 pom 实测），builder 配了四项：

| 配置 | 作用 |
|---|---|
| `initializer(() -> SanState.INITIAL)` | 首次询问时给 100.00F/100.00F，不必自己造默认值 |
| `persistent(SanState.CODEC)` | 存档持久化 |
| `copyOnDeath()` | 死亡重生保留（语义判断：San 是经历的记录，重生不该洗白） |
| `syncWith(STREAM_CODEC, targetOnly())` | 只同步给本人（未来渲染是第一人称的） |

**为什么不用 Mixin**（用户已认可）：这四件事 API 原生就做了。注入 `Player` 加字段要自己写存读钩子、重生拷贝钩子、同步包，还会和其他 mod 撞车。**没有注入的理由。**

**为什么 current 和 max 合成一个 record**：两者互相约束（`0 ≤ current ≤ max`），分开存会出现瞬时非法状态，且要发两个同步包。invariant 在 canonical constructor 里强制，连反序列化和网络解包都过这一关。

**还有一个 JOIN 钩子，别当成多余的删掉**：`AbyssFallCoreSystem.initialize()` 里注册了 `ServerPlayerEvents.JOIN`（fabric-entity-events-v1），回调里做一次 `player.getAttachedOrCreate(SAN)` 并打 debug 日志。

```java
ServerPlayerEvents.JOIN.register(player -> {
    SanState state = player.getAttachedOrCreate(SAN);
    AbyssFall.LOGGER.debug(...);
});
```

原因：`initializer` 只保证「被问到时有值」，纯读（`getAttachedOrElse`）不会把值真正写进 attachment，于是全新玩家可能压根没有存储的 attachment，也就不会触发同步推送。JOIN 时主动 `getAttachedOrCreate` 一次，把值落盘并推给客户端，从第一 tick 起客户端就拿得到。**这是 API 事件而非 Mixin，符合原则。**

**为什么事件从 `set()` 派发而不用 attachment 自带的 `onAttachedSet`**：后者是**按 target 实例**的（`default <A> Event<OnAttachedSet<A>> onAttachedSet(...)`），必须先拿到每个玩家实例才能订阅，无法全局监听。而所有写入本来就汇聚在 `set()`。

**`set()` 里回读了两次**，别「优化」掉：
```java
SanState previous = get(player);
player.setAttached(SAN, state);
SanState stored = get(player);   // 回读：传入值可能被 clamp
```
事件必须携带**真实存储值**，否则监听方会基于一个从未存在过的值做判断。

### API 一览

**读（5 个，两端安全，收 `Player`）**：`get` `getCurrent` `getMax` `getRatio` `getPercent`
**写（8 个，只收 `ServerPlayer`）**：`set` `modify` `addCurrent` `setCurrent` `addMax` `setMax` `restore` `reset`

`SanState`：`ratio()` `percent()` `isFull()` `isEmpty()` `withCurrent` `addCurrent` `withMax` `addMax` `full(max)` `INITIAL`
常量：`DEFAULT_MAX=100` `MIN_MAX=1` `MAX_MAX=10000`

`SanChangedCallback.Change`：`currentDelta()` `maxDelta()` `ratioDelta()` `isNoOp()` `crossedDown(t)` `crossedUp(t)`

`crossedDown/Up` 的阈值是**参数**，由调用方传入——这正是「连续参数」理念的落地方式：
```java
// 「跌破 80% 的那一瞬间，只触发一次」
if (change.crossedDown(0.80F)) { ... }

// 「随 San 连续变化」——无阈值
float intensity = f(change.current().ratio());
```

### 两个语义决策（用户可能会问，也可能想改）

1. **提高上限不白送 San**：`withMax` 提高时 current 不动（有了成长空间，但没变更清醒）。降低上限到 current 以下才会把 current 一起拖下来。
2. **`restore` vs `reset`**：`restore` = current 回满到**当前上限**；`reset` = current 和上限**一起**回默认 100。上限没动过时两者结果相同——用户问过这个。

### /san 命令（8 条）

| 指令 | 权限 |
|---|---|
| `/san` | 无（看自己） |
| `/san query <玩家>` | gamemaster |
| `/san set <玩家> <值>` | gamemaster |
| `/san add <玩家> <增量>` | gamemaster |
| `/san max set <玩家> <值>` | gamemaster |
| `/san max add <玩家> <增量>` | gamemaster |
| `/san restore <玩家>` | gamemaster |
| `/san reset <玩家>` | gamemaster |

输出格式：`<名字>: San 100.00 / 100.00 (100.00%)`。刻意用英文调试格式（`Component.literal`），因为这是工具而非内容，所以没有 lang key。百分比两位小数是为了能看清 0.1% 级变化。

**1.21.11 权限 API 变了**：不是老的 `hasPermission(int)`，而是
`Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` 返回 `PermissionProviderCheck<T>`（`net.minecraft.server.permissions`，`record ... implements Predicate<T>`），可直接传给 `.requires()`。

### San 系统当前状态

**San 系统只有框架，零具体世界规则。** 事件目前**没有任何监听者**——这是预期的。用户说：「我们现在要的是框架」。

未来的差异化渲染（San 低的玩家看到不同的方块/物品渲染）由用户后续提出时再做。

---


## 配置系统（第三次交接时新建，仅次于 San 的第二块地基）

用户的定位：「我预感咱们项目以后的可自定义配置会特别多，别欠技术债，趁现在没什么代码的时候赶紧重构」。

**所以这套系统是为「配置项会长到很多」设计的，不是为当前这几项设计的。加配置时请沿用它，不要另起炉灶。**

### 文件位置与格式

`config/abyssfall.json`。**是 JSON，不是 properties**——第一版曾用 `.properties`，同一次会话内就被用户要求换成主流 mod 的 JSON 格式。旧实现已删除，别复活它。

用户当时问过「为什么不用 json 或 conf」，换之前给的理由是「properties 能带注释、零依赖」。换成 JSON 的决定性理由是**结构化配置**：`.properties` 表达列表只能靠逗号分隔字符串自己解析，而 `target_tables` 天生是数组。代价是 JSON 不能写注释，接受了。

### 分块结构（加配置就是加块）

```
config/
├── AbyssFallConfig.java       静态门面：load() / save() / get() + 便捷访问器
├── AbyssFallConfigData.java   根 record，持有各块
├── DeveloperSettings.java     developer 块
├── LootSettings.java          loot 块
└── VisualSettings.java        visuals 块
```

每个块都是 `record` + 三件套：`DEFAULT`、`CODEC`、`LENIENT_CODEC`。

**加一个配置项**：往对应块的 record 加字段、`CODEC` 里加一行 `fieldOf`、`DEFAULT` 给值。完事。
**加一整块**：照抄任一现有块写 record，然后在 `AbyssFallConfigData` 加一个字段 + 一段 `fieldOf(...).orElse(...)`。

### 关键设计：读写用不同的 Codec（勿「优化」成一个）

| | 用什么 | 为什么 |
|---|---|---|
| **写** | `CODEC`（`fieldOf`） | 总是输出全部字段，生成的文件列出每一项，玩家看得见有什么可配 |
| **读** | `LENIENT_CODEC`（`CODEC.orElse(...)`） | 缺字段/整块坏掉 → 回落该块默认，**不废掉整个文件** |

**这不是过度设计，是实测踩出来的**：第一版两边都用 `optionalFieldOf`，跑测试发现默认配置文件被写成 `{}`——因为 `optionalFieldOf` 在值等于默认值时**编码时会省略该字段**。玩家打开文件是个空对象，根本不知道能配什么。

副作用：加字段永不破坏旧文件（旧文件缺新字段 → 回落默认），**所以不需要任何迁移代码**。

`Codec.orElse` / `MapCodec.orElse` 有 `Consumer<String>` 和 `UnaryOperator<String>` 两个重载，**直接传 lambda 会编译不过（引用不明确）**，必须显式 `(Consumer<String>)` 强转。代码里那些 cast 是必需的。

### 三种失败的区别（很重要，别合并处理）

| 情形 | 行为 | 日志 |
|---|---|---|
| 文件不存在 | 写一份默认 | INFO |
| **JSON 语法坏了** | **备份原文件 + 写一份默认** | ERROR |
| 语法对但某个值不合法 | 该块回落默认，**其余字段照常生效，不动文件** | WARN |

第三种**刻意不重写文件**：玩家可能配了几十项只错一个，重写会把对的全冲掉。第二种才重写，因为整个文件无法解析、没有任何东西可救。

`read()` 返回 `null` 表示「第二种」，返回对象表示「第一或第三种」——这个 null 语义写在 javadoc 里，别改成 Optional 顺手改掉含义。

### 坏文件备份

`abyssfall.json.broken-yyyy-MM-dd_HH-mm-ss`，同秒撞名追加 `-2`/`-3`（备份被备份覆盖就失去意义）。

**分隔符必须是 `-` 和 `_`，不能用 `:`**。用户最初要的格式是 `broken-yyyy:MM:dd:HH:mm:ss`，我实测 Windows 拒绝含 `:` 的文件名（`不支持给定路径的格式`），报给用户后改成了现在这个。**若后人想「改回冒号更好看」——不行，rename 会直接失败，导致坏文件既没备份也没替换。**

用户已实测这条路径可用，回报的日志：
```
[Render thread/ERROR] (abyssfall) Could not understand ...\run\config\abyssfall.json;
it has been moved to abyssfall.json.broken-2026-08-20_10-52-50 and replaced with default settings
```

### 不做热加载（用户明确要求）

配置只在 `onInitialize()` 读一次，改完必须重启。

**但要知道这是需求决定，不是技术限制**：`LootTableEvents.MODIFY` 本身每次数据包重载都会触发（读 fabric-loot-api-v3 源码确认，它 mixin 到 `ReloadableServerRegistries.reload`，在 `map.replaceAll` 里逐表调用）。想开热加载只需在回调里实时读配置，几行的事。注释里也写了这一点。

### 当前全部配置项（默认值 = 改动前的行为，逐值实测对齐）

```json
{
  "developer": { "dev_inventory": false },
  "loot": {
    "flower_chance": 0.05,
    "target_tables": [ "...18 个 minecraft:chests/..." ]
  },
  "visuals": {
    "bloom_particle_scale": 1.0,
    "bloom_sound_volume": 1.0
  }
}
```

用户要求「所有默认值除开发者模式外全部按项目当前状态写」，已做到：`0.05` 对应原 `1:19` 权重、`1.0` 倍率对应原粒子数 12/8/20/6 与音量 0.7/0.5。**改默认值就等于改游戏行为，动之前想清楚。**

### `flower_chance`：概率而非权重（用户明确要求）

「建议表达为玩家理解的概率，而不是暴露内部 LootPool 权重」。

换算在 `LootSettings.emptyWeight(int)`：`EMPTY = max(1, round((1-p)/p * flowerWeight))`，`FLOWER_WEIGHT` 恒为 1。

**两个边界必须特判，别删**：
- `p >= 1.0` → `isGuaranteed()` → **不加空条目**。否则公式算出 0、被 `max(1,..)` 兜成 1，结果只有 50%——设成 100% 却掉一半，这是实测跑出来的 bug。
- `p <= 0.0` → `injectsBaselinePool()` 为 false → **整个基础池不注入**，而不是加一个永不中奖的池。

实测换算精度：0.05→19(5.000%)、0.01→99、0.1→9、0.25→3、0.5→1，全部精确还原。

### `visuals`：倍率而非开关（用户明确要求）

「声音大小和粒子效果都可以自定义大小多少，而不是一刀切开或者关」。

两项各自独立、范围 `0.0~2.0`（`Codec.floatRange`）。`0.0` 仍可完全关闭，所以倍率是开关的超集。

- `scaleParticles(int)`：**非零倍率保证至少 1 个粒子**。否则 `0.05` 倍会让 SMOKE(6) 算成 0 而静默消失，看起来像 bug。
- `scaleVolume(float)`：**只乘音量，不动音调**。0.6/1.4 的一低一高是「付出→到来」的设计意图，不是可调参数。

---


### 目录结构
```
src/main/java/com/abyssfall/
├── AbyssFall.java                      主入口，MOD_ID + LOGGER + id(String) 工具方法
├── advancement/AbyssFallAdvancements.java
├── block/AbyssDirtBlock.java
├── block/AbyssFallBlocks.java
├── block/AbyssFallBoneMealHandler.java
├── config/AbyssFallConfig.java         ← 配置加载/保存，见「配置系统」一节
├── config/AbyssFallConfigData.java     ← 配置根记录
├── config/DeveloperSettings.java
├── config/LootSettings.java
├── config/VisualSettings.java
├── core/AbyssFallCoreSystem.java       ← San 系统，见上一节
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
src/client/java/com/abyssfall/client/AbyssFallClient.java   （空实现）
```

`AbyssFall.onInitialize()` 的调用顺序（有依赖关系，勿随意调整）：
```java
AbyssFallConfig.load();               // 最先！注册与否取决于配置，注册后无法回头
AbyssFallCoreSystem.initialize();     // San 最先，它是其他一切要移动的值
AbyssFallSanCommand.initialize();
AbyssFallEffects.initialize();
AbyssFallItems.initialize();
AbyssFallBlocks.initialize();
AbyssFallItemGroups.initialize();     // 依赖 Items 和 Blocks
AbyssFallLootTables.initialize();     // 读配置，必须在 load() 之后
AbyssFallBoneMealHandler.initialize();
AbyssFallDevInventory.initialize();   // 最后，条件注册
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

**第三次交接时接入了配置倍率**：上表数量与音量都会乘 `visuals.bloom_particle_scale` / `bloom_sound_volume`（默认 1.0，即上表原值）。**音调不受配置影响**，理由见「配置系统」一节。骨粉催熟机制本身**没有开关**——用户明确说骨粉是核心机制，不给其他 mod 让路，只有特效可调。

### 7. 药水效果：深渊探索者 `abyssfall:abyss_explorer`
`MobEffectCategory.BENEFICIAL`，颜色 `0x9B6BC9`。**纯标记效果，不覆盖任何 tick 方法**，逻辑全在战利品侧。18×18 图标由 `make-effect-icon.ps1` 生成。

注册用 `Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, ...)`。

**注意**：全项目**没有任何地方给玩家上这个效果**，只有战利品侧读取它。获取途径尚未设计。


### 8. 战利品表注入
`AbyssFallLootTables` 用 `LootTableEvents.MODIFY`（`net.fabricmc.fabric.api.loot.v3`），回调签名 `(key, tableBuilder, source, registries)`。

**第三次交接时已改为读配置**：注入哪些表、概率多少都来自 `LootSettings`，见「配置系统」一节。默认仍是原来那 18 张高价值结构宝箱表（沙漠神殿、丛林神庙、末地城宝藏、林地府邸、要塞×3、堡垒残骸×4、沉船宝藏、远古城市、试炼密室 unique×2、下界要塞、掠夺者前哨站、埋藏的宝藏），**行为与改动前一致**。

**两个独立池**：
1. 基础概率池：由 `flower_chance` 决定（默认 5%）
2. 带「深渊探索者」效果时**必定**额外给 1 个

第 2 个池的条件是 `LootItemEntityPropertyCondition.hasProperties(EntityTarget.THIS, EntityPredicate.effects(...))`。**能这样做的依据**：`RandomizableContainer.unpackLootTable()` 在玩家开箱时会 `withParameter(LootContextParams.THIS_ENTITY, player)`，所以战利品表知道开箱者是谁、身上有什么效果。破坏箱子不走这条路径，符合「开启宝箱」语义。

#### `isBuiltin()` 门槛已被刻意移除（用户明确决定，别加回去）

原先有 `if (!source.isBuiltin()) return;`，意图是「尊重数据包作者的重写」。**第三次交接时用户明确要求去掉**，原话：

> 「我没那么礼貌，我们的mod保留自己的权益的同时尊重他人即可，而不是牺牲自己的权益去尊重他人。」

用户同时明确表示，**「保持现状（继续拦）」这个选项以后不要再提**。

技术上的重要澄清（我上一轮判断错过一次，别重犯）：`LootTableSource` 有四个值，`VANILLA(true)` / `MOD(true)` / `DATA_PACK(false)` / `REPLACED(false)`。**其他 mod 自带的战利品表是 `MOD`，`isBuiltin()` 为 true**，所以那个检查从来就没有拦住别的 mod，它实际只拦「外部数据包」和「被 REPLACE 替换过的表」。

移除它的正当性：我们用 `withPool` **追加独立池**，不编辑任何既有池、不移除或改权重任何既有条目。别人写的东西一字不动。现在改为**遇到非 builtin 来源时打 INFO 说明「按配置追加了一个池」**，冲突可见但不改变行为。

#### 未命中的表会 WARN

配置里写了但整个加载周期从未出现的表 ID，会在 `LootTableEvents.ALL_LOADED` 时逐个 WARN，提示检查拼写或对应 mod 是否安装。

**为什么只能这样检测**：`LootTableEvents.Modify` 返回 `void`，`withPool` 也没有返回值或异常，**回调内部压根不存在「注入失败」这个状态**。真正的失败形态只有「回调从未被调用」（因为表不存在），而这只能等全部加载完才知道。用户原本要求的是「API 状态导致无法注入就 WARN」，我核实后报告了这一点，改成了现在的形态。

用 `ConcurrentHashMap.newKeySet()` 跟踪待命中集合，因为 Fabric 自己的 loot 实现注释写明「due to possible cross-thread handling」，战利品加载可能跨线程。`ALL_LOADED` 里会重新填充集合以支持 `/reload` 后再次检测。

**`target_tables` 不限于箱子、不限于原版**：用的是 `ResourceKey.codec(Registries.LOOT_TABLE)`，任何命名空间的任何战利品表都能写，包括 `gameplay/fishing/treasure`（钓鱼）、`gameplay/piglin_bartering`（猪灵交易）、`entities/*`（生物掉落）。这是换成字符串 ID 后免费获得的扩展性。


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

**已知结构限制**：用户想要 `abyss_fall + abyssdirt = abyss_gardeners`（两个父节点汇聚），但 `Advancement` 的 parent 字段类型是 `Optional<Identifier>` —— **单亲树，做不到多父节点**。故实现为链式。已向用户说明，用户接受。曾提出的备选方案 C（把 `abyss_gardeners` 的 criteria 扩为四条 AND）用户暂未采纳，可按需提起。

### 10. 资源与元数据
- `fabric.mod.json`：含 `icon`、`license: GPL-3.0-or-later`、`fabric-api: >=0.141.6`
- `assets/abyssfall/icon.png`（128×128，桃花，由 `make-icon.ps1` 生成）
- lang：`en_us.json` + `zh_cn.json`（**无 BOM 的 UTF-8**）
- `data/abyssfall/loot_table/blocks/abyss_dirt.json`（方块掉落自身）
- `data/minecraft/tags/block/mineable/shovel.json`
- `abyssfall.mixins.json`（`package: com.abyssfall.mixin`，`compatibilityLevel: JAVA_21`）已登记 `WitherRoseBlockMixin`；`abyssfall.client.mixins.json`（`package: com.abyssfall.client.mixin`）的 `client` 数组仍为空。两份都设了 `injectors.defaultRequire = 1` 和 `overwrites.requireAnnotations = true`——**前者意味着注入点找不到会直接崩，这是故意的**：宁可启动失败也不要静默失效。

### 11. 美术脚本（PowerShell + System.Drawing）
`make-icon.ps1`（128×128 mod 图标）、`make-item-texture.ps1`（16×16 物品贴图）、`make-effect-icon.ps1`（18×18 效果图标）。均用 `$PSScriptRoot` 相对定位，直接 `powershell -NoProfile -ExecutionPolicy Bypass -File .\xxx.ps1` 运行。

### 12. 开发者物品栏 `abyssfall:abyssfall_dev_inventory`（第三次交接时新增）

第二个创造模式标签，**仅当 `developer.dev_inventory = true` 时才注册**，默认 false。

标题是**三色**：「深渊」DARK_GRAY 粗 + 「浮现」GRAY 粗 + 「开发者物品栏」血红 `0xB01030` 粗斜。前两段**复用主标签的 lang key**（`itemGroup.abyssfall.head` / `.tail`），第三段是新增的 `itemGroup.abyssfall.dev`。用户已实测三色标题显示正常、tooltip 不变蓝。

血红用 `TextColor.fromRgb(0xB01030)` 而非 `ChatFormatting.DARK_RED`，因为创造界面背景偏亮，原版暗红读起来发棕。

en_us 的 `.dev` 值是 `" Dev Inventory"`（**有前导空格**），因为英文三段拼接不加空格会变成 `AbyssFallDev Inventory`；中文不需要空格。

**关键实现约束**：`AbyssFallDevInventory` 里的物品与标签**都不是 `static final`**，而是在 `initialize()` 里创建、用普通 static 字段持有。原因是 `static final` 在类被触碰的瞬间就完成注册，开关根本没机会起作用。`getSanCounter()` 在关闭时返回 null，javadoc 里说明了这是刻意的（项目里没有 `@Nullable` 依赖，所以只写在文档里）。

**后果要知道**：关掉开关后，存档里已有的这些物品会在加载时被当作未知物品**丢弃**。这是「真的没注册」的必然结果，不是 bug。用户当初的要求原话就是「物品也不会被注册」。若哪天想改成「物品仍存在、只是标签不显示」，那是另一套语义（只把标签注册设为条件性）。

### 13. 理智计数器 `abyssfall:san_counter`（第三次交接时新增）

开发者专用 debug 物品，放在开发者物品栏里，`stacksTo(1)`。图标暂用原版时钟贴图（`assets/abyssfall/items/san_counter.json` 直接指向 `minecraft:item/clock_00`）。

**作用**：主手右键，在生命/饱食度条上方显示 `理智值：当前 / 最大`，3 秒后淡出，再按重新计时。

**「3 秒 + 淡出 + 重按续期」全部是原版行为，一行计时器都没写。** 依据（1.21.11 mojmap 源码实测）：
- `Gui.setOverlayMessage`（`Gui:1130-1134`）把 `overlayMessageTime` 无条件设为 **60 ticks = 精确 3 秒**，无条件赋值所以重按即重置
- `Gui:313-314` 的 alpha 是 `(overlayMessageTime - partialTick) * 255 / 20` 且上限 255，所以最后 20 ticks（1 秒）线性淡出
- `Gui:325` 渲染位置 `translate(guiWidth/2, guiHeight - 68)`，正是生命/饱食度上方
- 传输链：`ServerPlayer.displayClientMessage(c, true)`（`:1541`）→ `sendSystemMessage(c, true)`（`:1755`）→ `ClientboundSystemChatPacket(overlay=true)` → 客户端 `ChatListener.handleSystemMessage`（`:186-188`）→ `gui.setOverlayMessage`

**刻意只在服务端读值**（`player instanceof ServerPlayer`）：客户端那份 attachment 只是服务端推送的镜像，debug 工具必须报告权威值，否则它验证不了任何东西。返回 `InteractionResult.SUCCESS`（`SwingSource.CLIENT`）让挥手动画立刻播放，不等往返。

**注意**：这个物品**不是**「客户端消费 San 同步值」的证明——它读的是服务端。所以下面「客户端是否真收到同步值」那条**仍然未验证**。


---

## Git / 发布流程（第一次交接时新建，以后由你负责）

用户明确授权：**「以后 github 构建都由你来」**。

### 仓库状态

| 项 | 值 |
|---|---|
| 远端 | `https://github.com/Kainy030/AbyssFall-Fabric.git` |
| 分支 | `main` |
| 凭据 | 已缓存（`credential.helper=manager`），push 无需交互 |
| git 用户 | Kainy / 1747110555@qq.com（global 已配） |

提交历史（截至本次交接，`main` 已推送到远端，工作区 clean）：
```
4fd816c  Rewrite HANDOFF.md for the next session
260b8a0  Add release workflow so tagged builds carry their jars
56a8877  Initial commit: AbyssFall 0.1-Dev
```
tag `0.1-Dev` → `260b8a0`（唯一的 tag）。

**注意**：这张表容易过时。开工时用 `git --no-pager log --oneline -5; git status --short; git --no-pager tag` 现场核一遍，别信文档里的哈希。

### tag 命名规则（重要）

**tag 不带 `v` 前缀**，就是项目版本本身：`0.1-Dev`。

所以 workflow 触发器写的是 `[0-9]+.[0-9]+*` 和 `v[0-9]+.[0-9]+*` 两种，**不要改成常见的 `v*` 模板**，那样永远不会触发。

### 三个发布产物

`build.gradle` 里加了两个自定义任务：

| 任务 | 作用 |
|---|---|
| `javadocJar` | 打包 API 文档（项目原本只有 `javadoc` 任务，没有打包任务） |
| `releaseJars` | 把三个 jar 按短名汇总到 `build/release/` |

产出（`build/release/`）：

| 文件 | 来源 |
|---|---|
| `abyssfall.jar` | `remapJar`（**不是 `jar`**，后者是 dev-mapped，真实环境跑不了） |
| `abyssfall-doc.jar` | `javadocJar` |
| `abyssfall-source.jar` | `remapSourcesJar` |

`build/libs/` 同时保留 Gradle 标准命名版（`abyssfall-0.1-Dev[-sources|-javadoc].jar`），内容字节一致。

**Javadoc 必须显式设 UTF-8**（`options.encoding` / `docEncoding` / `charSet`）：源码含中文注释和排印破折号，doclet 默认用平台编码（本机 GBK）会读失败。同时关了 doclint，因为 MC / Fabric API 无外链文档。

### GitHub Actions

`.github/workflows/release.yml`，tag push 或手动 `workflow_dispatch` 触发。

action 版本（都是当时查证过的最新版，将来可能需要再查）：
`checkout@v7` / `setup-java@v5` / `setup-gradle@v6` / `action-gh-release@v3` / `upload-artifact@v7`

**三个关键设计点**：
1. **不加 `--offline`** —— 本地能 offline 是因为缓存完整；CI 空缓存必须联网拉 Minecraft / Loom / Fabric API。首次约 3–8 分钟。
2. **直接用 `setup-java` 提供 JDK 21，不靠 Gradle toolchain** —— `gradle.properties` 里 `auto-download=false` 且硬编码了本机 JDK 路径，CI 上那路径不存在。
3. **`gradlew` 必须是 `100755`** —— Windows 建仓时它是 `100644`，Linux runner 上 `./gradlew` 会 `Permission denied`。已用 `git update-index --chmod=+x gradlew` 修好。**以后新建仓库或重置权限时要留意这一点。**

### 已验证的 CI 结果

用户回报的 SHA256 与本地产物比对：

| 文件 | 结果 |
|---|---|
| `abyssfall.jar` | ✅ `4ac04815...` 字节级一致 |
| `abyssfall-source.jar` | ✅ `72f83330...` 字节级一致 |
| `abyssfall-doc.jar` | ⚠️ 不一致（Javadoc HTML 内嵌时间戳/JDK 版本，CI 的 Temurin 与本机 JDK 21.0.10+7 不同。内容等价，非问题——这是推断，未逐字节 diff 证明） |

### 发布流程

```powershell
# 1. 改代码、提交
git add -A; git commit -m "..."
git push origin main

# 2. 打 tag 并推送 → CI 自动构建并附加三个 jar
git tag -a '<版本>' -m 'AbyssFall <版本>'
git push origin '<版本>'
```

也可在 Actions 页面手动 `workflow_dispatch` 选任意已有 tag 触发，不必重新打 tag。

### 网络坑（实际遇到过）

push 时偶发 `schannel: failed to receive handshake, SSL/TLS connection failed`。**读操作（`ls-remote`）正常、写操作失败 → 是网络不稳定，不是权限问题。** 这次连续失败 3 次、第 4 次成功。用重试循环：

```powershell
$env:GIT_TERMINAL_PROMPT='0'
for($i=1;$i -le 5;$i++){
  git push origin main 2>&1 | Out-String | Write-Output
  if($LASTEXITCODE -eq 0){ Write-Output 'SUCCESS'; break }
  Start-Sleep -Seconds 4
}
```
注意 `Start-Sleep` 总时长要控制在 30 秒内，否则工具超时。

### GitHub 的一个认知点

用户曾疑惑「手动用 tag 构建版本，Assets 里只有 Source code (.zip/.tar.gz)」。

**这是正常的**：那两个源码包是 GitHub 按 tag 自动生成、无法删除；编译产物 GitHub 永远不会自己生成，必须上传或用 CI。「用 tag 创建 Release」只是建了个条目，没有发生任何编译。

**`gh` CLI 本机未安装**，所以你无法查询 Actions 运行状态或创建 Release。需要时让用户去页面看，或建议他装 `winget install --id GitHub.cli`。

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

7. **先问清设计理念，再动手写抽象**。San 系统第一版我自作主张加了 `SanStage` 三档枚举，用户随后明确要求改成连续参数，整个枚举白写。**用户说「引入一个概念」时，先确认它是离散的还是连续的。**

8. **别凭记忆写 action 版本号**。我第一版写 `upload-artifact@v4`，查证后实际最新是 v7。GitHub Actions 生态版本迭代快，写之前去 `releases/latest` 查。

9. **`editor` 工具单次 6000 字符上限**。写长文件要分段插入，否则报错重来浪费 token。

10. **`run_commands` 30 秒硬上限**。`Start-Sleep 45` 会直接失败；长构建可能被截断，重跑看 `UP-TO-DATE`。

11. **删 tag 重建时先确认远端删成功了再建本地**。我这次远端删除失败（网络）但本地已删，导致中间状态不一致，多花了几轮才理清。

12. **别把文档里的「原则」当成「待办」**。第二次交接时，我读到 HANDOFF 写「零 Mixin」、又在代码里看到 `ServerPlayerEvents.JOIN` 钩子，就当成矛盾去报给用户。用户澄清：他的原则是「最大程度不用 Mixin、优先用 Fabric API 事件」，而**代码里存在的钩子都是不得不用的、已经评估过的**。教训：看到文档与代码「像是」冲突时，先读代码注释里的理由，多数时候上一个你已经解释过了。

13. **`Select-String` 没有 `-Recurse` 参数**。要递归搜项目文件得先 `Get-ChildItem -Recurse -File src -Include *.java` 再管道给 `Select-String`。我这次直接写 `-Recurse` 报错浪费了一轮。

14. **不要凭公式自证，跑一遍**（第三次交接）。`flower_chance` 的权重换算我推导完觉得没问题，实际编译跑起来才发现 `p=1.0` 会算出 50% 而不是 100%。同一轮里还发现 `optionalFieldOf` 会让默认配置文件写成 `{}`。**这两个 bug 都是「跑」发现的，读代码读不出来。** 涉及数值换算和序列化时，写个临时类挂到项目真实 classpath 上跑一次，成本极低：
```powershell
# 临时往 build.gradle 加个任务取 classpath，用完删掉并用 git diff 确认还原
tasks.register('afPrintCp') { doLast { println sourceSets.main.runtimeClasspath.asPath } }
# 然后 javac -nowarn -proc:none -cp <classpath> -d out Check.java && java -cp out;<classpath> Check
```
用完记得删临时文件、还原 `build.gradle`，并用 `git status` / `git diff` 确认工作区干净。

15. **`editor` 工具不能直接覆盖已存在的文件**（没有 `old_text` 会报错）。要整体重写一个文件就先 `Remove-Item` 再创建。

16. **文件名里不能有 `:`**（Windows）。用户要的时间戳格式含冒号，实测创建失败。**任何要写进文件名的用户输入格式都先验一下**，别等运行时才炸。

17. **报告技术约束时先核实，别顺着需求答应**。用户要求「注入失败就 WARN」，我先去读了 `LootTableEvents.Modify` 的签名，发现返回 `void`、不存在失败状态，于是把方案改成「检测配置的表从未加载」并说明了原因。**上一轮我还犯过反例**：没核实就断言「别的 mod 的表往往不是 builtin」，实际 `LootTableSource.MOD` 的 `isBuiltin()` 是 true，判断完全错了，下一轮读源码才纠正。


---

## 当前状态（第三次交接时）

- 编译：**已验证**，本次会话多次 `gradlew build --offline` 全部 `BUILD SUCCESSFUL`，`remapJar` 实际执行
- Git 工作区：**有未提交改动**（见下），`main` 落后于本地工作区。上一次提交是 `0286a0f`
- tag 仍只有 `0.1-Dev`
- 产物：`build/release/{abyssfall,abyssfall-doc,abyssfall-source}.jar`（需 `releaseJars` 生成）

**本次会话结束时未提交的改动**（下一个你开工前先决定要不要提交）：
```
 M src/main/java/com/abyssfall/AbyssFall.java
 M src/main/java/com/abyssfall/block/AbyssDirtBlock.java
 M src/main/java/com/abyssfall/loot/AbyssFallLootTables.java
 M src/main/resources/assets/abyssfall/lang/{en_us,zh_cn}.json
?? src/main/java/com/abyssfall/config/          （5 个文件）
?? src/main/java/com/abyssfall/item/AbyssFallDevInventory.java
?? src/main/java/com/abyssfall/item/SanCounterItem.java
?? src/main/resources/assets/abyssfall/items/san_counter.json
```
用「开工前请做」里的命令现场核一遍，别信这张清单的时效性。

### 用户已在 runClient 实测通过的功能

**前两次会话**：创造标签双色标题、tooltip 不再变蓝、深渊之花 EPIC 紫色、宝箱掉落、药水效果必定掉落、村民箱子不掉落、玫瑰可种深渊污泥、其他作物不可种、骨粉催熟出花且玫瑰被消耗、灵魂特效、三个成就正常触发。

**第三次会话（用户明确回报「功能全部验证完毕，全部可用」）**：
1. 理智计数器右键显示 San、3 秒淡出、重按续期（用户评价「第三条实现的很完美」，指三色标题 tooltip 不变蓝）
2. 开发者物品栏三色标题、条件注册（`false` 时标签与物品都不存在）
3. 配置文件生成、读取、各项生效
4. **坏 JSON 备份+重写**，用户提供的日志证据：
   ```
   [Render thread/ERROR] (abyssfall) Could not understand
   D:\...\run\config\abyssfall.json; it has been moved to
   abyssfall.json.broken-2026-08-20_10-52-50 and replaced with default settings
   ```

`run/config/` 里现在同时有 `abyssfall.json` 和一个 `abyssfall.json.broken-2026-08-20_10-52-50`，后者是用户测试留下的，可以删。

### 我本次用真实 classpath 实测过的行为（非推断）

编译临时测试类挂项目 classpath 跑出来的，9 项全过：
默认配置文件三块完整输出、往返一致、默认 18 张表、权重换算 5 个值精确、`p=1.0` 走 guaranteed、`p=0.0` 不注入池、粒子倍率（含 `0.05→1` 不归零）、音量倍率、残缺文件其余项回落默认、外部 mod 表 ID 可解析、越界值整块回落。

### San 系统：已实测通过（第二次会话确认，本次未变动）

`/san` 全部 8 条、持久化、死亡保留、权限分级都已实测。**不要再列成待确认项去催用户测。**

**未验证的仍然只有一件事**：客户端是否真的收到了同步值。理智计数器读的是服务端，**没有**解决这一点。等第一个真正在客户端读 attachment 的消费方出现时才能验证。

### 第二次交接时的 CI 结论（仍然有效，本次未重跑）

CI 首次运行**成功**，三个 jar 已附加到 Release `0.1-Dev`，mod jar 与 source jar 与本地产物字节级一致。**本次会话没有再跑 CI，也没有新增 tag**，所以远端 Release 里的产物不含本次的配置系统与两个新物品。

---

## 未完成 / 可能的下一步

**San 系统（项目主线）**
- 事件无任何监听者，框架就绪但未接入任何玩法
- 差异化渲染（San 低的玩家看到不同渲染）—— 用户明确说以后开发
- 什么行为改变 San、什么行为改变上限 —— 全未设计
- 无 HUD（用户明确要求「暂时别设置可视化的 HUD」）
- 客户端侧无任何代码读取该值（同步通道已配好，但没有消费方，因此「客户端确实收到」这一点仍未验证）

**内容**
- 深渊污泥仍用原版泥土材质（用户说「暂时」）
- `abyss_gardeners` 图标是向日葵占位、`abyssdirt` 是原版泥土（用户说「暂时替换」）
- 理智计数器图标是原版时钟 `clock_00` 占位（**指针不会转**，因为原版时钟靠 `range_dispatch` 切 64 个模型才会转；要转就改那个 json）
- 深渊之花无实际功能（纯注册占位）
- 「深渊探索者」效果无获取途径，只被战利品侧读取
- 无配方、无 datagen、无自定义音效资源
- `abyssfall.client.mixins.json` 的 `client` 数组为空，`src/client` 下只有一个空实现的 `AbyssFallClient`，尚无任何客户端逻辑

**配置系统（本次新建，已可用，可继续扩展）**
- 只有 3 个块 7 个配置项。用户预期「以后可自定义配置会特别多」，架构已就绪，加块加项照「配置系统」一节的流程即可
- **不做热加载**是用户明确要求，别自作主张开
- 上一轮讨论中被用户明确否决/搁置的候选项，别再提：
  - 凋零玫瑰能种在深渊污泥上 —— **不进配置**，用户说是核心机制
  - 骨粉催熟机制开关 —— **不进配置**，用户说是核心机制、不给别的 mod 让路（只有特效可调）
  - `isBuiltin()` 保留拦截 —— 用户明确要求以后不要再提这个选项
- 我曾建议但**用户尚未表态**的：San 相关阈值不该进配置（因为 San 上限是存档数据，改配置会让新老玩家规则不一致，还可能静默 clamp 玩家数据）。真要做之前先问。
- `ALL_LOADED` 是否在每次 `/reload` 都触发，**仍未实测**。若不触发，未命中表的 WARN 只会在首次加载时报告一次。


---

## 开工前请做

1. 读 `gradle.properties`、`fabric.mod.json`、`AbyssFall.java` 确认状态与本文档一致
2. 读 `src/main/java/com/abyssfall/core/` 全部四个文件——这是项目地基
2b. 读 `src/main/java/com/abyssfall/config/` 全部五个文件——这是第二块地基，以后加配置都走它
2c. 现场核实 Git 状态，别信文档里的清单：
```powershell
git --no-pager log --oneline -5; git status --short; git --no-pager tag
```
3. 用 `Fabric-Knowledge` MCP 查 1.21.11 官方参考。**本次实测**：`get_fabric_context(minecraft_version="1.21.11", fabric_api_version="0.141.6+1.21.11", mapping_system="mojmap")` 返回 `status = version_match_only`、`exact_match = false`，命中的是 `reference/1.21.11`（Fabric API 0.141.1，与本项目 0.141.6 有小差异）。另外 `reference/latest` 现在指向的是 **MC 26.2 / Fabric API 0.155.2**，跟本项目毫无关系，**永远不要拿 `latest` 当本项目证据**。
4. 用 `minecraft-dev` MCP（`mapping: "mojmap"`, `version: "1.21.11"`）核实所有类/方法/字段签名，**不要凭记忆**
5. 涉及 Mixin 时用 `analyze_mixin` 校验
6. **查 Fabric API 实际行为时，直接读 Gradle 缓存里的 sources jar 最可靠**：
```powershell
$p="$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\net.fabricmc.fabric-api\fabric-api\0.141.6+1.21.11"
$f=(Get-ChildItem -Recurse -Filter *.pom $p).FullName
[xml]$x=Get-Content $f; $x.project.dependencies.dependency | ForEach-Object { "$($_.artifactId) $($_.version)" }
```
这比官方文档更贴近项目实际依赖（文档对应 0.141.1，项目是 0.141.6）。

本次已用上面的方法查出、项目实际用到的四个子模块版本（**已验证**）：

| 子模块 | 版本 | 项目里用它做什么 |
|---|---|---|
| `fabric-data-attachment-api-v1` | `1.8.48+eed0806f3e` | San attachment |
| `fabric-entity-events-v1` | `3.1.1+1d0ab4303e` | `ServerPlayerEvents.JOIN` |
| `fabric-events-interaction-v0` | `4.1.1+3b89ecf63e` | `ItemEvents.USE_ON` 骨粉催熟 |
| `fabric-loot-api-v3` | `2.0.20+78c8b4663e` | `LootTableEvents.MODIFY` |

7. **lang 文件用 `Get-Content` 看会是乱码**（PowerShell 默认按 GBK 解，文件是无 BOM UTF-8）。这是显示问题不是文件坏了，要看真实内容用：
```powershell
[System.Text.Encoding]::UTF8.GetString([System.IO.File]::ReadAllBytes('src/main/resources/assets/abyssfall/lang/zh_cn.json'))
```

---

## 给下一个你的话

用户和我们之间已经建立了很高的信任度。维持它的关键不是「多做」，而是：

- **不撒谎**。没验证就说没验证。他会因此更信你，而不是更少。
- **说清「为什么」**。他不只要能跑的代码，他要知道为什么这样做。每个非平凡决策都给出依据。
- **先确认理念再写抽象**。San 的 `SanStage` 就是反面教材。
- **该问就问，但别为小事问**。视觉细节、翻译、注释直接改；涉及玩法语义和数据结构走向时问一句。
- **保持简洁**。他明确说过省 token。长回复只在真的有必要时用。
- **信任已有的决策，但要理解它**。项目里每个看起来「不够优雅」的地方（JOIN 钩子、`set()` 里的回读、双色标题的空根组件、代码授予成就、配置读写用两个不同 Codec、`p=1.0` 和 `p=0.0` 的特判）都有写在注释里的理由。先读理由，再判断要不要动。他要求过「最大程度按 HANDOFF 执行，有矛盾随时通知我」——照做，但矛盾要先自己核实过再报。
- **能跑就跑一遍**。第三次会话里两个真 bug（`p=1.0` 只出 50%、默认配置写成 `{}`）都是靠临时编译一个测试类挂真实 classpath 跑出来的，读代码读不出来。用户不要你跑 runClient，但**不禁止你跑纯 Java 验证**，这条路成本极低且他很认这种证据。

祝顺利。
