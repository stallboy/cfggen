import {memo,} from "react";
import {useTranslation} from "react-i18next";
import {Button, Card, Divider, Form, Input} from "antd";
import {
    setAIConf,
    useMyStore
} from "../../store/store.ts";

import {formItemLayoutWithOutLabel, formLayout} from "./BasicSetting.tsx";
import {Schema} from "../table/schemaUtil.tsx";
import {TauriSetting} from "./TauriSeting.tsx";
import {isTauri} from "@tauri-apps/api/core";


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
        <Divider/>
        <Card title={t("aiConf")}>
            <Form name="aiConf"  {...formLayout} initialValues={aiConf} onFinish={onFinishAIConf}
                  autoComplete="off">
                <Form.Item name='baseUrl' label={t('baseUrl')}>
                    <Input placeholder="base url"/>
                </Form.Item>
                <Form.Item name='apiKey' label={t('apiKey')}>
                    <Input placeholder="api key"/>
                </Form.Item>

                <Form.Item name='model' label={t('model')}>
                    <Input placeholder="model"/>
                </Form.Item>

                <Form.Item {...formItemLayoutWithOutLabel}>
                    <Button type="primary" htmlType="submit">
                        {t('setAIConf')}
                    </Button>
                </Form.Item>
            </Form>
        </Card>
        <Divider/>
        {isTauri() && <TauriSetting schema={schema}/>}
    </>;

});
