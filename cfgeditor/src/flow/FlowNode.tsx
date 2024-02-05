import {memo} from "react";
import {Handle, NodeProps, Position} from "@xyflow/react";
import {Entity} from "./entityModel.ts";
import {getNodeBackgroundColor} from "./colors.ts";
import {Flex, Popover, Tabs, TabsProps, Typography} from "antd";
import {EntityCard, getDsLenAndDesc} from "./EntityCard.tsx";
import {EntityProperties} from "./EntityProperties.tsx";
import {EntityForm} from "./EntityForm.tsx";
import {NodeShowType} from "../routes/setting/storageJson.ts";
import {ActionIcon} from "@ant-design/pro-editor";
import {CloseOutlined} from "@ant-design/icons";
import {store} from "../routes/setting/store.ts";
import {convertFileSrc} from "@tauri-apps/api/tauri";

const {Text} = Typography;

function isVideo(r: string) {
    return r.endsWith('.mp4');
}

function isAudio(r: string) {
    return r.endsWith('.wav');
}

function isImage(r: string) {
    return r.endsWith('.jpg') || r.endsWith('.png') || r.endsWith('.jpeg');
}

function label(r: string) {
    let i = r.length - 1;
    for (; i >= 0; i--) {
        const c = r[i];
        if (c == '/' || c == '\\') {
            break;
        }
    }
    return r.substring(i + 1);
}

function getResBrief(res: string[]) {
    let v = 0;
    let a = 0;
    let o = 0;
    for (let r of res) {
        if (isVideo(r)) {
            v++;
        } else if (isAudio(r)) {
            a++;
        } else {
            o++;
        }
    }
    let info = '';
    if (v > 0) {
        info += v + 'v';
    }
    if (a > 0) {
        info += a + 'a';
    }
    if (o > 0) {
        info += o + 'o';
    }
    return info;
}

export const ResPopover = memo(function ResPopover({res}: { res: string[] }) {
    const items: TabsProps['items'] = [];
    let vKey;
    for (let r of res) {
        let key = label(r);
        let content;
        if (isVideo(key)) {
            const assetUrl = convertFileSrc(r);
            content = <video src={assetUrl} controls={true} width="320px"/>;
            if (!vKey) {
                vKey = key;
            }
            // console.log(assetUrl);
        } else if (isAudio(key)) {
            const assetUrl = convertFileSrc(r);
            content = <audio src={assetUrl} controls={true}/>;
        } else if (isImage(key)) {
            const assetUrl = convertFileSrc(r);
            content = <img src={assetUrl} alt={r}/>;
        } else {
            content = <p>{r}</p>
        }
        items.push({
            key: key,
            label: key,
            children: content
        });
    }

    return <Tabs tabPosition='left' defaultActiveKey={vKey} items={items}/>
});


export const FlowNode = memo(function FlowNode(nodeProps: NodeProps<Entity>) {
    const {fields, brief, edit, handleIn, handleOut, id, label, nodeShow} = nodeProps.data;
    const {resMap} = store;
    const color: string = getNodeBackgroundColor(nodeProps.data);
    const width = edit ? 280 : 240;
    const copy: any = {}
    if (nodeShow?.showHead == 'showCopyable') {
        copy.copyable = true;
    }
    // console.log('flownode', nodeProps)


    let resBrief;
    let resContent;
    if (brief) {
        const res = resMap.get(label);
        if (res) {
            resContent = < ResPopover res={res}/>;
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
        title = <Popover content={resContent} trigger='click' style={{color: "#fff"}}>
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
                    const len = (editField.value as any[]).length
                    cnt += len + 1;
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
                        if (row > 1) {
                            extra += row * 22 + 10;
                        } else {
                            cnt++;
                        }
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