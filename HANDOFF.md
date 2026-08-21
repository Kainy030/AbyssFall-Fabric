# AbyssFall 项目交接提示词

> 把下面全部内容作为新对话的第一条消息发出即可。
>
> **这个文件属于你**。它是上一个你留给你的，你也应该在本次工作结束时更新它留给下一个你。
> 用户会明确要求你更新，但即使他不说，发现文档与事实不符时也应该主动修正。
>
> **两个文件，分工明确**（第六次交接拆开，原来是一个 900 行的文件）：
>
> | 文件 | 装什么 | 怎么读 |
> |---|---|---|
> | **`HANDOFF.md`**（本文件） | 角色约定、两块地基（San / 配置）、血泪教训、当前状态、下一步 | **通读**。这些是「不知道就会做错事」的内容 |
> | **`REFERENCE.md`** | 已实现功能逐个的实现细节与设计依据、Git/发布流程、CI、gh 用法 | **按需查**。要动某个已有功能之前，先读它那一节 |
>
> 拆开的理由：前者每次开工都必须看完，后者是「动到才查」的手册。合在一起会让必读部分被淹没。
> **加新内容时按这个界线放**：约定/原则/教训/状态 → `HANDOFF.md`；某个功能怎么实现的 → `REFERENCE.md`。
>
> **维护这两份文件的规矩**（第二次交接时定下，第四、六次补充）：
> - 写进来的每一条都要能说清是「已验证 / 未验证 / 推断」，尤其是别人的验证结果要写明是转述。
> - 会过时的东西（commit 哈希、构建结果、依赖子模块版本）要么标注获取方法让下一个你现场重查，要么就别写死。
> - 修文档前先把要改的地方在项目里核一遍，**不要凭读文档时的印象判断文档写错了**——上一次就有人把「用户要求少用 Mixin」误读成「代码里的钩子都该消灭」。
> - 保持总体意思不变。这两份文件的价值在于连续性，不是在于漂亮。
> - **只写「下一个你会因为不知道而做错事」的内容。** 更新时优先合并同类项、删掉已被推翻的旧状态和纯过程叙述（谁在第几轮说了什么、精确行号、逐条实测清单），不要为了显得详尽而堆砌。用户第四次交接时明确要求过精简。
> - **血泪教训的编号不要重排**（含那个空着的 8 号），有多处交叉引用。

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
- **不要每写完一个功能就更新 `HANDOFF.md` / `REFERENCE.md`**（第七次交接用户明确要求，为省 token）。只在用户要求时、或一轮工作收尾时统一更新。写代码期间发现文档失实，记着，别立刻去改。

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
| 版本 | 0.5-Dev（`gradle.properties` 的 `version`） |
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
**规则化写（第七次交接新增，只收 `ServerPlayer`）**：`erode(player, amount)` + 判定用的 `canErode(player)`。世界侵蚀 San 必须走这里，见「配置系统」的「`san` 块」一节。

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

**第七次交接起 San 已经会自己动了**：`SanBreakdownEffect` / `SanSpiritedEffect` 这一对药水效果每 10 秒扣/回 San（见 `REFERENCE.md` 7b），`erode()` 有了第一个调用方。但这仍不是「世界规则」——**什么情况下给玩家上这个 debuff 依然完全没有设计**，目前只能靠 `/effect` 手动给。真正的侵蚀来源（黑暗、深渊、目击恐怖等）还没有。

未来的差异化渲染（San 低的玩家看到不同的方块/物品渲染）由用户后续提出时再做。

### 🔴 三层信息可见性模型（第四次交接确立，架构级设计意图）

用户的设计意图（原话带三个感叹号）：

> **但是玩家永远不可以知道 San 值的真实 Folt 值，只可以知道百分比，这是故意的游戏设计！！**

他随后澄清了**强度边界**（重要，别按最严格的读）：

> 玩家不可知真实 Folt 数值并不是针对那种逆向的人，而是玩家在游戏过程中不知道，**是游戏性行为，而不是技术行为**……所以这部分无需改动代码。

| 层 | 途径 | 玩家看到什么 | 门禁 |
|---|---|---|---|
| **调试层** | `/san`、理智计数器 | 精确 float | `dev_command` / `dev_tools` + 3 级权限 |
| **游戏内进阶层** | **认知窥镜**（第七次交接已实现） | 百分比（`San: 90.00%`） | 目前无门禁，创造栏可取；将来若要加制作配方再说 |
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
├── SanSettings.java           san 块（第七次交接新增）
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
  },
  "san": {
    "peaceful_prevents_loss": true
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

### `san` 块：玩法规则，不是阈值（第七次交接新增）

`san.peaceful_prevents_loss`（布尔，默认 `true`）：和平难度下不掉理智。

**这一块的定位要守住**：它装的是「**世界在什么情况下有权侵蚀 San**」这类规则，**不装阈值**。San 阈值不进配置这条原则（见「未完成/可能的下一步」）依然有效——所以别因为有了 `san` 块就往里塞 `low_san_percent` 之类的东西。

规则**只在 `AbyssFallCoreSystem.erode(ServerPlayer, float)` 生效**，`addCurrent` / `setCurrent` / `/san` 全部不受影响。这个分界是**按「谁在写」而不是「写多少」划的**：管理员敲 `/san set` 是在陈述玩家的 San 是多少，被难度悄悄改掉会让调试工具说谎；只有游戏自己施加的压力才受规则约束。**以后写侵蚀机制（黑暗、深渊、目击恐怖等）一律走 `erode()`，不要直接 `addCurrent(负数)`**，否则这个开关就被绕过了。

`erode()` 的三个细节：
- 被拒绝时**不发 `SanChangedCallback`**（而不是发一个 no-op）。什么都没发生，报告一个没发生的变化会误导所有只用 `isNoOp()` 过滤 clamp 的监听者。
- 金额守卫写成 `!(amount > 0.0F)` 而**不是** `amount <= 0`，这样 NaN 也会被拒（NaN 的所有比较都为 false）。别「简化」它。
- `canErode(ServerPlayer)` 单独公开，供调用方提前跳过一次注定被拒的计算。

难度取自 `player.level().getDifficulty()`（`LevelAccessor` 的 default 方法，委托 `getLevelData().getDifficulty()`，1.21.11 mojmap 已用 MCP 核实）。**零 Mixin**。

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

⚠️ 注意：**血泪教训里没有第 8 条**。不是漏了，是历次精简时那条被合并进了别处，编号刻意没重排——因为项目里多处注释和文档按编号交叉引用，重排会让所有引用一起失效。**以后加新教训就往后接，别去填 8 这个空位、也别重排。**

18. **工具报「找不到」时，先判断是能力边界还是真不存在**。`analyze_mixin` 只认 Minecraft 类、**不认 Fabric API 的 `impl` 类**，注入 `HudStatusBarHeightRegistryImpl` 时它报 `target_not_found`，但目标其实存在。改用直接读 jar 字节码确认方法存在 + 编译通过来证明签名正确。

19. **改配置键名会静默破坏用户的本机配置**。加字段永不坏旧文件，但改名会让整块回落默认。**改名时必须主动告知用户去改他的 `run/config/abyssfall.json`**，别让他的设置无声失效。详见「配置系统」一节。

20. **「我的函数返回了预期值」不等于「这个值有实际效果」**（第五次交接，最惨的一次）。我给闪光写了个 `brighten()` 把 tint 往白插值，还跑了 8 条断言全绿、宣布「已验证」。用户实测后问「你确定真有闪光吗」——**一点效果都没有**。原因：`blitSprite` 的 tint 是**乘算**的（只能变暗），而 `ARGB.white(alpha)` 的 RGB 本身就是 `0xFFFFFF`，从 255 插到 255 恒等于没动。**我测的是自己那个错函数的算术，完全没碰渲染语义。** 涉及渲染管线（乘算/加算、tint 能做什么、alpha 怎么合成）时，必须去读目标 API 的实际实现，光测自己的输入输出等于没测。vanilla 的做法是**另做一张亮贴图**（`heart/full_blinking`）。

21. **对称的参照物才能验证方向，不对称的会骗你**（同上一轮，用户原话「这TM半格san值左右部分是TM反的」）。我从 `food_half.png` 反推半格图的裁切方向，得出「保留右半」，写进了脚本还注明「实测得来」。实际反了。**鸡腿图案是倾斜不对称的**，它的半格看着像「变瘦」而不是「切掉一半」，所以从它身上读不出方向。改查**心**（`heart/full` vs `heart/half`）立刻清楚：保留左半。**选证据时要挑那个能让结论唯一的样本，而不是手边第一个样本。**

22. **别在用户没说的地方替他做决定，但要把冲突说出来**。用户要求「和原版恢复生命值的效果一模一样」并称之为「高亮脉冲」。我核实后发现原版 `Gui.renderHearts` 只把心**上抬 2 像素**、**亮度一点没改**——「从左到右的行波」对得上，「高亮」对不上。我的处理是照「一模一样」做上抬、另外加了提亮并**明确标注这是我加的、给出关闭方式**。用户随后说「抬亮去掉吧」。**如果我默默按自己的理解二选一，就会要么丢掉他要的效果、要么塞进他没要的东西。**

23. **涉及游戏数值语义的歧义，必须问，不能自己选一个做完再解释**（第七次交接，用户明确训过）。做精神崩溃时「降低玩家 1% 的 san 值」有两种读法——上限的 1%，还是当前值的 1%。我自己选了「上限」，做完才在总结里说明。**用户认可了这个选择，但明确说「下次通过 cline 问我，而不是自己给这个事情做了，我是设计师，你是执行层」、「下不为例」。** 教训 22 的「说出冲突」是**做完再标注**，这条比它更严：**关系到玩法数值的歧义要事前问**。判断标准：如果两种做法会让玩家体验到不同的数值，就问。

24. **别把外部顾虑（法律/洁癖/最佳实践）带进开发阶段的占位工作**（同上一轮，用户原话「你他妈有病吧」）。要求是「图标暂时用原版中毒图标替代」。我担心把 Mojang 的 `poison.png` 拷进 GPL 项目算再分发，于是写了个 PowerShell 脚本自己画了一张。**用户明确批评：占位图最终都会换成自己的美术资产，开发阶段怎么省心怎么来，遇到这类问题要问他。** 这类「我觉得有风险所以绕一下」的自作主张，成本由用户承担（多出一个要维护的脚本），收益是零。**先问。**

25. **`Util.getMillis()` 不是墙钟，不能和 `System.currentTimeMillis()` 相减**（第七次交接，我自己的测试翻车）。它是基于 `System.nanoTime()` 的单调时钟，纪元完全无关。我在验证 HUD reveal 窗口长度时混用了两者，得到 `-1787144507646` 这种荒谬值，白排查一轮。生产代码全程只用 `Util.getMillis()` 所以没受影响，但**写验证代码时时钟来源也要对齐**。


---

## 当前状态（第七次交接时）

- 编译：**已验证**，多次 `gradlew build --offline` 全部 `BUILD SUCCESSFUL`，`remapJar` 实际执行
- 版本：`gradle.properties` 的 `version=0.5-Dev`
- 产物：`build/release/{abyssfall,abyssfall-doc,abyssfall-source}.jar`（需 `releaseJars` 生成）

**Git 状态、tag 列表、CI 结果一律现场核实**，别信文档里写死的（命令见「开工前请做」，`gh` 用法见 `REFERENCE.md`）。截至第七次交接：tag 有 `0.1-Dev`、`v0.2-Dev`、`v0.3-Dev`、`v0.4-Dev`、`v0.4-Dev-Fix`、`v0.5-Dev`，Release workflow **全部 `completed success`**（1m25s ~ 1m49s，每次三个 jar）。

### 已实测通过的功能（用户在真实环境验证，别再列成待确认项去催他测）

**San 系统全部通过**：`/san` 8 条、持久化、死亡保留、3 级权限、客户端同步。

**内容与交互**：创造标签双色/三色标题、tooltip 不变蓝、深渊之花 EPIC 紫、宝箱掉落与概率、药水效果必定掉落、村民箱子不掉落、玫瑰可种深渊污泥而其他作物不可、骨粉催熟出花且玫瑰被消耗、灵魂特效、三个成就正常触发。

**配置系统**：生成/读取/各项生效、开发者内容条件注册（`false` 时标签与物品都不存在）、理智计数器 3 秒淡出与重按续期、坏 JSON 备份+重写。

**HUD（两种读数都已通过）**：100% 时不显示且不占空间、紧贴饱食度且饱食度不移位、约 1 秒淡出且结束时上方不跳、氧气条/物品名不被压、F1 一同隐藏；图标行的半格映射、抖动、下降抖一下、上升行波、回满闪光；进度条的颜色与长度随 San 变化、RGB 高亮闪烁、整条抖动与抬升；认知窥镜双向切换与 500ms reveal。

**药水效果**：精神崩溃五级掉 San、精神饱满五级回 San、同级并存净变化为 0、**和平模式拦住精神崩溃但不拦精神饱满**、`/san add` 在和平模式仍生效（`erode` 与 `addCurrent` 的分界得到验证）。

**测试协议系统**：用户 build 后放进真实玩家环境测的（非 runClient），Swing 弹窗在 `preLaunch` 时机确实能显示。

### 唯一仍未验证的观感项

**连续小额恢复会不会一直闪、显得吵。** 每次数值变动都重启慢闪，若 San 每 tick 涨一点，闪光会被不断重启从而一直停在亮相。真出现就加个最小间隔；四个常量在一起（`FULL_FLASH_BLINK_TICKS` / `FULL_FLASH_BLINKS` / `GAIN_FLASH_BLINK_TICKS` / `GAIN_FLASH_BLINKS`），两个 HUD 元素各有一套。

### 我用真实 classpath 实测过的行为（非推断）

**这条路成本极低、用户很认这种证据，遇到算术/数据/时序问题就用。** 做法：临时 init script 导出 `runtimeClasspath` → `javac -proc:none` 编译一个测试类 → 跑 → **删掉临时文件**。碰 `MobEffect` / 注册表要先 `SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();`（但 bootstrap 会**冻结注册表**，之后就碰不了 `AbyssFallEffects` 了）。

累计约 180 项，覆盖：配置往返与各类残缺回落、战利品权重换算与 `p=1.0`/`p=0.0` 特判、测试协议密钥比较与无头降级、DEV 图标逐像素、jar 内容解包、HUD 全部动效时序（半格映射/抖动周期/行波顺序/闪光次数/时钟倒流取消）、两个药水效果的数值表与镜像对称、认知窥镜切换与语言键完整性、进度条高亮的 RGB 计算、reveal 时间轴。

**注意血泪教训 20：这类验证证明不了渲染效果。**

**它抓到过读代码抓不到的真 bug**：`p=1.0` 只出 50%、默认配置写成 `{}`。

---

## 未完成 / 可能的下一步

**San 系统（项目主线）**
- `SanChangedCallback` 仍无任何监听者。框架就绪，等玩法来用
- **什么情况下侵蚀 San —— 这是最大的空白。** 药水效果已经能扣 San 了，但没有任何东西会给玩家上那个 debuff。黑暗、深渊、目击恐怖等真正的侵蚀来源全未设计
- **扣 San 一律走 `AbyssFallCoreSystem.erode()`**，别直接 `addCurrent(负数)`，否则绕过 `san.peaceful_prevents_loss`
- 差异化渲染（San 低的玩家看到不同渲染）—— 用户明确说以后开发
- San 显示道具**已实现**（认知窥镜），四个待定点用户已定：双向切换、手持右键、无耐久时效、与理智计数器并存

**内容**
- **所有图标都是占位**，正式发布前统一换成自己的美术资产：深渊污泥用原版泥土材质、`abyss_gardeners` 成就图标是向日葵、理智计数器与认知窥镜都用原版时钟 `clock_00`（**指针不会转**，原版靠 `range_dispatch` 切 64 个模型才会转）、精神崩溃/精神饱满是脚本生成的图
- 深渊之花无实际功能（纯注册占位）
- 三个药水效果都**无获取途径**：「深渊探索者」只被战利品侧读取，精神崩溃/精神饱满只能 `/effect` 手动给
- **反精神崩溃魔咒：用户已给完整数值，明确说「先记录，不要实现」**（数值见 `REFERENCE.md` 7b 末尾）
- 无配方、无 datagen、无自定义音效资源
- DEV 图标物品 `abyss_dev_icon` **刻意不命名**（保持键值显示），这是用户要求，不要「补全」它的 lang key

**配置系统（已可用，可继续扩展）**
- 目前 **5 个块、8 个配置项**（`developer` 2 + `loot` 2 + `visuals` 2 + `hud` 1 + `san` 1）。用户预期「以后可自定义配置会特别多」，架构已就绪
- **不做热加载**是用户明确要求，别自作主张开
- 被用户明确否决/搁置的，**别再提**：凋零玫瑰能种深渊污泥（核心机制，不进配置）、骨粉催熟机制开关（只有特效可调）、`isBuiltin()` 保留拦截
- **San 阈值不该进配置**（我建议、用户尚未表态）：San 上限是存档数据，改配置会让新老玩家规则不一致，还可能静默 clamp 玩家数据。真要做之前先问。注意 `san` 块装的是「**规则**」不是阈值，别因为有了这个块就往里塞 `low_san_percent`
- `ALL_LOADED` 是否每次 `/reload` 都触发，**仍未实测**。若不触发，未命中表的 WARN 只在首次加载报一次

---

## 开工前请做

0. **本文件通读完**，再按需查 `REFERENCE.md`（要动哪个已有功能就读它那一节，别通读）
1. 读 `gradle.properties`、`fabric.mod.json`、`AbyssFall.java` 确认状态与本文档一致
2. 读 `src/main/java/com/abyssfall/core/` 全部六个文件——这是项目地基（`SanHudMode` / `SanHudModeState` 是第七次交接加的，只是显示偏好，不属于 San 数据本身）
2b. 读 `src/main/java/com/abyssfall/config/` 全部七个文件——这是第二块地基，以后加配置都走它
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
[xml]
$x=Get-Content $f; $x.project.dependencies.dependency | ForEach-Object { "$($_.artifactId) $($_.version)" }
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

⚠️ **`fabric-rendering-v1` 的版本尤其重要**：client Mixin 注入的是它的 `impl` 包内部实现。升级 Fabric API 时必须重新核实（见 `REFERENCE.md` 的「15a. 位置系统」那三点警告）。

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
- **该问就问，但别为小事问**。视觉细节、翻译、注释直接改；**涉及玩法语义、数值含义、数据结构走向时必须先问**（血泪教训 23、24）。
- **保持简洁**。他明确说过省 token。长回复只在真的有必要时用。
- 🔴 **不要动那些看起来「不够优雅」的代码。** 用户第七次交接原话：「项目中所有看起来'不太优雅'的代码全部都是你或者你曾经经过验证后不得不这么做的妥协之法，不要瞎鸡巴乱改。」

  **每一处都有写在注释里的理由，先读理由。** 已知的一批：
  - `ServerPlayerEvents.JOIN` 钩子、`set()` 里的回读（attachment 会 clamp，必须回读才知道存进去的是什么）
  - 双色标题的空根组件（`CreativeModeInventoryScreen` 只替换根组件 style）
  - 代码授予成就、配置读写用**两个不同 Codec**（`fieldOf` 写 / `LENIENT_CODEC` 读，合并会让新文件变成 `{}`）
  - `p=1.0` 和 `p=0.0` 的特判
  - 图标 HUD 用**两套贴图**做高亮而不是调 tint（`blitSprite` 的 tint 是乘算，只能变暗）
  - 进度条却用 RGB 插值做高亮（`fill()` 颜色直给）——**这两处「不一致」是对的，别统一**
  - `erode()` 里 `!(amount > 0.0F)` 而非 `amount <= 0`（为了拦 NaN）
  - 认知窥镜判 `isClientSide()` 而非 `instanceof ServerPlayer`（单人世界会切两次）
  - 只注册**一个** HUD 分发元素而不是两个（注册表会冻结、Mixin 只认一个 id）
  - reveal 用 `Math.max(lastShownAt, revealEndsAt())`（双向边界）
  - 两个 Mixin（`WitherRoseBlockMixin`、`HudStatusBarHeightRegistryImplMixin`）

  他要求过「最大程度按 HANDOFF 执行，有矛盾随时通知我」——照做，但矛盾要先自己核实过再报。
- **能跑就跑一遍**。真 bug（`p=1.0` 只出 50%、默认配置写成 `{}`）都是靠临时编译一个测试类挂真实 classpath 跑出来的，读代码读不出来。用户不要你跑 runClient，但**不禁止你跑纯 Java 验证**，这条路成本极低且他很认这种证据。

祝顺利。
