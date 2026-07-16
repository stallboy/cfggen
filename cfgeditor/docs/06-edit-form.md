# 06 编辑表单

编辑表单把 schema 的字段结构渲染成 antd Form，让用户在节点上改字段值。核心是 `EntityForm` 无状态容器 + `FieldRenderer` 六类分发 + 值回写双轨接入 EditingSession。

> **不讲**：内嵌字段机制（→ [07](07-embedding.md)）、EditingSession 内部（→ [03](03-editing-session-undo.md)）。本文只讲「schema → form 渲染 + 值回写」。
>
> 【承前】05 的 editable 节点交互 + 03 EditingSession 的写入（`recordEditEntityCreator` 桥接 ③↔⑥）。　【启后】字段里的「内嵌」结构 → [07](07-embedding.md)。

---

## 一、EntityForm：无状态容器

[`EntityForm.tsx`](../src/flow/edit/EntityForm.tsx):16 是 memo 的 antd `<Form>` 容器，把 `edit.fields` 拆成多个 `<FieldRenderer>`。**自身不维护任何状态**——所有状态在 EditingSession（03）：

```tsx
export const EntityForm = memo(function EntityForm({edit, nodeProps, nodeShow}) {
    const [form] = Form.useForm();
    return (
        <Form {...FORM_LAYOUT} form={form}
            onValuesChange={(changed, allValues) => {
                // allValues 写回 editingObject（完整 array 安全）；changed 喂 coalescing 合并 key + Form.List 长度 diff
                edit.editOnUpdateValues(changed, allValues);
            }}
            style={FORM_STYLE}>
            {edit.fields.map(field => (
                <FieldRenderer key={field.name} field={field} edit={edit} nodeProps={nodeProps} nodeShow={nodeShow}/>
            ))}
        </Form>
    );
});
```

- **值回写走 `onValuesChange(changed, allValues)`**（:29-32）→ `edit.editOnUpdateValues` → `session.updateFormValues`（03）。表单自己不存值。
- **提交（funcSubmit）不走组件级路由**：`alt+s` 由 CfgEditorApp 全局单点注册，直达 `getCurrentEditingSession().submit()`（:19-21 注释）。提交是「当前编辑会话」的全局语义，不是某字段的局部命令——原先每个 EntityForm 各注册 `useHotkeys` 靠 DOM 冒泡「碰巧」命中，是历史多次触发 submit bug 的过设计修复。
- **`initialValue` 放在每个 `Form.Item` 里**（:22-24 注释）：因 `Form.Item.initialValue` 仅首次注册生效，父级 `key={field.name}` 复用实例时新值被忽略（antd issue #56102），需命令式同步（§5）。

---

## 二、recordEditEntityCreator：③↔⑥ 桥

[`recordEditEntityCreator.ts`](../src/features/record/recordEditEntityCreator.ts):26 把 `EditingSession.editingObject`（共享引用）递归包装成 `EditableEntity` 树，并产出每个节点的 `EntityEditField[]`（含内嵌字段判定与构造）。这是 03 EditingSession ↔ 06 表单 的桥：

- 表单吃 `EntityEdit`（`entity.edit`），遍历 `edit.fields` 渲染。
- `EntityEditField` 中的函数闭包（`func` / `interfaceOnChangeImpl` / 各 `editOnXxx`）吃 EditingSession 方法 + `fieldChain` 路径——表单触发闭包 → EditingSession 执行。

（只读对照 `recordEntityCreator.ts`，浏览视图链路，不属于本篇。）

---

## 三、FieldRenderer：六类 switch 分发

[`FieldRenderer.tsx`](../src/flow/edit/FieldRenderer.tsx):28 按 `field.type` switch 分发：

```tsx
switch (field.type) {
    case "structRef":
        if (field.embeddedField) return <EmbeddedSimpleStructuralItem .../>;   // 内嵌压缩态（07 讲）
        return <StructRefItem name comment handleOut bgColor width/>;          // 子节点引用占位
    case "arrayOfPrimitive": return <ArrayOfPrimitiveFormItem field bgColor/>; // list<primitive> / map entry
    case "primitive":        return <PrimitiveFormItem field bgColor/>;        // bool/int/long/float/str/text
    case "funcAdd":          return <FuncAddFormItem ... func={field.value}/>;  // list<struct/interface>/map 的「+」
    case "interface":        return <InterfaceFormItem field renderField={...}/>;  // 选 impl + 递归
    case "funcSubmit":       return <FuncSubmitFormItem field/>;                // 仅根 STable 提交/重置
}
```

六类字段组件（[`fields/`](../src/flow/edit/fields/)）：

| `field.type` | 组件 | 场景 |
|---|---|---|
| `primitive` | PrimitiveFormItem | bool / int / long / float / str / text 单值；配置了 autoCompleteOptions 的列（外键 / impl 名等）变 Select / CustomAutoComplete |
| `arrayOfPrimitive` | ArrayOfPrimitiveFormItem | `list<primitive>`、map entry list（antd `Form.List` + `ArrayItemExpandButton`）|
| `interface` | InterfaceFormItem | Select 选 impl + 递归展开当前 impl 字段 |
| `structRef`（无 `embeddedField`）| StructRefItem | 子节点引用占位：Tag 名 + source Handle（真子节点是另一个 node）|
| `structRef`（有 `embeddedField`）| EmbeddedSimpleStructuralItem | 内嵌压缩态（**07 讲**）|
| `funcAdd` | FuncAddFormItem | `list<struct>` / `list<interface>` / map 的「+」添加 |
| `funcSubmit` | FuncSubmitFormItem | 仅根 STable 的提交 / 重置 |

structRef 分支前先判 `field.embeddedField`（:35），命中则改走 `EmbeddedSimpleStructuralItem`——这就是 07 的入口。

---

## 四、interface 递归：注入 `renderField` 打破循环依赖

`InterfaceFormItem` 的 `implFields` 要复用 FieldRenderer 的分发逻辑，但 `FieldRenderer ↔ InterfaceFormItem` 直接 import 会循环。解法：父 FieldRenderer 把 `renderField` 回调注入 InterfaceFormItem（:81），interface 递归调这个回调：

```tsx
case "interface":
    return <InterfaceFormItem field={field} nodeProps={nodeProps}
        renderField={(f) => <FieldRenderer field={f} edit={edit} nodeProps={nodeProps} nodeShow={nodeShow}/>}/>;
```

注释（:23-27）：**key 落在组件边界**（`EntityForm` 的 `edit.fields.map` 与 `InterfaceFormItem` 的 `implFields.map`），而非函数式渲染那样每次重渲产生新 element、靠子组件 memo 比较运气。注入回调而非 import，打破循环依赖。

---

## 五、值回写双轨（红线 1 接入点）

`onValuesChange` 给 EditingSession **两份数据**（EntityForm.tsx:29-32）：

- `allValues`：整张表安全写回 `editingObject`（数组也安全）。
- `changed`：增量，喂给 coalescing 识别「值类编辑（同字段连续键入合并）还是 Form.List 长度变化（结构步）」（03 §8）。

→ 这就是 03 值类 / 结构类二分在表单侧的入口。

---

## 六、useSyncFieldValue：命令式同步 + 守门

[`useSyncFieldValue.ts`](../src/flow/edit/shared/useSyncFieldValue.ts) 把外部 `field.value` 命令式同步进 antd Form 内部，配合「值类编辑不重渲」契约：

```ts
export function useSyncFieldValue(form: FormInstance, field: {name: string; value: unknown}) {
    useEffect(() => {
        if (form.getFieldValue(field.name) !== field.value) {   // 比较守门
            form.setFieldValue(field.name, field.value);
        }
    }, [form, field]);
}
```

**为什么不用 antd `Form.Item.initialValue`**（:5-7）：initialValue 仅在字段**首次注册**时生效；父级 `key={field.name}` 复用实例时（impl 切换后同 key 复用）新 initialValue 被忽略，必须命令式 `setFieldValue`（antd issue #56102）。

两个守门决策（:8-18 注释）：

- **比较后再 set（`getFieldValue !== field.value`）**：`@rc-component/form` 的 `setField` 对路径匹配字段**无条件 forceUpdate**（不比值，Field.js:239），全量 set 会让所有 form item 多一次重渲；比较后挡掉未变字段——结构编辑只重渲真正变化的字段，不会「所有 form item 全刷」。
- **依赖整个 `field` 引用而非仅 `field.value`**（:26）：entityMap 重算时每个 field 都是新对象（哪怕值没变），保证 undo/redo 后 effect 必跑、重新评估同步需求；配合上一条比较守门不会引入额外重渲。

**值类 undo 是必须 set 的场景**（:11-13）：值类编辑不重算 entityMap（性能契约 1），`field.value` 快照停在旧值、而 antd Form 内部已被用户输入改到新值；undo 让 `editingObject` 回旧值、entityMap 重算出 `field.value=旧值`，此时 `getFieldValue(新) !== field.value(旧)` → set，同步成功。

> `setFieldValue` by design 不触发 `onValuesChange`，不会回流污染 coalescing 栈（:19）。

---

## 七、primitiveControl：基本类型控件工厂

[`primitiveControl.tsx`](../src/flow/edit/shared/primitiveControl.tsx):7 按 `eleType` 与 `autoCompleteOptions` 产出控件：Select / CustomAutoComplete / Switch / InputNumber / Input.TextArea。

textarea **去掉 `autoSize`**（:29-35）：rc-textarea 的 ResizeObserver 随节点数放大成 rAF 风暴，是 `/edit/record/` 大图加载卡顿根因；改固定 `rows`（str rows=1 滚动、text rows=4）→ 无 ResizeObserver。

`StructRefItem` 的 handle 颜色用 antd `colorPrimary` 而非硬编码（[useRefItemStyles.ts](../src/flow/edit/shared/useRefItemStyles.ts):18-27，`theme.useToken()` 取色）——原硬编码 `#0000ff` 过饱和，换品牌主色给字段级 handle 统一的「可连接点」高亮。

---

## 八、Cheat Sheet

**加一种字段类型**：① `entityModel.ts` 的 `EntityEditField` 判别联合加一支 → ② `recordEditEntityCreator` 产出它 → ③ `fields/` 加组件 → ④ `FieldRenderer` switch 加分支。

**字段值写入 EditingSession**：走 `onValuesChange(changed, allValues)` 双轨 → `edit.editOnUpdateValues`（别在组件里直接改 editingObject）。

**外部值同步进 antd Form**：用 `useSyncFieldValue`（比较守门），别用 `initialValue`（impl 切换后失效）。

**interface 子字段递归**：经 `renderField` 回调注入（别 import FieldRenderer，会循环）。

---

## 一句话速记

- **EntityForm 无状态**：值经 `onValuesChange` 双轨（`allValues` 写回 + `changed` 喂 coalescing）→ EditingSession；提交 `alt+s` 全局单点。
- **FieldRenderer 六类 switch**：primitive / arrayOfPrimitive / interface / structRef（含 embeddedField 分支→07）/ funcAdd / funcSubmit。
- **interface 递归**：注入 `renderField` 回调打破循环依赖；key 落组件边界保 memo 稳定。
- **`useSyncFieldValue` 命令式同步**：`initialValue` 在 `key` 复用时失效；比较守门（`getFieldValue !== field.value`）避免全量 forceUpdate；依赖整个 field 引用保 undo/redo 后必跑。
- **textarea 去 `autoSize`**：避免 ResizeObserver rAF 风暴（大图加载卡顿根因）。
