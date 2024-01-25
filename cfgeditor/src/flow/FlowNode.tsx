import {memo} from "react";
import {Handle, NodeProps, Position} from "reactflow";
import {Entity} from "./entityModel.ts";
import {getNodeBackgroundColor} from "./colors.ts";
import {Flex, Typography} from "antd";
import {EntityCard} from "./EntityCard.tsx";
import {EntityProperties} from "./EntityProperties.tsx";
import {EntityForm} from "./EntityForm.tsx";

const {Text} = Typography;

export const FlowNode = memo(function FlowNode(nodeProps: NodeProps<Entity>) {
    const {fields, brief, edit, handleIn, handleOut, id, label, nodeShow} = nodeProps.data;
    const color: string = getNodeBackgroundColor(nodeProps.data);
    const width = edit ? 280 : 240;
    const copy: any = {}
    if (nodeShow?.showHead == 'showCopyable') {
        copy.copyable = true;
    }

    return <Flex key={id} vertical gap={'small'} className='flowNode' style={{width: width, backgroundColor: color}}>

        <Text strong style={{fontSize: 18, color: "#fff"}} {...copy}>
            {label}
        </Text>
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
