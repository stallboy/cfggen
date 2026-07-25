import {setTauriConf, useMyStore} from "@/store/store.ts";
import {memo, useCallback} from "react";
import {useTranslation} from "react-i18next";
import {useQuery} from "@tanstack/react-query";
import {queryKeys} from "@/services/queryKeys.ts";
import {App, Button, Checkbox, Divider, Form, Input, Typography} from "antd";
import {Schema} from "@/domain/schema.ts";
import {invalidateResInfos} from "@/res/readResInfosAsync.ts";
import {summarizeResAsync} from "@/res/summarizeResAsync.ts";
import {path} from "@tauri-apps/api";
import {FormRowList} from "./NodeShowSetting.tsx";

const {Title} = Typography;

function onFinishTauriConf(values: never) {
    setTauriConf(values);
}

export const TauriSetting = memo(function ({schema}: {
    schema: Schema | undefined
}) {
    const {t} = useTranslation();
    const {data: resourceDir} = useQuery({
        queryKey: queryKeys.tauriResourceDir(),
        queryFn: path.resourceDir,
    });
    const {resMap, tauriConf} = useMyStore();
    const {notification} = App.useApp();
    const summarizeRes = useCallback(() => {
        if (schema) {
            summarizeResAsync(schema, resMap).then((fullPath: string) => {
                notification.info({
                    title: `saveTo ${fullPath}`,
                    placement: 'topRight',
                    duration: 3
                });
            }).catch((e) => {
                // 失败也要给用户反馈，且兜底 unhandled rejection
                notification.error({
                    title: `summarizeRes failed: ${e}`,
                    placement: 'topRight',
                    duration: 3
                });
            })
        }
    }, [notification, schema, resMap])

    return <>
        <Divider/>
        <Title level={4}>{t('tauriConf')}</Title>
        <p>resourceDir: {resourceDir}</p>


        <Form name="tauriConf" size={"small"} layout={"vertical"}
              initialValues={tauriConf} onFinish={onFinishTauriConf}
              autoComplete="off">

            <Form.Item name='assetDir' label={t('assetDir')}>
                <Input placeholder="asset dir"/>
            </Form.Item>

            <Form.Item name='assetRefTable' label={t('assetRefTable')}>
                <Input placeholder="asset ref table"/>
            </Form.Item>

            <FormRowList name="resDirs" label={t('resDirs')} addText={t('addResDir')}>
                {(name) => <>
                    <Form.Item name={[name, 'dir']} noStyle>
                        <Input placeholder="dir"/>
                    </Form.Item>
                    <Form.Item name={[name, 'txtAsSrt']} valuePropName='checked' noStyle>
                        <Checkbox>txtAsSrt</Checkbox>
                    </Form.Item>
                    <Form.Item name={[name, 'lang']} noStyle>
                        <Input placeholder="lang"/>
                    </Form.Item>
                </>}
            </FormRowList>
            <Form.Item>
                <Button type="primary" htmlType="submit">
                    {t('setTauriConf')}
                </Button>
            </Form.Item>
        </Form>


        {schema && <Button onClick={summarizeRes}> {t('summarizeRes')}</Button>}
        <Button onClick={invalidateResInfos}> {t('reloadRes')}</Button>
    </>

});
