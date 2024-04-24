import {Button, Card, Checkbox, ColorPicker, Form, Input, Radio, Space, Switch} from "antd";
import {CloseOutlined, PlusOutlined} from "@ant-design/icons";
import {useTranslation} from "react-i18next";

import {KeywordColor, NodeShowType, TableHideAndColor} from "./storageJson.ts";
import {
    setNodeShow,
    store
} from "./store.ts";
import {memo} from "react";
import {formLayout} from "./BasicSetting.tsx";


function fixColor(color: any) {
    let c;
    if (typeof color == 'object') {
        c = color.toHexString();
    } else if (typeof color == 'string') {
        c = color;
    } else {
        c = '#1677ff';
    }
    return c;
}

function fixColors(keywordColors: any[]): KeywordColor[] {
    const colors = [];
    for (const {keyword, color} of keywordColors) {
        colors.push({keyword: keyword, color: fixColor(color)})
    }
    return colors;
}

function fixHideAndColors(keywordHideAndColors: any[]): TableHideAndColor[] {
    const colors = [];
    for (const {keyword, hide, color} of keywordHideAndColors) {
        colors.push({keyword: keyword, hide: !!hide, color: fixColor(color)})
    }
    return colors;
}

function onFinish(values: any) {
    // console.log(values);
    const newNodeShow: NodeShowType = {
        ...values,
        keywordColors: fixColors(values.keywordColors),
        tableHideAndColors: fixHideAndColors(values.tableHideAndColors),
        fieldColors: fixColors(values.fieldColors),
    };
    setNodeShow(newNodeShow);
}

export const RecordShowSetting = memo(function RecordRefSetting() {
    const {t} = useTranslation();
    const {nodeShow,} = store;


    return <>

        <Card title={t("nodeShowSetting")}>
            <Form name="node show setting"  {...formLayout} initialValues={nodeShow} onFinish={onFinish}
                  autoComplete="off">

                <Form.Item name='showHead' label={t('showHead')}>
                    <Radio.Group optionType='button' buttonStyle='solid' options={[
                        {label: t('show'), value: 'show'},
                        {label: t('showCopyable'), value: 'showCopyable'},]}/>
                </Form.Item>

                <Form.Item name='showDescription' label={t('showDescription')}>
                    <Radio.Group optionType='button' buttonStyle='solid' options={[
                        {label: t('show'), value: 'show'},
                        {label: t('showFallbackValue'), value: 'showFallbackValue'},
                        {label: t('showValue'), value: 'showValue'},
                        {label: t('none'), value: 'none'}]}/>
                </Form.Item>

                <Form.Item name='containEnum' label={t('containEnum')} valuePropName='checked'>
                    <Switch/>
                </Form.Item>

                <Form.Item name='nodePlacementStrategy' label={t('nodePlacementStrategy')}>
                    <Radio.Group optionType='button' buttonStyle='solid' options={[
                        {label: t('LINEAR_SEGMENTS'), value: 'LINEAR_SEGMENTS'},
                        {label: t('SIMPLE'), value: 'SIMPLE'},
                        {label: t('BRANDES_KOEPF'), value: 'BRANDES_KOEPF'}]}/>
                </Form.Item>

                <Form.Item label={t('keywordColors')}>
                    <Form.List name="keywordColors">
                        {(fields, {add, remove}) => (
                            <div style={{display: 'flex', flexDirection: 'column', rowGap: 16}}>
                                {fields.map(({key, name}) => (
                                    <Space key={key}>
                                        <Form.Item name={[name, 'keyword']} noStyle>
                                            <Input placeholder="keyword"/>
                                        </Form.Item>
                                        <Form.Item name={[name, 'color']} noStyle>
                                            <ColorPicker/>
                                        </Form.Item>
                                        <CloseOutlined onClick={() => remove(name)}/>
                                    </Space>
                                ))}

                                <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined/>}>
                                    {t('addKeywordColor')}
                                </Button>
                            </div>
                        )}
                    </Form.List>
                </Form.Item>

                <Form.Item label={t('tableHideAndColors')}>
                    <Form.List name="tableHideAndColors">
                        {(fields, {add, remove}) => (
                            <div style={{display: 'flex', flexDirection: 'column', rowGap: 16}}>
                                {fields.map(({key, name}) => (
                                    <Space key={key}>
                                        <Form.Item name={[name, 'keyword']} noStyle>
                                            <Input placeholder="table"/>
                                        </Form.Item>
                                        <Form.Item name={[name, 'hide']} valuePropName='checked' noStyle>
                                            <Checkbox>hide</Checkbox>
                                        </Form.Item>
                                        <Form.Item name={[name, 'color']} noStyle>
                                            <ColorPicker/>
                                        </Form.Item>
                                        <CloseOutlined onClick={() => remove(name)}/>
                                    </Space>
                                ))}

                                <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined/>}>
                                    {t('addTableHideAndColor')}
                                </Button>
                            </div>
                        )}
                    </Form.List>
                </Form.Item>


                <Form.Item label={t('fieldColors')}>
                    <Form.List name="fieldColors">
                        {(fields, {add, remove}) => (
                            <div style={{display: 'flex', flexDirection: 'column', rowGap: 16}}>
                                {fields.map(({key, name}) => (
                                    <Space key={key}>
                                        <Form.Item name={[name, 'keyword']} noStyle>
                                            <Input placeholder="field"/>
                                        </Form.Item>
                                        <Form.Item name={[name, 'color']} noStyle>
                                            <ColorPicker/>
                                        </Form.Item>
                                        <CloseOutlined onClick={() => remove(name)}/>
                                    </Space>
                                ))}

                                <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined/>}>
                                    {t('addFieldColor')}
                                </Button>
                            </div>
                        )}
                    </Form.List>
                </Form.Item>
                
                <Form.Item>
                    <Button type="primary" htmlType="submit">
                        {t('setNodeShow')}
                    </Button>
                </Form.Item>
            </Form>
        </Card>
    </>;

});
