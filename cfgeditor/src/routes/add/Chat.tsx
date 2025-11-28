import type {BubbleListProps} from "@ant-design/x";
import {Bubble, Sender, Welcome} from "@ant-design/x";
import XMarkdown from "@ant-design/x-markdown";
import {OpenAIChatProvider, useXChat, XModelParams, XModelResponse, XRequest} from "@ant-design/x-sdk";
import {Flex} from "antd";
import {createStyles} from "antd-style";
import {useState, useEffect} from "react";

import {useMyStore, useLocationData} from "../../store/store.ts";
import {memo, useRef} from "react";
import {Schema} from "../table/schemaUtil.tsx";
import {useQuery, useMutation} from "@tanstack/react-query";
import {getPrompt, checkJson} from "../api.ts";
import {Result, Spin} from "antd";
import {CheckJsonResult} from "./chatModel.ts";
import {applyNewEditingObject} from "../record/editingObject.ts";

const useChatStyle = createStyles(({token, css}) => {
    return {
        chatContainer: css`
            display: flex;
            flex-direction: column;
            background: ${token.colorBgContainer};
            color: ${token.colorText};
            height: calc(100vh - 80px);
        `,
        chatHeader: css`
            height: 52px;
            box-sizing: border-box;
            border-bottom: 1px solid ${token.colorBorder};
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 10px 0 16px;
        `,
        headerTitle: css`
            font-weight: 600;
            font-size: 15px;
        `,
        headerButton: css`
            font-size: 18px;
        `,
        chatList: css`
            margin-block-start: ${token.margin}px;
            display: flex;
            height: calc(100% - 114px);
            flex-direction: column;
        `,
        chatWelcome: css`
            margin-inline: ${token.margin}px;
            padding: 12px 16px;
            border-radius: 2px 12px 12px 12px;
            background: ${token.colorBgTextHover};
            margin-bottom: ${token.margin}px;
        `,
        chatSend: css`
            padding: ${token.padding}px;
        `,
    };
});

const role: BubbleListProps["role"] = {
    assistant: {
        placement: "start",
        contentRender(content: string) {
            const newContent = content.replace("/\n\n/g", "<br/><br/>");
            return <XMarkdown content={newContent}/>;
        },
    },
    user: {placement: "end"},
};

export const Chat = memo(function Chat({schema}: { schema: Schema | undefined; }) {
    const {styles} = useChatStyle();
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
            chatRef.current?.setInputAreaValue(error.message);
        },
        onSuccess: (result: CheckJsonResult) => {
            if (result.resultCode == 'ok') {
                if (curTableId == result.table) {
                    applyNewEditingObject(JSON.parse(result.jsonResult));
                } else {
                    chatRef.current?.setInputAreaValue(`table changed! ${result.table} != ${curTableId}`);
                }
            } else {
                chatRef.current?.setInputAreaValue(result.jsonResult);
            }
        },
    });


    const isAiSet = aiConf.baseUrl.length > 0;
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

                                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                                    const data = JSON.parse((chunk as any).data);
                                    const choices = data.choices;
                                    if (choices && Array.isArray(choices) && choices.length > 0) {
                                        const choice = choices[0];

                                        // 累积内容
                                        if (choice.delta?.content) {
                                            fullContent += choice.delta.content;
                                        }

                                        // 检查是否完成
                                        if (choice.finish_reason) {
                                            // 调用 checkJsonMutation 验证生成的 JSON
                                            const finalContent = fullContent.trim();
                                            if (finalContent) {
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
            setMessages([
                {
                    id: "1111",
                    message: {
                        role: "user",
                        content: promptRes.prompt,
                    },
                    status: 'success' as const,
                },
                {
                    id: "2222",
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
        onRequest({
            messages: [{role: "user", content: val}],
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
        <div className={styles.chatHeader}>
            <div className={styles.headerTitle}>AI Chat</div>
        </div>
    );

    const chatList = (
        <div className={styles.chatList}>
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
                        className={styles.chatWelcome}
                    />
                </>
            )}
        </div>
    );

    const chatSender = (
        <Flex vertical gap={12} className={styles.chatSend}>
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
        <div className={styles.chatContainer}>
            {chatHeader}
            {chatList}
            {chatSender}
        </div>
    </>);
});
