import {Fragment, memo, useCallback, useMemo} from "react";
import type {CSSProperties} from "react";
import {Button, Flex, Tag} from "antd";
import {ArrowsAltOutlined} from "@ant-design/icons";
import type {NodeProps} from "@xyflow/react";
import {PrimitiveValue, StructRefEditField} from "@/domain/entityModel";
import {PrimitiveType} from "@/api/schemaModel";
import type {EntityNode} from "../../FlowGraph.tsx";
import {nodeAnchor} from "../../nodeAnchor.ts";
import {LabelWithTooltip} from "../shared/LabelWithTooltip.tsx";

interface EmbeddedSimpleStructuralItemProps {
    field: StructRefEditField;
    nodeProps: NodeProps<EntityNode>;
    bgColor?: string;
}

export const EmbeddedSimpleStructuralItem = memo(
    function EmbeddedSimpleStructuralItem({field, nodeProps}: EmbeddedSimpleStructuralItemProps) {
        const embeddedData = field.embeddedField!;

        // 组合comment：field.comment + embeddedData.note
        const fieldComment = useMemo(() => {
            const parts: string[] = [];
            if (field.comment) parts.push(field.comment);
            if (embeddedData.note) parts.push(embeddedData.note);
            return parts.join(' ');
        }, [field.comment, embeddedData.note]);

        // 字段名称Tag颜色（有note时黄色）
        const fieldNameTagColor = embeddedData.note ? '#876800' : 'blue';

        // 点击展开（写父对象上 $embed_<fieldName>=false，展开成独立子节点）
        const handleExpand = useCallback(() => {
            field.expandEmbedded?.(nodeAnchor(nodeProps));
        }, [field, nodeProps]);

        // 格式化单个值的显示
        const formatDisplayValue = useCallback((value: PrimitiveValue, type: PrimitiveType): string => {
            if (value === undefined || value === null) {
                return '-';
            }
            if (type === 'bool') {
                return value ? '✓' : '✗';
            }
            return String(value);
        }, []);

        // 渲染单个值Tag
        const renderValueTag = useCallback((
            value: PrimitiveValue,
            type: PrimitiveType,
            name: string,
            comment?: string
        ) => {
            const displayValue = formatDisplayValue(value, type);
            const valueStyle: CSSProperties = {
                color: (value === undefined || value === null || value === '')
                    ? '#999'
                    : undefined,
            };

            // 值的comment组合：name + comment
            const valueComment = comment ? `${name}: ${comment}` : name;

            return (
                <Tag color="blue" style={valueStyle}>
                    <LabelWithTooltip
                        name={displayValue}
                        comment={valueComment}
                    />
                </Tag>
            );
        }, [formatDisplayValue]);

        return (
            <Flex gap="small" justify="flex-end" align="center">
                {/* 显示字段名称（根据note显示黄色或蓝色Tag） */}
                <Tag color={fieldNameTagColor}>
                    <LabelWithTooltip
                        name={field.name}
                        comment={fieldComment || undefined}
                    />
                </Tag>

                {/* 显示implName（如果存在且非defaultImpl） */}
                {embeddedData.implName && (
                    <Tag color="cyan">
                        {embeddedData.implName}
                    </Tag>
                )}

                {/* 显示值（可能有多个） */}
                {embeddedData.fields.map(f => (
                    <Fragment key={f.name}>
                        {renderValueTag(f.value, f.type, f.name, f.comment)}
                    </Fragment>
                ))}

                {/* 展开按钮 */}
                <Button
                    className="nodrag"
                    type="text"
                    style={{color: '#1890ff'}}
                    icon={<ArrowsAltOutlined />}
                    onClick={handleExpand}
                />
            </Flex>
        );
    });
