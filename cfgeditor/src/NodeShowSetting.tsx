import {Button, Card, ColorPicker, Form, Input, Select, Space, Switch} from "antd";
import {MinusCircleOutlined, PlusOutlined} from "@ant-design/icons";
import {useTranslation} from "react-i18next";

import {NodeShowType} from "./model/entityModel.ts";


export function NodeShowSetting({nodeShow, setNodeShow}: {
    nodeShow: NodeShowType;
    setNodeShow: (nodeShow: NodeShowType) => void;
}) {
    const {t} = useTranslation();

    function onFinish(values: any) {
        // console.log(values.keywordColors);
        let colors = [];
        for (let keywordColor of values.keywordColors) {
            let color;
            if (typeof keywordColor.color == 'object') {
                color = keywordColor.color.toHexString();
            } else if (typeof keywordColor.color == 'string') {
                color = keywordColor.color;
            } else {
                color = '#1677ff';
            }
            colors.push({keyword: keywordColor.keyword, color})
        }

        const newNodeShow = {...values, keywordColors: colors};
        setNodeShow(newNodeShow);
        localStorage.setItem('nodeShow', JSON.stringify(newNodeShow));
    }

    const formLayout = {
        labelCol: {xs: {span: 24}, sm: {span: 6},},
        wrapperCol: {xs: {span: 24}, sm: {span: 18},},
    };

    const formItemLayout = formLayout;

    const formItemLayoutWithOutLabel = {
        wrapperCol: {
            xs: {span: 24, offset: 0},
            sm: {span: 18, offset: 6},
        },
    };
    return <Card title={t("nodeShowSetting")}>
        <Form name="node show setting"  {...formLayout} initialValues={nodeShow} onFinish={onFinish} autoComplete="off">

            <Form.Item name='showHead' label={t('showHead')}>
                <Select options={[
                    {label: t('show'), value: 'show'},
                    {label: t('showCopyable'), value: 'showCopyable'},]}/>
            </Form.Item>

            <Form.Item name='showDescription' label={t('showDescription')}>
                <Select options={[
                    {label: t('show'), value: 'show'},
                    {label: t('showFallbackValue'), value: 'showFallbackValue'},
                    {label: t('showValue'), value: 'showValue'},
                    {label: t('none'), value: 'none'}]}/>
            </Form.Item>

            <Form.Item name='containEnum' label={t('containEnum')} valuePropName='checked'>
                <Switch/>
            </Form.Item>

            <Form.Item name='nodePlacementStrategy' label={t('nodePlacementStrategy')}>
                <Select options={[
                    {label: t('LINEAR_SEGMENTS'), value: 'LINEAR_SEGMENTS'},
                    {label: t('SIMPLE'), value: 'SIMPLE'},
                    {label: t('BRANDES_KOEPF'), value: 'BRANDES_KOEPF'}]}/>
            </Form.Item>

            <Form.List name="keywordColors">
                {(fields, {add, remove}) => (
                    <>
                        {fields.map(({key, name}, index) => (
                            <Form.Item {...(index === 0 ? formItemLayout : formItemLayoutWithOutLabel)}
                                       label={index === 0 ? t('keywordColors') : ''}
                                       key={key}>
                                <Space>
                                    <Form.Item name={[name, 'keyword']} noStyle>
                                        <Input placeholder="keyword"/>
                                    </Form.Item>
                                    <Form.Item name={[name, 'color']} noStyle>
                                        <ColorPicker/>
                                    </Form.Item>
                                    <MinusCircleOutlined className="dynamic-delete-button"
                                                         onClick={() => remove(name)}/>
                                </Space>
                            </Form.Item>
                        ))}
                        <Form.Item {...(fields.length === 0 ? formItemLayout : formItemLayoutWithOutLabel)}
                                   label={fields.length === 0 ? t('keywordColors') : ''}>
                            <Space>
                                <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined/>}>
                                    {t('addKeywordColor')}
                                </Button>
                            </Space>
                        </Form.Item>
                    </>
                )}
            </Form.List>

            <Form.Item>
                <Button type="primary" htmlType="submit">
                    {t('setNodeShow')}
                </Button>
            </Form.Item>
        </Form>
    </Card>;

}