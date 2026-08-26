# AbyssFall 交接（必读，通读完再动手）

> **本文件属于你**，上一个你留给你的，收尾时更新留给下一个你。与事实不符即主动修正。
>
> | 文件 | 内容 | 读法 |
> |---|---|---|
> | **`HANDOFF.md`** | 约定、三块地基（San/配置/Shader）、**总路线（4c）**、教训、状态、下一步 | **通读** |
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

⇒ 目前**三个** Mixin：`client/mixin/HudStatusBarHeightRegistryImplMixin`（HUD 高度，`REFERENCE.md` 15a）+ `mixin/PlayerAttackMixin`（毕业武器接管，`REFERENCE.md` 17）+ `client/mixin/RenderTypeInvoker`（Shader 系统造 RenderType，`REFERENCE.md` 18b）。**都不要当技术债清理、不要试图用 API 重写**——前两个的理由见各自那节，第三个是 `RenderType.create` 为 package-private 且 Fabric API 未提供替代（已逐个核实 `api` 包）。写新功能时优先找 API 事件，找不到再注入并说明理由。

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
| 版本 / 许可 | `1.5-Dev` / GPL-3.0-or-later（**每个 .java 带 GPL 头，新文件照抄**） |
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

## 4b. 地基三：Shader 渲染系统（1.4-Dev 新增）

用户定位：**「SanCore 负责的是游戏的规则框架，Shader 负责的是游戏物品的渲染框架」**、**「这个渲染器只给一个物品用就太浪费了」**、**「未来我想随着 san 值变化，实时在游戏内给各种物品贴上"异常"渲染」**。

### 4b.1 🔴 它不是死兆将至的专属系统

`shadercore` 包里**没有任何一处提到那把剑**。剑只是 `AbyssFallShader.json` 默认文件里的一条 entry，是**消费者之一**。

**禁忌**：不要往 `shadercore` 加任何物品专属逻辑。要让某物品有渲染 → 加 provider 或加 effect 类型，两者都不用碰系统类。

### 4b.2 两条正交扩展轴（分开是刻意的）

| 轴 | 入口 | 加东西要做什么 |
|---|---|---|
| **效果种类**（长什么样） | `ShaderEffectTypes.register()` | 写一个 record（`implements ShaderEffect`）+ 一个 GLSL，注册。**不碰现有类** |
| **决策来源**（何时、给谁） | `AbyssFallShaderCore.addProvider()` | 写一个 provider，可返回任意已注册种类 |

⇒ **「San 低了给物品贴异常渲染」是加一个 provider，不是改架构。** provider 决定「何时/多强」，复用已有种类决定「长什么样」。

**已实测**：从系统外部定义并注册一个全新种类（自带字段与 shader），零系统改动即生效。

### 4b.3 每帧决策，不是启动时固化

`ItemModel#update` 每帧都跑 ⇒ **每帧问一次 `AbyssFallShaderCore.effectFor()`**，返回值可以每帧不同。这是「实时贴异常渲染」能成立的根本。

**因此渲染层包装了所有物品的模型**，不是只包装配置里那几个——否则未在配置中的物品没有 wrapper，provider 声明了也画不出来。不命中时不加图层、渲染结果与原版一致。无 provider 时完全不安装。**别"优化"成按配置预筛，那会砍掉动态性。**

**Provider 优先级 = 注册顺序倒序**（后注册的先问，第一个非 null 胜出）。配置 provider 最先注册 ⇒ 优先级最低，任何反应式 provider 都能覆盖它。

### 4b.4 🔴 颜色来源是一条**刻意留空**的接缝

用户原话：「颜色来源、计算方式以及后续 Provider 怎么决定效果，等整体渲染架构稳定后再单独设计」、「让 Shader System 不绑定任何一种颜色来源，避免以后选择方案时需要重做底层渲染系统」。

⇒ `ShaderColorSource` 接口 + **唯一的占位实现** `FixedColorSource`。

**`FixedColorSource` 是占位，不是决定。** 它的三条限制写在自己的 javadoc 里，且明确标注「这是占位的限制，不是系统的限制」：编译期常量、整块同色、不读原贴图。

**已知未解决**：绿/蓝两通道共用一个颜色——`opacity` 合并那步就把来源信息丢了。修它必然涉及颜色方案设计，故未修。

**动颜色系统前先读那个接口的 javadoc。** 若将来要「每帧变色」，改的是那**一个接口文件**（从「贡献 define」扩成「也能贡献 uniform」），不是渲染代码——这正是这条接缝的价值。

### 4b.5 参数走编译期 define，不走 uniform

**原因（已验证）**：uniform buffer 必须手动驱动 `RenderPass` 填充，而渲染在 `submitCustomGeometry` 内部，**拿不到 `RenderPass`**。

**代价**：参数不同 ⇒ 不同 pipeline。所以 **provider 不要每帧造新 effect 实例**（连续变化的值会编译上千条 pipeline）。effect 按值缓存 pipeline，配置相同的物品自动共享。

⚠️ `withShaderDefine` 只有 `(String)` / `(String,int)` / `(String,float)` 三个重载，**没有 String 值**。故 `shaderDefines()` 返回 `Map<String, Float>`——顺带避开了 GLSL「整数除法」陷阱。

### 4b.6 独立配置文件 `config/AbyssFallShader.json`

与 `abyssfall.json` **分开**（用户明确要求单独文件）：那个是玩法，这个是外观。容错逻辑刻意与主配置**同构**（同样的三种失败、同样的时间戳备份、同样不抛异常）。

**`"type"` 字段是可扩展的关键** —— dispatch codec 按它选 codec，新种类无需改文件格式。用 `partialDispatch`（不是 `dispatch`，后者签名不收 `DataResult`）。

**必须在 `AbyssFallShaderCore.initialize()` 之后 load** —— 解析 entry 需要类型已注册。

### 4b.7 静默失败（调试时必读）

自定义 pipeline **不在** `RenderPipelines.getStaticPipelines()` 里 ⇒ `ShaderManager.apply` 不预编译、不报错。**GLSL 编译失败会静默**：什么都不画、日志无异常。

⇒ 「遮罩没显示」既可能是美术画错，也可能是 shader 没编译过。**排查时先怀疑后者。**

### 4b.8 已知未解决

- ~~**`Z_PLANE = 8.5/16` 假设平面物品** ⇒ 3D 模型物品（盾牌、方块物品）位置会偏。需从 baked quads 推真实包围盒~~
  ✅ **v1.5-Dev 已解决，并且这条旧表述是错的**：受影响的不是「3D 物品」这个子集，而是**全部物品** —— vanilla 给每个生成型物品都造了 1/16 厚度 + 逐像素侧壁，**没有一个物品是平的**。现在几何跟随物品真实外壳（`ShaderGeometrySource` / `ItemHullGeometry`，见 `REFERENCE.md` 18h），`Z_PLANE` 已删除
- **bind group / 顶点格式固定**：所有效果 shader 必须 import 同一套 uniform。⚠️ **v1.5-Dev 起 `Sampler0` 是物品图集、`Sampler1` 是遮罩**（原先两个都绑遮罩且 `Sampler0` 从不读），新种类若需第三张贴图，得给 `AbyssFallPipelines` 加选项
- 绿/蓝共色（见 4b.4）
- **`glowing` 推导对近黑物品等于不发光**（公式与底层亮度成正比，实测增量仅 +3.5/255，见 `REFERENCE.md` 18g）
- **`masked_pulse` 之外仍无第二个效果种类**（星空未做）

---

## 4c. 🔴 项目大方向：各 core 分工 + 一个 game core 总闸

用户给的架构图（1.4-Dev 收尾时确认）：**`san core` / `shader core` / `other` / `other` 四个箭头全部指向中心的 `game core`**。

用户原话：**「给各个 system core 写出来，最后再去写 game core，这样项目会非常好写，而且屎山代码基本没有，未来会很好维护，哪里坏了修哪里，而不用为了一个系统重构整个项目」**。

### 4c.1 箭头方向是解耦的关键，别画反

箭头指向 game core = **「core 被使用」，不是「core 去使用」**。core 永远不知道自己被谁用、为什么被用。

已验证（依赖图静态分析 + 无环检测）：

| 检查 | 结果 |
|---|---|
| 图是否有环 | 无环 |
| `SanCore` 认识 `ShaderCore` | **false** |
| `ShaderCore` 认识 `SanCore` | **false** |
| 任何 core 认识 `GameCore` | **false** |
| `GameCore` 能触达全部 core | true |
| 加第三个 core 要改现有 core | **0 处**，只在 GameCore 加一条边 |

⇒ **禁忌：任何 core 里不得出现另一个 core 的 import。** 现在两套 core 的 `com.abyssfall.*` import 只有 `AbyssFall`（LOGGER/MOD_ID）和 `config`，已逐个核实。

### 4c.2 写作顺序：先 core，最后 game core（用户明确要求）

**先写 core 的好处不是习惯问题**：总闸不存在时，core 压根没法偷偷依赖别人。等总闸最后写，它拿到的是一堆已经证明能独立存在的积木。

反过来先写 game core，core 会长出「为了配合总闸」的接口 —— 那是屎山的起点。

### 4c.3 ⚠️ game core 必须是「main 本体 + client 薄臂」两个文件

**不是设计选择，是源集分裂的硬约束**（已验证）：

- `src/client` 能看见 `src/main`，**反过来不行**
- `AbyssFallCoreSystem`、`AbyssFallShaderCore.addProvider()` 都在 `src/main`
- 但「读当前客户端玩家的 San」只能在 `src/client` 做

⇒ 住 main 的 GameCore 够不到客户端玩家；住 client 的驱动不了服务端规则。

**项目里已有现成模式照抄**：`SanHudModeState`（main，被 common code 的 `SanLensItem` 调）+ 三个 HUD 元素（client，读它）。**game core 照这个形状做。**

### 4c.4 换算规则住在 game core，不住任何 core

```
SanCore（只管数值，不知道用途）
    ↓ 被读
GameCore（唯一知道 san% → 渲染强度 怎么换算的地方）
    ↓ 驱动
ShaderCore（只管渲染，不知道数值从哪来）
```

**「san% = shader%」这个公式属于玩法，两个 core 都不该知道它存在。**

### 4c.5 现状

- `SanCore` ✅ 已完成，零跨 core 依赖
- `ShaderCore` ✅ 已完成，零跨 core 依赖（1.5-Dev 升级后复测仍为零，见 `REFERENCE.md` 18d-4）
- `GameCore` ⬜ **未开始**，等其他 core 齐了再写

**现在没有总闸**，消费方（HUD、药水效果、物品）各自直连 SanCore ⇒ 「San 变化 → 渲染变化」这条线没人负责，这就是 §8 记的最大空白。**引入 game core 不是重构，是把散落的连线收进一处，现有 core 一行不用改。**

---

## 4d. 🔴 连续 San 驱动渲染：走顶点颜色，不走 define

**这条推翻了 4b.5 的隐含结论。** 4b.5 说「参数走编译期 define」——那对**配置态**参数是对的，但对**每帧变化**的值是灾难。

### 4d.1 实测：San 直接喂 define 会编译上千条 pipeline

模拟真实侵蚀（0.1/tick，满值 100）：

| 方案 | pipeline 数 |
|---|---|
| San ratio 直接进 define | **1001 条** |
| 上限取非整数（137） | **2000 条** |

每条都是真实 GLSL 编译。**这条路不能走。**

### 4d.2 顶点颜色通道不受这条限制（已验证）

已核实的事实：

- `DefaultVertexFormat.ENTITY` 的 `Color` 是 **`GpuFormat.RGBA8_UNORM`**（26.2 源码实证）
- `ShaderLayerRenderer` 现在写 `setColor(255,255,255,255)` —— **常量，通道完全空着**
- `masked_pulse.vsh` 声明了 `in vec4 Color` 但**从不转发**，`.fsh` 也不读

**关键：pipeline 缓存 key 是 `ShaderEffect` record，而顶点数据是每次 draw call 写的，不在 record 里。**

⇒ **provider 永远返回同一个 effect（1 条 pipeline），San 每帧从顶点颜色进去。**

实测 6000 帧 San 连续正弦变化：**pipeline = 1**（不是 6000），可分辨 256 档。

### 4d.3 精度足够

| 通道数 | 档数 | San 粒度 |
|---|---|---|
| 1 个 | 256 | **0.3922%** |
| 2 个打包 | 65536 | **0.001526%** |

单通道最坏往返误差 **0.196% San**，已比图标 HUD 刻意的 5% 细十倍。

### 4d.4 这不是「写死的档位」

实测验证（用户特别在意这点）：

```
79% -> density 0.20999998
80% -> density 0.19999999
81% -> density 0.19
all different? true
-> 无平台期、无区间、每一步都在动
```

**是量化的连续函数，不是分段常量。** 与「10%=90% 写死」在结构上不是一回事。

### 4d.5 要动的地方（真正的架构变动，动前先报告）

| 改动 | 规模 |
|---|---|
| `ShaderLayerRenderer` 写真实颜色而非常量 | 小，但 `NoDataSpecialModelRenderer` 现在拿不到 San，需要一条通路 |
| `.vsh` 转发 `Color`、`.fsh` 读它 | 小，每个想响应的效果种类各改自己的 shader |
| **`ShaderEffect` 要区分「编译期参数」与「每帧参数」** | **中，接口变动** |

⚠️ 第三条正是 `ShaderColorSource` javadoc 预留的那条接缝，但**实测证明不该走 uniform 而该走顶点属性** —— uniform 拿不到 `RenderPass`（4b.5），顶点属性没这个问题。

**未验证**：以上全是算法与 API 层面的验证，**没有进游戏**。顶点颜色能否真把值送到 fragment stage、`RGBA8_UNORM` 归一化在 Vulkan 后端是否一致，都要实跑。

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
29. 🔴 **改了值不等于屏幕会变——渲染路径可能有缓存。** `MutableComponent` 把渲染结果存在 `visualOrderText`，**只在语言变化时才重算** ⇒ 事后 `setStyle` 永远不上屏（tooltip 动态颜色第一版就这么静止了一整轮）。**凡是"改完看不出效果"，先查目标类有没有缓存字段，别怀疑自己的算法。** 这是教训 20 的同族：函数返回了预期值 ≠ 那个值有实际效果。

30. 🔴 **改共享对象会伤到别人：MC 的 `Component.copy()` 是浅拷贝。** 它只 `new ArrayList<>(getSiblings())`，**sibling 本身是共享引用**。创造界面给每个物品都加一行 `tab.getDisplayName().copy()`，而本项目标签名由两个共享 Component 拼成 ⇒ 递归 `setStyle` 穿过 copy 污染原始实例，**游戏内所有物品的那一行一起闪**。**处理 `Component` 树一律"重建"不"就地改"**（`REFERENCE.md` 17g）。改任何拿得到的对象前先问：这个实例还有谁在用？

31. **`javap` 查 API 比读反编译源码更可靠地能回答"这个成员是 public 吗"。** 本轮两次踩到可见性：`Level.random` 是 protected（要用 `getRandom()`）、`ChatFormatting` 没有 `getColor()`（要 `TextColor.fromLegacyFormat`）。反编译源码不显式标注继承来的可见性，`javap -p` 一目了然。

32. 🔴 **写 API 名之前用 `javap` 列一遍重载，别只确认「这个方法存在」。** 本轮两次栽在重载集合上：`RenderPipeline.Builder.withShaderDefine` 只有 `(String)` / `(String,int)` / `(String,float)`，**没有 String 值版**，我按 `Map<String,String>` 设计了整个接口才发现（已改 `Map<String,Float>`）；`Codec.dispatch` 的两个重载都不收 `DataResult`，要用 `partialDispatch`。**「方法名对」不等于「签名对」。**

33. **跨版本抄代码时，类名/字段名是最先腐烂的部分。** 从 1.21.1 的无尽贪婪移植时抄错三处：`DefaultVertexFormat.NEW_ENTITY`（26.2 只有 `BLOCK`/`ENTITY`）、`LightTexture.FULL_BRIGHT`（26.2 是 `net.minecraft.util.LightCoordsUtil.FULL_BRIGHT`）、`ShaderInstance`+`AbstractUniform`（26.2 整套没了，改 `RenderPipeline`+UBO）。**参考实现给的是思路，名字一律现查。**

34. **物品模型空间是 `0..1`，不是 `-0.5..0.5`。** `ItemModelGenerator` 在 0..16 网格里造，`FaceBakery:145` 除以 16 ⇒ 成品占 0..1、中心在 `(0.5,0.5)`；平面物品 z 在 `7.5/16 ~ 8.5/16`。**反直觉之处**：`ItemTransform.apply` 每条路径**最后**都有 `translate(-0.5,-0.5,-0.5)`，那句才是居中——所以它作用于仍是 0..1 的几何，按原点画的东西会被一起平移半格。另：`ItemStackRenderState.submit` 是**逐图层**套变换的，新建图层默认 `NO_TRANSFORM`，不手动 `setItemTransform` 就不会跟着物品转。

35. 🔴 **「参数走编译期常量」这个决定有作用域，别当全局结论用。** 4b.5 定下「shader 参数走 define」，对**配置态**参数完全正确；但把它套到**每帧变化**的值上，实测会编译 1001~2000 条 pipeline。**同一个技术决定在不同频率的数据上是相反的答案**——判断「该走哪条路」时，先问这个值多久变一次。正解不在 uniform（拿不到 `RenderPass`）而在顶点属性（每次 draw call 写，不进 pipeline 缓存的 key），见 4d。

36. **找「值往哪儿塞」时，先把顶点格式的每个属性都数一遍有没有被用。** `DefaultVertexFormat.ENTITY` 的 `Color` 是 `RGBA8_UNORM`、四个 8 bit 通道，而项目的 shader 层一直写常量 `setColor(255,255,255,255)`、vsh 声明了 `in vec4 Color` 却从不转发 ⇒ **整条通道白白空着**。它不进 pipeline 缓存的 key，所以是「每帧变化的值」的天然载体。**类比**：教训 32 说「方法名对≠签名对」，这条是「格式声明了≠有人在用」。**v1.5-Dev 又用了一次**：遮罩 UV 挤进了同样闲置的 `UV1`（`REFERENCE.md` 18h-2）。

37. 🔴 **先问「要画的东西存在吗」，再问「怎么画」。** 本轮最大的浪费：用户报「物品被 shader 渲染后没有中间厚度」，我从「shader 怎么画」往下查了**五轮** —— 改深度状态、改几何、加四种颜色推导、重画遮罩 —— 每一步都解决了真实存在的问题，但都不是他问的那个。真正的原因是**贴图有 83 个 alpha 1~254 的半透明像素**，vanilla 用 `alpha == 0` 严格判透明 ⇒ 116 个侧壁里 76 个长在 alpha 均值 6.6/255 的像素上，**那圈厚度本来就不可见**，我一直在给不存在的东西上色。

    **一条命令就能定位**（alpha 分布统计，见 `REFERENCE.md` 18i）。**教训不是「要多验证」，而是验证的顺序**：渲染问题先确认「目标几何/像素是否真的可见」，再往管线下游查。这条是教训 20（函数返回预期值 ≠ 有实际效果）与教训 29（改了值 ≠ 屏幕会变）的上游 —— 那两条问「效果有没有到屏幕」，这条问「对象存不存在」。

38. **归因错了要立刻承认并回滚，不要在错误归因上继续加码。** 本轮我先断言「shader 层写深度压掉了本体细节」并据此改了 `DepthStencilState`（写深度→不写）。用户实测反馈「毫无变化，反而效果变淡了」——**那是归因错误的直接证据**。当时正确的动作是回滚 + 重新查，我做到了；但更早的信号是：我给出那个归因时**没有验证过「本体细节被盖住」这件事本身**，只验证了「写深度会盖住同深度的东西」这条通则。**通则成立 ≠ 它是本例的原因。**



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

28. **PowerShell 读本项目文件必须显式指定 UTF-8**：`Get-Content`（含 `-Raw`）按本机 GBK 解码，含中文的文件会整片乱码——`zh_cn.json` 会假报「JSON 无效」，`HANDOFF.md` 会读出天书。用 `[System.IO.File]::ReadAllText(path, [System.Text.Encoding]::UTF8)` / `ReadAllLines`。**别因为假报错去「修」文件。** 另：`ConvertFrom-Json` **拒绝空字符串键**，而 `blockstates/abyss_dirt.json` 的 `"variants": { "": {...} }` 是 MC 合法写法 ⇒ 它永远会被误报，**不是文件坏了**。

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
- **Git 状态 / tag / CI 结果一律现场核实。** tag 到 `v1.5-Dev`（26.2 时期为 `v1.1-Dev` 起；`0.1-Dev`~`v0.5-Dev` 属 1.21.11 时期）

### 7.1 已实测通过（用户在真实环境验证，**别再列成待确认项去催他测**）

1.21.11 时期全部通过，26.2 迁移后他复测确认「全部事项表面看起来没任何问题」：

- **San**：`/san` 8 条、持久化、死亡保留、3 级权限、客户端同步
- **内容**：双色/三色标题、tooltip 不变蓝、EPIC 紫、宝箱掉落与概率、药水效果必定掉落、村民箱子不掉落、玫瑰可种深渊污泥而其他作物不可、骨粉催熟出花且玫瑰被消耗、灵魂特效、三个成就
- **配置**：生成/读取/各项生效、开发者内容条件注册（`false` 时标签与物品都不存在）、计数器 3 秒淡出与重按续期、坏 JSON 备份+重写
- **HUD 两种读数**：满值不显示且不占空间、紧贴饱食度且饱食度不移位、约 1 秒淡出且结束时上方不跳、氧气条/物品名不被压、F1 一同隐藏；图标行半格映射/抖动/下降抖一下/上升行波/回满闪光；进度条颜色长度随 San 变化、RGB 高亮、整条抖动抬升；窥镜双向切换与 500ms reveal
- **药水**：两效果五级数值、同级并存净变化为 0、**和平模式拦精神崩溃但不拦精神饱满**、`/san add` 在和平仍生效
- **测试协议**：真实玩家环境（非 runClient）测过，Swing 弹窗在 `preLaunch` 确实能显示

**1.2-Dev 新增内容（本轮，用户已实测确认）**：

- **认知窥镜贴图**：金框立镜 + 会左右扫视的眼睛（16×48 动画条）。**用户明确说「这一版做的我很满意」**——那四条画眼睛的约束别动（`REFERENCE.md` 13d）
- **金镜** `gold_lens`：窥镜的无眼版，纯占位，配方 = 金锭×4 + 黑曜石×4 + 遮光玻璃板×1
- **遮光玻璃板** `tinted_glass_pane`：**已实测「还真 tm 遮光」**，且能与原版各种玻璃板正常拼接，两个创造栏位置都准确（`REFERENCE.md` 13e）
**1.3-Dev 新增内容（毕业武器，见 `REFERENCE.md` 17）**：

- **死兆将至** `final_death_omen`：无视一切减伤的秒杀剑。`@WrapMethod` 包裹 `Player#attack`，不进 `hurtServer` ⇒ 凋灵/末影龙/52 个子类覆写/PvP 开关/创造模式/图腾全部不执行。贴图 `abyssfall:item/final_death_omen`（16×16）
- **伤害类型** `abyssfall:death_omen` + 8 个 `bypasses_*` tag；三条随机死亡消息（`DamageSource` 子类覆写，零额外 Mixin）
- **tooltip 逐字波浪染色**：`+深渊 攻击伤害`，`+` 与属性名用原版蓝、「深渊」/「Abyss」按 code point 拆字做灰黑波浪（3.5 秒周期）
- **恢复了 `src/main` 的 mixin 通道**（`abyssfall.mixins.json`），这是本轮唯一的架构变动，已事先报告并获同意

⚠️ **1.3-Dev 内容已由用户在游戏内实测通过**（本轮确认）。


- **认知窥镜品质改 EPIC**

**1.4-Dev 新增内容（Shader 渲染系统，见 4b 与 `REFERENCE.md` 18）**：

- **地基三 `shadercore`**：物品渲染框架。两条正交扩展轴（效果种类 / 决策来源），每帧决策，不绑定任何颜色来源
- **独立配置** `config/AbyssFallShader.json`（dispatch codec，`"type"` 可扩展）
- **第一个效果种类** `abyssfall:masked_pulse`：遮罩绿=常驻、蓝=随机抽样，共用脉动
- **第三个 Mixin** `RenderTypeInvoker`（`@Invoker` 取 package-private 的 `RenderType.create`）
- **死兆将至贴图定稿**，并成为 Shader 系统的第一个消费者

🟢 **用户已实测通过的部分**（这是难点，别再列成待测）：
- mod 自带 GLSL 能被 26.2 加载并编译（纯红上屏）
- **OpenGL 与 Vulkan 两个后端都正常**
- `GameTime` uniform 通路（红蓝脉动可见）
- 物品栏 / 掉落物 / 手持 / 视角四种场景变换全部正确
- **通用化重构（多物品、可扩展种类、颜色接缝）之后也已实测通过**（本轮确认）

**1.5-Dev 新增内容（Shader 系统升级 + 一次归因失败的教训）**：

本轮起因是用户报「物品被 shader 渲染后变成单片、只渲染一面、中间没厚度」。**最终查明主因是贴图问题不是代码问题**（教训 37），但排查过程中做的几项改动是真正的升级，用户原话：**「感觉也算是shader升级了」**。

- **几何从单平面改为跟随物品真实外壳** —— `ShaderGeometrySource` 接缝 + `ItemHullGeometry` 实现，覆盖前面/后面/逐像素侧壁。`Z_PLANE` 已删除（`REFERENCE.md` 18h）
- **`DerivedColorSource` + 四种推导**（`tinted`/`drained`/`inverted`/`glowing`）—— 颜色可从物品自己的贴图推导，⇒ **以后不必逐物品画遮罩**。用户原话「这个丁很重要」（`REFERENCE.md` 18g）
- **`Sampler0` 改绑物品图集**（原先绑遮罩且从不读），`ShaderVertex` 携带两套 UV
- **颜色 codec 改成 dispatch** —— 兑现了 18d-3 记的那个待办，写路径不再抛 CCE，旧文件仍可读
- **贴图 alpha 二值化** —— `make-death-omen-texture.ps1`，幽灵像素 76→0，侧壁可见率 34.5%→**100%**（`REFERENCE.md` 18i）
- **debug 遮罩改按几何分区** —— `make-death-omen-mask.ps1`，剑刃蓝（抽样）/ 护手与柄绿（常驻）
- 顺带修掉 `TextureAtlas.LOCATION_*` 的 deprecation 警告

⚠️ **1.5-Dev 的渲染观感尚未经用户完整实测**：贴图二值化与 debug 遮罩刚做完就收尾了。**待测项见 7.2。**

### 7.2 仍未验证的项

**观感（1.2-Dev 遗留）**：连续小额恢复会不会一直闪、显得吵。每次数值变动都重启慢闪，若 San 每 tick 涨一点会一直停在亮相。真出现就加最小间隔；四个常量在一起（`FULL_FLASH_BLINK_TICKS`/`FULL_FLASH_BLINKS`/`GAIN_FLASH_BLINK_TICKS`/`GAIN_FLASH_BLINKS`），两个 HUD 元素各一套。

**Shader 性能**：渲染层包装**所有**物品模型（理由见 4b.3）。开满物品的创造栏 / 大量掉落物场景是否掉帧 —— 唯一有性能风险的地方，用户未报告问题但也未专门压测。⚠️ **v1.5-Dev 起单个物品的 quad 数从 1 涨到「2 + 侧壁数」**（死兆将至实测 98 个），压测的必要性上升了。

**🔴 1.5-Dev 渲染观感（本轮遗留，优先级最高）**：
- **厚度是否终于可见** —— 贴图二值化后侧壁 100% 长在可见像素上，但**没有进游戏确认过**
- **debug 遮罩的分区是否清晰**（剑刃闪 / 护手与柄常驻，边界是否一眼可辨）
- **四种推导各自的观感** —— `tinted`/`drained`/`inverted`/`glowing` 全部实现且 codec 验证通过，但**一个都没在游戏里看过**。改 `run/config/AbyssFallShader.json` 的 `derivation` 字段即可切换
- **`glowing` 对近黑物品的缺陷**已算清（+3.5/255，见 `REFERENCE.md` 18g），但修法涉及数值语义，**未动、待用户决定**
- 当前配置刻意是**红蓝 + 高抽样密度**（`sample_density: 0.85`、`sample_period_ticks: 20`）用于 debug，不是最终观感

**顶点颜色通路（4d 提出的方案）**：算法与 API 层面已验证，但**没有进游戏**。待测：顶点颜色能否真把值送到 fragment stage、`RGBA8_UNORM` 归一化在 Vulkan 后端是否与 OpenGL 一致。

**`ALL_LOADED` 是否每次 `/reload` 都触发**，仍未实测。

### 7.3 已用真实 classpath 实测过约 210 项

覆盖：配置往返与残缺回落、战利品权重换算与两个边界特判、测试协议密钥与无头降级、DEV 图标逐像素、jar 解包、HUD 全部动效时序、两个药水效果数值表与镜像对称、窥镜切换与语言键完整性、进度条高亮 RGB、reveal 时间轴、**HUD 层序归位的 9 种注册顺序场景**（`REFERENCE.md` 15a）。**注意教训 20：这类验证证明不了渲染效果。**

---

## 8. 未完成 / 下一步

### 🔴 8.0 总路线（用户确认，见 4c）

**把各 system core 逐个写出来，最后才写 game core。** 现在 `SanCore` 与 `ShaderCore` 已完成且互不相识，缺的是：①更多 core，②总闸。

**写新 core 时守住 4c.1 那条禁忌：core 里不得出现另一个 core 的 import。**

**San（主线）**
- 事件仍无监听者。框架就绪，等玩法来用
- 🔴 **什么情况下侵蚀 San —— 最大的空白。** 药水效果已能扣 San，但没有任何东西会给玩家上那个 debuff。黑暗、深渊、目击恐怖等真正的侵蚀来源全未设计
- 差异化渲染（San 低看到不同渲染）—— **属于 game core 的活**，不是 SanCore 也不是 ShaderCore 的（见 4c.4）
- 显示道具已实现（认知窥镜），四个待定点已定：双向切换、手持右键、无耐久时效、与理智计数器并存

**内容**
- **毕业武器（死兆将至）待定项**：横扫附带目标是否也秒杀未定；`stabAttack` 那条路是剑就不需要覆盖

**Shader 渲染系统（地基三，1.4-Dev 建立，1.5-Dev 升级）**
- ~~🔴 **颜色系统未设计**~~ ✅ **1.5-Dev 兑现了第一步**：`DerivedColorSource` + 四种推导，从物品自己的贴图推色（`REFERENCE.md` 18g）。⚠️ **这不等于「颜色系统设计完了」** —— 用户当初要的「颜色来源、计算方式、Provider 怎么决定效果」中，**「Provider 怎么决定」仍未设计**（那属于 game core）
- ~~🔴 **遮罩定稿后要删掉整套 debug 配色**~~ ⏸ **用户明确改为「暂不删」**：新美术定稿前，红蓝是对比最强的 debug 工具（`REFERENCE.md` 18d-2）。那行危险的 xmap 已在 1.5-Dev 换成 dispatch codec
- **星空效果种类未做** —— 无尽贪婪那套程序化生成（球面射线 + 伪随机撒点）已调研完（`REFERENCE.md` 18f），素材问题因此消失，但没实现。⚠️ 它**不需要遮罩**，会撞上 `ShaderEffect.mask()` 的强制约束（见 `REFERENCE.md` 18d-4）。✅ **但几何那一半的路已经铺好** —— 加一个 `ShaderGeometrySource` 实现即可，不必改渲染器（18h）
- **用户点名的下一个想法：用丁的机制程序化生成一只眼睛贴到物品上。** ⚠️ **它不属于颜色轴而属于效果种类轴**（`ShaderEffectTypes.register()`）——写一个 record + 一个 GLSL（SDF 画圆与竖缝），零系统改动。**已跟用户说明过这个归属**
- **San 联动 provider 未做** —— 这是这套系统存在的理由，但**它属于 game core**（见 4c.4），不该直接塞进 shadercore
- ~~**`Z_PLANE` 假设平面物品** ⇒ 3D 物品位置会偏~~ ✅ **1.5-Dev 已解决**（见 4b.8 与 18h）
- 绿/蓝共用一个颜色（见 4b.4）
- 遮罩红色通道空着，可作第三种行为
- `AbyssFallPipelines.clear()` 无人调用，加资源重载支持时记得接上
- **`glowing` 对近黑物品几乎不发光** —— 修法涉及数值语义，待用户决定（`REFERENCE.md` 18g）

- **少数图标仍是占位**（多数已换成自己的美术）：`abyss_gardeners` 图标是向日葵、计数器与窥镜都用原版 `clock_00`（**指针不会转**，原版靠 `range_dispatch` 切 64 个模型才转）、两个精神效果是脚本生成的图。⚠️ **`final_death_omen_mask.png` 是脚本生成的 debug 遮罩**（1.5-Dev 改为按几何分区，见 `REFERENCE.md` 18i-2），等用户美术。✅ **剑本体贴图已由用户重画**（1.5-Dev，经 alpha 二值化后入库）
- 深渊之花无实际功能；三个药水效果**无获取途径**（「深渊探索者」只被战利品侧读取，另两个只能 `/effect`）
- **反精神崩溃魔咒：用户已给完整数值，明确说「先记录，不要实现」**（数值见 `REFERENCE.md` 7b 末尾）
- 无 datagen、无自定义音效资源
- `abyss_dev_icon` **刻意不命名**（保持键值显示），别「补全」它的 lang key

**配置**
- 架构已就绪，用户预期「以后可自定义配置会特别多」
- **不做热加载**是明确要求
- **被否决/搁置的别再提**：凋零玫瑰能种深渊污泥（核心机制，不进配置）、骨粉催熟机制开关（只有特效可调）、`isBuiltin()` 保留拦截
- **San 阈值不该进配置**（我建议、他未表态）：上限是存档数据，改配置会让新老玩家规则不一致、还可能静默 clamp 玩家数据。真要做先问

---


## 9. 开工前请做

0. 通读本文件，再按需查 `REFERENCE.md`（动哪个功能读哪节，别通读）
1. 读 `gradle.properties`、`fabric.mod.json`、`AbyssFall.java` 确认状态与本文档一致
2. 读 `core/` 六个 + `config/` 七个文件（两块地基）
3. **动渲染就读 `shadercore/` 八个文件**（地基三）。**动颜色必读 `ShaderColorSource` 的 javadoc**——那是刻意留空的接缝，不是没写完
4. **写新 core 或动架构前先读 4c**（总路线：各 core 分工 + 最后写 game core）
5. 现场核实 Git：`git --no-pager log --oneline -5; git status --short; git --no-pager tag`

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

**本机没有 Python**（已实测：`WindowsApps` 下只有 0 字节的 App 执行别名占位符、无 `py` 启动器、注册表无 Python 项、无 pip/conda；PowerShell 只有 5.1，无 `pwsh`）。CI 也只装 JDK 25。⇒ **日常验证一律用 JDK 25 单文件源码启动 + Gradle 缓存里的 jar**，例如用 Gson 2.14.0（MC 自己用的库）验 JSON，能绕开 PS 5.1 `ConvertFrom-Json` 对空字符串键的假报错：

```powershell
& 'C:\Program Files\Java\jdk-25.0.2\bin\java.exe' '-Dfile.encoding=UTF-8' --class-path <gson.jar> Check.java <files...>
```

**用户已授权：遇到下面两类瓶颈时直接提醒他配置 Python 环境（Pillow/numpy 等），不要硬用 PowerShell 扛**：
1. **批量图像处理 / 图集运算** —— `System.Drawing` 逐像素 `SetPixel` 在 9×9、16×16 上很合适，上到几十上百张做像素 diff、对照表、超采样质量比较就慢得不合理
2. **解析超大日志 / 性能采样**

提醒时要连带说明 CI 影响：**CI 无 Python ⇒ 产物必须本机生成并提交**，会多出一条不被 `gradlew build` 校验的路径。

⚠️ **缓存里同时躺着 1.21.11 时期的旧版本**（如 `fabric-rendering-v1 16.2.10`），别读错。Fabric Loader 的 sources jar（查 `preLaunch` 时机、`Knot.init()`、`EnvType`）在 `...\net.fabricmc\fabric-loader`，**本机有多个版本，项目用 0.19.3**。

**项目实际用到的七个子模块**（已验证，`gradlew dependencies --configuration compileClasspath`）：

| 子模块 | 版本 | 用途 |
|---|---|---|
| `fabric-data-attachment-api-v1` | `2.2.18+515ac5339e` | San attachment |
| `fabric-entity-events-v1` | `5.0.5+06488ac19e` | `ServerPlayerEvents.JOIN` |
| `fabric-events-interaction-v0` | `5.2.7+515ac5339e` | `ItemEvents.USE_ON` 骨粉催熟 |
| `fabric-loot-api-v3` | `3.0.17+06488ac19e` | `LootTableEvents.MODIFY` |
| `fabric-rendering-v1` | `25.3.2+515ac5339e` | HUD 元素/高度注册 + **那个 Mixin 的注入目标** |
| `fabric-creative-tab-api-v1` | `5.0.14+d871b99e9e` | 两个创造标签 |
| `fabric-item-api-v1` | `14.5.0+c68f6cbe9e` | `ItemTooltipCallback`（tooltip 逐字波浪，`REFERENCE.md` 17g）。**是 fabric-api 传递依赖，无需在 `build.gradle` 声明** |

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
  - 毕业武器**不调 `original`**、判 `ServerLevel`、秒杀四步顺序、`bypasses_*` 明知冗余仍保留（`REFERENCE.md` 17b–17e）
  - tooltip 染色**重建 Component 而非 `setStyle`**（`REFERENCE.md` 17g，两个已修 bug 的成因都记在那）
  - 那个 `+` 是正确的 ASCII `U+002B`，字形像「十」是 MC 字体所致，别换字符（`REFERENCE.md` 17g）
  - `final_death_omen` 的红蓝配色**是 debug 产物**，~~遮罩定稿时连带 `FixedColorSource` 一起删~~ ⏸ **用户已改主意：暂不删**，新美术定稿前红蓝是最强对照色（`REFERENCE.md` 18d-2）；那行 xmap 的强转已于 1.5-Dev 换成 dispatch codec（18g-3）
  - **贴图 alpha 只能是 0 或 255**（1.5-Dev）——`make-death-omen-texture.ps1` 的二值化不是"洁癖"，是让厚度可见的唯一办法（`REFERENCE.md` 18i）
  - **`ShaderVertex` 携带两套 UV**（遮罩 UV + 图集 UV），`UV1` 装遮罩 UV 的定点数 —— 顶点格式只有一个浮点 UV 槽，这不是冗余设计（18h-2）
  - **`ItemHullGeometry` 沿各自法线外推而非固定轴** —— 固定轴对侧壁全错（18h）

  他要求过「最大程度按 HANDOFF 执行，有矛盾随时通知我」——照做，但矛盾要先自己核实过再报。
- **能跑就跑一遍**（教训 13）。他不要你跑 runClient，但**不禁止你跑纯 Java 验证**，成本极低且他很认这种证据。

祝顺利。
