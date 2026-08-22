# AbyssFall 交接（必读，通读完再动手）

> **本文件属于你**，上一个你留给你的，收尾时更新留给下一个你。与事实不符即主动修正。
>
> | 文件 | 内容 | 读法 |
> |---|---|---|
> | **`HANDOFF.md`** | 约定、两块地基（San/配置）、教训、状态、下一步 | **通读** |
> | **`REFERENCE.md`** | 各功能实现要点与禁忌、Git/CI/发布 | **按需查**，动某功能前读它那节 |
>
> **🔴 本文件的写法约定（重要，决定了你该怎么读它）**
>
> 用户原话：「项目中所有看起来'不够优雅'的代码全部都是你或者你曾经经过验证后不得不这么做的妥协之法，**不要瞎鸡巴乱改**。」
>
> 因此这两份文档**刻意不再保留「当年为什么这么做」的举例论证与复现过程**——那些验证都已经做过了。留下的是**结论、禁忌、数值、API 事实**。看到标注「勿改 / 别统一 / 刻意如此」的地方，就当作已验证的既成事实执行；**真要推翻它，先自己重做验证并报告，不要凭直觉改**。
>
> **维护规矩**：①每条标清「已验证/未验证/推断/未知」，转述要写明；②易过时的（哈希、构建结果、子模块版本）写获取方法别写死；③改文档前先在项目里核一遍，别凭印象判断文档错了；④只写「不知道会做错事」的，不写过程叙事；⑤**教训编号不重排**（含空的 8 号）；⑥同一件事只在一处讲透，别处引用；⑦约定/原则/教训/状态→本文件，某功能怎么实现→`REFERENCE.md`。

## 1. 角色与协作

**用户（Kainy）给设计与需求，你让它运作起来**，实现自由度很高。

**已授权自主项**（不必问，改完告知）：视觉/听觉细节（配色看不清直接换）、翻译（有语法错直接改，**必须避免机翻感与僵硬表达**）、注释/javadoc/命名/内部结构、**全部 GitHub 操作**、**开发环境问题**。

**验证约定**（为省 token）：
- **不要自己跑 `runClient`**——他跑得更快。你需要确认什么就列成待测项交给他。
- 大改动才 `gradlew build`；小改动只做代码验证（读文件 + MCP 核实 API），**不构建**。
- 回复简洁，**但结论必须分清「已验证/未验证/推断/未知」**。宁可说「我没验证」也不要含糊。
- **不要每写完一个功能就更新这两份文档**，只在他要求或一轮收尾时统一更新。

**判断顺序：自有数据结构 → Fabric API/事件 → Mixin。** 他的原话（照抄以免走形）：

> 「原则是尽量不使用 mixin，因为我以前是写外挂的，我的思考方式就是遇事不决用钩子，所以需要你来最大程度地不用 mixin，用 Fabric API 事件。但凡是有例外，有时候不得不用钩子的时候就要放心大胆地用钩子，**你在代码中看到的钩子就是不得不用的情况**。」

⇒ 目前仅一个 Mixin：`client/mixin/HudStatusBarHeightRegistryImplMixin`。**不要当技术债清理、不要试图用 API 重写。** 写新功能时优先找 API 事件，找不到再注入并说明理由。

**其他相处方式**：
- 他问「这两个功能有什么区别」是真想搞清语义边界 → 直接答区别 + 什么情况下才看得出差异。说「简单回复即可」时别长篇大论。
- **「本轮只做理论验证，不要写代码」** → 只出方案、只收集证据，不改文件。该问的一次问清。
- **冲突取舍**：「保留自己的权益的同时尊重他人，而不是牺牲自己的权益去尊重他人」。**不要提「为礼貌而放弃功能」的方案**（他说过这类选项以后不要再提）；可以提「照做 + 记日志让冲突可见」。
- **架构变动先报告**：「改代码最好别改项目架构，如果需要更改也要告知我」。
- **语言**：中文（zh-CN）。

## 2. 项目基本信息（已验证）

| 项 | 值 |
|---|---|
| 路径 / modid / 包名 | `D:/MC26.2-AbyssFall-Fabric` / `abyssfall` / `com.abyssfall` |
| Minecraft | **26.2**；**无映射**（26.1 起不再混淆，Fabric 停止维护第三方映射） |
| Loader / Loom / Fabric API | 0.19.3 / 1.17.19（插件 id **`net.fabricmc.fabric-loom`**）/ 0.158.0+26.2 |
| Gradle / JDK | 9.7.0 / **25**（`java-runtime-epsilon`），toolchain 与 `release` 都是 25 |
| 版本 / 许可 | `1.1-Dev` / GPL-3.0-or-later（**每个 .java 带 GPL 头，新文件照抄**） |
| 源集 | `splitEnvironmentSourceSets()`：`src/main` + `src/client` |
| Git | `https://github.com/Kainy030/AbyssFall-Fabric.git`，分支 `main` |

**类名是 Mojang 名**（`net.minecraft.world.item.Item`、`net.minecraft.resources.Identifier`）。**不要写 Yarn 名。**

```powershell
cd D:/MC26.2-AbyssFall-Fabric
.\gradlew.bat build --console=plain
.\gradlew.bat compileJava --console=plain --rerun-tasks   # 强制重编译，暴露警告
.\gradlew.bat releaseJars --console=plain                 # 三个发布 jar
```
⚠️ **别加 `--offline`**（26.2 的 MC 依赖库要联网拉，离线会失败在依赖解析上）。⚠️ 工具 30 秒上限，`build` 常被截断 → `Start-Process -RedirectStandardOutput` 后台跑再分次读日志。

`runProductionClient` / `runProductionServer`：**用户明确说已无意义**，别用也别推荐；留着不删是避免无关改动。远端 **`1.21.11` 分支已冻结**，可回查但**不要**按它写代码。

---


## 3. 地基一：San 值系统

用户定位：「这个系统贯穿始终，决定了咱们这个项目玩法的基础，咱们这个项目是随着 san 进行推进的，这是一个很重要的变量。」

### 3.1 🔴 San 是连续参数，不是状态机

用户原话：

> 「不要做成传统的线性分段状态……我希望 0%～100% 的整个百分比区间都可以拥有不同的行为规则……例如 San ≥ 80% 时保持正常，一旦从 80% 以下开始下降，即使只下降 0.1%，也可以触发一次对应的状态变化……所以 San 百分比应该被视为一个连续参数。」

**禁忌：不要往 core 引入任何档位枚举或阈值常量**（初版的 `SanStage` 已被他否决并删除）。阈值属于消费方，core 不表态。

### 3.2 结构与 API

`core/`：`AbyssFallCoreSystem`（门面：attachment 注册 + 全部读写 + 事件派发）、`SanState`（不可变 record，自带 Codec/StreamCodec）、`SanChangedCallback`、`SanHudMode` + `SanHudModeState`（只是显示偏好，见 `REFERENCE.md` 15c）、`AbyssFallSanCommand`。

- **读**（两端安全，收 `Player`）：`get` `getCurrent` `getMax` `getRatio` `getPercent`
- **写**（只收 `ServerPlayer`）：`set` `modify` `addCurrent` `setCurrent` `addMax` `setMax` `restore` `reset`
- **规则化写**：`erode(player, amount)` + `canErode(player)` ← **世界侵蚀 San 必须走这里**（见 4.5）
- `SanState`：`ratio()` `percent()` `isFull()` `isEmpty()` `withCurrent/addCurrent/withMax/addMax` `full(max)` `INITIAL`；常量 `DEFAULT_MAX=100` `MIN_MAX=1` `MAX_MAX=10000`
- `Change`：`currentDelta()` `maxDelta()` `ratioDelta()` `isNoOp()` `crossedDown(t)` `crossedUp(t)`——阈值由调用方传入：

```java
if (change.crossedDown(0.80F)) { ... }           // 跌破 80% 那一瞬，只触发一次
float intensity = f(change.current().ratio());   // 随 San 连续变化，无阈值
```

### 3.3 实现事实与禁忌（零 Mixin，一个 API 事件钩子）

**attachment 注册名 `abyssfall:core_system_san`**（用户指定，是存档 key，**改名会孤立所有存档**）。Fabric Data Attachment API，builder 四项：`initializer(() -> SanState.INITIAL)`、`persistent(CODEC)`、`copyOnDeath()`（San 是经历的记录，重生不洗白）、`syncWith(STREAM_CODEC, targetOnly())`（只同步本人）。

**四处勿改的写法**（已验证的妥协，别「优化」）：
1. **current 与 max 合成一个 record**，invariant 在 canonical constructor 强制。
2. **`ServerPlayerEvents.JOIN` 钩子不是多余的**：回调做一次 `getAttachedOrCreate(SAN)`，否则全新玩家可能没有存储的 attachment、不触发同步推送。**这是 API 事件不是 Mixin。**
3. **事件从 `set()` 派发，不用 `onAttachedSet`**：后者按 target 实例订阅（`default <A> Event<OnAttachedSet<A>> onAttachedSet(...)`），无法全局监听。
4. **`set()` 里回读两次**（`previous` → `setAttached` → `stored`）：传入值可能被 clamp，事件必须携带真实存储值。

### 3.4 两个语义决策（他可能会问，也可能想改）

1. **提高上限不白送 San**：`withMax` 提高时 current 不动；降低到 current 以下才会把 current 拖下来。
2. **`restore`** = current 回满到当前上限；**`reset`** = current 与上限一起回默认 100。上限没动过时两者结果相同。

### 3.5 `/san` 命令（8 条）

**两道门**：整棵树要 `dev_command=true` 才注册，且全部分支要 3 级 `LEVEL_ADMINS`。分支：`/san`、`query` `set` `add` `max set` `max add` `restore` `reset`。输出 `<名字>: San 100.00 / 100.00 (100.00%)`（刻意用英文调试格式、无 lang key，两位小数为看清 0.1% 级变化）。

- `.requires()` **只写在根节点一处**（Brigadier 对失败节点不向下遍历）。**别「补全」成每分支一遍。**
- **无参数 `/san` 也要 3 级**：玩家只应通过游戏内手段得知百分比、永不得知底层 float（见 3.7），打印 float 的指令是 debug 设施而非权利。已写进 javadoc，别当旧注释删掉。
- 权限 API：`Commands.hasPermission(Commands.LEVEL_ADMINS)` 返回 `PermissionProviderCheck<T>`（`net.minecraft.server.permissions`，`record ... implements Predicate<T>`），可直接传 `.requires()`。
- ⚠️ **`PermissionLevel` 枚举**（MCP 实测）：`ALL`0 / `MODERATORS`1 / **`GAMEMASTERS`2** / `ADMINS`3 / **`OWNERS`4**。**`GAMEMASTERS` 不是 4 级。**
- ⚠️ 26.2 内部重构过：`LEVEL_ADMINS` 现在是 `new PermissionCheck.Require(Permissions.COMMANDS_ADMIN)`。编译通过、用户实测 `/san` 可用，但**是否与旧「3 级」严格等价，未验证**。

### 3.6 当前状态：只有框架，零世界规则

`SanChangedCallback` **没有任何监听者**——预期如此（「我们现在要的是框架」）。San 已经会自己动了（两个药水效果每 10 秒扣/回，`REFERENCE.md` 7b，`erode()` 有了第一个调用方），但**什么情况下给玩家上这个 debuff 完全没设计**，目前只能 `/effect` 手动给。真正的侵蚀来源（黑暗、深渊、目击恐怖等）还没有。

### 3.7 🔴 三层信息可见性模型

用户原话（带三个感叹号）：

> **但是玩家永远不可以知道 San 值的真实 Folt 值，只可以知道百分比，这是故意的游戏设计！！**

**强度边界**（重要，别按最严格的读）：

> 玩家不可知真实 Folt 数值并不是针对那种逆向的人，而是玩家在游戏过程中不知道，**是游戏性行为，而不是技术行为**……所以这部分无需改动代码。

| 层 | 途径 | 看到 | 门禁 |
|---|---|---|---|
| 调试 | `/san`、理智计数器 | 精确 float | `dev_command`/`dev_tools` + 3 级 |
| 游戏内进阶 | 认知窥镜 | 百分比 | 无门禁，创造栏可取 |
| 游戏内基础 | HUD 默认态 | 图标（约 5% 粒度） | 无 |

**核心原则：内部连续、外部模糊。** 内部读真实 ratio；玩家感知是粗糙的，而「能知道多精确」本身是玩法内容。所以图标 HUD 看不出 5% 以内变化**是设计意图，不是与「连续参数」矛盾**。

**已知且被接受的「泄漏」，不要修**：`STREAM_CODEC` 把两个 float 同步给客户端。**用户明确说不改。**

**只需守住一件事：所有游戏内官方界面只显示百分比。** 新增任何 San 显示途径时，先问它属于哪一层。

---


## 4. 地基二：配置系统

用户定位：「我预感咱们项目以后的可自定义配置会特别多，别欠技术债，趁现在没什么代码的时候赶紧重构」。**为「配置项会长到很多」设计的，加配置沿用它，别另起炉灶。**

### 4.1 格式与结构

`config/abyssfall.json`。**是 JSON 不是 properties**（结构化配置需要数组；代价是不能写注释，已接受。旧 `.properties` 实现已删，别复活）。

`config/` 七个文件：`AbyssFallConfig`（静态门面 `load()`/`save()`/`get()` + 便捷访问器）、`AbyssFallConfigData`（根 record）、`DeveloperSettings` / `HudSettings` / `LootSettings` / `SanSettings` / `VisualSettings`。每块是 `record` + 三件套 `DEFAULT` / `CODEC` / `LENIENT_CODEC`。

- **加一项**：块的 record 加字段 + `CODEC` 加一行 `fieldOf` + `DEFAULT` 给值。
- **加一块**：照抄任一现有块，然后在 `AbyssFallConfigData` 加字段 + `fieldOf(...).orElse(...)`。

### 4.2 🔴 读写用不同 Codec（勿「优化」成一个）

| | 用什么 | 结果 |
|---|---|---|
| 写 | `CODEC`（`fieldOf`） | 总是输出全部字段，玩家看得见有什么可配 |
| 读 | `LENIENT_CODEC`（`CODEC.orElse`） | 缺字段/整块坏 → 回落该块默认，**不废掉整个文件** |

**不要两边都用 `optionalFieldOf`**：它在值等于默认值时编码会省略该字段，会让默认配置文件被写成 `{}`。

**副作用是好的**：加字段永不破坏旧文件 ⇒ **不需要任何迁移代码**。

⚠️ **但改键名会破坏旧文件**：旧键无人认领、新键缺失 → **整块**回落默认，用户的设置静默失效。⇒ ①**改键名前必须告知用户手改本机 `run/config/abyssfall.json`**（教训 19）；②**块是原子单元**（一块里任何字段缺失/不合法则整块回落，这是 `CODEC.orElse(DEFAULT)` 的固有行为不是 bug）。

⚠️ `Codec.orElse`/`MapCodec.orElse` 有两个重载，**直接传 lambda 编译不过（引用不明确）**，必须显式 `(Consumer<String>)` 强转。**代码里那些 cast 是必需的。**

### 4.3 三种失败必须区别处理（别合并）

| 情形 | 行为 | 日志 |
|---|---|---|
| 文件不存在 | 写一份默认 | INFO |
| **JSON 语法坏** | **备份原文件 + 写默认** | ERROR |
| 语法对但值不合法 | 该块回落默认，**其余照常生效，不动文件** | WARN |

第三种**刻意不重写文件**。`read()` 返回 `null` 表示第二种、返回对象表示第一/三种——这个 null 语义写在 javadoc，别改成 Optional 顺手改掉含义。

**备份名** `abyssfall.json.broken-yyyy-MM-dd_HH-mm-ss`，同秒撞名追加 `-2`/`-3`。**分隔符必须是 `-` 和 `_`，不能用 `:`**（Windows 拒绝含 `:` 的文件名，rename 会失败导致坏文件既没备份也没替换）。这条路径用户已实测可用。

### 4.4 不做热加载（用户明确要求）

只在 `onInitialize()` 读一次，改完必须重启。**这是需求决定不是技术限制**（`LootTableEvents.MODIFY` 本身每次数据包重载都会触发，想开热加载只需在回调里实时读配置）。**别自作主张开。**

### 4.5 当前 5 块 8 项（默认值 = 改动前的行为，逐值实测对齐）

```json
{ "developer": { "dev_tools": false, "dev_command": false },
  "loot":      { "flower_chance": 0.05, "target_tables": [ "...18 个 minecraft:chests/..." ] },
  "visuals":   { "bloom_particle_scale": 1.0, "bloom_sound_volume": 1.0 },
  "hud":       { "show_below_percent": 100.0 },
  "san":       { "peaceful_prevents_loss": true } }
```

- **`dev_tools`** 管开发者物品栏标签 + 里面的物品是否**注册**；**`dev_command`** 管 `/san` 是否**注册**。拆成两项是因为「创造世界用 debug 物品」和「服务器开指令」是两个不同的决定。两项默认 `false`。
- **`hud.show_below_percent`**：百分比**低于**此值时显示。默认 `100.0` = 满值不显示、掉一点就显示。范围 `[0,100]`，**`0` 等于彻底关闭 HUD**（已写进 javadoc，不是漏洞）。
- ⚠️ 用户要求「所有默认值除开发者模式外全部按项目当前状态写」。**改默认值 = 改游戏行为。**

**🔴 `san` 块装「规则」不装阈值**：`peaceful_prevents_loss`（默认 `true`）= 和平难度不掉理智。**别往里塞 `low_san_percent` 之类的东西。**

规则**只在 `erode()` 生效**，`addCurrent`/`setCurrent`/`/san` 全不受影响。**这个分界按「谁在写」而非「写多少」划**：管理员敲 `/san set` 是在陈述事实，被难度悄悄改掉会让调试工具说谎。**以后写侵蚀机制一律走 `erode()`，不要 `addCurrent(负数)`**，否则这开关被绕过。

`erode()` 三处勿改：①被拒时**不发事件**（而非发 no-op）；②守卫写 `!(amount > 0.0F)` 而**非** `amount <= 0`（这样 NaN 也被拒），别「简化」；③`canErode()` 单独公开供提前跳过。难度取自 `player.level().getDifficulty()`（26.2 已核实）。**零 Mixin。**

**`flower_chance` 是概率不是权重**（用户要求「表达为玩家理解的概率，而不是暴露内部 LootPool 权重」）。换算在 `LootSettings.emptyWeight(int)`：`EMPTY = max(1, round((1-p)/p * flowerWeight))`，`FLOWER_WEIGHT` 恒为 1。**两个边界特判必须保留**：`p >= 1.0` → `isGuaranteed()` → **不加空条目**（否则实际只有 50%）；`p <= 0.0` → `injectsBaselinePool()` 为 false → **整个基础池不注入**。实测：0.05→19、0.01→99、0.1→9、0.25→3、0.5→1。

**`visuals` 是倍率不是开关**（用户要求「声音大小和粒子效果都可以自定义大小多少，而不是一刀切开或者关」）。两项独立、范围 `0.0~2.0`。`scaleParticles(int)` **非零倍率保证至少 1 个粒子**；`scaleVolume(float)` **只乘音量不动音调**（0.6/1.4 的一低一高是设计意图，不是可调参数）。

---


## 5. 血泪教训

> **编号刻意不重排**（含空的 8 号），多处交叉引用。加新教训往后接。**这里只留结论。**

**A · 证据与验证**

1. **路径必须用真实 vanilla jar 验证**，别只信官方参考仓库（fabric-docs 里有无 `block/` 的旧格式遗留文件会误导人）。26.2 无 remapped jar，查 MCP 缓存的原版 jar：
```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z=[System.IO.Compression.ZipFile]::OpenRead('D:\MCDxAIminecraft-dev-mcp\cache\jars\minecraft_client.26.2.jar')
$z.Entries | Select-Object -ExpandProperty FullName | Where-Object { $_ -match 'tags/block/.*dirt' }; $z.Dispose()
```
**26.2 数据目录仍是单数**（`advancement/` `loot_table/` `tags/block/`，已实测）。

2. **不要凭记忆断言 vanilla 行为**（例：`HoeItem.TILLABLES` 是硬编码 Map、不查 tag）。**能验证就验证，不能验证就说不知道。**

10. **别凭记忆写版本号/API 名**（GitHub Actions 与 MC 的颜色/工具类 API 变动都很频繁）。写之前用 MCP 或查 `releases/latest` 核实。

13. 🔴 **不要凭公式自证，跑一遍**。做法（成本极低，用户很认这种证据）：临时往 `build.gradle` 加任务取 classpath → `javac -nowarn -proc:none` 编译测试类 → 跑 → 删干净。
```powershell
tasks.register('afPrintCp') { doLast { println sourceSets.main.runtimeClasspath.asPath } }
```
碰 `MobEffect`/注册表要先 `SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();`（bootstrap 会**冻结注册表**，之后碰不了 `AbyssFallEffects`）。⚠️ **用完必须清干净，包括 `build.gradle` 里的临时任务**。**每轮结束前 `git status --short` 逐行看。**

15. **报告技术约束时先核实，别顺着需求答应**（也别没核实就下反向断言）。

18. **工具报「找不到」时先判断是能力边界还是真不存在**：`analyze_mixin` **不认 Fabric API 的 `impl` 类**，那个 Mixin 只能靠 `javap` 读字节码 + 编译通过来证明。

20. 🔴 **「我的函数返回了预期值」≠「这个值有实际效果」**。`blitSprite` 的 tint 是**乘算**（只能变暗），代码提亮做不出闪光，必须**另做一张亮贴图**。涉及渲染管线（乘算/加算、tint 能做什么、alpha 怎么合成）必须去读目标 API 的实际实现，光测自己的输入输出等于没测。

21. **选证据要挑能让结论唯一的样本**：判断半格贴图裁切方向要看**心**（对称）而不是鸡腿（倾斜不对称，会读反）。

25. **`Util.getMillis()` 不是墙钟**（基于 `nanoTime()`），不能和 `System.currentTimeMillis()` 相减。**写验证代码时时钟来源也要对齐。**

26. 🔴 **`gradlew build` 成功不代表游戏能启动**：资源文件原样拷贝、**Gradle 不校验 JSON 语法**，一个多余字符能让 build 通过而 Loader 崩在 bootstrap。**改完任何 json/mixins 配置后验首字节**（应是 `7B` 即 `{`，非 `EF BB BF`）：
```powershell
$b=[System.IO.File]::ReadAllBytes($f); ($b[0..2] | ForEach-Object{ $_.ToString('X2') }) -join ' '
```
⚠️ `blockstates/*.json` 用 `ConvertFrom-Json` 会**误报**（空字符串作属性名是合法 blockstate 写法），别据此改文件；**含中文的 `.ps1` 必须存成带 BOM 的 UTF-8**。

**B · 与用户协作**

6. **不要过度设计。** 先想最简方案。

7. **先问清设计理念再写抽象**。**他说「引入一个概念」时，先确认它是离散的还是连续的。**

12. **别把文档里的「原则」当成「待办」**。看到文档与代码「像是」冲突时，先读代码注释里的理由——多数时候上一个你已经解释过了。

17. **枚举的「等级」和「名字」不是一回事**。有歧义时**报给他让他选，而不是自己挑一个**。

22. **别在他没说的地方替他做决定，但要把冲突说出来**。若自己加了他没要求的东西，**必须明确标注是你加的并给出关闭方式**。

23. 🔴 **涉及玩法数值语义的歧义必须事前问**（他明确训过：「我是设计师，你是执行层」、「下不为例」）。**判断标准：如果两种做法会让玩家体验到不同的数值，就问。**

24. **别把外部顾虑（法律/洁癖/最佳实践）带进开发阶段的占位工作**（原话「你他妈有病吧」）。**占位图怎么省心怎么来，有这类顾虑先问他。**

**C · 工具与环境**

3. **advancement 背景只有 5 个**：`husbandry`/`end`/`nether`/`stone`/`adventure`。

4. **`ServerPlayer` 没有 `getServer()`**，用 `player.level().getServer()`。

5. **PowerShell 显示中文乱码是编码显示问题**（lang 文件是无 BOM UTF-8，控制台按 GBK 解），不代表文件坏了：
```powershell
[System.Text.Encoding]::UTF8.GetString([System.IO.File]::ReadAllBytes('src/main/resources/assets/abyssfall/lang/zh_cn.json'))
```

9. **工具硬限制**：`editor` 单次 **6000 字符**、超了静默截断；`editor` 不能无 `old_text` 覆盖已存在文件（整体重写先 `Remove-Item`）；`run_commands` **30 秒**上限（`Start-Sleep 45` 直接失败，长构建会被截断，重跑看 `UP-TO-DATE` 判断上次是否成功）；`Select-String` **没有** `-Recurse`；PowerShell **不支持 heredoc**（多行 commit message 先写临时文件再 `git commit -F`）；`read_files` 读长文件会截断、其分页可能持续返回 `[outdated]` ⇒ **改用 PowerShell 按行区间 dump**，别反复重试。

11. **删 tag 重建时先确认远端删成功了再建本地。**

14. **文件名里不能有 `:`**（Windows）。**任何要写进文件名的用户输入格式都先验一下。**

16. **坐标 API 别按名字猜语义**：`getHeight(id)` 返回**顶边 Y**且**求和不含自身**，正确写法 `guiHeight - getHeight(id)`、零额外偏移。**先读官方 javadoc 的用法示例，别先改数字。**

19. **改配置键名会静默破坏用户的本机配置** → 必须主动告知他去改。详见 4.2。

27. **改长文档时别用大块 `editor` 覆写**：`Copy-Item` 备份 → 分段写（每段 ≤6000 字符，`insert_line` 追加）→ 每段后核行数 → 收尾核字节与首字节。

---


## 6. 26.2 迁移（已完成，用户复测通过）

**构建层五处形态变化**（不是版本号变化）：插件 id `fabric-loom-remap` → **`fabric-loom`**；`loom.officialMojangMappings()` → **整行删除**；`modImplementation` → **`implementation`**；发布任务依赖 `remapJar`/`remapSourcesJar` → **`jar`/`sourcesJar`**（前两者已不存在）；Java 21 → **25**。副作用是好的：**「`jar` 是 dev-mapped、跑不了」这个老坑随之消失**。

**代码层**：68 个 `net.minecraft.*` import 只有 3 个要改：`advancements.criterion.EntityPredicate` → `advancements.predicates.entity.EntityPredicate`；`advancements.criterion.MobEffectsPredicate` → `advancements.predicates.MobEffectsPredicate`；`client.gui.GuiGraphics` → `client.gui.GuiGraphicsExtractor`。

**三处 API 改名（逻辑一行未动）**：
1. **HUD 元素接口** → **`extractRenderState(GuiGraphicsExtractor, DeltaTracker)`**。`blitSprite`/`fill`/`pose`/`guiWidth`/`guiHeight` 全同签名；`drawString(...)` → **`text(...)`**（参数序相同、`dropShadow` 默认 `true`，**文字观感无变化**）。`guiWidth()/2+91` 与 `foodLevel*3+1` 在 26.2 逐字未变 ⇒ **动效常量与判定全部原样保留**。
2. **创造标签** → **`fabric-creative-tab-api-v1`**；`FabricCreativeModeTab.builder()`；`CreativeModeTabEvents.modifyOutputEvent`。`FabricCreativeModeTabOutput` 实现 `CreativeModeTab.Output` ⇒ **`accept(...)` 一字未改**。
3. **覆盖层消息** `displayClientMessage(c, true)` 已**移除** → **`sendOverlayMessage(c)`**，落点完全相同（无条件 `overlayMessageTime = 60`）⇒ **「3 秒 + 淡出 + 重按续期全由 vanilla 提供、项目不写计时器」依然成立**。

**Mixin 从 2 减到 1**：26.2 的 `WitherRoseBlock.mayPlaceOn` 变成 `state.is(BlockTags.SUPPORTS_WITHER_ROSE)`，改用数据文件（`REFERENCE.md` 4）。**其他**：`Gui` → `Hud`（`minecraft.gui.hud`）；权限系统内部重构（见 3.5）；数据目录路径**未变**。

---

## 7. 当前状态

- **编译已验证**：`build` 与 `releaseJars` 都 `BUILD SUCCESSFUL`。产物 `build/release/{abyssfall,abyssfall-doc,abyssfall-source}.jar`
- **Git 状态 / tag / CI 结果一律现场核实。** tag 到 `v1.1-Dev`（26.2 首版；`0.1-Dev`~`v0.5-Dev` 属 1.21.11 时期）

### 7.1 已实测通过（用户在真实环境验证，**别再列成待确认项去催他测**）

1.21.11 时期全部通过，26.2 迁移后他复测确认「全部事项表面看起来没任何问题」：

- **San**：`/san` 8 条、持久化、死亡保留、3 级权限、客户端同步
- **内容**：双色/三色标题、tooltip 不变蓝、EPIC 紫、宝箱掉落与概率、药水效果必定掉落、村民箱子不掉落、玫瑰可种深渊污泥而其他作物不可、骨粉催熟出花且玫瑰被消耗、灵魂特效、三个成就
- **配置**：生成/读取/各项生效、开发者内容条件注册（`false` 时标签与物品都不存在）、计数器 3 秒淡出与重按续期、坏 JSON 备份+重写
- **HUD 两种读数**：满值不显示且不占空间、紧贴饱食度且饱食度不移位、约 1 秒淡出且结束时上方不跳、氧气条/物品名不被压、F1 一同隐藏；图标行半格映射/抖动/下降抖一下/上升行波/回满闪光；进度条颜色长度随 San 变化、RGB 高亮、整条抖动抬升；窥镜双向切换与 500ms reveal
- **药水**：两效果五级数值、同级并存净变化为 0、**和平模式拦精神崩溃但不拦精神饱满**、`/san add` 在和平仍生效
- **测试协议**：真实玩家环境（非 runClient）测过，Swing 弹窗在 `preLaunch` 确实能显示

### 7.2 唯一仍未验证的观感项

**连续小额恢复会不会一直闪、显得吵。** 每次数值变动都重启慢闪，若 San 每 tick 涨一点会一直停在亮相。真出现就加最小间隔；四个常量在一起（`FULL_FLASH_BLINK_TICKS`/`FULL_FLASH_BLINKS`/`GAIN_FLASH_BLINK_TICKS`/`GAIN_FLASH_BLINKS`），两个 HUD 元素各一套。

### 7.3 已用真实 classpath 实测过约 210 项

覆盖：配置往返与残缺回落、战利品权重换算与两个边界特判、测试协议密钥与无头降级、DEV 图标逐像素、jar 解包、HUD 全部动效时序、两个药水效果数值表与镜像对称、窥镜切换与语言键完整性、进度条高亮 RGB、reveal 时间轴、**HUD 层序归位的 9 种注册顺序场景**（`REFERENCE.md` 15a）。**注意教训 20：这类验证证明不了渲染效果。**

---

## 8. 未完成 / 下一步

**San（主线）**
- 事件仍无监听者。框架就绪，等玩法来用
- 🔴 **什么情况下侵蚀 San —— 最大的空白。** 药水效果已能扣 San，但没有任何东西会给玩家上那个 debuff。黑暗、深渊、目击恐怖等真正的侵蚀来源全未设计
- 差异化渲染（San 低看到不同渲染）—— 用户明确说以后开发
- 显示道具已实现（认知窥镜），四个待定点已定：双向切换、手持右键、无耐久时效、与理智计数器并存

**内容**
- **所有图标都是占位**，发布前统一换自己的美术：深渊污泥用原版泥土材质、`abyss_gardeners` 图标是向日葵、计数器与窥镜都用原版 `clock_00`（**指针不会转**，原版靠 `range_dispatch` 切 64 个模型才转）、两个精神效果是脚本生成的图
- 深渊之花无实际功能；三个药水效果**无获取途径**（「深渊探索者」只被战利品侧读取，另两个只能 `/effect`）
- **反精神崩溃魔咒：用户已给完整数值，明确说「先记录，不要实现」**（数值见 `REFERENCE.md` 7b 末尾）
- 无配方、无 datagen、无自定义音效资源
- `abyss_dev_icon` **刻意不命名**（保持键值显示），别「补全」它的 lang key

**配置**
- 架构已就绪，用户预期「以后可自定义配置会特别多」
- **不做热加载**是明确要求
- **被否决/搁置的别再提**：凋零玫瑰能种深渊污泥（核心机制，不进配置）、骨粉催熟机制开关（只有特效可调）、`isBuiltin()` 保留拦截
- **San 阈值不该进配置**（我建议、他未表态）：上限是存档数据，改配置会让新老玩家规则不一致、还可能静默 clamp 玩家数据。真要做先问
- `ALL_LOADED` 是否每次 `/reload` 都触发，**仍未实测**

---


## 9. 开工前请做

0. 通读本文件，再按需查 `REFERENCE.md`（动哪个功能读哪节，别通读）
1. 读 `gradle.properties`、`fabric.mod.json`、`AbyssFall.java` 确认状态与本文档一致
2. 读 `core/` 六个 + `config/` 七个文件（两块地基）
3. 现场核实 Git：`git --no-pager log --oneline -5; git status --short; git --no-pager tag`

**MCP 用法（都实测过）**：
- **`Fabric-Knowledge`**：`get_fabric_context(minecraft_version="26.2")` → `status = exact`。⚠️ 传 `fabric_api_version="0.158.0+26.2"` 会得到 `version_match_only`（上游把 `reference/latest` 钉在 `0.155.2+26.2`，项目用 0.158.0）。**已实测这差异不影响开发**（两版相关子模块 12 个类签名全同、类集合零增删）。**别去手改 MCP 索引里的版本号**，那会伪造 provenance。⚠️ `reference/latest` 是滚动别名（26.3 快照已存在），**每次都要用显式版本号复核**。
- **`minecraft-dev`**（`mapping:"mojmap"`, `version:"26.2"`）核实所有签名，**不要凭记忆**。已反编译并建好索引（7055 文件），`search_indexed` 很快。⚠️ **26.2 不再混淆 ⇒ `find_mapping` 会直接报错，这是正常的**，改用 `get_minecraft_source`/`search_minecraft_code`。缓存在 `D:\MCDxAIminecraft-dev-mcp\cache\decompiled\26.2\mojmap`、jar 在 `...\cache\jars\minecraft_client.26.2.jar`，**离线直接读文件比调 MCP 更快**。
- Mixin 用 `analyze_mixin` 校验，但注意教训 18。

**查 Fabric API 实际行为：直接读 Gradle 缓存的 jar**（没有 sources jar 时 `javap` 是唯一可靠办法）：
```powershell
$p="$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\net.fabricmc.fabric-api"
Get-ChildItem $p -Directory | ForEach-Object { $_.Name }
& 'C:\Program Files\Java\jdk-25.0.2\bin\javap.exe' -p -cp <jar> 'net.fabricmc.fabric.impl.client.rendering.hud.HudStatusBarHeightRegistryImpl'
```
⚠️ **缓存里同时躺着 1.21.11 时期的旧版本**（如 `fabric-rendering-v1 16.2.10`），别读错。Fabric Loader 的 sources jar（查 `preLaunch` 时机、`Knot.init()`、`EnvType`）在 `...\net.fabricmc\fabric-loader`，**本机有多个版本，项目用 0.19.3**。

**项目实际用到的六个子模块**（已验证，`gradlew dependencies --configuration compileClasspath`）：

| 子模块 | 版本 | 用途 |
|---|---|---|
| `fabric-data-attachment-api-v1` | `2.2.18+515ac5339e` | San attachment |
| `fabric-entity-events-v1` | `5.0.5+06488ac19e` | `ServerPlayerEvents.JOIN` |
| `fabric-events-interaction-v0` | `5.2.7+515ac5339e` | `ItemEvents.USE_ON` 骨粉催熟 |
| `fabric-loot-api-v3` | `3.0.17+06488ac19e` | `LootTableEvents.MODIFY` |
| `fabric-rendering-v1` | `25.3.2+515ac5339e` | HUD 元素/高度注册 + **那个 Mixin 的注入目标** |
| `fabric-creative-tab-api-v1` | `5.0.14+d871b99e9e` | 两个创造标签 |

⚠️ **`fabric-rendering-v1` 版本尤其重要**：Mixin 注入的是它的 `impl` 包。升级 Fabric API 时必须重新核实（`REFERENCE.md` 15a 那三点）。

---

## 10. 给下一个你的话

信任度已经很高。维持它的关键不是「多做」：

- **不撒谎**。没验证就说没验证。
- **说清「为什么」**（针对你**新做**的决策）。
- **该问就问，别为小事问**：视觉细节/翻译/注释直接改；**玩法语义、数值含义、数据结构走向必须先问**（教训 23、24）。
- **保持简洁**，他明确说过省 token。
- 🔴 **不要动那些看起来「不够优雅」的代码。** 原话：「项目中所有看起来'不够优雅'的代码全部都是你或者你曾经经过验证后不得不这么做的妥协之法，不要瞎鸡巴乱改。」

  **每一处都有写在注释里的理由，先读理由。** 已知的一批（本文档与 `REFERENCE.md` 里都标了「勿改」）：
  - `ServerPlayerEvents.JOIN` 钩子、`set()` 里的回读（3.3）
  - 双色标题的空根组件（`REFERENCE.md` 1）
  - 配置读写用两个不同 Codec（4.2）、代码授予成就（`REFERENCE.md` 9）
  - `p=1.0` / `p=0.0` 的特判（4.5）
  - 图标 HUD 用**两套贴图**做高亮，进度条却用 RGB 插值——**这两处「不一致」是对的，别统一**（`REFERENCE.md` 15c-2）
  - `erode()` 里 `!(amount > 0.0F)`（4.5）
  - 窥镜判 `isClientSide()` 而非 `instanceof ServerPlayer`（`REFERENCE.md` 13b）
  - 只注册**一个** HUD 分发元素（`REFERENCE.md` 15c）
  - reveal 用 `Math.max(lastShownAt, revealEndsAt())`（`REFERENCE.md` 15c）
  - 唯一那个 Mixin 注入 Fabric API 的 `impl` 包（`REFERENCE.md` 15a）

  他要求过「最大程度按 HANDOFF 执行，有矛盾随时通知我」——照做，但矛盾要先自己核实过再报。
- **能跑就跑一遍**（教训 13）。他不要你跑 runClient，但**不禁止你跑纯 Java 验证**，成本极低且他很认这种证据。

祝顺利。
