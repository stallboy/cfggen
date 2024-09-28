import {memo,} from "react";
import {useTranslation} from "react-i18next";
import {Button, Card, type SelectProps, Form, Input, Select, Flex, Space} from "antd";
import {
    setServer, setAIConf,
    store
} from "./store.ts";

import {formLayout} from "./BasicSetting.tsx";
import {Schema} from "../table/schemaUtil.ts";
import {CloseOutlined, PlusOutlined} from "@ant-design/icons";


function onFinishAIConf(values: any) {
    console.log(values);
    setAIConf(values);
}

export const ServerAndAi = memo(function ServerAndAi({schema}: {
    schema: Schema | undefined;
}) {
    const {t} = useTranslation();
    const {server, aiConf} = store;

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

                <Form.Item label={t('examples')}>
                    <Form.List name="examples">
                        {(fields, {add, remove}) => (
                            <div style={{display: 'flex', flexDirection: 'column', rowGap: 16}}>
                                {fields.map(({key, name}) => (
                                    <Flex key={key} vertical>
                                        <Space>
                                            <Form.Item name={[name, 'table']} noStyle>
                                                <Select options={tableOptions} style={{width:120}}/>
                                            </Form.Item>
                                            <Form.Item name={[name, 'id']} noStyle>
                                                <Input placeholder="id"/>
                                            </Form.Item>
                                            <CloseOutlined onClick={() => remove(name)}/>
                                        </Space>

                                        <Form.Item name={[name, 'description']} noStyle>
                                            <Input.TextArea placeholder="description"/>
                                        </Form.Item>

                                    </Flex>
                                ))}

                                <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined/>}>
                                    {t('addExample')}
                                </Button>
                            </div>
                        )}
                    </Form.List>
                </Form.Item>
                <Form.Item>
                    <Button type="primary" htmlType="submit">
                        {t('setAIConf')}
                    </Button>
                </Form.Item>
            </Form>
        </Card>
    </>;

});
