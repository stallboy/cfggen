import {memo, useCallback} from "react";
import {Button, Flex} from "antd";
import {Handle, Position} from "@xyflow/react";
import type {NodeProps} from "@xyflow/react";
import {PlusSquareTwoTone} from "@ant-design/icons";
import {FuncAddType} from "@/domain/entityModel";
import type {EntityNode} from "../../FlowGraph.tsx";
import {nodeAnchor} from "../../nodeAnchor.ts";
import {useRefItemStyles} from "../shared/useRefItemStyles.ts";
import {LabelWithTooltip} from "../shared/LabelWithTooltip.tsx";
import type {StructRefItemProps} from "./StructRefItem.tsx";

interface FuncAddFormItemProps extends StructRefItemProps {
    func: FuncAddType;
    nodeProps: NodeProps<EntityNode>;
}

export const FuncAddFormItem = memo(function FuncAddFormItem({
                                                                name,
                                                                comment,
                                                                handleOut,
                                                                bgColor,
                                                                func,
                                                                nodeProps,
                                                                width,
                                                            }: FuncAddFormItemProps) {
    const {rowStyle, handleOutStyle} = useRefItemStyles(width, bgColor);

    const handleAdd = useCallback(() => {
        func(nodeAnchor(nodeProps));
    }, [func, nodeProps]);

    return (
        <Flex gap="middle" justify="flex-end" style={rowStyle}>
            <Button className="nodrag" onClick={handleAdd} icon={<PlusSquareTwoTone/>}>
                <LabelWithTooltip name={name} comment={comment}/>
            </Button>
            {handleOut && (
                <Handle id={name} type="source" position={Position.Right} style={handleOutStyle}/>
            )}
        </Flex>
    );
});
