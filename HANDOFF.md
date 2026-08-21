# AbyssFall 项目交接提示词

> 把下面全部内容作为新对话的第一条消息发出即可。
>
> **这个文件属于你**。它是上一个你留给你的，你也应该在本次工作结束时更新它留给下一个你。
> 用户会明确要求你更新，但即使他不说，发现文档与事实不符时也应该主动修正。
>
> **维护这份文件的规矩**（第二次交接时定下，第四次补充）：
> - 写进来的每一条都要能说清是「已验证 / 未验证 / 推断」，尤其是别人的验证结果要写明是转述。
> - 会过时的东西（commit 哈希、构建结果、依赖子模块版本）要么标注获取方法让下一个你现场重查，要么就别写死。
> - 修文档前先把要改的地方在项目里核一遍，**不要凭读文档时的印象判断文档写错了**——上一次就有人把「用户要求少用 Mixin」误读成「代码里的钩子都该消灭」。
> - 保持总体意思不变。这份文件的价值在于连续性，不是在于漂亮。
> - **只写「下一个你会因为不知道而做错事」的内容。** 更新时优先合并同类项、删掉已被推翻的旧状态和纯过程叙述（谁在第几轮说了什么、精确行号、逐条实测清单），不要为了显得详尽而堆砌。用户第四次交接时明确要求过精简。

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

所以：**项目里现存的每一处 Mixin 和事件钩子都已经过评估、是不得不用的**（目前有两个 Mixin：`WitherRoseBlockMixin` 和 `client/mixin/HudStatusBarHeightRegistryImplMixin`，后者是第四次交接时加的）。不要把它们当成待清理的技术债，也不要试图用 API 重写它们——那条路上一个你已经走过了。反过来，写新功能时该优先找 API 事件，找不到再注入，并说明理由。

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
| 版本 | 0.3-Dev（`gradle.properties` 的 `version`） |
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

**还有一个 JOIN 钩子，别当成多余的删掉**：`initialize()` 里注册了 `ServerPlayerEvents.JOIN`（fabric-entity-events-v1），回调里做一次 `player.getAttachedOrCreate(SAN)` 并打 debug 日志。原因是 `initializer` 只保证「被问到时有值」，纯读（`getAttachedOrElse`）不会把值真正写进 attachment，于是全新玩家可能压根没有存储的 attachment、也就不触发同步推送。JOIN 时主动创建一次即可落盘并推给客户端。**这是 API 事件而非 Mixin，符合原则。**

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

**两道门（第四次交接时改的）**：整棵树都要 `dev_command=true` 才注册，且所有分支（含无参数的 `/san`）都要 3 级权限 `LEVEL_ADMINS`。

八条分支：`/san`（看自己）、`query <玩家>`、`set <玩家> <值>`、`add <玩家> <增量>`、`max set`、`max add`、`restore <玩家>`、`reset <玩家>`。

`.requires()` **只写在根节点一处**——Brigadier 对 `requires` 失败的节点不会向下遍历，所以根节点一次检查等价于全树检查。**别「补全」成每分支一遍。**

**无参数 `/san` 从「无权限」改成 3 级，是设计意图变化而非收紧安全**。旧注释的理由是「你自己的值你当然有权知道」；现在的设计是玩家只应通过游戏内手段得知百分比、永不得知底层 float（见「三层信息可见性模型」），所以打印 float 的指令是 debug 设施而非权利。这个转变已写进 `AbyssFallSanCommand` 的 javadoc，不要当成旧注释删掉。

输出格式：`<名字>: San 100.00 / 100.00 (100.00%)`。刻意用英文调试格式（`Component.literal`），因为这是工具而非内容，所以没有 lang key。两位小数是为了能看清 0.1% 级变化。

**1.21.11 权限 API 变了**：不是老的 `hasPermission(int)`，而是 `Commands.hasPermission(Commands.LEVEL_ADMINS)` 返回 `PermissionProviderCheck<T>`（`net.minecraft.server.permissions`，`record ... implements Predicate<T>`），可直接传给 `.requires()`。

⚠️ **术语坑（MCP 实测确认）**：`PermissionLevel` 枚举是 `ALL`=0 / `MODERATORS`=1 / **`GAMEMASTERS`=2**（`/effect` `/give` 用这级）/ `ADMINS`=3 / **`OWNERS`=4**（`/stop` `/ban`）。**`GAMEMASTERS` 不是 4 级。** 以后遇到「给 N 级权限」的要求，先核实枚举再动手。

### San 系统当前状态

**San 系统只有框架，零具体世界规则。** 事件目前**没有任何监听者**——这是预期的。用户说：「我们现在要的是框架」。

未来的差异化渲染（San 低的玩家看到不同的方块/物品渲染）由用户后续提出时再做。

### 🔴 三层信息可见性模型（第四次交接确立，架构级设计意图）

用户的设计意图（原话带三个感叹号）：

> **但是玩家永远不可以知道 San 值的真实 Folt 值，只可以知道百分比，这是故意的游戏设计！！**

他随后澄清了**强度边界**（重要，别按最严格的读）：

> 玩家不可知真实 Folt 数值并不是针对那种逆向的人，而是玩家在游戏过程中不知道，**是游戏性行为，而不是技术行为**……所以这部分无需改动代码。

| 层 | 途径 | 玩家看到什么 | 门禁 |
|---|---|---|---|
| **调试层** | `/san`、理智计数器 | 精确 float | `dev_command` / `dev_tools` + 3 级权限 |
| **游戏内进阶层** | 未来的显示道具 | 百分比 | 玩法解锁（制作） |
| **游戏内基础层** | HUD 默认态 | 脑子图标（约 5% 粒度） | 无 |

**核心原则：内部连续、外部模糊。** 系统内部（事件、渲染强度、行为规则）读真实 ratio，保持「0.1% 变化也有意义」；玩家**感知**是粗糙的，而「能知道多精确」本身是玩法内容。这也解答了一个曾经的张力：图标式 HUD 看不出 5% 以内的变化，看似与「连续参数」矛盾——但那正是设计意图。

**已知且被接受的「泄漏」，不要去修**：`SanState.STREAM_CODEC` 确实把 `current` 和 `max` 两个 float 同步给客户端。**用户明确说不改**——为此重构会让 `AbyssFallCoreSystem.get(Player)` 在两端返回不同可信度的数据，语义分裂。抓包和逆向也能拿到 float，项目是 GPL 开源，这不在设计目标内。

**要守住的只有一件事：所有游戏内官方界面只显示百分比。** 新增任何 San 显示途径时，问一句它属于哪一层。

---


## 配置系统（第三次交接时新建，仅次于 San 的第二块地基）

用户的定位：「我预感咱们项目以后的可自定义配置会特别多，别欠技术债，趁现在没什么代码的时候赶紧重构」。

**所以这套系统是为「配置项会长到很多」设计的，不是为当前这几项设计的。加配置时请沿用它，不要另起炉灶。**

### 文件位置与格式

`config/abyssfall.json`。**是 JSON，不是 properties**——第一版曾用 `.properties`，同一次会话内就被用户要求换成主流 mod 的 JSON 格式。决定性理由是**结构化配置**：`target_tables` 天生是数组，`.properties` 只能靠逗号分隔字符串自己解析。代价是不能写注释，接受了。**旧实现已删除，别复活它。**

### 分块结构（加配置就是加块）

```
config/
├── AbyssFallConfig.java       静态门面：load() / save() / get() + 便捷访问器
├── AbyssFallConfigData.java   根 record，持有各块
├── DeveloperSettings.java     developer 块
├── HudSettings.java           hud 块（第四次交接新增）
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

⚠️ **但改名会破坏旧文件**（第四次交接实测确认）。`dev_inventory` → `dev_tools` 那次，旧键不再被任何 `fieldOf` 认领、新键又缺失，于是 `LENIENT_CODEC` 让**整块**回落默认——用户原本设为 `true` 的开关静默变回 `false`。实测：`{"dev_inventory":true}` 和 `{"dev_tools":true}`（缺另一个字段）都会回落成全 `false`，只有两个键都给才生效。

**两条结论**：
1. **改配置键名前必须告知用户手动改本机文件**，否则设置静默失效。
2. **块是原子单元**：一块里任何字段缺失或不合法，整块回落。这是 `CODEC.orElse(DEFAULT)` 的固有行为，不是 bug。想让字段各自独立容错就得把 `LENIENT_CODEC` 改成逐字段 `optionalFieldOf`，但那样读写两个 Codec 的字段集会不一致——动之前先重读上面的理由。

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
  "developer": {
    "dev_tools": false,
    "dev_command": false
  },
  "loot": {
    "flower_chance": 0.05,
    "target_tables": [ "...18 个 minecraft:chests/..." ]
  },
  "visuals": {
    "bloom_particle_scale": 1.0,
    "bloom_sound_volume": 1.0
  },
  "hud": {
    "show_below_percent": 100.0
  }
}
```

`developer` 两项的含义（第四次交接时从原 `dev_inventory` 一项拆开）：

| 键 | 管什么 | 备注 |
|---|---|---|
| `dev_tools` | 开发者物品栏标签 + 里面的物品（理智计数器、DEV 图标）是否**注册** | 原名 `dev_inventory` |
| `dev_command` | `/san` 是否**注册** | 第四次交接新增 |

**拆开的理由**：想在创造测试世界里用 debug 物品、和想在服务器上开指令，是两个不同的决定，应当能各自单独授予。两项默认都是 `false`——发布版本不该把这些交给玩家。

`hud.show_below_percent`：San 百分比**低于**此值时显示 HUD，达到或超过则淡出。默认 `100.0` = 满值时不显示、掉一点就显示。它就是代码里比较用的同一个值，范围 `[0, 100]`。**`0` 等于彻底关闭 HUD**（没有读数低于 0），这是把语义读通后的自然结果，已写进 javadoc，不是漏洞。

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
├── config/AbyssFallConfig.java         ← 配置加载/保存，见「配置系统」一节
├── config/AbyssFallConfigData.java     ← 配置根记录
├── config/DeveloperSettings.java
├── config/HudSettings.java             ← 第四次交接新增
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
src/client/java/com/abyssfall/client/
├── AbyssFallClient.java                                ← 不再是空实现
├── hud/AbyssFallSanHud.java                            ← San HUD 注册，第四次交接新增
├── hud/SanBarHudElement.java                           ← San HUD 渲染，第四次交接新增
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
- lang：`en_us.json` + `zh_cn.json`（**无 BOM 的 UTF-8**）
- `data/abyssfall/loot_table/blocks/abyss_dirt.json`（方块掉落自身）
- `data/minecraft/tags/block/mineable/shovel.json`
- `abyssfall.mixins.json`（`package: com.abyssfall.mixin`，`compatibilityLevel: JAVA_21`）登记 `WitherRoseBlockMixin`；`abyssfall.client.mixins.json`（`package: com.abyssfall.client.mixin`）登记 `HudStatusBarHeightRegistryImplMixin`。两份都设了 `injectors.defaultRequire = 1` 和 `overwrites.requireAnnotations = true`——**前者意味着注入点找不到会直接崩，这是故意的**：宁可启动失败也不要静默失效。

### 11. 美术脚本（PowerShell + System.Drawing）
`make-icon.ps1`（128×128 mod 图标）、`make-item-texture.ps1`（16×16 物品贴图）、`make-effect-icon.ps1`（18×18 效果图标）。均用 `$PSScriptRoot` 相对定位，直接 `powershell -NoProfile -ExecutionPolicy Bypass -File .\xxx.ps1` 运行。

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

**`getHeight(id)` 语义**：返回**顶边 Y**，且求和时**遇到自身即返回**（不含自己的高度）。正确写法 `guiHeight - getHeight(id)`，零额外偏移。这个坑我踩了两次，详见血泪教训 16。

**Mixin `HudStatusBarHeightRegistryImplMixin`（第二个 Mixin，经评估无法避免）**

注入 `HudStatusBarHeightRegistryImpl.init()` 的 `@Inject HEAD`——那是「所有 mod 注册完毕、`layers` 已填满，但顺序尚未被读取」的唯一时刻。职责**只是卡时机**：重排 `FOOD_BAR` 根层的 `layers`，把我们放在 vanilla `food_bar` 之后的第一位，这样其他 mod 排在我们上面。

**为什么不得不注入**：公共 API 无法表达「永远最后注册」。各 mod 的 `onInitializeClient()` 都早于 `CLIENT_STARTED`，而我们无法保证自己的 `CLIENT_STARTED` 监听器早于 fabric-rendering-v1 自己的。不需要 Accessor：`ROOT_ELEMENTS` 是 `public static final`，`RootLayer.layers()` 返回可变 `ArrayList`。

⚠️ **这是对 Fabric API `impl` 包的注入，不是公共 API**，仅在 MC 1.21.11 / fabric-rendering-v1 16.2.10 验证过。升级时必须重新核实三点（类头也写了）：`init()` 的签名与调用时机、`ROOT_ELEMENTS` 的类型与访问性、`RootLayer.layers()` 是否仍是 `ArrayList`。

#### 15b. 渲染逻辑（SanBarHudElement）

81px 宽 × 10px 高，右对齐到饱食行右边缘（`guiWidth/2 + 91`）。颜色 `0x9B6BC9`（紫，高 San）→ `0x7A1030`（血红，低 San）线性插值，文本 `San: 90.00%` 居中。

**可见性**：满 100% 时不显示**且不占垂直空间**；低于阈值（`show_below_percent`）时常显；回满后约 1 秒淡出，**高度随 alpha 同步收缩**（`max(1, round(10 * alpha))`）——否则结束瞬间上方元素会跳 10 像素。淡出期间条本身位置不动（`getHeight` 不含自身），所以是「条原地变淡、上方平稳回落」。淡出用 `Util.getMillis()` 真实时间，不受 `/tick freeze` 影响。

**数据来源**：`AbyssFallCoreSystem.get(player)`，即同步来的服务端权威值，与理智计数器同源。

#### 15c. 未来：图标式 HUD（可行性已验证，尚未实现）

用户设想换成「和原版饱食度类似的 HUD，脑子图标」。已验证的证据：

- **贴图零代码注册**：`DirectoryLister` 走 `ResourceManager.listResources` 遍历所有命名空间，`FileToIdConverter.fileToId` 用 `withPath` 保留命名空间。所以把图放进 `assets/abyssfall/textures/gui/sprites/hud/brain_full.png` 就自动进 GUI 图集、得到 `abyssfall:hud/brain_full`，**无需注册代码/Mixin/datagen**，与 vanilla 的 `hud/food_full` 平等。附带好处：玩家可用资源包替换。
- **排布抄 `Gui.renderFood`**：10 个图标从右往左排、间距 8、三套贴图（empty/half/full）、20 点刻度。San 映射就是 `round(percent / 5)`。
- **位置系统一行不用改**：`OCCUPIED_HEIGHT = 10` 正是 vanilla 一行状态栏高度，只需重写 `render` 内容。
- **vanilla 现成范式**：饥饿效果换整套贴图（`Gui:919-921`）、饱和度为 0 时 `y += random.nextInt(3) - 1` 抖动（**与 San 主题契合度极高**）、受伤时 `healthBlinkTime` 闪烁。

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

tag：`0.1-Dev`、`v0.2-Dev`、`v0.3-Dev`。**提交历史和哈希一律现场核实**（`git --no-pager log --oneline -5; git status --short; git --no-pager tag`），别信文档里写死的。

### tag 命名规则（重要）

`0.1-Dev` 无前缀，`v0.2-Dev` 起改为**带 `v` 前缀**。workflow 触发器同时接受两种形状：
```yaml
tags:
  - '[0-9]+.[0-9]+*'      # 匹配 0.1-Dev
  - 'v[0-9]+.[0-9]+*'     # 匹配 v0.2-Dev
```
**别简化成只留一种**——旧 tag 还在，两种都要能构建。

tag 名与 `gradle.properties` 的 `version` **不必一致**：`version=0.3-Dev`（jar 内的版本号，无 `v`），tag 是 `v0.3-Dev`。

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

### 发布流程

```powershell
git add -A; git commit -m "..."; git push origin main
git tag -a '<版本>' -m 'AbyssFall <版本>'; git push origin '<版本>'   # CI 自动构建并附加三个 jar
```
也可在 Actions 页面手动 `workflow_dispatch` 选任意已有 tag 触发，不必重新打 tag。

**两个坑**：
- push 时偶发 `schannel: failed to receive handshake`。**读操作（`ls-remote`）正常、写操作失败 → 是网络不稳定，不是权限问题**，重试即可（有次连失败 3 次、第 4 次成功）。重试循环的 `Start-Sleep` 总时长要控制在 30 秒内。
- push 成功时 git 会把进度写到 stderr，**PowerShell 会因此报非零退出码**。看输出里有没有 `<old>..<new> main -> main` 才是判断依据，别信退出码。

### GitHub 的一个认知点

用户曾疑惑「手动用 tag 构建版本，Assets 里只有 Source code」。**这是正常的**：那两个源码包由 GitHub 按 tag 自动生成、无法删除；编译产物 GitHub 永远不会自己生成，必须上传或用 CI。「用 tag 创建 Release」只是建了个条目，没有发生任何编译。

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

9. **工具的硬限制**（都实际撞过）：`editor` 单次 6000 字符上限，写长文件要分段；`editor` 不能无 `old_text` 覆盖已存在文件，要整体重写就先 `Remove-Item`；`run_commands` 30 秒硬上限，`Start-Sleep 45` 直接失败、长构建可能被截断（重跑看 `UP-TO-DATE` 判断上次是否已成功）；`Select-String` **没有** `-Recurse`，要递归搜得先 `Get-ChildItem -Recurse -File src -Include *.java` 再管道；PowerShell **不支持 heredoc**，多行 commit message 要先写进临时文件再 `git commit -F`。

10. **别凭记忆写版本号**。我第一版写 `upload-artifact@v4`，查证后实际最新是 v7。GitHub Actions 生态迭代快，写之前去 `releases/latest` 查。同理适用于 Minecraft API：本轮我按印象写 `ARGB.lerp`（1.21.11 **不存在**，改用逐通道 `Mth.lerpInt`）、又搞错 `Util` 的包名（是 `net.minecraft.util.Util`）。**颜色和工具类的 API 变动频繁，写之前用 MCP 核实。**

11. **删 tag 重建时先确认远端删成功了再建本地**。我这次远端删除失败（网络）但本地已删，导致中间状态不一致，多花了几轮才理清。

12. **别把文档里的「原则」当成「待办」**。第二次交接时，我读到 HANDOFF 写「零 Mixin」、又在代码里看到 `ServerPlayerEvents.JOIN` 钩子，就当成矛盾去报给用户。用户澄清：他的原则是「最大程度不用 Mixin、优先用 Fabric API 事件」，而**代码里存在的钩子都是不得不用的、已经评估过的**。教训：看到文档与代码「像是」冲突时，先读代码注释里的理由，多数时候上一个你已经解释过了。

13. **不要凭公式自证，跑一遍**。`flower_chance` 的权重换算我推导完觉得没问题，实际编译跑起来才发现 `p=1.0` 会算出 50% 而不是 100%；同一轮还发现 `optionalFieldOf` 会让默认配置文件写成 `{}`。**这两个 bug 都是「跑」发现的，读代码读不出来。** 涉及数值换算、序列化、或任何「无头环境会不会炸」的判断时，写个临时类挂到项目真实 classpath 上跑一次，成本极低：
```powershell
# 临时往 build.gradle 加个任务取 classpath，用完删掉
tasks.register('afPrintCp') { doLast { println sourceSets.main.runtimeClasspath.asPath } }
# 然后 javac -nowarn -cp <classpath> -d out Check.java && java -cp "out;<classpath>" Check
```
⚠️ **用完必须清干净，包括 `build.gradle` 里的临时任务**——本轮我的备份文件被中途一次 `Move-Item` 消耗掉，临时任务留在了文件里，最后靠 `git checkout -- build.gradle` 还原。**每轮结束前用 `git status --short` 逐行看，别只看自己记得改过的文件。**

14. **文件名里不能有 `:`**（Windows）。用户要的时间戳格式含冒号，实测创建失败。**任何要写进文件名的用户输入格式都先验一下**，别等运行时才炸。

15. **报告技术约束时先核实，别顺着需求答应**。用户要求「注入失败就 WARN」，我先去读了 `LootTableEvents.Modify` 的签名，发现返回 `void`、不存在失败状态，于是把方案改成「检测配置的表从未加载」并说明了原因。**上一轮我还犯过反例**：没核实就断言「别的 mod 的表往往不是 builtin」，实际 `LootTableSource.MOD` 的 `isBuiltin()` 是 true，判断完全错了。

16. **坐标 API 别按名字猜语义**（本轮同一个坑踩了两次）。`HudStatusBarHeightRegistry.getHeight(id)` 听起来像「高度」，实际返回**顶边 Y 坐标**且**求和不含自身**。我先减了一次 `BAR_HEIGHT`（条飘高一行），用户说「还是高」，我又减了 `OCCUPIED_HEIGHT`（还是高）。**正确做法是先读官方 javadoc 的用法示例**——它写的就是 `guiHeight() - getHeight(id)`，零额外偏移。两次都是我先动手改数字、后找证据。

17. **枚举的「等级」和「名字」不是一回事**。用户说「4 级权限（管理员权限 GameMaster）」，我核实后发现 1.21.11 的 `GAMEMASTERS` 是 **2 级**、4 级叫 `OWNERS`。**报给用户让他选，而不是自己挑一个**——他选了 3 级 `ADMINS`。

18. **工具报「找不到」时，先判断是能力边界还是真不存在**。`analyze_mixin` 只认 Minecraft 类、**不认 Fabric API 的 `impl` 类**，注入 `HudStatusBarHeightRegistryImpl` 时它报 `target_not_found`，但目标其实存在。改用直接读 jar 字节码确认方法存在 + 编译通过来证明签名正确。

19. **改配置键名会静默破坏用户的本机配置**。加字段永不坏旧文件，但改名会让整块回落默认。**改名时必须主动告知用户去改他的 `run/config/abyssfall.json`**，别让他的设置无声失效。详见「配置系统」一节。


---

## 当前状态（第四次交接时）

- 编译：**已验证**，本次会话多次 `gradlew build --offline` 全部 `BUILD SUCCESSFUL`，`remapJar` 实际执行
- Git 工作区：本次会话结束时**已全部提交并推送**，打了 tag `v0.3-Dev`
- 版本：`gradle.properties` 的 `version=0.3-Dev`
- tag：`0.1-Dev`、`v0.2-Dev`、`v0.3-Dev`
- 产物：`build/release/{abyssfall,abyssfall-doc,abyssfall-source}.jar`（需 `releaseJars` 生成）

**用「开工前请做」里的命令现场核一遍 Git 状态，别信文档里的哈希和清单。**

### 已实测通过的功能（用户在真实环境验证，别再列成待确认项去催他测）

**San 系统全部通过**，包括 `/san` 8 条、持久化、死亡保留、权限分级、**以及客户端同步**（曾悬了两次交接的未验证项，第三次会话已关闭）。

**内容与交互**：创造标签双色/三色标题、tooltip 不变蓝、深渊之花 EPIC 紫色、宝箱掉落、药水效果必定掉落、村民箱子不掉落、玫瑰可种深渊污泥而其他作物不可、骨粉催熟出花且玫瑰被消耗、灵魂特效、三个成就正常触发。

**配置系统**：文件生成/读取/各项生效、开发者物品栏条件注册（`false` 时标签与物品都不存在）、理智计数器 3 秒淡出与重按续期、**坏 JSON 备份+重写**（用户提供了日志证据）。

**第四次会话**：HUD 全部行为（100% 时无 HUD 且不占空间、颜色与条长随 San 变化、紧贴饱食度且饱食度不移位、约 1 秒淡出且结束时上方不跳、氧气条/物品名不压住、F1 一同隐藏、三处读数一致）、DEV 图标（第一版 V 平底被读作 DEU，改 5 列尖底后通过）、`dev_tools`/`dev_command` 拆分、`/san` 3 级权限。**测试协议系统是用户自己 build 后放进真实玩家环境测的**（非 runClient），所以 Swing 弹窗在 `preLaunch` 时机确实能正常显示。

### 我用真实 classpath 实测过的行为（非推断）

**第三次会话（9 项）**：默认配置文件三块完整输出、往返一致、默认 18 张表、权重换算 5 个值精确、`p=1.0` 走 guaranteed、`p=0.0` 不注入池、粒子倍率（含 `0.05→1` 不归零）、音量倍率、残缺文件其余项回落默认、外部 mod 表 ID 可解析、越界值整块回落。

**第四次会话（20 项）**：配置（默认写出含两项 developer + hud 块、旧键与缺字段都整块回落、两键齐全才生效、旧文件其余块不受影响且自动补 hud 块）；测试协议（无头环境不抛异常、正确降级、密钥比较 8 个输入全对）；DEV 图标（逐像素比对只含两种 ARGB 值）；jar 内容（解包确认 `preLaunch` 入口点与 `agreement` 两个 class 在包内）。

### CI 状态

首次运行（0.1-Dev）**成功**，三个 jar 已附加到 Release，详见上面「GitHub Actions」一节的 SHA256 比对。**第四次会话推送了 tag `v0.3-Dev` 但没有等 CI 跑完** —— 下一个你可以去 GitHub Actions 核实它的运行结果。

---

## 未完成 / 可能的下一步

**San 系统（项目主线）**
- 事件无任何监听者，框架就绪但未接入任何玩法
- 什么行为改变 San、什么行为改变上限 —— 全未设计
- 差异化渲染（San 低的玩家看到不同渲染）—— 用户明确说以后开发
- 未来的 San 显示道具：**用户明确说「我没构思好，等我想出来之后会主动说」，不要主动追问、不要自行设计。** 大纲讨论过的四个待定点（与理智计数器并存、手持还是背包内、单向解锁还是双向切换、是否有耐久/时效）已从待办中划走。

**内容**
- 深渊污泥仍用原版泥土材质、`abyss_gardeners` 成就图标是向日葵、理智计数器图标是原版时钟 `clock_00`（**指针不会转**，原版时钟靠 `range_dispatch` 切 64 个模型才会转）——都是占位，用户说「暂时」
- 深渊之花无实际功能（纯注册占位）
- 「深渊探索者」效果无获取途径，只被战利品侧读取
- 无配方、无 datagen、无自定义音效资源
- DEV 图标物品 `abyss_dev_icon` **刻意不命名**（保持键值显示），这是用户要求，不要「补全」它的 lang key

**配置系统（已可用，可继续扩展）**
- 目前 4 个块 6 个配置项。用户预期「以后可自定义配置会特别多」，架构已就绪，加块加项照「配置系统」一节的流程即可
- **不做热加载**是用户明确要求，别自作主张开
- 被用户明确否决/搁置的候选项，**别再提**：凋零玫瑰能种深渊污泥（核心机制，不进配置）、骨粉催熟机制开关（同上，只有特效可调）、`isBuiltin()` 保留拦截（用户明确要求以后不要再提）
- 我曾建议但**用户尚未表态**的：San 相关阈值不该进配置（San 上限是存档数据，改配置会让新老玩家规则不一致，还可能静默 clamp 玩家数据）。真要做之前先问。
- `ALL_LOADED` 是否在每次 `/reload` 都触发，**仍未实测**。若不触发，未命中表的 WARN 只会在首次加载时报告一次。


---

## 开工前请做

1. 读 `gradle.properties`、`fabric.mod.json`、`AbyssFall.java` 确认状态与本文档一致
2. 读 `src/main/java/com/abyssfall/core/` 全部四个文件——这是项目地基
2b. 读 `src/main/java/com/abyssfall/config/` 全部六个文件——这是第二块地基，以后加配置都走它
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
| `fabric-rendering-v1` | `16.2.10`（第四次交接查出） | HUD 元素注册 + 状态栏高度注册 + 那个 client Mixin 的注入目标 |

⚠️ **`fabric-rendering-v1` 的版本尤其重要**：client Mixin 注入的是它的 `impl` 包内部实现。升级 Fabric API 时必须重新核实（见「15a」的三点警告）。

**另一个可靠的证据来源**：Fabric Loader 自己的 sources jar。查 `preLaunch` 时机、`Knot.init()` 流程、`EnvType`、异常处理路径都靠它（第四次交接大量使用）：
```powershell
$l="$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\net.fabricmc\fabric-loader"
Get-ChildItem -Recurse -Filter '*sources.jar' $l | Select-Object -ExpandProperty FullName
```
**注意本机缓存里有 0.16.14 和 0.19.3 两个版本，项目用的是 0.19.3**（`gradle.properties` 的 `loader_version`），别读错。

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
