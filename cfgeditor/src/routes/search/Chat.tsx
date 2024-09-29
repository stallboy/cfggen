import {ProChat} from "@ant-design/pro-chat";

import {useTheme} from "antd-style";
import OpenAI from "openai";
import {store, useLocationData} from "../setting/store.ts";
import {ChatMessage} from "@ant-design/pro-chat";
import {AIConf} from "../setting/storageJson.ts";
import {useState} from "react";
import {Schema} from "../table/schemaUtil.ts";
import {useQuery} from "@tanstack/react-query";
import {generatePrompt} from "../api.ts";
import {Result} from "antd";
import {PromptRequest} from "./chatModel.ts";


export function Chat({schema}: {
    schema: Schema | undefined;
}) {
    const theme = useTheme();
    const {server, aiConf} = store;
    const {curTableId} = useLocationData();
    // const {t} = useTranslation();
    const [chats, setChats] = useState<ChatMessage[]>([]);

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
        examples: aiConf.examples,
    }

    const {isLoading, isError, error, data: promptResult} = useQuery({
        queryKey: ['chat', curTableId],
        queryFn: () => generatePrompt(server, req),
        enabled: editable
    })

    if (!editable) {
        return;
    }

    if (isLoading) {
        return;
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

    // const prompts =
    return <>
        <div style={{height: "5vh"}}/>
        <div style={{backgroundColor: theme.colorBgLayout}}>

            <ProChat style={{height: "95vh"}}
                     chats={chats}
                     onChatsChange={(chats) => {
                         setChats(chats);
                     }}
                     onChatGenerate={(messages) => {
                         console.log("gen", messages)
                     }}
                     request={async (messages: ChatMessage[]) => {
                         return ask(messages, aiConf)
                     }}
            />
        </div>
    </>;
}


export async function ask(messages: any[], aiConf: AIConf) {
    const openai = new OpenAI({
        baseURL: aiConf.baseUrl,
        apiKey: aiConf.apiKey,
        dangerouslyAllowBrowser: true
    });

    console.log(messages)
    const stream = await openai.chat.completions.create({
        messages: [...messages],
        model: aiConf.model,
        // response_format: {type: "json_object"},
        stream: true,
    });
    // console.log(stream);
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
