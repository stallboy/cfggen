import {memo} from "react";
import {Form, InputNumber, Switch} from "antd";
import {setMaxImpl, setMaxNode, setRefIn, setRefOutDepth, setSearchMax, store} from "./store.ts";
import {useTranslation} from "react-i18next";

export const formLayout = {
    labelCol: {xs: {span: 24}, sm: {span: 6},},
    wrapperCol: {xs: {span: 24}, sm: {span: 18},},
};

export const TableSetting = memo(function TableSetting() {
    const {t} = useTranslation();
    const {maxImpl, refIn, refOutDepth, maxNode, searchMax} = store;

    return <Form {...formLayout} layout={'horizontal'}
                 initialValues={{maxImpl, refIn, refOutDepth, maxNode, searchMax}}>
        <Form.Item label={t('implsShowCnt')} name='maxImpl'>
            <InputNumber min={1} max={500} onChange={setMaxImpl}/>
        </Form.Item>

        <Form.Item name='refIn' label={t('refIn')} valuePropName="checked">
            <Switch onChange={setRefIn}/>
        </Form.Item>

        <Form.Item name='refOutDepth' label={t('refOutDepth')}>
            <InputNumber min={1} max={500} onChange={setRefOutDepth}/>
        </Form.Item>

        <Form.Item name='maxNode' label={t('maxNode')}>
            <InputNumber min={1} max={500} onChange={setMaxNode}/>
        </Form.Item>

        <Form.Item name='searchMax' label={t('searchMaxReturn')}>
            <InputNumber min={1} max={500} onChange={setSearchMax}/>
        </Form.Item>
    </Form>;
});