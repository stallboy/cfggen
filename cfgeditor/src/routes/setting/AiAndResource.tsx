import {memo,} from "react";
import {useTranslation} from "react-i18next";
import {Button, Form, Input, Typography} from "antd";
import {
    setAIConf,
    useMyStore
} from "../../store/store.ts";

import {Schema} from "../table/schemaUtil.tsx";
import {TauriSetting} from "./TauriSeting.tsx";
import {isTauri} from "@tauri-apps/api/core";

const {Title} = Typography;

function onFinishAIConf(values: any) {
    console.log(values);
    setAIConf(values);
}

export const AiAndResource = memo(function ({schema}: {
    schema: Schema | undefined;
}) {
    const {t} = useTranslation();
    const {aiConf} = useMyStore();


    return <>
        <Form name="aiConf" layout={"vertical"} size={"small"}
              initialValues={aiConf} onFinish={onFinishAIConf}
              autoComplete="off">
            <Title level={4} style={{marginTop: -4}}>{t('aiConf')}</Title>

            <Form.Item name='baseUrl' label={t('baseUrl')}>
                <Input placeholder="base url"/>
            </Form.Item>
            <Form.Item name='apiKey' label={t('apiKey')}>
                <Input placeholder="api key"/>
            </Form.Item>

            <Form.Item name='model' label={t('model')}>
                <Input placeholder="model"/>
            </Form.Item>

            <Form.Item>
                <Button type="primary" htmlType="submit">
                    {t('setAIConf')}
                </Button>
            </Form.Item>
        </Form>

        {isTauri() && <TauriSetting schema={schema}/>}
    </>;

});
