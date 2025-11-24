import {Button, ColorPicker, Divider, Form, Input, Radio, Select, Space, Switch, Typography} from "antd";
import {CloseOutlined, PlusOutlined} from "@ant-design/icons";
import {useTranslation} from "react-i18next";

import {NodeShowType} from "../../store/storageJson.ts";
import {
    setNodeShow,
    useMyStore
} from "../../store/store.ts";
import {CSSProperties, memo, useMemo} from "react";

import { fixColors } from "./colorUtils.ts";

const {Title} = Typography;
const selectStyle: CSSProperties = {width: 160};

export const NodeShowSetting = memo(function () {
    const {t} = useTranslation();
    const {nodeShow} = useMyStore();

    function onFinish(values: any) {
        // console.log(values);
        const newNodeShow: NodeShowType = {
            ...nodeShow,
            ...values,
            nodeColorsByValue: fixColors(values.nodeColorsByValue),
            nodeColorsByLabel: fixColors(values.nodeColorsByLabel),
            fieldColorsByName: fixColors(values.fieldColorsByName)
        };
        setNodeShow(newNodeShow);
    }

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

    return <Form name="node show setting"  layout={"vertical"}
                 initialValues={nodeShow} onFinish={onFinish} size={"small"}
                 autoComplete="off">

        <Title level={4} style={{ marginTop: -4 }}>{t('layoutSettingTitle')}</Title>
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


        <Divider/>
        <Title level={4}>{t('colorSettingTitle')}</Title>

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
                                    <ColorPicker format="hex"/>
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
                                    <ColorPicker format="hex"/>
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
                                    <ColorPicker format="hex"/>
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

        <Divider />
        <Title level={4}>{t('otherSetting')}</Title>

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

        <Form.Item>
            <Button type="primary" htmlType="submit">
                {t('setNodeShow')}
            </Button>
        </Form.Item>
    </Form>;
});
