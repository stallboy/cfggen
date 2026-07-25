import {Button, ColorPicker, Form, Input, Radio, Select, Space, Switch, Typography} from "antd";
import {CloseOutlined, PlusOutlined} from "@ant-design/icons";
import {useTranslation} from "react-i18next";

import {NodeShowType} from "@/domain/storageJson.ts";
import {
    setNodeShow,
    useMyStore
} from "@/store/store.ts";
import {CSSProperties, memo, ReactNode, useMemo} from "react";

import {fixColors} from "./colorUtils.ts";

const {Title} = Typography;
const selectStyle: CSSProperties = {width: 160};

// Form.List 行骨架：每行渲染 children(name) 加删除按钮，底部一个添加按钮
export function FormRowList({name, label, addText, children}: {
    name: string;
    label: string;
    addText: string;
    children: (name: number) => ReactNode;
}) {
    return <Form.Item label={label}>
        <Form.List name={name}>
            {(fields, {add, remove}) => (
                <div style={{display: 'flex', flexDirection: 'column', rowGap: 16}}>
                    {fields.map(({key, name}) => (
                        <Space key={key}>
                            {children(name)}
                            <CloseOutlined onClick={() => remove(name)}/>
                        </Space>
                    ))}

                    <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined/>}>
                        {addText}
                    </Button>
                </div>
            )}
        </Form.List>
    </Form.Item>;
}

// 关键字+颜色列表，三类配色共用，label 取与 name 同名的文案
function KeywordColorList({name, placeholder}: {
    name: string;
    placeholder: string;
}) {
    const {t} = useTranslation();
    return <FormRowList name={name} label={t(name)} addText={t('addColor')}>
        {(name) => <>
            <Form.Item name={[name, 'keyword']} noStyle>
                <Input placeholder={placeholder}/>
            </Form.Item>
            <Form.Item name={[name, 'color']} noStyle>
                <ColorPicker format="hex"/>
            </Form.Item>
        </>}
    </FormRowList>;
}

export const NodeShowSetting = memo(function () {
    const {t} = useTranslation();
    const {nodeShow} = useMyStore();

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

    return <Form name="node show setting" layout={"vertical"}
                 initialValues={nodeShow} size={"small"} autoComplete="off"
                 onValuesChange={(_changed, allValues) => {
                     const newNodeShow: NodeShowType = {
                         ...nodeShow,
                         ...allValues,
                         nodeColorsByValue: fixColors(allValues.nodeColorsByValue),
                         nodeColorsByLabel: fixColors(allValues.nodeColorsByLabel),
                         fieldColorsByName: fixColors(allValues.fieldColorsByName)
                     };
                     setNodeShow(newNodeShow);
                 }}>

        <Title level={4} style={{marginTop: -4}}>{t('layoutSettingTitle')}</Title>
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

        <Title level={4}>{t('colorSettingTitle')}</Title>

        <KeywordColorList name="nodeColorsByValue" placeholder="keyword"/>
        <KeywordColorList name="nodeColorsByLabel" placeholder="keyword"/>
        <KeywordColorList name="fieldColorsByName" placeholder="field"/>

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

        <FormRowList name="refTableHides" label={t('refTableHides')} addText={t('addTableHide')}>
            {(name) => <Form.Item name={name} noStyle>
                <Input placeholder="table"/>
            </Form.Item>}
        </FormRowList>

    </Form>;
});
