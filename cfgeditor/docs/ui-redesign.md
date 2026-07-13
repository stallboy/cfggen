# cfgeditor UI 重构设计文档
### 范围：`routes/headerbar` · `routes/search`(Finder) · `routes/setting` · `routes/add`

> 本文档不是"把组件换一换"的清单，而是一次**信息架构与职责边界的重新梳理**。
> 先讲清楚每个区域"为什么现在别扭"（诊断），再给出"应当如何组织"（理念），最后落到"具体怎么做"（做法 + antd v6 代码）。
> 所有 antd API 已用 `@ant-design/cli` 针对 **v6** 核实，非凭记忆。

---

## 0. 一句话总目标

让**顶栏**只回答 **"我在哪 + 此刻高频要做什么"**；让**侧栏**按意图切换面板，分别回答 **"什么和我相关"(发现) / "怎么写入数据"(添加) / "应用该怎么表现"(设置)** ——
区域边界清晰、生效模型一致、布局没有补丁，且**低频与危险操作一律收进深入口**、不占顶栏。

---

## 1. 现状诊断：五个根本问题

### 问题 ❶ 避让散落：浮层遮挡后，每个文字面板自己挖坑

`HeaderBar` 是**有意的浮层**（`position:absolute; zIndex:1`，让画布占满全屏，详见 §3）。浮层本身没错，错在**被遮挡的文字面板各自挖坑避让**，且数值还不一致：

```tsx
// Finder.tsx / Chat.tsx 里：height: 32
// Setting.tsx 里：    height: 16   ← 连避让量都不统一
return <>
    <div style={{height: 32}}/>   // ← 纯粹为了不被 HeaderBar 遮住
    ...
```

这是"避让责任散落到 N 个子页面"：新增面板要记得加 px、还得猜加多少，忘了就遮挡。

### 问题 ❷ HeaderBar 职责过载，五类语义挤一行

当前顶栏一个 `<Space>` 里塞了 5 种完全不同的语义，且无视觉分区：

| 语义 | 当前实现 | 类别 |
|---|---|---|
| 切换左侧面板 | `Dropdown`（AppstoreOutlined） | 视图配置 |
| 选表 / 选记录 | 两个独立 `Select` | **定位**（我在哪） |
| 未保存 / 下一个 ID | 裸 `<Text>` | **状态标记** |
| 未引用记录 | `Button` + `<Tag>{count}</Tag>` | **数据动作** |
| 前进 / 后退 | 两个 `Button` | **历史导航** |

用户无法一眼读出"我在哪、我能去哪"。问题不在"挤"，而在"**没有按问什么来分组**"。

### 问题 ❸ Finder 用 `Table` 画导航列表，且四处重复

`LastAccessed` / `LastModified` / `RefIdList` / `SearchValue` 是**同一个模式**——"一组可点击条目，点了跳转"，却各自手写一份 `<Table showHeader={false} columns={...} />`，`columns` 的 `render` 里都是 `<Button type="link" onClick={navigate}>`。

`Table` 是为**数据密集型表格**（排序/筛选/固定列/虚拟滚动）设计的组件。用它画纯导航列表是"杀鸡用牛刀"，且四份高度相似的列定义是**复制粘贴债**。

### 问题 ❹ Setting 是个"杂物间"

打开 `Setting.tsx` 的 Tabs，顺序是：`basic`(一堆深度/节点数 InputNumber) → `recordShow`(布局/颜色) → `theme`(主题文件，**还偷偷嵌了 FlowVisualizationSetting**) → `operations`(大杂烩) → `addJson` → `ai` → `resource`。

三个子问题：

- **归属错位**：`FlowVisualizationSetting`（节点宽/边颜色/布局间距）被嵌在 `ThemeSetting`（主题 JSON 文件）里。这两件事毫无关系。
- **operations 是动词垃圾桶**：一个 tab 里同时有「连服务器(设置) / 切页面(导航) / 固定页(配置) / 导出 PNG(操作) / 删除记录(危险) / 全屏(视图) / 快捷键(参考)」——七个不同意图。
- **生效模型不一致**：`BasicSetting`/`FlowVisualizationSetting` 用 `onValuesChange`（改即生效）；`AiSetting`/`NodeShowSetting`/`TauriSetting`/`ThemeSetting`/`FixPages` 用 `onFinish`（要点保存）。用户**不知道改完要不要点按钮**。

### 问题 ❺ Add(Chat / AddJson) 归属错位 + 实现重复

- `AddJson` 出现在 **Setting 的 tab** 里（它是"写数据"，不是"设置"）。
- `Chat` 出现在 **dragPanel** 里。
- 两者其实都是"**给当前表生成/导入一条记录**"的入口，却散落在两处，且各自重复了一遍 `editable` 判断、`getCurrentEditingSession()` 写入逻辑。
- `Chat.tsx` 把整个 `styles` 对象**内联在组件函数体里**，每次 render 重建（含 8 个 CSSProperties）。

---

## 2. 设计理念（五条原则，带教学）

### 原则 ① 职责按"问什么"划分（The Question Principle）

不要按"这是什么组件"分，要按"用户在这里想问什么"分：

| 区域 | 用户的问题 | 本质 |
|---|---|---|
| **HeaderBar** | 我在哪？此刻高频做什么？ | 定位 + 高频动作（"去哪"由侧栏面板切换 + 历史导航承担） |
| **Finder（侧栏）** | 什么和我相关？ | 探索（最近/引用/搜索） |
| **Setting** | 应用该怎么表现？ | 配置（显示/连接/数据源） |
| **Add** | 我怎么写入数据？ | 写入（AI 生成 / JSON 导入） |

**教学**：当一个 tab/区域让你犹豫"这个该放哪"时，问一句"用户在做这件事时，心里的问题是哪个"——这是主原则。但有一个 **override：频率 + 危险性**。即使某操作语义上属于"当前对象"（如"删除当前记录"是对当前数据的操作），若它**低频且危险**，就应收进深入口（Setting 工具 tab）埋深防误触，而不是摆在记录视图的浅处。"按问什么分组"决定大方向，"频率 + 危险性"决定深浅。

### 原则 ② 一致的生效模型（The Effect Model）

配置项要么"即时生效"，要么"显式提交"，**不能在一个面板里混用而不标注**。规则：

- **即时生效**（`onValuesChange`）：所有"显示偏好"——深度、节点数、颜色、布局、开关。这类的心智是"调一下看看效果"，所见即所得最符合直觉。
- **显式提交**（`onFinish` + 保存按钮）：只留给"有副作用 / 需要校验 / 涉及密钥"的——AI 密钥、服务器地址、主题文件、资源目录。
- **在每个 tab 顶部用一行提示标注生效方式**，消除"改完要不要点保存"的猜测。

**教学**：不一致的生效模型比"没有功能"更糟——因为它让用户**无法建立确定的心智模型**，每次都要试。

### 原则 ③ 组件选型看语义，不看"能不能凑"（Semantic Selection）

| 需求 | 该用 | 现在用（错/次优） | 为什么 |
|---|---|---|---|
| 可点击的条目列表 | **List** | Table(showHeader=false) | Table 是数据网格，导航是 List 的语义 |
| 模式 / 类型切换（少选项 + 高频 + 空间允许） | **Segmented** | Radio.Group button | Segmented 更轻、当前态可见、原生支持 icon。选项多/低频/空间紧时仍用 Dropdown（见 §5） |
| 危险操作前确认 | **Popconfirm** | 直接 mutate | 删除无确认=数据事故 |
| 计数标记 | **Badge** | `<Tag>{n}</Tag>` | Badge 表"数量"，Tag 表"分类" |
| 随时可能用的浮动动作 | **FloatButton.Group** | 顶栏 Button | 不占顶栏，语义即"浮动工具" |
| 只读速查信息 | **Popover** | 常驻 tab / Descriptions | 参考信息不需要常驻 |

**教学**：`Tag` 和 `Badge` 都能显示数字，但 `Tag` 语义是"给事物贴分类标签"，`Badge` 语义是"附着在事物上的计数/状态"。选错不会报错，但会让 UI"哪里都对、哪里都别扭"。

### 原则 ④ 避让责任归一处，不散落到子页面（Layout Owns Its Space）

HeaderBar 是**有意的浮层**（让画布占满全屏，见 §3），它遮挡文字面板顶部是既定代价。问题不在浮层，而在"避让"由谁负责：现状是每个文字面板自己挖一个 `height:32`/`height:16` 的坑——**避让责任散落到 N 个子页面**。正确做法是把避让收敛到**一处**（`SidePanelShell` + `HEADER_HEIGHT` 常量），子面板不再关心 header 有多高。

**教学**：这是"责任归属"问题。浮层把"给自己腾位置"的责任转嫁给了 N 个子页面，耦合点从 1 变成 N。收敛到一处后耦合点归 1，新增面板套个 Shell 即可，魔法数也随之消失。

### 原则 ⑤ 聚合相似意图（Cohesion by Intent）

Chat（AI 生成）和 AddJson（手动粘贴）是同一意图——"往当前表加一条数据"——的两种手段，应并列在同一个"添加数据"区域，用 Segmented 切手段，而不是一个塞 Setting、一个塞 dragPanel。

---

## 3. 全局：保留 HeaderBar 浮层，把"避让"收敛到一处（P0，最高优先，最低风险）

### 先认清现状：浮层是有意设计，不是 bug

`content`(Splitter / FlowGraph) 是 `position:absolute; height:100vh; width:100vw`，从 (0,0) 铺满整个视口；`HeaderBar` 内层 `position:absolute; zIndex:1` 浮于其上。**这是为了让画布占据全部空间（含 header 区域）、HeaderBar 作为 overlay 叠加，从而最大化 FlowGraph 的可用面积。这个设计目标正确，不动。**

```tsx
// CfgEditorApp.tsx —— 保持
const contentDivStyle = {position: "absolute", height: "100vh", width: "100vw", ...};

// HeaderBar.tsx —— 保持浮层
const SPACE_STYLE = {position: 'absolute', zIndex: 1};
```

### 真正的问题：避让散落在每个文字面板里

浮层的唯一副作用是**文字类侧栏面板**会被 overlay 遮住顶部，于是它们各自挖坑避让，且不一致（Finder/Chat 用 `height:32`、Setting 用 `height:16`）。这是"浮层的债让子页面还"——新增面板得记得加 px、还得猜加多少。

### 改法：区分两类面板，避让收敛到一处

| 侧栏内容 | 是否避让 header | 原因 |
|---|---|---|
| **画布类**（RecordRef、固定页、右侧主画布——都是 FlowGraph） | **不避让** | 可被 overlay 遮，保持全屏，最大化画布 |
| **文字类**（Finder、Chat、Setting） | **避让** | 有内容不能被遮，顶部下移 `HEADER_HEIGHT` |

```tsx
// 一处定义
export const HEADER_HEIGHT = 40;

// 文字类侧栏统一外壳：顶部固定占位（避让浮层 header）+ 下方独立滚动
// ⚠️ 别用 paddingTop——它在 overflow 容器里会随内容滚走，导致内容滚到 header 下面。
function SidePanelShell({children}: {children: React.ReactNode}) {
  return (
    <div style={{height: '100%', display: 'flex', flexDirection: 'column'}}>
      <div style={{flexShrink: 0, height: HEADER_HEIGHT}}/>      {/* 固定占位，不参与滚动 */}
      <div style={{flex: 1, minHeight: 0, overflow: 'auto'}}>{children}</div>
    </div>
  );
}

// CfgEditorApp.tsx —— 文字类套 Shell，画布类不套
if (dragPanel === 'finder')  dragPage = <SidePanelShell><Finder schema={schema}/></SidePanelShell>;
else if (dragPanel === 'chat')    dragPage = <SidePanelShell><Suspense fallback={null}><Chat .../></Suspense></SidePanelShell>;
else if (dragPanel === 'setting') dragPage = <SidePanelShell><Suspense fallback={null}><Setting .../></Suspense></SidePanelShell>;
// recordRef / 固定页 = FlowGraph，不加 Shell → 保持全屏被 overlay 遮 ✅
```

然后**删除** `Finder.tsx` / `Chat.tsx` / `Setting.tsx` 里各自的顶部占位 `<div style={{height:32}}/>` / `<div style={{height:16}}/>`。

- HeaderBar 浮层与 `contentDivStyle` 的绝对定位**全部保留**。
- 魔法数 32/16 消失，统一为 `HEADER_HEIGHT` 常量。
- 新增文字面板只需套 `SidePanelShell`；新增画布面板天然不受影响。
- 可选优化（非必须）：把 overlay 写得更直白——根容器 `position: relative`，HeaderBar `position:absolute; top:0; left:0; right:0; z-index:10`。当前靠"外层 relative 高度坍缩 + 内层 absolute"也能工作。

> **为什么 P0**：零行为风险（不动 FlowGraph 全屏、不动浮层），却消灭了散落的魔法数 hack 和"新面板要记得加 px"的隐性坑。**先做这步**。

---

## 4. HeaderBar 重构：两段分区（P2）

### 设计取舍：顶栏只放"高频 + 当前状态相关"

页面类型切换、全屏、快捷键速查都是**低频**操作（页面切换还有 `alt+1~4` 快捷键兜底），故意的 deeper 入口——留在 Setting 的"工具"tab（见 §7.1），**不占顶栏**。顶栏只留两类：
- **定位**（我在哪）：表/记录选择 + 状态标记
- **高频动作**：未引用入口、历史前进/后退

### 目标布局

```
┌──────────────────────────────────────────────────────────┐
│ [表▾ 记录▾]•unsaved  nextId            [未引用³] [‹ ›]    │
│  ← 定位(我在哪) →                      ← 高频动作/历史 →  │
└──────────────────────────────────────────────────────────┘
```

两段用 `Flex justify="space-between"`。

### 4.1 左段：定位（我在哪）

把"选表 + 选记录"从两个孤立 Select 收成一个**视觉整体**（它们本质是一个"路径"）：

```tsx
import {Space, Badge} from 'antd';

{/* 未保存 = 附着在定位条右上角的小圆点。Badge 必须包裹子元素圆点才附着；
    dot=false 时仅作透明包裹（不渲染圆点），所以无条件包裹即可 */}
<Badge dot={!!unsavedSign} status="warning" offset={[-4, 0]}>
  <Space.Compact size="small">
    <TableList schema={schema}/>          {/* 内部 Select，去自带 width，由 Compact 分配 */}
    <IdList curTable={curTable}/>
  </Space.Compact>
</Badge>

{/* nextId：从裸 Text 改成 Tooltip + 可复制小标签 */}
{nextId && <Tooltip title={t('nextSlot')}><Typography.Text copyable type="secondary">{nId}</Typography.Text></Tooltip>}
```

要点：
- `Space.Compact` 把两个 Select 视觉缝合成"一条定位条"，比并排两个独立 Select 更像"路径"。（`Space.Compact` v6 有 `block`/`direction`/`size`）
- **未保存**用 `Badge dot`（status="warning"）比 `<Text>unsaved</Text>` 更轻、更"状态化"。
- 顶栏只放"全局定位"。表内/记录内的状态（编辑中等）归记录视图。

### 4.2 右段：高频动作 + 历史

页面切换、全屏、快捷键都已收进 Setting（低频，见 §7.1），顶栏右段只留**和当前上下文强相关、且高频**的动作：

```tsx
<Space size="small">
  <UnreferencedButton curTable={curTable}/>   {/* 内部 Tag→Badge，见 §4.3 */}
  <Button.Group>
    <Button icon={<LeftOutlined/>}  onClick={prev} disabled={!canPrev}/>
    <Button icon={<RightOutlined/>} onClick={next} disabled={!canNext}/>
  </Button.Group>
</Space>
```

> 历史导航（`alt+c`/`alt+v`）高频且和"当前位置"强绑定，留顶栏；页面切换/全屏/快捷键是低频，回 Setting。

**（可选）历史导航改 FloatButton.Group**：历史导航是"随时可能用、但不常驻视线"的浮动动作，若想进一步释放顶栏，可改用 `FloatButton.Group`（v6 已核实：`trigger`/`placement`/`shape`；子项 `disabled` 需 6.4.0+）挂在右下角与 `BackTop` 同组。保守起见先用 `Button.Group`，两者不冲突，可后置评估。

### 4.3 UnreferencedButton：Tag → Badge

```tsx
// 现在
<Button size="small">{t('unreferenced')} <Tag color="default">{count}</Tag></Button>

// 建议：count 作为 Badge 附着在按钮上，或按钮本身用 Badge 包裹
<Badge count={count} offset={[-2, 0]} size="small" overflowCount={99}>
  <Button size="small" onClick={handleClick}>{t('unreferenced')}</Button>
</Badge>
```

> `Badge` 语义是"计数/状态"，`Tag` 语义是"分类标签"。未引用数是计数，用 Badge。`overflowCount` 可设上限（如 99）。

---

## 5. 面板切换（dragPanel）：保留二级菜单，优化内部（P2）

### 设计取舍：收进 Dropdown，不铺开占空间

侧栏面板类型（`finder`/`chat`/`setting`/`recordRef`/`none` + 用户固定页）选项多、切换不算高频。**铺开成 Segmented/图标组会占掉宝贵的顶栏或侧栏顶部空间**——所以保留现在的 `Dropdown` 二级菜单形态：平时只占一个图标位，点开才展开。这个选择是对的，不动它的形态。

### 现状内部的两个小问题

1. **内置面板与用户固定页混在同一扁平菜单**：`finder`/`chat` 和用户自定义的"某固定页"并列，语义不分层，读起来割裂。
2. **当前激活项不够醒目**：虽然传了 `selectedKeys`，但纯文字菜单选中态弱，且 trigger 按钮（`AppstoreOutlined`）本身不暴露"现在是哪个面板"。

### 改法：菜单分组 + trigger 暴露当前态（不增加常驻空间）

```tsx
const menuItems = [
  {type: 'group', label: t('builtinPanel'), children: [
    {key: 'finder',   icon: <CompassOutlined/>,   label: t('finder')},
    {key: 'recordRef',icon: <ApartmentOutlined/>, label: t('recordRef')},
    {key: 'add',      icon: <FileAddOutlined/>,   label: t('addData')},   // 原 chat 入口演进为 AddPanel(AI+JSON)，见 §8
    {key: 'setting',  icon: <SettingOutlined/>,   label: t('setting')},
    {key: 'none',     icon: <CloseOutlined/>,     label: t('closePanel')},
  ]},
  ...(pageConf.pages.length ? [{
    type: 'group' as const, label: t('fixedPages'),
    children: pageConf.pages.map(fp => ({key: fp.label, label: fp.label})),
  }] : []),
];

<Dropdown menu={{items: menuItems, selectedKeys: [dragPanel],
                 onClick: e => setDragPanel(e.key)}} trigger={['click']}>
  <Button icon={<AppstoreOutlined/>} title={t('panelMenu')}>
    {/* 可选：trigger 按钮露出当前面板短名，不点开也知道现在是谁 */}
    {currentPanelLabel && <span style={{marginInlineStart: 4}}>{currentPanelLabel}</span>}
  </Button>
</Dropdown>
```

收益（**不增加常驻空间**的前提下）：
- **分组**：Menu `type:'group'` 把内置 vs 用户固定页分层，心智清晰。
- **当前态可见**：`selectedKeys` 高亮 +（可选）trigger 按钮露出当前面板短名。
- **关闭侧栏**作为一等选项配 `CloseOutlined`，比孤零零的 `none` 明确。

---

## 6. Finder（search）重构（P3）

### 6.1 抽取公共 `NavList`，消灭四份复制粘贴

四个列表（`LastAccessed`/`LastModified`/`RefIdList`/`SearchValue`）是同一模式。抽一个泛型组件：

```tsx
// routes/search/NavList.tsx
import {List, Empty} from 'antd';
import {navTo, useCurPageRecordOrRecordRef, useMyStore} from '@/store/store';
import {useNavigate} from 'react-router';

interface NavListProps<T> {
  items: T[];
  rowKey: (item: T) => string;
  toNav: (item: T) => {table: string; id: string};   // 怎么跳
  renderTitle: (item: T) => React.ReactNode;          // 主标题
  renderExtra?: (item: T) => React.ReactNode;         // 右侧次要信息（TimeAgo / depth）
  empty?: React.ReactNode;
}

export function NavList<T>({items, rowKey, toNav, renderTitle, renderExtra, empty}: NavListProps<T>) {
  const navigate = useNavigate();
  const {curPage} = useCurPageRecordOrRecordRef();
  const {isEditMode} = useMyStore();
  return (
    <List size="small" split dataSource={items} rowKey={rowKey}
          locale={{emptyText: empty ?? <Empty description={false}/>}}
          renderItem={(item) => (
            <List.Item style={{cursor: 'pointer'}}
              onClick={() => navigate(navTo(curPage, toNav(item).table, toNav(item).id, isEditMode))}>
              <List.Item.Meta title={<Typography.Link>{renderTitle(item)}</Typography.Link>}/>
              {renderExtra && <div>{renderExtra(item)}</div>}
            </List.Item>
          )}/>
  );
}
```

每个原组件瘦成"算 items + 传参"：

```tsx
// LastModified.tsx —— 从 ~100 行 Table 配置，瘦成：
<NavList
  items={orderedItems}
  rowKey={i => `${i.table}-${i.id}`}
  toNav={i => ({table: i.table, id: i.id})}
  renderTitle={i => `${i.id}-${i.title}`}
  renderExtra={i => <TimeAgo date={i.lastModified}/>}
/>
```

### 6.2 Table → List 的**诚实权衡**（重要）

`List` 在 v6 **没有原生 `virtual`**（已核实）。当前用 `Table virtual={n>30}` 是为了长列表性能。所以**不要一刀切换 List**，按场景选：

| 列表 | 典型条数 | 建议 |
|---|---|---|
| LastAccessed | 历史去重，几十 | **List** |
| LastModified | 改过的记录，几十 | **List** |
| RefIdList | 引用图，**可能上百** | **保留 Table virtual**（或 List + 限量"显示更多"） |
| SearchValue | 搜索结果，受 searchMax 限制 | **List**（已分页/限量） |

> 这是"按场景选型"原则的直接体现：List 胜在语义和交互，Table virtual 胜在长列表性能。RefIdList 是唯一可能很长的，务实保留 Table 即可，不必为统一而牺牲性能。

`NavList` 可以加一个 `virtual?: boolean` 或直接提供一个 `NavTable` 变体，让 RefIdList 用 Table 版、其余用 List 版，**对外 API 一致**。

### 6.3 Collapse + lock 机制修正

当前 `lock`（锁定 refIdList 跟随当前记录）的控件挂在 **Collapse 公共 header 的 extra**，但它只对 `refIdList` 这一项有意义，挂在公共位置会误导。

改法：**lock 控件移到 `RefIdList` 内部**（一个图标按钮 + Tooltip 说明），Collapse header 回归干净的标题。

顺序与默认展开：建议默认展开 `search`（用户打开 Finder 最常是为了搜），`lastAccessed` 次之；其余折叠。

### 6.4 SearchValue 统一到 React Query

当前 `SearchValue` 用裸 `fetch` + 手动 `setLoading`，和其他三个面板（都用 `useQuery`）不一致，也没有缓存/取消。改成 `useQuery`：

```tsx
const {data, isFetching, error} = useQuery({
  queryKey: ['search', value, searchMax],
  queryFn: ({signal}) => fetch(/* ... */, signal).then(r => r.json()),
  enabled: value.length > 0,
});
```

收益：loading/error 统一、切走自动取消、天然防重复。

---

## 7. Setting 重构（P1，改动最值得做）

### 7.1 重新分组：按"意图"重组 tab

| 新 tab | 内容（来源） | 生效模型 |
|---|---|---|
| **显示 Display** | `NodeShowSetting`(布局/颜色) **+ `FlowVisualizationSetting`**(节点尺寸/边/间距) | 即时 |
| **行为 Behavior** | `BasicSetting`(各类深度/节点数/开关) | 即时 |
| **连接 Connection** | 服务器地址 **+ `AiSetting`**(baseUrl/apiKey/model) | 显式提交 |
| **数据源 Resources** | `TauriSetting`(asset/resDirs) —— 仅桌面端 | 显式提交 |
| **主题 Theme** | **仅**主题文件（`FlowVisualizationSetting` 移出） | 显式提交 |
| **固定页 Pinned** | `FixPages` | 显式提交 |
| **工具 Tools** | 页面类型切换（`Radio.Group`→可改 `Segmented`）+ 全屏 + 快捷键速查（`KeyShortcut`）+ 导出 PNG | 即时 / 只读 |

被**移出 Setting** 的（仅一项）：
- AddJson → 与 Chat 聚合为"添加数据"独立侧栏面板（§8）

> **删除当前记录**故意**留在"工具"tab 深处**：低频 + 危险操作就该埋深防误触（再叠加 §7.3 的 Popconfirm）。**不提到记录视图**——浅入口对危险操作是缺点，不是优点。

> **明确归属**：页面切换 / 全屏 / 快捷键 / 导出 PNG 都是低频，**留在 Setting 的"工具"tab**，不占顶栏（顶栏只放高频，见 §4）。
> **关键纠正**：`FlowVisualizationSetting` 从 `ThemeSetting` 拆出归"显示"——它和主题 JSON 文件无关，历史误植。

### 7.2 生效模型一致性

两类 tab 顶部各加一行提示（用 `Alert` type="info" banner 或 `Typography.Text type="secondary"`）：

- 显示 / 行为 tab：`"修改即时生效"`（并改用 `onValuesChange`，删掉 NodeShowSetting 现在的"保存"按钮——它和 BasicSetting 行为不一致）
- 连接 / 数据源 / 主题 / 固定页 tab：保留"保存"按钮，顶部标 `"修改后点击保存生效"`

> 现状里 `NodeShowSetting` 用 `onFinish`（要保存），而 `BasicSetting`/`FlowVisualizationSetting` 用 `onValuesChange`（即时），三者同属"显示偏好"却生效模型不一。统一为即时生效。

### 7.3 删除记录：保留深入口，只补 Popconfirm

当前"删除当前记录"是裸 `Button danger` 直接 `mutate()`，**无二次确认**。**位置（埋在 Setting"工具"tab 深处）是对的，不动**——低频 + 危险操作就该埋深防误触。唯一要补的是执行前的二次确认：

```tsx
// 留在"工具"tab 原位，只把裸 Button 包一层 Popconfirm
<Popconfirm title={t('deleteCurRecordConfirm')} okText={t('delete')} okButtonProps={{danger: true}}
            onConfirm={() => deleteRecordMutation.mutate()}>
  <Button danger icon={<DeleteOutlined/>}>{t('deleteCurRecord')}</Button>
</Popconfirm>
```

> 危险操作的安全靠两层叠加：**深入口**（降低误触概率）+ **Popconfirm**（执行前显式确认）。不是二选一——埋深已经挡掉大部分误触，Popconfirm 再兜底最后一步。

### 7.4 Tabs 写法（v6 已核实）

```tsx
<Tabs
  tabPlacement="start"              // ✅ v6 正确写法（top|end|bottom|start）；tabPosition 已废弃
  items={[
    {key: 'display',  label: <span><EyeOutlined/> {t('display')}</span>,  children: <DisplaySetting/>, destroyOnHidden: true},
    {key: 'behavior', label: <span><SlidersOutlined/> {t('behavior')}</span>, children: <BehaviorSetting/>, destroyOnHidden: true},
    // ...
  ]}
/>
```

> - `tabPlacement`（不是 `tabPosition`）—— 现代码 `tabPlacement='start'` **本身是对的**，文档在此澄清，避免重构时被人"顺手改错"。
> - `items[].label` 是 ReactNode，可带 icon。
> - `destroyOnHidden`（5.25.0+）让非活动 tab 内容卸载、省内存；Setting 里 `TauriSetting`（带资源查询）等有数据获取的 tab 切走时及时卸载，避免后台查询空跑。

---

## 8. Add（Chat + AddJson）重构（P4）

### 8.1 聚合为"添加数据"，Segmented 切手段

两者同意图，并列：

```tsx
// routes/add/AddPanel.tsx
const [mode, setMode] = useState<'ai' | 'json'>('ai');

if (!editable) return <Result title={t('notEditable')}/>;   // 共享守卫，见 8.2

return (
  <>
    <Segmented block value={mode} options={[
      {icon: <RobotOutlined/>, label: t('aiGenerate'), value: 'ai'},
      {icon: <CodeOutlined/>,  label: t('jsonImport'),  value: 'json'},
    ]} onChange={setMode}/>
    {mode === 'ai'  ? <Chat schema={schema}/>  : <AddJson schema={schema}/>}
  </>
);
```

这个 `AddPanel` 作为一个**独立侧栏面板**（dragPanel 新增 `'add'` 类型），和 Finder/Setting 平级——它是"对当前表写入数据"的主入口，地位对等。

### 8.2 抽 `useIsCurTableEditable()` hook

Chat 和 AddJson 现在各写一遍 `editable` 判断：

```tsx
// routes/add/useEditable.ts
export function useIsCurTableEditable(schema?: Schema): boolean {
  const {curTableId} = useLocationData();
  return !!(schema?.isEditable && schema.getSTable(curTableId));
}
```

两处复用，删重复。

### 8.3 Chat 的 styles 外提到模块级

```tsx
// 现在：styles 在组件函数体内，每次 render 重建 8 个对象
// 改为：用 createStyles / 模块级常量 + token
import {createStyles} from 'antd-style';

const useStyles = createStyles(({token}) => ({
  container: {display: 'flex', flexDirection: 'column', height: 'calc(100vh - 48px)', background: token.colorBgContainer},
  // ...
}));

// 组件内
const {styles} = useStyles();
```

> 现状每帧重建 styles 对象是无谓开销，且与项目"用 antd-style 管理 CSS-in-JS"的约定一致（见 cfgeditor CLAUDE.md）。

### 8.4 AddJson 的 "show" 命名含糊

当前 `show` 按钮做的事是 `replaceEditingObject(JSON.parse(json))`——把 JSON 灌进当前编辑会话。命名 `show`（"显示"）语义不明。改为 `预览到表单` / `loadIntoForm` 并加 Tooltip 说明，避免用户误以为是"预览 JSON 文本"。

---

## 9. antd v6 组件选型速查（本次重构涉及，API 已核实）

| 组件 | 关键 v6 API | 用在哪 |
|---|---|---|
| **Segmented** | `value`/`onChange`/`options`(项含 icon/label/value/tooltip)/`block`/`size`/`shape` | 页面类型切换、面板切换、Add 手段切换 |
| **FloatButton(.Group)** | `icon`/`onClick`/`tooltip`/`type`/`shape`；Group: `trigger`/`placement`/`shape` | 历史导航浮动按钮（可选） |
| **Tabs** | `tabPlacement`(非 tabPosition)/`items`(label 可 ReactNode + icon)/`destroyOnHidden` | Setting 分组 |
| **List** | `dataSource`/`renderItem`/`rowKey`/`split`/`size`；`List.Item.Meta`(title/description/avatar)；**无原生 virtual** | Finder 导航列表 |
| **Space.Compact** | `block`/`direction`/`size` | 表+记录 Select 组合 |
| **Breadcrumb** | `items[]`(项含 title/onClick/menu/href)；children 拼接已废弃 | （可选）表›记录 路径 |
| **Badge** | `count`/`status`/`dot`/`offset`/`overflowCount` | 未保存、未引用计数 |
| **Popconfirm** | `title`/`onConfirm`/`okButtonProps:{danger:true}` | 删除记录二次确认 |
| **Popover** / **Tour** | `content`/`trigger`；`open`/`steps`/`current` | 本方案**未采用**——快捷键速查已留 Setting 工具 tab；Tour 仅适合一次性首引导，不在本方案范围 |

> **避坑**：`tabPosition` 别用（已废弃，用 `tabPlacement`）；List 别指望 `virtual`（没有）；Breadcrumb 别用 children 拼接（用 `items`）。

---

## 10. 增量迁移路线图

每一步**可独立提交、独立验证**，不必一次性大改：

| 阶段 | 内容 | 风险 | 收益 |
|---|---|---|---|
| **P0** | 保留 HeaderBar 浮层 + FlowGraph 全屏；把散落的 `height:32`/`height:16` 避让收敛到 `SidePanelShell`（`HEADER_HEIGHT` 常量），删各面板占位 div | 极低 | 消除隐性坑，干净地基，画布空间不受损 |
| **P1** | Setting 分组重排 + `FlowVisualizationSetting` 归位"显示" + 生效模型标注/统一 + 删除加 Popconfirm | 中（动 tab 结构） | Setting 终于不是杂物间 |
| **P2** | HeaderBar 两段分区（定位 + 高频动作/历史）；低频项（页面切换/全屏/快捷键/导出）收进 Setting"工具"tab；面板切换保留 Dropdown 仅优化分组 | 中 | 顶栏聚焦高频，低频归位 |
| **P3** | Finder 抽 `NavList` + Table→List（RefIdList 保留 virtual）+ SearchValue 用 useQuery + lock 归位 | 中 | 消灭复制粘贴，语义正确 |
| **P4** | Add 聚合（Chat+AddJson，Segmented 切）+ `useEditable` hook + Chat styles 外提 + AddPanel 独立侧栏面板 | 低 | 写入入口聚合 |

建议顺序：**P0 → P1 → P2 → P3 → P4**。P0 必须最先；P1/P2 可并行规划但建议 P1 先（Setting 最乱、收益最大）。

---

## 附录 A：风险与权衡备忘

- **List 无 virtual**：RefIdList 长列表必须保留 Table virtual 或自实现限量/虚拟化，不可盲目换 List。（§6.2）
- **FloatButton.disabled** 在 6.4.0 才加入，给历史按钮做禁用前确认项目 antd 版本 ≥6.4。（§4.2 可选方案）
- **`destroyOnHidden`** 是 5.25.0+，确认项目 antd ≥5.25（v6 默认满足）。（§7.4）
- **dragPanel 新增 `'add'` 类型**：需在 `store` 的 `dragPanel` 类型、`CfgEditorApp` 的面板渲染分支、`HeaderBar` 的面板菜单里同步新增，三处缺一不可。
- **删除记录**：留 Setting"工具"tab 原位（低频危险，故意埋深），仅叠加 Popconfirm；P1 即可完成，不涉及任何视图改动。

## 附录 B：一页纸总览

```
顶栏  = [定位: 表▾记录▾]•unsaved nextId        [未引用³] [‹ ›]
         (我在哪)                                (高频动作/历史)
         ← 只放高频；页面切换/全屏/快捷键 不上顶栏

侧栏(dragPanel) 切换 = [面板菜单 ▾(当前:发现)]   ← Dropdown 二级菜单，内置/固定页分组
                       平时只占一个图标位，不铺开

侧栏内容：
  发现(Finder)  = Collapse{ 搜索 / 最近访问 / 最近修改 / 引用列表(lock在内) }
                   ↳ NavList（List 为主，RefIdList 保留 Table virtual）
  添加数据(Add) = Segmented{ AI生成 | JSON导入 }   ← 聚合，共享 editable
  设置(Setting) = Tabs{
    显示(布局/颜色/节点样式, 即时) | 行为(深度/节点数, 即时) |
    连接(服务器/AI, 保存) | 数据源(资源, 保存) | 主题(文件, 保存) |
    固定页(保存) | 工具(页面切换/全屏/快捷键/导出, 低频)
  }
  ↳ 删除记录 → 留 Setting"工具"tab 深处 + Popconfirm（低频危险，故意埋深）；低频工具同在此 tab（均不上顶栏）
```
