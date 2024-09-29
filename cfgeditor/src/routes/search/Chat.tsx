import {ProChat, ProChatInstance} from "@ant-design/pro-chat";

import {useTheme} from "antd-style";
import {OpenAI} from "openai";
import {invalidateAllQueries, navTo, store, useLocationData} from "../setting/store.ts";
import {ChatMessage} from "@ant-design/pro-chat";
import {AIConf} from "../setting/storageJson.ts";
import {memo, useCallback, useRef, useState} from "react";
import {Schema} from "../table/schemaUtil.ts";
import {useMutation, useQuery} from "@tanstack/react-query";
import {addOrUpdateRecord, generatePrompt} from "../api.ts";
import {App, Result, Spin} from "antd";
import {PromptRequest} from "./chatModel.ts";
import {JSONObject, RecordEditResult} from "../record/recordModel.ts";
import {useNavigate} from "react-router-dom";


export const Chat = memo(function Chat({schema}: {
    schema: Schema | undefined;
}) {
    const theme = useTheme();
    const {server, aiConf} = store;
    const {curTableId} = useLocationData();
    // const {t} = useTranslation();
    const [chats, setChats] = useState<ChatMessage[]>([]);
    const chatRef = useRef<ProChatInstance|undefined>();

    let editable = false;
    if (schema && schema.isEditable) {
        const sTable = schema.getSTable(curTableId);
        if (sTable && sTable.isEditable) {
            editable = true;
        }
    }

    const req: PromptRequest = {
        role: aiConf.role,
        table: curTableId,
        examples: [],
    }
    for (let e of aiConf.examples) {
        if (e.table == curTableId) {
            req.examples.push({
                id: e.id,
                description: e.description
            });
        }
    }

    const {isLoading, isError, error, data: promptResult} = useQuery({
        queryKey: ['prompt', curTableId],
        queryFn: () => generatePrompt(server, req),
        staleTime: Infinity,
        enabled: editable
    })

    const {notification} = App.useApp();
    const navigate = useNavigate();
    const addOrUpdateRecordMutation = useMutation<RecordEditResult, Error, JSONObject>({
        mutationFn: (jsonObject: JSONObject) => addOrUpdateRecord(server, curTableId, jsonObject),

        onError: (error) => {
            notification.error({
                message: `addOrUpdateRecord  ${curTableId}  err: ${error.toString()}`,
                placement: 'topRight', duration: 4
            });
        },
        onSuccess: (editResult) => {
            if (editResult.resultCode == 'updateOk' || editResult.resultCode == 'addOk') {
                notification.info({
                    message: `addOrUpdateRecord  ${curTableId} ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 3
                });

                invalidateAllQueries();
                navigate(navTo('record', editResult.table, editResult.id, true));
            } else {
                notification.warning({
                    message: `addOrUpdateRecord ${curTableId} ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 4
                });
            }
        },
    });


    // const onChatEnd = (id: string, type: 'done' | 'error' | 'abort') => {
    //     const data = chatRef?.current?.getChatById(id);
    //     console.log("end", id, type, data)
    // };

    const onChatGenerate = useCallback((chunk : string) => {
        const json = parseMarkdownToJson(chunk);
        // console.log(chunk, json);
        if (json){
            addOrUpdateRecordMutation.mutate(json);
        }
    }, [addOrUpdateRecordMutation]);


    if (!editable) {
        return <Result title={'not editable'}/>;
    }

    if (isLoading) {
        return <Spin/>;
    }

    if (isError) {
        return <Result status={'error'} title={error.message}/>;
    }

    if (!promptResult) {
        return <Result title={'promptResult result empty'}/>;
    }

    if (promptResult.resultCode != 'ok') {
        return <Result status={'error'} title={promptResult.resultCode}/>;
    }

    const pre: ChatMessage[] = [{
        id: '1111',
        createAt: 1234,
        updateAt: 1235,
        role: 'user',
        content: promptResult.prompt,
    }, {
        id: '2222',
        createAt: 2234,
        updateAt: 2235,
        role: 'system',
        content: promptResult.answer,
    }];

    const showChats = chats.length > 0 ? chats : pre;

    return <>
        <div style={{height: "5vh"}}/>
        <div style={{backgroundColor: theme.colorBgLayout}}>

            <ProChat chatRef={chatRef}
                     style={{height: "95vh"}}
                     chats={showChats}
                     onChatsChange={setChats}
                     // onChatEnd={onChatEnd}
                     onChatGenerate={onChatGenerate}
                     request={async (messages: ChatMessage[]) => {
                         return ask(preAndLastMessage(pre, messages), aiConf)
                     }}
            />
        </div>
    </>;
});


function parseMarkdownToJson(chunk:string) {
    const c = chunk.trim();
    if (c.startsWith("```json") && c.endsWith("```")){
        const jsonStr = c.substring(7, c.length-3);
        return JSON.parse(jsonStr);
    }
}

function preAndLastMessage(pre: ChatMessage[], messages: ChatMessage[]): ChatMessage[] {
    const r = [...pre]
    if (messages.length > 0) {
        r.push(messages[messages.length - 1]);
    }
    return r;
}

// 因为使用stream方式，onChatEnd，没办法拿到最后的结果，通过chatRef也不行，会差一些字符，
// 所以回归直接方式
export async function ask(messages: ChatMessage[], aiConf: AIConf) {
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
    // console.log("before send", msgs)
    const completion = await openai.chat.completions.create({
        messages: msgs,
        model: aiConf.model,
    });

    return new Response(completion.choices[0].message.content);
}

export async function askStream(messages: ChatMessage[], aiConf: AIConf) {
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
