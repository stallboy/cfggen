# URL、API 与 React Query：数据流全景与教学（cfgeditor）

> 面向 cfgeditor 的「数据从哪来、怎么缓存、URL 怎么编码状态」文档：通用原理 → 本项目三层现状 → queryKey/mutation 实战 → 可改进之处。
> 配套：[`状态管理-总结与演进.md`](./状态管理-总结与演进.md)（状态分类与 resso/EditingSession）、[`DIRECTORY_STRUCTURE.md`](./DIRECTORY_STRUCTURE.md)（分层）、[`fitview-视口适配机制.md`](./fitview-视口适配机制.md)（layout 查询的下游消费）。

---

## 一、本质问题：一次「看一条记录」牵动三层

用户点开一条记录，背后是三层性质截然不同的代码在协作。把它们混为一谈是绝大多数 bug 的根：

| 层 | 关注的问题 | 本项目落点 | 典型动作 |
|---|---|---|---|
| **URL / 路由层** | 「现在在看什么」如何编码进地址栏，可分享、可刷新、可前进后退 | `main.tsx`（`createBrowserRouter`）、`store/store.ts`（`navTo` / `useLocationData`） | `/record/Item/1001` → 解析出 `curTableId=Item`、`curId=1001` |
| **API 层（HTTP 客户端）** | 「怎么把请求发出去、怎么把响应解析回来」——纯传输，不含任何业务缓存语义 | `src/api/apiClient.ts`（axios） | `fetchRecord(server, table, id, signal)` |
| **服务端状态层（React Query）** | 「请求来的数据怎么缓存、何时过期、谁负责重取、取消与并发」 | `src/services/queryClient.ts` + 各组件里的 `useQuery`/`useMutation` | `useQuery({queryKey:['record',...], queryFn:...})` |

**关键判据**：
- API 层**只管搬数据**，不关心调几次、缓存与否——它是无状态的工具函数。
- 缓存/重取/取消是 React Query 的职责，**不要**在 API 层手写缓存，也**不要**在组件里手写 loading/error 状态机（让 `useQuery` 的 `isLoading/isError/data` 来扛）。
- URL 是「看什么」的真值；服务端缓存是「数据长什么样」的快照。两者解耦：换 URL 触发新查询，缓存命中时不发请求。

> 一句话：**URL 决定查什么，API 决定怎么查，React Query 决定查过的是否还能用。** 三层各司其职，别让任何一层越界。

---

## 二、URL 层：把「应用当前在看什么」编码进地址栏

### 2.1 路由表（`main.tsx`）

用 react-router v8 的 `createBrowserRouter`（v7 起包名统一为 `react-router`，`RouterProvider` 从 `react-router/dom` 导入，不再是 v6 的 `react-router-dom`）。根路由 `/` 挂 `AppLoader`，子路由全部 **`lazy` 懒加载**（按需打包，首屏只加载壳）：

```ts
createBrowserRouter([{
    path: "/",
    Component: AppLoader,
    children: [
        { path: "table/:table/*",        lazy: () => import("@/features/table/Table.tsx").then(m => ({Component: m.Table})) },
        { path: "tableRef/:table/*",     lazy: () => import("@/features/table/TableRef.tsx").then(m => ({Component: m.TableRef})) },
        { path: "edit?/record/:table/*", lazy: () => import("@/features/record/Record.tsx").then(m => ({Component: m.Record})) },
        { path: "recordRef/:table/:id",  lazy: () => import("@/features/record/RecordRef.tsx").then(m => ({Component: m.RecordRefRoute})) },
        { path: "recordUnref/:table/*",  lazy: () => import("@/features/record/RecordRef.tsx").then(m => ({Component: m.RecordRefRoute})) },
        { path: "*",                     lazy: () => import("./app/PathNotFound.tsx").then(m => ({Component: m.PathNotFound})) },
    ]
}])
```

URL 形态约定：

```
/<curPage>/<tableId>/<id>          例：/record/Item/1001
/edit/record/<tableId>/<id>        编辑模式：加 /edit 前缀（edit? 的 ? 让前缀可选）
/recordRef/<tableId>/<id>          引用关系图
/recordUnref/<tableId>/<?id>       未引用记录页（id 段可空，仅作「切回 record」的记忆）
/table/<tableId>  /tableRef/<tableId>
```

**两个不显然的设计**：

- **`/*` 而非 `:id`**：record/table/recordUnref 用 splat（`/*`），因为 `id` 段允许包含 `/`（`useLocationData` 用 `split.slice(idx).join("/")` 拼回）。recordRef 用 `:id` 单段即可。统一靠 `useLocationData` 解析，不依赖 `useParams`。
- **编辑态用 URL 前缀 `/edit`，而非 query string（`?edit=1`）**：前缀更干净、可被路由直接匹配，且 `edit?` 的可选语法让浏览态和编辑态共用一个路由项。

### 2.2 构建：`navTo`（`store/store.ts`）

所有跳转都走 `navTo`，它做三件事：

```ts
export function navTo(curPage, tableId, id, edit = false, addHistory = true) {
    // 1. 维护访问历史（forward/back 栈）
    if (addHistory) { /* 比 history.cur() 变了才 push，避免重复 */ }
    // 2. 把当前位置写进「个人偏好」持久化（冷启动恢复用）
    setPref('curPage', curPage); setPref('curTableId', tableId); setPref('curId', id);
    // 3. 拼出 URL 返回，交给 navigate()
    const url = `/${curPage}/${tableId}/${id}`;
    return (curPage == 'record' && edit) ? '/edit' + url : url;
}
```

调用方 `navigate(navTo('record', table, id, true))`。**不要**手拼 URL 字符串——绕过 `navTo` 会丢历史记录和持久化。

### 2.3 解析：`useLocationData`（`store/store.ts`）

```ts
const { curPage, curTableId, curId, edit, pathname } = useLocationData();
```

它内部 `useLocation()` 拿 pathname，手写 `split('/')` 解析（识别 `/edit` 前缀、识别 `curPage`、把剩余段 join 成 `curId`）。**为什么不用 `useParams`**：`recordUnref/:table/*` 的 id 段是无名 splat，`useParams` 取不到；统一用 pathname 解析对所有路由都正确。

### 2.4 URL 与持久化的分工

| | URL（`useLocationData`） | 持久化偏好（`setPref('curTableId'...)`） |
|---|---|---|
| **角色** | 运行时真值 | 冷启动恢复 |
| **何时读** | 每次渲染 | app 启动一次（`getLastNavToInLocalStore`） |
| **谁写** | `navigate(navTo(...))` | `navTo` 内部同步写 |

启动流程：`AppLoader` 跑完 → `CfgEditorApp` 里若 `curTableId` 为空，`navigate(getLastNavToInLocalStore())` 把地址栏拨到上次位置。之后一切以 URL 为准。

> 教学点：**URL 是会话内的真值，持久化只负责「下次开窗时回到哪」。** 不要把 URL 当成可有可无的装饰——刷新页面、分享链接、前进后退全靠它。

---

## 三、API 层：`apiClient.ts`（无状态传输）

整层是一组**纯函数**：入参 `(server, ..., signal)`，出参 `Promise<T>`。不持有任何缓存、不维护任何状态。

### 3.1 axios 实例按需创建

```ts
function httpClient(server: string) {
    return axios.create({
        baseURL: `http://${normalizeServer(server)}`,
        timeout: 15000,
        headers: { 'Content-Type': 'application/json' },
    });
}
```

- **为什么每次 `axios.create`**：`server` 是用户动态配置的（可换库），无法在模块加载时定死一个全局实例。`axios.create` 开销可忽略。
- **`normalizeServer`**：剥掉用户可能误填的 `http://`/`https://` 前缀和尾随 `/`，避免拼出 `http://https://host`。固定 `http://` 是因为后端 `cfggen -gen server` 只支持 http。

### 3.2 端点清单

| 函数（`apiClient.ts`） | 方法 / 路径 | 参数位置 | 返回类型 |
|---|---|---|---|
| `fetchSchema` | GET `/schemas` | — | `RawSchema` |
| `fetchRecord` | GET `/record` | query: `table,id,depth=1` | `RecordResult` |
| `fetchRecordRefs` | GET `/record` | query: `table,id,depth,maxObjs,refs,in?` | `RecordRefsResult` |
| `fetchUnreferencedRecords` | GET `/record` | query: `table,maxObjs,noRefIn` | `UnreferencedRecordsResult` |
| `fetchRecordRefIds` | GET `/recordRefIds` | query: `table,id,in,out,maxIds` | `RecordRefIdsResult` |
| `addOrUpdateRecord` | POST `/recordAddOrUpdate` | query:`table` + JSON body | `RecordEditResult` |
| `deleteRecord` | POST `/recordDelete` | query:`table,id` + null body | `RecordEditResult` |
| `fetchNotes` | GET `/notes` | — | `Notes` |
| `updateNote` | POST `/noteUpdate` | query:`key` + **text body** | `NoteEditResult` |
| `getPrompt` | GET `/prompt` | query:`table` | `PromptResult` |
| `checkJson` | POST `/checkJson` | query:`table` + **text body** | `CheckJsonResult` |

### 3.3 三个容易踩的细节

1. **AbortSignal 全链路透传**：每个 fetch 都收 `signal: AbortSignal`，传给 axios 的 `{ signal }`。来源是 React Query 的 `queryFn: ({signal}) => fetchX(server, ..., signal)`。组件卸载或 query 失效时，RQ 自动 abort，axios 取消在途请求。**这条链断了 = 取消不了 = 切记录时旧请求继续跑、回来还可能覆盖新数据。**

2. **`Content-Type: text/plain` 要显式设**：`updateNote` / `checkJson` 的 body 是裸字符串。axios 实例默认 `application/json`，发裸字符串会被当 JSON 序列化出错，所以这两个函数单独 `headers: {'Content-Type': 'text/plain'}`。

3. **后端用「参数存在性」做开关**：`fetchRecordRefs` 里 `refs: ''`（空串占位）、`refIn ? {in:''} : {}`。后端 `EditorServer` 对 `refs`/`in` 做 `!= null` 判断，所以传空串等价于原先无值的 `&refs`。这是后端契约，不是 axios 的事。

> 教学点：**API 层越「笨」越好。** 它不该知道「这条记录改了要不要刷别的表」——那是 React Query 的事。它只该忠实搬运。

---

## 四、React Query 层：缓存、失效与取消

### 4.1 全局配置（`services/queryClient.ts`）

```ts
export const queryClient = new QueryClient({
    defaultOptions: { queries: { staleTime: 1000 * 30 } },   // 默认 30s 内不重取
});

export function invalidateAllQueries() {
    queryClient.invalidateQueries({queryKey: []}).catch(console.log);
}
```

- `{queryKey: []}` 是「匹配所有 query」的前缀（空数组是所有 key 的公共前缀）。
- 故意**不**加 `refetchType: 'all'`：默认 `'active'` 只立即重取当前挂载的查询，未挂载的标记 stale 后，下次 mount 时自然刷新——正确性不变，但避免一次性轰炸后端。

挂载在 `main.tsx`：`<QueryClientProvider client={queryClient}>` 包住 `<RouterProvider>`。整个 app 共享一个 `queryClient` 单例。

### 4.2 queryKey 设计哲学（最重要的教学点）

**queryKey 是「这条缓存对应哪份数据」的唯一身份证。** 三个原则：

1. **第一段是「资源域」**：`'schema'` / `'notes'` / `'record'` / `'recordRef'` / `'layout'` / `'prompt'` / `'setting'`。这样 `invalidateQueries({queryKey:['recordRef']})` 能按域批量失效。
2. **凡是「会影响返回结果」的参数都进 key**：`tableId`、`id`、`refOutDepth`、`maxNode`、`refIn`……漏一个 = 不同参数共享同一缓存 = 张冠李戴的脏数据。
3. **`select` 必须是稳定引用**（见 §5.2 的血泪教训）。

所有 queryKey 经 `services/queryKeys.ts` 的 **Query Key Factory** 集中构造——避免字面量散落各处、invalidate 时拼错：

```ts
export const queryKeys = {
    setting: () => ['setting'],
    resInfo: () => ['setting', 'resInfo'],          // 挂在 setting 域下
    schema: () => ['schema'],
    notes: () => ['notes'],
    record: (tableId, id) => ['record', tableId, id],
    recordRef: (tableId, id, refOutDepth, maxNode, refIn) => ['recordRef', tableId, id, refOutDepth, maxNode, refIn],
    unreferenced: (tableId, maxNode) => ['unreferenced', tableId, maxNode],
    layout: (pathname, layoutKeys, topologyKeys, isEdited) => isEdited
        ? ['layout', pathname, 'e', layoutKeys, topologyKeys]
        : ['layout', pathname, layoutKeys, topologyKeys],
    prompt: (tableId) => ['prompt', tableId],
};
// useQuery({queryKey: queryKeys.record(curTableId, curId)})
// queryClient.invalidateQueries({queryKey: queryKeys.notes()})
```

> 例外：layout 的「按前缀批量失效」（`['layout', pathname]`、`['layout', pathname, 'e']`）仍手写——那是批量语义，与「构造单条 key」不同；第一段 `'layout'` 与 factory 一致即可。

**当前 queryKey 清单**（factory 调用形式）：

| 构造（`queryKeys.xxx`） | 文件 | queryFn | staleTime | 备注 |
|---|---|---|---|---|
| `.setting()` | `AppLoader` | `readPrefAsyncOnce` | `Infinity`, `retry:0` | 启动期一次性加载偏好，失败不重试 |
| `.resInfo()` | `AppLoader` | `readResInfosAsync` | 30s | `enabled: !!data`；Tauri 下扫资源目录 |
| `.schema()` | `CfgEditorApp` | `fetchSchema` | 5min | `select: schemaSelector`（**模块级常量**） |
| `.notes()` | `CfgEditorApp` | `fetchNotes` | 5min | `select: notesToMap` |
| `.record(curTableId, curId)` | `Record` | `fetchRecord` | 30s | `enabled: !isNewRecord` |
| `.recordRef(curTableId, curId, refOutDepth, maxNode, refIn)` | `RecordRef` | `fetchRecordRefs` | 10s | 引用关系频繁变，staleTime 短 |
| `.unreferenced(curTableId, maxNode)` | `RecordRef` | `fetchUnreferencedRecords` | 10s | 同上 |
| `.layout(pathname, layoutKeys, topologyKeys, isEdited)` | `useEntityToGraph` | `layoutAsync`（ELK） | 5min | 编辑态 `'e'` 段 + staleTime=0，见 §5.3 |
| `.prompt(curTableId)` | `Chat` | `getPrompt` | `Infinity` | prompt 基本不变，`enabled: editable` |

> **staleTime 的取舍**：`Infinity` = 「我认为它永远新鲜，除非被显式 invalidate」（schema/prompt/setting 这类）；`0` = 「永远要最新的」（编辑态布局）；中间值 = 「N 秒内的快照够用，过期再问后端」。staleTime 越长越省请求，但越可能脏；越短越准，但越费。本项目 schema 5min、引用图 10s，是按「变更频率」定的。

### 4.3 三种缓存失效手段（重点对比）

| 手段 | 语义 | 何时用 |
|---|---|---|
| `invalidateQueries({queryKey})` | 标记 stale +（默认）重取当前 active 的 | **最常用**。数据可能变了，让它刷新 |
| `removeQueries({queryKey})` | 直接删除缓存，**不**主动 fetch | 数据彻底失效，但想等「重渲后用新 queryFn 闭包」再取（见 §6.4） |
| `setQueryData(key, data)` | 直接写一份新缓存 | 后端返回了最新全量数据，省一次请求（见 §6.3） |

`invalidate` 是「标记 + 拉取」，`remove` 是「清空 + 等自然重取」，`setQueryData` 是「我替后端回答了」。三者不可混用——尤其 `invalidate` 会**立即用当前 queryFn 闭包 refetch**，这在某些时序下是错的（§6.4）。

---

## 五、Query 实战模式

### 5.1 标准数据加载（`Record.tsx`）

```ts
const { isLoading, isError, error, data: recordResult } = useQuery({
    queryKey: queryKeys.record(curTableId, curId),
    queryFn: ({signal}) => fetchRecord(server, curTableId, curId, signal),
    enabled: !isNewRecord,        // 新记录不发请求（用默认数据）
});
if (isLoading) return;            // loading：返回 null（壳子兜底）
if (isError)  return <Result status='error' title={error.message}/>;
if (!recordResult) return <Result title='record result empty'/>;
if (recordResult.resultCode != 'ok') return <Result status='error' title={recordResult.resultCode}/>;
// 拿到数据，渲染
```

四要素：`queryKey` 定身份、`queryFn` 透传 `signal`、`enabled` 控条件、四态兜底（loading/error/empty/business-error）。业务错误（HTTP 200 但 `resultCode != 'ok'`）要单独判——后端用 resultCode 表达业务结果，axios 不会抛。

### 5.2 `select` 必须稳定引用（`CfgEditorApp` 的 `schemaSelector`）

```ts
// ❌ 反例：内联箭头，每次 render 新身份
useQuery({ queryKey:['schema'], queryFn:..., select: (raw) => new Schema(raw) });

// ✅ 正例：模块级常量
const schemaSelector = (rawSchema: RawSchema) => new Schema(rawSchema);
useQuery({ queryKey:['schema'], queryFn:..., select: schemaSelector });
```

**为什么**：`select` 每次 render 换新引用 → RQ 每次 render 重跑它 → 每次 `new Schema()`（遍历全部 items、建多个 Map，毫秒级）。更糟的是 `Schema` 是含 `Map` 字段的 class 实例，RQ 的 `replaceEqualDeep` 判不等 → `schema` 引用每帧变 → outlet context 每帧新建 → Outlet 子树（Table/TableRef/Record/RecordRef）全树重渲。

> 教学点：**`select` 是「每次都跑」的派生函数，它的引用稳定性直接决定派生结果是否稳定。** 凡是 `select` 里 `new` 了非平凡对象（class 实例、含 Map/Set 的结构），务必提为模块级常量。

### 5.3 layout 查询：把「影响输出的设置」塞进 queryKey（`useEntityToGraph`）

```ts
const layoutKeys = pickLayoutKeys(nodeShowSetting);                 // 布局相关字段
const topologyKeys = { maxImpl, refIn, refOutDepth, maxNode,        // 拓扑 setting
                       recordRefIn, recordRefOutDepth, recordMaxNode, tauriConf };
const queryKey = editingObjectRes?.isEdited
    ? ['layout', pathname, 'e', layoutKeys, topologyKeys]           // 编辑态：'e' 隔离 + staleTime 0
    : ['layout', pathname, layoutKeys, topologyKeys];               // 浏览态：5min 缓存
```

精妙之处：
- **`pathname` 进 key**：每个图（每条 record/每个引用图）布局独立缓存。
- **`layoutKeys`/`topologyKeys` 进 key**：改拓扑参数（如 `refOutDepth`）→ key 变 → 缓存自然失效重布局；改纯颜色字段 → key 不变 → 命中缓存不重跑 ELK。**用 queryKey 替代了手动的 `clearLayoutCache` 命令式清缓存**，store 重新变成纯状态容器。
- **`'e'` 隔离编辑态**：编辑可能改拓扑，故编辑态 staleTime=0（每次重算）；浏览态 5min 复用。结构变更时 `removeQueries(['layout', pathname, 'e'])` 只清编辑态，浏览态缓存不受影响。

---

## 六、Mutation 实战模式

### 6.1 提交后全量失效（`Record.tsx`、`AddJson.tsx`）

```ts
const addOrUpdateRecordMutation = useMutation({
    mutationFn: (jsonObject) => addOrUpdateRecord(server, curTableId, jsonObject),
    onError: (error) => notification.error({title: `...err: ${error}`}),
    onSuccess: (editResult) => {
        if (editResult.resultCode == 'updateOk' || editResult.resultCode == 'addOk') {
            notification.info({title: `...${editResult.resultCode}`});
            invalidateAllQueries();                       // 全量标记 stale
            if (curId === NEW_RECORD_ID) {
                navigate(navTo('record', curTableId, editResult.id, true));  // 新记录跳真实 id
            }
        } else {
            notification.warning({title: `...${editResult.resultCode}`});    // 业务失败
        }
    },
});
```

要点：
- **`onSuccess` 按 `resultCode` 分流**：HTTP 200 不代表业务成功（后端用 `resultCode` 表达）。只有 `updateOk`/`addOk` 才真成功。
- **新记录 navigate 到真实 id**：创建前用临时 `NEW_RECORD_ID`，成功后后端返回真实 id，跳过去——key 变了，旧 session unmount，新 session 用真实 id 构造。

### 6.2 失败保留编辑态

`onError` **不**关编辑框（不 `setIsEdit(false)`），保留用户已输入的内容便于重试。业务失败（非 OK resultCode）同理。这是「别让用户因一次失败丢掉整段输入」的体验设计。

### 6.3 精确写缓存（`NoteShowOrEdit.tsx`）

```ts
const { mutate } = useMutation({
    mutationFn: (newNote) => updateNote(server, id, newNote),
    onSuccess: (editResult) => {
        if (resultCode ok) {
            queryClient.setQueryData(['notes'], notesToMap(editResult.notes));  // 后端返回全量 notes
            setIsEdit(false);
        }
    },
});
```

`updateNote` 后端返回**全量 notes**，所以直接 `setQueryData(['notes'], ...)` 写缓存，**省一次 refetch**。这比 `invalidateQueries(['notes'])`（会触发一次 GET /notes）更省。前提：后端真的返回了完整的新状态。

### 6.4 `removeQueries` 而非 `invalidate`（`Record.tsx` 的 `onStructureChange`）

```ts
onStructureChange: () => queryClient.removeQueries({queryKey: ['layout', pathnameRef.current, 'e']})
```

**为什么用 remove 不用 invalidate**：结构变更（增删节点）后，`entityMap` 会重算，`useEntityToGraph` 的 `queryFn` 闭包里的 `nodes` 变了。若用 `invalidate`，它会**立即用重渲前的旧闭包 refetch** → 拿到旧布局；用 `remove` 只删缓存不主动 fetch，等重渲那一帧 `useQuery` 用**新闭包**（新 nodes）自然重取 → 新布局。且 `remove` 必须在 render 期调用（不能挪 effect），否则重渲那一帧会读到还没被删的旧缓存。

> 教学点：**`invalidate` = 「现在就用当前闭包重取」；`remove` = 「清掉，等下次自然重取（用未来最新的闭包）」。** 当 queryFn 依赖的闭包变量即将变化时，用 remove；当闭包稳定、只是数据可能过时，用 invalidate。

### 6.5 纯校验 mutation（`Chat.tsx`）

```ts
const checkJsonMutation = useMutation({
    mutationFn: (raw) => checkJson(server, curTableId, raw),
    onSuccess: (result) => { if (result.resultCode == 'ok') replaceEditingObject(JSON.parse(result.jsonResult)); }
});
```

不写缓存、不失效——`checkJson` 是「校验 + 回填表单」的纯操作，不落库。mutation 不一定要碰缓存。

### 6.6 `refetchType: 'all'`（`readResInfosAsync` 的 `invalidateResInfos`）

```ts
queryClient.invalidateQueries({queryKey: ['setting', 'resInfo'], refetchType: 'all'});
```

资源目录刷新用 `refetchType: 'all'`（含 inactive），因为 resInfo 查询此时未必挂载——要强制重扫。与 `invalidateAllQueries` 故意用默认 `'active'` 形成对比：**是否连未挂载的查询一起刷，看「这份数据是否可能被别处依赖」。**

---

## 七、速查（Cheat Sheet）

**加一个新查询**：
1. `apiClient.ts` 加无状态 fetch 函数，签名带 `signal`。
2. `services/queryKeys.ts` 加构造函数（如 `foo: (a, b) => ['foo', a, b]`）——**别在组件里手写 queryKey 字面量**。
3. 组件里 `useQuery({queryKey: queryKeys.foo(a, b), queryFn:({signal})=>fetchX(...,signal)})`。
4. `select` 若 `new` 了非平凡对象 → 提为模块级常量。
5. 四态兜底：loading / error / empty / business-error（`resultCode`）。

**加一个新提交**：
1. `apiClient.ts` 加 POST 函数（裸字符串 body 记得 `Content-Type: text/plain`）。
2. 组件里 `useMutation`，`onSuccess` 按 `resultCode` 分流。
3. 成功后失效缓存：默认用 `invalidateAllQueries()`；后端返回全量数据用 `setQueryData`；queryFn 闭包即将变化用 `removeQueries`。
4. 失败保留用户输入，别急着关表单。

**跳转**：永远 `navigate(navTo(curPage, table, id, edit))`，别手拼 URL。

**换 server / 换库**：`setServer` 已自动清缓存（见 §A），无需额外处理。

**记住三句话**：URL 决定查什么，API 决定怎么查，React Query 决定查过的是否还能用。
