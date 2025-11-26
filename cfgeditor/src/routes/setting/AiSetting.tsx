import {memo,} from "react";
import {useTranslation} from "react-i18next";
import {Button, Form, Input, Typography} from "antd";
import {
    setAIConf,
    useMyStore
} from "../../store/store.ts";

const {Title} = Typography;

export const AiSetting = memo(function () {
    const {t} = useTranslation();
    const {aiConf} = useMyStore();


    return <>
        <Form name="aiConf" layout={"vertical"} size={"small"} initialValues={aiConf} autoComplete="off" onFinish={
            (values) => {
                console.log(values);
                setAIConf(values);
            }}>

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
    </>;

});
