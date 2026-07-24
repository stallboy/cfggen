# 01 数据流：URL → API → React Query

> cfgeditor 是瘦前端，所有配表数据来自后端 HTTP 服务（cfggen `-gen server`）。本文讲三层：URL 怎么编码「在看什么」、API 怎么搬数据、React Query 怎么缓存 / 失效 / 取消。
>
> **不讲**：状态分类与 resso / EditingSession（→ [02 状态管理](02-state-management.md)）、目录分层（→ [README §四](README.md)）。本文只盯 URL / API / React Query 这条数据主线。
>
> 【承前】README 的分层地图——`api/` 是最底层。　【启后】数据进来要放进**好几种状态**，不止 React Query → [02](02-state-management.md)。

---

## 一、本质：一次「看一条记录」牵动三层

用户点开一条记录，背后三层性质截然不同的代码在协作。三层职责要分清，否则容易出现「该让 React Query 扛的状态写进了组件、该走 URL 的真值写进了缓存」这类错位：

| 层 | 关注什么 | 本项目落点 | 典型动作 |
|---|---|---|---|
| **URL / 路由** | 「现在在看什么」如何编码进地址栏，可分享 / 刷新 / 前进后退 | [`main.tsx`](../src/main.tsx) `createBrowserRouter`、[`store.ts`](../src/store/store.ts) `navTo` / `useLocationData` | `/record/Item/1001` → 解析出 `curTableId=Item`、`curId=1001` |
| **API（HTTP 客户端）** | 「怎么发请求、怎么解析响应」——纯传输，无业务缓存语义 | [`apiClient.ts`](../src/api/apiClient.ts)（axios） | `fetchRecord(server, table, id, signal)` |
| **React Query（服务端状态）** | 「请来的数据怎么缓存、何时过期、谁重取、取消与并发」 | [`queryClient.ts`](../src/services/queryClient.ts) + 各组件 `useQuery` / `useMutation` | `useQuery({queryKey:['record',...], queryFn:...})` |

**三条判据**：

- API 层**只管搬数据**，不关心调几次、缓存与否——它是无状态工具函数。**不要**在 API 层手写缓存。
- 缓存 / 重取 / 取消是 React Query 的职责，**不要**在组件里手写 loading / error 状态机（让 `useQuery` 的 `isLoading/isError/data` 扛）。
- URL 是「看什么」的真值；服务端缓存是「数据长什么样」的快照。两者解耦：换 URL 触发新查询，缓存命中时不发请求。

> 一句话：**URL 决定查什么，API 决定怎么查，React Query 决定查过的是否还能用。** 三层各司其职，别让任一层越界。

---

## 二、URL 层：把「在看什么」编码进地址栏

### 2.1 路由表

react-router v8 的 `createBrowserRouter`（在 [`main.tsx`](../src/main.tsx)；v7 起包名统一为 `react-router`，`RouterProvider` 从 `react-router/dom` 导入）。根路由 `/` 挂 [`AppLoader`](../src/app/AppLoader.tsx)，子路由**全部 `lazy` 懒加载**——每条 `import()` 一个 feature 组件（Table / TableRef / Record / RecordRef / PathNotFound），按需打包，首屏只加载壳。

URL 形态：

```
/<curPage>/<tableId>/<id>         例：/record/Item/1001
/edit/record/<tableId>/<id>       编辑态：加 /edit 前缀（edit? 的 ? 让前缀可选）
/recordRef/<tableId>/<id>         引用关系图
/recordUnref/<tableId>/<?id>      未引用记录页（id 段可空，仅作「切回 record」的记忆）
/table/<tableId>   /tableRef/<tableId>
```

**两个不显然的设计**：

- **`/*` 而非 `:id`**：record / table / recordUnref 用 splat（`/*`），因为 `id` 段允许含 `/`（`useLocationData` 用 `split.slice(idx).join("/")` 拼回）。recordRef 用 `:id` 单段即可。
- **编辑态用 URL 前缀 `/edit` 而非 query string（`?edit=1`）**：前缀更干净、可被路由直接匹配，`edit?` 可选语法让浏览态和编辑态共用一个路由项。

### 2.2 构建：`navTo`（store.ts）

所有跳转都走 `navTo`，它做三件事：维护访问历史栈 → 把当前位置写进个人偏好持久化（冷启动恢复）→ 拼出 URL 返回。调用方 `navigate(navTo('record', table, id, true))`。**不要手拼 URL**——绕过 `navTo` 会丢历史和持久化。

### 2.3 解析：`useLocationData`（store.ts）

`useLocationData()` 返回 `{ curPage, curTableId, curId, edit, pathname }`。内部 `useLocation()` 拿 pathname，手写 `split('/')` 解析（识别 `/edit` 前缀、识别 `curPage`、剩余段 join 成 `curId`）。

**为什么不用 `useParams`**：`recordUnref/:table/*` 的 id 段是无名 splat，`useParams` 取不到；统一用 pathname 解析对所有路由都正确，参数同源不漂移。

### 2.4 URL 与持久化的分工

|  | URL（`useLocationData`） | 持久化偏好（`setPref('curTableId'...)`） |
|---|---|---|
| 角色 | 运行时真值 | 冷启动恢复 |
| 何时读 | 每次渲染 | app 启动一次（`getLastNavToInLocalStore`） |
| 谁写 | `navigate(navTo(...))` | `navTo` 内部同步写 |

启动流程：`AppLoader` 跑完 → `CfgEditorApp` 若 `curTableId` 为空，`navigate(getLastNavToInLocalStore())` 把地址栏拨到上次位置（见 [`CfgEditorApp.tsx`](../src/app/CfgEditorApp.tsx)）。之后一切以 URL 为准。

> URL 是会话内真值，持久化只负责「下次开窗回到哪」。刷新 / 分享链接 / 前进后退全靠 URL。

---

## 三、API 层：apiClient.ts（无状态传输）

整层是一组**纯函数**：入参 `(server, ..., signal)`，出参 `Promise<T>`。不持有任何缓存、不维护任何状态。

### 3.1 axios 实例按需创建

[`apiClient.ts`](../src/api/apiClient.ts) 内两件事：

- **每次请求 `axios.create` 一个新实例**：`server` 是用户动态配置（可换库），无法在模块加载时定死全局实例。`axios.create` 开销可忽略。
- **`normalizeServer` 剥前缀 / 尾斜杠**：剥掉用户可能误填的 `http://` / `https://` 前缀和尾随 `/`，避免拼出 `http://https://host`。固定拼 `http://` 因后端 `cfggen -gen server` 只支持 http。

### 3.2 端点清单

| 函数（apiClient.ts） | 方法 / 路径 | 参数位置 | 返回类型 |
|---|---|---|---|
| `fetchSchema` | GET `/schemas` | — | `RawSchema` |
| `fetchRecord` | GET `/record` | query:`table,id,depth=1` | `RecordResult` |
| `fetchRecordRefs` | GET `/record` | query:`table,id,depth,maxObjs,refs,in?` | `RecordRefsResult` |
| `fetchUnreferencedRecords` | GET `/record` | query:`table,maxObjs,noRefIn` | `UnreferencedRecordsResult` |
| `fetchRecordRefIds` | GET `/recordRefIds` | query:`table,id,in,out,maxIds` | `RecordRefIdsResult` |
| `addOrUpdateRecord` | POST `/recordAddOrUpdate` | query:`table` + JSON body | `RecordEditResult` |
| `deleteRecord` | POST `/recordDelete` | query:`table,id` + null body | `RecordEditResult` |
| `fetchNotes` | GET `/notes` | — | `Notes` |
| `updateNote` | POST `/noteUpdate` | query:`key` + **text body** | `NoteEditResult` |
| `getPrompt` | GET `/prompt` | query:`table` | `PromptResult` |
| `checkJson` | POST `/checkJson` | query:`table` + **text body** | `CheckJsonResult` |

> 另有 `/search` 端点（GET `?q=&max=`，[`SearchValue.tsx`](../src/features/finder/SearchValue.tsx) 原生 `fetch` **绕过 apiClient**，不走上表 axios 管线）。

### 3.3 三个易踩细节

1. **AbortSignal 全链路透传（GET 查询）**：每个 GET 查询函数都收 `signal: AbortSignal`，传给 axios `{ signal }`。来源是 React Query 的 `queryFn: ({signal}) => fetchX(...)`。组件卸载或 query 失效时 RQ 自动 abort，axios 取消在途请求。**链断了 = 取消不了 = 切记录时旧请求继续跑、回来还可能覆盖新数据。**（POST mutation 的 `signal` 可选、调用方一般不传——mutation 不参与 RQ 自动取消。）

2. **`Content-Type: text/plain` 要显式设**：`updateNote` / `checkJson` body 是裸字符串。axios 实例默认 `application/json`，发裸字符串会被当 JSON 序列化出错，故单独 `headers: {'Content-Type':'text/plain'}`。

3. **后端用「参数存在性」做开关**：`fetchRecordRefs` 里 `refs: ''`（空串占位）、`refIn ? {in:''} : {}`。后端 `EditorServer` 对 `refs` / `in` 做 `!= null` 判断，传空串等价于原先无值的 `&refs`。这是后端那边的协议约定，不是 axios 的事。

> API 层越「笨」越好。它不该知道「这条记录改了要不要刷别的表」——那是 React Query 的事。它只忠实搬运。

---

## 四、React Query 层：缓存、失效、取消

### 4.1 全局配置

[`queryClient.ts`](../src/services/queryClient.ts) 设全局默认 `staleTime: 30s`（30s 内不重取）。`invalidateAllQueries()` 调 `queryClient.invalidateQueries({queryKey: []})`。

- `{queryKey: []}` 是「匹配所有 query」的前缀（空数组是所有 key 的公共前缀）。
- 故意**不**加 `refetchType:'all'`：默认 `'active'` 只立即重取当前挂载的查询，未挂载的标记 stale 后下次 mount 自然刷新——正确性不变，避免一次性轰炸后端。

挂在 [`main.tsx`](../src/main.tsx)：`<QueryClientProvider client={queryClient}>` 包 `<RouterProvider>`，整个 app 共享一个 `queryClient` 单例。

### 4.2 queryKey 设计要点

**queryKey 是「这条缓存对应哪份数据」的唯一身份证。** 三原则：

1. **第一段是资源域**（`'schema'` / `'record'` / `'layout'` …），便于按域批量失效。
2. **凡「会影响返回结果」的参数都进 key**——漏一个 = 不同参数共享同一缓存 = 张冠李戴的脏数据。
3. **`select` 必须稳定引用**（见 §5.2）。

所有 queryKey 经 [`queryKeys.ts`](../src/services/queryKeys.ts) 的 **Query Key Factory** 集中构造，避免字面量散落、invalidate 时拼错。工厂构造（脱掉 TS 类型后的 key 形状）：

```
queryKeys 工厂:
  setting()                  → ['setting']
  resInfo()                  → ['setting', 'resInfo']        // 挂 setting 域下，enabled 依赖 setting 完成
  schema()                   → ['schema']
  notes()                    → ['notes']
  record(table, id)          → ['record', table, id]
  recordRef(table, id, depth, max, refIn)
                             → ['recordRef', table, id, depth, max, refIn]
  unreferenced(table, max)   → ['unreferenced', table, max]
  layout(pathname, layoutKeys, topologyKeys, isEditRoute):
      isEditRoute → ['layout', pathname, 'e', layoutKeys, topologyKeys]   // 编辑路由态：'e' 隔离（节点集合不同）
      otherwise   → ['layout', pathname, layoutKeys, topologyKeys]
  prompt(table)              → ['prompt', table]
```

> 缓存写操作收口为**动词函数**，调用方不碰 `queryClient` 实例：key 相关的动词收在本 factory 同文件——`removeEditLayoutCache(pathname)` = `removeQueries(['layout', pathname, 'e'])`（结构编辑清编辑态，§6.4）、`invalidateLayoutCache(pathname)` = `invalidateQueries(['layout', pathname])`（布局失败 retry，04）、`refetchResInfoCache()`、`setNotesCache(notes)`（§6.3）；key 无关的全量操作（`invalidateAllQueries` / `removeAllQueryCache`）留在 `queryClient.ts`。前缀含 `'e'` 分桶段的位置与 `queryKeys.layout` 同文件共处，改分桶约定时同步改——调用方不再手写字面量。

**factory 维护的 queryKey 清单**（另有 `recordRefIds` / `search` / `tauri` / `vtt2` 等手写 key 散落在组件里）：

| 构造（`queryKeys.xxx`） | 文件 | queryFn | staleTime | 备注 |
|---|---|---|---|---|
| `.setting()` | AppLoader | `readPrefAsyncOnce` | `Infinity`, `retry:0` | 启动一次性加载偏好，失败不重试（02 讲启动门卫）|
| `.resInfo()` | AppLoader | `readResInfosAsync` | 默认 30s | `enabled: !!data`；Tauri 下扫资源目录 |
| `.schema()` | CfgEditorApp | `fetchSchema` | 5min | `select: schemaSelector`（**模块级常量**，§5.2）|
| `.notes()` | CfgEditorApp | `fetchNotes` | 5min | `select: notesToMap` |
| `.record(tableId, id)` | Record | `fetchRecord` | 默认 30s | `enabled: !isNewRecord`（新记录用本地 mock 不发请求）|
| `.recordRef(...)` | RecordRef | `fetchRecordRefs` | 10s | |
| `.unreferenced(tableId, maxNode)` | RecordRef / UnreferencedButton | `fetchUnreferencedRecords` | 10s | UnreferencedButton **手写同形 key**（[`UnreferencedButton.tsx`](../src/features/headerbar/UnreferencedButton.tsx)）→ 跨组件共享缓存 |
| `.layout(pathname, layoutKeys, topologyKeys, isEditRoute)` | useEntityToGraph | `layoutAsync`（ELK） | `isEdited ? 0 : 5min` | `'e'` 段按路由态分桶（`type==='edit'`），staleTime 跟脏标记；04 讲 |
| `.prompt(tableId)` | Chat | `getPrompt` | `Infinity` | prompt 后端静态生成，`enabled: editable` |

> **staleTime 取舍**：`Infinity` = 「永远新鲜除非显式 invalidate」（schema / prompt / setting）；`0` = 「永远要最新」（编辑路由且脏——有未提交修改）；中间值 = 「N 秒内快照够用」。越长越省请求但越可能脏；越短越准但越费。

### 4.3 三种缓存失效手段（重点对比）

| 手段 | 语义 | 何时用 |
|---|---|---|
| `invalidateQueries({queryKey})` | 标记 stale +（默认）重取当前 active 的 | **最常用**。数据可能变了，让它刷新 |
| `removeQueries({queryKey})` | 直接删缓存，**不**主动 fetch | 数据彻底失效，但想等「重渲后用新 queryFn 闭包」再取（§6.4）|
| `setQueryData(key, data)` | 直接写新缓存 | 后端返回了最新全量数据，省一次请求（§6.3）|

`invalidate` 是「标记 + 立即用当前闭布拉」，`remove` 是「清空 + 等自然重取（用未来新闭包）」，`setQueryData` 是「替后端回答」。三者不可混用——尤其 `invalidate` 会**立即用当前 queryFn 闭包 refetch**，某些时序下是错的（§6.4）。

---

## 五、Query 实战模式

### 5.1 标准数据加载

四要素：`queryKey` 定身份、`queryFn` 透传 `signal`、`enabled` 控条件、四态兜底。

```
useQuery({
    queryKey: queryKeys.record(table, id),            // 定身份
    queryFn:  ({signal}) => fetchRecord(..., signal), // signal 透传给 axios
    enabled:  !isNewRecord,                           // 控条件
})
```

四态兜底（按顺序短路返回）：

| 兜底态 | 触发 | 处理 |
|---|---|---|
| loading | `isLoading` | 壳子兜底 |
| error | `isError` | `<Result status='error' title={error.message}/>` |
| empty | `!recordResult` | `<Result title='record result empty'/>` |
| 业务错误 | `resultCode != 'ok'` | `<Result status='error' title={resultCode}/>` |

**业务错误（HTTP 200 但 `resultCode != 'ok'`）要单独判**——后端用 resultCode 表达业务结果，axios 不会抛。

### 5.2 `select` 必须稳定引用（CfgEditorApp 的 schemaSelector）

```
❌ 反例：内联箭头，每次 render 新身份
   select: (raw) => new Schema(raw)

✅ 正例：模块级常量（CfgEditorApp）
   const schemaSelector = (raw) => new Schema(raw);    // 模块加载时定一次
   select: schemaSelector
```

**为什么**：`select` 每次 render 换新引用 → RQ 每次 render 重跑它 → 每次 `new Schema()`（遍历全部 items、建多个 Map、为每张 table 建 idMap，毫秒级）。更糟：`Schema` 是含 `Map` 字段的 class 实例，RQ 的 `replaceEqualDeep` 判不等 → `schema` 引用每帧变 → `outletCtx` 每帧新建 → Outlet 子树（Table / TableRef / Record / RecordRef，context 变化绕过 memo）**全树重渲**。提为模块级常量后只在 `rawSchema` 变化时构造。

> `select` 是「每次都跑」的派生函数，它的引用稳定性直接决定派生结果是否稳定。凡 `select` 里 `new` 了非平凡对象（class 实例、含 Map / Set），务必提为模块级常量。

### 5.3 layout 查询：把「影响输出的设置」塞进 queryKey（useEntityToGraph）

[`useEntityToGraph`](../src/flow/useEntityToGraph.ts) 把两类 setting 都纳入 layout 的 queryKey：

```
layoutKeys   = 布局字段白名单（颜色等纯表现字段）
topologyKeys = 拓扑 setting（maxImpl / refIn / refOutDepth / maxNode / ...）

queryKey = isEditRoute    // type === 'edit'：按 entityMap 构建方式分桶（编辑/浏览节点集合不同）
    ? ['layout', pathname, 'e', layoutKeys, topologyKeys]    // 编辑路由态：'e' 隔离
    : ['layout', pathname, layoutKeys, topologyKeys]         // 浏览路由态
staleTime = isEdited ? 0 : 5min   // 脏标记驱动，与 queryKey 分桶独立
```

要点（详见 04）：

- **`pathname` 进 key**：每个图布局独立缓存。
- **`topologyKeys` 进 key**：改拓扑参数（`refOutDepth` 等）→ key 变 → 自然失效重布局；改纯颜色字段 → key 不变 → 命中缓存不重跑 ELK。**用 queryKey 替代了手动的 `clearLayoutCache`**——store 因此回归纯状态容器（store 作为纯状态容器的定位详见 [02 §3.1](02-state-management.md)）。
- **`'e'` 按路由态分桶**：编辑/浏览 entityMap 构建方式不同（节点集合不同），必须分桶防 `nodes`/`rectMap` 错配。分桶用 `type==='edit'` 而非脏标记——提交后脏标记翻 false 但 entityMap 仍是编辑态构建；`staleTime` 才跟脏标记（脏 0 / 干净 5min）。结构变更 `removeQueries(['layout', pathname, 'e'])` 只清编辑路由态。

---

## 六、Mutation 实战模式

### 6.1 提交后全量失效 + 按 resultCode 分流（Record.tsx）

[`Record.tsx`](../src/features/record/Record.tsx) 的提交 mutation 在回调里按 `resultCode` 分流：

```
onSuccess(editResult):
  resultCode ∈ {updateOk, addOk}:
      notification.info(resultCode)
      session.onCommitSuccess()           // 重置 undo 基准 + 清栈（详见 03 §九）
      invalidateAllQueries()              // 全量标记 stale
      if (curId === NEW_RECORD_ID):
          navigate(navTo('record', table, editResult.id, true))   // 跳到后端返回的真实 id
  else:
      notification.warning(resultCode)    // 业务失败
onError(error):
      notification.error(error)
```

- **HTTP 200 不代表业务成功**：后端用 `resultCode` 表达，只有 `updateOk` / `addOk` 才真成功。
- **新记录 navigate 到真实 id**：创建前用临时 `NEW_RECORD_ID`，成功后跳后端返回的真实 id——key 变了，旧 session unmount，新 session 用真实 id。

### 6.2 失败保留编辑态

`onError` **不关编辑框**，保留用户已输入内容便于重试。业务失败同理。「别让用户因一次失败丢掉整段输入」。

### 6.3 精确写缓存（NoteShowOrEdit.tsx）

[`NoteShowOrEdit.tsx`](../src/flow/NoteShowOrEdit.tsx) 在 `updateNote` 成功后，调 `setNotesCache(editResult.notes)`（`queryKeys.ts` 动词，内部 `setQueryData(queryKeys.notes(), notesToMap(notes))`）直接写缓存。因为 `updateNote` 后端返回**全量 notes**，直接写就**省一次 refetch**——比 `invalidateQueries(['notes'])`（触发一次 GET /notes）更省。前提：后端真返回了完整新状态。

### 6.4 `removeQueries` 而非 `invalidate`（Record.tsx 的 onStructureChange）

`Record.tsx` 的 `onStructureChange` 在事件期同步调 `removeEditLayoutCache(pathnameRef.current)`（`queryKeys.ts` 的动词，内部即 `removeQueries({queryKey: ['layout', pathname, 'e']})`）。

**为什么 remove 不 invalidate**：结构变更（增删节点）后 `entityMap` 会重算，`useEntityToGraph` 的 `queryFn` 闭包里的 `nodes` 变了。若用 `invalidate`，它会**立即用重渲前的旧闭包 refetch** → 旧布局；用 `remove` 只删缓存不主动 fetch，等重渲那一帧 `useQuery` 用**新闭包**（新 nodes）自然重取 → 新布局。且 `remove` 必须在事件期同步调用（不能挪 effect），否则重渲那一帧读到还没删的旧缓存。这是 03 → 04 的真实钩子。

> `invalidate` = 「现在就用当前闭包重取」；`remove` = 「清掉，等下次自然重取（用未来最新闭包）」。queryFn 依赖的闭包即将变化时用 remove；闭包稳定、只是数据可能过时时用 invalidate。

---

## 七、Cheat Sheet

**加新查询**：① `apiClient.ts` 加无状态 fetch 函数（带 `signal`）→ ② `queryKeys.ts` 加 factory（别在组件手写字面量）→ ③ 组件 `useQuery` + 四态兜底 → ④ `select` 若 `new` 了非平凡对象就提为模块级常量。

**加新提交**：① `apiClient.ts` 加 POST（裸字符串 body 记得 `text/plain`）→ ② `useMutation`，`onSuccess` 按 `resultCode` 分流 → ③ 失效缓存：默认 `invalidateAllQueries()`；后端返回全量用 `setQueryData`；queryFn 闭包即将变化用 `removeQueries` → ④ 失败保留用户输入。

**跳转**：永远 `navigate(navTo(curPage, table, id, edit))`，别手拼 URL。

**换库**：`setServer` 已 `removeAllQueryCache()` 全清（多数 queryKey 不含 `server`，个别如 `search` 含——但全清已覆盖，旧库数据不会赖缓存），无需额外处理。

---

## 一句话速记

- **三层各司其职**：URL 决定查什么、API 决定怎么查、React Query 决定查过的是否还能用。
- **queryKey 是缓存身份证**：第一段资源域 + 全部入参；经 factory 集中构造。
- **`select` 要稳定引用**：内联箭头 → 派生每帧重跑 + 引用每帧变 → 全树重渲；`new` 了非平凡对象就提为模块级常量。
- **三种失效别混**：`invalidate`（立即用当前闭包重取）/ `remove`（清掉等未来新闭包）/ `setQueryData`（后端已给全量就替它回答）。
- **业务结果看 `resultCode` 不看 HTTP**：200 不代表成功。
