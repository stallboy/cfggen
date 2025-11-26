import {
    CloseOutlined,
    CopyOutlined,
    LikeOutlined,
    DislikeOutlined,
    ReloadOutlined,
} from "@ant-design/icons";
import type {BubbleListProps} from "@ant-design/x";
import {Bubble, Sender, Welcome} from "@ant-design/x";
import XMarkdown from "@ant-design/x-markdown";
import {DeepSeekChatProvider, SSEFields, useXChat, XModelParams, XModelResponse, XRequest} from "@ant-design/x-sdk";
import {Button, Flex, Space} from "antd";
import {createStyles} from "antd-style";
import {useState} from "react";

import {useMyStore, useLocationData} from "../../store/store.ts";
import {memo} from "react";
import {Schema} from "../table/schemaUtil.tsx";
import {useQuery} from "@tanstack/react-query";
import {getPrompt} from "../api.ts";
import {Result, Spin} from "antd";

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
            height: calc(100% - 194px);
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
        footer: (
            <div style={{display: "flex"}}>
                <Button type="text" size="small" icon={<ReloadOutlined/>}/>
                <Button type="text" size="small" icon={<CopyOutlined/>}/>
                <Button type="text" size="small" icon={<LikeOutlined/>}/>
                <Button type="text" size="small" icon={<DislikeOutlined/>}/>
            </div>
        ),
        contentRender(content: string) {
            const newContent = content.replace("/\n\n/g", "<br/><br/>");
            return <XMarkdown content={newContent}/>;
        },
    },
    user: {placement: "end"},
};

export const Chat = memo(function Chat({
                                           schema,
                                       }: {
    schema: Schema | undefined;
}) {
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

    const {
        isLoading,
        isError,
        error,
        data: promptRes,
    } = useQuery({
        queryKey: ["prompt", curTableId],
        queryFn: async ({signal}) => {
            const result = await getPrompt(server, curTableId, signal);
            return {
                result: result,
                prompt: {
                    id: "1111",
                    createAt: 1234,
                    updateAt: 1235,
                    role: "user",
                    content: result.prompt,
                },
                init: {
                    id: "2222",
                    createAt: 2234,
                    updateAt: 2235,
                    role: "assistant",
                    content: result.init,
                },
            };
        },
        staleTime: Infinity,
        enabled: editable,
    });


    const isAiSet = aiConf.baseUrl.length > 0;
    const baseUrl = isAiSet ? aiConf.baseUrl : 'https://api.x.ant.design/api/big_model_glm-4.5-flash';
    const model = isAiSet ? aiConf.model : 'glm-4.5-flash';
    const apiKey = isAiSet ? aiConf.apiKey : 'xxx';
    const {onRequest, messages, isRequesting, abort} = useXChat({
        provider: new DeepSeekChatProvider({
            request: XRequest<XModelParams, Partial<Record<SSEFields, XModelResponse>>>(
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

    const handleUserSubmit = (val: string) => {
        onRequest({
            messages: [{role: "user", content: val}],
        });
    };

    const handleClearChat = () => {
        abort();
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

    if (promptRes.result.resultCode != "ok") {
        return <Result status={"error"} title={promptRes.result.resultCode}/>;
    }

    const chatHeader = (
        <div className={styles.chatHeader}>
            <div className={styles.headerTitle}>✨ AI Chat</div>
            <Space size={0}>
                <Button
                    type="text"
                    icon={<CloseOutlined/>}
                    onClick={handleClearChat}
                    className={styles.headerButton}
                />
            </Space>
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

    return (
        <div className={styles.chatContainer}>
            {chatHeader}
            {chatList}
            {chatSender}
        </div>
    );
});
