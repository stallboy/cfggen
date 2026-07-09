# cfgeditor 代码审查报告

> 审查日期：2026-07-09
> 审查范围：`cfgeditor/src/` 全部 ~90 个 TS/TSX 文件 + `src-tauri/` Rust 后端与权限配置（约 11,800 行）
> 审查方法：五轴审查（正确性 / 可读性 / 架构 / 安全 / 性能）。由 5 个并行子代理按子系统深读 + 主审独立复核关键项；所有 Critical/必须修改项均已人工核对源码与行号。

---

## 一、总体结论

cfgeditor 整体架构清晰、类型安全水平高（判别联合 + 类型守卫）、React Query 与 memo 化使用规范，是一份质量在中上的代码库。**但存在若干会在真实使用中触发崩溃、数据丢失或密钥泄露的实打实问题**，集中在三条链路：

1. **安全链路**：AI API Key 明文落盘到**共享配置文件**（S1）；Tauri 的 fs 读权限全开经与维护者确认属**可接受风险**（只读 + 动态目录），仅 **CSP 缺失**建议补上（见 S2）。
2. **健壮性链路**：渲染期正则注入（单字符崩溃）、全局热键重复触发（一次按键 N 次提交）、对后端不可信数据多处缺空值/类型校验（白屏）。
3. **持久化链路**：写盘无防抖/串行化（配置丢失/损坏）、关窗 fire-and-forget（丢偏好）。

修复这 3 条链路 + 1 个正则转义后，即可达到生产可安心维护水平。其余为慢性债，可逐步消化。

### 问题统计

| 严重程度 | 数量 | 说明 |
|---|---|---|
| 🔴 Critical | 3 | S1/S3/S4：密钥泄露 / 单字符崩溃 / 一次按键 N 次提交，建议优先修 |
| 🟠 必须修改 | 17 | 会造成用户可见错误或数据错误的实打实 bug（C5 已转为误报） |
| 🟡 架构/结构性 | 3 | A1 经核查降级（单编辑不变量下不冲突，详见 A1）；剩 A2/A3/A4 |
| 🟢 已确认可接受风险 | 1 | S2 的 `fs:read-all` + `assetProtocol` 全开（只读、动态目录，经维护者确认保留）；CSP 单列为安全加固建议 |
| ⚪ Optional | ~21 | 改进项、一致性、性能（含 `antd lint` 检出的深路径导入） |
| ✅ 已核实为误报 | 2 | `<Alert title>`（FP1）；`map<>` 默认 `[]`（C5/FP2） |

---

## 二、🔴 Critical（建议优先修复）

### S1. AI API Key 明文持久化到**共享**配置文件 + 控制台泄露

- **位置**：`src/store/store.ts:445-448`（`setAIConf`）、`src/store/storage.ts:87-103`（`savePrefAsyncIf`）、`src/store/store.ts:139-144`（`prefKeySet` 计算）、`src/routes/setting/AiSetting.tsx:19,28`
- **问题**：`aiConf`（含 `apiKey`）不在 `prefSelfKeySet` / `notSaveKeySet` 中，因此 `getPrefKeySet()` 把它归入「共享配置」，在 Tauri 下明文写入资源目录的 **`cfgeditor.yml`**（随安装目录分发/共享时泄露）；同时写入浏览器 `localStorage`（明文）。更糟的是 `AiSetting.tsx:19` `console.log(values)` 把 API Key 打到控制台，`AiSetting.tsx:28` 用普通 `<Input>`（非密码框）展示 key。
- **失败场景**：用户配置 DeepSeek key → key 明文落在 `cfgeditor.yml`（若该文件被分享/提交/同步到云）和 localStorage（任何能执行 JS 的注入可读）→ 付费 API Key 泄露。
- **Key 流向（已核实）**：`AiSetting` → `setAIConf` → `localStorage` + `cfgeditor.yml`（明文）；使用时 `Chat.tsx:134` 以 `Authorization: "Bearer " + apiKey` **直接从 webview 发往 AI 提供商**（桌面端可接受，但 devtools/网络面板可见）。
- **修复建议**：
  1. **立即**：把 `aiConf`（或仅 `apiKey`）加入 `prefSelfKeySet`，至少不写入共享的 `cfgeditor.yml`；删除 `AiSetting.tsx:19` 的 `console.log`；`apiKey` 改用 `<Input.Password>`。
  2. **更好**：Key 不应明文落盘，改用 OS 凭据存储（`tauri-plugin-stronghold` 或系统 keychain）。
  3. **收尾**：排查 git 历史是否提交过含真实 key 的 `cfgeditor.yml`，如有立即吊销。

---

### S2. Tauri 权限：fs 读权限已确认可接受，CSP 为唯一保留项（已重评级）

> **修订（2026-07-09，经与维护者确认）**：原报告把 `fs:read-all "**"`、`csp: null`、`assetProtocol`、`withGlobalTauri` 打包判为 Critical。经讨论，本工具定位是「浏览用户**动态配置**的任意目录」，需要读权限是合理且正常的，`**` 读是务实选择。因此 fs 读权限**降级为可接受风险**，CSP 拆出来作为唯一保留项。

- **位置**：`src-tauri/capabilities/migrated.json:18-21`（`fs:read-all` scope `**`）、`src-tauri/tauri.conf.json`（`security.csp: null`、`assetProtocol.scope: ["**"]`、`withGlobalTauri: true`）

**🟢 可接受风险（按维护者判断保留，不必改）**
- `fs:read-all "**"` + `assetProtocol.scope: ["**"]`：应用需读取用户动态配置的、磁盘任意位置的目录，无法构建期白名单。这是**只读**访问，写入仍正确 scope 在 `$RESOURCE/*`（**这个写权限边界要保留，不要扩大**）。对本地单机配置工具属正常行为，类似 VS Code / 文件管理器。**结论：保留。**
- Rust 后端（`lib.rs`/`main.rs`）无自定义 `#[tauri::command]` → **无命令注入面**，设计正确。

**🟠 唯一保留项：补 CSP（`csp: null` → 最小化 CSP）**
- **为什么 CSP 与文件访问正交**：CSP 管的是「脚本/样式/资源从哪加载」，不管 Tauri 的 fs/IPC（走 IPC 桥，不经 CSP）。所以「需要访问文件资源」**不是** `csp: null` 的有效理由——两者互不影响，配了 CSP 也不会挡住任何合法 `readFile`。
- **威胁模型**：`fs:read-all` 本身不危险，它是**倍增器**——只有先发生 JS 注入(XSS) 才能读任意文件并外传。当前应用 XSS 概率低（AI 输出走 XMarkdown 默认转义、字段值 React 自动转义、`Highlight` 安全），但一旦引入（如未来开启 markdown raw HTML、第三方依赖被投毒），无 CSP 时伤害直达「读取 `~/.ssh/id_rsa`/cookie/`.env` 并外传」。**CSP 是堵这条链里成本最低、收益最高的一环**，且不干扰任何文件读取。
- **修复**：配一条放行 asset 协议 + IPC + 本地后端的 CSP，例如：
  ```
  default-src 'self';
  script-src 'self';
  img-src 'self' asset: http://asset.localhost data:;
  connect-src 'self' ipc: http://ipc.localhost http://localhost:*;
  style-src 'self' 'unsafe-inline'
  ```
  （按实际渲染需要微调；`'unsafe-inline'` for style 是 antd 内联样式的常见让步。）改完务必 `pnpm tauri build` 验证图片/资源/AI 请求加载不被破坏。

**⚪ 次要（可选加固，与文件访问无关）**
- `withGlobalTauri: true` → `false`：把 Tauri API 从 `window` 全局拿掉，纯减攻击面。
- `shell:allow-execute` explorer `args: true`（`migrated.json:56-66`）：建议把 args 收紧为路径形式；并评估是否真需要 `shell:default`。

---

### S3. 渲染期正则注入：搜索框输入一个 `(` 即崩溃

- **位置**：`src/flow/EntityCard.tsx:21`
- **代码**：
  ```tsx
  const parts = text.split(new RegExp(`(${keyword})`, "gi"));
  ```
- **问题**：`keyword` 直接来自 `sharedSetting.query`（用户搜索框），未转义就拼进 `new RegExp`。`Highlight` 被 `FlowNode.tsx:196`、`EntityProperties.tsx:67`、`EntityCard.tsx:55,92` 多处调用。
- **失败场景**：用户在搜索框输入 `(`、`*`、`[`、`+`、`\` 等正则元字符 → `new RegExp("(("))` 抛 `SyntaxError` → 渲染期同步抛出 → 整个节点（无 Error Boundary 时整棵 React 树）白屏。
- **修复建议**：转义后再构造正则：
  ```ts
  const escapeRegExp = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const parts = text.split(new RegExp(`(${escapeRegExp(keyword)})`, "gi"));
  ```

---

### S4. 全局热键 `alt+s` 每个节点重复注册 → 一次按键 N 次提交

- **位置**：`src/flow/EntityForm.tsx:585`
- **代码**：
  ```tsx
  const FuncSubmitFormItem = memo(function FuncSubmitFormItem({field}) {
      ...
      useHotkeys("alt+s", () => func.funcSubmit());  // 未指定 scope/ref
  ```
- **问题**：`useHotkeys` 默认全局注册（react-hotkeys-hook v5：同一按键多次注册时所有回调都会触发）。`FuncSubmitFormItem` 是每个可编辑实体节点都会渲染的表单项。
- **失败场景**：画布上有 5 个可编辑节点 → 5 个全局 `alt+s` handler → 用户按一次 `alt+s` → 同时发出 5 条 `recordAddOrUpdate` 请求，5 条记录被同时保存（可能写入脏数据/相互覆盖）。
- **修复建议**：用 ref 限定到当前节点 DOM 子树；或直接删除该热键（提交按钮已是 `htmlType="submit"`，热键收益有限）：
  ```tsx
  const ref = useHotkeys("alt+s", () => func.funcSubmit(), { enableOnFormTags: true });
  return <Form.Item ... ref={ref as any}>...</Form.Item>;
  ```

---

## 三、🟠 必须修改（正确性 Bug）

### 安全/网络（C1–C4，集中在 `api.ts`，建议用「一个共享 axios 实例」一次性解决）

| # | 位置 | 问题 | 修复 |
|---|---|---|---|
| **C1** | `api.ts:23,29,43,57,63,81,101,118,125`、`search/SearchValue.tsx:28` | URL 参数（`table`/`id`/`key`/`q`）未 `encodeURIComponent`，手工字符串拼接。含 `&`/`#`/`+`/空格/中文的 id 会被截断或错位（`id="1#2"`→服务端收到 `id=1`） | 改用 `axios` 的 `params` 选项（自动编码）或 `encodeURIComponent` |
| **C2** | `api.ts:16,23,...`（全部）、`store.ts:428-431` `setServer` 无校验 | 硬编码 `http://${server}/...`。用户填 `https://host` → 变成 `http://https://host`；连远端只能明文 HTTP | `server` 只存 `host:port`，输入时剥离协议；支持完整 origin 并据此选择 http/https；`setServer` 加规范化与校验 |
| **C3** | `api.ts:67-74,84-90,105-113,129-136` | POST 的 `headers` 里塞了 `mode`/`credentials`/`redirect`/`referrerPolicy`/`cache`——这些是 **fetch 的 Request 选项，不是 HTTP 头**，axios 会当作自定义头发出去，既不生效又污染请求；且 `axios.post(..., {method:'POST'})` 冗余 | 删除这些字段；建一个共享 axios instance（`baseURL`/`timeout`/默认 header/interceptor），每个函数只剩 2~3 行 |
| **C4** | `api.ts` 全局 | 无 `timeout`；POST 函数（`addOrUpdateRecord`/`deleteRecord`/`updateNote`/`checkJson`）不接收 `AbortSignal` | 共享 instance 设默认 `timeout`（如 15s）；POST 加可选 `signal` |

### 数据正确性（C5–C8）

| # | 位置 | 问题 | 修复 |
|---|---|---|---|
| **C5** ❌误报 | `routes/table/schemaUtil.tsx:262-263` | ~~`map<K,V>` 默认值应为 `{}`~~ **已核实为误报**：cfggen 把 `map<K,V>` 序列化为 entry 结构体的 **list**（见 schemaUtil 构造 mapEntryType、recordEntityCreator 注释「map is list of $entry」），且 `JSONValue` 的对象变体要求 `$type`，`{}` 根本无法赋值（tsc 直接报错）。原 `[]` 正确，已加注释说明（见 FP2） | 不修改，保留 `[]` |
| **C6** | `routes/record/editingObject.ts:334-356` | `toInt`/`toFloat` 用 try/catch 包裹 `parseInt`/`parseFloat`——但它们**永不抛异常**，非法输入返回 `NaN`，try/catch 是死代码，`NaN` 被静默写回并提交（序列化为 `null` 或被后端拒绝，无提示） | 删 try/catch，改 `Number.isNaN(n) ? 0 : n`（与 `schemaUtil.tsx:355-357` `getNextId` 已有的 `isNaN` 写法一致） |
| **C7** | `routes/record/recordEditEntityCreator.ts:325,339` | `const impl = getImpl(sInterface, implName) as SStruct;` 强转吞掉 null。`getImpl` 返回 `SStruct \| null`，当记录数据 `$type` 指向不存在的 impl 时 → `impl.fields` 抛 TypeError → 整个 `useMemo`（`Record.tsx:85`）抛出 → 白屏 | 加 null 检查（与 `embeddingChecker.ts:177-178` 一致） |
| **C8** | `routes/record/embedding/embeddingChecker.ts:175-176`、`embeddingFieldExtractor.ts:32` | `const type = fieldValue['$type'] as string;` 后立即 `type.split('.').pop()`。若 interface 字段对象缺 `$type`（后端脏数据/新旧 schema 不一致）→ `undefined.split` 抛 TypeError → 白屏 | `if (typeof type !== 'string') return false;` 早退 |

### AI 聊天链路（C9–C11，均在 `Chat.tsx`）

| # | 位置 | 问题 | 修复 |
|---|---|---|---|
| **C9** | `routes/add/Chat.tsx:72` | `content.replace("/\n\n/g", "<br/><br/>")` —— 第一个参数是**字符串字面量**（双引号内 `\n` 已被解析为真换行），不是正则 `/\n\n/g`。`.replace(string,...)` 只替换字面子串 `/\n\n/g` 的首次出现，对真正的 `\n\n` 毫无作用；且即使匹配，`<br/>` 也会被 markdown 转义。是死代码 | 直接删除这两行——`XMarkdown` 本就按 markdown 规范处理换行 |
| **C10** | `routes/add/Chat.tsx:123-126` | `isAiSet = aiConf.baseUrl.length > 0` 永真（store 默认 baseUrl 是 deepseek URL 非空），而默认 `apiKey` 为 `''`。结果新用户对 deepseek 发 `Bearer `（空）→ 401，且无任何引导降级 | `isAiSet` 应同时校验 `apiKey`；未配置时禁用 Sender 并提示「请先在设置中配置 AI」，而不是走硬编码的 ant.design demo 端点 fallback |
| **C11** | `routes/add/Chat.tsx:151,112` | `JSON.parse((chunk as any).data)` 与 `JSON.parse(result.jsonResult)` 均无 try/catch。AI 网关下发非 JSON 帧（`[DONE]`/keepalive）或后端返回非法 JSON 时，异常从 `onSuccess` 抛出无人捕获 → 整条流处理中断，用户看不到任何错误 | 包 try/catch，对 `[DONE]` 等哨兵显式跳过，失败时 `setInputValue` 提示 |

> 附：`Chat.tsx` 的 `console.log('sse object', chunk)`（`:179`）会持续打印每个 SSE chunk 到控制台，建议一并移除。

### 状态/持久化（C12–C15）

| # | 位置 | 问题 | 修复 |
|---|---|---|---|
| **C12** | `store.ts:399-413` `setFixedPagesConf` | 判断「dragPanel 是否引用了已不存在的页面」时，内置面板排除名单是 `none/recordRef/finder/adder/setting`，**漏了 `'chat'`**（`store.ts:41` 注释与 `CfgEditorApp.tsx:120` 都明确 `chat` 是合法面板） | 内置面板统一为常量数组 `BUILTIN_PANELS`，用 `includes` 判断；同时核实 `'adder'`（在排除名单里但无渲染分支，疑似死代码） |
| **C13** | `CfgEditorApp.tsx:62-80` | React Query 的 `select` 回调内调用 `clearLayoutCache()`（`queryClient.removeQueries`）——`select` 在 RQ 内部会被反复调用（重渲染、相等性比较），导致已算好的 `['layout']` 缓存被非预期清空、布局反复重算 | 副作用移到 `useEffect`：`useEffect(() => { if (schema) clearLayoutCache(); }, [schema])`；`select` 保持纯转换 |
| **C14** | `storage.ts:98-119` | `setPref` → 每次都把**整个** `prefKeySet` 序列化并 `writeTextFile` 全量重写 `cfgeditor.yml`，无 await、无串行化、无 debounce。`navTo`（`store.ts:528-530`）一次产生 3 次 `setPref`；快速导航 → 多个 `writeTextFile` 并发对同一文件 → 轻则丢字段，重则文件损坏 | 引入 debounce（300-500ms）合并写入；写入链路加 mutex 保证串行；`saveSelfPrefAsync` 同理 |
| **C15** | `main.tsx:67-69`、`storage.ts:109-112` | 窗口关闭时 `onCloseRequested(saveSelfPrefAsync)` —— `saveSelfPrefAsync` 内部 `...catch(log)` 不返回 promise，回调立即返回，窗口可能在 `writeTextFile('cfgeditorSelf.yml')` 完成前被销毁 → `query`/`curPage`/`curTableId`/`curId` 等自身会话态丢失 | 回调改 `async (event) => { await saveKeySetPrefAsync(...); }`；必要时 `event.preventDefault()` 在写入 resolve 后再 `window.destroy()` |

### 资源/输入（C16–C18）

| # | 位置 | 问题 | 修复 |
|---|---|---|---|
| **C16** | `flow/ResPopover.tsx:26-27` | 为每个字幕文件 `URL.createObjectURL(blob)`，但全文件无任何 `URL.revokeObjectURL`；结果被 React Query 缓存（`['vtt', path]`）。反复打开/关闭不同视频的 ResPopover → blob URL 累积 → webview 内存持续增长无法回收 | 组件卸载或缓存替换时 revoke：`useEffect(() => () => vttUrls?.forEach(u => URL.revokeObjectURL(u)), [vttUrls])`；或用 `data:` URL |
| **C17** | `flow/NoteShowOrEdit.tsx:46-53` | 提交 note 网络失败时 `onError` 立即 `setIsEdit(false)` → `NoteEdit` 卸载 → 用户刚输入的 `newNote` 本地状态销毁 | `onError` 不要关编辑框，保持打开仅提示错误，让用户重试 |
| **C18** | `recordEditEntityCreator.ts:91-92`、`recordEntityCreator.ts:65-99` | `const ft = typeof fieldValue; if (ft != 'object') continue;` —— `typeof null === 'object'`，null 不会被跳过，随后对 null 调用内嵌检查 → TypeError 白屏 | `if (ft !== 'object' \|\| fieldValue === null) continue;` |

---

## 四、🟡 架构 / 结构性问题

### A1. ❌ 经核查降级：`editState` 单例与分屏布局**不冲突**（依赖「单编辑」不变量，当前安全）

- **位置**：`routes/record/editingObject.ts:33-45`（单例）、`Record.tsx:112,122-124`
- **原质疑**：编辑会话状态挂在进程级全局变量上，分屏后两个 Record 编辑面板会互相覆盖 `editState`。
- **核查结论：不成立**。失败场景需要「两个并发的可编辑 Record 实例」，而当前架构保证至多一个：
  1. **可编辑 Record 只挂在主面板唯一 `<Outlet/>`**：路由 `edit?/record/:table/*`（`main.tsx:40`），`CfgEditorApp.tsx:170` 全应用仅一个 `<Outlet/>`；且 `Record` 带 `key={`${curTableId}-${curId}`}`（`Record.tsx:307`），切换记录是卸载旧实例再挂载新实例（**串行，非并发**）。
  2. **左面板（`dragPanel`）在代码与 UI 上都不可能渲染 Record**：取值仅 `recordRef/finder/chat/setting/none/固定页面 label`（`HeaderBar.tsx:78-87`、`store.ts:41`），`CfgEditorApp.tsx:106-158` 各分支分别渲染 `RecordRef`/`Finder`/`Chat`/`Setting`/固定页——**全是只读或非编辑组件**；即便手动把 `dragPanel` 塞成 `'record'`，也会落到 `getFixedPage` 查无此 label → `dragPage=null` → 不分屏，仍不渲染 Record。
  3. **固定页面只有只读类型**：`FixedPage` 仅 `FixedRefPage`/`FixedUnrefPage`（`storageJson.ts:21,67,79`），都渲染只读 `RecordRef`（`CfgEditorApp.tsx:132-154`）。
  4. **`editState` 消费方全在编辑 Record 链路**：`Record.tsx`、`recordEditEntityCreator.ts`、`editingObject.ts` 内部；`AddJson.tsx:59`/`Chat.tsx:112` 只用 `applyNewEditingObject` 作「生成新记录→导航进编辑」的**串行**生产者→消费者通道，非并发编辑。
- **保留的合理内核（防御性备注，非缺陷）**：单例的安全**依赖一条不变量**——「应用同一时刻至多一个可编辑 Record」。该不变量当前由 `CfgEditorApp` 的渲染结构**隐式**保证，无类型/断言强制。因此：
  - **现状不改代码**：A1 原建议的「收敛为 `useRef`」在单编辑架构下无功能收益，反而会切断 `Chat`/`AddJson` → `Record` 现有的单例传递通道，代价大于收益。
  - **未来风险**：若哪天把左面板改成可渲染编辑 Record（或 Outlet 多实例），单例会立刻出错——届时需重构整个面板系统，不是现在加 `useRef` 能预防的。

### A2. `joinPath` 路径穿越防护不完整

- **位置**：`res/resUtils.ts:38-59`
- **问题**：`while (selfPath.startsWith('../') || selfPath.startsWith('..\\'))` **只剥离开头的 `..`**。输入 `a/../../../etc/passwd`（不以 `../` 开头）循环直接跳过，返回 `baseDir + sep + "a/../../../etc/passwd"`；绝对路径 `C:\windows`、`/etc/passwd`、`\\server\share` 也未拦截。`_path` 数据源是后端 record 的 `ref.toId`（`findAllResInfos.ts:60-61`）与用户配置，不属于完全可信数据。
- **失败场景**：恶意/脏的 `ref.toId` → 拼出的资源路径越界读取。
- **修复建议**：拼接后做规范化（canonicalize）并校验结果仍以 `baseDir` 为前缀，否则拒绝；Windows 单独拦截盘符/UNC。
- **缓解说明**：Tauri fs 写权限 scope 为 `$RESOURCE/*`，写越界大概率被 capability 拦截；但读路径受 `fs:read-all "**"`（见 S2）影响，所以 S2 收紧前这条算深度防御缺口，S2 收紧后这条也要补规范化。

### A3. 内嵌判定条件在 checker 与 extractor 两处复制 → 判定漂移 → 字段凭空消失

- **位置**：`embedding/embeddingFieldExtractor.ts:102-131` 逐行复制自 `embedding/embeddingChecker.ts:61-89`；并在 `recordEditEntityCreator.ts` 中 `createEntity`（`:122-150,212-227`）与 `tryCreateEmbeddedFieldForStruct`/`extractEmbeddedFieldData`（`:550-656`）两处独立计算「是否内嵌」
- **问题**：embedding 子系统本是为消除重复而拆分，但提取器里又把 5 条内嵌条件原样抄了一遍。同一个 struct/interface 字段是否内嵌，在 `createEntity`（决定是否建子节点）和 edit-field 构造路径（决定是否返回 embeddedField）里各算一遍。任一处返回 null → 回退成普通 structRef。
- **失败场景**：`canBeEmbeddedCheck` 返回 true → `createEntity` 走 `continue` 不建子节点；但 `extractEmbeddedFieldData` 因内部重复校验返回 null → 字段回退成指向空 `'[]'` 的普通 structRef，却无对应子节点 → 字段「消失」或显示为断链。未来改 `EMBEDDING_CONFIG` 条件只改一处忘改另一处 → 必然触发。
- **修复建议**：删除提取器里的 `canEmbedWithConfig`（提取器只负责「取值」，假定调用方已校验）；或让 checker 暴露单一 `matchesAnyCondition` 给两边复用；把「可否内嵌」与「提取内嵌字段」合并为单一职责调用，结果在 `createEntity` 与 `makeEditFields` 间**共享**。

### A4. `FoldStateHelper` ↔ `recordEditEntityCreator` 循环依赖 + 层级倒置

- **位置**：`recordEditEntityCreator.ts:28` import `FoldStateHelper`；`flow/embedded/FoldStateHelper.tsx:1` import `Folds`
- **问题**：`flow/embedded/FoldStateHelper`（应属底层通用层）反向依赖 `routes/record/recordEditEntityCreator`（路由层）里的 `Folds` 类，形成 ES module 循环。当前因双方都在函数体内使用、不在模块顶层求值，运行时侥幸不崩，但属于架构 smell。
- **修复建议**：把 `Folds` 下沉到 `flow/embedded/` 或独立文件（它只是 `(string|number)[][]` 的包装，不依赖路由层），打破环并纠正分层。

---

## 五、⚪ Optional（改进项，按主题归并）

### 性能
- **O1 `flow/layoutAsync.ts:51,89`**：elkjs 默认引入主线程实现，`elk.layout` 的 async 只是 Promise 包装，分层/MR 树算法是同步阻塞。几十~几百节点时单次布局数百 ms~数秒，期间整个 webview冻结。建议改用 `elkjs/lib/elk-worker.min.js` + Web Worker（`useEntityToGraph.tsx:103` 已是 useQuery 异步，迁移成本低）。
- **O2 `flow/FlowNode.tsx:230` + `EntityForm`**：`title` 的 `useMemo` 依赖整个 `nodeProps`；React Flow 节点拖拽时 `positionAbsoluteX/Y` 每帧变化 → `nodeProps` 引用每帧变 → title 及内部按钮、`EntityForm` 子组件（接收 `nodeProps`）每帧重建，击穿 memo。建议把坐标从 `nodeProps` 单独取出作依赖。
- **O3 `routes/record/Record.tsx:85-120`**：`useMemo` 依赖整个 `addOrUpdateRecordMutation` 对象（`isPending`/`isSuccess` 状态变化即引用变化）→ 提交一次记录后整个 useMemo 重算（`structuredClone` + 深比较 + 实体重建 + re-layout）。建议只把稳定的 `mutate` 放进依赖。
- **O4 `routes/record/recordRefEntity.ts:161→47→27-34`**：引用链接存在性检查 `isRefIdInBriefRecords` 对每条记录的每个 ref 线性扫全部 briefRecords，总体 O(R·F·B)。被大量表引用的核心表（B 上千）会明显卡顿。建议预构 `Set<"table|id">` 降为 O(R·F)。
- **O5 `routes/search/LastAccessed.tsx:62`**：未启用 `virtual`（`LastModified`/`SearchValue` 已启用 `virtual={length>30}`）。`history.items` 无上限，长期使用后可能渲染上百行无虚拟化行。
- **O6 `routes/record/editingObject.ts:66,78`**：`startEditingObject` 同表同 id 不等路径里两次 `structuredClone` + `delete$refInPlace`（`:66` 的克隆被丢弃），大记录双倍深拷贝。复用 `:66` 的克隆即可。
- **O7-antd 🔧 antd 深路径导入（`antd lint` 权威检出）**：以下 6 处从 antd 内部路径 `antd/es/*` 或 `antd/lib/*` 导入，违反 antd 最佳实践、破坏 tree-shaking 一致性，且 `es`(ESM)/`lib`(CJS) 混用、跨版本易碎。`antd lint` 把其中 3 处 default 导入直接判为 `performance: error`：
  - `flow/EntityForm.tsx:21` `import TextArea from "antd/es/input/TextArea"` ⚠️ lint error
  - `flow/NoteShowOrEdit.tsx:3` `import TextArea from "antd/es/input/TextArea"` ⚠️ lint error
  - `routes/setting/ThemeSetting.tsx:7` `import Title from "antd/lib/typography/Title"` ⚠️ lint error（注意是 `lib` = CJS 构建）
  - `flow/EntityCard.tsx:2` `import type {DescriptionsItemType} from "antd/es/descriptions"`
  - `routes/add/AddJson.tsx:12` `import {ResultStatusType} from "antd/es/result"`
  - `routes/setting/FixPages.tsx:9` `import {useForm} from "antd/es/form/Form"`
  - **修复**：改为从包根具名导入——`TextArea` → `import { Input } from "antd"` 后用 `Input.TextArea`（或 `const { TextArea } = Input`）；`Title` → `import { Typography } from "antd"` 后 `Typography.Title`；`useForm` → `import { useForm } from "antd"`（根已导出）；类型（`DescriptionsItemType`/`ResultStatusType`）改用根导出的等价类型（如 `DescriptionsProps['items'][number]`、`ResultProps['status']`）。修后跑 `antd lint ./src` 确认归零。

### 正确性/健壮性
- **O7 `store.ts:257-355`**：所有数值 setter 用 `if (value)` 过滤，无法写入 `0`（`searchMax` 设 0 表示无上限等场景将静默失效）。改 `if (value != null)`。
- **O8 `storage.ts:64`**：`readPrefAsyncOnce` 启动时先 `localStorage.clear()` 再读配置；若 `readConf` 解析失败，先前偏好已被清空。建议读成功后再覆盖，或单文件 try/catch + 提示。
- **O9 `storage.ts:78-84`**：`localStorage.setItem(key, value)` 对 YAML 中布尔/数字/对象值会 toString 成 `"true"`/`"[object Object]"`。当前全是标量勉强 OK，但若有人把 YAML 写成嵌套会数据损坏。
- **O10 `AppLoader.tsx:6-25`**：YAML 解析错误被 eslint-disable 后静默吞掉，用户得到「静默回退默认配置」无任何提示。建议展示可关闭 Alert。
- **O11 `editingObject.ts:224-236`**：`onMoveItemInArray` 实际是 **swap**（交换两元素），与命名/参数（`curIndex`/`newIndex`）语义不符。当前调用方都传相邻索引（相邻 swap 等价 move）所以能跑；未来 `onMoveItemInArray(0,3)` 会顺序错乱。建议改名或实现真正的 splice move。
- **O12 `recordEditEntityCreator.ts:434-444`**：`getPrimitiveValue` 用真值判断 `if (fieldValue)`，对 `0/false/''` 走默认值分支（当前恰好与默认值相等故「看起来对」）。与 `embeddingFieldExtractor.ts:136-141` 的 `value !== undefined && value !== null` 不一致。建议统一。
- **O13 `embedding/types.ts:38-41,46`**：`EmbeddingCheckResult`、`FieldDataType` 全仓库无引用，死代码，可删。

### 一致性/重复
- **O14 `i18n.ts`**：`zh` 段含大量 `en` 段没有的 key（`basicSetting`/`finder`/`aiSetting`/`resourceSetting`/...），英文用户看到裸 key。建议补齐 en 翻译 + 加一条对比 `Object.keys(en)` vs `Object.keys(zh)` 的测试/lint 防漂移。
- **O15 `routes/search/Query.tsx:16-44` vs `routes/add/Adder.tsx`**：`Query` 的 tabs 数组**完整包含** `Adder` 的 Chat + AddJson，用相同 key。两处重复，维护易分叉。建议 `Adder` 复用 `Query` 或抽公共 tabs 配置。
- **O16 `routes/table/Table.tsx` vs `TableRef.tsx`**：`TableRef` 用了 `memo`+`useCallback`+`useMemo`，`Table.tsx` 完全没有，每次渲染都重建 `entityMap`/`creator`/菜单。建议对齐 memo 化（`entityMap` 放进 `useMemo`，依赖 `schema/curTable/maxImpl`）。
- **O17 `search/SearchValue.tsx`**：全文件唯一用原生 `fetch`（其它都走 `api.ts` 的 axios），且未检查 `response.ok`。建议统一改 axios/React Query。
- **O18 `setting/colorUtils.ts:33-37`**：`toggleFullScreen`（被 `Operations.tsx`、`HeaderBar.tsx` 引用）放在 `colorUtils` 命名不当，建议拆到独立 `windowUtils.ts`。
- **O19 `setting/themeService.ts:38-40`**：`getThemePath(themeFile){ return themeFile; }` 空操作；`:14-16` `AntdThemeConfig.components` 索引签名 `[key:string]: never` 使任何 component 配置都无法表达，`validateThemeConfig` 形同虚设。
- **O20 `setting/TauriSetting.tsx:14-17`**：`onFinishTauriConf(values: never)` 类型错误，应给 `TauriConf` 类型。
- **O21 `flow/entityToNodeAndEdge.ts:30-53,70`**：`fillHandles`/`convertNodeAndEdges` 直接 mutate 传入的 entity/field 对象（设 `handleIn/handleOut/sharedSetting`），副作用式 API，被 `useMemo` 调用。建议改纯函数（被「entityMap 每次重建」掩盖的隐性风险）。
- **O22 `resso.ts:17`**：`process.env.NODE_ENV` 在 Vite 下功能正常（构建期 define 替换），但 Vite 推荐写法是 `import.meta.env.DEV`；非 Vite 环境会因 `process` 未定义抛错。
- **O23 残留调试 `console.log`**：`useEntityToGraph.tsx:56`、`entityToNodeAndEdge.ts:36,49,52`、`layoutAsync.ts:52,95,100`、`Chat.tsx:179` 等，建议生产环境移除。
- **O24 `routes/record/Record.tsx:291`**：`if (isLoading) { return; }` 返回 undefined 无任何反馈（对比 isError 有 Result），建议返回骨架/spinner。
- **O25 `flow/CustomAutoComplete.tsx:34`**：`onSearch: onChange` 把每次按键的搜索中间态写回表单值，可能在 `onSelect` 前把非法前缀串持久化。建议确认意图，必要时 `onSearch` 只更新内部搜索词。
- **O26 `res/readResInfosAsync.ts:155-168`**：`alreadyRead` 一进入就置 true，中途抛错（已被 catch 吞掉）或 `isTauri()` 为 false 时调用方以为成功但 `resMap` 为空且不重跑；`:15-66` `readDir` 递归无深度/数量上限。建议成功完成才置位 + 加阈值。

### 可读性（Nit）
- `FlowNode.tsx:95-103` 多条 `useCallback`/`useMemo` 挤同一行，建议独立成行。
- `getDsLenAndDesc.tsx`/`getResBrief.tsx`/`embedded/FoldStateHelper.tsx` 无 JSX 但后缀 `.tsx`，应为 `.ts`。
- `recordEditEntityCreator.ts`（778 行）：`RecordEditEntityCreator` 单类承担「创建实体节点 + 创建编辑字段 + 内嵌判定 + fold 读取 + 自动补全」五项职责，建议把内嵌相关抽到独立 `EmbeddedFieldBuilder`，类可降到 ~400 行。
- `editingObject.ts:388` `delete$refInPlace`（单数）实际删的是 `$refs`（复数），建议改名 `delete$refsInPlace`。

---

## 六、✅ 已核实为误报（不要改）

### FP1. `<Alert title={...}/>` 在 antd v6 中是**正确**用法

- **位置**：`routes/PathNotFound.tsx:9`、`CfgEditorApp.tsx:197`
- **原始质疑**：有审查认为 antd `Alert` 没有 `title` prop、应为 `message`，导致错误信息不显示。
- **核实结论**：**错误**。已查阅 `node_modules/antd/es/alert/Alert.d.ts`（antd 6.5.0）：
  ```ts
  /** Content of Alert */
  title?: React.ReactNode;
  /** @deprecated please use `title` instead. */
  message?: React.ReactNode;
  ```
  antd v6 已把 `Alert` 的主文本 prop 从 `message` 改为 `title`，`message` 标记为 deprecated。整个代码库一致使用 `title`，**无误**。同一变更也适用于 `notification.xxx({ title })`。**此处无需修改。**

> **权威复核**：已用 `@ant-design/cli`（v6.5.0，与项目一致）验证——`antd info Alert --version 6.5.0` 确认 `title` 为有效 prop；`antd lint ./src` 在全代码库**未发现任何 deprecated 用法**（deprecated 计数为 0）。结论可靠。

> 说明：本项特意保留在报告中，是为了体现审查的诚实性——AI 生成的审查结论需要人工复核，避免按误报去「修复」本就正确的代码。

### FP2. `map<K,V>` 字段默认值 `[]` 是**正确**的（C5 误报）

- **原始质疑**：C5 认为 `schemaUtil.tsx:262` 的 `map<` 分支给 `[]` 是 bug，应为 `{}`。
- **核实结论**：**错误**。cfggen 把 `map<K,V>` 序列化为 **entry 结构体的 list**（`schemaUtil.tsx:32-43` 构造 `mapEntryType` 作为 SStruct；`recordEntityCreator.ts:67` 注释「list or map, (map is list of $entry)」）。且 `recordModel.ts` 中 `JSONValue` 的对象变体是 `JSONObject & Refs`——要求 `$type: string`，普通 `{}` 无法赋值（尝试 `{}` 时 tsc 直接报 `Type '{}' is not assignable to type 'JSONValue'`）。因此 `list<` 与 `map<` 共用 `[]` 默认值是正确的。已在原代码补注释说明，**不改行为**。

---

## 七、优点（不需要改，值得保持）

- **数据模型类型化水平高**：`flow/entityModel.ts` 用判别联合（`Entity`/`EntityEditField`）+ 类型守卫；`flow/embedded/typeGuards.ts` 用 `getFieldValueSafely` 重载消除 `as` 断言；`api/*.ts` 结果码用字面量联合类型。整体类型安全做得好。
- **`routes/table/schemaUtil.tsx` 的 `Schema` 类**（450 行）：高内聚领域模型，BFS 依赖收集（`getAllDepStructs`）与缓存（`item.refTables`）设计得当，正确处理 interface/impl 展开和 map entry 类型合成。**450 行可接受，不建议强拆**。
- **`flow/EntityForm.tsx`（771 行）**：虽大，但内部按字段类型拆成多个独立 `memo` 子组件，职责单一，主要是多字段类型渲染分支，拆文件收益有限，不建议强拆。
- **React Query 使用规范**：`queryKey` 设计合理（含 depth/maxNode 参数）、`staleTime`/`enabled`/`virtual` 用得到位（`UnreferencedButton`、`LastModified`、`SearchValue`），错误态用 `Result`/`notification` 统一呈现。
- **Creator 模式职责清晰**：`RecordEntityCreator`（只读）/ `RecordEditEntityCreator`（可编辑）/ `createRefEntities`（引用）三条数据来源独立、互不污染；`Folds` 用不可变更新（`setFold` 返回新实例）。
- **Rust 后端极简**：无自定义命令 → 无命令注入面，安全设计正确。
- **`storageJson.ts`** quicktype 自动生成，提供运行时类型守卫，解析外部 JSON 有防护。
- `Operations.tsx` 的 `onToPng`（`html-to-image`）错误处理完整（catch + 友好提示）。

---

## 八、修复优先级路线图

建议按以下顺序分批提交（每批控制在 ~100-300 行，便于 review）：

**第 1 批（安全 + 崩溃，最高优先级）**
- S1（API Key 存储/日志/输入框）— 含 git 历史排查
- S3（Highlight 正则转义）
- S4（alt+s 热键 scope）
- C5、C7、C8、C18（对不可信数据的崩溃性 bug）
- 注：S2 的 fs 读权限按可接受风险保留；其中 **CSP 补全**可与本批一起做（低成本、纯加固），`withGlobalTauri`/`shell args` 收紧可顺带

**第 2 批（api.ts 重构，一次性解决 C1/C2/C3/C4）**
- 建共享 axios instance（baseURL 派生 + timeout + 默认 header + interceptor + params 自动编码），消除四处重复与脏 header

**第 3 批（持久化可靠性 C13/C14/C15）**（A1 经核查降级，无需改动，详见 A1）

**第 4 批（AI 聊天健壮性 C9/C10/C11 + C16/C17）**

**第 5 批（结构性 A2/A3/A4 + 剩余 C6/C12 + Optional 性能项 O1-O6）**

**第 6 批（一致性/重复/死代码 O14-O26，可与功能迭代并行）**

---

## 九、实现进度（2026-07-09）

第 1、2 批已在分支 `fix/code-review-batch-1-2` 上按 increment 提交，全部通过 `tsc --noEmit` + `eslint` + `vite build` + `antd lint`（无新增 deprecated/性能问题）。

| 项 | 状态 | Commit | 说明 |
|---|---|---|---|
| **第 2 批 C1–C4**（api.ts） | ✅ | `ee243a3` | 共享 axios 实例：params 自动编码、`normalizeServer` 剥协议、15s 超时、删 fetch-only headers、POST 加可选 signal。已核对后端 `EditorServer.queryToMap` 存在性语义，flag 空串与原无值 flag 字节等价 |
| **S3**（Highlight 正则注入） | ✅ | `c962928` | `escapeRegExp` + 空 keyword 短路 |
| **C7**（getImpl 强转吞 null） | ✅ | `c962928` | 去掉 `as SStruct`，两处 null 守卫 |
| **C8**（`$type` 缺失崩溃） | ✅ | `c962928` | checker + extractor 两处 `typeof type !== 'string'` 早退 |
| **C18**（null 字段值崩溃） | ✅ | `c962928` | 读写两条路径 `typeof === 'object'` 增加 `!== null` |
| **C5**（map 默认值） | ❌误报 | `c962928` | 核实为误报（FP2），保留 `[]` 并加注释 |
| **S4**（alt+s 全局重复触发） | ✅ | `3da24d5` | 热键上移至 EntityForm，RefCallback 绑定表单子树 + `enableOnFormTags`，每节点仅一次且按焦点触发 |
| **S1**（API Key 存储/日志/输入） | ✅ | `f05ad5b` | `aiConf` 移入 `prefSelfKeySet`（只写 gitignore 的 cfgeditorSelf.yml）；删 `console.log(values)`；`Input.Password`。git 历史无泄露 key |

**未在本批实现（需运行期验证）**
- **S2-CSP**（`tauri.conf.json` 的 `csp: null`）：CSP 必须放行 asset 协议、IPC、**以及 AI 提供商的 https**（否则会直接掐断 Chat），正确性依赖运行时加载验证，盲改有白屏/断 Chat 风险。建议在能跑 dev server 的环境里增量调（加 CSP → 跑 → 看 console 违规报告 → 收紧），而非一次性盲配。
- **S2-`withGlobalTauri`/`shell args`**：纯减面，可与 CSP 一起在运行期验证时顺手改。

**运行期未验证说明**：本环境无法启动 Tauri 桌面端 + Java cfggen 后端做端到端验证，上述均已通过编译/lint/build 静态验证；params 编码、热键作用域、Key 持久化路径建议在实机回归一次。

---

### 第 3-6 批实现进度（2026-07-09，分支 `fix/code-review-batch-3-6`）

按 increment 分多次提交，每批均通过 `tsc --noEmit` + `eslint` + `vite build`。遵循「不确定的先不做并记录」原则。

| 批 | 项 | 状态 | Commit | 说明 |
|---|---|---|---|---|
| 第3批 | C13/C14/C15 | ✅ | `c2700e3b` | select 副作用移 useEffect；setPref debounce(300ms)+写入串行化；关窗 async+preventDefault+destroy |
| 第4批 | C9/C10/C11/C17 | ✅ | `6e1182ec` | 删 Chat replace 死码；isAiSet 校验 apiKey；两处 JSON.parse 防御；NoteEdit 失败不关框；删 sse console.log |
| 第5批 | C6/C12/O3/O5/O6 | ✅ | `02a85de1` | toInt/toFloat NaN 守卫；BUILTIN_PANELS 补 chat；Record memo 依赖改 mutate；LastAccessed virtual；startEditingObject 复用 clone |
| 第5批 | A4 | ✅ | `90a51b19` | Folds 下沉到 flow/embedded/Folds.ts，打破 FoldStateHelper↔recordEditEntityCreator 循环依赖 |
| 第5批 | A3 | ✅ | `6120ca1d` | 删除 extractor 复制的 canEmbedWithConfig，复用 checker.matchesAnyCondition（消除 5 条内嵌条件漂移；深层"判断+提取合并"未做） |
| 第6批 | O13 + O11 改名 | ✅ | `b1f7e46b` | 删 embedding 死类型；onMoveItemInArray→onSwapItemInArray（实现保持 swap，调用方均为相邻索引） |

### 未做 / 待评估项（不确定或有回归风险，先记录）

**第4批**
- **C16（ResPopover blob URL revoke）**：未做。VideoAudioSyncer 用 useQuery 缓存 blob URL 无 revoke 导致内存累积；但 React 19 StrictMode 开发模式 effect 双调用 + `gcTime:0` 方案会让字幕 URL 被提前 revoke → 重开同一视频字幕失效。需在能跑 dev 的环境实机验证 revoke 时机（建议改 queryFn 返回 blob、组件挂载 createObjectURL / 卸载 revoke）后再做。

**第5批**
- **A2（joinPath 路径穿越）**：未做。`resUtils.joinPath` 只剥离开头 `..`，未规范化+前缀校验/拦截盘符 UNC。修复需读全调用方（findAllResInfos 等）确认合法相对路径不被误拦，回归风险中等，待专项。
- **A3 深层（「可否内嵌」与「提取内嵌字段」合并为单一职责、结果在 createEntity 与 makeEditFields 间共享）**：未做。A3 的**核心去重已完成**（`6120ca1d`：extractor 删除逐行复制的 `canEmbedWithConfig`、改复用 `EmbeddingConditionChecker.matchesAnyCondition`，5 条内嵌条件单一来源，checker 与 extractor 判定永远一致，消除「checker 说能内嵌、extractor 判定不能 → extractFields 返回 null → 字段回退断链」的漂移）；但报告选项3 的数据流合并（让 createEntity 与 edit-field 构造共享同一次「是否内嵌 + 提取」结果，而非各自调 canBeEmbeddedCheck + extract）风险高，仍留专项。
- **O1（elkjs 主线程→Web Worker）**：未做。elkjs 默认主线程实现，`elk.layout` 的 async 只是 Promise 包装，几十~几百节点时阻塞 webview。迁移到 `elkjs/lib/elk-worker.min.js` + Worker 改动较大，待专项。
- **O2（FlowNode title useMemo 依赖整个 nodeProps）**：未做。React Flow 拖拽时 positionAbsoluteX/Y 每帧变化击穿 memo。需读 FlowNode/EntityForm 把坐标从 nodeProps 拆出，中风险。
- **O4（isRefIdInBriefRecords 线性扫 O(R·F·B)）**：未做。需读调用方预构 `Set<"table|id">`，中。

**第6批（一致性/重复，多为低优先级，逐项可独立做）**
- **C10 后半**：isAiSet 已校验 apiKey（核心 bug 已修），但「未配置时禁用 Sender + 移除硬编码 ant.design demo 端点 fallback + 配置提示」未做（Sender disabled API 未确认、i18n key 待补）。
- **O14（i18n en 段缺 key）**：未做。zh 段含大量 en 段没有的 key，英文用户看裸 key。需补齐翻译 + 加 key 漂移测试，工作量大。
- **O15（Query/Adder tabs 重复）**：未做。两处独立维护相同 tabs，建议抽公共配置。
- **O16（Table.tsx 未 memo）**：未做。兄弟 TableRef 用了 memo，Table 未对齐。
- **O17（SearchValue 原生 fetch）**：未做。可改用第2批建立的共享 axios 实例 + `response.ok` 检查。
- **O18（toggleFullScreen 放在 colorUtils）**：未做。命名不当，建议拆 windowUtils。
- **O19（themeService getThemePath 空操作 / components `[key:string]:never`）**：未做。validateThemeConfig 形同虚设，需读后修。
- **O20（TauriSetting onFinishTauriConf(values: never)）**：未做。类型错误，应给 TauriConf 类型（低风险，可顺手做）。
- **O21（fillHandles/convertNodeAndEdges 原地 mutate 入参）**：未做。被 useMemo 调用的副作用式 API，改纯函数中-高风险。
- **O22（resso.ts process.env.NODE_ENV）**：未做。resso.ts 是 vendored 库代码，Vite 下 `process.env.NODE_ENV` 构建期 define 正常工作；改 `import.meta.env.DEV` 虽更规范但属修改 vendored 代码、升级时易冲突，暂留。
- **O23（残留 console.log）**：部分已清理（storage 的 log 随 C14、Chat 的 sse log 随第4批）；其余散落 useEntityToGraph/entityToNodeAndEdge/layoutAsync/Operations/readResInfosAsync 等，建议统一清理。
- **O24（Record isLoading 返回 undefined 无反馈）**：未做。建议返回骨架/spinner。
- **O25（CustomAutoComplete onSearch 写回表单值）**：未做。`onSearch=onChange` 可能持久化非法前缀，需确认意图。
- **O26（alreadyRead 提前置位 / readDir 无深度上限）**：未做。中途失败被 catch 吞、调用方误判成功，需成功才置位 + 阈值。
- **O11（onMoveItemInArray → onSwapItemInArray）**：已重命名（实现保持 swap 不变）。所有调用方均为相邻索引（上/下移按钮传 `arrayIndex-1` / `arrayIndex+1`），相邻 swap 即移动一格，swap 是正确语义；原命名含 move 与 swap 实现不符，故改名并对称化参数（curIndex/newIndex → indexA/indexB），而非改实现。
- **可读性 nit**：`.tsx`→`.ts`（FoldStateHelper/getDsLenAndDesc/getResBrief 无 JSX）、`delete$refInPlace`→`delete$refsInPlace`（实删 `$refs` 复数）、FlowNode 多个 useCallback/useMemo 挤同一行。

**第3-6批运行期未验证**：同第1、2批，仅静态验证（tsc/eslint/build）。C14 的 debounce/串行化、C15 的关窗写盘、Chat 流式 JSON 防御、A4 的 Folds 模块拆分，建议在能跑 Tauri+Java 后端的实机回归一次。

---

## 附：审查覆盖范围与方法

- **子系统分工**：① 状态/存储/入口 ② flow 可视化 ③ record 路由 ④ table/search/add 路由 ⑤ api/setting/res/headerbar/Tauri，各由独立子代理逐文件深读。
- **主审独立复核**：亲自通读 `api.ts`、`resso.ts`、`store.ts`、`storage.ts`、`Chat.tsx`、`AiSetting.tsx`、`tauri.conf.json`、`capabilities/migrated.json`、`lib.rs`/`main.rs`、`resUtils.ts`、`schemaUtil.tsx`、`PathNotFound.tsx`、`promptStorage.ts`、`EntityCard.tsx`、`EntityForm.tsx`，并核对 antd v6 `AlertProps` 类型定义。
- **antd v6 权威校验**：已安装 `@ant-design/cli`（v6.5.0，与项目 antd 版本一致），运行 `antd lint ./src`（检出 3 个 performance error、0 deprecated）与 `antd info Alert --version 6.5.0`（确认 `title` prop 有效）。antd 相关结论均以 CLI 为准，不依赖记忆。
- **结论置信度**：🔴 Critical 与 🟠 必须修改项均已人工核实源码行号；🟡/⚪ 项基于子代理报告 + 交叉验证，置信度高。
- **未覆盖**：未运行完整测试套件/构建验证（仓库内未见前端单测）；建议修复后补充针对 S3/S4/C5/C6/C7/C8 的回归测试。
- **后续约定**：任何涉及 antd 的代码改动后，请跑 `antd lint ./src`（以及 `antd info <组件> --version 6.5.0` 查 API），避免引入 deprecated 用法或深路径导入。
