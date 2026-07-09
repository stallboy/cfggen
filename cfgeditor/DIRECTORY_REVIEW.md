# cfgeditor 目录结构重构 · 待办（批次3、批次4）

> 本文档是 cfgeditor 目录结构 review 的**待优化部分**，供后续推进。
> **已落地**：批次1（零风险速胜：删 4 死代码、4 处 `.tsx→.ts`、storageJson/resso 注释头、`toggleFullScreen` 拆到 windowUtils、补 i18n en 缺失 key、修 CLAUDE.md）、批次2（全局核心错置：`schema`→`domain/schema.tsx`、`themeService`→`services/`）。
> **本文档保留**：批次3（反向/循环依赖治理）、批次4（命名）。

---

## 一、待解决的依赖问题

### P1 · store 反向依赖 routes

`store/store.ts`（底层状态）import 上层 `routes/`：

- `getId`（`from "../routes/record/recordRefEntity"`）→ 用于 `makeFixedPage`（store.ts:374）
- 原 `Schema` import（store.ts:5）随批次2 已迁 `domain/schema`，该边不再是"反向"（store→domain 合理）；P1 剩 `recordRefEntity` 这条边。

> **关键**：治理对象**不是 nav hook**。`navTo`/`useLocationData`/`useCurPageRecordOrRecordRef` 等 nav hook 内部完全不用 `Schema`/`getId`，外迁消不掉 P1（且涉及 19 文件改 import，性价比低）。P1 真正根源是：
> - `getLastOpenIdByTable(schema, ...)`（store.ts:492，用 `Schema`）
> - `makeFixedPage`（store.ts:374，用 `getId`）

### P2 · store ↔ storage 循环

`store/store.ts` ↔ `store/storage.ts` 互引；持久化键集 `prefSelfKeySet`/`notSaveKeySet` 定义在 store.ts 却只被 storage 用。

---

## 二、批次3 · 反向/循环依赖治理（待做，中风险）

### P1 治理

把 `getLastOpenIdByTable` 与 FixedPage 工厂（`makeFixedPage`/`makeUnrefPage`/`isFixedRefPage`/`isFixedUnrefPage`/`setFixedPagesConf`/`getFixedPage`）从 `store.ts` 迁到 `domain/`（它们天然依赖 `Schema`/`recordRefEntity`，与批次2 迁过去的 `domain/schema` 同区）。迁后 `store.ts` 不再 import routes，**P1 消除**。

### P2 治理

仅把**纯函数**键集 `prefSelfKeySet`/`notSaveKeySet`/`getPrefKeySet`/`getPrefSelfKeySet` 从 `store.ts` 下沉到 `storage.ts` → 消除 P2 成因。

- **不要下沉 `readStoreStateOnce`**：它访问 `store`(resso 实例)，下沉会让 `storage.ts` 反向 import `store`，把 store→storage 单向**变成双向循环**，加剧 P2。

---

## 三、批次4 · 命名（可选，建议与 P1 同批）

`recordRefEntity.ts → recordRefUtils.ts`（5 引用，含 store.ts）。

该文件是一组函数（`getId`/`getLabel`/`createRefs`/`createRefEntities`...），无类、不定义 Entity 类型，`Entity` 后缀与 `flow/entityModel` 的 `Entity` 核心类型混淆。与 P1 治理同批做（都动 store.ts/recordRefEntity）边际成本低。

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
