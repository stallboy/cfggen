# `EditingSession` 重构后的优化改进意见

> 对象：编辑管线三次重构（`f6d27484` / `9f542399` / `3ab0527a`）落地后的代码。
> 配套文档：[`startEditingObject-重构分析.md`](./startEditingObject-重构分析.md)（方案诊断与 §5 方案 C 正解设计，附录 D 记录已实施决策）。
> 视角：重构本身已经达成核心目标（render 期变异消除、Compiler 等价性恢复、两条性能契约保留、纯逻辑可测）。本文只列**后续可改进点**，不是返工清单。

---

## 总体评价（先说结论）

这次重构是**高质量**的：

- 状态建模正确：模块级可变单例 → 每会话 store 实例 + `useSyncExternalStore`，副作用全部移出 render，落在构造期 / effect / 事件回调。
- 两条性能契约完整保留：值类编辑就地改不 bump（几十表单零重渲）、结构类编辑 bump 重算 entityMap（共享引用让闭包自动见最新值）。
- `subscribe` / `getStructureVersion` 用**实例箭头函数属性**绑定 `this`（`editingSession.ts:67/74`），引用稳定，`getSnapshot` 返回基本类型 `number`——天然满足 `useSyncExternalStore` 的引用稳定性契约，没有无限重渲风险。这个写法是正确的，**后人勿改成 prototype 普通方法**（会丢 `this`）。
- 纯逻辑单测 + 确定性 fuzz 已建立，`maybeReset` 幂等性、值类/结构类二分、submit 读到最新值都有断言。

下面是按优先级排的改进项。

---

## P0 — 语义澄清与健壮性

### 1. holder 是单值，与文档"天然支持分屏多 Record"的说法存在 gap

**现状**：`currentEditingSession` 是模块级**单值**指针（`editingSession.ts:280`）。`RecordWithResult` 在 mount effect 注册自己、unmount 时"自己仍是 current 才置 null"（`Record.tsx:142-149`）。Chat / AddJson 通过 `getCurrentEditingSession()` 寻址写入（`Chat.tsx:104`、`AddJson.tsx:61`）。

**问题**：相比旧的全局单例，每会话实例已经**隔离了状态**（分屏 A/B 的 `editingObject` 不互踩）——这是实质进步。但"寻址当前会话"仍是单值：分屏同时挂载两个 Record 时，**只有最后注册的会话能被 Chat/AddJson 命中**。如果用户在 A 面板用 Chat 生成内容，但 B 后挂载，写入会落到 B。附录 C "天然支持分屏多 Record" 的说法对状态隔离成立，对 Chat/AddJson 寻址**不成立**。

**建议**：
- 短期（低成本）：在文档/注释里把"支持分屏"降级表述为"状态隔离支持分屏；Chat/AddJson 写入仍只命中最后活动会话"，避免后人误以为已完全支持。
- 长期（若真要做分屏）：holder 改成按 `(table, id)` 或面板 tab 上下文寻址（一个 `Map<panelKey, EditingSession>`），Chat/AddJson 按自己所在面板取对应 session。

**优先级**：P0（澄清）/ P3（真正分屏支持，按需）。当前单活动会话大概率够用。

---

### 2. `updateFormValues` 的 `$impl` 早退分支：强副作用且无测试

**现状**（`editingSession.ts:126-136`）：表单提交的 `values` 若含 `$impl`，且 `impl` 与当前对象 `$type` 的末尾名不一致 → **整个函数 `return`，连普通字段都不更新**。

```ts
if ("$impl" in values) {
    // ...
    if (impl != typeName) {
        return; // impl变化由updateInterfaceValue处理，这里不处理
    }
}
```

**问题**：这是为防 antd Form 在 impl 切换瞬间把旧 struct 的残留字段值一起提交（impl 切换走 `interfaceOnChangeImpl → updateInterfaceValue` 整对象替换，`recordEditEntityCreator.ts:311`）。但"丢弃本次全部普通字段更新"是**较强副作用**：
- 一旦 antd Form 在非切换场景下也混入 `$impl`（如 form state 残留），普通字段更新会被静默吞掉；
- 这段逻辑是从旧 `editingObject.ts` 原样搬来的、有行为但**无任何测试覆盖**。

**建议**：
- 至少补一条单测固化当前行为（带 `$impl` 且不一致时 → editingObject 不变），把它从"隐式约定"升级为"显式契约"。
- 注释补一句"整段跳过是为拦截 impl 切换过渡帧的残留字段；普通字段依赖 impl 一致才写入"，降低误改风险。
- 若后续想精细化：只在确实处于切换过渡时跳过 `$impl` 字段本身，其余字段照常更新（但需先有测试兜底当前行为，再谈改语义）。

**优先级**：P0。这是编辑链里逻辑密度最高、却零覆盖的一段。

---

### 3. `onStructureChange` 同步调 `queryClient.removeQueries`：选型理由需落注释

**现状**（`Record.tsx:83` + `editingSession.ts:265-270`）：每个结构方法末尾 `bumpStructure()` 内**同步**调 `onStructureChange?.()`，组件侧注入的回调是 `queryClient.removeQueries({queryKey: ['layout', pathname, 'e']})`。也就是说结构变更 → bump version → 同步删 layout 缓存 → emit 触发 Record 重渲。

**问题**：这是关键时序决策，附录 D 已说明"不能用 effect 补救——React Query 不因 queryFn 闭包里 nodes 变化重执行"。但**为什么用 `removeQueries` 而非 `invalidateQueries`** 没有落注释：
- `removeQueries`：立即删除缓存项 → 下一次 render 读 layout query 必 miss → 重取（与 emit 同帧，下一帧拿到新 nodes）。
- `invalidateQueries`：标记 stale + 主动触发 refetch，时序上 refetch 与组件重渲可能竞态。
- 当前选 `remove` 是对的（"下一帧必 miss → 重取"语义更确定），但这个判断只活在附录 D 的散文里，代码现场只有一句"清 layout 缓存"。

**建议**：在 `Record.tsx:83` 那行回调旁补注释，写明"用 remove 而非 invalidate，保证 emit 同帧后下帧 layout 必重取；不可挪 effect（见附录 D）"。

**优先级**：P0（防止后人"顺手优化"成 invalidate 或挪进 effect 触发一帧错乱，这正是 `970b768` 那次 revert 的同类坑）。

---

## P1 — 兑现"可单测"承诺

重构的核心收益之一是"编辑链可单测"（分析文档 §7、附录 D）。但**最有逻辑密度的几段还没测**，目前测试集中在结构/版本号语义（`editingSession.test.ts`）。

### 4. `updateFormValues` 的类型转换逻辑零覆盖

**现状**：`toInt`（`parseInt` NaN→0）、`toFloat`（同）、`getFieldPrimitiveTypeConverter`（int/long/float/list<int>/list<long|float> 分发）、数组过滤 undefined（`editingSession.ts:153-166`、`307-349`）。这些都是从旧代码搬来的**纯函数**，行为有判定逻辑（非法输入→0、antd 残留 undefined 被过滤），却无测试。

**风险**：
- `toInt("abc")` 静默写 0（`editingSession.ts:336-337` 注释已点明"NaN 会被静默写回提交"），无测试固化；
- `list<int>` 的元素转换分发（`ft.startsWith('list<')`）若哪天漏分支，静默走 `same` 不转换。

**建议**：补 `updateFormValues` 单测——构造最小 `Schema`（含 int/float/list<int> 字段）+ `values`，断言：合法值转换、非法字符串→0、数组含 undefined 被过滤。这是对 P0#2 的天然补充。

**优先级**：P1。

### 5. `pasteStruct` 的深拷贝独立性无覆盖

**现状**（`editingSession.ts:229-243`）：粘贴时 `structuredClone(copied)` 再写入。`getCopiedObject()` 返回 app 级 `copiedObject`（`clipboard.ts`）。

**风险**：若哪天漏掉 `structuredClone`（比如"优化"成直接赋值），同一段剪贴板内容粘到两处会**共享引用、联动变异**——非常隐蔽。当前无测试。

**建议**：补一条测试——`structCopy(A)` → 粘贴到位置 P1 → 修改 P1 处 → 再粘贴到 P2，断言 P2 仍是原始 A 的副本（未被 P1 的修改污染）。

**优先级**：P1（这是"防回归"型测试，成本低收益明确）。

---

## P2 — 代码清理与小优化

### 6. `delete$refInPlace` 命名误导 + `replaceEditingObject` 未净化 `$refs`（✅ 已实施）

> 原评 P2（仅命名）。深挖后发现是两件同源问题，含一个真实正确性缺口，已一并修复。

**原问题（两件）**：
1. 函数名 `delete$refInPlace`（单数）实现删的是 `o['$refs']`（复数，`FieldRef[]`，后端"谁引用了我"的展示元数据）。更深一层：`$ref` 是 JSON Schema 引用指针的标准保留键，`$refs` 是本项目的引用列表——命名混淆掩盖了语义，不只是单复数笔误。
2. `prepareEditingObject` 的 `$refs` 净化只在**构造期 / `maybeReset`** 跑；`replaceEditingObject`（Chat / AddJson / funcClear）**绕过了净化**。外部/AI 输入若带 `$refs`，会进入 `editingObject`——既可能随 `submit` 提交回后端，也会让 `getIsEdited` 误判 dirty（基准 `originalEditingObject` 构造期已净化、无 `$refs`，比较时不等）。

**已实施**：
- `delete$refInPlace` → `deleteRefsInPlace`（定义 / 调用 / 两处递归全部统一），函数上方补注释写清语义。
- `replaceEditingObject` 入口加 `deleteRefsInPlace(newEditingObject)`，与 `prepareEditingObject` 对齐；注释说明入参均 fresh、就地净化而非 clone 的理由。
- 补单测 `replaceEditingObject：就地剥离入参的 $refs`，固化净化契约。

**安全性已核到根**（就地改入参是否污染调用方引用）：
- Chat / AddJson 入参是 `JSON.parse(...)`，fresh 对象，就地删 `$refs` 是期望净化。
- funcClear 入参是 `schema.defaultValueOfStructural(structural)`（`schema.tsx:251`）：每次 `const res = {"$type":...}` 新建 `return`、**不缓存**，且只产 `$type` + 字段、**根本不含 `$refs`** → 净化为 no-op。
- 就地不换引用 → 现有 `expect(s.getEditingObject()).toBe(newObj)` 测试不受影响；无需 clone。

**验证**：tsc 零错误、vitest 205 全绿。

**优先级**：已修（原 P2 低估；实际为 P0/P1，含输入净化正确性修复）。

### 7. `getIsEdited` 每次值类编辑触发 O(n) 深比较

**现状**：`getIsEdited()` 做 `isDeeplyEqual(editingObject, originalEditingObject)`（`editingSession.ts:78`）。它被 `notifyEditingState()` 调用——即**每次 primitive 键入**都跑一次全树深比较。大记录 + 几十表单连续输入 = 几十次 O(n)。

**权衡**：当前是**精确**脏判定（改回原值 → isEdited 归 false）。若换成"任意编辑即 dirty=true"的近似，会失去"改回原值=未脏"的精确性。

**建议**：暂不动，除非实测到输入卡顿。若要优化：维护 `dirtyVersion` 标记，任意就地改 bump 它；只有需要精确判定时（如 reset 后、提交前）才跑一次深比较重算。低优先，需先有 P1#4 的测试兜底。

**优先级**：P2（当前性能契约下值类编辑不重渲，深比较只是 store 通道开销，resso 写入 + HeaderBar 重渲才是大头；可先观察）。

### 8. `maybeReset` 首帧多一次 `structuredClone`

**现状**：构造期 `prepareEditingObject(recordResult.object)`（clone + 删 `$refs`）+ `structuredClone(obj)` 造两份（`editingSession.ts:58-62`）。随后 mount effect `maybeReset(recordResult)` 又调一次 `prepareEditingObject(recordResult.object)`（第 99 行）做深度比较——若首帧 `recordResult` 未变，这次 clone 只为比较，比较完即弃。

**建议**（小优化）：session 持有上次喂给 `maybeReset` 的 `recordResult` 引用，引用相同直接早退，跳过 `prepareEditingObject` + 深比较。StrictMode effect 双调下也能省掉第二次的 clone。

**优先级**：P2（只在 recordResult 频繁 refetch 的大记录上有可感收益）。

### 9. `Record.tsx` useMemo 依赖里的稳定引用

**现状**：依赖数组含 `session`（`sessionRef.current`，引用恒定）、`setFolds`（useState setter，恒定）。放进去对 lint 友好、无害但冗余。

**建议**：可不动（保持 exhaustive-deps 一致性优先）。仅记录在此，避免有人误以为它们"会变"。

**优先级**：P2（不必改）。

---

## 不建议改的（有意设计，勿动）

以下已在代码注释或附录 D 标明为**有意保留**，列出来防止后人当 bug 修：

- **`editingObjectRes.isEdited` 在纯值类编辑期间不刷新**（`editingSession.ts:84-86`）：因为值类编辑不重算 entityMap → `getEditingObjectRes` 不被调。`useEntityToGraph` 用 `isEdited` 决定 layout 的 `queryKey`/`staleTime`（`useEntityToGraph.ts:99-101`）；值类不改拓扑，layout 用 5min 缓存是**正确**的，不是漏刷。修了反而会让几十表单输入触发 layout 重算。
- **`replaceEditingObject` 不更新 `originalEditingObject`**（`editingSession.ts:220-226`）：保留脏比较基准。Chat/AddJson/funcClear 写入后 isEdited 必为 true（写入对象 ≠ 会话原始快照），保存后 refetch → `maybeReset` 真 reset 重置基准。语义自洽。
- **就地变异 `editingObject`**：方案 C 的核心，性能契约 2 的基石。不可改不可变更新（详见分析文档 §5.0）。
- **`subscribe`/`getStructureVersion` 是实例箭头函数属性**：为绑定 `this` 且引用稳定，满足 `useSyncExternalStore` 契约。

---

## 建议落地顺序

1. **P0#3**（注释 removeQueries 选型理由）——零风险，纯文档，最先做。
2. **P1#4 + P0#2**（补 `updateFormValues` 测试，固化 `$impl` 早退）——一起做，测试即契约。
3. **P1#5**（`pasteStruct` 深拷贝测试）——防回归。
4. **P0#1**（holder 分屏表述澄清）——改文档措辞。
5. P2#6 / #7 / #8 按需，观察性能后再定。
