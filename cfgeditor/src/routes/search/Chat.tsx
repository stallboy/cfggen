import {ProChat, ProChatInstance} from "@ant-design/pro-chat";

import {useTheme} from "antd-style";
import {OpenAI} from "openai";
import {store, useLocationData} from "../setting/store.ts";
import {ChatMessage} from "@ant-design/pro-chat";
import {AIConf} from "../setting/storageJson.ts";
import {memo, useCallback, useRef, useState} from "react";
import {Schema} from "../table/schemaUtil.ts";
import {useMutation, useQuery} from "@tanstack/react-query";
import {checkJson, getPrompt} from "../api.ts";
import {Button, Result, Spin} from "antd";
import {CheckJsonResult} from "./chatModel.ts";
import {applyNewEditingObject} from "../record/editingObject.ts";


export const Chat = memo(function Chat({schema}: {
        schema: Schema | undefined;
    }) {
        const theme = useTheme();
        const {server, aiConf} = store;
        const {curTableId} = useLocationData();
        let editable = false;
        if (schema && schema.isEditable) {
            const sTable = schema.getSTable(curTableId);
            if (sTable && sTable.isEditable) {
                editable = true;
            }
        }

        // const {t} = useTranslation();
        const [chats, setChats] = useState<ChatMessage[]>([]);
        const chatRef = useRef<ProChatInstance | undefined>();

        const {isLoading, isError, error, data: promptRes} = useQuery({
            queryKey: ['prompt', curTableId],
            queryFn: async ({signal}) => {
                const result = await getPrompt(server, curTableId, signal)
                return {
                    result: result,
                    prompt: {
                        id: '1111',
                        createAt: 1234,
                        updateAt: 1235,
                        role: 'user',
                        content: result.prompt,
                    },
                    init: {
                        id: '2222',
                        createAt: 2234,
                        updateAt: 2235,
                        role: 'assistant',
                        content: result.init,
                    }
                };
            },
            staleTime: Infinity,
            enabled:
            editable
        })


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

        const [lastReply, setLastReply] = useState<string>('');
        const onChatsChange = useCallback((chatList: ChatMessage[]) => {
            setChats(chatList);
            if (chatList.length > 0) {
                const last = chatList[chatList.length - 1];
                if (last.isFinished) {
                    const c = last.content as string;
                    if (c != lastReply) {
                        setLastReply(c);
                        console.log(c);
                        checkJsonMutation.mutate(c);
                    }
                }
            }
        }, [setChats, lastReply, setLastReply, chatRef, checkJsonMutation]);


        const actionsRender = useCallback(() => {
            return <div style={{height: 40}}>
                <Button type={'text'} onClick={() => {
                    chatRef.current?.stopGenerateMessage();
                    setChats([]);
                }}> {'clear'}</Button>
            </div>
        }, [chatRef, setChats, promptRes]);


        const sendMessageRequest = useCallback(async (message:ChatMessage[]) => {
            let old = chats
            if (chats.length == 0) {
                old = promptRes?.init ? [promptRes?.init, ...message] : message
            }else{
                old = chats.slice(0, chats.length - 1)
            }
            const messages = promptRes?.prompt ? [promptRes.prompt, ...old] : old
            console.log(chats, message, messages);

            return askStream(messages, aiConf)
        }, [promptRes, chats, aiConf]);

        if (!editable) {
            return <Result title={'not editable'}/>;
        }

        if (isLoading) {
            return <Spin/>;
        }

        if (isError) {
            return <Result status={'error'} title={error.message}/>;
        }

        if (!promptRes) {
            return <Result title={'promptResult result empty'}/>;
        }

        if (promptRes.result.resultCode != 'ok') {
            return <Result status={'error'} title={promptRes.result.resultCode}/>;
        }

        const showChats = chats.length == 0 ? [promptRes.init] : chats

        return <ProChat chatRef={chatRef}
                        style={{height: 'calc(100vh - 80px)', backgroundColor: theme.colorBgLayout}}
                        chatList={showChats}
                        onChatsChange={onChatsChange}
                        actionsRender={actionsRender}
                        sendMessageRequest={sendMessageRequest}/>

    })
;

async function askStream(messages: ChatMessage[], aiConf: AIConf) {
    const openai = new OpenAI({
        baseURL: aiConf.baseUrl,
        apiKey: aiConf.apiKey,
        dangerouslyAllowBrowser: true
    });

    const msgs: Array<OpenAI.ChatCompletionMessageParam> = []
    for (let m of messages) {
        msgs.push({
            role: m.role as any,
            content: m.content as string,
        })
    }
    const stream = await openai.chat.completions.create({
        messages: msgs,
        model: aiConf.model,
        // response_format: {type: "json_object"},
        stream: true,
    });

    const encoder = new TextEncoder();
    const readableStream = new ReadableStream({
        async start(controller) {
            for await (const chunk of stream) {
                // console.log(chunk);
                const text = chunk.choices[0]?.delta?.content || '';
                // console.log(text);
                controller.enqueue(encoder.encode(text));
            }
            controller.close();
        }
    });
    return new Response(readableStream);
}
