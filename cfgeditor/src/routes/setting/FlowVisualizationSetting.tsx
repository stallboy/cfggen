import { memo } from "react";
import { Form, InputNumber, ColorPicker, Space, Divider, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { useMyStore, setNodeShow } from "./store.ts";
import { fixColor } from "./colorUtils.ts";
import { NodeShowType } from "./storageJson.ts";

const { Title } = Typography;

export const FlowVisualizationSetting = memo(function FlowVisualizationSetting() {
    const { t } = useTranslation();
    const { nodeShow } = useMyStore();

    function onValuesChange(_changedValues: Partial<NodeShowType>, allValues: Partial<NodeShowType>) {
        const newNodeShow: NodeShowType = {
            ...nodeShow,
            ...allValues,
            edgeColor: fixColor(allValues.edgeColor),
            editFoldColor: fixColor(allValues.editFoldColor),
            nodeColor: fixColor(allValues.nodeColor),
            nodeRefColor: fixColor(allValues.nodeRefColor),
            nodeRef2Color: fixColor(allValues.nodeRef2Color),
            nodeRefInColor: fixColor(allValues.nodeRefInColor)
        };
        setNodeShow(newNodeShow);
    }

    return (
        <Form
            layout="vertical"
            initialValues={nodeShow}
            onValuesChange={onValuesChange}
        >
            <Title level={4}>{t('nodeDimensions')}</Title>

            <Space direction="vertical" style={{ width: '100%' }}>
                <Form.Item label={t('nodeWidth')} name="nodeWidth">
                    <InputNumber
                        min={100}
                        max={500}
                        step={10}
                        addonAfter="px"
                    />
                </Form.Item>

                <Form.Item label={t('editNodeWidth')} name="editNodeWidth">
                    <InputNumber
                        min={100}
                        max={500}
                        step={10}
                        addonAfter="px"
                    />
                </Form.Item>
            </Space>

            <Divider />

            <Title level={4}>{t('edgeStyling')}</Title>

            <Space direction="vertical" style={{ width: '100%' }}>
                <Form.Item label={t('edgeColor')} name="edgeColor">
                    <ColorPicker
                        format="hex"
                        showText
                    />
                </Form.Item>

                <Form.Item label={t('edgeStrokeWidth')} name="edgeStrokeWidth">
                    <InputNumber
                        min={1}
                        max={10}
                        step={0.5}
                        addonAfter="px"
                    />
                </Form.Item>

                <Form.Item label={t('editFoldColor')} name="editFoldColor">
                    <ColorPicker
                        format="hex"
                        showText
                    />
                </Form.Item>
            </Space>

            <Divider />

            <Title level={4}>{t('nodeColors')}</Title>

            <Space direction="vertical" style={{ width: '100%' }}>
                <Form.Item label={t('nodeColor')} name="nodeColor">
                    <ColorPicker
                        format="hex"
                        showText
                    />
                </Form.Item>

                <Form.Item label={t('nodeRefColor')} name="nodeRefColor">
                    <ColorPicker
                        format="hex"
                        showText
                    />
                </Form.Item>

                <Form.Item label={t('nodeRef2Color')} name="nodeRef2Color">
                    <ColorPicker
                        format="hex"
                        showText
                    />
                </Form.Item>

                <Form.Item label={t('nodeRefInColor')} name="nodeRefInColor">
                    <ColorPicker
                        format="hex"
                        showText
                    />
                </Form.Item>
            </Space>

            <Divider />

            <Title level={4}>{t('layoutSpacing')}</Title>

            <Space direction="vertical" style={{ width: '100%' }}>
                <Form.Item
                    label={t('mrtreeSpacing')}
                    name="mrtreeSpacing"
                    tooltip={t('mrtreeSpacingTooltip')}
                >
                    <InputNumber
                        min={20}
                        max={200}
                        step={10}
                    />
                </Form.Item>

                <Form.Item
                    label={t('layeredSpacing')}
                    name="layeredSpacing"
                    tooltip={t('layeredSpacingTooltip')}
                >
                    <InputNumber
                        min={20}
                        max={200}
                        step={10}
                    />
                </Form.Item>

                <Form.Item
                    label={t('layeredNodeSpacing')}
                    name="layeredNodeSpacing"
                    tooltip={t('layeredNodeSpacingTooltip')}
                >
                    <InputNumber
                        min={20}
                        max={200}
                        step={10}
                    />
                </Form.Item>
            </Space>
        </Form>
    );
});