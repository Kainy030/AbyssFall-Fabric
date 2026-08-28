# Shader Reference — Abyss 深渊渲染特效

> 本文专门记录 `abysseffect`（深渊渲染，死兆将至）这条线。与 `REFERENCE.md` 分开：那是项目整体功能手册，这里是本特效开发中的架构、走过的弯路、调试方法和待办。
> **javadoc 已写清的不重复**——字段语义、define、通道打包、函数职责请看 `AbyssEffect.java`、`abyss.fsh`/`abyss.vsh`、`ViewerState.java`、`ShaderLayerRenderer.java` 的注释。本文只讲「为什么」和「怎么测」。

## 结论先行

深渊的最终模型是 **分层方向场（layered direction field）**，不是 3D 体 march。
**无限远 vs 无限深是两件事**：星空（`cosmic`）用「方向哈希」做无限远，便宜且正确；深渊一度想在这套无限远算法上硬加真实纵深（3D 体 march），结果每片元做世界生成，是「在 MC 里渲染一个 MC」——成本与窗口面积挂钩、还伴随穿帮。最终采用视觉欺骗：深渊的「深」在浓雾黑暗里玩家本就测不准，只需**暗示**。

- 每片元成本**只取决于层数常数**，与「距离」无关；12 层甚至 32/128 层都远比一次 march 便宜。
- 没有逐格积分、没有 DDA、没有 3D 点距离、没有 march 距离边界。
- 内容是**世界固定的方向天空**，转头晕随头部扫过、平移时近层滑过远层不动（层间视差造假），天然无「区块加载」穿帮。

## 走过的弯路（教训，别重走）

1. **移植尸体 → 从零重写**：abyss 一开始是 cosmic 的逐文件副本（16 层旋转球壳 + 手绘 sprite，Avaritia 14 年前算法）。那是无限**远**天空，平移视差为零（球壳靠自转假深度）。已按 HANDOFF 4b.9 的七问从零重写，cosmic 一个字没动。
2. **第一版从零重写用了真 3D march**（DDA 穿过体素格、3×3×3 邻居找结构点、固定步长雾 march）。数学对、能跑出纵深，但：
   - 成本 ∝ march 格数 `N = max_distance / cell_size`（线性，不是指数），却与手持物品在屏幕上的面积挂钩（手持占大块全屏），800fps→100fps。
   - **把渲染距离/可视距离越缩越省不到哪去**：帧时是「基线 + S·N」，N 已经很小之后继续缩球收益趋零，且球小于 ~3 格时深渊塌成「贴在物品上的一层雾壳」，纵深死亡。
   - 「像区块加载」的穿帮本质是**玩家能看到新内容在运动前方从无到有地展开**。加距离、加双球、加统一 edge fade 都只是缓解。
3. **正解是「有限能见度 / 视觉欺骗」**，即方向场。雾本来就是最受好评的观感（「贴脸迷雾像深渊中有迷雾」），它几乎不需要真实世界锚定。
4. **occupancy 方向反了**：shader 判据是 `cellHash < occupancy 才放点`，所以 occupancy 越**小**点越密、`=1.0` 全空。在 march 版它是「格被占比例」语义，搬到方向场时按老语义设 1.0，导致整窗纯黑。
5. **光点半径在错的空间里**：半径若按 grid 单位（`dir*freq` 之后），会被 freq 缩到亚像素看不见。现在半径与雾都按**方向空间（dir）**量，屏幕张角跨层稳定；grid（`dir*freq`）只决定散布密度。
6. **竖直视差符号**：相机位置 Y 取反（`unfoldCamera` 现为方向场 main 里的内联）实现「下降=坠入、上升=远离」，且**只反相机位置、不反射线方向 Y**——两个都反会连屏幕竖直轴一起镜像、效果抵消（曾实测「完全没变化」）。
7. **RecordCodecBuilder.group 上限 16 字段**：超了编译报泛型 T1..T16 消歧失败。雾参数收进嵌套 `Haze` record + 自己的 `MapCodec`（配置里是 `haze:{…}` 子对象）。
8. **GLSL 严格类型**：ivec3 不隐式转 uvec3（曾报 C7011），要显式 `uvec3(c)`。
9. **MapCodec 没有 `optionalFieldOf(name, default)`**：用 `.codec().optionalFieldOf(name, default)` 提升。
10. **26.2 无相机位置 uniform**（核实过 dynamictransforms/chunksection/projection include），位置走顶点通道折叠打包。

## 当前方向场架构（abyss.fsh）

- 射线：片元视空间位置归一化 → pitch/yaw 旋转回世界方向（旋转顺序沿用 cosmic 已实测正确的那套）。
- 相机位置：顶点流折叠 0..1（X/Z 走 UV0 float，Y 走 Color.g/b 两字节 16 位），展开回米、除 cellSize。
- **层循环**（`ABYSS_LAYERS` 层，近→远）：
  - 每层一个方向平面，频率近低远高（`ABYSS_LAYER_FREQ_NEAR→FAR`），近层大块慢、远层细密密——频率梯度假装透视。
  - 视差：`parallax()` 把方向按相机位置偏移 `ABYSS_PARALLAX × (1-depth)^ABYSS_PARALLAX_FALLOFF`，近层偏移大、远层≈0。这是「深」的全部来源。
  - 每层：`shellField()` = 2D value-noise 雾（`hazeField2`，两 octave）+ 散布光点（3×3 邻格哈希、occupancy 阈值、软辉光、极微 stir 微动）。
  - 合成：debug 阶段目前**纯叠加**、不做前景压暗（`behind` 衰减已移除，待视觉调校时决定要不要加回）。
- 光点子点在方向空间定位、stir 用 GameTime（每日归零的周期量，微动不可察觉）。
- mask 红通道 = 窗口透明度；窗口内填近黑虚空 + 叠加层光。`debug_solid:true` 可平铺品红验证几何/混合。

## 调试工作流（本轮验证用）

- **16×16 全红 mask 套到理智计数器**（`abyssfall:san_counter`）：武器观察窗太小、手持又难静止；计数器是开发者物品、物品面大，用 `textures/item/san_counter/san_counter_mask.png`（整面红）全屏开口，最好看。
  - 这是**临时 debug 资源 + `run/config` 条目**，**没**写进 `ShaderConfigData.DEFAULT`（理智计数器是 dev 物品、不应影响普通玩家）。
- 运行配置 `run/config/abyssfall/AbyssFallShader.json`：只写 `mask`+`type`，其余字段全部回落 record 默认，调外观直接改这里、重启生效（shader 配置启动时读一次、**不热加载**）。
- shader 编译错误看日志 `Couldn't compile fragment shader (abyssfall:core/abyss)`；`Sampler0/2 does not use ...` 是预期 WARN（深渊不读物品贴图/光照，三件套 bind 是固定的）。
- UBO `Resizing ... capacity limit reached` 是 vanilla 正常 INFO，无关。
- Java 验证：`gradlew compileJava compileClientJava`（后台跑、分次读日志，见 HANDOFF 的 30s 工具上限）；GLSL 无法本地编译，以游戏日志为准。

## 已实测通过的事实（用户确认）

- 方向场版：FPS 回到甜点区（800+ 量级），测试全部通过。
- 雾的观感在「贴脸迷雾」方向最好。
- 竖直方向：下降=坠入、上升=远离（相机 Y 取反）。
- 用 16×16 mask 观察时能看出方向场是**球**（原本只在剑的小窗口看），属预期，因为本来就是给剑的小窗做的材质。

## 待办 / 下轮入口

- **外观数值回调**：当前是「debug 往明显里填」的值（occupancy 0.3、hazeBrightness 0.55、layers 12、parallax 0.05、freq 6→28、结构半径有 0.02 下限等）。确定方向后往「深渊氛围」回调：雾暗下来、光点锐化、层数/视差平衡。
- **6 个分层常量还没进 codec**：`ABYSS_LAYERS / PARALLAX / PARALLAX_FALLOFF / LAYER_FREQ_NEAR / LAYER_FREQ_FAR / HAZE_DEPTH_SHALLOW` 目前硬编码在 `AbyssEffect.shaderDefines()`。要可调就提为字段（注意 16 字段上限，可并入 `Haze` 或新建 `Layers` 嵌套 record）。
- record 里 `maxDistance / domainEdge / distanceFalloff / haze.distance / haze.scale` 等 march 时代字段：方向场不再用它们（`ABYSS_MAX_CELLS` 仍被相机位置单位引用）。下轮决定是清理还是保留为别的用途。
- `textures/item/san_counter/` 与 run/config 的 san_counter 条目是 debug 产物，验证收尾时决定去留。
- 若哪天要「真·无限深且永不穿帮」：方向是客户端体素分块缓存（像 chunk load 一样跨帧摊销），属 game core，不是 shader 该背的——但方向场视觉欺骗够用，暂无此需求。
