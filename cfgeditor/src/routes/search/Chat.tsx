import {ProChat} from "@ant-design/pro-chat";

import {useTheme} from "antd-style";
import OpenAI from "openai";
import {store} from "../setting/store.ts";
import {ChatMessage} from "@ant-design/pro-chat/es/types/message";
import {AIConf} from "../setting/storageJson.ts";
import {Button, Col, Form, Input, Row} from "antd";
import {useTranslation} from "react-i18next";
import {formLayout} from "../setting/BasicSetting.tsx";

function onAICreate(values: any) {
    console.log(values)
}

export function Chat() {
    const theme = useTheme();
    const {aiConf} = store;
    const {t} = useTranslation();

    return (
        <div style={{backgroundColor: theme.colorBgLayout}}>
            <Form name="aiCreate"  {...formLayout} onFinish={onAICreate} autoComplete="off">
                <Row  >
                    <Col span={20}>
                        <Form.Item name='prompt'>
                            <Input.TextArea placeholder="id,description"/>
                        </Form.Item>
                    </Col>

                    <Col span={4}>
                        <Button type="primary" htmlType="submit">
                            {t('aiCreate')}
                        </Button>
                    </Col>
                </Row>

            </Form>

            <ProChat style={{height: "75vh"}}
                     request={async (messages: ChatMessage[]) => {
                         return ask(messages, aiConf)
                     }}
            />
        </div>
    );
}


export async function ask(messages: any[], aiConf: AIConf) {
    const openai = new OpenAI({
        baseURL: aiConf.baseUrl,
        apiKey: aiConf.apiKey,
        dangerouslyAllowBrowser: true
    });

    console.log(messages)
    const completion = await openai.chat.completions.create({
        messages: [...messages],
        model: aiConf.model,
    });

    console.log(completion)
    return completion.choices[0].message.content;
}