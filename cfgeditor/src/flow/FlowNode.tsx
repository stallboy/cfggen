import {CSSProperties, memo, useCallback, useMemo, useState} from "react";
import {Handle, Node, NodeProps, Position} from "@xyflow/react";
import {Entity} from "./entityModel.ts";
import {getNodeBackgroundColor} from "./colors.ts";
import {Button, Card, Flex, Popover, Space, Typography} from "antd";
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
const redIconButtonStyle = {borderWidth: 0, backgroundColor: 'cyan'};
const titleStyle = {width: '100%'};
const titleTextStyle = {fontSize: 14, color: "#fff"};
const closeIcon = <CloseOutlined/>;
const moveUpIcon = <ArrowUpOutlined/>;
const moveDownIcon = <ArrowDownOutlined/>;

const foldIcon = <ShrinkOutlined/>;
const unfoldIcon = <ArrowsAltOutlined/>;

const resBriefButtonStyle = {color: '#fff'};

export const FlowNode = memo(function FlowNode(nodeProps: NodeProps<Node<{ entity: Entity }, "node">>) {
    const entity = nodeProps.data.entity
    const {id, label, fields, edit, brief, handleIn, handleOut, note, sharedSetting, assets} = entity;
    const color: string = getNodeBackgroundColor(entity);
    const width = edit ? 280 : 240;
    const nodeStyle = useMemo(() => {
        return {width: width, backgroundColor: color}
    }, [width, color]);

    const [isEditNote, setIsEditNote] = useState<boolean>(false);
    const onEditNote = useCallback(() => {
        setIsEditNote(true);
    }, [setIsEditNote]);

    // 不能稍微改一下note，就刷新整个树，所以note要缓存下来作为状态
    const [tmpNote, setTmpNote] = useState<string | undefined>();
    // 因为list.item的id为index，当删除一项有note的节点后，后面的节点id跟此相同，所以就继承了前面的tmpNote
    // 所以记录prevEntity，如果entity更改，则重置tmpNote为undefined，
    const [prevEntity, setPrevEntity] = useState(entity);
    const onEditNoteInEdit = useCallback(() => {
        setTmpNote("note：");
    }, [setTmpNote]);

    const updateNoteInEdit = useCallback((v: string) => {
        if (edit && edit.editOnUpdateNote) {
            edit.editOnUpdateNote(v);
            setTmpNote(v);
        }
    }, [edit, setTmpNote]);

    const unfoldNode = useCallback(() => {
        if (edit && edit.editOnUpdateFold) {
            edit.editOnUpdateFold(false);
        }
    }, [edit]);

    const foldNode = useCallback(() => {
        if (edit && edit.editOnUpdateFold) {
            edit.editOnUpdateFold(true);
        }
    }, [edit]);

    const [resBriefButton, firstImage] = useMemo(() => {
        let btn;
        const firstImage = findFirstImage(assets);
        if (assets) {
            btn = <Popover content={<ResPopover resInfos={assets}/>}
                           placement='rightTop'
                           trigger='click'>
                <Button type='text' style={resBriefButtonStyle}>{getResBrief(assets)}</Button>
            </Popover>
        }
        return [btn, firstImage];
    }, [label, assets]);


    const handleStyle: CSSProperties = useMemo(() => {
        return {position: 'absolute', backgroundColor: color}
    }, [color]);


    // 如果entity更改，则重置tmpNote为undefined，让其重新渲染
    if (entity !== prevEntity) {
        // console.log("not equal", entity.id, prevEntity.id, "--", note);
        setPrevEntity(entity);
        setTmpNote(undefined);
        return <></>
    }

    let foldButton;
    let fold = false;
    if (edit && edit.hasChild) {
        if (edit.fold) {
            foldButton = <Button style={redIconButtonStyle} icon={unfoldIcon}
                                 onClick={unfoldNode}/>;
            fold = true;
        } else {
            foldButton = <Button style={iconButtonStyle} icon={foldIcon}
                                 onClick={foldNode}/>;
        }
    }

    // 用‘label是否包含空格’做为它是table_id格式的标志
    const mayHasResOrNote = label.includes('_');
    let editNoteButton;
    let noteShowOrEdit;
    if (mayHasResOrNote) {
        const notes = sharedSetting?.notes;
        const recordNote = notes?.get(id) ?? '';
        if ((recordNote.length > 0) || isEditNote) {
            if (isEditNote) {
                noteShowOrEdit = <NoteEdit id={id} note={recordNote} setIsEdit={setIsEditNote}/>
            } else {
                noteShowOrEdit = <NoteShow note={recordNote} setIsEdit={setIsEditNote}/>;
            }
        } else if (note) {
            noteShowOrEdit = <NoteShowInner note={note}/>;
        } else {
            editNoteButton = <Button style={iconButtonStyle} icon={bookIcon}
                                     onClick={onEditNote}/>;
        }
    } else {
        if (edit) {
            let showNote;
            if (tmpNote != undefined) { // 表明有设置过
                if (tmpNote.length > 0) {
                    showNote = tmpNote;
                }
            } else if (note && note.length > 0) {
                showNote = note;
            }
            if (showNote) {
                noteShowOrEdit = <NoteEditInner note={showNote}
                                                updateNoteInEdit={updateNoteInEdit}/>
            } else {
                editNoteButton = <Button style={iconButtonStyle} icon={bookIcon}
                                         onClick={onEditNoteInEdit}/>;
            }

        } else if (note) {
            noteShowOrEdit = <NoteShowInner note={note}/>;
        }
    }
    const copyable = mayHasResOrNote && sharedSetting?.nodeShow?.showHead == 'showCopyable'
    const keyword = sharedSetting?.query

    const title = <Flex justify="space-between" style={titleStyle}>
        {foldButton}

        <Text strong style={titleTextStyle} ellipsis={false}
              copyable={copyable}>
            {keyword ? <Highlight text={label} keyword={keyword}/> : label}
        </Text>

        {editNoteButton}
        {resBriefButton}
        <Space size={1}>
            {edit && edit.editOnMoveUp &&
                <Button className='nodrag' style={iconButtonStyle} icon={moveUpIcon} onClick={edit.editOnMoveUp}/>}
            {edit && edit.editOnMoveDown &&
                <Button className='nodrag' style={iconButtonStyle} icon={moveDownIcon} onClick={edit.editOnMoveDown}/>}
            {edit && edit.editOnDelete &&
                <Button className='nodrag' style={iconButtonStyle} icon={closeIcon} onClick={edit.editOnDelete}/>}
        </Space>
    </Flex>


    const node = <Flex key={id} vertical gap='small' className='flowNode' style={nodeStyle}>
        {noteShowOrEdit}
        {title}

        {fields && <EntityProperties fields={fields} sharedSetting={sharedSetting} color={color}/>}
        {brief && <EntityCard entity={nodeProps.data.entity} image={firstImage}/>}
        {edit && <EntityForm edit={edit} sharedSetting={sharedSetting}/>}

        {(handleIn && <Handle type='target' position={Position.Left} id='@in'
                              style={handleStyle}/>)}
        {(handleOut && <Handle type='source' position={Position.Right} id='@out'
                               style={handleStyle}/>)}
    </Flex>;

    if (fold) {
        return <Card key={id} size='small' style={{backgroundColor: 'cyan'}}> {node}</Card>
    } else {
        return node;
    }
});
