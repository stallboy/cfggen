import {memo} from "react";
import {Handle, NodeProps, Position} from "@xyflow/react";
import {Entity} from "./entityModel.ts";
import {getNodeBackgroundColor} from "./colors.ts";
import {Flex, Typography} from "antd";
import {EntityCard, getDsLenAndDesc} from "./EntityCard.tsx";
import {EntityProperties} from "./EntityProperties.tsx";
import {EntityForm} from "./EntityForm.tsx";
import {NodeShowType} from "../io/localStoreJson.ts";
import {ActionIcon} from "@ant-design/pro-editor";
import {CloseOutlined} from "@ant-design/icons";

const {Text} = Typography;

export const FlowNode = memo(function FlowNode(nodeProps: NodeProps<Entity>) {
    const {fields, brief, edit, handleIn, handleOut, id, label, nodeShow} = nodeProps.data;
    const color: string = getNodeBackgroundColor(nodeProps.data);
    const width = edit ? 280 : 240;
    const copy: any = {}
    if (nodeShow?.showHead == 'showCopyable') {
        copy.copyable = true;
    }
    // console.log('flownode', nodeProps)

    return <Flex key={id} vertical gap={'small'} className='flowNode' style={{width: width, backgroundColor: color}}>

        <Flex justify="space-between" style={{width: '100%'}}>
            <Text strong style={{fontSize: 18, color: "#fff"}} {...copy}>
                {label}
            </Text>
            {edit && edit.editOnDelete && <ActionIcon icon={<CloseOutlined/>} onClick={edit.editOnDelete}/>}
        </Flex>

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

// 在一次又一次尝试了等待node准备好，直接用node的computed理的width，height后，增加这一个异步，太容易有闪烁和被代码绕晕了。
// 放弃放弃，还是预先估算好。
export function calcWidthHeight(entity: Entity, nodeShow: NodeShowType) {
    const {fields, brief, edit} = entity;
    const width = edit ? 280 : 240;
    let height = 40;

    if (fields) {
        height += 41 * fields.length;

    } else if (brief) {
        height += 48 + (brief.title ? 32 : 0);
        let [showDsLen, desc] = getDsLenAndDesc(brief, nodeShow);
        height += showDsLen * 38;
        if (desc) {
            height += 22 * desc.length / 13;
        }

    } else if (edit) {
        let cnt = 0;
        let extra = 0;
        for (let editField of edit.editFields) {
            switch (editField.type) {
                case "arrayOfPrimitive":
                    cnt += (editField.value as any[]).length;
                    break;

                case "interface":
                    cnt++;
                    if (editField.implFields) {
                        cnt += editField.implFields.length;
                    }
                    break;
                case 'primitive':
                    if (editField.eleType == 'text' || editField.eleType == 'str') {
                        let row = (editField.value as string).length / 10;
                        if (row > 10) {
                            row = 10;
                        }
                        extra += row * 22 + 10;
                    } else {
                        cnt++;
                    }
                    break;
                default:
                    cnt++;
                    break;
            }
        }
        height += 20 + 40 * cnt + extra;
    }

    return [width, height];

}