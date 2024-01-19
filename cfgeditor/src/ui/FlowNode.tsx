import {memo} from "react";
import {Handle, NodeProps, Position} from "reactflow";
import {Entity} from "../model/entityModel.ts";
import {getNodeBackgroundColor} from "./colors.ts";
import {Flex, Typography} from "antd";
import {EntityCard} from "./EntityCard.tsx";
import {EntityProperties} from "./EntityProperties.tsx";
import {EntityForm} from "./EntityForm.tsx";

const {Text} = Typography;

export const FlowNode = memo(function (nodeProps: NodeProps<Entity>) {
    const {fields, brief, edit, handleIn, handleOut, label} = nodeProps.data;
    const color: string = getNodeBackgroundColor(nodeProps.data);
    const width = edit ? 360 : 240;


    return <Flex vertical gap={'small'} className='flowNode' style={{width: width, backgroundColor: color}}>
        <Text strong style={{fontSize: 18, color: "#fff"}} ellipsis={{tooltip: true}}>
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
