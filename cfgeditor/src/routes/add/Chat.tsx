import type {BubbleListProps} from "@ant-design/x";
import {Bubble, Sender, Welcome} from "@ant-design/x";
import XMarkdown from "@ant-design/x-markdown";
import {OpenAIChatProvider, useXChat, XModelParams, XModelResponse, XRequest} from "@ant-design/x-sdk";
import {Flex, Result, Spin, theme} from "antd";
import {useState, useEffect} from "react";

import {useMyStore, useLocationData} from "@/store/store";
import {memo, useRef, type CSSProperties} from "react";
import {Schema} from "@/domain/schema";
import {useQuery, useMutation} from "@tanstack/react-query";
import {getPrompt, checkJson} from "@/api/api";
import {CheckJsonResult} from "@/api/chatModel";
import {getCurrentEditingSession} from "@/services/editingSession";

const role: BubbleListProps["role"] = {
    assistant: {
        placement: "start",
        contentRender(content: string) {
            return <XMarkdown content={content}/>;
        },
    },
    user: {placement: "end"},
};

export const Chat = memo(function Chat({schema}: { schema: Schema | undefined; }) {
    const {token} = theme.useToken();
    const styles = {
        chatContainer: {
            display: 'flex',
            flexDirection: 'column',
            background: token.colorBgContainer,
            color: token.colorText,
            height: 'calc(100vh - 80px)',
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

    let editable = false;
    if (schema && schema.isEditable) {
        const sTable = schema.getSTable(curTableId);
        if (sTable) {
            editable = true;
        }
    }

    const [inputValue, setInputValue] = useState("");
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const chatRef = useRef<any>(null);

    const {isLoading, isError, error, data: promptRes} = useQuery({
        queryKey: ["prompt", curTableId],
        queryFn: ({signal}) => getPrompt(server, curTableId, signal),
        staleTime: Infinity,
        enabled: editable,
    });

    const checkJsonMutation = useMutation<CheckJsonResult, Error, string>({
        mutationFn: (raw: string) => checkJson(server, curTableId, raw),

        onError: (error) => {
            setInputValue(error.message);
        },
        onSuccess: (result: CheckJsonResult) => {
            if (result.resultCode == 'ok') {
                if (curTableId == result.table) {
                    try {
                        getCurrentEditingSession()?.replaceEditingObject(JSON.parse(result.jsonResult));
                    } catch (e) {
                        setInputValue(`parse jsonResult failed: ${e}`);
                    }
                } else {
                    setInputValue(`table changed! ${result.table} != ${curTableId}`);
                }
            } else {
                setInputValue(result.jsonResult);
            }
        },
    });


    // 同时校验 baseUrl 与 apiKey：默认 baseUrl 非空但 apiKey 默认为 ''，
    // 仅校验 baseUrl 会以空 key 发 Bearer 导致 401 且无引导
    const isAiSet = aiConf.baseUrl.length > 0 && aiConf.apiKey.length > 0;
    const baseUrl = isAiSet ? aiConf.baseUrl : 'https://api.x.ant.design/api/big_model_glm-4.5-flash';
    const model = isAiSet ? aiConf.model : 'glm-4.5-flash';
    const apiKey = isAiSet ? aiConf.apiKey : 'xxx';
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
                            // 处理流式响应数据数组
                            if (Array.isArray(chunks)) {
                                let fullContent = '';

                                // 累积流式内容，遇到 finish_reason 就停止
                                for (const chunk of chunks) {
                                    // 跳过 [DONE] 哨兵/keepalive 等非 JSON 帧，并防御非法 JSON，避免整条流处理中断
                                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                                    const raw = (chunk as any).data;
                                    if (raw === '[DONE]' || raw == null) {
                                        continue;
                                    }
                                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                                    let data: any;
                                    try {
                                        data = JSON.parse(raw);
                                    } catch {
                                        continue;
                                    }
                                    const choices = data.choices;
                                    if (choices && Array.isArray(choices) && choices.length > 0) {
                                        const choice = choices[0];

                                        // 累积内容
                                        if (choice.delta?.content) {
                                            fullContent += choice.delta.content;
                                        }

                                        // 检查是否完成
                                        if (choice.finish_reason) {
                                            const finalContent = fullContent.trim();
                                            if (finalContent) {
                                                // 验证生成的 JSON
                                                checkJsonMutation.mutate(finalContent);
                                            }
                                            break;
                                        }
                                    }
                                }
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
        requestFallback: (_, {error}) => {
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
        onRequest({
            messages: [{ role: "user", content: val }],
        });
    };

    if (!editable) {
        return <Result title={"not editable"}/>;
    }

    if (isLoading) {
        return <Spin/>;
    }

    if (isError) {
        return <Result status={"error"} title={error.message}/>;
    }

    if (!promptRes) {
        return <Result title={"promptResult result empty"}/>;
    }

    if (promptRes.resultCode != "ok") {
        return <Result status={"error"} title={promptRes.resultCode}/>;
    }

    const chatHeader = (
        <div style={styles.chatHeader}>
            <div style={styles.headerTitle}>AI Chat</div>
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
                ref={chatRef}
                loading={isRequesting}
                value={inputValue}
                onChange={(v) => setInputValue(v)}
                onSubmit={() => {
                    handleUserSubmit(inputValue);
                    setInputValue("");
                }}
                onCancel={() => {
                    abort();
                }}
                placeholder="Ask me to generate configuration data..."
            />
        </Flex>
    );

    return (<>
        <div style={{height: 32}}/>
        <div style={styles.chatContainer}>
            {chatHeader}
            {chatList}
            {chatSender}
        </div>
    </>);
});
