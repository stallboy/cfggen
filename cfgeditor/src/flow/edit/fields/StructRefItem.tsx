import {memo, useCallback} from "react";
import {Button, Flex, Tag} from "antd";
import {ShrinkOutlined} from "@ant-design/icons";
import {Handle, Position} from "@xyflow/react";
import type {NodeProps} from "@xyflow/react";
import type {EntityPosition} from "@/domain/entityModel";
import type {EntityNode} from "../../FlowGraph.tsx";
import {nodeAnchor} from "../../nodeAnchor.ts";
import {useRefItemStyles} from "../shared/useRefItemStyles.ts";
import {LabelWithTooltip} from "../shared/LabelWithTooltip.tsx";

export interface StructRefItemProps {
    name: string;
    comment?: string;
    handleOut?: boolean;
    bgColor?: string;
    width?: number;
    nodeProps: NodeProps<EntityNode>;
    /** 回嵌入口（子结构可内嵌且当前展开时挂载）：点击删 `$embed_<fieldName>` 回到内嵌 Tag 态 */
    reEmbed?: (position: EntityPosition) => void;
}

export const StructRefItem = memo(function StructRefItem({
                                                            name,
                                                            comment,
                                                            handleOut,
                                                            bgColor,
                                                            width,
                                                            nodeProps,
                                                            reEmbed,
                                                        }: StructRefItemProps) {
    const {rowStyle, handleOutStyle} = useRefItemStyles(width, bgColor);

    const handleFold = useCallback(() => {
        reEmbed?.(nodeAnchor(nodeProps));
    }, [reEmbed, nodeProps]);

    return (
        <Flex gap="middle" justify="flex-end" style={rowStyle}>
            <Tag color="blue">
                <LabelWithTooltip name={name} comment={comment}/>
            </Tag>
            {reEmbed && (
                <Button className="nodrag" type="text" icon={<ShrinkOutlined/>} onClick={handleFold}/>
            )}
            {handleOut && (
                <Handle id={name} type="source" position={Position.Right} style={handleOutStyle}/>
            )}
        </Flex>
    );
});
