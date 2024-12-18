import {Button, ColorPicker, Divider, Form, Input, Radio, Select, Space, Switch} from "antd";
import {CloseOutlined, PlusOutlined} from "@ant-design/icons";
import {useTranslation} from "react-i18next";

import {KeywordColor, NodeShowType} from "./storageJson.ts";
import {
    setNodeShow,
    store
} from "./store.ts";
import {CSSProperties, memo, useMemo} from "react";
import {formItemLayoutWithOutLabel, formLayout} from "./BasicSetting.tsx";


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


function onFinish(values: any) {
    // console.log(values);
    const newNodeShow: NodeShowType = {
        ...values,
        nodeColorsByValue: fixColors(values.nodeColorsByValue),
        nodeColorsByLabel: fixColors(values.nodeColorsByLabel),
        fieldColorsByName: fixColors(values.fieldColorsByName),
        editFoldColor: fixColor(values.editFoldColor)
    };
    setNodeShow(newNodeShow);
}

const selectStyle: CSSProperties = {width: 160};

export const NodeShowSetting = memo(function () {
    const {t} = useTranslation();
    const {nodeShow} = store;

    const descOptions = useMemo(() =>
        [{label: t('show'), value: 'show'},
            {label: t('showFallbackValue'), value: 'showFallbackValue'},
            {label: t('showValue'), value: 'showValue'},
            {label: t('none'), value: 'none'}], [t]);

    const layoutOptions = useMemo(() =>
        [{label: t('LINEAR_SEGMENTS'), value: 'LINEAR_SEGMENTS'},
            {label: t('SIMPLE'), value: 'SIMPLE'},
            {label: t('BRANDES_KOEPF'), value: 'BRANDES_KOEPF'},
            {label: t('mrtree'), value: 'mrtree'}], [t]);

    return <Form name="node show setting"  {...formLayout} initialValues={nodeShow} onFinish={onFinish}
                 autoComplete="off">

        <Form.Item name='recordLayout' label={t('recordLayout')}>
            <Select style={selectStyle} options={layoutOptions}/>
        </Form.Item>

        <Form.Item name='editLayout' label={t('editLayout')}>
            <Select style={selectStyle} options={layoutOptions}/>
        </Form.Item>
        <Form.Item name='refLayout' label={t('refLayout')}>
            <Select style={selectStyle} options={layoutOptions}/>
        </Form.Item>

        <Form.Item name='tableLayout' label={t('tableLayout')}>
            <Select style={selectStyle} options={layoutOptions}/>
        </Form.Item>
        <Form.Item name='tableRefLayout' label={t('tableRefLayout')}>
            <Select style={selectStyle} options={layoutOptions}/>
        </Form.Item>

        <Form.Item label={t('nodeColorsByValue')}>
            <Form.List name="nodeColorsByValue">
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
                            {t('addColor')}
                        </Button>
                    </div>
                )}
            </Form.List>
        </Form.Item>

        <Form.Item label={t('nodeColorsByLabel')}>
            <Form.List name="nodeColorsByLabel">
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
                            {t('addColor')}
                        </Button>
                    </div>
                )}
            </Form.List>
        </Form.Item>

        <Form.Item label={t('fieldColorsByName')}>
            <Form.List name="fieldColorsByName">
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
                            {t('addColor')}
                        </Button>
                    </div>
                )}
            </Form.List>
        </Form.Item>

        <Form.Item name='editFoldColor' label={t('editFoldColor')}>
            <ColorPicker/>
        </Form.Item>

        <Divider />
        <Form.Item name='refIsShowCopyable' label={t('refIsShowCopyable')} valuePropName='checked'>
            <Switch/>
        </Form.Item>

        <Form.Item name='refShowDescription' label={t('refShowDescription')}>
            <Radio.Group optionType='button' buttonStyle='solid' options={descOptions}/>
        </Form.Item>

        <Form.Item name='refContainEnum' label={t('refContainEnum')} valuePropName='checked'>
            <Switch/>
        </Form.Item>

        <Form.Item label={t('refTableHides')}>
            <Form.List name="refTableHides">
                {(fields, {add, remove}) => (
                    <div style={{display: 'flex', flexDirection: 'column', rowGap: 16}}>
                        {fields.map(({key, name}) => (
                            <Space key={key}>
                                <Form.Item name={name} noStyle>
                                    <Input placeholder="table"/>
                                </Form.Item>
                                <CloseOutlined onClick={() => remove(name)}/>
                            </Space>
                        ))}

                        <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined/>}>
                            {t('addTableHide')}
                        </Button>
                    </div>
                )}
            </Form.List>
        </Form.Item>

        <Form.Item {...formItemLayoutWithOutLabel}>
            <Button type="primary" htmlType="submit">
                {t('setNodeShow')}
            </Button>
        </Form.Item>
    </Form>;
});
