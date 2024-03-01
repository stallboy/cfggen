import {CSSProperties, memo, useCallback, useMemo, useState} from "react";
import {Handle, NodeProps, Position} from "@xyflow/react";
import {Entity} from "./entityModel.ts";
import {getNodeBackgroundColor} from "./colors.ts";
import {Button, Flex, Popover, Typography} from "antd";
import {EntityCard} from "./EntityCard.tsx";
import {EntityProperties} from "./EntityProperties.tsx";
import {EntityForm} from "./EntityForm.tsx";
import {BookOutlined, CloseOutlined} from "@ant-design/icons";
import {getResBrief, ResPopover} from "./ResPopover.tsx";
import {NoteShow, NoteEdit} from "./NoteShowOrEdit.tsx";
import {findFirstImage} from "./calcWidthHeight.ts";

const {Text} = Typography;
const bookIcon = <BookOutlined/>;
const iconButtonStyle = {borderWidth: 0, backgroundColor: 'transparent'};
const titleStyle = {width: '100%'};
const titleTextStyle = {fontSize: 18, color: "#fff"};
const closeIcon = <CloseOutlined/>;
const resBriefButtonStyle = {color: '#fff'};

export const FlowNode = memo(function FlowNode(nodeProps: NodeProps<Entity>) {
    const [isEditNote, setIsEditNote] = useState<boolean>(false);
    const {fields, brief, edit, handleIn, handleOut, id, label, sharedSetting, assets} = nodeProps.data;
    const color: string = getNodeBackgroundColor(nodeProps.data);
    const width = edit ? 280 : 240;
    const nodeStyle = useMemo(() => {
        return {width: width, backgroundColor: color}
    }, [width, color]);

    const onEditNote = useCallback(() => {
        setIsEditNote(true);
    }, [setIsEditNote]);

    // 用label包含做为它是table_id格式的标志
    const mayHasResOrNote = label.includes('_');
    let editNoteButton;
    let noteShowOrEdit;
    if (mayHasResOrNote) {
        const notes = sharedSetting?.notes;
        let note = notes?.get(id) ?? '';
        if ((note.length > 0) || isEditNote) {
            if (isEditNote) {
                noteShowOrEdit = <NoteEdit id={id} note={note} setIsEdit={setIsEditNote}/>
            } else {
                noteShowOrEdit = <NoteShow note={note} setIsEdit={setIsEditNote}/>;
            }
        } else {
            editNoteButton = <Button style={iconButtonStyle} icon={bookIcon}
                                     onClick={onEditNote}/>;
        }
    }

    const [resBriefButton, firstImage] = useMemo(() => {
        let btn;
        let firstImage = findFirstImage(assets);
        if (assets) {
            btn = <Popover content={<ResPopover resInfos={assets}/>}
                           placement='rightTop'
                           trigger='click'>
                <Button type='text' style={resBriefButtonStyle}>{getResBrief(assets)}</Button>
            </Popover>
        }
        return [btn, firstImage];
    }, [label, assets]);


    let title = <Flex justify="space-between" style={titleStyle}>
        <Text strong style={titleTextStyle} ellipsis={false}
              copyable={sharedSetting?.nodeShow?.showHead == 'showCopyable'}>
            {label}
        </Text>
        {editNoteButton}
        {resBriefButton}
        {edit && edit.editOnDelete &&
            <Button className='nodrag' style={iconButtonStyle} icon={closeIcon} onClick={edit.editOnDelete}/>}
    </Flex>

    const handleStyle: CSSProperties = useMemo(() => {
        return {position: 'absolute', backgroundColor: color}
    }, [color]);

    return <Flex key={id} vertical gap='small' className='flowNode' style={nodeStyle}>
        {noteShowOrEdit}
        {title}

        {fields && <EntityProperties fields={fields} color={color}/>}
        {brief && <EntityCard entity={nodeProps.data} image={firstImage}/>}
        {edit && <EntityForm edit={edit}/>}

        {(handleIn && <Handle type='target' position={Position.Left} id='@in'
                              style={handleStyle}/>)}
        {(handleOut && <Handle type='source' position={Position.Right} id='@out'
                               style={handleStyle}/>)}
    </Flex>;
});
