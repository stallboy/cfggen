import {memo} from "react";
import {Handle, NodeProps, Position} from "reactflow";
import {Entity} from "../model/entityModel.ts";
import {getNodeBackgroundColor} from "./colors.ts";
import {Flex, Typography} from "antd";
import {BriefControl} from "./BriefControl.tsx";
import {PropertiesControl} from "./PropertiesControl.tsx";

const {Text} = Typography;

export const FlowNode = memo(function (nodeProps: NodeProps<Entity>) {
    const {fields, brief, handleIn, handleOut, label} = nodeProps.data;

    const color: string = getNodeBackgroundColor(nodeProps.data);


    return <Flex vertical gap={'small'} className='propertiesNode' style={{width: 240, backgroundColor: color}}>
        <Text strong style={{fontSize: 18, color: "#fff"}} ellipsis={{tooltip: true}}>
            {label}
        </Text>
        {fields && <PropertiesControl fields={fields} color={color}/>}
        {brief && <BriefControl entity={nodeProps.data}/>}

        {(handleIn && <Handle type='target' position={Position.Left} id='@in'
                              style={{
                                  position: 'absolute',
                                  backgroundColor: color
                              }}/>)}
        {(handleOut && <Handle type='source' position={Position.Right} id='@out'
                               style={{
                                   position: 'absolute',
                                   backgroundColor: color
                               }}/>
        )
        }
    </Flex>
        ;

});
