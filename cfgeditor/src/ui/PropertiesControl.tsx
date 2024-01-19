import {Handle, Position} from "reactflow";
import {EntityField} from "../model/entityModel.ts";
import {Flex, List, Tooltip, Typography} from "antd";

const {Text} = Typography;

function tooltip({comment, name}: { name: string, comment?: string }) {
    return comment ? `${name}: ${comment}` : name;
}

const re = /[（(]/;

function text({comment, name}: { name: string, comment?: string }) {
    if (comment) {
        const c = comment.split(re)[0];

        return `${c.substring(0, 6)} ${name}`
    }
    return name;
}

export function PropertiesControl({fields, color}: {
    fields: EntityField[],
    color: string,
}) {
    if (fields.length == 0) {
        return <></>;
    }
    return <List size='small' style={{backgroundColor: '#fff'}} bordered dataSource={fields}
                 renderItem={(item) => {
                     return <List.Item key={item.key} style={{position: 'relative'}}>
                         <Flex justify="space-between" style={{width: '100%'}}>
                             <Tooltip title={tooltip(item)}>
                                 <Text style={{color: color, maxWidth: '80%'}} ellipsis={{tooltip: true}}>
                                     {text(item)}
                                 </Text>
                             </Tooltip>
                             <Text style={{maxWidth: '70%'}} ellipsis={{tooltip: true}}>
                                 {item.value}
                             </Text>
                         </Flex>

                         {item.handleIn && <Handle type='target' position={Position.Left} id={`@in_${item.name}`}
                                                   style={{
                                                       position: 'absolute',
                                                       left: '-10px',
                                                       backgroundColor: color
                                                   }}/>}
                         {item.handleOut && <Handle type='source' position={Position.Right} id={item.name}
                                                    style={{
                                                        position: 'absolute',
                                                        left: '230px',
                                                        backgroundColor: color
                                                    }}/>}
                     </List.Item>;

                 }}/>;
}
