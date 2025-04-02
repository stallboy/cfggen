import {memo,} from "react";
import {useTranslation} from "react-i18next";
import {Button, Card, type SelectProps, Form, Input} from "antd";
import {
    setServer, setAIConf,
    useMyStore
} from "./store.ts";

import {formItemLayoutWithOutLabel, formLayout} from "./BasicSetting.tsx";
import {Schema} from "../table/schemaUtil.tsx";


function onFinishAIConf(values: any) {
    console.log(values);
    setAIConf(values);
}

export const ServerAndAi = memo(function ServerAndAi({schema}: {
    schema: Schema | undefined;
}) {
    const {t} = useTranslation();
    const {server, aiConf} = useMyStore();

    const tableOptions: SelectProps['options'] = []
    if (schema) {
        for (const t of schema.getAllEditableSTables()) {
            tableOptions.push({value: t.name});
        }
    }

    return <>
        <Form {...formLayout} layout={'horizontal'}
              initialValues={{server}}>

            <Form.Item label={t('curServer')}>
                {server}
            </Form.Item>
            <Form.Item name='server' label={t('newServer')}>
                <Input.Search enterButton={t('connect')} onSearch={(value: string) => setServer(value)}/>
            </Form.Item>
        </Form>

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
    </>;

});
