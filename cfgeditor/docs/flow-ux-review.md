# src/flow UX 体验评审

> 范围：`cfgeditor/src/flow/` 全目录（FlowGraph / FlowNode / EntityCard / EntityForm / 各 field / layout / ResPopover / Note 等）。
> 视角：交互体验 + 视觉审美。已通读渲染层全部源码，关键 antd 事实已用 `@ant-design/cli` 核实（项目 antd `^6.5.0`）。
> 分级：**P0**（破坏性/反馈缺失）→ **P1**（高性价比审美/可用性）→ **P2**（可发现性/交互细节）→ **P3**（一致性/微调）。
> 每条都标了 ROI。按"先做高 ROI"的顺序读即可。

---

## TL;DR — 最值得动的 6 件事

| # | 问题 | 位置 | ROI |
|---|------|------|-----|
| 1 | 布局失败时所有节点塌叠在 (100,100) 成一摞，无任何用户提示 | `useEntityToGraph.ts:146-148` + `entityToNodeAndEdge.ts:87` | **极高** |
| 2 | note 区硬编码纯 `yellow`，与整体低饱和配色冲突 | `NoteShowOrEdit.tsx:31-32` | **高**（改一行） |
| 3 | 折叠态用 `8px solid #ffd6e7` 描边，过重且 outline 不进 ELK 估算→与邻居重叠 | `style.css:32-37` | **高** |
| 4 | 节点标题/资源按钮文字恒为 `#fff`，但节点底色可被用户任意配置→低对比/不可读 | `NodeTitle.tsx:12`、`FlowNode.tsx:24` | **高**（可访问性） |
| 5 | 所有图标按钮（折叠/移动/删除/note/资源）零 Tooltip、零 `aria-label` | `NodeToolbar.tsx`、`FlowNode.tsx:76-86`、`NodeNote.tsx` | **高**（可发现性） |
| 6 | 删除节点无二次确认，一次误点即触发 | `NodeToolbar.tsx:29-31` | **高**（破坏性操作） |

> 已核实**不是 bug**：`notification({title})` 在 antd v6+ 合法（`title` since 6.0.0，本项目 6.5.0）。原以为是 `message` 误写，查证后排除。

---

## 架构优点（先肯定，免得下面的建议显得片面）

- 呈现层 / domain 解耦干净：`nodeShow`/`notes` 走 `node.data` 而非盖章到 `entity`，entity 保持不可变、memo-safe。
- 高度估算与渲染**同源**（`calcWidthHeight` 的魔数 ↔ 实际 antd 组件尺寸，note 行高共用 `NOTE_ROW_H`），这是这类图形编辑器里最难得的工程纪律。
- 视口语义有不变量测试兜底（`viewportMath`：relayout 后锚点屏幕坐标不变）。
- ConfigProvider 从"每节点一个"上提到 `FlowGraph` 单实例（45→1），是有意识做的性能优化。

这些底子很好。下面的问题大多是"最后一公里的审美与交互打磨"，不是架构债。

---

## P0 — 破坏性 / 反馈缺失

### 1. 布局失败 → 全部节点塌成一摞，且零反馈 ⚠️

`useEntityToGraph.ts:142-148` 的 error 兜底：

```ts
} else if (layoutError) {
    console.error('[layout] failed, ...', layoutError);
    setNodes(nodes);   // nodes 里每个 position 都是 {x:100, y:100}
    setEdges(edges);
}
```

而 `entityToNodeAndEdge.ts:87` 给**每个**节点的默认 `position` 都是 `{x: 100, y: 100}`。ELK 正常时位置会被 `applyRectToNodes` 覆盖；但一旦 layout throw（abort/dropped_nodes/no_children），兜底直接塞未布局的 `nodes`——**所有节点完全重叠在 (100,100)**，画布变成一摞看不清的卡片，用户除了控制台 `console.error` 外得不到任何提示。

**改法（低成本）：**
- 兂底时给节点做个**就地散开**（按 index 螺旋/网格铺一下，哪怕丑也比一摞强），或至少 `position` 加 `index*20` 偏移；
- 画布上给一个 antd `Result`/`Empty` + "布局失败，点此重试"（重试即 `queryClient.invalidateQueries(['layout', pathname])`）。
- 这是少数我建议**优先做**的，因为它是静默灾难——平时不触发，一触发用户完全不知道发生了什么。

---

## P1 — 高性价比审美 / 可用性

### 2. note 区硬编码纯 `yellow`，刺眼且脱离配色体系

`NoteShowOrEdit.tsx:31-32`：

```ts
const noteStyle = {backgroundColor: "yellow", borderRadius: '8px', whiteSpace: 'pre-wrap'};
const TEXT_AREA_STYLE = {backgroundColor: "yellow"};
```

纯黄 `#FFFF00` 是最高饱和的色之一。整个 flow 的节点底色是精心调过的低饱和青/绿/蓝（`#0898b5`/`#207b4a`/`#006d75`/`#003eb3`，见 `colors.ts:19-25`），note 又挂在节点**顶部**——每个有 note 的节点都顶着一坨亮黄，视觉上像贴了张荧光便利贴，和周围的克制感打架。

**改法（一行）：** 换成 antd 的 warning 系 token色，保留"提醒/批注"语义但不刺眼：
- 展示态背景：`token.colorWarningBg`（≈ `#fffbe6`，浅暖黄）；
- 边框/编辑态可用 `token.colorWarningBorder`；
- 文字保持默认色（现在是默认黑，没问题）。

> 顺便：`noteStyle` 用了 `borderRadius:8px`，而表单 `FORM_STYLE` 是 `borderRadius:15``（constants.ts:29），节点本身 8px。三套圆角，建议统一（见 P3-2）。

### 3. 折叠态 `8px solid #ffd6e7` 描边——过重、且不进布局估算

`style.css:32-37`：

```css
.flowNodeWithBorder {
    outline: 8px solid #ffd6e7;
    ...
}
```

三个问题叠加：
1. **8px 实心描边太重**，视觉上像"错误/告警"状态，而非中性的"已折叠"。粉到发亮的 `#ffd6e7` 进一步放大了这种"警报感"。
2. **`outline` 不占布局空间**，ELK 估算（`calcWidthHeight`）也不知道它。所以折叠节点四周会**和相邻节点/边重叠 8px**——既是审美问题也是布局问题。
3. 同一个 `#ffd6e7` 还兼作展开按钮底色（`FlowNode.tsx:51`），"折叠态指示色"和"按钮色"撞在一起，信息密度反而下降。

**改法：** 选一种更克制的折叠表达，任选其一：
- 细描边 + 虚线：`outline: 2px dashed <editFoldColor>; outline-offset: 2px;`（虚线天然读作"折叠/占位"）；
- 或保留实线但降到 `2-3px`、配色降饱和（如 antd `token.colorPrimaryBg` 或 `#ffe7f0`→更浅）；
- 若想保留"粉色=折叠"的语义，把展开按钮换成图标差异（已经有了 `Shrink`/`ArrowsAlt`）就够了，不必再用大色块喊。

### 4. 节点标题与资源按钮文字恒为白色，但底色可被用户配置 → 对比度失控

`NodeTitle.tsx:12`：`titleTextStyle = { fontSize: 14, color: "#fff" }`
`FlowNode.tsx:24`：`resBriefButtonStyle = { color: '#fff' }`

而节点底色走 `getNodeBackgroundColor`，用户可通过 `nodeColorsByValue` / `nodeColorsByLabel` 配**任意 hex**（`colors.ts:41-59`）。一旦用户配了浅色底（很常见，比如用浅色区分某类节点），白字标题直接糊掉、不可读。这是配色系统开放性与标题硬编码白色的内在矛盾。

**改法（按投入排序）：**
- **最小：** 给 `nodeShow` 增一个"标题色"字段（和 `nodeColor` 同级），默认白，让用户能跟着底色调；
- **更稳：** 在 `getNodeBackgroundColor` 旁加一个 `getReadableTextColor(bg)` 工具，按背景亮度（相对亮度公式）自动返回 `#fff`/`#000`，标题与资源按钮都读它。一次性解决，永远不糊。

> 同类隐患：`EntityProperties` 的字段标签也用节点 `color` 当文字色（`itemKeyStyle.color = color`，`EntityProperties.tsx:45`），但标签底是白 `#fff`，所以是"节点色字 on 白底"——浅节点色时同样会低对比。可一并按"自动反色"解决。

### 5. 字段标签用 `<i>` 包裹 → 默认斜体，且长短标签字体不一致

`LabelWithTooltip.tsx:23-28`：

```ts
function autoSizeName(name: string, autoSize?: boolean) {
    const shouldShrink = name.length >= 9 && autoSize;
    const style = shouldShrink ? {fontSize: "0.75em"} : undefined;
    const Wrapper = shouldShrink ? "span" : "i";   // ← <i> 默认 font-style: italic
    return <Wrapper style={style}>{name}</Wrapper>;
}
```

`<i>` 的 UA 默认样式是 `font-style: italic`。后果：
- 所有编辑表单的字段名（`PrimitiveFormItem`/`ArrayOfPrimitiveFormItem`/`StructRefItem`/`FuncAddFormItem`/`EmbeddedSimpleStructuralItem` 都走 `LabelWithTooltip`）**默认渲染成斜体**；
- 当 `name.length >= 9 && autoSize` 时又切回 `<span>`（非斜体 + 缩小字号）。
- 于是**同一个表单里，短字段名斜体、长字段名正体**，排版割裂。而且 `<i>` 语义是"习语/不同声口"，包字段名属于标签误用。

**改法（一行）：** 统一用 `<span>`；若确实想要斜体效果，显式写 `fontStyle:'italic'` 并对长短标签一致应用。阈值 `>= 9` 也建议改成基于可用宽度而非字符数，但那是 P3 的事。

### 6. 字段级连接点硬编码纯 `blue`

`useRefItemStyles.ts:15-22`：

```ts
const handleOutStyle = {
    position: "absolute",
    left: `${(width ?? DEFAULT_EDIT_NODE_WIDTH) - 10}px`,
    backgroundColor: "blue",   // ← #0000ff
};
```

这些是 `StructRefItem` / `FuncAddFormItem` 右侧的小连接圆点。纯 `#0000ff` 是最饱和的蓝，和节点级 handle（用 `color` 即节点底色，`FlowNode.tsx:72-74`）的处理方式也不一致——节点 handle 会融进底色，字段 handle 却突兀地亮蓝。

**改法：** 让它和节点 handle 同源，`backgroundColor: color`（节点底色）或用 antd `token.colorPrimary`，去掉裸 `blue`。

---

## P2 — 可发现性 / 交互细节

### 7. 所有图标按钮零 Tooltip、零 aria-label

折叠/展开、上移、下移、删除（`NodeToolbar.tsx`）、note 触发（`NodeNote.tsx` 的 `bookIcon`）、资源摘要（`FlowNode.tsx:63-70`）——清一色 `<Button icon={...} />`，既没文字也没 Tooltip，更没 `aria-label`。

- **可发现性：** 书本图标到底代表"查看 note / 添加 note / 详情"？`ShrinkOutlined`/`ArrowsAltOutlined` 哪个是收起？新用户只能瞎点。`getResBrief` 那个按钮（见 #10）更费解。
- **可访问性：** 屏幕阅读器读到的是空。

**改法（高 ROI、低改动）：** 用 antd `<Tooltip>` 包一层（hover 即提示，键盘 focus 也能弹），并补 `aria-label`。示例：

```tsx
<Tooltip title={t('foldNode')}><Button style={iconButtonStyle} icon={foldIcon} onClick={foldNode} aria-label={t('foldNode')} /></Tooltip>
```

> 注意 antd v6 推荐 `<Tooltip><Button/></Tooltip>`（Tooltip 包 trigger），而非当前 `FuncSubmitFormItem.tsx:16-18` 那种 `<Button><Tooltip>{text}</Tooltip></Button>` 的反向写法——后者 Tooltip 只在 hover 文字时触发、按钮内边距区不触发。

### 8. 删除节点无二次确认

`NodeToolbar.tsx:29-31`：点 `CloseOutlined` 直接 `edit.editOnDelete(nodeAnchor(...))`。虽然有 undo 系统，但：
- 一次误点就删（按钮很小、又紧挨上移/下移）；
- 用户不一定知道有 undo、也不一定知道热键。

**改法：** 换成 antd `<Popconfirm>`（`title="确认删除？"`、`okText="删除"` `okButtonProps={{danger:true}}`）。对"移动"这种可逆操作不用加，只给"删除"加即可，避免过度打断。

### 9. 右键菜单位置偏移 -30/-30，且不贴边翻转

`FlowGraph.tsx:63` / `:71`：

```ts
setMenuStyle({top: event.clientY - 30, left: event.clientX - 30});
```

两个体感问题：
1. 菜单出现在光标**左上方 30px**处，而不是光标处——用户右键后要往左上挪一下鼠标才到第一项，违反"菜单出现在点击点"的直觉。
2. 用的是裸 `<div className='contextMenu' style={{position:fixed}}>` + antd `<Menu>`，**没有视口贴边翻转**。在画布右下角右键，菜单直接溢出屏幕外看不到。antd `<Dropdown trigger={['contextMenu']}>` 自带 smart placement，能解决这两个问题。

**改法：** 要么去掉 `-30` 偏移让菜单贴光标，要么干脆改用 `<Dropdown trigger="contextMenu" menu={{items}}>`，把 `FlowContextMenu` 的手卷定位换成 antd 原生能力（顺便拿到键盘导航、a11y）。

**小项：** 菜单不在 `Escape` / 外部 panel 点击时关闭（现在靠 `onPaneClick`/`onNodeClick`/`onMoveStart`/`onNodeDragStart`）。加个 `useEffect` 监听 `keydown Escape → closeMenu` 成本极低。

### 10. note"新增"预填了字面量 `"note："`

`NodeNote.tsx:49-51`（编辑态点书本图标新增 note）：

```ts
const onEditNoteClickInEdit = useCallback(() => {
    setTmpNote({note: "note：", entity});   // ← 真的把 "note：" 当初始内容塞进去
}, [entity]);
```

这不是 placeholder，是**真实内容**：用户点了一下"添加 note"，出来一个预填了 `note：`（中文冒号）的输入框；如果用户不手动清掉就直接提交，`note：` 这几个字会被当成 note 内容写进 json。

**改法：** 留空 `setTmpNote({note: "", entity})`，把提示语交给 `Input.TextArea` 的 `placeholder`（顺带把硬编码英文 `placeholder='note'` i18n 化，见 #11）。

### 11. note 输入框：无 autofocus、placeholder 硬编码英文、无删除入口

`NoteShowOrEdit.tsx`：
- `:109` `placeholder='note'` 是写死的英文，而同文件按钮已经走了 `t('updateNote')`/`t('cancelUpdateNote')`——一个组件里中英混出。
- 编辑器出现后不自动聚焦，用户还得再点一下 textarea 才能打字。`Input.TextArea` 加 `autoFocus` 即可。
- 后端 `updateNote` 空串即 `'deleteOk'`，但 UI 没有"删除 note"按钮——要删只能清空再提交，靠一条 warning 提示推断结果。给个 `DeleteOutlined` + Popconfirm 的显式删除更清楚。

### 12. 资源摘要按钮显示 `2v3a1i`，无图例

`getResBrief.ts` 把资源压缩成 `"2v3a1i"`（v=video / a=audio / i=image / o=other），作为按钮文字渲染在节点标题栏（`FlowNode.tsx:68`）。问题是没有任何 tooltip 解释这串字母的含义——用户第一次看到 `2v3a1i` 完全不知道是什么。

**改法（任选）：**
- 给按钮加 Tooltip："2 个视频 / 3 个音频 / 1 个图片"（把 `getResBrief` 的分类计数顺便喂给 tooltip）；
- 或用图标 + 数字：`🎬2 🔊3 🖼1`，跨语言直观；
- 至少在悬停时给出语义，别让用户猜。

### 13. "在资源管理器中打开"把完整路径当按钮文字

`ResPopover.tsx:163`（default 分支）：

```tsx
content = <Button onClick={() => goExplorer(path)}>{path}</Button>
```

完整文件路径可能很长，按钮会被撑得很宽/换行。应显示文件名、tooltip 给全路径（与 video/audio 分支里用 `resInfo.name` 一致）。

---

## P3 — 一致性 / 微调（低优先，顺手做）

### 1. 混用两套图标风格
`PlusSquareTwoTone`/`MinusSquareTwoTone`（`ArrayOfPrimitiveFormItem`、`FuncAddFormItem`、`ArrayItemExpandButton`）是 TwoTone 实色填充；而 `ShrinkOutlined`/`ArrowsAltOutlined`/`ArrowUp/Down`/`CloseOutlined` 是 Outlined 线条。antd 官方建议同一界面统一一套图标族。建议全 Outlined（TwoTone 在密集节点里偏重）。另外 `ArrayItemExpandButton` 用 `LeftOutlined`/`RightOutlined` 表"展开/收起"也反直觉（惯例是 `Down`/`Right`）。

### 2. 圆角不统一
节点/note/contextMenu 用 `8px`，表单 `FORM_STYLE` 用 `15px`（`constants.ts:29`）。表单嵌在节点里时出现"15 内 8 外"的嵌套圆角不齐。统一一套（建议都 8 或都 10）。

### 3. `Space size={50}` 是脱离 spacing 体系的魔数
`FuncSubmitFormItem.tsx:15`。antd Space 的语义档是 small=8/middle=16/large=24。50 显得按钮间距异常宽，和别处 `Space size={1}`/`size="small"` 也不一致。换 `large` 或 `size="middle"` 即可。

### 4. 搜索高亮 `<mark>` 是蓝字黄底
`style.css:11-14`：`mark { color:#2986ff; font-weight:bold }`（没覆盖背景，保留 UA 默认黄底）。蓝+黄是互补色，对比强但偏躁。搜索高亮保留黄底是好的（扫视友好），但文字色建议用默认/深色而非饱和蓝，更耐看。

### 5. `minZoom={0.1}` 过小
`FlowGraph.tsx:109`。缩到 10% 时节点文字完全不可读，多数应用下限 0.2~0.3。`fitView` 已用 `minZoom:0.3`（`useEntityToGraph.ts:164`），手动缩放可对齐到 0.2。

### 6. 缺 MiniMap / 空状态 / hover 选中反馈（按需）
- 大图（几十~上百节点）没有 `<MiniMap>` 做总览导航；
- `entityMap` 为空时画布只有背景点，无"无数据/无引用"占位；
- 节点无 hover 高亮、选中无可见环（密集图里不易追踪鼠标停在哪）。

这三项**视实际图规模决定**，不一定要做——如果用户的图通常 < 30 节点，MiniMap 就是噪音。先观察再定，别为做而做。

---

## 不建议动的（避免过度设计）

- **高度估算的魔数体系**（`calcWidthHeight.ts`）：看着像"该抽象"，但它和 antd 实测 DOM 尺寸一一对应、且有 `calcWidthHeight.test.ts` 锁算术，抽象成 token 表反而会切断"魔数↔实测"的可追溯性。保持现状。
- **`useNodeNote` 用 hook 吐 JSX**：注释里已经解释过这是 headless-hook 写法（产出一段喂 `<NodeTitle>` 的 prop + 一段自己摆的 DOM），比 render-prop 更精确。不是反模式，别重构。
- **ELK 布局走 worker + 失败 throw 而非 resolve undefined**：语义正确（已在 `layoutAsync.ts` 注释论证）。要改的只是 P0 那个**用户侧兜底展示**，不是这套契约本身。

---

## 建议的落地顺序

1. **P0-1**（布局失败兜底）— 静默灾难，先堵。
2. **P1 批量改色**（#2 note 黄 / #3 折叠描边 / #4 白字对比 / #6 蓝 handle / #5 `<i>`→`<span>`）— 都是局部、互不依赖、肉眼可见的审美提升，适合一个 PR 一起做。
3. **P2 交互**（#7 Tooltip / #8 删除确认 / #9 右键菜单 / #10 note 预填 / #11 note 输入 / #12 资源图例 / #13 路径按钮）— 按用户痛点挑。
4. **P3** — 顺手。

> P1 里 #4（自动反色）和 P2 里 #7（Tooltip 全补）是两个"一次做完全局受益"的杠杆点，优先级可以再往上提。
