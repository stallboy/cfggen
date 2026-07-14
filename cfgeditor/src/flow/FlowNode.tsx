import {CSSProperties, memo, useCallback, useMemo} from "react";
import {Handle, NodeProps, Position} from "@xyflow/react";
import {useMyStore} from "@/store/store";
import {getNodeBackgroundColor} from "./layout/colors.ts";
import {getNodeWidth} from "./layout/dimensions.ts";
import {Button, Popover} from "antd";
import {ArrowsAltOutlined, ShrinkOutlined} from "@ant-design/icons";
import {EntityCard} from "./EntityCard.tsx";
import {EntityProperties} from "./EntityProperties.tsx";
import {EntityForm} from "./edit/EntityForm.tsx";
import {ResPopover} from "./ResPopover.tsx";
import {findFirstImage} from "./layout/calcWidthHeight.ts";
import {getReadableTextColor} from "./layout/colors.ts";
import {getResBrief} from "@/res/getResBrief";
import {EntityNode} from "./FlowGraph.tsx";
import {nodeAnchor} from "./nodeAnchor.ts";
import {useNodeNote} from "./NodeNote.tsx";
import {NodeTitle} from "./NodeTitle.tsx";

const iconButtonStyle = { borderWidth: 0, backgroundColor: 'transparent' };

const foldIcon = <ShrinkOutlined />;
const unfoldIcon = <ArrowsAltOutlined />;

export const FlowNode = memo(function FlowNode(nodeProps: NodeProps<EntityNode>) {
    const entity = nodeProps.data.entity;
    // nodeShow/notes 是呈现层配置，从 nodeProps.data 读——保留 FixedPage per-graph override；
    // query 无 per-graph override，走全局 store（resso per-key 订阅，仅 query 变时重渲）。
    const nodeShow = nodeProps.data.nodeShow;
    const notes = nodeProps.data.notes;
    const { query } = useMyStore();
    const { id, label, handleIn, handleOut } = entity;

    // F1：entity 本就是 ReadOnlyEntity|EditableEntity|CardEntity 判别联合，用 entity.type 三元取出互斥的
    // edit/fields/brief（真分支 TS 自动收窄），替代原 let+if 命令式级联。note/assets 在 EntityBase 上无需窄化。
    const note = entity.note;
    const assets = entity.assets;
    const edit = entity.type === 'editable' ? entity.edit : undefined;
    const fields = entity.type === 'readonly' ? entity.fields : undefined;
    const brief = entity.type === 'card' ? entity.brief : undefined;

    // nodeShow 进 color 的 useMemo deps：entity 引用不变时，改主题色仍要让 color 重算（避免 stale）。
    const color: string = useMemo(() => getNodeBackgroundColor(entity, nodeShow), [entity, nodeShow]);
    const width = getNodeWidth(entity, nodeShow);
    const nodeStyle: CSSProperties = useMemo(() => {
        return { width: width, backgroundColor: color, outlineColor: nodeShow?.editFoldColor };
    }, [width, color, nodeShow?.editFoldColor]);

    const unfoldIconButtonStyle = useMemo(() => {
        return { borderWidth: 0, backgroundColor: nodeShow?.editFoldColor ?? '#ffd6e7' };
    }, [nodeShow?.editFoldColor]);

    const unfoldNode = useCallback(() => {
        edit?.editOnUpdateFold?.(false, nodeAnchor(nodeProps));
    }, [edit, nodeProps]);
    const foldNode = useCallback(() => {
        edit?.editOnUpdateFold?.(true, nodeAnchor(nodeProps));
    }, [edit, nodeProps]);

    // 首图与资源摘要按钮各自独立 memo（原为一个 useMemo 返回 [btn, firstImage] 元组，两个不相关产物混算）。
    const firstImage = useMemo(() => findFirstImage(assets), [assets]);
    const resBriefButton = useMemo(() => {
        if (!assets) return undefined;
        // 资源摘要按钮文字按节点底色自动反色（原硬编码 #fff 在浅底色上会糊掉）。
        return <Popover content={<ResPopover resInfos={assets} />}
            placement='rightTop'
            trigger='click'>
            <Button type='text' style={{color: getReadableTextColor(color)}}>{getResBrief(assets)}</Button>
        </Popover>;
    }, [assets, color]);

    const handleStyle: CSSProperties = useMemo(() => {
        return { position: 'absolute', backgroundColor: color };
    }, [color]);

    const foldButton = useMemo(() => {
        // 显示 fold 按钮的条件：有子节点，或可以被内嵌（从内嵌展开的节点）。
        if (edit && (edit.hasChild || edit.canBeEmbedded)) {
            if (edit.fold) {
                return <Button style={unfoldIconButtonStyle} icon={unfoldIcon} onClick={unfoldNode} />;
            } else {
                return <Button style={iconButtonStyle} icon={foldIcon} onClick={foldNode} />;
            }
        }
        return null;
    }, [edit, unfoldIconButtonStyle, unfoldNode, foldNode]);

    // note 双模式（触发按钮 + 内容区）收口到 useNodeNote；editNoteButton 渲染于 title，noteBlock 渲染于顶部。
    const {noteBlock, editNoteButton} = useNodeNote({id, entity, edit, note, notes, label});

    return <div key={id} className={edit && edit.fold ? 'flowNodeWithBorder' : 'flowNode'} style={nodeStyle}>
        {/* HeightDriftGuard 已停用并归档（见 ./__dev__/HeightDriftGuard.tsx） */}
        {noteBlock}
        <NodeTitle
            foldButton={foldButton}
            label={label}
            query={query}
            copyable={!!brief && !!nodeShow?.refIsShowCopyable}
            editNoteButton={editNoteButton}
            resBriefButton={resBriefButton}
            edit={edit}
            nodeProps={nodeProps}
            nodeBgColor={color}
        />
        {fields && <EntityProperties fields={fields} nodeShow={nodeShow} color={color} />}
        {brief && <EntityCard entity={entity} image={firstImage} nodeShow={nodeShow} />}
        {edit && <EntityForm edit={edit} nodeProps={nodeProps} nodeShow={nodeShow} />}
        {(handleIn && <Handle type='target' position={Position.Left} id='@in' style={handleStyle} />)}
        {(handleOut && <Handle type='source' position={Position.Right} id='@out' style={handleStyle} />)}
    </div>;
});
