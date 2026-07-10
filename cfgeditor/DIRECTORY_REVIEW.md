# cfgeditor 目录结构重构 · 待办（批次3、批次4）

> 本文档是 cfgeditor 目录结构 review 的**待优化部分**，供后续推进。
> **已落地**：批次1（零风险速胜：删 4 死代码、4 处 `.tsx→.ts`、storageJson/resso 注释头、`toggleFullScreen` 拆到 windowUtils、补 i18n en 缺失 key、修 CLAUDE.md）、批次2（全局核心错置：`schema`→`domain/schema.tsx`、`themeService`→`services/`）。
> **本文档保留**：批次3（反向/循环依赖治理）、批次4（命名）。
> **2026-07-10 修订**：批次3 原方案（迁 FixedPage 工厂到 domain）经代码核实会制造 `domain→store` 反向依赖，已否决，改最小治理（只动 `getId`），详见第二节。

---

## 一、待解决的依赖问题

### P1 · store 反向依赖 routes

`store/store.ts`（底层状态）import 上层 `routes/`：

- `getId`（`from "../routes/record/recordRefEntity"`）→ 用于 `makeFixedPage` 拼 label（store.ts:374）。`getId` 是一行纯字符串拼接（`table + "_" + id`），store 全仓仅此一处依赖 recordRefEntity。
- 原 `Schema` import（store.ts:5）随批次2 已迁 `domain/schema`，该边不再是"反向"（store→domain 合理）；P1 剩 `recordRefEntity` 这一条边。

> **关键**：治理对象**不是 nav hook**。`navTo`/`useLocationData`/`useCurPageRecordOrRecordRef` 等 nav hook 内部完全不用 `Schema`/`getId`，外迁消不掉 P1（且涉及 19 文件改 import，性价比低）。P1 真正根源是：
> - `getLastOpenIdByTable(schema, ...)`（store.ts:492，用 `Schema`）
> - `makeFixedPage`（store.ts:374，用 `getId`）

### P2 · store ↔ storage 循环

`store/store.ts` ↔ `store/storage.ts` 互引；持久化键集 `prefSelfKeySet`/`notSaveKeySet` 定义在 store.ts 却只被 storage 用。

---

## 二、批次3 · 反向/循环依赖治理（待做，中风险）

### P1 治理（最小方案：只动 `getId`）

P1 唯一反向边是 `getId`（store.ts:8 ← `routes/record/recordRefEntity`），用于 `makeFixedPage` 拼 page label；`getId` 是一行纯字符串拼接（`table + "_" + id`）。

**做法（首选）**：把 `makeFixedPage` 里 ``label: getId(curTableId, curId)``（store.ts:374）内联为 ``label: `${curTableId}_${curId}` ``，删 store.ts:8 import → **P1 消除**。零新文件、零外部迁移、影响面 1 行。
- 备选：若不愿 store 与 recordRefEntity 各留一份 `getId`，可新建无依赖 `domain/recordId.ts` 放之、两边同引；代价是改 recordRefEntity 及其引用方路径。按反过度设计偏好，**首选内联**。

> **❌ 否决原方案（迁 FixedPage 工厂到 domain）**：原计划把这组工厂迁 `domain/`，理由「它们天然依赖 Schema/recordRefEntity」**不成立**——实测其中 4 个函数直接读写 `store` 运行态实例：
> | 函数 | 访问 store 的证据 |
> |---|---|
> | `makeFixedPage`(372) | `const { recordRefIn, recordRefOutDepth, recordMaxNode, nodeShow } = store` |
> | `makeUnrefPage`(386) | `const { recordRefOutDepth, recordMaxNode, nodeShow } = store` |
> | `setFixedPagesConf`(403/412) | `store.dragPanel` / `store.pageConf` + `setPref` + `clearLayoutCache`(store 内部) |
> | `getLastOpenIdByTable`(493) | `const { history } = store` |
>
> 而 `domain/schema.tsx` 当前**完全不依赖 store**（纯领域层）。迁这些函数会**制造 `domain→store` 反向依赖**，把 P1 从 `store→routes` 搬到 `domain→store`——**搬家而非消除**，还污染 domain 纯净性；且要改 3 个外部 importer（CfgEditorApp.tsx / FixPages.tsx / TableList.tsx），domain 又得回头引 store → 形成 domain↔store 新循环。它们本质是「读 store 状态构造/校验固定页」，属 store 层，**留在 store.ts**。
> （`isFixedRefPage` / `isFixedUnrefPage` / `getFixedPage` 是纯函数，但当前 domain 无需求，单独迁移收益太小，不动。）

### P2 治理

把**纯字面量**键集 `prefSelfKeySet` / `notSaveKeySet`（store.ts:140/143）下沉到 storage.ts，消除 storage→store 反向边（storage.ts:3 现引 `getPrefKeySet` / `getPrefSelfKeySet`）。

- **`getPrefKeySet` 非纯，不能照搬**：它依赖模块级 `storeState`（`Object.keys(storeState)`，store.ts:148）。下沉须重构——store 初始化时把 keys 喂给 storage（如 `registerPrefKeys(Object.keys(storeState))`）或导出 key 列表，**不要无脑搬**。
- **不要下沉 `readStoreStateOnce`**：它写 `store.xxx`（resso 实例），下沉会让 storage 反向 import store，把 store→storage 单向**变成双向循环**，加剧 P2。

---

## 三、批次4 · 命名（可选）

`recordRefEntity.ts → recordRefUtils.ts`。

- **引用 6 文件**（5 生产 + 1 测试）：store.ts、Record.tsx、recordEditEntityCreator.ts、RecordRef.tsx、recordEntityCreator.ts、`recordRefEntity.test.ts`。改名须同步改 **test 的 import**。
- 理由：该文件是一组函数（`getId` / `getLabel` / `createRefs` / `createRefEntities`...），无类、不定义 Entity 类型，`Entity` 后缀与 `flow/entityModel` 的 `Entity` 核心类型混淆。
- **诚实重估**：稳定低频文件的命名整洁，软收益 + 低成本，维持「可选」。若 P1 走内联最小方案（不动 recordRefEntity），本项可独立择期做。
- 仅改名，未触及「recordRefEntity 被 store 跨层引用却置于 `routes/record/`」的位置错位——有意控成本（移位要动 6 处路径，ROI 不足）。

---

## 四、验证清单（每批落地后必跑）

项目零前端单测、无 CI，现有质量门只有 lint + build：

1. `pnpm run lint`（oxlint）
2. `pnpm run build`（tsc + vite build）
3. `pnpm tauri dev` 冒烟（**必须桌面端**，浏览器端无法验证 Tauri 文件 IO）：
   - 打开含 table + record + recordRef 的真实配置
   - 走一次：选中节点 → 编辑字段 → 保存 → 重连服务器 → 切换中英文
   - 验证 elk 布局 worker 正常（`layoutAsync.ts` 的 `elk-worker.min.js?url` 是包导入，不受影响）
4. 改名/移动后 grep 全仓确认无残留旧路径（含 `.oxlintrc.json` / `*.bat` / `CLAUDE.md`）

---

## 五、操作纪律与风险

### 纪律
- **纯移动/改名 PR 禁止夹带逻辑修改**——否则 reviewer 难辨，且 `git blame` 失真。
- 用 `git mv`（而非删旧建新）保护 blame 链；review 用 `git log --follow --find-renames` 验证。
- **import 扩展名约定**：全仓相对 import 几乎 100% 带 `.ts`/`.tsx` 后缀（`allowImportingTsExtensions: true`），改名/移动要同步改引用方扩展名——把"旧名.tsx → 新名.ts"作为整体替换单元。

### 外部路径耦合（移动文件时必须同步）
| 外部文件 | 硬编码的 src 路径 |
|---|---|
| `genJsonParser.bat` | `src/store/storageJson.ts`（输出 `-o`） |
| `.oxlintrc.json` ignorePatterns | `src/store/storageJson.ts` |
| `.oxlintrc.json` overrides | `src/store/resso.ts`、`src/main.tsx` |
| `CLAUDE.md`（cfgeditor） | 目录描述 |
| `main.tsx` | `import './i18n.js'`（.ts 用 .js 扩展名导入） |

### 高风险文件
- **`storageJson.ts`（自动生成）**：移动须同步改 `genJsonParser.bat` + 改 **13 个 importer** + 改 `.oxlintrc.json` ignorePatterns；建议把源 `json.ts` 移到生成产物旁。改后 `pnpm tauri build` 验证。
- **`resso.ts`（vendored）**：整文件覆盖式升级，别加业务代码；建议不动位置、仅注释头（批次1 已加）。
- **Tauri 打包（明示安全）**：`src-tauri/tauri.conf.json` 仅引用 `../dist`，对 src 内部移动无感。

---

## 附：已评估不做的项（避免重复提议）

| 项 | 理由 |
|---|---|
| 配 `@/` 路径别名 | 相对路径痛点小（`../../../` 全仓仅 7 处）；存量 285 处带扩展名 import 不自动受益，混用三风格 |
| `flow/node/` 子目录 | `getResBrief` 有簇外引用、`CustomAutoComplete↔EntityForm` 循环互引，"仅经 FlowNode 暴露"不成立；导航靠 IDE 搜索 |
| `i18n.ts` 拆 index/en/zh | 翻译增长慢；拆 3 文件改一个 key 要跳 2 文件反增摩擦（en 缺 key 已在批次1 修复） |
| `api/` 拆 `api/`+`models/` | 6 文件扁平已清晰，HTTP 与类型强相关放一起是惯例 |
| `store/` 子目录化 generated/vendor | 各 1 文件，建子目录过度；已用注释头替代（批次1） |
| 完整分层 routes/features/domain/services | 86 文件单应用四层分层是 over-engineering |
| 其他改名（`tableRefEntity`/`recordEntityCreator` 各 1 引用、`ResPopover→ResViewer`） | 成本 > 收益 |
| `recordRefEntity` 移位（`routes/record/` → domain/utils 等） | 被 store 跨层引用确是位置错位信号，但移位要动 6 处路径（含 test），纯结构收益软；先用内联消 P1（批次3）、改名消混淆（批次4），移位 ROI 不足 |
