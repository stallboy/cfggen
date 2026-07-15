# 前端目录结构：原则、落地与守门（cfgeditor）

> 面向 cfgeditor 的目录结构文档：通用原则 → 本项目落地 → 工程化护栏。
> 第一~三节是通用教学；第四~六节反映**当前已落地的实际结构**（治理记录见末尾）。

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

> **经验法则**：页面 < 5 个、一人开发 → 切法 A；页面多、多人协作、某业务已自成一摊 → 切法 B（或混合）。**大项目几乎都收敛到 B**，因为 type-based 在大型项目里必然崩塌。

---

## 二、主流框架/脚手架怎么规划

| 框架/方案 | 默认风格 | 关键约定 |
|---|---|---|
| **create-react-app / Vite 模板** | 扁平、切法 A | 自由度高，只给 `src/`，由你决定 |
| **Next.js (App Router)** | 路由优先 + 特性混合 | `app/` 目录即路由，业务逻辑另放 `features/` 或 `lib/` |
| **Remix** | 路由优先 | `routes/` 约定式路由，路由文件同时承载数据加载（loader）和 UI |
| **Angular** | 严格分层 + 模块化 | 强约定：`core/`（单例服务）、`shared/`（通用件）、`features/`（特性模块） |
| **Nuxt (Vue)** | 约定目录 | `pages/` `components/` `composables/` `layouts/` `stores/` 各司其职 |
| **Bulletproof React**（社区标杆） | feature-based + 严格导入 | `app/` `features/` `entities/` `shared/` `widgets/`，**禁止特性之间互相深引用** |

两个共识：
1. **路由目录 ≠ 业务逻辑目录**：Next/Remix 把"路由"和"业务特性"分开——路由层薄，业务下沉到 `features/` 或 `domain/`。
2. 分层骨架是地基，feature-based 是骨架之上的"特性自包含"封装。

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

**违反方向的味道**：低层 import 高层。一旦出现，说明被引用的东西"位置太低或太偏"，该上浮或抽到中立层。本项目靠 lint 自动守门（见第五节）。

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

## 四、cfgeditor 当前结构

以 type-based 分层为主，`flow/`/`res/`/`routes/` 是特性目录（混装，见下）。`domain/` 是核心纯层，收纳了所有跨层共享的模型/规则/契约类型。

```
src/
├── main.tsx             React 入口（挂 React + 路由 + queryClient）
├── app/                 应用层：主组件、Provider、布局壳、i18n（第三节的 app/ 落点）
│   ├── CfgEditorApp / AppLoader / SidePanelShell   主组件 / 加载器 / 分割面板壳
│   ├── headerbar/        顶栏布局件：HeaderBar + IdList / TableList / UnreferencedButton
│   ├── queryClient.ts      React Query 配置（queryClient / invalidateAllQueries）
│   └── i18n.ts / types.ts  国际化文案 / SchemaTableType 等全局类型
│
├── api/                 后端契约层（最底，不依赖任何上层）
│   ├── recordModel / schemaModel / searchModel / noteModel / chatModel   DTO 类型
│   └── apiClient.ts             HTTP 客户端
│
├── domain/              【纯领域层】核心模型 + 纯规则 + 跨层共享契约类型（无 UI / 无副作用 / 无全局状态）
│   ├── entityModel.ts     Entity / EntityEdit / EFitView / EditingObjectRes…
│   ├── schema.tsx         Schema 类 + getField/getImpl/getIdOptions…
│   ├── storageJson.ts     quicktype 生成的配置契约（NodeShowType/TauriConf/FixedPage/AIConf…）+ Convert
│   ├── resInfo.ts         ResInfo 资源类型
│   ├── embedding.ts(+.md) 内嵌判定（规则函数 + 阈值，原 checker+config 合并）+ 机制说明
│   ├── entityPredicates.ts 实体判定纯函数
│   ├── nodeShowLayoutKeys.ts 节点展示布局 key 派生
│   ├── historyModel.ts    导航历史模型（纯，原 store 下沉）
│   └── undoStack.ts       undo/redo 纯数据栈（Snapshot 快照栈，不依赖 React）
│
├── store/               全局状态（Resso）+ 持久化
│   ├── store.ts / storage.ts   Resso store + localStorage/YAML 持久化
│   └── resso.ts            【vendored】resso 库源码副本（升级整文件覆盖，oxlint 单独 override）
│
├── services/            【有状态/有副作用服务】
│   ├── editingSession.ts  编辑态单例（原 editingObject；方案 C 重构：useSyncExternalStore + UndoStack）
│   ├── clipboard.ts       剪贴板服务
│   ├── windowUtils.ts     Tauri 窗口封装（toggleFullScreen；原 utils/，按纯度挪此）
│   └── themeService.ts    主题文件读取
│
├── flow/                图形可视化特性（已内聚出 edit / layout 子系统）
│   ├── （根）FlowGraph/FlowNode/EntityCard/FlowContextMenu/NodeToolbar/ResPopover/EntityProperties/Highlight/NodeNote/NodeTitle/NoteShowOrEdit…  React Flow 渲染
│   ├── edit/              编辑表单子系统
│   │   ├── EntityForm.tsx / FieldRenderer.tsx
│   │   ├── fields/        各类型字段项（Primitive/Array/Interface/Func/StructRef…）
│   │   └── shared/        编辑共享件（CustomAutoComplete/fieldUtils/primitiveControl/types/hooks…）
│   ├── layout/            【纯布局计算】colors/dimensions/calcWidthHeight/entityToNodeAndEdge/viewportMath/layoutAsync/getDsLenAndDesc（各自带 .test）
│   ├── useEntityToGraph.ts / nodeAnchor.ts / devLog.ts   hook + 锚点 + dev 日志
│   └── __dev__/           开发期护栏（HeightDriftGuard）
│
├── routes/              路由/页面层（部分子目录已含子系统级业务逻辑，非纯薄壳）
│   ├── record/   Record / RecordRef + recordEntityCreator / recordEditEntityCreator / recordRefUtils（record 子系统，偏重）
│   ├── table/    Table / TableRef + tableEntityCreator / tableRefEntity
│   ├── finder/   Finder + NavList / RefIdList / LastAccessed / LastModified / SearchValue（原 search/）
│   ├── setting/  Setting + 多个子设置面板（Ai/Basic/Tauri/FixPages/FlowVis/NodeShow/Theme/Display/Connection/KeyShortcut/Tools）+ colorUtils
│   ├── add/      Chat / AddJson / AddPanel + useEditable（AI 录入）
│   └── PathNotFound.tsx
│
├── res/                 资源处理特性（findAllResInfos / readResInfosAsync / summarizeResAsync / getResBrief）
└── test/                测试 fixture + setup
```

### 各层一句话定位
- **app**：入口与全局装配。`main.tsx` 挂 React + 路由 + queryClient；`app/` 装主组件、Provider、分割面板壳、i18n。
- **api**：后端长什么样，这里就长什么样。纯类型 + 请求函数，不依赖任何上层。
- **domain**：项目的"心脏"。放"不管用什么 UI 框架、跑在哪"都成立的模型、规则、契约类型。**它越厚越健康**——可测、可复用、不依附 React。`schema.tsx`、`entityModel.ts`、`embedding`、`storageJson`（配置契约）、`resInfo`、`entityPredicates`、`nodeShowLayoutKeys`、`historyModel`、`undoStack` 都属此。
- **store / services**：管"变化"和"副作用"。`store/resso.ts` 是 vendored 的状态库源码（非项目代码）；`services/editingSession.ts` 持有编辑态可变单例（方案 C 后用 useSyncExternalStore 订阅 + UndoStack 存快照），`clipboard`/`windowUtils`/`themeService` 是典型服务。
- **flow**："图形可视化"特性。domain 之上、routes 之下的业务特性层；内部已分出 `edit/`（编辑表单子系统）与 `layout/`（纯布局计算，自带测试）。
- **routes**：页面，尽量薄——但 `record/`、`setting/` 等子目录已携带子系统级业务逻辑（creator/ref 工具），非纯薄壳。

### 特性目录 vs 层目录：flow / res 为什么不拆

项目里两类目录混着用，要分清：

| 目录 | 维度 | 说明 |
|---|---|---|
| `api/` `store/` `domain/` `utils/` | 按技术类型（**层**） | 全是同一类代码 |
| `flow/` `res/` `routes/` `services/` | 按业务子系统（**特性**） | 内部混装：类型 + 工具 + UI + 服务 + hook |

`flow/`（图形可视化）和 `res/`（资源处理）内部都是"一个子系统把它的类型/工具/UI/I/O 服务打包在一起"——feature-based 的内聚形态，**不要拆**。唯一原则：**跨特性共享的核心模型/契约类型**（如曾经的 `entityModel`、`getResBrief`、`storageJson`）不独属任一特性，下沉到 `domain/`。

### 依赖方向（已由 lint 强制，见第五节）
```
app/routes
   ↓
  flow ──→ res
   ↓         ↓
 store / services
   ↓
  domain
   ↓
   api
```
- `flow` 可 import `res`（如 `flow/ResPopover` 调 res）；`res` 不得反向 import flow。
- `flow` / `store` / `services` / `res` 均不得 import `routes`；`flow`/`store`/`services`/`res` 之间除 flow→res 外不横向依赖（`services`→`store` 有 1 处例外：`editingSession` 调 `setEditingState`）。
- `domain` 只能 import `api`；`api` 不 import 任何上层。
- `app` 是顶层（main 装配它），理论上谁都可 import；但 app 内只应放"装配级"件（主组件 / Provider / 壳 / i18n / 顶栏），**不得承载被下层复用的数据层件或契约类型**。曾经 `app/queryClient.ts`（queryClient / invalidateAllQueries）与 `app/types.ts`（SchemaTableType）被 flow/res/routes 向上 import，是 app 唯一向下泄漏的软边界；现已下沉（queryClient→`services/`、SchemaTableType→`domain/types.ts`）并由 lint 堵死（见 5.2）。

---

## 五、工程化护栏（已落地）

### 5.1 路径别名 `@/`
- `tsconfig.json`：**TS7 已移除 `baseUrl`**，用 `"paths": { "@/*": ["./src/*"] }`（值必须 `./` 相对）。
- `vite.config.ts` + `vitest.config.ts`：`resolve.alias`（ESM 用 `fileURLToPath(new URL('./src', import.meta.url))`）。三处都要配，缺一则对应环节报 "Could not resolve @/"。
- **约定**：跨目录 `../` → `@/`；同目录/子目录 `./` 保留（可读性好）。

### 5.2 依赖方向守门（oxlint）
oxlint **不支持** `import/no-restricted-paths`（zone 规则），但可用 ESLint core 的 `no-restricted-imports` 的 `patterns` + `overrides`（按 `files` 区分目录）模拟——每个目录配一条 override，禁其 import 的 `@/` 上层前缀。`.oxlintrc.json`：

```jsonc
"overrides": [
  {"files": ["src/api/**"],      "rules": {"no-restricted-imports": ["error", {"patterns": ["@/domain/**","@/store/**","@/flow/**","@/routes/**","@/res/**","@/services/**"]}]}},
  {"files": ["src/domain/**"],   "rules": {"no-restricted-imports": ["error", {"patterns": ["@/flow/**","@/routes/**","@/store/**","@/res/**","@/services/**"]}]}},
  {"files": ["src/flow/**"],     "rules": {"no-restricted-imports": ["error", {"patterns": ["@/routes/**"]}]}},
  {"files": ["src/store/**"],    "rules": {"no-restricted-imports": ["error", {"patterns": ["@/routes/**","@/flow/**"]}]}},
  {"files": ["src/res/**"],      "rules": {"no-restricted-imports": ["error", {"patterns": ["@/flow/**","@/routes/**"]}]}},
  {"files": ["src/services/**"], "rules": {"no-restricted-imports": ["error", {"patterns": ["@/routes/**","@/flow/**"]}]}}
]
```

要点：
- patterns 用 `@/X/**`（`*` 不跨目录段，单 `@/X/*` 会漏子目录）。
- 前提是跨目录 import 都走 `@/`（同目录 `./` 不被检查，但 `./` 本就不跨层）。
- 抓到违反 = 该类型位置错了。本项目正是因此把 `storageJson.ts`、`resInfo.ts`（跨层共享契约类型）下沉到 `domain/`，才让 domain 规则 0 违反。
- 生成文件（`domain/storageJson.ts`，quicktype 产出）要进 `ignorePatterns`，否则其内部 `no-unused-vars` 等会误报。
- 除 6 条方向规则外，`.oxlintrc.json` 另有两个**非方向**的特例 override：`src/store/resso.ts` 关 `react-hooks/rules-of-hooks`（vendored 库源码，hooks 调用模式不合规则）；`src/main.tsx` 关 `react/only-export-components`（入口文件允许导出非组件）。这两个是"既知特殊文件"的豁免，不涉及方向。
- **app 边界已守门**：原本 6 条规则都没禁 `@/app/**`——app 被当作"顶层谁都可 import"，导致 flow/res/routes 曾向上 import `@/app/queryClient` 与 `@/app/types`（`SchemaTableType`）。现已先下沉（`queryClient`→`services/queryClient.ts`，`SchemaTableType`→`domain/types.ts`），再给 flow/res/store/services 补 `@/app/**` 禁令堵死回归。（api/domain 暂未加 `@/app/**`——二者当前无 @/app 引用，需要时一并补即可。）

从此任何反向 `import`，`pnpm lint` 立即红——靠 lint 守门，不靠人 review。

### 5.3 测试随源码
`*.test.ts` 与源码同目录（vitest），纯逻辑测试天然落 `domain/`，强化"domain 是纯的、可测的"。

### 5.4 barrel 文件谨慎用
`index.ts` 统一导出会让"谁用了谁"变模糊、拖慢构建。小项目按需直接 import 具体文件更好。

---

## 六、治理记录与后续演进

目录重构最忌"大爆炸重排"。本项目按小步走，每步可验证。**已完成**（master 上的提交）：

1. ✅ **纯规则下沉**：`embedding` → `domain/`
2. ✅ **核心模型归位**：`entityModel.ts` → `domain/`
3. ✅ **消除反向依赖**：`EFitView`/`EditingObjectRes` 抽到中立层（解开 flow→routes）；`getResBrief` 下沉 res（解开 res→flow）
4. ✅ **路径别名**：`@/`（`../`→`@/`，`./` 保留），tsconfig/vite/vitest 三处
5. ✅ **lint 方向守门**：oxlint 6 方向规则 + `storageJson`/`resInfo` 契约类型下沉 domain
6. ✅ **domain 扁平化 + services 上浮**：`Folds` 下沉 `domain/folds.ts`（纯数据结构，决策逻辑留上层）；`embeddingChecker`+`embeddingConfig` 合并成 `domain/embedding.ts`（消除子目录 + 命名）；`editingObject` → `services/`（状态服务归位）；`flow/embedded/` 目录消失

> 下列为近一轮（方案 C 编辑态重构 + 纯逻辑继续下沉）的变更：

7. ✅ **editingObject → editingSession（方案 C 重构）**：render 期变异 `editState` 的反模式根治——引入 `EditingSession`（`useSyncExternalStore` 外部 store，外部订阅读写分离）+ `UndoStack`（纯快照栈，下沉 `domain/undoStack.ts`）。**注意**：`editingObject` 的就地变异保留为有意设计（见 reducer 注释），勿当反模式回改。
8. ✅ **historyModel 下沉 domain**：导航历史模型从 `store/` 下沉纯层，可独立单测（`domain/historyModel.test.ts`）。
9. ✅ **Folds 移除**：fold 状态不再独立 React state、`domain/folds.ts` 删除，改由 `$fold` 派生（undo/redo 恢复 `$fold` 即恢复 fold；见 `record/recordEditEntityCreator.ts` 注释）。→ 第 6 条中的 `domain/folds.ts` 已成历史。
10. ✅ **纯判定/派生继续下沉 domain**：`entityPredicates`、`nodeShowLayoutKeys` 下沉纯层（均可单测）。
11. ✅ **search → finder 重命名**：路由目录 `routes/search/` → `routes/finder/`，语义校准——该目录承载的是查找/导航面板（最近访问、最近修改、引用列表、搜索值），非全文搜索。
12. ✅ **命名/位置纯度修正**：① `domain/undoStore` → `undoStack`（消除纯层的 `Store` 命名误导——它是纯数据栈、非全局可变状态；类 `UndoStore` → `UndoStack`）；② `utils/windowUtils` → `services/`（Tauri 窗口封装有副作用，按第三节纯度判定不属"通用纯工具"）。挪后 `utils/` 目录已整个删除（暂无通用纯工具需要它，待真有再建）。
13. ✅ **app 边界守门（堵软边界）**：`app/queryClient.ts` → `services/queryClient.ts`、`app/types.ts`（`SchemaTableType`）→ `domain/types.ts`，并给 flow/res/store/services 补 `@/app/**` 方向规则——app 不再向下泄漏数据层件 / 契约类型，下层对 app 的反向依赖由 lint 拦截。


---

## 一页速记

- **两种切法**：type-based（小）vs feature-based（大），本项目是分层型 + flow/res/routes 特性目录。
- **一条铁律**：依赖只能向下；低层 import 高层 = 该重构的信号（本项目靠 lint 自动拦截）。
- **一个判据**：纯 → 下沉 domain；有状态/副作用 → 抬 store/services；有 UI → 上层。
- **两个护栏**：路径别名 `@/`（治深路径）+ oxlint 方向规则（治依赖方向）。
- **domain 是心脏**：跨层共享的模型/规则/契约类型（entityModel/schema/embedding/storageJson/resInfo/entityPredicates/nodeShowLayoutKeys/historyModel/undoStack）都沉这，越厚越健康。
