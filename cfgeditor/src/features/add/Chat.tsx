import type {BubbleListProps} from "@ant-design/x";
import {Bubble, Sender, Welcome} from "@ant-design/x";
import XMarkdown from "@ant-design/x-markdown";
import {OpenAIChatProvider, useXChat, XModelParams, XModelResponse, XRequest} from "@ant-design/x-sdk";
import {App, Flex, theme} from "antd";
import {memo, useState, useEffect, useRef, type CSSProperties} from "react";

import {useMyStore, useLocationData} from "@/store/store.ts";
import {getPrefStr} from "@/store/storage";
import {useIsCurTableEditable} from "./useEditable.ts";
import {Schema} from "@/domain/schema.ts";
import {useQuery, useMutation} from "@tanstack/react-query";
import {getPrompt, checkJson} from "@/api/apiClient.ts";
import {CheckJsonResult} from "@/api/chatModel.ts";
import {EditingSession, getCurrentEditingSession} from "@/services/editingSession.ts";
import {queryKeys} from "@/services/queryKeys.ts";
import {accumulateSseContent} from "./chatSse.ts";
import {QueryGate} from "@/app/QueryGate.tsx";

const role: BubbleListProps["role"] = {
    assistant: {
        placement: "start",
        contentRender(content: string) {
            return <XMarkdown content={content}/>;
        },
    },
    user: {placement: "end"},
};

// AI 未配置（baseUrl/apiKey 任一为空）时禁用 Sender 并给出配置引导——不做第三方端点兜底：
// 静默把用户 prompt（含表结构）发往公共演示服务既是数据外泄，假 key 也必然 401 且无引导
export const Chat = memo(function Chat({schema}: { schema: Schema | undefined; }) {
    const {token} = theme.useToken();
    const styles = {
        chatContainer: {
            display: 'flex',
            flexDirection: 'column',
            background: token.colorBgContainer,
            color: token.colorText,
            height: '100%',
        },
        chatHeader: {
            height: 52,
            boxSizing: 'border-box',
            borderBottom: `1px solid ${token.colorBorder}`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '0 10px 0 16px',
            flexShrink: 0,
        },
        headerTitle: {
            fontWeight: 600,
            fontSize: 15,
        },
        chatList: {
            marginBlockStart: token.margin,
            display: 'flex',
            flex: 1,
            flexDirection: 'column',
            overflowY: 'auto',
            paddingBottom: token.margin,
        },
        chatWelcome: {
            marginInline: token.margin,
            padding: '12px 16px',
            borderRadius: '2px 12px 12px 12px',
            background: token.colorBgTextHover,
            marginBottom: token.margin,
        },
        chatSend: {
            padding: token.padding,
            flexShrink: 0,
            borderTop: `1px solid ${token.colorBorder}`,
            background: token.colorBgContainer,
        },
    } satisfies Record<string, CSSProperties>;
    const {server, aiConf} = useMyStore();
    const {curTableId} = useLocationData();
    const editable = useIsCurTableEditable(schema);

    const [inputValue, setInputValue] = useState("");
    const {notification} = App.useApp();

    // 提交请求时捕获当时的 curTableId：AI 流式请求未结束时用户可能切表，
    // 校验请求须仍以旧表发起；回调里再与实时 curTableId（navTo 写入 pref）比对，不一致则放弃写入
    const submitTableIdRef = useRef('');
    // 同时捕获当时的编辑会话：流式 + checkJson 在途期间用户可能切换/关闭会话或做了手动修改，
    // 写入前必须确认会话未变且无未保存编辑，否则 AI 结果会静默覆盖用户修改或写错记录
    const submitSessionRef = useRef<EditingSession | null>(null);

    const promptQuery = useQuery({
        queryKey: queryKeys.prompt(curTableId),
        queryFn: ({signal}) => getPrompt(server, curTableId, signal),
        staleTime: Infinity,
        enabled: editable,
    });
    const promptRes = promptQuery.data;

    const checkJsonMutation = useMutation<CheckJsonResult, Error, string>({
        mutationFn: (raw: string) => checkJson(server, submitTableIdRef.current, raw),

        onError: (error) => {
            // 不动输入框：异步回调到达时用户可能正在输入下一条消息
            notification.error({title: `checkJson err: ${error.message}`, placement: 'topRight', duration: 4});
        },
        onSuccess: (result: CheckJsonResult) => {
            const nowTableId = getPrefStr('curTableId', '');
            if (submitTableIdRef.current !== nowTableId) {
                console.warn(`checkJson: 请求期间已切表 ${submitTableIdRef.current} -> ${nowTableId}，放弃写入`);
                return;
            }
            if (result.resultCode != 'ok') {
                // jsonResult 仅 ParseJsonError 时有内容（chatModel.ts），其余结果码回退显示 resultCode，避免空 toast
                notification.error({title: result.jsonResult || `checkJson failed: ${result.resultCode}`, placement: 'topRight', duration: 4});
                return;
            }
            const session = getCurrentEditingSession();
            if (!session) {
                notification.warning({title: 'No open editing session, AI result discarded', placement: 'topRight', duration: 4});
                return;
            }
            if (session !== submitSessionRef.current) {
                notification.warning({title: 'Editing session changed while generating, AI result discarded to avoid writing to the wrong record', placement: 'topRight', duration: 4});
                return;
            }
            if (session.getIsEdited()) {
                notification.warning({title: 'Record has unsaved manual edits, AI result not applied (would overwrite them)', placement: 'topRight', duration: 4});
                return;
            }
            try {
                session.replaceEditingObject(JSON.parse(result.jsonResult));
            } catch (e) {
                notification.error({title: `parse jsonResult failed: ${e}`, placement: 'topRight', duration: 4});
            }
        },
    });


    // 同时校验 baseUrl 与 apiKey：默认 baseUrl 非空但 apiKey 默认为 ''，
    // 仅校验 baseUrl 会以空 key 发 Bearer 导致 401 且无引导；未配置时禁用 Sender（见 chatSender）
    const isAiSet = aiConf.baseUrl.length > 0 && aiConf.apiKey.length > 0;
    const {baseUrl, model, apiKey} = aiConf;
    const {onRequest, messages, isRequesting, abort, setMessages} = useXChat({
        defaultMessages: [],
        provider: new OpenAIChatProvider({
            request: XRequest<XModelParams, XModelResponse>(
                baseUrl,
                {
                    headers: {
                        Authorization: "Bearer " + apiKey
                    },
                    manual: true,
                    params: {
                        stream: true,
                        model: model,
                    },
                    callbacks: {
                        onSuccess: (chunks) => {
                            // 流式帧累积见 chatSse.accumulateSseContent（纯函数、有单测）：
                            // 跳过 [DONE]/keepalive/非法 JSON，累积 delta.content，遇 finish_reason 返回 trim 内容。
                            // 仅非空返回值触发 checkJson 校验（'' 与 null 都不触发）。
                            const finalContent = Array.isArray(chunks) ? accumulateSseContent(chunks) : null;
                            if (finalContent) {
                                checkJsonMutation.mutate(finalContent);
                            }
                        },
                        onError: (err) => {
                            console.error(err);
                        },
                        onUpdate: () => {
                            // 流式增量更新暂未实现（完整内容由 onSuccess 统一处理）
                        }
                    }

                },
            ),
        }),
        requestPlaceholder: () => {
            return {
                content: "Thinking...",
                role: "assistant",
            };
        },
        requestFallback: (_, {error} :  {error: Error}) => {
            if (error.name === "AbortError") {
                return {
                    content: "Request was cancelled",
                    role: "assistant",
                };
            }
            return {
                content: `Error: ${error.message}`,
                role: "assistant",
            };
        },
    });

    // 当 promptRes 可用时，设置初始消息
    useEffect(() => {
        if (promptRes && messages.length === 0) {
            // 使用时间戳作为ID，避免重复
            const timestamp = Date.now();
            setMessages([
                {
                    id: `user-${timestamp}`,
                    message: {
                        role: "user",
                        content: promptRes.prompt,
                    },
                    status: 'success' as const,
                },
                {
                    id: `assistant-${timestamp}`,
                    message: {
                        role: "assistant",
                        content: promptRes.init,
                    },
                    status: 'success' as const,
                },
            ]);
        }
    }, [promptRes, messages.length, setMessages]);

    const handleUserSubmit = (val: string) => {
        // 直接调用 onRequest，useXChat 会自动管理消息
        // 不需要手动更新 messages 数组
        // 捕获提交时的表：流式请求期间切表后，checkJson 仍以旧表校验、回调比对放弃写入
        submitTableIdRef.current = curTableId;
        // 捕获提交时的编辑会话：checkJson 回调里校验会话未变且无未保存编辑才写入
        submitSessionRef.current = getCurrentEditingSession();
        onRequest({
            messages: [{ role: "user", content: val }],
        });
    };

    const chatHeader = (
        <div style={styles.chatHeader}>
            <div style={styles.headerTitle}>AI Chat</div>
            <div style={{color: token.colorTextSecondary, fontSize: 13}}>{model}</div>
        </div>
    );

    const chatList = (
        <div style={styles.chatList}>
            {messages.length ? (
                <Bubble.List
                    style={{paddingInline: 16}}
                    items={messages.map((i) => ({
                        ...i.message,
                        key: i.id,
                        status: i.status,
                        loading: i.status === "loading",
                    }))}
                    role={role}
                />
            ) : (
                <>
                    <Welcome
                        variant="borderless"
                        title={`👋 Welcome to AI Chat`}
                        description="I can help you generate and edit configuration data"
                        style={styles.chatWelcome}
                    />
                </>
            )}
        </div>
    );

    const chatSender = (
        <Flex vertical gap={12} style={styles.chatSend}>
            <Sender
                loading={isRequesting}
                disabled={!isAiSet}
                value={inputValue}
                onChange={(v) => setInputValue(v)}
                onSubmit={() => {
                    handleUserSubmit(inputValue);
                    setInputValue("");
                }}
                onCancel={() => {
                    abort();
                }}
                placeholder={isAiSet
                    ? "Ask me to generate configuration data..."
                    : "Please configure AI baseUrl and apiKey in Settings first"}
            />
        </Flex>
    );

    return <QueryGate query={promptQuery} emptyTitle={'promptResult result empty'}>
        {() => <div style={styles.chatContainer}>
            {chatHeader}
            {chatList}
            {chatSender}
        </div>}
    </QueryGate>;
});
