# 08 AI Chat

AI Chat 用 LLM 按当前表 schema 生成配置 JSON，写回编辑会话。**前端直连 OpenAI 兼容端点，后端只产系统提示词 + 校验**。

> **不讲**：x-sdk 内部协议解析（黑盒）；后端 `/prompt` / `/checkJson` 的服务端实现（cfggen 后端，标注契约即可）。本文讲 Chat 架构 + 与 EditingSession / 后端的关系。
>
> 【承前】03 的 `replaceEditingObject`（回写）+ 01 后端契约 + 02 `aiConf`。　【启后】—（扩展篇，最后看 [09](09-cross-cutting.md)）。

---

## 一、整体架构：x-sdk

[`Chat.tsx`](../src/features/add/Chat.tsx):28 用 ant-design 的 x 系列搭对话：

- `@ant-design/x`（`Bubble` / `Sender` / `Welcome`）搭对话 UI。
- `@ant-design/x-markdown`（`XMarkdown`）渲染 assistant（:21-23）。
- `@ant-design/x-sdk`（`OpenAIChatProvider` / `useXChat` / `XRequest`）管 messages / abort / placeholder / fallback。

为什么用 x-sdk 而不直接用 openai SDK：x-sdk 把 OpenAI 兼容协议包成 React 友好的 provider / `useXChat`，统一管 messages / abort / placeholder / fallback。项目实际是「OpenAI 兼容端点」（deepseek / glm 等），不是 OpenAI 官方 SDK。

```ts
const {onRequest, messages, isRequesting, abort, setMessages} = useXChat({
    defaultMessages: [],
    provider: new OpenAIChatProvider({ request: XRequest(baseUrl, {headers, manual, params, callbacks}) }),
    requestPlaceholder: () => ({content: "Thinking...", role: "assistant"}),
    requestFallback: (_, {error}) => error.name === "AbortError" ? {content: "Request was cancelled"} : {content: `Error: ${error.message}`},
});
```

---

## 二、前端直连，不走后端代理

`XRequest(baseUrl, {Authorization: "Bearer "+apiKey, stream:true, model})`（:122-132）——baseUrl / apiKey / model 全在浏览器端（来自 `useMyStore().aiConf`）。store 默认填了 deepseek 的 baseUrl / model，但 **apiKey 默认空 → `isAiSet=false` → 实际生效的是下面的 ant.design 演示端点**。**LLM 请求由前端直发**，后端 `/prompt` + `/checkJson` 是另一条独立链路（§4），不转发 LLM。

未配置 aiConf 时降级到 ant.design 演示端点（:113-118）：

```ts
// 同时校验 baseUrl 与 apiKey：默认 baseUrl 非空但 apiKey 默认为 ''
// 仅校验 baseUrl 会以空 key 发 Bearer 导致 401 且无引导
const isAiSet = aiConf.baseUrl.length > 0 && aiConf.apiKey.length > 0;
const baseUrl = isAiSet ? aiConf.baseUrl : 'https://api.x.ant.design/api/big_model_glm-4.5-flash';
const apiKey  = isAiSet ? aiConf.apiKey  : 'xxx';
const model   = isAiSet ? aiConf.model   : 'glm-4.5-flash';
```

apikey 存个人 pref 文件 `cfgeditorSelf.yml`（02 讲过，`prefSelfKeySet` 含 `aiConf`，与团队共享隔离）。

---

## 三、流式累积，但增量 UI 未实现

`stream: true` 开启（:130），但**流式增量 UI 暂未实现**（`onUpdate` 空实现，:179-181）。业务只关心最终 JSON 是否合法——在 `onSuccess` 里累积 `delta.content`（:134-175）：

```ts
onSuccess: (chunks) => {
    if (Array.isArray(chunks)) {
        let fullContent = '';
        for (const chunk of chunks) {
            const raw = chunk.data;
            if (raw === '[DONE]' || raw == null) continue;       // 跳过 [DONE] 哨兵 / keepalive
            let data; try { data = JSON.parse(raw); } catch { continue; }   // 防御非法 JSON 帧
            const choice = data.choices?.[0];
            if (choice?.delta?.content) fullContent += choice.delta.content;  // 累积内容
            if (choice?.finish_reason) {                          // 完成时校验
                const finalContent = fullContent.trim();
                if (finalContent) checkJsonMutation.mutate(finalContent);
                break;
            }
        }
    }
},
onUpdate: () => { /* 流式增量更新暂未实现（完整内容由 onSuccess 统一处理）*/ }
```

跳过 `[DONE]` 哨兵与非法 JSON 帧（`try/catch continue`）防整条流处理中断。UI 上 `Bubble` 是 `useXChat` 自己管的占位 / 最终消息。

---

## 四、后端双链路：/prompt + /checkJson

两条独立的后端契约（[apiClient.ts](../src/api/apiClient.ts)，01 §3.2）：

### 4.1 `/prompt`（GET）—— 初始对话种子

```ts
const {data: promptRes} = useQuery({
    queryKey: queryKeys.prompt(curTableId),
    queryFn: ({signal}) => getPrompt(server, curTableId, signal),
    staleTime: Infinity, enabled: editable,
});
```

产 `{prompt, init}` 双消息——系统提示词 + 开场白由后端按当前表 schema 生成（前端不内置），保证 LLM 输出贴合表结构。`staleTime: Infinity`（后端静态生成）+ `enabled: editable`（只可编辑表启用）。

`promptRes` 可用且 messages 为空时，注入两条种子消息（:208-231）：user 的 `prompt` + assistant 的 `init`。

### 4.2 `/checkJson`（POST text/plain）—— 校验 + 回填

LLM 输出的 JSON 不直接写入，先经后端按当前 `tableId` 的 schema 校验 + 可能修复（:89-110）：

```ts
const checkJsonMutation = useMutation({
    mutationFn: (raw) => checkJson(server, curTableId, raw),
    onSuccess: (result) => {
        if (result.resultCode == 'ok') {
            if (curTableId == result.table) {                    // 防中途切表
                getCurrentEditingSession()?.replaceEditingObject(JSON.parse(result.jsonResult));
            } else { setInputValue(`table changed! ${result.table} != ${curTableId}`); }
        } else { setInputValue(result.jsonResult); }             // 业务失败：错误信息塞回输入框
    },
    onError: (error) => setInputValue(error.message),
});
```

只有 `resultCode==='ok'` 且 `curTableId===result.table` 才回写，否则错误信息塞回输入框（让用户继续对话修正）。

**只生成配置 JSON，不生成 schema**（全仓 grep `schema-gen` 零命中）。schema 仅作可编辑性门禁（`useIsCurTableEditable` 读 `schema?.isEditable && schema.getSTable(curTableId)`）和后端 `/prompt` / `/checkJson` 的 schema 来源。

---

## 五、回写：replaceEditingObject

Chat 生成的是整条记录的 JSON，回写用 `replaceEditingObject`（:99，03 §2.3）——**整体替换 + FitFull + capture undo 快照**：

```ts
getCurrentEditingSession()?.replaceEditingObject(JSON.parse(result.jsonResult));
```

→ 回到 03 的 EditingSession：bump `structureVersion` → Record 重渲 → entityMap 重算 → 布局重算（04）。`replaceEditingObject` 内部会 `deleteRefsInPlace` 剥离后端附加的 `$refs`（避免污染提交载荷 + 误判 dirty）。

AddJson 的「loadIntoForm」按钮也调 `replaceEditingObject`，但**跳过 `/checkJson` 校验**（直接 `JSON.parse`）——见 [AddJson.tsx](../src/features/add/AddJson.tsx):52。

---

## 六、AddPanel 双面板常驻

[AddPanel.tsx](../src/features/add/AddPanel.tsx) 用 Segmented 切 `'ai'` / `'json'` 两态，两侧常驻用 `display:none` 切换——避免切换卸载 Chat 丢失 `useXChat` 的对话历史。

`useIsCurTableEditable`（[useEditable.ts](../src/features/add/useEditable.ts)）：`schema?.isEditable && schema.getSTable(curTableId)` 存在才可编辑——AddPanel 用它做面板门禁（Chat / AddJson 都被它拦）；Chat 另调一次控制 prompt query 的 `enabled`。

---

## 七、Cheat Sheet

**接自己的 LLM**：设置面板写 `aiConf`（baseUrl + apiKey + model）→ 存个人 pref 文件。OpenAI 兼容端点即可（deepseek / glm / 自部署）。

**改系统提示词**：在后端 `/prompt`（cfggen `-gen server`）改，不在前端——前端只消费 `{prompt, init}`。

**回写不走值类 patch**：Chat 生成整条 JSON，用 `replaceEditingObject`（整体替换 + FitFull + undo），不走 `updateFormValues`。

**新增 AI 能力**：x-sdk 黑盒消费；业务逻辑（校验 / 回写）在 `onSuccess` + `checkJsonMutation`。

---

## 一句话速记

- **x-sdk 直连 OpenAI 兼容端点**：`XRequest(baseUrl, Bearer apiKey, stream)`；未配置降级到 ant.design 演示端点；apikey 存个人 pref。
- **流式累积但增量 UI 未实现**：`onSuccess` 累积 `delta.content`，跳过 `[DONE]` / 非法 JSON 帧；`finish_reason` 时 `checkJsonMutation`。
- **后端双链路独立**：`/prompt`（GET，按表 schema 产 prompt+init 种子，`staleTime:∞`）/ `/checkJson`（POST，校验+修复生成 JSON）；后端不转发 LLM。
- **回写 `replaceEditingObject`**：整体替换 + FitFull + undo，内部 `deleteRefsInPlace` 剥 `$refs`。
- **只生成配置 JSON 不生成 schema**；schema 仅作可编辑门禁 + 后端校验来源。
- **AddPanel 双面板常驻**（`display:none`）：避免切换卸载丢对话历史。
