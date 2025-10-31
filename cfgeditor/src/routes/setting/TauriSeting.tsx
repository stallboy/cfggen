import {setTauriConf, useMyStore} from "../../store/store.ts";
import {memo, useCallback} from "react";
import {useTranslation} from "react-i18next";
import {useQuery} from "@tanstack/react-query";
import {App, Button, Card, Checkbox, Form, Input, Space} from "antd";
import {formLayout} from "./BasicSetting.tsx";
import {CloseOutlined, PlusOutlined} from "@ant-design/icons";
import {Schema} from "../table/schemaUtil.tsx";
import {invalidateResInfos} from "../../res/readResInfosAsync.ts";
import {summarizeResAsync} from "../../res/summarizeResAsync.ts";
import {path} from "@tauri-apps/api";



function onFinishTauriConf(values: any) {
    console.log(values);
    setTauriConf(values);
}

export const TauriSetting = memo(function ({schema}: {
    schema: Schema | undefined
}) {
    const {t} = useTranslation();
    const {data: resourceDir} = useQuery({
        queryKey: ['tauri', 'resourceDir'],
        queryFn: path.resourceDir,
    });
    const {tauriConf} = useMyStore();
    const {notification} = App.useApp();
    const summarizeRes = useCallback(() => {
        if (schema) {
            summarizeResAsync(schema).then((fullPath: string) => {
                notification.info({
                    message: `saveTo ${fullPath}`,
                    placement: 'topRight',
                    duration: 3
                });
            })
        }
    }, [notification, schema])

    return <>
        <p>resourceDir: {resourceDir}</p>

        <Card title={t("tauriConf")}>
            <Form name="tauriConf"  {...formLayout} initialValues={tauriConf} onFinish={onFinishTauriConf}
                  autoComplete="off">
                <Form.Item  name='assetDir' label={t('assetDir')}>
                    <Input placeholder="asset dir"/>
                </Form.Item>

                <Form.Item name='assetRefTable' label={t('assetRefTable')} >
                    <Input placeholder="asset ref table"/>
                </Form.Item>

                <Form.Item label={t('resDirs')}>
                    <Form.List name="resDirs">
                        {(fields, {add, remove}) => (
                            <div style={{display: 'flex', flexDirection: 'column', rowGap: 16}}>
                                {fields.map(({key, name}) => (
                                    <Space key={key}>
                                        <Form.Item name={[name, 'dir']} noStyle>
                                            <Input placeholder="dir"/>
                                        </Form.Item>
                                        <Form.Item name={[name, 'txtAsSrt']} valuePropName='checked' noStyle>
                                            <Checkbox>txtAsSrt</Checkbox>
                                        </Form.Item>
                                        <Form.Item name={[name, 'lang']} noStyle>
                                            <Input placeholder="lang"/>
                                        </Form.Item>
                                        <CloseOutlined onClick={() => remove(name)}/>
                                    </Space>
                                ))}

                                <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined/>}>
                                    {t('addResDir')}
                                </Button>
                            </div>
                        )}
                    </Form.List>
                </Form.Item>
                <Form.Item>
                    <Button type="primary" htmlType="submit">
                        {t('setTauriConf')}
                    </Button>
                </Form.Item>
            </Form>
        </Card>

        {schema && <Button onClick={summarizeRes}> {t('summarizeRes')}</Button>}
        <Button onClick={invalidateResInfos}> {t('reloadRes')}</Button>
    </>

});