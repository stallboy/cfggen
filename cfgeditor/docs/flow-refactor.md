# cfgeditor `src/flow` 重构与优化建议

> 范围：`src/flow/**`（21 个非测试源文件，约 2750 行）。
> 本文是**架构 review + 重构建议**，不是"把组件换一换"的清单。每条先讲"为什么现在别扭"（诊断），再给"具体怎么改"（做法），最后标风险/收益与优先级。
> 基于 2026-07-14 代码快照；行号会随后续改动漂移，定位时以符号名为准。
> 配套阅读：`docs/ui-redesign.md`（routes 层重构，与本文件正交）。

---

## 0. 一句话总目标

让 `flow` 维持它**已经做对的领域解耦**（entity 不可变 / 布局走 worker / 视口语义可测），同时拆掉两个"超级组件"（`EntityForm` 783 行、`FlowNode` 252 行）、清掉一批重复与死代码、把"估算高度 ↔ 真实渲染"这对最脆弱的隐性契约从注释提升为可守护的东西。

---

## 1. 现状全景：数据流与职责分层

```
routes (Record / Table / RecordRef / TableRef)
  │  构造 entityMap: Map<string, Entity>，调 fillHandles(entityMap)
  │  调 useEntityToGraph({ type, pathname, entityMap, notes, nodeMenuFunc, paneMenu, editingObjectRes })
  │  return null   ← 路由本身不渲染画布
  ▼
useEntityToGraph (hook)                       ← 编排核心
  ├─ convertNodeAndEdges(entityMap, nodeShow, notes)  纯函数 → nodes/edges      [entityToNodeAndEdge.ts]
  ├─ useQuery → layoutAsync(nodes, edges, strategy, nodeShow, signal)           [layoutAsync.ts, ELK worker]
  │      → id2RectMap (位置 + 估算宽高)
  ├─ applyPosition / applyWidthHeight → newNodes
  ├─ Effect 1: setNodes/setEdges + flowGraph.setPaneMenu/setNodeMenuFunc(...)   ← 命令式反向注入菜单
  └─ Effect 2: pickViewportAction → fitView / setViewport                       [viewportMath.ts]

CfgEditorApp
  └─ <FlowGraph>                               ← 常驻外壳：ReactFlowProvider + ConfigProvider + FlowGraphContext.Provider
       ├─ <ReactFlow nodeTypes={node: FlowNode}>
       └─ <Routes>{children}</Routes>          ← 路由在 children 内，靠 context 反向下发菜单
            └─ 每节点 <FlowNode>
                 ├─ readonly → <EntityProperties>
                 ├─ card     → <EntityCard>
                 └─ editable → <EntityForm>     ← 783 行，按字段类型分发 7 种 FormItem
```

**关键架构约束（理解它才能评估建议）**：`FlowGraph` 是**常驻单例**（包裹 `<Routes>`，路由切换不重建 `ReactFlow`，省 N 次 provider/theme 开销）。正因路由在 `children` 内、无法直接给 `FlowGraph` 传 props，菜单才走 `FlowGraphContext` 的 setter **反向注入**。这是约束下的合理选择，不是随手写的（见 §5-A1）。

---

## 2. 设计亮点（必须保留，勿在重构中破坏）

review 不是只挑错。这套代码有几处质量很高，重构时**要绕开它们**：

1. **domain / presentation 解耦** — `entity` 是纯 domain（不可变、memo-safe），`nodeShow/notes` 走 `node.data` 下发，`query` 走 store per-key 订阅。`FlowGraph.tsx:10-21`、`entityToNodeAndEdge.ts:60-66` 有极详尽的"为什么"注释。这是教科书级的关注点分离。
2. **布局走 Web Worker + react-query + AbortSignal** — `layoutAsync.ts` 模块级单例 ELK、`LayoutError` 一律 throw（绝不 resolve undefined，避免 react-query 把 undefined 当"成功无数据"）、`signal` 透传放弃过期布局。失败语义清晰。
3. **`viewportMath.ts` 把视口数学抽纯函数 + 不变量测试** — 把"relayout 后锚点屏幕坐标不变"从注释提升为**可执行契约**（`computeStableViewport`）。这正是 §4 想推广到其它模块的做法。
4. **`colors.ts` 三级着色（值→标签→类型）+ `NODE_SHOW_DEFAULTS` 单一兜底来源**；`dimensions.ts` 注释明确"节点宽度三处同源"（ELK 边界框 / CSS / Handle 偏移）。
5. **`FORM_THEME` 上提到 `FlowGraph` 单实例**（`FlowGraph.tsx:37-48`）—— 原 N 个可编辑节点 = N 个 ConfigProvider，已收敛为 1。有性能意识。
6. **注释文化** — 几乎每个非平凡决策都写了"为什么"（性能契约1、doc A2、memory 引用）。重构时**这些注释要随代码迁移，不能丢**。

---

## 3. 优先级矩阵

| 优先级 | 主题 | 收益 | 风险 | 详见 |
|---|---|---|---|---|
| **P0** | 小修清理（死代码、重复、无用参数、console.log、命名） | 中 | 极低 | §4 |
| **P1** | `FlowNode` 拆分（抽 NodeNote / NodeTitle / NodeToolbar） | 高（可读/可改） | 中 | §5-C |
| **P2** | `EntityForm` 拆目录 + `renderFieldItem` 改组件 | 高（最大文件） | 中 | §5-B |
| **P3** | 高度估算 ↔ 渲染对账机制（HeightDriftGuard 复活 / 替代） | 高（最脆弱契约） | 中高 | §5-A3 |
| 讨论 | FlowGraph 菜单反向注入、fillHandles 盖章 handle（分层小瑕疵，**非不变性问题**） | 低（认知/文档） | — | §5-A1/A2、§6 |

建议顺序：**P0（批量、安全）→ P1 → P2 → P3**。P1/P2 可分文件独立提交；P3 需先有性能回归基线。

---

## 4. P0：小修清理（独立、安全、可一次性批量做）

这一节每条都是低风险点，互相独立，可以攒一个 PR 一起过。

### 4.1 删除/归档死代码 `HeightDriftGuard.tsx`

**诊断**：`HeightDriftGuard` 已停用，全仓**零 import**（`FlowNode.tsx:243` 仅一句注释提及）。它停在 `src/flow/` 主目录、文件名无 `dev` 标记，容易被误以为在用。停用原因见文件头注释：`useStore(measured.height)` 订阅在节点高度抖动时引发 re-render 风暴。

**做法（二选一）**：
- **删**：若接受"靠 `calcWidthHeight.test.ts` 锁魔数"作为唯一护栏 → 直接删文件 + `FlowNode.tsx:243` 注释。
- **留但归档**：移到 `src/flow/__dev__/HeightDriftGuard.tsx`（或加 `.dev.tsx` 后缀），明确"仅 dev 手动启用"。不要留在主路径下装作活代码。

> 它承载的是 §5-A3 的"高度对账"诉求，删之前先读 §5-A3 决定要不要以另一种形态复活它。

### 4.2 合并 `applyPositionToNodes` + `applyWidthHeightToNodes`

**诊断**：`useEntityToGraph.ts:47-77` 两个函数都在 `nodes.map(n => id2RectMap.get(n.id) ? {...n, ...} : n)`，一个写 `position`、一个写 `width/height`，调用处还遍历两次：
```ts
const positionedNodes = applyPositionToNodes(nodes, id2RectMap);
return applyWidthHeightToNodes(positionedNodes, id2RectMap);   // 第二次遍历同一份 map
```

**做法**：合并成一次遍历：
```ts
function applyRectToNodes(nodes: EntityNode[], rectMap: Map<string, Rect>): EntityNode[] {
    return nodes.map(n => {
        const r = rectMap.get(n.id);
        return r ? {...n, position: {x: r.x, y: r.y}, width: r.width, height: r.height} : n;
    });
}
```
调用点（`useEntityToGraph.ts:136-142`）随之简化。注意保留原 `applyPositionToNodes` 里 `console.log('not found', ...)` 的诊断意图（见 4.3）。

### 4.3 清理生产路径上的 `console.log`

- `useEntityToGraph.ts:57` — `console.log('not found', n, id2RectMap)`（合并 4.2 时顺手处理：要么删，要么包进 `import.meta.env.DEV`）。
- `entityToNodeAndEdge.ts:37,50,53` — `fillHandles` 里的 handle-not-found 诊断。这些**有诊断价值**（数据异常时定位用），但建议统一收口到一个 `devLog`/`devWarn` 工具（`if (import.meta.env.DEV) console.warn(...)`），避免生产构建打日志。

### 4.4 抽 `nodeAnchor(nodeProps)` 消除重复对象构造

**诊断**：`{id, x: nodeProps.positionAbsoluteX, y: nodeProps.positionAbsoluteY}` 这个"节点锚点"对象在 `FlowNode.tsx`（fold/unfold/moveUp/moveDown/delete 共 ~5 处）和 `EntityForm.tsx`（`EmbeddedSimpleStructuralItem`/`FuncAddFormItem`/`InterfaceFormItem` 共 ~3 处）重复出现约 8 次。

**做法**：
```ts
// flow/nodeAnchor.ts
export function nodeAnchor(nodeProps: NodeProps<EntityNode>): { id: string; x: number; y: number } {
    return { id: nodeProps.data.entity.id, x: nodeProps.positionAbsoluteX, y: nodeProps.positionAbsoluteY };
}
```
各处 `onClick={() => edit.editOnMoveUp?.(nodeAnchor(nodeProps))}`。顺带把 `FlowNode.tsx:106-115` 的 `unfoldNode/foldNode`（已 useCallback）和三个内联箭头按钮统一成同一来源，消除"有的 memo 了有的没 memo"的不一致（见 §5-C3）。

### 4.5 抽共享 `useFieldItemStyle(bgColor)`

**诊断**：`EntityForm.tsx` 里 `PrimitiveFormItem`(462-465) 与 `ArrayOfPrimitiveFormItem`(499-502) 有逐字符相同的 `itemStyle` useMemo：
```ts
const itemStyle = useMemo(() => (bgColor === undefined ? {} : {backgroundColor: bgColor}), [bgColor]);
```

**做法**：抽 `useFieldItemStyle(bgColor)` 或直接 `fieldItemStyle(bgColor)` 纯函数（无 hook 依赖时不必是 hook）。

### 4.6 命名与小修

- **`FlowGraph.tsx:8,128` — `FlowGraphContext as FlowGraphContext1`**：别名后缀 `1` 无任何冲突来源，是历史残留。去掉别名。
- **`EntityForm.tsx:102` — `getFilter(_isValueInteger, useSearch)`**：第一个参数 `_isValueInteger` 函数体完全没用（只看 `useSearch`）。删参数，调用点（177、644）同步。
- **`FlowGraphContext.ts:11-18` — dummy 默认值**：`createContext` 用 `dummy` noop 默认值，若在 `FlowGraph` 外误用 context 会**静默失败**。改为默认 `undefined`，`useContext` 处加 `if (!ctx) throw new Error('FlowGraphContext missing')`，把误用变显式报错。
- **`FlowNode.tsx:116-127` — `[resBriefButton, firstImage]` 元组**：一个 useMemo 同时算"资源按钮"和"首图"两个不相关产物，返回元组。拆成两个 memo 或一个 `{resBriefButton, firstImage}` 对象，可读性更好。

---

## 5. 中大型重构（按文件 / 维度）

### §5-B — P2：`EntityForm.tsx` 拆分（783 行 → 目录）

这是 flow 里**最大的单文件**，也是收益最高的拆分目标。当前一个文件塞了：常量/类型、4 个工具函数、`useSyncFieldValue`/`useRefItemStyles` 两个 hook、`primitiveControl` 控件选择器、`LabelWithTooltip` + **7 个 FormItem 子组件** + `renderFieldItem` 分发器 + 主组件。

#### B1. 按字段类型拆成目录

```
src/flow/edit/
  EntityForm.tsx              主组件 + <FieldRenderer> 分发
  fields/
    PrimitiveFormItem.tsx
    ArrayOfPrimitiveFormItem.tsx
    InterfaceFormItem.tsx
    StructRefItem.tsx
    EmbeddedSimpleStructuralItem.tsx
    FuncAddFormItem.tsx
    FuncSubmitFormItem.tsx
    ArrayItemExpandButton.tsx
  shared/
    useSyncFieldValue.ts
    LabelWithTooltip.tsx
    useRefItemStyles.ts
    primitiveControl.tsx
    fieldUtils.ts             getDefaultPrimitiveValue / isArrayPrimitiveBoolOrNumber / getFilter
    constants.ts              FORM_LAYOUT / FORM_ITEM_LAYOUT_WITHOUT_LABEL / FORM_STYLE ...
```

每个 FormItem 子组件已经 `memo` 且边界清晰，拆分是**机械搬运**，风险主要在 import 路径与 `FilterOption` 类型导出（现 `EntityForm.tsx:82` 导出，被 `CustomAutoComplete.tsx` 引用 → 移到 `shared/types.ts`）。

#### B2. 把 `renderFieldItem` / `renderFieldItems` 从"函数"改成"组件"

**诊断**：`EntityForm.tsx:674-749` 的 `renderFieldItem` 是**普通函数返回 JSX**，被 `EntityForm` 和 `InterfaceFormItem`（递归渲染 `implFields`）调用。函数式渲染的代价：
- React 不把它当组件实例（无稳定 fiber、无独立 hooks 边界、`key` 作用域混乱）；
- 父级每次重渲都重新执行函数产生新 element，子组件 `memo` 的 props 比较要靠运气；
- `renderFieldItem` 透传整个 `nodeProps`，每个字段都拿到完整 `nodeProps`（含 `positionAbsoluteX/Y`），耦合面过宽。

**做法**：改成判别组件：
```tsx
// edit/FieldRenderer.tsx
function FieldRenderer({field, edit, nodeProps, nodeShow}: FieldRenderProps) {
    switch (field.type) {
        case 'primitive':        return <PrimitiveFormItem field={field} bgColor={getFieldBackgroundColor(field, nodeShow)} />;
        case 'arrayOfPrimitive': return <ArrayOfPrimitiveFormItem field={field} bgColor={getFieldBackgroundColor(field, nodeShow)} />;
        case 'interface':        return <InterfaceFormItem field={field} edit={edit} nodeProps={nodeProps} nodeShow={nodeShow} />;
        case 'structRef':        return field.embeddedField
            ? <EmbeddedSimpleStructuralItem field={field} edit={edit} nodeProps={nodeProps} bgColor={...} />
            : <StructRefItem name={field.name} comment={field.comment} handleOut={field.handleOut}
                             bgColor={...} width={getEditNodeWidth(nodeShow)} />;
        case 'funcAdd':          return <FuncAddFormItem .../>;
        case 'funcSubmit':       return <FuncSubmitFormItem field={field} />;
    }
}

// EntityForm.tsx
{edit.fields.map(field => <FieldRenderer key={field.name} field={field} edit={edit} nodeProps={nodeProps} nodeShow={nodeShow} />)}
```
`InterfaceFormItem` 内的 `renderFieldItems(field.implFields, ...)` 同样改成 `implFields.map(f => <FieldRenderer key={f.name} .../>)`。`key` 终于落在组件边界上。

#### B3. `useSyncFieldValue` 的 N 个分散 effect（可选优化）

**诊断**：`PrimitiveFormItem`/`ArrayOfPrimitiveFormItem`/`InterfaceFormItem` 各自在内部调 `useSyncFieldValue(form, field)` → 每个字段一个 effect。这是 antd Form.Item `initialValue` 不随 `key` 复用更新的已知问题（注释 `EntityForm.tsx:142-158` 解释得很清楚），设计上必要。

**权衡**：现状每个 effect deps 精确（只依赖自己的 `field`），任一字段变只跑那个字段的 effect。若改成顶层一个 `<SyncAllFieldValues form fields />` 遍历，则任一字段变都会重跑全量遍历（O(N)）。**对字段数不多的表单，分散写法更优；只有字段数极大时才值得集中化。** 标记为"可选"，默认不动。

---

### §5-C — P1：`FlowNode.tsx` 拆分（252 行）

`FlowNode` 是个"超级分发器"，内联了 note 双模式、fold 态、res brief、title、5 种按钮。8 个 `useMemo`，状态（`isEditNote`/`tmpNote`）交错。

#### C1. 抽 `NodeNote` 子组件，封装两套 note 机制

**诊断**：`FlowNode.tsx:87-198` 的 note 逻辑是最绕的部分：
- 只读态：`NoteShow`/`NoteEdit`（走 `useMutation` → `updateNote` API）；
- 编辑态：`NoteEditInner` + 本地 `tmpNote`（走 `edit.editOnUpdateNote`，不触网）；
- `editNoteButton`（149-165）和 `noteShowOrEdit`（167-198）两个大 useMemo，依赖 `tmpNote/entity/note/notes/id/isEditNote/label` 全部交织。

**做法**：抽 `<NodeNote id entity edit note notes nodeShow label />`，把 `isEditNote`/`tmpNote` 状态和两套渲染收进子组件。`FlowNode` 只在 `mayHaveResOrNote(label)` 时渲染它。`editNoteButton` 也并入（它本质是 note 的触发器）。这能把 FlowNode 砍掉约 60 行、消除两个大 useMemo。

#### C2. 抽 `NodeTitle` + `NodeToolbar`

`title` useMemo（201-240）混合了 foldButton / 文本 / editNoteButton / resBriefButton / moveUp-moveDown-delete 按钮组。拆成：
- `<NodeTitle>`：fold 按钮 + 文本（含 query 高亮）+ 右侧 `<NodeToolbar>`；
- `<NodeToolbar>`：moveUp/moveDown/delete 三按钮，接收 `edit` 与 `nodeAnchor`（4.4）。

#### C3. 统一按钮的命令构造

**诊断**：`FlowNode.tsx:106-115` 的 `unfoldNode`/`foldNode` 用了 `useCallback`，但 211-237 的 moveUp/moveDown/delete 三个按钮是**内联箭头**各自构造 `{id, x, y}`。同一组件内"有的 memo 有的没 memo"是不一致。

**做法**：配合 4.4 的 `nodeAnchor`，把三个按钮也收敛成 useCallback，或全部交给 `<NodeToolbar>` 内部处理。

#### C4. 类型分支用现成 `entity.type` 收窄（与 §5-F1 联动，见下）

---

### §5-A — 架构级（讨论 / P3）

#### A1. FlowGraph 菜单"反向注入"——保持，但固化契约

**诊断**：见 §1，`FlowGraph` 是常驻单例，路由在 `children` 内，菜单只能走 `FlowGraphContext` 的 setter 反向注入（`useEntityToGraph.ts:145-152`）。这是约束下的合理设计，**不建议改成 props**（会破坏单例 ReactFlow）。

真正的脆弱点在 Effect 1 与 Effect 2 之间的**隐性引用契约**：`useEntityToGraph.ts:165-169` 注释解释得很坦白——值类编辑 coalescing flush 曾让 `paneMenu` 拿到新引用 → 视口被连带重置。现在靠"`paneMenu` 惰性化 disabled 维持引用稳定"挡住。

**建议（低优先，主要是文档化）**：把"`paneMenu`/`nodeMenuFunc`/`nodeDoubleClickFunc` 必须引用稳定"从注释提升为 `FlowGraphInput` 上的 JSDoc 契约 + 一条 oxlint/ESLint 规则（如强制 `useCallback`/`useMemo` 包裹）。不必动结构。

#### A2. `fillHandles` 给 entity 盖章 handle 标志——不是"不变性"矛盾，是"分层"小瑕疵

**先纠正一个常见误读**：初读容易觉得"`fillHandles` 直接 `entity.handleOut = true`，与'entity 不可变'叙事矛盾"——**其实不矛盾**。`entityToNodeAndEdge.ts:60-66` 那段"entity 保持纯 domain、不可变 memo-safe"针对的是 **entity 进入图之后**：`convertNodeAndEdges` 把 entity 快照进 `node.data` 之后，下游（`FlowNode`/`EntityForm`/memo 层）**不再改它**，所以按 entity 引用做 memo 才安全。而 `fillHandles` 发生在路由构造 `entityMap` 的**同一阶段**（§1：构造 entityMap → 调 fillHandles → 才交给 useEntityToGraph），是构造期的一次性盖章，盖完再无人写。memo 安全性在"发布后"始终成立。把它当"不变性矛盾"，是混淆了"构造期赋值"与"发布后被改"——前者是 entityMap 构造过程的一部分，本就不在"不可变"契约的管辖范围内。

**真正（且很轻）的观察在分层维度**：`handleIn/handleOut` 本质是**呈现/连接点**标志（`FlowNode` 据它渲染 `<Handle>`，`FlowNode.tsx:249-250`），却挂在 **domain** 的 `Entity`/`FieldBase` 上（`entityModel.ts:16-17,284-285`）。它由 `sourceEdges`（已在 entity 上）派生，是"为渲染预计算的反规范化索引"——一个轻微的"domain 承载呈现关注"的分层瑕疵，与不变性无关。

**建议（低优先，认知统一）**：
- **默认：承认并文档化（推荐）**。在 `entityModel.ts` 的 `FieldBase.handleIn/handleOut` 与 `EntityBase.handleIn/handleOut` 上加注释，点明"entityMap 构造期由 `fillHandles` 盖章、供渲染层读；构造完成后不再变"。叙事即自洽，零风险，ROI 最高。
- **不推荐：搬到 `node.data`**。理论上能让 entity 彻底不含呈现标志，但要改 `convertNodeAndEdges`（由 `sourceEdges` 派生写进 node.data）+ `FlowNode`/`EntityProperties`/`EntityForm` 所有读 handle 处，改动中等，收益仅"domain 更纯"，无正确性/性能收益。按 [[reject-over-engineering-dedup]] 的判据，性价比低，除非未来为别的理由重写转换层，否则别动。

#### A3. 高度估算 ↔ 真实渲染的对账（flow 最脆弱的契约）⭐

这是整个 review 里**最值得投入**的一处，也是 `HeightDriftGuard` 当初想解决的事。

**诊断**：`calcWidthHeight.ts` 用一堆魔数（`FIELD_ROW_H=41` / `EDIT_ROW_H=40` / `CARD_DS_H=38` / `NOTE_ROW_H=22`…）**估算**每个节点高度喂给 ELK；真实高度由 `EntityForm`(antd Form) / `EntityProperties`(antd List) / `EntityCard`(antd Card+Descriptions) 的 DOM 决定。两套必须一致，否则节点 overlap / 留异常间隙。现状：
- Form 部分对齐良好（`text` 估算 `4*EDIT_TEXT_ROW_H` 对应渲染 `rows={4}`，`str` 单行对应 `rows={1}`，见 `EntityForm.tsx:193-199` 与 `calcWidthHeight.ts:111-119`）；
- **note 部分对不齐**：估算按 `NOTE_WRAP_COLS=15` 换行算多行（`NOTE_MIN_ROWS=2`~`NOTE_MAX_ROWS=10`），渲染却是 `NoteEditInner` 的 `TextArea rows={1}` 滚动 / `NoteShow` 裸 div —— 估算系统性偏高；
- 护栏 `HeightDriftGuard` 因 `useStore(measured.height)` 订阅引发 re-render 风暴而停用（memory: `cfgeditor-load-jank-textarea-autosize` 的同类陷阱）→ **现在高度漂移只能靠肉眼发现**。

**做法（按风险递增三档）**：

1. **(低风险) 对齐 note 估算与渲染——渲染侧按估算的多行展开（已定方案）**：估算侧保留现有多行模型（`note.length / NOTE_WRAP_COLS` 夹到 `[NOTE_MIN_ROWS, NOTE_MAX_ROWS]`，`calcWidthHeight.ts:73-85`），**渲染侧改成吃同一份行数**，让估算高度与真实渲染高度同源：
   - 抽 `estimateNoteRows(note)`（把 `calcWidthHeight` 里那段 row 计算提出来，估算与渲染都 import，保证两侧永远同源，杜绝再各自维护一套魔数）；
   - `NoteEditInner`/`NoteEdit` 的 `Input.TextArea` 把 `rows={1}` 改成 `rows={estimateNoteRows(note)}`（`NoteShowOrEdit.tsx:91,124`）；
   - `NoteShow`/`NoteShowInner` 的裸 `<div>`（`:25,107`）按估算行数预留 `min-height = row * NOTE_ROW_H`，消除"估算留 N 行、渲染只占 1 行"的纵向空隙；
   - **与 §6 的"textarea 去 autoSize、固定 rows"约束不冲突**：`rows` 是从 note 内容**算一次**的固定值，不是 autoSize 那种按 DOM 测量动态伸缩（那才是 rAF 风暴的根因）。约束保住。
   - 改后跑 `calcWidthHeight.test.ts` 确认算术未变（行为不变，仅行数计算被提取复用）。
2. **(中风险) 让魔数与渲染同源**：Form 的 `itemMarginBottom` 已在 `FlowGraph` 的 `FORM_THEME`（`FlowGraph.tsx:42-48`）。让 `calcWidthHeight` 的 `EDIT_ROW_H` 等也从同一常量派生（而非独立魔数），改 antd 主题时估算自动跟随。需建一个 `flow/edit/metrics.ts` 单一来源，Form 渲染与估算都 import。
3. **(高风险, 需性能基线) 复活对账护栏，但不订阅 store**：`HeightDriftGuard` 的问题在**持续订阅** `useStore(measured.height)`。改成**一次性测量**：节点 mount 后用一个 effect + `requestAnimationFrame`（或 `ResizeObserver` 的**单次**回调后立即 `disconnect()`）读一次 measured，偏差超阈值 `console.warn` 一次（按 id 去重，已有 `heightDriftWarned` Set），**绝不 setState、绝不持续订阅**。这样既有对账，又不引入 re-render 风暴。放 `__dev__/`，仅 dev 生效。**上线前用 `docs/性能测量陷阱` 的金标准（PerformanceObserver longtask）A/B 验证零回归。**

> A3 的根因是"ELK 不测 DOM"。在 ELK 侧无解，只能在"估算尽量准 + 偏差可发现"两头发力。第 1 档现在就能做。

---

### §5-F — 类型与健壮性

#### F1. `FlowNode` 的 `let fields/edit/brief/note/assets = undefined` 级联——直接用现成的判别联合

**先澄清**：`Entity` 本身**已经是** `ReadOnlyEntity | EditableEntity | CardEntity`，判别属性 `type: 'readonly' | 'editable' | 'card'` 就在类型上（`entityModel.ts:297,306,315`）。所以问题**不是**"缺一个判别联合"——它早就在了。问题是 `FlowNode.tsx:57-74` 的命令式级联**把现成的收窄扔了**：

```ts
let fields = undefined; let edit = undefined; let brief = undefined; ...
if (isReadOnlyEntity(entity)) { fields = entity.fields; note = entity.note; assets = entity.assets; }
else if (isEditableEntity(entity)) { edit = entity.edit; note = entity.note; }
else if (isCardEntity(entity)) { brief = entity.brief; ... }
```

赋完值 `fields/edit/brief` 的类型被拍平成 `T | undefined`，下游 `{fields && <EntityProperties/>}`、`{edit && <EntityForm/>}`、`{brief && <EntityCard/>}` 只能靠 truthy，丢了类型保证。

**做法（用现成的 `entity.type`，不新造类型）**：
- **（最小，推荐）就地窄化取值**：`note`/`assets` 本就在 `EntityBase`（`entityModel.ts:280,283`），无需窄化；三个互斥的 `edit/fields/brief` 用 `entity.type` 三元取出，真分支里 TS 自动收窄：
  ```ts
  const note = entity.note, assets = entity.assets;
  const edit   = entity.type === 'editable' ? entity.edit   : undefined;
  const fields = entity.type === 'readonly'  ? entity.fields : undefined;
  const brief  = entity.type === 'card'      ? entity.brief  : undefined;
  ```
  结构基本不动（`edit/brief` 仍被上面那些 useMemo 用，hook 不能条件化），只是"先 let 再 if 赋值"换成"一次窄化取值"。
- **（可选）三个分叉子节点收敛进 `switch(entity.type)`**：每个 case 里 `entity` 自动收窄到对应成员，与 C1/C2 的子组件拆分契合。

**撤销原 `EntityView`/`pickEntityView` 提案**：原稿想新造一个 `{kind: 'readonly'|...}` union 再投影——这是**重复发明已有的判别联合**（`kind` 只是给 `type` 换名），多一层投影却没换来什么。`colors.ts` 的 `getEntityValueString`（`colors.ts:83-99`）虽也是三分支，但它投影成**值字符串**、与 FlowNode 渲染树无形状重叠，复用不上 `EntityView`；它自己想收窄同样直接 `switch(entity.type)` 即可。按 [[reject-over-engineering-dedup]]，不新造类型。

---

### §5-G — 性能（小项）

- **`ResPopover.tsx:62` — `document.querySelectorAll("video")` 全局互斥播放**：跨节点全局 DOM 查询。Popover 同时只开一个，可接受，但建议加注释标注"已知妥协：依赖 popover 单开假设"。
- **`NoteShowOrEdit.tsx:62` — `queryClient.setQueryData(['notes'], ...)` 硬编码 queryKey**：与 `Record.tsx` 等地的 queryKey 统一到常量（`['notes']` 提到 `queryKeys.ts`），防散落。
- **`colors.ts:83-99` — `getEntityValueString` 对 editable 收集所有字段值 `join(",")` 后 `includes` 匹配**：大表单时串可能很长。按值着色是低频配置，通常可接受；若实测卡顿可改为"匹配第一个命中即返回"。

---

## 6. 不建议动的（明确边界，避免过度重构）

- **FlowGraph 的常驻单例 + 菜单反向注入**（§1、§5-A1）：约束下的合理设计，改了会破坏单 ReactFlow 实例的性能收益。
- **layout 走 worker + react-query + AbortSignal 的整体编排**（`useEntityToGraph`/`layoutAsync`）：失败语义（throw 而非 resolve undefined）、缓存隔离（`'e'` 标记）、视口 effect 拆分都已深思熟虑，注释详尽。保持。
- **`query` 走 store per-key 订阅、不进 node.data**：resso per-key 订阅是搜索时不重跑 ELK 的根因（memory: `cfgeditor-resso-per-key-subscription`），勿改。
- **textarea 去 autoSize、固定 rows**：这是修 rAF 风暴的成果（memory: `cfgeditor-load-jank-textarea-autosize`），§5-A3 的 note 估算对齐要**顺着这个约束**做（别为了对账把 autoSize 加回来）。
- **`FlowGraph` 的 `memo`、各 FormItem 的 `memo`**：保持。拆分时子组件继续 `memo`。

---

## 7. 增量路线图

| 阶段 | 内容 | 风险 | 收益 | 验证 |
|---|---|---|---|---|
| **P0** | §4 全部：删/归档 HeightDriftGuard、合并 applyRect、清 console.log、抽 nodeAnchor/useFieldItemStyle、命名小修 | 极低 | 中 | `pnpm test:run` + `pnpm lint` + 手动跑各视图 |
| **P1** | §5-C：FlowNode 拆 NodeNote/NodeTitle/NodeToolbar + F1 用 `entity.type` 收窄 | 中 | 高（可读/可改） | 各视图手测 note/fold/moveUp/delete 行为不变 + Profile 看 per-node 渲染耗时未升 |
| **P2** | §5-B：EntityForm 拆 `flow/edit/` 目录 + B2 FieldRenderer 组件化 | 中 | 高（最大文件） | `/edit/record/` 大图加载耗时基线（memory 的 ~1.3s/60fps）不回归 |
| **P3** | §5-A3：note 估算对齐 → 魔数同源 → (可选)复活非订阅版对账护栏 | 中→高 | 高（最脆弱契约） | `calcWidthHeight.test.ts` + PerformanceObserver longtask A/B |
| 讨论 | §5-A1 契约文档化、§5-A2 handle 标志加注释（**不建议搬 node.data**） | — | 认知 | — |

**顺序**：P0 必须先做（给后续拆分一个干净地基）；P1→P2 可分别独立提交；P3 需先有性能基线再动。

---

## 附录：拆分后的目标目录结构（参考）

```
src/flow/
  FlowGraph.tsx                  常驻外壳（保持）
  FlowGraphContext.ts            菜单 setter context（保持，默认值改 throw）
  FlowContextMenu.tsx            （保持）
  FlowStyleManager.tsx           （保持）
  FlowNode.tsx                   瘦身：外壳 + switch(entity.type) 分发（~80 行）
  NodeNote.tsx                   新：note 双模式 + editNoteButton（来自 FlowNode）
  NodeTitle.tsx / NodeToolbar.tsx 新：标题与按钮组（来自 FlowNode）
  EntityProperties.tsx           （保持）
  EntityCard.tsx                 （保持）
  Highlight.tsx                  （保持）
  ResPopover.tsx                 （保持）
  NoteShowOrEdit.tsx             （保持）
  CustomAutoComplete.tsx         （保持）
  nodeAnchor.ts                  新：§4.4
  edit/
    EntityForm.tsx               主组件 + FieldRenderer（~120 行）
    fields/                      7 个 FormItem + ArrayItemExpandButton
    shared/                      useSyncFieldValue / LabelWithTooltip / useRefItemStyles / primitiveControl / fieldUtils / metrics(§A3-2) / constants / types
  layout/                        （纯逻辑，已测，保持）
    layoutAsync.ts / calcWidthHeight.ts / entityToNodeAndEdge.ts / viewportMath.ts
    colors.ts / dimensions.ts / getDsLenAndDesc.ts
  __dev__/
    HeightDriftGuard.tsx         §4.1 / §A3-3（仅 dev 手动启用）
```

> `layout/` 下的纯逻辑模块**已全部有单测**（见 `CLAUDE.md` 测试清单），拆目录时测试文件随源码同目录迁移，保持 `*.test.ts` 约定。
