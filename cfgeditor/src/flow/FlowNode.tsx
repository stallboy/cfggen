import { CSSProperties, memo, useCallback, useMemo, useState } from "react";
import { Handle, NodeProps, Position } from "@xyflow/react";
import { Entity, isReadOnlyEntity, isEditableEntity, isCardEntity } from "./entityModel.ts";
import { getNodeBackgroundColor } from "./colors.ts";
import { Button, Flex, Popover, Space, Typography } from "antd";
import { EntityCard, Highlight } from "./EntityCard.tsx";
import { EntityProperties } from "./EntityProperties.tsx";
import { EntityForm } from "./EntityForm.tsx";
import {
    ArrowDownOutlined,
    ArrowsAltOutlined,
    ArrowUpOutlined,
    BookOutlined,
    CloseOutlined,
    ShrinkOutlined
} from "@ant-design/icons";
import { ResPopover } from "./ResPopover.tsx";
import { NoteShow, NoteEdit, NoteShowInner, NoteEditInner } from "./NoteShowOrEdit.tsx";
import { findFirstImage } from "./calcWidthHeight.ts";
import { getResBrief } from "./getResBrief.tsx";
import { EntityNode } from "./FlowGraph.tsx";

const { Text } = Typography;
const bookIcon = <BookOutlined />;
const iconButtonStyle = { borderWidth: 0, backgroundColor: 'transparent' };

const titleStyle = { width: '100%' };
const titleTextStyle = { fontSize: 14, color: "#fff" };
const closeIcon = <CloseOutlined />;
const moveUpIcon = <ArrowUpOutlined />;
const moveDownIcon = <ArrowDownOutlined />;

const foldIcon = <ShrinkOutlined />;
const unfoldIcon = <ArrowsAltOutlined />;

const resBriefButtonStyle = { color: '#fff' };

interface TempNote {
    note: string;
    entity: Entity;
}

export const FlowNode = memo(function FlowNode(nodeProps: NodeProps<EntityNode>) {
    const entity = nodeProps.data.entity;
    const { id, label, handleIn, handleOut, sharedSetting } = entity;

    // 使用类型守卫确定 Entity 类型并获取相应属性
    let fields = undefined;
    let edit = undefined;
    let brief = undefined;
    let note = undefined;
    let assets = undefined;

    if (isReadOnlyEntity(entity)) {
        fields = entity.fields;
        note = entity.note;
        assets = entity.assets;
    } else if (isEditableEntity(entity)) {
        edit = entity.edit;
        note = entity.note;
    } else if (isCardEntity(entity)) {
        brief = entity.brief;
        note = entity.note;
        assets = entity.assets;
    }

    const color: string = useMemo(() => getNodeBackgroundColor(entity), [entity]);
    const width = edit ? (entity.sharedSetting?.nodeShow?.editNodeWidth ?? 280) : (entity.sharedSetting?.nodeShow?.nodeWidth ?? 240);
    const nodeStyle: CSSProperties = useMemo(() => {
        return { width: width, backgroundColor: color, outlineColor: entity.sharedSetting?.nodeShow?.editFoldColor };
    }, [width, color, entity.sharedSetting?.nodeShow?.editFoldColor]);

    const unfoldIconButtonStyle = useMemo(() => {
        return { borderWidth: 0, backgroundColor: entity.sharedSetting?.nodeShow?.editFoldColor ?? '#ffd6e7' };
    }, [entity.sharedSetting?.nodeShow?.editFoldColor]);

    const [isEditNote, setIsEditNote] = useState<boolean>(false);
    const onEditNote = useCallback(() => {
        setIsEditNote(true);
    }, [setIsEditNote]);

    // 当设置note时，会存$note到jsonobject里,会存tmpNote在这里，不会直接网络请求去update json
    // 同时记录下tmpNote时的entity，因为切换不同record时，可能key相同，此时note不能使用
    const [tmpNote, setTmpNote] = useState<TempNote | undefined>();

    const onEditNoteClickInEdit = useCallback(() => {
        setTmpNote({ note: "note：", entity });
    }, [setTmpNote, entity]);

    const updateNoteInEdit = useCallback((note: string) => {
        if (edit && edit.editOnUpdateNote) {
            edit.editOnUpdateNote(note);
            setTmpNote({ note, entity });
        }
    }, [edit, setTmpNote, entity]); const unfoldNode = useCallback(() => {
        if (edit && edit.editOnUpdateFold) {
            edit.editOnUpdateFold(false, { id, x: nodeProps.positionAbsoluteX, y: nodeProps.positionAbsoluteY });
        }
    }, [edit, id, nodeProps.positionAbsoluteX, nodeProps.positionAbsoluteY]); const foldNode = useCallback(() => {
        if (edit && edit.editOnUpdateFold) {
            edit.editOnUpdateFold(true, { id, x: nodeProps.positionAbsoluteX, y: nodeProps.positionAbsoluteY });
        }
    }, [edit, id, nodeProps.positionAbsoluteX, nodeProps.positionAbsoluteY]); const [resBriefButton, firstImage] = useMemo(() => {
        let btn;
        const firstImage = findFirstImage(assets);
        if (assets) {
            btn = <Popover content={<ResPopover resInfos={assets} />}
                placement='rightTop'
                trigger='click'>
                <Button type='text' style={resBriefButtonStyle}>{getResBrief(assets)}</Button>
            </Popover>;
        }
        return [btn, firstImage];
    }, [assets]);

    const handleStyle: CSSProperties = useMemo(() => {
        return { position: 'absolute', backgroundColor: color };
    }, [color]);

    const foldButton = useMemo(() => {
        if (edit) {
            // 显示 fold 按钮的条件：
            // 1. 有子节点，或
            // 2. 可以被内嵌（从内嵌展开的节点）
            if (edit.hasChild || edit.canBeEmbedded) {
                if (edit.fold) {
                    return <Button style={unfoldIconButtonStyle} icon={unfoldIcon} onClick={unfoldNode} />;
                } else {
                    return <Button style={iconButtonStyle} icon={foldIcon} onClick={foldNode} />;
                }
            }
        }

        return null;
    }, [edit, unfoldIconButtonStyle, unfoldNode, foldNode]);
    const editNoteButton = useMemo(() => {
        const mayHasResOrNote = label.includes('_');
        if (mayHasResOrNote && !edit) {
            const recordNote = sharedSetting?.notes?.get(id) ?? '';
            if (!((recordNote.length > 0) || isEditNote) && !note) {
                return <Button style={iconButtonStyle} icon={bookIcon} onClick={onEditNote} />;
            }
        }
        const hasExistingNote =
            (tmpNote && tmpNote.entity === entity && tmpNote.note.length > 0) ||
            (note && note.length > 0);

        if (edit && !hasExistingNote) {
            return <Button style={iconButtonStyle} icon={bookIcon} onClick={onEditNoteClickInEdit} />;
        }

        return null;
    }, [sharedSetting, id, isEditNote, note, edit, tmpNote, entity, label, onEditNote, onEditNoteClickInEdit]);

    const noteShowOrEdit = useMemo(() => {
        const mayHasResOrNote = label.includes('_');
        if (mayHasResOrNote) {
            const notes = sharedSetting?.notes;
            const recordNote = notes?.get(id) ?? '';
            if ((recordNote.length > 0) || isEditNote) {
                if (isEditNote) {
                    return <NoteEdit id={id} note={recordNote} setIsEdit={setIsEditNote} />;
                } else {
                    return <NoteShow note={recordNote} setIsEdit={setIsEditNote} />;
                }
            } else if (note) {
                return <NoteShowInner note={note} />;
            }
        }

        if (edit) {
            let showNote;
            if (tmpNote && tmpNote.entity === entity) { // 有设置过就用它了
                if (tmpNote.note.length > 0) {
                    showNote = tmpNote.note;
                } // else {} 这样允许设置为空后不显示，虽然此时json里有note
            } else if (note && note.length > 0) {
                showNote = note;
            }
            if (showNote) {
                return <NoteEditInner note={showNote} updateNoteInEdit={updateNoteInEdit} />;
            }
        } else if (note) {
            return <NoteShowInner note={note} />;
        }

        return null;
    }, [label, edit, note, sharedSetting?.notes, id, isEditNote, tmpNote, entity, updateNoteInEdit]);


    const title = useMemo(() => {
        return <Flex justify="space-between" style={titleStyle}>
            {foldButton}
            <Text strong style={titleTextStyle} ellipsis={false}
                copyable={brief && sharedSetting?.nodeShow?.refIsShowCopyable}>
                {sharedSetting?.query ? <Highlight text={label} keyword={sharedSetting.query} /> : label}
            </Text>
            {editNoteButton}
            {resBriefButton}
            <Space size={1}>
                {edit && edit.editOnMoveUp &&
                    <Button className='nodrag' style={iconButtonStyle} icon={moveUpIcon}
                        onClick={() => {
                            edit.editOnMoveUp?.({
                                id,
                                x: nodeProps.positionAbsoluteX,
                                y: nodeProps.positionAbsoluteY
                            })
                        }} />}
                {edit && edit.editOnMoveDown &&
                    <Button className='nodrag' style={iconButtonStyle} icon={moveDownIcon}
                        onClick={() => {
                            edit.editOnMoveDown?.({
                                id,
                                x: nodeProps.positionAbsoluteX,
                                y: nodeProps.positionAbsoluteY
                            })
                        }} />}
                {edit && edit.editOnDelete &&
                    <Button className='nodrag' style={iconButtonStyle} icon={closeIcon}
                        onClick={() => {
                            edit.editOnDelete?.({
                                id,
                                x: nodeProps.positionAbsoluteX,
                                y: nodeProps.positionAbsoluteY
                            })
                        }} />}
            </Space>
        </Flex>
    }, [foldButton, sharedSetting, label, brief, editNoteButton, resBriefButton, edit, id, nodeProps]);

    return <div key={id} className={edit && edit.fold ? 'flowNodeWithBorder' : 'flowNode'} style={nodeStyle}>
        {noteShowOrEdit}
        {title}
        {fields && <EntityProperties fields={fields} sharedSetting={sharedSetting} color={color} />}
        {brief && <EntityCard entity={nodeProps.data.entity} image={firstImage} />}
        {edit && <EntityForm edit={edit} nodeProps={nodeProps} sharedSetting={sharedSetting} />}
        {(handleIn && <Handle type='target' position={Position.Left} id='@in' style={handleStyle} />)}
        {(handleOut && <Handle type='source' position={Position.Right} id='@out' style={handleStyle} />)}
    </div>;
});
