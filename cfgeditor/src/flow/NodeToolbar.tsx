import {memo} from "react";
import {Button, Space} from "antd";
import {ArrowDownOutlined, ArrowUpOutlined, CloseOutlined} from "@ant-design/icons";
import {useTranslation} from "react-i18next";
import type {NodeProps} from "@xyflow/react";
import {EntityEdit} from "@/domain/entityModel";
import type {EntityNode} from "./FlowGraph.tsx";
import {nodeAnchor} from "./nodeAnchor.ts";

const iconButtonStyle = { borderWidth: 0, backgroundColor: 'transparent' };
const moveUpIcon = <ArrowUpOutlined />;
const moveDownIcon = <ArrowDownOutlined />;
const closeIcon = <CloseOutlined />;

interface NodeToolbarProps {
    edit?: EntityEdit;
    nodeProps: NodeProps<EntityNode>;
}

// 节点操作按钮组（上移/下移/删除），仅 editable 态出现。命令参数统一走 nodeAnchor（§5-C3：原为三个内联箭头各自构造）。
// 纯图标按钮保持简洁，不加 Tooltip；aria-label 留给屏幕阅读器（不可见，不影响视觉）。
export const NodeToolbar = memo(function NodeToolbar({edit, nodeProps}: NodeToolbarProps) {
    const {t} = useTranslation();
    if (!edit) return null;
    return <Space size={1}>
        {edit.editOnMoveUp &&
            <Button className='nodrag' style={iconButtonStyle} icon={moveUpIcon} aria-label={t('nodeMoveUp')}
                onClick={() => edit.editOnMoveUp?.(nodeAnchor(nodeProps))} />}
        {edit.editOnMoveDown &&
            <Button className='nodrag' style={iconButtonStyle} icon={moveDownIcon} aria-label={t('nodeMoveDown')}
                onClick={() => edit.editOnMoveDown?.(nodeAnchor(nodeProps))} />}
        {edit.editOnDelete &&
            <Button className='nodrag' style={iconButtonStyle} icon={closeIcon} aria-label={t('nodeDelete')}
                onClick={() => edit.editOnDelete?.(nodeAnchor(nodeProps))} />}
    </Space>;
});
