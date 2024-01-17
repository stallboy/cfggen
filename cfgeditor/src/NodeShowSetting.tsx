import {Button, Card, ColorPicker, Form, Input, Radio, Space, Switch} from "antd";
import {CloseOutlined, PlusOutlined} from "@ant-design/icons";
import {useTranslation} from "react-i18next";

import {KeywordColor} from "./func/localStoreJson.ts";
import {setNodeShow, store} from "./model/store.ts";


export function NodeShowSetting() {
    const {t} = useTranslation();
    const {nodeShow} = store;

    function onFinish(values: any) {
        // console.log(values);
        const newNodeShow = {
            ...values,
            keywordColors: fixColors(values.keywordColors),
            tableColors: fixColors(values.tableColors)
        };
        setNodeShow(newNodeShow);
    }

    function fixColors(keywordColors: any[]): KeywordColor[] {
        let colors = [];
        for (let {keyword, color} of keywordColors) {
            let c;
            if (typeof color == 'object') {
                c = color.toHexString();
            } else if (typeof color == 'string') {
                c = color;
            } else {
                c = '#1677ff';
            }
            colors.push({keyword: keyword, color: c})
        }
        return colors;
    }

    const formLayout = {
        labelCol: {xs: {span: 24}, sm: {span: 6},},
        wrapperCol: {xs: {span: 24}, sm: {span: 18},},
    };

    return <Card title={t("nodeShowSetting")}>
        <Form name="node show setting"  {...formLayout} initialValues={nodeShow} onFinish={onFinish} autoComplete="off">

            <Form.Item name='showHead' label={t('showHead')}>
                <Radio.Group optionType='button' buttonStyle='solid' options={[
                    {label: t('show'), value: 'show'},
                    {label: t('showCopyable'), value: 'showCopyable'},]}/>
            </Form.Item>

            <Form.Item name='showDescription' label={t('showDescription')}>
                <Radio.Group optionType='button' buttonStyle='solid'  options={[
                    {label: t('show'), value: 'show'},
                    {label: t('showFallbackValue'), value: 'showFallbackValue'},
                    {label: t('showValue'), value: 'showValue'},
                    {label: t('none'), value: 'none'}]}/>
            </Form.Item>

            <Form.Item name='containEnum' label={t('containEnum')} valuePropName='checked'>
                <Switch/>
            </Form.Item>

            <Form.Item name='nodePlacementStrategy' label={t('nodePlacementStrategy')}>
                <Radio.Group optionType='button' buttonStyle='solid'  options={[
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

            <Form.Item label={t('tableColors')}>
                <Form.List name="tableColors">
                    {(fields, {add, remove}) => (
                        <div style={{display: 'flex', flexDirection: 'column', rowGap: 16}}>
                            {fields.map(({key, name}) => (
                                <Space key={key}>
                                    <Form.Item name={[name, 'keyword']} noStyle>
                                        <Input placeholder="table"/>
                                    </Form.Item>
                                    <Form.Item name={[name, 'color']} noStyle>
                                        <ColorPicker/>
                                    </Form.Item>
                                    <CloseOutlined onClick={() => remove(name)}/>
                                </Space>
                            ))}

                            <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined/>}>
                                {t('addTableColor')}
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
    </Card>;

}
