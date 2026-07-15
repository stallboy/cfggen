# 单元测试：测什么、怎么测、为什么不测 UI/网络/IPC

> 本文是 cfgeditor 单元测试的**长期指导**：讲清测什么、怎么测、为什么这么取舍。前半部分讲原则（带业界出处，偏教学），后半部分讲本项目怎么落地、测什么、不测什么。
>
> **不讲**：目录分层与「测试随源码同目录」的约定背景（→ [`02-directory-structure.md`](./02-directory-structure.md) §5.3）、各纯逻辑模块的机制本身。
>
> **锚点**：配置 `vitest.config.ts`；fixture 与 setup 在 `src/test/`；测试与源码同目录、命名 `*.test.ts`。

---

## 一、定位：这份文档回答四个问题

1. **为什么测？** —— 防止回归、逼出设计问题、充当可执行文档。
2. **测什么？** —— 纯逻辑（domain / services / flow 里的纯函数与类），**不测** UI 渲染、网络、Tauri IPC。
3. **怎么测？** —— vitest + jsdom，喂 fixture 进去，断言输出，**零业务 mock**。
4. **测什么、不测什么？** —— 见第五节。

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

### 2.2 覆盖范围

测试针对纯逻辑模块（按什么标准挑选见 §5.1）。**本指南不维护逐文件覆盖清单**——清单注定随重构漂移、需要人肉同步，性价比低；想知道现在测了哪些，直接 `Glob src/**/*.test.ts` 看实际文件即可。

> 例外：`flow/layout/layoutAsync` 是唯一带 `vi.mock` 的——它替换的是 elkjs `layout` 入口的**出站命令契约边界**，断言的是 `layoutAsync` 自身如何组织入参 / 处理错误 / 映射结果，不是 elkjs 内部行为，故不违反 §3.7「不 mock 你不拥有的东西」（mock 边界、不 mock 内部）。

### 2.3 这些测试做对了什么（值得学的范本）

读 `src/services/editingSession.test.ts` 和 `src/domain/schema.test.ts`，能看到本项目测试的几个高阶特征：

1. **行为命名，中文 `it`**：`it('早退：同 table/id 且内容相等时保留当前编辑态（不额外 bump）')`——失败信息自带语义，不用读用例体就知道断的什么。
2. **把隐式设计契约显式化为测试**：`describe('EditingSession 值类 vs 结构类（性能契约）')` 用一组用例固化一个隐式约定——值类操作（`updateNote`）就地改、**不 bump `structureVersion`、不通知 listeners**；结构类操作（`addArrayItem` 等）**bump 且通知一次**。这是性能关键设计，靠测试锁住，重构时一旦破坏立刻红。
3. **引用相等性当作契约来测**：`expect(s.getEditingObject()).toBe(editingRef)` 验证「早退路径不换引用」，把"就地变异 vs 新建对象"这种隐式设计**显式化**为可执行契约。
4. **DAMP fixture**：`makeRecord` / `ITEM` 这类极小工厂就近定义，每个 `it` 自给自足，读一个用例不需要上下文。

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
- `api/apiClient.ts` 里调 axios 是**出站命令** → 如果要测，应 mock axios 断言"发出了正确请求"；本项目当前不测这一层。
- 不要为了凑覆盖率去测第三方库的**出站查询**（例如别 mock antd 的 Form 再断言它内部状态）。

一句话：**测 public 行为，不测 private 实现。** 私有方法被测，是因为把它当成 public 入口喂数据；不是为了"覆盖私有方法"才去测它。

### 3.3 测纯逻辑，不测 UI/网络/IPC —— 本项目的核心取舍

cfgeditor 刻意把可测的**规则与状态**下沉到 `domain/` `services/` `flow/*.ts`（纯函数与纯类），让 UI 组件（`*.tsx`）尽量**薄**——只做"读状态 → 渲染 → 派发动作"。

带来的结论：
- ✅ 测 `domain/schema`、`services/editingSession`、`flow/layout/colors`：喂 fixture，断言输出，**零 mock**。
- ❌ 不测 `*.tsx` UI 组件（`flow/edit/` 下的 `EntityForm` 等编辑表单、`features/record/Record` 等）：它们只做渲染与派发，逻辑已下沉到纯模块。
- ❌ 不测 `api/apiClient.ts`（HTTP）、`res/readResInfosAsync.ts` / `res/summarizeResAsync.ts`（Tauri 文件 IPC）。`flow/layout/layoutAsync.ts` 的 elkjs 交互已用 mock 边界测（见 §2.2）。
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

本项目范例：
- **互斥不变量**（`entityModel.test.ts`）：三个 type guard（`isReadOnlyEntity` / `isEditableEntity` / `isCardEntity`）两两互斥——任意 entity 不可能同时属于两类。断言"互斥"比逐个枚举组合更紧凑、更难漏。
- **契约不变量**（`editingSession.test.ts`）：值类操作永远不 bump `structureVersion`、不通知；结构类操作永远 bump 且通知一次。无论怎么换数据，这条性质恒成立。

进阶：当不变量足够重要，可用 **property-based testing**（Haskell QuickCheck，Claessen & Hughes, 2000；JS 圈 `fast-check`）让框架自动生成大量随机输入。本项目目前用手写断言已够用；若 `embedding.ts`、`schema.tsx` 这类规则模块要加深覆盖，可引入 `fast-check`（务必用**固定 seed**，见 3.6）。

### 3.6 FIRST 原则（Robert C. Martin）

好的单元测试应当：

- **F**ast —— 毫秒级。本项目全量跑完应在秒级。慢了说明依赖了网络/真实 IO，重新审视。
- **I**ndependent —— 任一用例独立可跑，顺序无关。别让用例 B 依赖用例 A 的副作用。
- **R**epeatable —— 任意环境结果一致。`Math.random()` / `Date.now()` 是大敌——需要随机性时用**固定 seed 的伪随机源**，保证失败可复现、不偶发红。
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
| 随机性 | 需要随机时用**固定 seed** 的伪随机源；不要用裸 `Math.random()` / `Date.now()` |

---

## 五、测什么、不测什么

判定标准：**价值 / 成本**。价值看「逻辑复杂度 × 回归损失」，成本看「纯度 × 是否要 mock」。纯函数、零 mock、规则密集的模块优先。

### 5.1 该测的：纯逻辑模块

凡是「规则集中、容易被改坏、且不碰 UI/网络/IPC」的代码，都是单测的目标。按**类型维度**看，本项目里典型该测的有（举例，非穷举清单）：

- **规则密集的纯函数**：魔数与多分支集中的计算——配色、尺寸、计数/拼串这类。喂 fixture 断言输出即可。
- **带阈值的判定逻辑**：多条规则 + 两套阈值 + 边值的判定（如 `domain/embedding` 的 `canBeEmbeddedCheck`）——典型的"边值用例"温床。
- **状态机 / 编辑会话**：如 `services/editingSession` 的值类 vs 结构类 bump 契约、早退路径、引用相等性；以及 `undoStack` 这类栈式结构。
- **图构建与布局**：实体→节点/边转换、视口计算、引用表的深度/节点截断。
- **类型守卫与谓词**：如 type guard 的互斥不变量、前缀匹配边界。

可深化的方向（按需，无优先级）：
- `domain/schema.tsx` 还有 `getField` / `getImpl` / `getNextId` / `defaultValue` / `getAllDepStructs` 等纯函数，`schema.test.ts` 目前只覆盖一部分。
- `api/recordModel.ts` 等若有纯转换/规整函数（参照 `noteModel.notesToMap` 的测法）可补；纯类型定义不必测。
- 若 `embedding.ts` / `schema.tsx` 这类规则模块要追求更高置信，可引入 `fast-check` 做 property-based（见 §3.5）。

### 5.2 明确不测的（当前策略）

- 所有 `*.tsx` UI 组件（`flow/edit/` 下的 `EntityForm` 等编辑表单、`features/record/Record`、`features/table/Table`、`features/finder/Finder`、`features/setting/Setting` 下的 `.tsx`……）。注意 `features/setting/colorUtils.ts` 是纯 `.ts` 函数，已测。
- `api/apiClient.ts`（HTTP，需 mock axios——违反 3.7，留给手测/集成）。
- `res/readResInfosAsync.ts` / `res/summarizeResAsync.ts`（Tauri 文件 IPC，留给手测）。`flow/layout/layoutAsync.ts` 已用 mock 边界测（见 §2.2）。
- `store/store.ts` / `store/resso.ts`（全局状态库，跨组件耦合）。
- `domain/storageJson.ts`（quicktype **自动生成**，测它等于测生成器；应通过 `genJsonParser.bat` 保证正确性，而非单测）。
- `i18n.ts`（翻译表，靠目视 / 用时验证）。

---

## 六、反模式清单（不要这样做）

1. **为覆盖率而测**：追求 100% 行覆盖，写出 `expect(x).toBeDefined()` 这种空断言。覆盖率是**副产品**，不是目标。
2. **测实现细节**：断言私有变量、断言"某函数被调用了 N 次"（除非它是出站命令契约）。实现一重构测试就红，是脆性测试。
3. **mock 你不拥有的库的内部**：mock axios 的拦截器细节、mock antd 组件树。一旦猜错，测试给你**虚假的安全感**。
4. **真实随机 / 时间**：用裸 `Math.random()`、`new Date()` 驱动断言 → 偶发红、不可复现。需要随机性时用固定 seed 的伪随机源。
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

- 新增 / 删除测试文件后**无需同步清单**——本指南与 `CLAUDE.md` 都不维护逐文件覆盖清单（理由见 §2.2）。
- 改了纯逻辑模块的公共行为，先改/补对应 `*.test.ts`（FIRST 的 Timely）。
- 引入新的全局测试依赖（如 `fast-check`、RTL）前，先在本文件登记理由与边界。
- 本文描述的是**目标与原则**；若现实与本文冲突，优先修现实（把逻辑抽纯 / 修测试），其次才更新本文。

---

## 一句话速记

- **只测纯逻辑**（`domain` / `services` / `flow` 里的纯函数与纯类），**不测** UI/网络/Tauri IPC/全局 store——是架构使然，不是偷懒。
- **零业务 mock**：喂 fixture 断言输出；只在 `setup.ts` 放「让代码跑起来」的 shim，不 mock 你不拥有的库。
- **DAMP > DRY**：每个 `it` 自给自足、中文行为命名、一个 `it` 一个意图。
- **不变量优先**：把「恒成立」的性质写成断言，比枚举 case 更紧凑难漏。
- **文件随源码同目录 `*.test.ts`**；本指南不维护逐文件覆盖清单（注定漂移），想知道测了啥直接 `Glob src/**/*.test.ts`。
