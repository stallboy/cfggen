# cfgeditor 单元测试指南

> 本文是 cfgeditor 单元测试的**现状记录**与**长期指导**。新增测试或讨论"要不要测某段代码"时，以本文为准。
> 面向新人：前半部分讲原则（带业界出处，偏教学），后半部分讲本项目怎么落地、下一步测什么。

---

## 一、定位：这份文档回答四个问题

1. **为什么测？** —— 防止回归、逼出设计问题、充当可执行文档。
2. **测什么？** —— 纯逻辑（domain / services / flow 里的纯函数与类），**不测** UI 渲染、网络、Tauri IPC。
3. **怎么测？** —— vitest + jsdom，喂 fixture 进去，断言输出，**零业务 mock**。
4. **下一步测什么？** —— 见第五节优先级清单。

---

## 二、现状：cfgeditor 现在怎么测

### 2.1 技术栈

| 项 | 选择 | 备注 |
|---|---|---|
| 测试框架 | **vitest 4** | 与 vite 共享转译管线，快 |
| DOM 环境 | **jsdom** | 给 antd / resso / `@tauri-apps/api` 提供 `window` |
| React 转译 | `@vitejs/plugin-react` | 编译被测的 `.tsx`（如 `schema.tsx`） |
| 配置 | `vitest.config.ts` | 与生产构建 `vite.config.ts` **解耦** |
| setup | `src/test/setup.ts` | 只做 Tauri 运行时 shim，**不 mock 业务** |
| fixture | `src/test/fixtures.ts` | 集中构造冗长类型（DAMP） |

约定（来自 `vitest.config.ts`）：
- 测试文件与源码**同目录**，命名 `*.test.ts` / `*.test.tsx`。
- 用**显式** `import { describe, it, expect } from 'vitest'`，**不开** `globals: true`——让依赖可见，也避免全局命名空间污染。
- `environment: 'jsdom'`，`include: ['src/**/*.{test,spec}.{ts,tsx}']`。

### 2.2 已覆盖模块（截至 2026-07-14）

共 **27 个测试文件**，全部针对纯逻辑（`layoutAsync` 是唯一带 mock 的——见下注）：

```
api/       noteModel, schemaModel(isPrimitiveType/isNumberType + 类型集合一致性)
domain/    schema, historyModel, undoStore, entityPredicates, nodeShowLayoutKeys,
           embedding, entityModel(type guards)
flow/      colors, calcWidthHeight, simpleStrRowCount, dimensions,
           getDsLenAndDesc, entityToNodeAndEdge, viewportMath, layoutAsync
res/       resUtils, getResBrief
routes/    record/{recordEntityCreator, recordEditEntityCreator, recordRefUtils}
           table/{tableEntityCreator, tableRefEntity}
           setting/colorUtils
services/  editingSession, clipboard
```

> 注：`layoutAsync` 用 `vi.mock` 替换 elkjs 的 `layout` 入口——mock 的是**出站命令的契约边界**，断言的是 `layoutAsync` 自身如何组织入参 / 处理错误 / 映射结果，不是 elkjs 内部行为，故不违反 §3.7「不 mock 你不拥有的东西」（它 mock 边界、不 mock 内部）。
> 维护提示：新增测试后请同步本清单，以及 `cfgeditor/CLAUDE.md` 的「单元测试」章节。

### 2.3 这些测试做对了什么（值得学的范本）

读 `src/services/editingSession.test.ts` 和 `src/domain/schema.test.ts`，能看到本项目测试的几个高阶特征：

1. **行为命名，中文 `it`**：`it('早退：同 table/id 且内容相等时保留当前编辑态（不额外 bump）')`——失败信息自带语义，不用读用例体就知道断的什么。
2. **不变量断言优于单点断言**：fuzz 用例喂 300 次随机混合操作，每步只断言两个不变量——`structureVersion` 单调不减、`editingObject` 始终有效。这比写 300 个具体用例覆盖更广、更便宜。
3. **确定性 fuzz**：随机源是固定 seed 的 LCG（`seed = 12345`），失败可复现，不会偶发红。
4. **引用相等性当作契约来测**：`expect(s.getEditingObject()).toBe(editingRef)` 验证「早退路径不换引用」，把"就地变异 vs 新建对象"这种隐式设计**显式化**为可执行契约。
5. **DAMP fixture**：`makeRecord` / `ITEM` 这类极小工厂就近定义，每个 `it` 自给自足，读一个用例不需要上下文。

---

## 三、原则（教学核心）

### 3.1 测试金字塔 与 测试奖杯

**测试金字塔**（Mike Cohn, *Succeeding with Agile*, 2009）：底层大量**快、稳、便宜**的单元测试，中层少量集成测试，顶层极少端到端（E2E）。越往上越慢、越脆、越贵。

```
        /\
       /e2e\         ← 慢、脆、贵；只覆盖关键用户路径
      /------\
     / 集成  \       ← 跨模块、真实依赖；本项目目前主要靠手测
    /----------\
   /   单元测试  \    ← 快、稳、便宜；本项目这一层 ← 你在这里
  /--------------\
```

**测试奖杯**（Kent C. Dodds, "The Testing Trophy", 2018）：在金字塔基础上强调**静态检查**（TypeScript、lint）权重最大，并把**集成测试**提到奖杯主体——因为它在"信心 / 成本"比上最划算。

> cfgeditor 的选择：静态层有 **TypeScript 7 + oxlint** 兜底；自动化测试集中在**单元层**。集成与 E2E 目前靠人工，是刻意的取舍（见 3.3）。

### 3.2 测行为，不测实现：Sandi Metz 的测试矩阵

Sandi Metz 在 *"The Magic Tricks of Testing"* (RailsConf 2013) 给出一张矩阵，按**消息方向 × 消息类型**决定要不要测：

|  | **入站消息（incoming）** | **出站消息（outgoing）** |
|---|---|---|
| **命令（command，有副作用）** | **测**：断言状态改变 | **测（用 mock）**：断言"发出了正确调用" |
| **查询（query，无副作用，有返回值）** | **测**：断言返回值 | **不测**：别 mock |

落到本项目：
- `Schema.getAllDepStructs(s)` 是**入站查询** → 直接喂数据断言返回值（`schema.test.ts` 正是这么做的）。
- `EditingSession.addArrayItem` 是**入站命令** → 断言它改了内部状态（`getEditingObject().items.length === 1`）+ 通知了订阅者（`notified === 1`）。
- `api/api.ts` 里调 axios 是**出站命令** → 如果要测，应 mock axios 断言"发出了正确请求"；本项目当前不测这一层。
- 不要为了凑覆盖率去测第三方库的**出站查询**（例如别 mock antd 的 Form 再断言它内部状态）。

一句话：**测 public 行为，不测 private 实现。** 私有方法被测，是因为把它当成 public 入口喂数据；不是为了"覆盖私有方法"才去测它。

### 3.3 测纯逻辑，不测 UI/网络/IPC —— 本项目的核心取舍

cfgeditor 刻意把可测的**规则与状态**下沉到 `domain/` `services/` `flow/*.ts`（纯函数与纯类），让 UI 组件（`*.tsx`）尽量**薄**——只做"读状态 → 渲染 → 派发动作"。

带来的结论：
- ✅ 测 `domain/schema`、`services/editingSession`、`flow/colors`：喂 fixture，断言输出，**零 mock**。
- ❌ 不测 `EntityForm.tsx`（782 行 UI）、`Record.tsx`、`api/api.ts`（HTTP）、`res/readResInfosAsync.ts`（Tauri 文件 IPC）。`flow/layoutAsync.ts` 的 elkjs 交互已用 mock 边界测（见 2.2 注）。
- ❌ 不测 `store/store.ts`：Resso 全局状态跨组件耦合，单测性价比低，留给手测。

> **这不是偷懒，是架构使然。** 当你发现一段业务逻辑只能靠"渲染组件 + mock 一堆依赖"才能测时，正确反应不是去写组件测试，而是**把逻辑抽成纯函数**（像 `embedding.ts`、`colors.ts` 那样），再测纯函数。UI 薄到不可测时，它也就**不需要**被单测了。
>
> 例外：若某 UI 交互足够复杂、回归成本高、且不便抽取，可引入 React Testing Library 做行为级组件测试（见 3.7 与第七节）。本项目目前未到此必要。

### 3.4 DAMP > DRY（测试代码不追求极致复用）

生产代码追求 **DRY**（Don't Repeat Yourself）；测试代码追求 **DAMP**（Descriptive And Meaningful Phrases）。

- 每个 `it` 应尽量**自给自足**：在一个用例里就能看全 arrange / act / assert，不必上下跳转。
- 重复的是**数据构造**，抽成 fixture 工厂（`fixtures.ts` 的 `makeTable` / `field` / `fk`）；**重复的是数据，不是断言意图**。
- 反例：为了 DRY 把多个不相关断言塞进一个 `it`、或用 `beforeEach` 隐藏关键 setup，导致失败时定位困难。

> 出处：Google Testing Blog 多次提及；Kent C. Dodds "Write tests. Not too many. Mostly integration." 也强调测试要"可读优先"。

### 3.5 不变量优先（property / invariant thinking）

写测试时先问：**这段代码在任意合法输入下，哪些性质恒成立？** 把不变量写成断言，比枚举具体 case 覆盖更广。

本项目范例（`editingSession.test.ts` 的 fuzz 用例）：
- 不变量 1：`structureVersion` 单调不减。
- 不变量 2：任意操作后 `editingObject` 始终是合法对象（有 `$type`）。

进阶：当不变量足够重要，可用 **property-based testing**（Haskell QuickCheck，Claessen & Hughes, 2000；JS 圈 `fast-check`）让框架自动生成大量随机输入。本项目目前用手写确定性 fuzz 已够用；若 `embedding.ts`、`schema.tsx` 这类规则模块要加深覆盖，可引入 `fast-check`。

### 3.6 FIRST 原则（Robert C. Martin）

好的单元测试应当：

- **F**ast —— 毫秒级。本项目全量跑完应在秒级。慢了说明依赖了网络/真实 IO，重新审视。
- **I**ndependent —— 任一用例独立可跑，顺序无关。别让用例 B 依赖用例 A 的副作用。
- **R**epeatable —— 任意环境结果一致。`Math.random()` / `Date.now()` 是大敌——本项目 fuzz 用**固定 seed 的 LCG** 正是为此。
- **S**elf-validating —— 通过/失败由断言自动判定，不需要人看输出。
- **T**imely —— 在被测代码**之前或同时**写。本项目适合"改纯逻辑前先补/改测试"。

### 3.7 不要 mock 你不拥有的东西

出处：Steve Freeman & Nat Pryce, *Growing Object-Oriented Software, Guided by Tests* (GOOS, 2009)。

- 你**不拥有** axios、antd、Tauri、elkjs——别在单测里 mock 它们的内部行为，否则你测的是你对它 API 的**猜测**，它一升级你的测试就骗你。
- 真要测与它们的集成，写**少量集成测试**或手测，而不是用 mock 自欺。
- 本项目 `setup.ts` 只给 Tauri 补了一个 `path.sep = '/'` 的**最小 shim**（因为 `joinPath` 的纯逻辑会在末尾触到它），并明确注释"分隔符取定值便于断言，不影响逻辑覆盖"——这是 shim 与 mock 的边界：**让代码能跑起来**，而非**伪造行为**。

### 3.8 结构：Arrange-Act-Assert / Given-When-Then

每个 `it` 的身体按三段写，用空行分隔：

```ts
it('返回大于 curId 的下一个空闲整数', () => {
    // Arrange（Given）：已有 {1,2,4}，curId=2
    const table = makeTable('T', [field('id', 'int')], {
        pk: ['id'],
        recordIds: [{id: '1'}, {id: '2'}, {id: '4'}],
    })
    // Act（When）
    const next = getNextId(table, '2')
    // Assert（Then）
    expect(next).toBe(3)
})
```

> Given-When-Then 是 BDD（Dan North, 2006）的表述，本质与 AAA 相同。中文 `it` 标题已经承担了"Then"的语义，用例体里可省略显式注释。

### 3.9 一个 `it` 一个意图

一个用例**只断言一件事的意图**（可以有多个 `expect`，但它们应服务于同一个结论）。理由：失败时一眼知道是哪条契约破了。反例是一个 `it('测试 schema')` 里塞十几个不相关断言。

---

## 四、cfgeditor 落地约定（速查）

| 约定 | 做法 |
|---|---|
| 文件位置 | 与源码**同目录**，`xxx.test.ts` |
| 命名风格 | `describe` 写被测对象/分组；`it` 写**行为**，中文，可含"条件：…→ …" |
| 断言 | `expect` + `toBe`/`toEqual`/`toStrictEqual`/`toBeNull`；引用契约用 `toBe`/`not.toBe` |
| 数据构造 | 用 `src/test/fixtures.ts` 的工厂；新类型补工厂函数，勿在各用例手搓冗长结构 |
| 依赖 | 显式 `import { describe, it, expect } from 'vitest'`，不依赖 globals |
| 环境 | 默认 jsdom；若被测代码不碰 DOM，照常跑，无需特殊处理 |
| 边界 setup | 只在 `src/test/setup.ts` 加**让代码跑起来**的 shim；**绝不**在此 mock 业务依赖 |
| 不要做 | 不 mock axios / antd / Tauri IPC；不渲染组件断言其内部状态；不用真实随机/时间 |
| fuzz | 用固定 seed 的 LCG（参考 `editingSession.test.ts`），保证可复现 |

---

## 五、下一步该测什么（优先级清单）

按"**价值 / 成本**"排序。价值看「逻辑复杂度 × 回归损失」，成本看「纯度 × 是否要 mock」。

### ✅ P0 —— `domain/embedding.ts`（已完成，`embedding.test.ts`，34 例）

覆盖：`EMBEDDING_CONFIG` 阈值快照、`isPrimitiveType` / `isNumberType`、`canBeEmbeddableCheck` 的 5 条规则（struct + interface 两套阈值）、边值（=阈值 / 超阈值）、`allPrimitive` 约束、`$type` 解析、`extractEmbeddingFields`（摊平 / 类型默认值 / `implNameToDisplay` / 空 list 过滤）。

> 为什么这块曾排 P0（保留作背景）：
> - **魔数集中 + 多分支规则**，最容易被改坏且不易察觉。
> - struct 与 interface 阈值不同（`maxNumberFields` 3 vs 2），是典型"边值用例"温床。
> - 纯函数，零 mock，成本极低。

### ✅ P1 —— 小而纯（已完成）

| 模块 | 测试文件 | 覆盖要点 |
|---|---|---|
| `routes/setting/colorUtils.ts` | `colorUtils.test.ts` | `fixColor` 三种入参形态（string / `{toHexString}` / null）+ 默认色；`fixColors` 映射 |
| `services/clipboard.ts` | `clipboard.test.ts` | `isCopiedFitAllowedType` 前缀 + `.` 边界 / off-by-one（每 `it` 先 `structCopy` 重置模块级 `copiedObject`，遵守 3.6） |
| `flow/getDsLenAndDesc.ts` | `getDsLenAndDesc.test.ts` | `refShowDescription` 4 分支 switch + ±1 契约 |
| `res/getResBrief.ts` | `getResBrief.test.ts` | 4 类计数 + 音轨/字幕累加 + 拼串顺序 + 全 0 边界 |

### ✅ P2 —— 中价值（前两项已完成）

| 模块 | 测试文件 | 覆盖要点 |
|---|---|---|
| `routes/table/tableRefEntity.ts` | `tableRefEntity.test.ts` | `includeRefTables`：refIn 分支、`maxOutDepth` 截断（Ref / Ref2）、`maxNode` 截断、curTable 常驻、BFS 连边 |
| `domain/entityModel.ts` | `entityModel.test.ts` | 三个 type guard（`isReadOnlyEntity` / `isEditableEntity` / `isCardEntity`）+ 互斥不变量 |

### 后续候选（无明确优先级）

- `domain/schema.tsx` 还有 `getField` / `getImpl` / `getNextId` / `defaultValue` / `getAllDepStructs` 等纯函数，`schema.test.ts` 目前只覆盖一部分，可按需深化。
- `api/recordModel.ts` 等若有纯转换/规整函数（参照 `noteModel.notesToMap` 的测法）可补；纯类型定义不必测。
- 若 `embedding.ts` / `schema.tsx` 这类规则模块要追求更高置信，可引入 `fast-check` 做 property-based（见 §3.5）。

### 明确**不**测（当前策略）

- 所有 `*.tsx` UI 组件（`EntityForm`、`Record`、`Table`、`Finder`、`Setting` 下的 `.tsx`……）。注意 `Setting/colorUtils.ts` 是纯 `.ts` 函数，已测。
- `api/api.ts`（HTTP，需 mock axios——违反 3.7，留给手测/集成）。
- `res/readResInfosAsync.ts` / `res/summarizeResAsync.ts`（Tauri 文件 IPC，留给手测）。`flow/layoutAsync.ts` 已用 mock 边界测（见 2.2 注）。
- `store/store.ts` / `store/resso.ts`（全局状态库，跨组件耦合）。
- `domain/storageJson.ts`（quicktype **自动生成**，测它等于测生成器；应通过 `genJsonParser.bat` 保证正确性，而非单测）。
- `i18n.ts`（翻译表，靠目视 / 用时验证）。

---

## 六、反模式清单（不要这样做）

1. **为覆盖率而测**：追求 100% 行覆盖，写出 `expect(x).toBeDefined()` 这种空断言。覆盖率是**副产品**，不是目标。
2. **测实现细节**：断言私有变量、断言"某函数被调用了 N 次"（除非它是出站命令契约）。实现一重构测试就红，是脆性测试。
3. **mock 你不拥有的库的内部**：mock axios 的拦截器细节、mock antd 组件树。一旦猜错，测试给你**虚假的安全感**。
4. **真实随机 / 时间**：用 `Math.random()`、`new Date()` 驱动断言 → 偶发红、不可复现。用固定 seed（见 `editingSession.test.ts`）。
5. **跨用例共享可变状态**：用例 A 改了模块级变量，用例 B 默默依赖 → 顺序敏感、难定位。每个 `it` 自备数据。
6. **一个 `it` 塞多个不相关结论**：失败信息丢失语义。
7. **把 setup 塞进 `setup.ts` 当万能 mock 池**：那里只放"让代码跑起来"的环境 shim，不放业务 mock。
8. **测试比被测代码还复杂**：测试读不懂，它本身就成了一坨需要测试的代码。复杂度往往是抽函数的信号。

---

## 七、业界参考（深入阅读）

| 主题 | 出处 |
|---|---|
| 测试金字塔 | Mike Cohn, *Succeeding with Agile* (2009) |
| 测试奖杯（静态>单元>集成>E2E） | Kent C. Dodds, "The Testing Trophy" (2018, kentcdodds.com) |
| 测什么不测什么（消息矩阵） | Sandi Metz, "The Magic Tricks of Testing" (RailsConf 2013, YouTube) |
| 不要 mock 你不拥有的 | Freeman & Pryce, *Growing Object-Oriented Software, Guided by Tests* (GOOS, 2009) |
| FIRST 原则 | Robert C. Martin（Uncle Bob） |
| BDD / Given-When-Then | Dan North, "Introducing BDD" (2006) |
| Property-based testing | Koen Claessen & John Hughes, "QuickCheck" (2000)；JS 实现 `fast-check` |
| 测试的可读性与规模 | Kent C. Dodds, "Write tests. Not too many. Mostly integration." |
| React 组件测试（当真要测时） | React Testing Library + jest-dom，测"用户视角的行为"，配套 `@testing-library/user-event` |

> 关于 React 组件测试：本项目目前**不引入** RTL，因为架构上已把可测逻辑下沉到纯模块。若将来某 UI 流程复杂到必须测，按 Kent C. Dodds 的方式——**以用户能感知的行为断言**（"点击按钮后出现 X"），而不是断言组件内部 state 或调用栈。

---

## 八、维护

- 新增 / 删除测试文件后，更新**第二节清单**和 `cfgeditor/CLAUDE.md` 的「单元测试」章节。
- 改了纯逻辑模块的公共行为，先改/补对应 `*.test.ts`（FIRST 的 Timely）。
- 引入新的全局测试依赖（如 `fast-check`、RTL）前，先在本文件登记理由与边界。
- 本文描述的是**目标与原则**；若现实与本文冲突，优先修现实（把逻辑抽纯 / 修测试），其次才更新本文。
