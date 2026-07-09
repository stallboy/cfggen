# cfgeditor 目录结构 Review 与重构建议

> 审查日期：2026-07-10（v2，多视角交叉审查修订版）
> 审查范围：`cfgeditor/src/` 全部 86 个 TS/TSX 文件（约 11,796 行）
> 审查方法：3 个并行子代理分块深读（flow / routes / store+api+res+根）产出 v1 → **4 个独立批判视角交叉审查 v1**（事实核查 / 成本可行性 / 必要性 YAGNI / 遗漏角度）→ 据此修订为本 v2。
> 本报告聚焦**目录结构层面**的归类、分层、命名问题，不重复 `CODE_REVIEW.md` 已覆盖的文件内 bug。

---

## 〇、v2 修订说明（v1 的主要错误与收敛）

v1 经 4 视角交叉核实，发现 **5 处事实错误** 与 **一批过度建议**，本版已修正：

| v1 错误 | 真相（源码核实） | 修正 |
|---|---|---|
| 建议 `schemaUtil.tsx → schema.ts`（去 .tsx） | **文件含 JSX**（`getIdOptions` 425 行 `<Flex>`、`getIdOptionsWithNew` 445 行 `<>➕ new</>`） | 迁移目标保留 `.tsx`；且须先拆这两个 JSX 函数 |
| P3 依赖图把 `themeService` 列为 `CfgEditorApp` 的 import | `themeService` 实由 `main.tsx:22` import，非 CfgEditorApp | 依赖图修正（v1 内部前后矛盾） |
| `promptStorage.ts` 是"错置需迁移" | `savePromptAsync` **全仓零调用** → 死代码 | 改为删除，并入死代码清理 |
| nav hook 外迁可消 P1（反向依赖） | nav hook 内部**不用** Schema/getId；P1 真正根源是 `getLastOpenIdByTable(schema)` 与 `makeFixedPage`(用 getId) | P1 治理对象改为这两个函数 |
| 持久化键集下沉 storage.ts「零风险消 P2」 | `readStoreStateOnce` 访问 `store`(resso 实例)，下沉会从 store→storage **单向变双向循环**，加剧 P2 | 仅纯函数 `getPrefKeySet/getPrefSelfKeySet` 可安全下沉 |

**额外发现 2 处死代码**（v1 未识别）：`flow/embedded/typeGuards.ts`（146 行，零引用）、`search/promptStorage.ts`（见上）。

**收敛原则**：v1 把「1 个真错置(schemaUtil) + 几个死代码 + 1 个用户可见 bug」包装成「四层分层 + 10 批次 + 别名前置 + 全面子目录化」的大重构。本版按 YAGNI 砍掉低收益/洁癖项（@/ 别名降级、flow/node 拆分、i18n 三拆、api 拆 models、完整分层、多数改名），收敛到 **5 件真正值得做的事**。

---

## 一、总体结论

代码质量基础好——判别联合 + 类型守卫、React Query/memo 规范、Creator 模式职责清晰。**真正值得动的结构问题只有 5 类**：

1. **全局核心错置**：`schemaUtil`（全局 `Schema` 类，被 27 个外部文件引用）埋在 `routes/table/`。
2. **反向依赖 P1**：`store.ts`（底层）import `routes/table/schemaUtil`、`routes/record/recordRefEntity`。
3. **循环依赖 P2**：`store.ts` ↔ `storage.ts` 互引。
4. **死代码**：`add/Adder.tsx`、`search/Query.tsx`、`search/promptStorage.ts`、`flow/embedded/typeGuards.ts`。
5. **用户可见 bug**：i18n en 段缺几十个 key，英文用户看到裸 key。

其余（layout 反向依赖 routes 内部、命名不一致、.tsx 误用、目录语义混杂）多为**慢性债**，收益低或属洁癖，按机会推进或不做。

---

## 二、现状概览

### 2.1 模块行数分布（wc 核实无误）

| 目录 | 文件 | 行数 | 性质 |
|---|---|---|---|
| `flow/` (含 `embedded/`) | 22 | 3123 | 可视化（最大模块） |
| `routes/record/` (含 `embedding/`) | 10 | 2389 | record 路由 + 实体工厂 + 编辑态 |
| `store/` | 5 | 1415 | 状态 / 持久化 / vendored |
| `routes/setting/` | 12 | 1212 | 设置面板 + 服务 + 工具 |
| `routes/table/` | 5 | 865 | table 路由 + **全局 Schema** |
| `routes/search/` | 7 | 520 | Finder 面板簇 |
| `routes/add/` | 3 | 494 | Chat + AddJson |
| `res/` | 5 | 436 | 资源读取 |
| `api/` | 6 | 388 | HTTP + 数据类型 |
| `routes/headerbar/` | 4 | 301 | 顶部栏导航控件 |
| `src/` 根 | 6 | 640 | 入口 + i18n + queryClient |

最大文件：`flow/EntityForm.tsx` 782、`routes/record/recordEditEntityCreator.ts` 728、`store/store.ts` 589、`store/storageJson.ts` 452(自动生成)、`routes/table/schemaUtil.tsx` 452、`routes/record/editingObject.ts` 411。

### 2.2 依赖现状（修正 P3 归属）

```
store/store.ts ──import──▶ routes/table/schemaUtil      (P1 反向，store.ts:5)
                ──import──▶ routes/record/recordRefEntity (P1 反向，store.ts:8)
store/store.ts ◀──循环──▶ store/storage.ts               (P2 循环)
CfgEditorApp(layout) ──import──▶ routes/{Chat,Finder,Setting,RecordRef,HeaderBar} (P3 layout 反向)
main.tsx ──import──▶ routes/setting/themeService          (全局服务埋在 setting/)
routes/table/schemaUtil.tsx ◀──被 27 个外部文件 import── (record/add/search/headerbar/setting/store/res/CfgEditorApp) (P4 全局核心错置)
```

> 注：P3 中 `CfgEditorApp` 的 import 不含 themeService（v1 依赖图错误已修正）；themeService 由 `main.tsx` 引用。

---

## 三、值得做的事（收敛后的 5 类）

### 3.1 死代码清理（零风险，立即可做）

| 文件 | 行数 | 核实 |
|---|---|---|
| `routes/add/Adder.tsx` | 34 | 零引用；`CfgEditorApp` 的 `dragPanel=='chat'` 直接渲染 `<Chat>`，不经 Adder |
| `routes/search/Query.tsx` | 48 | 零引用；无 React.lazy/动态 import |
| `search/promptStorage.ts` | 20 | `savePromptAsync` **全仓零调用**（v1 误判为"错置"，实为死代码） |
| `flow/embedded/typeGuards.ts` | 146 | 零引用（Folds/FoldStateHelper 有引用者，唯独它没有；v1 未识别） |

> 注意：删 Adder/Query 后 schemaUtil 的引用数从 29 降到 27。

### 3.2 全局核心错置迁出（中风险，单独 PR）

**`schemaUtil.tsx` → `src/domain/schema.tsx`**（保留 `.tsx`！文件含 JSX）
- 被横跨 record/add/search/headerbar/setting/store/res/CfgEditorApp 的 **27 个外部文件** import，却埋在 table 路由目录——最反直觉的归类。
- **前置条件**（v1 漏掉）：文件含 JSX 函数 `getIdOptions`/`getIdOptionsWithNew`（被 `recordEditEntityCreator.ts`、`IdList.tsx` 引用）。若想把主体改 `.ts`，须先拆这两个函数到 `domain/idOptions.tsx`。否则主体只能保留 `.tsx`。
- 改名 `schemaUtil → schema` 是**可选加分项**（去 `Util` 贬义后缀），既然要动 27 处 import，顺带改名边际成本低。
- **import 扩展名问题**：27 处中多数写 `schemaUtil.tsx`（带扩展名），但 `embedding/embeddingChecker.ts`、`embeddingFieldExtractor.ts` 写不带扩展名的 `../table/schemaUtil`——改写要同时处理"路径+扩展名+大小写"三重变更。

**`themeService.ts` → `src/services/themeService.ts`**（低风险）
- 仅 2 个 importer（`main.tsx`、`ThemeSetting.tsx`）。全局基础设施（单例+缓存）不该埋在设置面板目录。

### 3.3 反向/循环依赖治理（v1 方案修正后，中风险）

#### P1 治理 —— 迁 `getLastOpenIdByTable` + FixedPage 工厂，**不是** nav hook
- v1 误以为外迁 nav hook 能消 P1。**源码核实**：nav hook（`navTo`/`useLocationData`/`useCurPageRecordOrRecordRef` 等）内部完全不用 `Schema`/`getId`，外迁后 P1 的两条 import 纹丝不动。
- P1 的真正根源：`store.ts:5` 的 `Schema` 用于 `getLastOpenIdByTable(schema, ...)`（492 行）；`store.ts:8` 的 `getId` 用于 `makeFixedPage`（374 行）。
- **正确治理**：把 `getLastOpenIdByTable` 与 FixedPage 工厂（`makeFixedPage`/`makeUnrefPage`/`isFixedRefPage`/`isFixedUnrefPage`/`setFixedPagesConf`/`getFixedPage`）迁到 `domain/`（它们天然依赖 Schema/recordRefEntity），store.ts 不再 import routes。
- nav hook 外迁是**独立的导航内聚改善**，可作为可选项，但它**不解决 P1**，且涉及 19 个文件改 import（useLocationData 被 17 文件、navTo 14、useCurPageRecordOrRecordRef 6），性价比低，**建议不做**。

#### P2 治理 —— 仅下沉纯函数键集，**不要**下沉 `readStoreStateOnce`
- `prefSelfKeySet`/`notSaveKeySet`/`getPrefKeySet`/`getPrefSelfKeySet`（纯函数）从 store.ts 下沉到 storage.ts → 安全，消除 P2 的成因之一。
- **不要**下沉 `readStoreStateOnce`：它被 `res/readResInfosAsync.ts:169` 引用（v1 说"外部无人 import"错误），且内部访问 `store`(resso 实例)；下沉会让 storage.ts 反向 import store，把 store→storage 单向**变成双向循环**，加剧 P2。

### 3.4 小错置清理

- **`colorUtils.ts` 拆出 `toggleFullScreen`** → 新建 `src/utils/windowUtils.ts`。`toggleFullScreen` 被 `headerbar/HeaderBar.tsx`、`setting/Operations.tsx` 跨目录引用，与 `fixColor/fixColors`（颜色，仅 setting 内部用）职责无关。仅拆这一个函数，`fixColor*` 留原处。

### 3.5 用户可见 bug：i18n en 段缺 key（v1 漏列，应单列）

- `src/i18n.ts` 内联翻译：zh ~150 key、en ~70 key，**en 段缺几十个 key**（`basicSetting/recordShowSetting/aiSetting/operations/keySetting/refIn/refOutDepth/maxNode/...`）。
- `fallbackLng: 'en'` 但 en 自己缺 key → 英文用户切 en 后看到裸 key 字符串。这是比"目录结构乱"更优先的用户可见缺陷。
- **修复**：为所有 zh key 补 en 翻译（至少 fallback）。零结构改动，与目录重构解耦，纯加翻译字符串。

---

## 四、明确不做 / 降级（防过度设计，v2 砍除项）

| 建议 | 处置 | 理由 |
|---|---|---|
| 配 `@/` 路径别名 | **降级为可选**，不当前置 | 相对路径痛点夸大：`../../../` 全仓仅 7 处（全在 embedding/）。存量 **285 处带扩展名 import 不会自动受益**，全量改写是另一大 PR；混用会形成"带扩展名相对 / 不带扩展名相对 / `@/`"三种风格。别名仅对新代码有用 |
| `flow/node/` 子目录 | **降级，按机会** | 19 文件对单人维护的可视化核心，导航靠 IDE 搜索而非目录树；且 `getResBrief` 有**簇外引用**(`res/summarizeResAsync.ts`)，`CustomAutoComplete ↔ EntityForm` **循环互引**——v1"仅经 FlowNode 暴露"不成立 |
| `i18n.ts` 拆 index/en/zh | **删除** | 翻译增长缓慢（近 10 提交 +30 行）；251 行单文件一屏可览，拆 3 文件改一个 key 要跳 2 文件反增摩擦。真问题是 en 缺 key（见 3.5），不是文件拆分 |
| `api/` 拆 `api/`+`models/` | **删除** | 6 文件 388 行扁平已清晰；HTTP 与其类型强相关放一起是行业惯例；拆完 `api/` 只剩 1 文件更尴尬 |
| `store/` 子目录化 generated/vendor | **降级为注释头** | storageJson、resso 各 1 文件，为 1 文件建子目录是教科书过度组织。改用文件头注释 `// AUTO-GENERATED` / `// VENDORED from resso` |
| 完整分层 routes/features/domain/services | **删除**（含"长期目标"提法） | 86 文件单应用四层分层是 over-engineering。迁出 4 个全局核心后 `routes/` 语义自然清晰，无需再引入 features/domain 顶层目录 |
| 命名统一（多数） | **降级** | `tableRefEntity`(1 引用)、`recordEntityCreator`(1 引用) 改名成本 > 收益。仅 `recordRefEntity→recordRefUtils`(5 引用，含 store.ts，与 P1 相关) 值得，且与 P1 治理一起做 |
| 4 处 `.tsx→.ts` | **保留但改措辞** | 文件确无 JSX 可安全改名，但牵动 **9 处 importer** 同步改扩展名（非"零工作量"）；useEntityToGraph 被 4 个路由文件 import。机械低风险，属批次1 |
| `ResPopover→ResViewer` 改名 | **删除** | 改名收益不抵 6 处 import + blame 中断；加文件头注释说明即可 |
| `flow/flowTypes.ts` 抽类型 | **降级** | 收益真实（消除类型寄生组件文件的反向依赖）但非急；10 处 import 调整。与 flow/node 一起按机会做 |

---

## 五、重构路线图（v2，速胜在前 + 验证/回滚/耦合清单）

### 5.0 验证清单（每批落地后必跑，因项目零单测、无 CI）

```
1. pnpm run lint        # oxlint 类型/钩子检查
2. pnpm run build       # tsc 类型 + vite 打包
3. pnpm tauri dev 冒烟   # 必须桌面端跑（浏览器端无法验证 Tauri 文件 IO）
   - 打开含 table+record+recordRef 的真实配置
   - 走一次：选中节点 → 编辑字段 → 保存 → 重连服务器 → 切换中英文
   - 验证 elk 布局 worker 正常（layoutAsync.ts 的 elk-worker.min.js?url 是包导入，不受影响）
4. 改名/移动后 grep 全仓确认无残留旧路径（含 .oxlintrc.json / *.bat / CLAUDE.md）
```

### 5.1 外部路径耦合清单（移动文件时必须同步，v1 漏了 .oxlintrc.json）

| 外部文件 | 硬编码的 src 路径 | 触发 |
|---|---|---|
| `genJsonParser.bat` | `src/store/storageJson.ts`（输出 `-o`） | 动 storageJson |
| `.oxlintrc.json` ignorePatterns | `src/store/storageJson.ts` | 动 storageJson |
| `.oxlintrc.json` overrides | `src/store/resso.ts`、`src/main.tsx` | 动 resso/main |
| `CLAUDE.md`（cfgeditor） | 多处目录描述、错误的 `src/locales/` | 任意目录变更 |
| `main.tsx` | `import './i18n.js'`（.ts 文件用 .js 扩展名导入） | 动 i18n |

### 5.2 import 扩展名约定（影响所有改名/移动的成本）

全仓 **285 处相对 import 几乎 100% 带 `.ts`/`.tsx` 后缀**（`allowImportingTsExtensions: true`）。因此每次"机械改名"都要同步改引用方的扩展名。建议改名 PR 的机械改写把"旧名.tsx → 新名.ts"作为整体替换单元；**本批不顺便去扩展名**（避免单 PR 范围爆炸，保留现状约定）。

### 5.3 批次划分（速胜在前，每批可独立采纳）

| 批次 | 内容 | 前置 | 独立价值 | 回滚 |
|---|---|---|---|---|
| **1 · 零风险速胜** | 删 4 个死代码(3.1) + 4 处 `.tsx→.ts`(9 import) + storageJson/resso 加注释头 + 补 i18n en key(3.5) + 拆 `toggleFullScreen`→windowUtils(3.4) + 修 CLAUDE.md | 无 | 死代码清理 + 文档修正 + 修复英文用户裸 key bug | 单项独立提交，互不影响 |
| **2 · 全局核心错置** | `schemaUtil.tsx → domain/schema.tsx`（先拆 `getIdOptions*` 到 idOptions.tsx，可选改名 schema）+ `themeService.ts → services/` | 批次1的 tauri dev 冒烟习惯 | 消 P4，schema 不再埋在路由目录 | `git mv` 保护 blame；纯移动 PR 禁逻辑修改 |
| **3 · 反向/循环依赖** | P1：迁 `getLastOpenIdByTable` + FixedPage 工厂 → domain/（**非 nav hook**）；P2：仅下沉纯函数键集（**非 readStoreStateOnce**） | 批次2（schema 已在 domain，FixedPage 工厂迁过去 import 稳定） | 消 P1/P2，store 不再反向依赖 routes | 每子项独立 PR，`git revert` |
| **4 · 命名（可选）** | `recordRefEntity→recordRefUtils`（5 引用，与 P1 同批做更省） | — | 语义准确 | `git mv` |

> v1 把"配 `@/` 别名"当前置批次0——**本版删除该前置**。批次 2-3 用相对路径完全可行（成本是手改几十处，但避免别名引入的三风格混用）。别名仅在团队明确想要时单独评估。

### 5.4 关键操作纪律

- **纯移动/改名 PR 禁止夹带逻辑修改**——否则 reviewer 难辨机械改名与逻辑改动，且 `git blame` 失真。
- 用 `git mv`（而非删旧建新）保护 blame 链；review 时 `git log --follow --find-renames` 验证。
- 批次2 的 schemaUtil 迁移**先拆 JSX 函数再迁主体**，不可一步到位。

---

## 六、风险提示与不动项

### 高风险点（动前必读）
- **`storageJson.ts`（自动生成）**：`genJsonParser.bat`（cfgeditor 根）输出路径 `-o src/store/storageJson.ts` 硬编码；移动须同步改 .bat + 改 **13 个 importer**（不止 store.ts/storage.ts 两处，v1 低估）+ 改 `.oxlintrc.json` ignorePatterns。建议同时把源 `json.ts` 移到生成产物旁。改后 `pnpm tauri build` 验证。
- **`resso.ts`（vendored）**：整文件覆盖式升级，别加业务代码；移动到 vendor/ 需改 store.ts import + `.oxlintrc.json` overrides。本版建议**不动文件位置，仅加注释头**。
- **import 扩展名**：285 处带后缀，改名/移动要同步改扩展名（见 5.2）。
- **Tauri 打包（明示安全）**：`src-tauri/tauri.conf.json` 仅引用 `../dist`，对 src 内部移动无感；`elk-worker.min.js?url` 是 npm 包导入非本地文件。

### 明确"保持现状"
- `flow/EntityForm.tsx`(782)、`recordEditEntityCreator.ts`(728)：内部已组织良好，不强拆（后者 EmbeddedFieldBuilder 拆分属另一议题，与本目录重构解耦）。
- `res/`（5 文件内聚）、`headerbar/`、`record/embedding/`：结构健康，无需动。
- `setting/` 9 个面板：由 Setting.tsx 作 Tab 容器天然组织，只移走 .ts 服务/工具，面板本身不细分子目录。

---

## 附：v1 → v2 变更摘要

- **修正 5 处事实错误**：schemaUtil 含 JSX / P3 themeService 归属 / promptStorage 是死代码 / nav hook 消不了 P1 / readStoreStateOnce 下沉加剧 P2。
- **新增 2 处死代码**：`typeGuards.ts`、`promptStorage.ts`。
- **砍除/降级 9 项过度建议**：@/别名、flow/node、i18n三拆、api拆models、store子目录化、完整分层、多数改名、ResPopover改名、flowTypes（降级）。
- **新增维度**：验证清单(5.0)、外部路径耦合清单(5.1)、import 扩展名约定(5.2)、git mv 纪律(5.4)、i18n en bug 单列(3.5)。
- **重排批次**：零风险速胜提到批次1（原 v1 把别名放批次0）；别名移出前置。
- **收敛后的核心动作**：删 4 死代码 → 迁 schema(先拆JSX)+themeService → 迁 FixedPage 工厂+下沉纯函数键集(消P1/P2) → 补 i18n en key。其余按机会或不动。
