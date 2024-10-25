import {ProChat, ProChatInstance} from "@ant-design/pro-chat";

import {useTheme} from "antd-style";
import {OpenAI} from "openai";
import {store, useLocationData} from "../setting/store.ts";
import {ChatMessage} from "@ant-design/pro-chat";
import {AIConf} from "../setting/storageJson.ts";
import {memo, useRef, useState} from "react";
import {Schema} from "../table/schemaUtil.ts";
import {useQuery} from "@tanstack/react-query";
import {getPrompt} from "../api.ts";
import {Result, Spin} from "antd";

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

    const {isLoading, isError, error, data: promptResult} = useQuery({
        queryKey: ['prompt', curTableId],
        queryFn: ({signal}) => getPrompt(server, curTableId, signal),
        staleTime: Infinity,
        enabled: editable
    })


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
        role: 'assistant',
        content: promptResult.init,
    }];

    const showChats = chats.length == 0 ? [pre[1]] : chats;

    return <div style={{backgroundColor: theme.colorBgLayout}}>
        <ProChat chatRef={chatRef}
                 style={{height: "85vh"}}
                 chats={showChats}
                 onChatsChange={setChats}
                 request={async (messages: ChatMessage[]) => {
                     return askStream([pre[0], ...messages], aiConf)
                 }}
        />
    </div>
});

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
