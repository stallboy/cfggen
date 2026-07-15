import {memo} from "react";
import {Divider, Form, Input, Typography} from "antd";
import {useTranslation} from "react-i18next";
import {setServer, useMyStore} from "@/store/store.ts";
import {AiSetting} from "./AiSetting.tsx";

const {Title} = Typography;

/**
 * "连接" tab：服务器地址（Input.Search onSearch 即时连接）+ AI 服务配置（AiSetting，提交保存）。
 */
export const ConnectionSetting = memo(function ConnectionSetting() {
    const {t} = useTranslation();
    const {server} = useMyStore();

    return <>
        <Title level={4} style={{marginTop: -4}}>{t('connection')}</Title>
        <Form layout="vertical" size="small">
            <Form.Item label={t('curServer')}>{server}</Form.Item>
            <Form.Item label={t('newServer')}>
                <Input.Search enterButton={t('connect')} onSearch={(value: string) => setServer(value)}/>
            </Form.Item>
        </Form>

        <Divider/>
        <AiSetting/>
    </>;
});
