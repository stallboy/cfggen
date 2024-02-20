import {memo, useState} from "react";
import {Handle, NodeProps, Position} from "@xyflow/react";
import {Entity} from "./entityModel.ts";
import {getNodeBackgroundColor} from "./colors.ts";
import {Button, Flex, Popover, Typography} from "antd";
import {EntityCard} from "./EntityCard.tsx";
import {EntityProperties} from "./EntityProperties.tsx";
import {EntityForm} from "./EntityForm.tsx";
import {ActionIcon} from "@ant-design/pro-editor";
import {BookOutlined, CloseOutlined} from "@ant-design/icons";
import {store} from "../routes/setting/store.ts";
import {getResBrief, ResPopover} from "./ResPopover.tsx";
import {NoteShow, NoteEdit} from "./NoteShowOrEdit.tsx";

const {Text} = Typography;


export const FlowNode = memo(function FlowNode(nodeProps: NodeProps<Entity>) {
    const {resMap} = store;
    const [isEditNote, setIsEditNote] = useState<boolean>(false);
    const {fields, brief, edit, handleIn, handleOut, id, label, sharedSetting} = nodeProps.data;
    const color: string = getNodeBackgroundColor(nodeProps.data);
    const width = edit ? 280 : 240;
    const copy: any = {}
    if (sharedSetting?.nodeShow?.showHead == 'showCopyable') {
        copy.copyable = true;
    }

    // 用label包含做为它是table_id格式的标志
    const mayHasResOrNote = label.includes('_');
    let editNoteButton;
    let noteShowOrEdit;
    if (mayHasResOrNote) {
        const notes = sharedSetting?.notes;
        let note = notes?.get(id) ?? '';

        if ((note.length > 0) || isEditNote) {
            if (isEditNote){
                noteShowOrEdit = <NoteEdit id={id} note={note} setIsEdit={setIsEditNote}/>
            }else{
                noteShowOrEdit = <NoteShow note={note} setIsEdit={setIsEditNote}/>;
            }
        } else {
            function onEditNote() {
                setIsEditNote(true);
            }
            editNoteButton = <ActionIcon icon={<BookOutlined/>} onClick={onEditNote}/>;
        }
    }

    let resBriefButton;
    if (mayHasResOrNote) {
        const res = resMap.get(label);
        if (res) {
            resBriefButton = <Popover content={<ResPopover resInfos={res}/>}
                                      placement='rightTop'
                                      trigger='click'>
                <Button type={'text'} style={{color: '#fff'}}>{getResBrief(res)}</Button>
            </Popover>
        }
    }

    let title = <Flex justify="space-between" style={{width: '100%'}}>
        <Text strong style={{fontSize: 18, color: "#fff"}} {...copy}>
            {label}
        </Text>
        {editNoteButton}
        {resBriefButton}
        {edit && edit.editOnDelete &&
            <ActionIcon className='nodrag' icon={<CloseOutlined/>} onClick={edit.editOnDelete}/>}
    </Flex>


    return <Flex key={id} vertical gap={'small'} className='flowNode' style={{width: width, backgroundColor: color}}>
        {noteShowOrEdit}
        {title}

        {fields && <EntityProperties fields={fields} color={color}/>}
        {brief && <EntityCard entity={nodeProps.data}/>}
        {edit && <EntityForm edit={edit}/>}

        {(handleIn && <Handle type='target' position={Position.Left} id='@in'
                              style={{
                                  position: 'absolute',
                                  backgroundColor: color
                              }}/>)}
        {(handleOut && <Handle type='source' position={Position.Right} id='@out'
                               style={{
                                   position: 'absolute',
                                   backgroundColor: color
                               }}/>)}
    </Flex>;
});
