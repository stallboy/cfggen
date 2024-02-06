import {memo} from "react";
import {Handle, NodeProps, Position} from "@xyflow/react";
import {Entity} from "./entityModel.ts";
import {getNodeBackgroundColor} from "./colors.ts";
import {Flex, Popover, Typography} from "antd";
import {EntityCard} from "./EntityCard.tsx";
import {EntityProperties} from "./EntityProperties.tsx";
import {EntityForm} from "./EntityForm.tsx";
import {ActionIcon} from "@ant-design/pro-editor";
import {CloseOutlined} from "@ant-design/icons";
import {store} from "../routes/setting/store.ts";
import {getResBrief, ResPopover} from "./ResPopover.tsx";

const {Text} = Typography;


export const FlowNode = memo(function FlowNode(nodeProps: NodeProps<Entity>) {
    const {fields, brief, edit, handleIn, handleOut, id, label, nodeShow} = nodeProps.data;
    const {resMap} = store;
    const color: string = getNodeBackgroundColor(nodeProps.data);
    const width = edit ? 280 : 240;
    const copy: any = {}
    if (nodeShow?.showHead == 'showCopyable') {
        copy.copyable = true;
    }

    let resBrief;
    let resContent;
    if (label.includes('_')) {
        const res = resMap.get(label);
        if (res) {
            resContent = <ResPopover resInfos={res}/>;
            resBrief = <Text style={{fontSize: 18, color: '#fff'}}>{getResBrief(res)}</Text>;
        }
    }

    let title = <Flex justify="space-between" style={{width: '100%'}}>
        <Text strong style={{fontSize: 18, color: "#fff"}} {...copy}>
            {label}
        </Text>
        {resBrief}
        {edit && edit.editOnDelete &&
            <ActionIcon className='nodrag' icon={<CloseOutlined/>} onClick={edit.editOnDelete}/>}
    </Flex>

    if (resContent) {
        title = <Popover content={resContent}
                         placement='bottomLeft'
                         trigger='click'>
            {title}
        </Popover>
    }

    return <Flex key={id} vertical gap={'small'} className='flowNode' style={{width: width, backgroundColor: color}}>
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
