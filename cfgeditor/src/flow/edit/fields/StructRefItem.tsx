import {memo} from "react";
import {Flex, Tag} from "antd";
import {Handle, Position} from "@xyflow/react";
import {useRefItemStyles} from "../shared/useRefItemStyles.ts";
import {LabelWithTooltip} from "../shared/LabelWithTooltip.tsx";

export interface StructRefItemProps {
    name: string;
    comment?: string;
    handleOut?: boolean;
    bgColor?: string;
    width?: number;
}

export const StructRefItem = memo(function StructRefItem({
                                                            name,
                                                            comment,
                                                            handleOut,
                                                            bgColor,
                                                            width,
                                                        }: StructRefItemProps) {
    const {rowStyle, handleOutStyle} = useRefItemStyles(width, bgColor);

    return (
        <Flex gap="middle" justify="flex-end" style={rowStyle}>
            <Tag color="blue">
                <LabelWithTooltip name={name} comment={comment}/>
            </Tag>
            {handleOut && (
                <Handle id={name} type="source" position={Position.Right} style={handleOutStyle}/>
            )}
        </Flex>
    );
});
