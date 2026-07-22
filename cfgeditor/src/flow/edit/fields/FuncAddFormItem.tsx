import {memo, useCallback} from "react";
import {Button, Flex, Tag} from "antd";
import {Handle, Position} from "@xyflow/react";
import {ArrowsAltOutlined, PlusSquareTwoTone, ShrinkOutlined} from "@ant-design/icons";
import {FuncAddType, ListFoldData} from "@/domain/entityModel";
import {nodeAnchor} from "../../nodeAnchor.ts";
import {useRefItemStyles} from "../shared/useRefItemStyles.ts";
import {LabelWithTooltip} from "../shared/LabelWithTooltip.tsx";
import type {StructRefItemProps} from "./StructRefItem.tsx";

interface FuncAddFormItemProps extends StructRefItemProps {
    func: FuncAddType;
    listFold?: ListFoldData;
    /** 折叠态摘要行的凸显底色（nodeShow.editFoldColor，与折叠节点同色） */
    foldColor?: string;
}

export const FuncAddFormItem = memo(function FuncAddFormItem({
                                                                name,
                                                                comment,
                                                                handleOut,
                                                                bgColor,
                                                                func,
                                                                nodeProps,
                                                                width,
                                                                listFold,
                                                                foldColor,
                                                            }: FuncAddFormItemProps) {
    const {rowStyle, handleOutStyle} = useRefItemStyles(width, bgColor);

    const handleAdd = useCallback(() => {
        func(nodeAnchor(nodeProps));
    }, [func, nodeProps]);

    const handleFold = useCallback(() => {
        listFold?.onUpdateListFold(true, nodeAnchor(nodeProps));
    }, [listFold, nodeProps]);

    const handleUnfold = useCallback(() => {
        listFold?.onUpdateListFold(false, nodeAnchor(nodeProps));
    }, [listFold, nodeProps]);

    // 折叠态：摘要行（字段名 + 元素数），editFoldColor 底色凸显（与折叠节点 flowNodeWithBorder 同语义）。
    // + 按钮保留：addArrayItem 会自动展开（session 层删 $fold_ 键，同一步 undo）。
    if (listFold?.folded) {
        return (
            <Flex gap="small" justify="flex-end" align="center"
                  style={{...rowStyle, backgroundColor: foldColor ?? '#ffd6e7'}}>
                <Button className="nodrag" onClick={handleAdd} icon={<PlusSquareTwoTone/>}/>
                <Tag>
                    <LabelWithTooltip name={`${name} (${listFold.itemCount})`} comment={comment}/>
                </Tag>
                <Button className="nodrag" type="text" icon={<ArrowsAltOutlined/>} onClick={handleUnfold}/>
                {handleOut && (
                    <Handle id={name} type="source" position={Position.Right} style={handleOutStyle}/>
                )}
            </Flex>
        );
    }

    return (
        <Flex gap="middle" justify="flex-end" style={rowStyle}>
            <Button className="nodrag" onClick={handleAdd} icon={<PlusSquareTwoTone/>}>
                <LabelWithTooltip name={name} comment={comment}/>
            </Button>
            {listFold && (
                <Button className="nodrag" type="text" icon={<ShrinkOutlined/>} onClick={handleFold}/>
            )}
            {handleOut && (
                <Handle id={name} type="source" position={Position.Right} style={handleOutStyle}/>
            )}
        </Flex>
    );
});
