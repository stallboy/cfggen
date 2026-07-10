# 前端目录结构规划：从原则到落地

> 面向 cfgeditor 项目的目录结构教学文档：从通用原则，到主流框架实践，再到本项目的具体落地。

---

## 一、本质问题：目录是按"什么"切？

所有目录方案都在回答一个问题：**代码按什么维度归类？** 只有两种基本切法，所有框架的实践都是它们的变体或组合。

### 切法 A：按技术类型分层（type-based / layered）
按"它是什么类型的代码"分：组件放 `components/`，hook 放 `hooks/`，工具放 `utils/`，接口放 `api/`。

```
src/
├── components/
├── hooks/
├── utils/
├── api/
└── store/
```
- **优点**：心智简单，上手快，小项目绝佳。
- **缺点**：项目一大，改一个功能要在 5 个目录间跳来跳去，"这个功能的代码散落各处"。

### 切法 B：按业务特性分模块（feature-based / domain-driven）
按"它属于哪个业务领域"分，每个特性**自包含**（自己的组件、hook、api、类型、甚至 store 切片）。

```
src/
├── features/
│   ├── auth/          # 登录特性的全部代码都在这
│   │   ├── components/
│   │   ├── api.ts
│   │   ├── hooks.ts
│   │   └── types.ts
│   └── record/
└── shared/            # 跨特性的通用件
```
- **优点**：高内聚，删一个特性只删一个文件夹；团队按特性分工不冲突。
- **缺点**：前期要想清楚边界；小项目用它是过度设计。

> **经验法则**：页面 < 5 个、一人开发 → 切法 A；页面多、多人协作、某业务已自成一摊 → 切法 B（或混合）。

---

## 二、主流框架/脚手架怎么规划

| 框架/方案 | 默认风格 | 关键约定 |
|---|---|---|
| **create-react-app / Vite 模板** | 扁平、切法 A | 自由度高，只给 `src/`，由你决定 |
| **Next.js (App Router)** | 路由优先 + 特性混合 | `app/` 目录即路由（`page.tsx`/`layout.tsx`/`loading.tsx`），业务逻辑另放 `features/` 或 `lib/` |
| **Remix** | 路由优先 | `routes/` 约定式路由，路由文件同时承载数据加载（loader）和 UI |
| **Angular** | 严格分层 + 模块化 | 强约定：`core/`（单例服务）、`shared/`（通用件）、`features/`（特性模块），依赖注入分明 |
| **Nuxt (Vue)** | 约定目录 | `pages/` `components/` `composables/` `layouts/` `stores/` 各司其职 |
| **Bulletproof React**（社区标杆指南） | feature-based + 严格导入规则 | `app/` `features/` `entities/` `shared/` `widgets/`，**禁止特性之间互相深引用** |

两个共识值得记住：
1. **路由目录 ≠ 业务逻辑目录**：Next/Remix 把"路由"和"业务特性"分开——路由层薄，业务下沉到 `features/` 或 `domain/`。
2. **大项目几乎都收敛到 feature-based**：因为 type-based 在大型项目里必然崩塌。

---

## 三、分层的灵魂：依赖方向 + 纯度

无论怎么切，**层的依赖只能向下，不能反向**。这是目录规划的底层物理定律：

```
        app/              ← 入口、Provider、路由装配
         ↓
      routes/             ← 页面（薄壳，组装下面的）
         ↓
   features / flow        ← 业务特性 / 可视化
         ↓
  store / services        ← 状态、有副作用的服务
         ↓
      domain/             ← 纯领域模型 + 纯规则（无 UI、无副作用）
         ↓
       api/               ← 后端数据契约（DTO）+ HTTP 客户端
         ↓
    shared / utils        ← 通用纯工具
```

**违反方向的味道**：低层 import 高层。比如 `flow/` 里的文件去 `import` `routes/record/` 的东西——这就是本项目中 `useEntityToGraph.ts` 反向 import `routes/record/editingObject.ts` 的问题。一旦出现，说明被引用的东西"位置太低或太偏"，该上浮或抽到中立层。

### 纯度判定（决定一个文件归哪层）

| 问自己 | 归属 |
|---|---|
| 它是后端返回的数据形状/DTO 吗？ | `api/` |
| 它是**纯类型 + 纯函数 + 纯业务规则**，无 UI、无全局可变状态、无网络/存储副作用吗？ | `domain/` |
| 它持有**全局可变状态**或调用存储/通知吗？ | `store/` 或 `services/` |
| 它渲染 UI（JSX/组件）吗？ | `routes/` / `flow/` / `components/` |
| 它和具体业务无关、到处能复用吗？ | `shared/` / `utils/` |

> 一句话：**越"纯"、越"通用"的，越往下沉；越"有状态"、越"靠近用户"的，越往上浮。**

---

## 四、cfgeditor 现状诊断

本项目是**混合型**——以 type-based 分层为主（`api/flow/store/utils`），但已经开始有了 `domain/` 这层领域抽象。当前结构方向是对的，问题集中在**归位不准 + 依赖倒置**：

| 文件 | 现位置 | 问题 | 正确归属 |
|---|---|---|---|
| `entityModel.ts` | `flow/` | 是全项目共享的**核心模型**，却被埋在"图形可视化"子目录下，导致到处 `../flow/entityModel` 反查 | `domain/` |
| `embeddingChecker/Config` | `routes/record/embedding/` | 是**纯领域规则**（5 条内嵌判定），却挂在"record 路由"下 | `domain/embedding/` |
| `schema.tsx` | `domain/` | ✅ 位置正确（基于 schema 的纯领域计算） | 保持 |
| `editingObject.ts` | `routes/record/` | 是**有状态服务层**（持有 `editState` 单例 + 副作用 + 耦合 store），却被 `flow/` 反向引用 | 不该进 domain，应去 `services/` 或留原位但抽类型 |

诊断结论：**分层骨架已经搭起来了，缺的是"把核心模型/规则沉到 domain，把有状态的东西抬到 services/store"。**

---

## 五、cfgeditor 的目标结构

推荐保持**分层为主**（当前规模还没到必须 feature-based 的程度），把归位修顺：

```
src/
├── app/                  # (可选) 入口装配：main.tsx、Provider、全局路由
├── api/                  # 后端契约层（最底层，无任何业务依赖）
│   ├── recordModel.ts    #   DTO 类型
│   ├── schemaModel.ts
│   └── api.ts            #   HTTP 客户端
│
├── domain/               # 【纯领域层】核心模型 + 纯规则，无 UI / 无副作用 / 无全局状态
│   ├── entityModel.ts    #   ← 从 flow/ 搬来：Entity / EntityEdit / 类型守卫
│   ├── schema.tsx        #   Schema 类 + getField/getImpl…
│   └── embedding/        #   ← 从 routes/record/ 搬来：内嵌判定规则
│       ├── embeddingChecker.ts
│       ├── embeddingConfig.ts
│       └── README.md
│
├── store/                # 全局状态层（Resso）+ 持久化 + history
├── services/             # 【有状态/有副作用的服务】← editingObject 这类放这
│                         #   （或暂留 routes/record，但先消除 flow→routes 反向依赖）
│
├── flow/                 # 图形可视化层（React Flow 渲染 + 布局计算）
│   ├── FlowNode.tsx      #   纯 UI 组件，import ../domain/entityModel
│   ├── EntityCard.tsx
│   ├── colors.ts         #   纯计算
│   └── useEntityToGraph.ts
│
├── routes/               # 路由/页面层（薄壳，组装 flow + services + domain）
│   ├── record/
│   │   ├── Record.tsx
│   │   └── recordEditEntityCreator.ts
│   └── table/
│
├── shared/  (或 utils/)  # 跨业务通用纯工具
├── res/                  # 资源
└── test/                 # 测试 fixture（测试文件本身随源码同目录）
```

### 各层一句话定位
- **api**：后端长什么样，这里就长什么样。纯类型 + 请求函数，不依赖任何上层。
- **domain**：项目的"心脏"。放那些"不管用什么 UI 框架、不管跑在哪"都成立的模型和规则。**它越厚，项目越健康**——因为可测、可复用、不依附 React。`schema.tsx`、`entityModel.ts`、`embeddingChecker` 都属此。
- **store / services**：管"变化"和"副作用"。`editingObject.ts` 这种持有可变单例、调 `setEditingState` 的，本质是服务，不是领域。
- **flow**：可以理解成"图形可视化"这个**特性**。它是 domain 之上、routes 之下的一个业务特性层。
- **routes**：页面。应当尽量薄——只做"取数据 + 调 service + 渲染 flow 组件"。

### 特性目录 vs 层目录：flow / res 为什么不动

项目里有两类目录混着用，要分清：

| 目录 | 维度 | 说明 |
|---|---|---|
| `api/` `store/` `utils/` `domain` | 按技术类型（**层**） | 全是同一类代码 |
| `flow/` `res/` `routes/` | 按业务子系统（**特性**） | 内部混装：类型 + 工具 + UI + 服务 + hook |

`flow/`（图形可视化子系统）和 `res/`（资源处理子系统）内部都是"一个子系统把它的类型 / 工具 / UI / I/O 服务打包在一起"——这正是 feature-based 的内聚形态，**不要拆**。

- **flow**：作为特性目录是合理的。唯一错位的是 `entityModel.ts`——它是**跨所有特性共享的核心模型**，不该独属 flow 这一个特性 → 搬 `domain/`。其余（FlowNode / EntityCard / colors / calcWidthHeight / entityToNodeAndEdge / useEntityToGraph / embedded…）留在 flow，拆了反而破坏内聚。
- **res**：内聚得不错，保持原位。`resInfo.ts` 虽是纯类型，但它是**前端自己构造的**（`findAllResInfos` 产出），不是后端 DTO，不归 `api/`，跟着 res 放合理。I/O 服务（`readResInfosAsync` / `summarizeResAsync`）是这个特性的副作用操作，跟特性走是对的。

> **可顺手清理**：`res/summarizeResAsync.ts` import 了 `flow/getResBrief.ts`（res→flow 跨特性引用）。`getResBrief` 是被两个特性共用的纯函数，理想情况下沉到 `shared/` 或 `domain/`。小问题，不急。

### 依赖方向红线
```
routes → flow → services/store → domain → api
                                   ↑
              （flow 不得 import routes；services 不得 import routes）
```
现在的 `useEntityToGraph.ts`（flow）import `routes/record/editingObject` 就是踩了红线——解法是把 `EFitView/EditingObjectRes` 这种纯类型抽到中立层。

---

## 六、工程化配套（让结构不靠人记）

1. **路径别名**：在 `tsconfig.json` 配 `paths`，用 `@/domain/entityModel` 代替 `../../../domain/entityModel`，深路径一扫而光，搬迁时也只需改一处别名指向。
   ```jsonc
   "paths": { "@/*": ["src/*"] }
   ```
2. **ESLint `no-restricted-imports` / `import/no-restricted-paths`**：用规则**强制**依赖方向——例如禁止 `domain/**` import `react`、禁止 `flow/**` import `routes/**`。靠 lint 守门，而不是靠人 review。
3. **测试随源码**：本项目已经在做（`*.test.ts` 同目录），符合 vitest 社区主流，继续保持。纯逻辑测试天然落在 `domain/`，正好强化"domain 是纯的、可测的"。
4. **barrel 文件谨慎用**：`index.ts` 统一导出会让"谁用了谁"变模糊、拖慢构建。小项目按需直接 import 具体文件更好。

---

## 七、演进路径（别一步到位）

目录重构最忌讳"大爆炸重排"。建议小步走，每步都可验证：

1. **先把最纯的沉下去**：`embedding` → `domain/embedding/`（零风险，纯规则）。
2. **核心模型归位**：`entityModel.ts` → `domain/`（改一批 import，tsc 兜底）。
3. **抽类型消反向依赖**：把 `EFitView/EditingObjectRes` 抽到中立位置，解开 `flow→routes`。
4. **状态层上浮**：`editingObject.ts` → `services/`（等 record 相关逻辑稳定后再做）。
5. **未来**：当 `record` 的 editing 逻辑（editingObject + recordEditEntityCreator + Record.tsx）膨胀到自成体系，再考虑把它们一起收进 `features/record/` 自包含——那时才真正进入 feature-based。

---

## 一页速记

- **两种切法**：type-based（小项目） vs feature-based（大项目），本项目现在是分层型。
- **一条铁律**：依赖只能向下；出现低层 import 高层 = 该重构的信号。
- **一个判据**：纯→下沉 domain；有状态/副作用→抬到 store/services；有 UI→上层。
- **核心动作**：把 `entityModel`、`embedding` 这类"核心纯模型/规则"沉到 `domain/`；把 `editingObject` 这类"有状态服务"识别出来别让它污染 domain。
- **两个护栏**：路径别名（治深路径）+ ESLint 路径规则（治依赖方向）。
