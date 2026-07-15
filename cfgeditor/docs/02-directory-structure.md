# 目录结构：代码按什么分层、依赖只能向下怎么守

> 这篇讲 cfgeditor 的**目录怎么切、依赖方向怎么管**：先讲通用的分层原则（type-based vs feature-based、依赖只能向下、纯度判定），再落到本项目的实际结构，最后讲 oxlint 怎么自动拦反向 import。
>
> **不讲**：cfgeditor 是什么、核心概念（→ [`01-overview.md`](./01-overview.md)）、各层里的具体机制（状态 → [`04-state-management.md`](./04-state-management.md)、数据流 → [`05-url-api-reactquery.md`](./05-url-api-reactquery.md)）。本文只管「东西该放哪、能不能 import」。
>
> **读法**：第一~三节通用，第四、五节是 cfgeditor 当前实际结构。改代码前先读这篇，知道文件该落哪层。

---

## 一、本质问题：目录按「什么」切

所有目录方案都在回答一个问题：**代码按什么维度归类？** 两种基本切法：

- **切法 A — 按技术类型分层（type-based）**：组件 `components/`、hook `hooks/`、工具 `utils/`、接口 `api/`。心智简单、小项目绝佳；项目一大，改一个功能要在多个目录间跳。
- **切法 B — 按业务特性分模块（feature-based）**：每个特性自包含（自己的组件 / hook / api / 类型）。大项目几乎都收敛到 B。

> 经验法则：页面少、一人开发 → A；页面多、多人协作 → B（或混合）。

---

## 二、主流框架怎么规划

| 框架 | 默认风格 | 关键约定 |
|---|---|---|
| CRA / Vite 模板 | 扁平、切法 A | 只给 `src/`，自由发挥 |
| Next.js (App Router) | 路由优先 + 特性混合 | `app/` 即路由，业务下沉 `features/` / `lib/` |
| Remix | 路由优先 | `routes/` 约定式路由，兼承 loader 与 UI |
| Angular | 严格分层 | `core/` `shared/` `features/` 强约定 |
| Bulletproof React（标杆） | feature-based + 严格导入 | `app/` `features/` `entities/` `shared/` `widgets/`，禁止特性间深引用 |

共识：**路由目录 ≠ 业务逻辑目录**——路由层薄，业务下沉到 `features/` 或 `domain/`。

---

## 三、分层的灵魂：依赖方向 + 纯度

**层的依赖只能向下，不能反向**——这是目录规划的底层物理定律：

```
        app/              ← 入口、Provider、路由装配
         ↓
   features → flow        ← 业务特性 / 可视化
         ↓
  store / services        ← 状态、有副作用的服务
         ↓
      domain/             ← 纯领域模型 + 纯规则
         ↓
       api/               ← 后端契约（DTO）+ HTTP 客户端
```

低层 import 高层 = 该重构的信号（本项目靠 lint 自动守门，见第五节）。

### 纯度判定（决定一个文件归哪层）

| 它是什么 | 归属 |
|---|---|
| 后端返回的数据形状 / DTO | `api/` |
| 纯类型 + 纯函数 + 纯规则（无 UI、无副作用、无全局状态） | `domain/` |
| 持有全局可变状态 / 调存储通知 | `store/` 或 `services/` |
| 渲染 UI | `features/` / `flow/` |
| 与业务无关、到处能复用 | `shared/` / `utils/` |

> 越「纯」越往下沉，越「有状态 / 靠近用户」越往上浮。

---

## 四、cfgeditor 当前结构

以分层骨架为主，`flow/`、`res/`、`features/` 是 feature-based 的特性目录（内部混装类型 / 工具 / UI / 服务）。`domain/` 是核心纯层。

```
src/
├── main.tsx        React 入口（挂 React + 路由 + queryClient）
├── app/            应用装配：主组件 / Provider / 分割面板壳 / i18n / 404 兜底
├── api/            后端契约层（DTO + HTTP 客户端）；最底，不依赖上层
├── domain/         纯领域层：模型 / 规则 / 跨层共享契约类型（无 UI、无副作用）
├── store/          全局状态（Resso）+ 持久化；resso.ts 是 vendored 库源码
├── services/       有状态 / 副作用服务：editingSession / queryClient / clipboard / windowUtils / themeService
├── flow/           图形可视化特性：根目录渲染件 + edit/（编辑表单）+ layout/（纯布局计算，自带测试）
├── features/       特性页：record / table / finder / setting / add / headerbar（路由在 main.tsx 装配）
├── res/            资源处理特性
└── test/           测试 fixture + setup
```

**层目录 vs 特性目录**：`api/`、`store/`、`domain/` 是按技术类型归类的「层」；`flow/`、`res/`、`features/`、`services/` 是按业务子系统归类的「特性」（内部类型 + 工具 + UI + 服务 + hook 混装，feature-based 的内聚形态，不拆）。

**依赖方向**（lint 强制，见第五节）：

```
app/features
   ↓
  flow ──→ res
   ↓         ↓
 store / services
   ↓
  domain
   ↓
  api
```
- `flow` 可 import `res`；`res` 不得反向 import `flow`。
- `flow` / `store` / `services` / `res` 不得 import `features`；彼此除 `flow→res` 外不横向依赖。
- `domain` 只 import `api`；`app` 只放装配级件，不承载被下层复用的数据层件或契约类型。

---

## 五、工程化护栏

### 5.1 路径别名 `@/`

跨目录 import 用 `@/`（同目录 `./` 保留）。`tsconfig.json` 的 `paths` + `vite.config.ts` / `vitest.config.ts` 的 `resolve.alias` 三处都要配，缺一则对应环节报 "Could not resolve @/"。

> 用 TS7，`paths` 不能配 `baseUrl`、值须 `./` 相对；改别名三处一起改。

### 5.2 依赖方向守门（oxlint）

`.oxlintrc.json` 用 `no-restricted-imports` 的 `patterns` + `overrides`（按 `files` 区分目录），给每层禁掉它不应 import 的 `@/` 上层前缀——任何反向 import 立即红。当前规则（以 `.oxlintrc.json` 为准）：

| 目录 | 不得 import |
|---|---|
| `api/` | `@/domain` `@/store` `@/services` `@/flow` `@/res` `@/features` |
| `domain/` | `@/store` `@/services` `@/flow` `@/res` `@/features` |
| `flow/` | `@/app` `@/features` |
| `store/` | `@/app` `@/features` `@/flow` |
| `services/` | `@/app` `@/features` `@/flow` `@/store` |
| `res/` | `@/app` `@/flow` `@/features` |

特例：`store/resso.ts`（vendored 库源码）关 `rules-of-hooks`；`main.tsx` 关 `only-export-components`；`domain/storageJson.ts`（quicktype 产出）进 `ignorePatterns`。

### 5.3 测试随源码

`*.test.ts` 与源码同目录（vitest），纯逻辑测试天然落 `domain/`。测试怎么写、测什么不测什么，见 [`09-unit-testing-guide.md`](./09-unit-testing-guide.md)。

---

## 一页速记

- **两种切法**：type-based（小） vs feature-based（大）；本项目 = 分层骨架 + flow/res/features 特性目录。
- **一条铁律**：依赖只能向下，反向 import 靠 lint 拦截。
- **一个判据**：纯 → 下沉 `domain`；有状态 / 副作用 → `store` / `services`；有 UI → 上层。
- **两个护栏**：`@/` 别名（治深路径）+ oxlint 方向规则（治依赖方向）。
- **domain 是心脏**：跨层共享的模型 / 规则 / 契约类型都沉这，越厚越健康。
