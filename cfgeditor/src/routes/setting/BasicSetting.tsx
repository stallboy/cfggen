import {memo} from "react";
import {Divider, Form, InputNumber, Switch} from "antd";
import {
    setIsNextIdShow,
    setMaxImpl,
    setMaxNode, setRecordMaxNode,
    setRecordRefIn,
    setRecordRefInShowLinkMaxNode, setRecordRefOutDepth, setRefIdsInDepth, setRefIdsMaxNode, setRefIdsOutDepth,
    setRefIn,
    setRefOutDepth,
    setSearchMax,
    store
} from "./store.ts";
import {useTranslation} from "react-i18next";

export const formLayout = {
    labelCol: {xs: {span: 24}, sm: {span: 4}},
    wrapperCol: {xs: {span: 24}, sm: {span: 20}},
};

export const formItemLayoutWithOutLabel = {
    wrapperCol: {
        xs: {span: 24, offset: 0},
        sm: {span: 20, offset: 4},
    },
};


export const BasicSetting = memo(function TableSetting() {
    const {t} = useTranslation();
    const {
        maxImpl, refIn, refOutDepth, maxNode,
        recordRefIn, recordRefInShowLinkMaxNode, recordRefOutDepth, recordMaxNode,
        refIdsInDepth, refIdsOutDepth, refIdsMaxNode,
        isNextIdShow, searchMax
    } = store;

    return <Form {...formLayout} layout={'horizontal'}
                 initialValues={{
                     maxImpl, refIn, refOutDepth, maxNode,
                     recordRefIn, recordRefInShowLinkMaxNode, recordRefOutDepth, recordMaxNode,
                     refIdsInDepth, refIdsOutDepth, refIdsMaxNode,
                     isNextIdShow, searchMax,
                 }}>


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

        <Divider/>
        <Form.Item name='recordRefIn' label={t('recordRefIn')} valuePropName="checked">
            <Switch onChange={setRecordRefIn}/>
        </Form.Item>

        <Form.Item name='recordRefInShowLinkMaxNode' label={t('recordRefInShowLinkMaxNode')}>
            <InputNumber min={1} max={20} onChange={setRecordRefInShowLinkMaxNode}/>
        </Form.Item>

        <Form.Item name='recordRefOutDepth' label={t('recordRefOutDepth')}>
            <InputNumber min={1} max={500} onChange={setRecordRefOutDepth}/>
        </Form.Item>

        <Form.Item name='recordMaxNode' label={t('recordMaxNode')}>
            <InputNumber min={1} max={500} onChange={setRecordMaxNode}/>
        </Form.Item>


        <Divider/>
        <Form.Item name='refIdsInDepth' label={t('refIdsInDepth')}>
            <InputNumber min={0} max={5} onChange={setRefIdsInDepth}/>
        </Form.Item>

        <Form.Item name='refIdsOutDepth' label={t('refIdsOutDepth')}>
            <InputNumber min={0} max={5} onChange={setRefIdsOutDepth}/>
        </Form.Item>

        <Form.Item name='refIdsMaxNode' label={t('refIdsMaxNode')}>
            <InputNumber min={1} max={50} onChange={setRefIdsMaxNode}/>
        </Form.Item>
        
        <Divider/>
        <Form.Item name='isNextIdShow' label={t('isNextIdShow')} valuePropName="checked">
            <Switch onChange={setIsNextIdShow}/>
        </Form.Item>

        <Form.Item name='searchMax' label={t('searchMaxReturn')}>
            <InputNumber min={1} max={500} onChange={setSearchMax}/>
        </Form.Item>

    </Form>;
});
