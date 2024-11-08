import {CSSProperties, memo, useCallback, useMemo, useState} from "react";
import {Handle, Node, NodeProps, Position} from "@xyflow/react";
import {Entity} from "./entityModel.ts";
import {getNodeBackgroundColor} from "./colors.ts";
import {Button, Flex, Popover, Space, Typography} from "antd";
import {EntityCard, Highlight} from "./EntityCard.tsx";
import {EntityProperties} from "./EntityProperties.tsx";
import {EntityForm} from "./EntityForm.tsx";
import {
    ArrowDownOutlined,
    ArrowsAltOutlined,
    ArrowUpOutlined,
    BookOutlined,
    CloseOutlined,
    ShrinkOutlined
} from "@ant-design/icons";
import {ResPopover} from "./ResPopover.tsx";
import {NoteShow, NoteEdit, NoteShowInner, NoteEditInner} from "./NoteShowOrEdit.tsx";
import {findFirstImage} from "./calcWidthHeight.ts";
import {getResBrief} from "./getResBrief.tsx";

const {Text} = Typography;
const bookIcon = <BookOutlined/>;
const iconButtonStyle = {borderWidth: 0, backgroundColor: 'transparent'};
const redIconButtonStyle = {borderWidth: 0, backgroundColor: '#ffd6e7'};
const titleStyle = {width: '100%'};
const titleTextStyle = {fontSize: 14, color: "#fff"};
const closeIcon = <CloseOutlined/>;
const moveUpIcon = <ArrowUpOutlined/>;
const moveDownIcon = <ArrowDownOutlined/>;

const foldIcon = <ShrinkOutlined/>;
const unfoldIcon = <ArrowsAltOutlined/>;

const resBriefButtonStyle = {color: '#fff'};

interface TempNote {
    note: string;
    entity: Entity;
}

export const FlowNode = memo(function FlowNode(nodeProps: NodeProps<Node<{ entity: Entity }, "node">>) {
    const entity = nodeProps.data.entity;
    const {id, label, fields, edit, brief, handleIn, handleOut, note, sharedSetting, assets} = entity;
    const color: string = getNodeBackgroundColor(entity);
    const width = edit ? 280 : 240;
    const nodeStyle = useMemo(() => {
        return {width: width, backgroundColor: color};
    }, [width, color]);

    const [isEditNote, setIsEditNote] = useState<boolean>(false);
    const onEditNote = useCallback(() => {
        setIsEditNote(true);
    }, [setIsEditNote]);

    // 当设置note时，会存$note到jsonobject里,会存tmpNote在这里，不会直接网络请求去update json
    // 同时记录下tmpNote时的entity，因为切换不同record时，可能key相同，此时note不能使用
    const [tmpNote, setTmpNote] = useState<TempNote | undefined>();

    const onEditNoteClickInEdit = useCallback(() => {
        setTmpNote({note: "note：", entity});
    }, [setTmpNote, entity]);

    const updateNoteInEdit = useCallback((note: string) => {
        if (edit && edit.editOnUpdateNote) {
            edit.editOnUpdateNote(note);
            setTmpNote({note, entity});
        }
    }, [edit, setTmpNote, entity]);

    const unfoldNode = useCallback(() => {
        if (edit && edit.editOnUpdateFold) {
            edit.editOnUpdateFold(id, false);
        }
    }, [edit, id]);

    const foldNode = useCallback(() => {
        if (edit && edit.editOnUpdateFold) {
            edit.editOnUpdateFold(id, true);
        }
    }, [edit, id]);

    const [resBriefButton, firstImage] = useMemo(() => {
        let btn;
        const firstImage = findFirstImage(assets);
        if (assets) {
            btn = <Popover content={<ResPopover resInfos={assets}/>}
                           placement='rightTop'
                           trigger='click'>
                <Button type='text' style={resBriefButtonStyle}>{getResBrief(assets)}</Button>
            </Popover>;
        }
        return [btn, firstImage];
    }, [label, assets]);

    const handleStyle: CSSProperties = useMemo(() => {
        return {position: 'absolute', backgroundColor: color};
    }, [color]);

    const foldButton = useMemo(() => {
        if (edit && edit.hasChild) {
            if (edit.fold) {
                return <Button style={redIconButtonStyle} icon={unfoldIcon} onClick={unfoldNode}/>;
            } else {
                return <Button style={iconButtonStyle} icon={foldIcon} onClick={foldNode}/>;
            }
        }
        return null;
    }, [edit, unfoldNode, foldNode]);


    const editNoteButton = useMemo(() => {
        const mayHasResOrNote = label.includes('_');
        if (mayHasResOrNote) {
            const notes = sharedSetting?.notes;
            const recordNote = notes?.get(id) ?? '';
            if (!((recordNote.length > 0) || isEditNote) && !note) {
                return <Button style={iconButtonStyle} icon={bookIcon} onClick={onEditNote}/>;
            }
        }
        if (edit) {
            let isShowNote = false
            if (tmpNote && tmpNote.entity === entity) {
                if (tmpNote.note.length > 0) {
                    isShowNote = true;
                }
            } else if (note && note.length > 0) {
                isShowNote = true;
            }

            if (!isShowNote) {
                return <Button style={iconButtonStyle} icon={bookIcon} onClick={onEditNoteClickInEdit}/>;
            }
        }
    }, [sharedSetting, id, isEditNote, note, edit, tmpNote, onEditNote, onEditNoteClickInEdit]);

    const noteShowOrEdit = useMemo(() => {
        const mayHasResOrNote = label.includes('_');
        if (mayHasResOrNote) {
            const notes = sharedSetting?.notes;
            const recordNote = notes?.get(id) ?? '';
            if ((recordNote.length > 0) || isEditNote) {
                if (isEditNote) {
                    return <NoteEdit id={id} note={recordNote} setIsEdit={setIsEditNote}/>;
                } else {
                    return <NoteShow note={recordNote} setIsEdit={setIsEditNote}/>;
                }
            } else if (note) {
                return <NoteShowInner note={note}/>;
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
                return <NoteEditInner note={showNote} updateNoteInEdit={updateNoteInEdit}/>;
            }
        } else if (note) {
            return <NoteShowInner note={note}/>;
        }

        return null;
    }, [sharedSetting, id, isEditNote, note, edit, tmpNote, updateNoteInEdit]);

    const title = useMemo(() => {
        const mayHasResOrNote = label.includes('_');
        return <Flex justify="space-between" style={titleStyle}>
            {foldButton}
            <Text strong style={titleTextStyle} ellipsis={false}
                  copyable={mayHasResOrNote && sharedSetting?.nodeShow?.showHead === 'showCopyable'}>
                {sharedSetting?.query ? <Highlight text={label} keyword={sharedSetting.query}/> : label}
            </Text>
            {editNoteButton}
            {resBriefButton}
            <Space size={1}>
                {edit && edit.editOnMoveUp &&
                    <Button className='nodrag' style={iconButtonStyle} icon={moveUpIcon} onClick={edit.editOnMoveUp}/>}
                {edit && edit.editOnMoveDown && <Button className='nodrag' style={iconButtonStyle} icon={moveDownIcon}
                                                        onClick={edit.editOnMoveDown}/>}
                {edit && edit.editOnDelete &&
                    <Button className='nodrag' style={iconButtonStyle} icon={closeIcon} onClick={edit.editOnDelete}/>}
            </Space>
        </Flex>
    }, [foldButton, sharedSetting, label, editNoteButton, resBriefButton, edit]);

    return <div key={id} className={edit && edit.fold ? 'flowNodeWithBorder' : 'flowNode'} style={nodeStyle}>
        {noteShowOrEdit}
        {title}
        {fields && <EntityProperties fields={fields} sharedSetting={sharedSetting} color={color}/>}
        {brief && <EntityCard entity={nodeProps.data.entity} image={firstImage}/>}
        {edit && <EntityForm edit={edit} sharedSetting={sharedSetting}/>}
        {(handleIn && <Handle type='target' position={Position.Left} id='@in' style={handleStyle}/>)}
        {(handleOut && <Handle type='source' position={Position.Right} id='@out' style={handleStyle}/>)}
    </div>;
});
