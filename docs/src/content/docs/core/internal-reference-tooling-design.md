---
title: Record 内部引用的工具链支持规划
description: 为 cfgdev skills、VSCode CFG 扩展和 cfgeditor 补齐内部引用支持
sidebar:
  order: 102
---

状态：阶段 0–8 已完成；设计先提交，再分阶段实现、测试、精简和独立审查。记录日期：2026-10-20。

本规划接续 [Record 内部对象引用设计](../internal-reference-design/) 和 [实现与交付记录](../internal-reference-implementation/)。先完成各语言示例的实际生成、编译、运行和必要修复，再依次实施本文阶段。每个阶段完成测试、精简和审查后独立提交，随后进入下一阶段。

## 目的与边界

让现有工具能够正确生成、理解和浏览新 schema，继续用原始键字段编辑数据。目标包括本 record、跨 record、最近祖先和独立来源路径，不增加新的引用语义。

- Java 的 `Cfg.g4`、`CfgReader` 和 `CfgWriter` 是语法及规范输出的权威。VSCode 继续使用现有 ANTLR TypeScript parser，grammar 从 Java 源同步，不单独演化语法。
- CFG 格式化沿用现有的 `CfgReader → CfgWriter` 读写流程，统一规范输出。
- cfgeditor 使用服务端已解析的引用结果，前端不再次实现祖先匹配、集合索引或引用解析。
- 编辑仍修改原始数据字段；派生引用仅用于展示和定位，不写进配置，不自动级联改名，不增加独立目标选择器。
- 本阶段不引入 LSP、符号数据库、持久对象 ID 或通用编辑器插件框架。

## 已核实的现状

| 模块 | 已有能力 | 缺口 |
| --- | --- | --- |
| `cfgdev/skills/cfggen-architect` | CFG 架构与 schema 生成指导、eval 用例 | 没有内部引用、`index`、混合来源规则；旧示例把 `->task.id` 当成字段引用，容易生成错误 schema |
| VSCode grammar | 独立的 `Cfg.g4` 及生成的 TypeScript parser | `ref` 仍是旧规则，不接受 `::`、`@`、空来源 `[]` 和来源路径 |
| VSCode 语言功能 | TextMate、semantic tokens、定义、引用、大纲 | visitor 假设引用只有一个表；缓存按磁盘 mtime/size，未保存文本可能复用旧结果 |
| Java schema 输出 | 新引用语法已有读写往返测试 | 沿用现有 `CfgWriter`，规范输出新引用表达式 |
| cfgeditor schema | DTO 已有 `index`、`pathReference`、`resultType` | 跨表边仍接到目标 record；本 record 和祖先引用不展示；缺少目标对象位置 |
| cfgeditor record | 子对象已用字段链和数组位置建节点，map 已用 `$entry` 数组展示 | `$refs` 只有 `toTable/toId/firstField`；服务端主动省略同 record 引用 |
| cfgeditor 编辑 | 现有键字段、undo/redo、fold/embed、保存校验 | 内部引用不能定位；`long` 使用普通 JSON number 和 InputNumber，超出 JavaScript 安全整数时可能损失精度 |

主要实现入口：`cfgdev/vscode-cfg-extension/src/grammar/Cfg.g4`、`src/definition/*`、`src/highlight/*`，以及 `app/schema/cfg`、`app/value/ValueRefCollector`、`ValueToJson`、`editorserver` 和 `cfgeditor/src/features/{table,record}`。

## Skills：教会现有语法

更新 `cfggen-architect/SKILL.md` 的语法示例与引用选择规则，并增加精简的内部引用参考和 eval：

| 场景 | 应使用的声明 |
| --- | --- |
| 普通表主键、唯一键 | `->table`、`->table[code]` |
| 本 record 内部对象 | `->::machines.phases` |
| 跨 record 内部对象 | `->ability::machines.phases`、`->ability[code]::machines.phases` |
| 最近祖先 | `->@Machine::phases`、`->@Command.StateMachine::phases` |
| 不同层来源 | `[@AbilityUse::binding.abilityId,@MachineUse::machineKey,phaseKey]` |
| 固定对象，无消耗键 | `->spawn:[] ->::origin` |

指导应明确：局部唯一是集合实例内唯一；最近祖先不回退；合法空键与 nullable 不等价；map 使用自然键；interface 可作终点但不能继续穿透；源路径不能穿过集合或派生引用。优先保留内嵌所有权，只有独立维护、共享发布确有需要时才拆子表。

示例直接引用 `example/config/combat`，避免维护另一份大型 schema。新增 eval 分别覆盖本 record、跨 record、混合祖先和拒绝非法目标路径；正确性用实际 `CfgReader`/schema 校验验证，不只检查输出中是否出现关键词。

## VSCode：同步语法与保持导航正确

### Grammar 单一来源

保留扩展内的 grammar 文件作为构建输入，但通过一个脚本从 `app/src/main/java/configgen/schema/cfg/Cfg.g4` 同步，再运行现有 `antlr-ng`。Java grammar 当前不含专属语言 action，应按原文同步；以后若出现语言专属内容，再针对该事实调整，不预先增加转换层。

仓库测试检查 grammar 副本与权威文件一致，忽略约定的换行差异。扩展打包包含生成代码，安装后的扩展不依赖仓库源码目录。调整 `postinstall`：源码在仓库中时同步，独立源码包使用已携带的 grammar，不能在安装阶段访问不存在的相对路径或从网络拉取 grammar。

语法验收以 `combat.cfg`、新引用语法测试和现有扩展 fixture 为共同输入。TextMate 只负责即时词法着色；目标表、祖先类型和字段的 semantic token 从 parser token range 获取，不用一个跨行大范围覆盖整条引用。

### 引用位置只做必要调整

同一条合法声明可以同时包含多个祖先来源和一个跨表目标，例如：

```cfg
->target:[@AbilityUse::binding.abilityId,@MachineUse::machineKey,phaseKey]
    ->ability::machines.phases;
```

现有一个 `Ref` 只能容纳一个 `refType` 和一个 `refTable`，会丢失上例的两个祖先类型；按声明起始行存引用也会使第二行点击失效。因此将文件缓存中的引用改为简单的 range 列表，每项只保存类别、名字、完整源码 range 和已有接口上下文。定义与查找引用共用该列表，不新增通用符号图。

首版必须完成：旧类型和表导航、新目标表导航、每个 `@Type` 的类型导航、`@Interface.Impl` 的实现导航，以及这些声明的查找引用。类型和表查找保持不同命名空间：本地 struct 不能遮蔽目标表。无声明名字的 `::` 和内部字段段不伪装成表引用。

字段路径的逐段跳转需要字段符号、各根 record 上下文和 list/map 元素类型推导，当前扩展尚无该能力；本轮不顺带重建 schema resolver。字段段应正确着色，定义命令可无结果，不能跳到同名表或错误类型。文档明确该边界。

缓存使用 `TextDocument.version` 判断打开文档是否变化；未保存文本直接解析当前 buffer。关闭文档后仍可使用已有文件缓存机制。修改、撤销之后的高亮和导航不得停留在旧文本。

ANTLR token offset/column 按 Unicode code point 计算，VSCode range 使用 UTF-16。所有 visitor 共用一处位置转换，不直接把 token 索引当成 JavaScript 字符串下标；用 metadata 字符串和前置注释中的 emoji 验证同一行及后续行的位置。

## cfgeditor：解释引用并定位现有对象

### Schema 浏览

复用现有字段和引用展示，在集合字段旁显示局部索引字段；为新引用显示只读表达式和结果类型。展示内容来自 DTO 的 `pathReference`、`resultType`、`index`，不重新解析 `.cfg`。

| 引用起点 | Schema 视图 | Record 视图 |
| --- | --- | --- |
| 本 record `::` | 显示根起点和目标路径；struct 被多个根复用时不猜一个根表 | 指向当前 record 的实际内部对象 |
| 最近祖先 `@Type` | 显示指定类型和路径；类型已在图中时可沿现有入口定位该类型 | 指向解析得到的最近实例，不搜索其他同名对象 |
| 外部表 `table` | 保留跨表关系线，补充唯一键和内部路径说明 | 先定位目标 record，再定位其内部对象 |

混合来源逐项显示原表达式；没有 CURRENT 来源时使用节点级出口，不绑定到一个不存在的字段 handle。固定 `[]` 引用也有只读解释。循环引用不递归展开新副本。首版不为虚拟引用增加输入控件或可写 JSON 属性。

### Record 定位协议

为现有 `FieldRef` 增加可选的 `toPath: (string | number)[]`，保留 `firstField/label/toTable/toId`。旧表引用不带 `toPath`，继续指向 record 根；新引用带 `toPath`，空数组表示根对象。`toPath` 是**这次响应中目标 record JSON 的字段链**，与现有 `fieldChain` 约定相同。

- struct/interface 字段使用字段名；list 使用当前数组下标；map 按现有 `$entry` JSON 数组表示，定位 value 时包含条目下标和 `value`。
- interface 目标定位到实际实现对象，字段链不添加额外 `$type` 层。
- 地址由服务端当前查询快照产生，map 顺序与 `ValueToJson` 的同一次输出保持一致。不得解析 `Occurrence.path` 的诊断字符串来猜 JSON 路径。
- `ValueReferences` 继续负责绑定与身份；通过序列化位置映射把实际目标转换成 JSON 字段链，不在每条引用上重新扫描所有表。映射局限于一次请求，不创建全局对象缓存。
- record 响应携带当前配置快照的 opaque `revision`，新路径引用附带 `toRevision`。版本在现有 editor 数据快照更新点生成，并与 value 原子取得，不把 revision 加入持久化配置。前端只把 `toPath` 应用到相同 revision 的目标 JSON；跨请求打开详情、watch 刷新或已有目标来自其他 revision 时，先回退到 record 并刷新关系，不把旧下标应用到新数组。旧服务没有 revision 时也只定位 record 根。
- 同 record 引用进入详细对象的 `$refs`，但不再扩展记录 frontier、深度或未引用 record 统计。不同 record 仍进入已有 frontier，不能因为新增内部目标而改变旧查询结果。
- nullable 目标缺失时没有可点击目标；schema 解释仍可见，服务端校验错误继续使用现有错误通道。

前端先用现有 record 子节点 ID 规则把字段链映射到节点。目标 record 已展开时直接连到内部节点；仅有 brief 卡片时仍连到卡片并显示内部路径，使用现有打开 record 动作进入详情后定位。目标被 fold/embed 隐藏时定位到最近可见父节点或字段摘要，提供已有展开入口，不自动改写持久化的 `$fold/$embed_*`。不为跨表目标一次性加载所有子图。

`toPath` 不保存为配置、书签、资源 ID 或引用键。排序、删除、切换接口实现后路径可能变化，必须随查询结果更新；原始键仍决定引用身份。边、tooltip 和定位使用当前结果，不把旧响应的数组下标应用到另一份数据上。

### 编辑继续修改原始键

当前 primitive、struct、list/map 编辑入口及 `EditingSession` 保持不变。单个直接字段仍在原字段输入；复合和混合来源分别编辑各自字段；固定引用无可写键。旧主键下拉照常工作，新内部引用不错误套用整个目标表的 record ID 下拉。

派生关系展示来自最后一次服务端校验结果。编辑会话变脏后不沿用旧对象位置作为确定目标，显示待校验；提交后随刷新得到新关系。undo/redo 和列表重排只修改已有数据，不自动重写其他引用的原始键；无效键由既有提交校验报告。

`long` 键必须完整往返。服务端 editor JSON 对 long 字段使用十进制字符串，前端 long 控件使用既有 InputNumber 的 `stringMode` 或等价精确文本输入；编辑器提交适配层按 schema 将合法十进制字符串恢复为 64 位值，严格校验整数和范围。旧客户端提交的安全整数 number 可兼容；对已不安全的 number 拒绝保存，不能猜测其原值。兼顾 long 位于 list、map key、struct key 和自动补全选项中的情况。

精确值转换仅用于 `RecordService`/编辑校验与保存入口的 editor 协议。`ValueToJson` 同时用于普通配置输出、查询和其他模块，不可全局把 `VLong` 改成字符串；应提供明确的 editor 序列化入口或窄适配层，默认输出行为保持不变。提交时只在 editor 适配层恢复已知 long 字段，不改变通用 `ValueJsonParser` 的 Number 要求，也不把持久化 JSON、bytes 或其他语言生成器切换为字符串。

后端新返回值要求同步升级前端：新前端读取旧服务的安全 number 或新服务的十进制字符串，旧前端可能无法编辑字符串形式。通过当前 schema 响应增加一个可选的 editor 值编码标志，让更新后的前端识别并拒绝精确编辑不受支持的旧服务；记录协议变化并将服务与前端共同发布，不把它宣称为旧前端完全兼容。若暂时不能升级服务，超过安全范围的 long 显示为不可提交，避免“未改值保存”静默破坏配置。

## 实施顺序与独立提交

每一行是一个可审查的切片。阶段结束都执行“验证 → 删除冗余/重构 → 独立审查 → 提交”，失败先修复再进入下一行，不累积多个未验证阶段。

| 阶段 | 范围与交付 | 必须通过的验收 |
| --- | --- | --- |
| 0 | 完成多语言示例优化，记录实际运行命令 | 所有现有示例重生成、编译、加载；新旧配置、Mod、i18n、64 位键；重复生成稳定 |
| 1 | 更新 skill 与 eval | 旧错误语法被移除；新引用示例可被实际 parser/schema 校验；合法与非法选择规则明确 |
| 2 | 同步 VSCode grammar、parser、高亮和必要的 range 引用列表 | grammar 一致性；旧 fixture 与 combat 解析；多来源、多行、同一行多声明；表/类型导航与引用；未保存编辑缓存 |
| 3 | editor long 精确协议及原控件适配 | `9007199254740993`、`long` 最小/最大值、负数/零、list/map/struct key；读后未改提交与 undo/redo 不改值；拒绝小数、越界和不安全 number |
| 4 | 扩展引用查询结果和 schema 展示 | `toPath` 与同次 JSON 输出一致，跨响应 revision 不同时回退；本 record/祖先/跨表、interface/map、空对象；不改变旧 frontier、深度和未引用 record 结果；每请求只复用一份绑定结果 |
| 5 | 接入现有图和编辑体验 | 内部目标连线、brief 回退、详情定位、fold/embed 回退、环；键字段正常编辑/保存/撤销；变脏不误用旧位置；新旧配置浏览验收 |
| 6 | 整体验收与文档收尾 | app 测试、扩展构建与语言 fixture、cfgeditor typecheck/lint/纯逻辑测试；真实 VSCode 和浏览器验证导航、定位、键编辑及保存 |

阶段 2 的导航不承担完整字段 resolver；阶段 4/5 的对象定位不承担对象选择器。若审查发现某个需求必须增加新模块，先说明具体缺口，再选择最小改动，不以“以后可能需要”为由预建抽象。

## 回归标准与完成条件

沿用 `example/config/combat` 和旧 equip/task/monster 数据作为共同基准，补充短 fixture，避免复制整份配置。静态断言只用于 grammar 和协议形状；关键功能用实际 parse、编辑前后值比较和运行环境验证。

实际环境验收至少包含：从混合来源声明的不同 token 跳到各自定义；浏览 Combat/Movement 同名 `Idle` 时定位正确；跨 record `RefTarget` 定位到 Hit；递归 StateMachine 最近作用域不回退；长整数字段打开后未修改保存仍为 `9007199254740993`；列表重排后刷新目标而不是保存数组位置；nullable 缺失和引用环不产生假节点或无限展开。

完成条件是上述范围功能可用、兼容性验收通过、生成物和文档同步、每个切片独立提交且工作区干净。继续优化只处理可证明的错误、重复或多余模块；当审查不再发现这类问题时停止本轮重构，不以无边界重写替代交付。

## 实施与验收记录

| 切片 | 提交与结果 |
| --- | --- |
| 计划 | `29237357`，实施前保存本设计及阶段顺序 |
| 示例收尾 | `2e05ded7` 记录全部 15 个示例的重新生成、编译和运行；Java/Lua 关系绑定并入已有模块，移除多余生成文件 |
| Skills | `c7bfca58`，更新指导、独立语法示例和 7 项 eval；示例经实际 CFG parser 和 schema resolve 验证 |
| VSCode 语法与导航 | `7e783a86`；grammar 从 app 同步，range 列表保留多个来源，统一 UTF-16 位置换算，缓存使用当前文档版本 |
| editor 精确 long | `892b182e`，显式 editor 协议与 schema 适配；普通 JSON 输出保持数字 |
| editor 路径与 schema | `7178197c`，快照 revision、结构化目标路径、同 record 引用及只读 schema 说明 |
| editor 图与编辑 | `53ac848f`，内部目标、brief/fold/embed 回退、临时详情定位、dirty 及保存后等待新校验 |
| 独立审查修复与精简 | `df243cf9` 删除无调用的扩展 helper；`0e114892` 补 JSON 导入的精确值边界；`ba639bd0` 修复深层模块漏引用；`c3b317d9` 对齐 BigInt 构建目标；`0fcec837` 删除重复的 schema 行说明 |

| 验证 | 结果 |
| --- | --- |
| app 默认测试 | 819 项通过，含 CFG 读写、内部引用语法、精确 long、目标路径、revision、旧查询投影及请求内解析器复用 |
| 额外运行时测试 | C# 11、TypeScript/Go 15、Godot 4、Lua 5；合计连同默认测试 854 项，零失败、零跳过 |
| 最终 JAR 的全部示例 | `example/gen_run.bat` 再次完成 15 种语言/文本模式的生成、编译和运行，`PASS=15 FAIL=0`；生成内容稳定 |
| 真实 VSCode | 独立 ExtensionHost 的 12 项语法与导航测试通过；TypeScript、lint 和生产 bundle 构建通过 |
| cfgeditor 纯逻辑 | 35 个文件、551 项通过；TypeScript、全项目 oxlint 通过 |
| cfgeditor 生产构建 | 默认 WebKit 语法目标与 Windows 目标均成功；long 运算需要支持 BigInt 的运行时，不宣称已经实测 macOS/Linux WebView |
| 文档 | 45 页构建成功；引用导航、编辑器使用说明与性能约定同步 |

实际浏览器验收使用 `example/config` 的临时副本和生产预览：跨 record 的 `refTarget` 聚焦 Combat/Hit；递归 StateMachine 的合法引用只连内层 Else，缺失的 Done 不回退外层；map 的 interface 目标定位到实际 Damage 对象；fold/embed 的目标回退到当前可见父节点。键修改后撤下旧边并提示待校验，undo 恢复原始键及关系。map 条目重排并保存后，false 的目标由 `modes[0].value` 更新为 `modes[1].value`；长整数字段及 map key 在保存前后仍为 `9007199254740993`。源页面打开后再刷新服务端快照，沿旧引用打开详情时，页面明确提示“配置已变化，已回退到记录根并刷新引用”，没有使用旧下标。旧 equip 的普通表外键和 struct 复合键浏览保持正常。

审查还覆盖保存成功与新 GET 返回之间的窗口：编辑会话已 clean 也不能恢复旧路径，必须等待响应版本及数据都与当前编辑对象对应。新服务以 revision 判断响应是否更新，旧服务才使用 React Query 的响应时间标记；同毫秒到达的新 revision 不会被误判为旧响应。相关回归从实际 `EditingSession` 重排、提交和重置基准开始验证，无 UI/network mock。

最终边界保持本设计：VSCode 不实现逐段字段 resolver，cfgeditor 不新增对象选择器或持久位置 ID。核心与工具链交叉审查已收敛，没有剩余的确证遗漏；后续改进以新的实际问题为依据。

## 2026-10-21 复审与精简

本轮复审从异步编辑、原始数值精度和跨模块名称解析边界重新检查实现。整体分层和复用方向保持：grammar 与 schema 规范输出由 app 提供，引用绑定在请求内复用，editor 的精确值协议与通用 JSON 输出分开。已复现的问题集中在边界状态与遗漏的输入形式，无需增加新的解析器、保存队列或缓存层。

| 问题或重复 | 修复与提交 |
| --- | --- |
| 重复提交覆盖单份快照，请求期间的编辑还可能改变发送对象 | `37237ca8`：复制独立提交快照，以既有快照字段限制同一会话的在途提交；失败释放并保留编辑及 undo |
| 成功重基准后，未保存提示依赖随后 GET 才更新 | `3264a52f`：立即调用既有状态通知，覆盖请求期间撤回旧基准后仍相对新基准 dirty 的情况 |
| 原始 JSON 先在浏览器解析，高精度小数及指数会先舍入成整数 | `6d851555`：原文先交给既有 `/checkJson`；后端保留十进制精度，成功后才解析规范化结果。裸对象支持字符串内反引号，原 AI 围栏输入保留；直接提交与载入表单共用此路径 |
| 跨模块限定类型继承来源 interface 的局部作用域 | `80bbead5`：进入目标模块后清除来源 interface 上下文，定义跳转和引用查找共用修正结果 |
| 扩展误扫描数字、下划线开头的辅助目录，遗漏补充平面汉字后缀 | `29186687`：模块目录判定与 Java loader 对齐，并由真实模块 fixture 验证 |
| `/recordRefIds` 展开记录时重复构建整库引用绑定 | `cc0dd816`：同请求的入边与出边收集器共用现有绑定结果；实际计数回归从 3 次降为 1 次，旧 schema 为 0 次，记录投影与深度保持一致 |
| 图构造、导航、primitive 默认值及扩展透传 helper 有重复 | `af3668a5`、`aeee48a9`、`ac8add34`：合并相同分支、复用 schema 默认值、删除未用数据和纯透传代码，无新增抽象 |
| 真实 VSCode undo 命令完成与文档更新有异步间隔 | `75c37940`：预先监听目标文档变化并有界等待，保留全文断言，无固定 sleep |

复审后的验证结果为 app 默认 841 项，加上 C# 11、TypeScript/Go 15、Godot 4、Lua 5，共 **876 项，零失败、零跳过**；cfgeditor **35 个文件、559 项**通过，TypeScript、全项目 oxlint 和生产构建通过；最终生产 bundle 的 **18 项真实 ExtensionHost** 验收通过，VSIX 已重新生成。文档保持 45 页构建通过。本轮没有修改生成器，前一轮全部 15 种示例的重新生成与运行记录仍作为生成物验收依据。

实际生产预览同时验证直接 JSON 提交与载入表单：`1e-400` 被拒绝且未写入记录；合法数据载入后清除旧错误；`9007199254740993` 和字符串内的反引号经过保存、重新打开及服务端读取仍完整一致。另用真实 HTTP 校验高精度小数与指数，并确认合法 `1e3` 规范化为 `"1000"`。测试只写入 build 下的临时配置，未修改示例数据。

原始 JSON 导入依赖更新后的 `/checkJson` 裸对象和精度校验，前端与服务需要一起升级；旧服务失败时不回退到浏览器解析。其余精简保持原有行为。继续拆分大型编辑会话、增加通用数字解析器或缓存均没有本轮证据支持，暂不引入。

### 最终 JAR 的全部示例复验

完成上述修复后，使用 `app/build/libs/cfggen.jar` 再次执行完整的 `example/gen_run.bat`。Java 2、C# 3、Go 3、TypeScript 3、Lua 2、GDScript 2，共 15 个模式均重新生成、实际编译并运行；补齐断言前后两轮均为 **`PASS=15 FAIL=0`**。额外运行 `-datadir example/config -gen verify` 通过，生成代码和数据无差异。

本轮未发现生成器或运行库缺陷，补齐的是示例验证入口的断言：`b0ace3cb` 为两个 Java 模式补齐主要内部引用结果身份和 Mod 原快照/来源断言，C# 共享入口补空包含对象身份；`e1eebca2` 为 Lua 补齐各类寻址、键型、nullable 与最近祖先断言，并加强 Godot 的 struct map/表键目标身份；`fa149daf` 为 TypeScript/Go 补旧 list、map value、反向集合外键和空对象身份。所有检查直接使用实际加载对象，无 mock 或额外验证框架。运行方式和各模式列表见 `example/README.md`；Lua 使用 Lupa Lua 5.4 本地 CLI 桥接，Godot 使用 4.6 headless。

### 示例验证代码复审

进一步审查发现几处验证自身的漏测：`next.next == Idle` 会放过 Idle 自环，集合循环可因没有元素而零次检查，struct map 取首项或要求所有 value 相同会漏报或误报，只比较部分空对象配对也不能证明全体身份互异。已改为明确验证 Idle→Hit→Idle、必要的基数或存在性检查、按 struct 键内容取目标，以及使用语言原生的局部身份集合；同时统一遍历 Combat 和 Movement 的各类索引、所有叶来源及固定接口终点，覆盖相同键值在不同包含作用域中的目标身份。

`4df5992f` 修正 TypeScript/Go 的上述断言并删除普通 Go 在 `DoTest()` 后重复的旧配置检查；`2b953694` 统一 Java/C# 双作用域验证，拆分含义不同的长条件并补齐空对象类型及全体身份检查；`79b123ae` 将两个 Lua 模式相同的 combat 块收敛为一个普通函数，删除已被覆盖的 list 字段检查，补强 Godot 双作用域与重新加载后的旧引用检查。已明确只有一个 `AbilityUse` 的外层循环改为直接取本地变量。不同语言、不同生成 API 的合理重复保留，不增加跨项目验证框架。

临时配置副本的真实生成与执行证明了修改有效：Idle 自环与空目标列表均通过旧 TypeScript 入口、被新入口拒绝；struct map 加入额外合法条目时旧入口失败、新入口通过。三份数据均先由最终 JAR 成功生成，反例仅保留在忽略的 build 目录中。Lua 的共享文件也同步复制到既有临时项目运行测试：先复现缺文件失败，补一行依赖复制后 **5 项 Lua 运行时测试通过，零失败、零跳过**。

修改后再次执行最终 JAR 的全部示例生成、编译和运行，**`PASS=15 FAIL=0`**；生成代码和数据无差异。文档仍为 45 页构建通过。

### 断言反例自动回归

`f14b6ff9` 将上述三个配置变体固定为 `example/ts/reference-regressions.cjs`，接入普通 TypeScript 的 `npm test`；`4215dcef` 接入既有 Test CI，构建最终 JAR 后重新生成 TS/bytes 并执行同一入口。脚本只使用 Node.js 标准库，在独立临时目录中修改完整配置的原文，检查替换次数并保留长整数。所有变体必须先生成成功；两个负例只接受各自指定的断言错误，合法 struct map 扩展必须通过全部示例断言。结束时只清理本次创建的目录。

本地重复验证三个回归均通过；另逐一暂时弱化方向、数量和 map 选择断言，确认测试会失败，并确认无关断言异常也不能冒充预期失败。随后恢复入口，app 默认 **841 项**通过，最终 JAR 的全部示例再次 **`PASS=15 FAIL=0`**，包含三个自动回归；生成代码和数据无差异。CI 接线已按相同命令本地验证，未将尚未执行的远端 CI 记为通过。

### RelationResolver 复审与精简

`bf9043ec` 统一精简 Java、C#、TypeScript、Go、Lua 和 GDScript 的包含上下文：每个结构位置只保存一个父上下文，代替复制完整祖先列表；只有存在引用计划的对象进入第二轮绑定。无计划的中间结构仍完整遍历，既可提供后代作用域，也保留未被引用索引和 map 的重复键校验。祖先查找从当前对象开始，最近类型命中后不向外层回退。独立复审还补齐 TypeScript 的 `exactOptionalPropertyTypes` 编译验证，父字段显式接受 `undefined`。

Lua 进一步取消普通 list 和未被引用的标量 map 的无用 map 索引。标量键由原生 table 按值区分；struct 键仍在完整遍历时按字段内容检查，即使 map 未被任何关系使用。新增两个真实加载回归：在绑定前通过生成的 bean 构造器插入不同实例，合法新键应加载成功，内容重复键必须以 `Duplicate map key` 失败。测试不改正常配置，不增加框架或容器元数据。

两个切片分别重新生成、编译并运行全部 15 个示例模式，均为 **`PASS=15 FAIL=0`**，包含三个自动断言回归；最终 JAR 的额外 `-gen verify` 通过。app 默认 **841**、C# **11**、TypeScript/Go **15**、Godot **4**、Lua **7**，共 **878 项，零失败、零跳过**，生成代码同步更新。

另用 JDK 的线程分配计数比较重构前后的 Java resolver：400 层无计划结构包含一个有两个计划的叶节点，每次新建 resolver，并检查根对象和最近父对象的引用身份。每版预热 300 次后采样 5 轮，每轮 100 次；每次解析分配量中位数由 **979,014 字节**降为 **46,616 字节**，减少 **95.24%**。这是深包含合成场景的分配证据，不作为生产吞吐承诺；探针和完整日志保留在忽略的 `app/build/relation-resolver-review`。

本轮保留两阶段绑定和按身份缓存：前者保证未使用索引也得到校验，后者区分内容相同但属于不同包含位置或快照的对象。没有新增持久父指针、跨快照缓存或通用遍历框架。复审未再发现需要扩大改动的确证问题。

### RelationResolver 与新增测试的二次复审

`8d2080a1` 将 Java 的单计划绑定从 `resolve()` 提取到私有 `bind()`，明确遍历调度与路径解析的职责，外部表判断只计算一次。公有描述符和生成调用均保持不变，已有复制运行库仍可编译使用。所有源作用域仍先完成检查，缺少作用域不被 nullable 忽略；索引错误仍在绑定前拒绝。

`98e3a90c` 将 Go 的标量 map 查找索引改为按需构建。生成的强类型 map 保证同一容器的键类型一致，解码入口已检查标量重复键；非空 struct map 仍完整执行内容重复校验。新增本地 fixture 的未引用 struct map 回归：仅 variant 不同的条目应允许，另一个指针具有相同完整键时必须报 `duplicate map key`。两次重新绑定都使用真实遍历，不新增辅助框架或共享夹具。

复审补齐 Java、C#、TypeScript、Go 和 Godot 既有运行时测试中的递归最近祖先断言：先证明外层存在同键目标，再验证内层未命中不回退，以及内层合法引用保持目标身份。Lua 复用已有 `CsvSource`，分别验证 struct 键首字段不同、第二字段不同和完整键重复，减少专用 import 和脚本变量。`6baa52b2` 同时让三个 TypeScript 示例启用 `exactOptionalPropertyTypes`，使既有 Test CI 的普通 TS 入口持续覆盖这一编译边界。

修改后完整执行 app 默认 **841**、C# **11**、TypeScript/Go **15**、Godot **4**、Lua **8**，共 **879 项，零失败、零跳过**。最终 JAR 重新生成、编译并运行全部 15 个示例模式，**`PASS=15 FAIL=0`**，包含三个自动断言回归；额外 `-gen verify` 通过。Java 和 Go 生成代码同步更新，其余生成代码及数据保持稳定。

### 精简数据包与生成代码

删除额外的关系元数据、专用包版本、各语言的关系摘要生成和比对，以及只服务这些检查的测试。数据包统一沿用版本 1，内部引用直接根据 schema 是否包含引用或索引启用；加载时继续执行原有 schema 检查、索引唯一性、作用域和引用结果校验。验收记录只保留实际生成、编译和运行结果。

删除后 app 默认 **833**、C# **11**、TypeScript/Go **15**、Godot **4**、Lua **8**，共 **871 项，零失败、零跳过**。减少的 8 项仅测试已移除的额外机制。最终 JAR 重新生成、编译并运行全部 15 个示例模式，**`PASS=15 FAIL=0`**，三个自动断言回归及额外 `-gen verify` 通过；生成代码与数据同步更新。

### 生成与加载流程的整体精简

删除 TypeScript 的运行库源码标记扫描、对应专用测试及两个未使用的字段输出方法。六种语言直接解析来源并读取键，去掉临时作用域容器和第二次遍历。C# 使用已有字典直接查询与遍历，删除 `MapView` 外壳；Go 根据扁平 struct 键约束删除递归转换；Lua 将只有同一个合并判断的加载标志收敛为一个；Godot 快照复用解析器已经收集的记录容器，去掉重复收集和数组。两阶段绑定、实例身份缓存、不同键定义的索引和弱引用生命周期保持原有用途，不增加替代机制。

修改后 app 默认 **833**、C# **11**、TypeScript/Go **14**、Godot **4**、Lua **8**，共 **870 项，零失败、零跳过**。减少的一项仅验证已删除的源码标记扫描。最终 JAR 的全部 15 个示例重新生成、编译并运行，**`PASS=15 FAIL=0`**，包含三个自动断言回归；额外 `-gen verify` 通过，生成代码同步更新，配置和导出数据无差异。
