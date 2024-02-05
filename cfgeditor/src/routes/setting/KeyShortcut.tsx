import {memo} from "react";
import {useTranslation} from "react-i18next";
import {Button, Card, Descriptions, Form, Input, Space} from "antd";
import {CloseOutlined, LeftOutlined, PlusOutlined, RightOutlined, SearchOutlined} from "@ant-design/icons";
import {path} from '@tauri-apps/api';
import {appWindow} from "@tauri-apps/api/window";
import {useQuery} from "@tanstack/react-query";
import {formLayout} from "./TableSetting.tsx";
import {setTauriConf, store} from "./store.ts";


async function queryResourceDir() {
    return await path.resourceDir();
}

export async function toggleFullScreen() {
    const isFullScreen = await appWindow.isFullscreen();
    await appWindow.setFullscreen(!isFullScreen);
}

export const TauriSetting = memo(function TauriSetting() {
    const {t} = useTranslation();
    const {data: resourceDir} = useQuery({
        queryKey: ['tauri', 'resourceDir'],
        queryFn: queryResourceDir,
    });
    const {tauriConf} = store;

    function onFinish(values: any) {
        console.log(values);
        setTauriConf(values);
    }


    return <>
        <p>resourceDir: {resourceDir}</p>
        <Button onClick={toggleFullScreen}> {t('toggleFullScreen')}</Button>

        <Card title={t("tauriConf")}>
            <Form name="tauriConf"  {...formLayout} initialValues={tauriConf} onFinish={onFinish}
                  autoComplete="off">
                <Form.Item label={t('resDirs')}>
                    <Form.List name="resDirs">
                        {(fields, {add, remove}) => (
                            <div style={{display: 'flex', flexDirection: 'column', rowGap: 16}}>
                                {fields.map(({key, name}) => (
                                    <Space key={key}>
                                        <Form.Item name={[name, 'dir']} noStyle>
                                            <Input placeholder="dir"/>
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

    </>

});

export const KeyShortCut = memo(function KeyShortcut() {
    const {t} = useTranslation();

    return <>
        <Descriptions title="Key Shortcut" bordered column={2} items={[
            {
                key: '1',
                label: <LeftOutlined/>,
                children: 'alt+x',
            },
            {
                key: '2',
                label: <RightOutlined/>,
                children: 'alt+c',
            },
            {
                key: '3',
                label: t('table'),
                children: 'alt+1',
            },
            {
                key: '4',
                label: t('tableRef'),
                children: 'alt+2',
            },
            {
                key: '5',
                label: t('record'),
                children: 'alt+3',
            },
            {
                key: '6',
                label: t('recordRef'),
                children: 'alt+4',
            },

            {
                key: '7',
                label: <SearchOutlined/>,
                children: 'alt+q',
            },
            {
                key: '8',
                label: t('toggleFullScreen'),
                children: 'alt+enter',
            },
        ]}/>
        {window.__TAURI__ && <TauriSetting/>}
    </>;


});
