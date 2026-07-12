# Undo/Redo 性能测试方案（release 实测）

> P0 undo 已实现（快照栈 + 值类 coalescing + UI 接入），单测 298 全过，tsc/oxlint 全过。
> 本文为 [`undo-redo-设计.md`](./undo-redo-设计.md) §7 四项 spike 的 release 实测方案。
>
> **为什么不在 dev 测**：dev 模式 longtask 不准——bundle 大、HMR、React Profiler 绝对耗时三重膨胀（见 [`perf-optimization.md`](./perf-optimization.md) 与 memory「性能测量陷阱」）。绝对成本须 release build 直测（PerformanceObserver longtask 是金标准）。

---

## 1. 核心结论（设计文档 §3.2）

**P0 undo/redo 走 `bumpStructure`，触发 entityMap 全量重算 → N 个 EntityForm 重渲（非 mount）。这个成本等于一次结构编辑（fold 切换 / 数组增删），不是快照栈特有的、也不是 undo 引入的。** 命令模式、JSON Patch 的结构 undo 同样要走 `bumpStructure` 全量重渲——卡顿根源是 entityMap 全量重算模型，换 undo 实现解决不了。

所以：
- 若现有结构编辑（fold/add）在最大 record 上不卡 → undo 也不卡（同路径）。
- 若深层 record 结构编辑卡（perf :71 fold 切换全量重建债）→ undo 也卡，触发 §3.4 entityMap 引用稳定化（一举两得修 perf :71）。

---

## 2. 测试环境

1. **release 构建**：`pnpm tauri build`（exe 在 `src-tauri/target/release/`），或 `pnpm build` 后 `pnpm preview` 静态 serve（生产 bundle，比 dev 准）
2. **后端**：`java -jar cfggen.jar -datadir example/config -gen server`（localhost:3456）
3. **最大 record**：`achievement`（24 records）或含长数组字段的深层 record

---

## 3. PerformanceObserver longtask 脚本（金标准）

打开应用后在 DevTools Console 执行：

```js
window.__longtasks = [];
new PerformanceObserver(list => {
    for (const e of list.getEntries())
        window.__longtasks.push({duration: Math.round(e.duration), name: e.name, startTime: Math.round(e.startTime)});
}).observe({entryTypes: ['longtask']});
// 操作后读 window.__longtasks（duration > 50ms 即 longtask）
```

---

## 4. §7 四项 spike

| # | 测试 | 步骤 | 预期 |
|---|---|---|---|
| 1 | 结构 undo 重渲 longtask | 进编辑态 → add array item → `ctrl+z` → 读 longtask | <50ms（重渲非 mount） |
| 2 | 值类 undo 重渲 longtask | 键入 primitive → `ctrl+z` → 读 longtask | <50ms（P0 走 bump） |
| 3 | Form.List 同步 | undo 含 Form.List 增删的 record → 断言行数与值正确刷新 | 行数/值正确；若 antd Form.List 不响应 `setFieldValue` → 需 `key={structureVersion+fieldChain}` 强制 remount |
| 4 | Tab 遍历 clone 累计 | 最大 record 上 Tab 遍历 N 字段 → 读值类组关闭的累计 longtask | 无累积 longtask |

---

## 5. 触发优化条件（>50ms）

- **spike 1/2 结构 undo 卡** → §3.4 entityMap 引用稳定化（`createThis` 复用未变节点 Entity，只重建变化子树）→ FlowNode memo 不击穿 → 一举两得修 perf :71 fold 切换债
- **spike 2 值类 undo bump 卡**（仅值类 undo 卡，结构 undo 不卡） → §3.3 form registry 轻量化（P1：EntityForm 注册 Form 实例，undo 时 `setFieldValue` 绕过 bump，零重渲）
- **spike 4 Tab 遍历累计卡** → lazy snapshot（组关闭只记 marker，undo 时才 clone）

---

## 6. P0 现状（截至 2026-07-13）

- **单测 298 全过**：栈语义 / 分叉 / maxDepth 封顶 / coalescing 同字段合并 / per-key O(1) 不变量 / Form.List 长度 diff / undo·redo / dispose / beforeStructuralChange / onCommitSuccess 清栈
- **tsc + oxlint 全过**
- **dev 应用启动正常**（localhost:1420 + 后端 3456，combobox 加载 table 列表正常）
- **release 实测待手动执行**（按上方四项 spike；dev smoke 因 antd combobox 选项交互 + dev longtask 膨胀未跑完，留 release）

---

## 7. 升级路径不变

`captureUndoPoint` / `applyUndoPoint` 是升级契约点（设计文档 §2.3）。P1.5 若内存仍是问题，换 JSON Patch 只动这两个方法体，上层不动。
