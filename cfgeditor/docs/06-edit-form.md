# 06 编辑表单

编辑表单把 schema 的字段结构渲染成 antd Form，让用户在节点上改字段值。核心是 `EntityForm` 无状态容器 + `FieldRenderer` 六类分发 + 值回写双轨接入 EditingSession。

> **不讲**：内嵌字段机制（→ [07](07-embedding.md)）、EditingSession 内部（→ [03](03-editing-session-undo.md)）。本文只讲「schema → form 渲染 + 值回写」。
>
> 【承前】05 的 editable 节点交互 + 03 EditingSession 的写入（`recordEditEntityCreator` 桥接 ③↔⑥）。　【启后】字段里的「内嵌」结构 → [07](07-embedding.md)。

---

## 一、EntityForm：无状态容器

[`EntityForm.tsx`](../src/flow/edit/EntityForm.tsx) 是 memo 的 antd `<Form>` 容器，把 `edit.fields` 拆成多个 `<FieldRenderer>`。**自身不维护任何状态**——所有状态在 EditingSession（03）。容器只做两件事：遍历 `edit.fields` 渲染 `<FieldRenderer>`，以及在 antd `onValuesChange(changed, allValues)` 回调里把值回写给 EditingSession。

- **值回写走 `onValuesChange(changed, allValues)`** → `edit.editOnUpdateValues` → `session.updateFormValues`（03）。表单自己不存值。
- **提交（funcSubmit）不走组件级路由**：`alt+s` 由 CfgEditorApp 全局单点注册，直达 `getCurrentEditingSession().submit()`。会话复制独立提交快照，在途忽略重复提交；失败释放后可重试，编辑和 undo 保留（[03 §十](03-editing-session-undo.md)）。全局单点的动机与历史 bug 详见 [09](09-cross-cutting.md)。
- **`initialValue` 放在每个 `Form.Item` 里**：因 `Form.Item.initialValue` 仅首次注册生效，父级 `key={field.name}` 复用实例时新值被忽略（antd issue #56102），需命令式同步（§5）。

---

## 二、recordEditEntityCreator：③↔⑥ 桥

[`recordEditEntityCreator.ts`](../src/features/record/recordEditEntityCreator.ts) 把 `EditingSession.editingObject`（共享引用）递归包装成 `EditableEntity` 树，并产出每个节点的 `EntityEditField[]`（含内嵌字段判定与构造）。这是 03 EditingSession ↔ 06 表单 的桥：

- 表单吃 `EntityEdit`（`entity.edit`），遍历 `edit.fields` 渲染。
- `EntityEditField` 中的函数闭包（`func` / `interfaceOnChangeImpl` / 各 `editOnXxx`）吃 EditingSession 方法 + `fieldChain` 路径——表单触发闭包 → EditingSession 执行。

（只读对照 `recordEntityCreator.ts`，浏览视图链路，不属于本篇。）

---

## 三、FieldRenderer：六类 switch 分发

[`FieldRenderer.tsx`](../src/flow/edit/FieldRenderer.tsx) 按 `field.type` switch 分发到对应组件。六类字段组件（[`fields/`](../src/flow/edit/fields/)）：

| `field.type` | 组件 | 场景 |
|---|---|---|
| `primitive` | PrimitiveFormItem | bool / int / long / float / str / text 单值；配置了 autoCompleteOptions 的列（外键 / impl 名等）变 Select / CustomAutoComplete |
| `arrayOfPrimitive` | ArrayOfPrimitiveFormItem | `list<primitive>`、map entry list（antd `Form.List` + `ArrayItemExpandButton`）|
| `interface` | InterfaceFormItem | Select 选 impl + 递归展开当前 impl 字段 |
| `structRef`（无 `embeddedField`）| StructRefItem | 子节点引用占位：Tag 名 + source Handle（真子节点是另一个 node）|
| `structRef`（有 `embeddedField`）| EmbeddedSimpleStructuralItem | 内嵌压缩态（**07 讲**）|
| `funcAdd` | FuncAddFormItem | `list<struct>` / `list<interface>` / map 的「+」添加 |
| `funcSubmit` | FuncSubmitFormItem | 仅根 STable 的提交 / 重置 |

structRef 分支前先判 `field.embeddedField`，命中则改走 `EmbeddedSimpleStructuralItem`——这就是 07 的入口。

---

## 四、interface 递归：注入 `renderField` 打破循环依赖

`InterfaceFormItem` 的 `implFields` 要复用 FieldRenderer 的分发逻辑，但 `FieldRenderer ↔ InterfaceFormItem` 直接 import 会循环。解法：父 FieldRenderer 把 `renderField` 回调注入 InterfaceFormItem，interface 递归调这个回调（而非 import FieldRenderer）。

注释强调：**key 落在组件边界**（`EntityForm` 的 `edit.fields.map` 与 `InterfaceFormItem` 的 `implFields.map`），而非函数式渲染那样每次重渲产生新 element、靠子组件 memo 比较运气。

---

## 五、值回写双轨

`onValuesChange` 给 EditingSession **两份数据**：

- `allValues`：整张表安全写回 `editingObject`（数组也安全）。
- `changed`：增量，喂给 coalescing 识别「值类编辑（同字段连续键入合并）还是 Form.List 长度变化（结构步）」（03 §8）。

→ 这就是 03 值类 / 结构类二分在表单侧的入口。

---

## 六、useSyncFieldValue：命令式同步 + 守门

[`useSyncFieldValue.ts`](../src/flow/edit/shared/useSyncFieldValue.ts) 把外部 `field.value` 命令式同步进 antd Form 内部。机制：

```
useSyncFieldValue(form, field):
  effect 依赖 [form, field]（整个 field 引用，非仅 field.value）
    每次 field 引用变 → 比较 form.getFieldValue(name) 与 field.value
      相等      → 跳过（避免无条件 forceUpdate）
      不相等    → setFieldValue(name, field.value)
```

**为什么不用 antd `Form.Item.initialValue`**：initialValue 仅在字段**首次注册**时生效；父级 `key={field.name}` 复用实例时（impl 切换后同 key 复用）新 initialValue 被忽略，必须命令式 `setFieldValue`（antd issue #56102）。

两个守门决策：

- **比较后再 set（`getFieldValue !== field.value`）**：`@rc-component/form` 的 `setField` 对路径匹配字段**无条件 forceUpdate**（不比值），全量 set 会让所有 form item 多一次重渲；比较后挡掉未变字段——结构编辑只重渲真正变化的字段，不会「所有 form item 全刷」。
- **依赖整个 `field` 引用而非仅 `field.value`**：entityMap 重算时每个 field 都是新对象（哪怕值没变），保证 undo/redo 后 effect 必跑、重新评估同步需求；配合上一条比较守门不会引入额外重渲。

**值类 undo 是必须 set 的场景**：值类编辑不重算 entityMap（见 03 值类 / 结构类二分），`field.value` 快照停在旧值、而 antd Form 内部已被用户输入改到新值；undo 让 `editingObject` 回旧值、entityMap 重算出 `field.value=旧值`，此时 `getFieldValue(新) !== field.value(旧)` → set，同步成功。

> `setFieldValue` by design 不触发 `onValuesChange`，不会回流污染 coalescing 栈。

---

## 七、primitiveControl：基本类型控件工厂

[`primitiveControl.tsx`](../src/flow/edit/shared/primitiveControl.tsx) 按 `eleType` 与 `autoCompleteOptions` 产出控件：Select / CustomAutoComplete / Switch / InputNumber / Input.TextArea。

textarea **去掉 `autoSize`**：rc-textarea 的 ResizeObserver 随节点数放大成 rAF 风暴，是 `/edit/record/` 大图加载卡顿根因；改固定 `rows`（str rows=1 滚动、text rows=4）→ 无 ResizeObserver。

`long` 使用 `InputNumber.stringMode`，值与 undo 快照保留十进制字符串，list、map 的 key/value 和 struct 键采用同一约定。提交时 `encodeEditorRecord` 按 schema 检查 64 位范围；`/schemas.editorValueEncoding == 'long-decimal-string'` 表示服务支持精确协议。旧服务的安全 number 仍可读取/提交；旧服务无法提交超出 JavaScript 安全整数范围的 long，已不安全的 number 一律拒绝，避免未改值保存时静默舍入。服务与前端应共同升级；旧前端不能保证编辑新协议的字符串 long。

精确 `long` 校验和下一个主键计算需要支持 `BigInt` 的 WebView/浏览器。构建语法目标为 Windows `chrome105`、其他平台 `safari14`，不提供 BigInt polyfill；这些目标与构建通过不代表已验证 macOS/Linux 的实际 WebView 运行。

`StructRefItem` 的 handle 颜色用 antd `colorPrimary` 而非硬编码（[useRefItemStyles.ts](../src/flow/edit/shared/useRefItemStyles.ts)，`theme.useToken()` 取色）——原硬编码 `#0000ff` 过饱和，换品牌主色给字段级 handle 统一的「可连接点」高亮。

### 原始 JSON 导入与载入表单

[`AddJson.tsx`](../src/features/add/AddJson.tsx) 的直接提交与「载入表单」都先将**原文**发给 `/checkJson`，成功后才由 `parseCheckedEditorRecord` 解析服务端规范化结果并按 schema 检查。浏览器不能先 `JSON.parse` 原文：不安全整数和高精度小数可能已被舍入，之后再检查或转字符串无法恢复。服务端保留十进制精度，拒绝 long 的 `1e-400`、`4.200000000000000000001e1` 等非整数，合法 `1e3` 则规范化为字符串 `"1000"`。

`/checkJson` 支持裸 JSON 对象与原有 AI JSON 围栏；裸对象内字符串的反引号保持原样。此导入流程要求前后端一起升级，校验失败或旧服务不支持裸对象时直接报错，不回退到本地解析。载入表单等待校验期间若切换会话或修改了当前数据，也会拒绝替换，避免覆盖新编辑。

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
