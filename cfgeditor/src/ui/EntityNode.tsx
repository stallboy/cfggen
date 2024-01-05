import {ClassicPreset} from "rete";
import {EntityControl} from "./EntityControl.tsx";
import {Entity, EntityType, KeywordColor} from "../model/entityModel.ts";
import {RenderEmit} from "rete-react-plugin";
import {css} from "styled-components";
import {RefSocket} from "rete-react-plugin/src/presets/classic/components/refs/RefSocket.tsx";
import {RefControl} from "rete-react-plugin/src/presets/classic/components/refs/RefControl.tsx";
import {NodeStyles} from "rete-react-plugin/src/presets/classic/components/Node.tsx";
import {Typography} from "antd";


export class EntityNode extends ClassicPreset.Node<
    { [key in string]: ClassicPreset.Socket },
    { [key in string]: ClassicPreset.Socket },
    {
        [key in string]:
        | EntityControl
        | ClassicPreset.Control
        | ClassicPreset.InputControl<"number">
        | ClassicPreset.InputControl<"text">;
    }
> {
    width = 280;
    height = 200;

    constructor(public entity: Entity,
                public keywordColors?: KeywordColor[],) {
        super(entity.label);
    }
}

export function EntityNodeComponent(props: { data: EntityNode, emit: RenderEmit<any> }) {
    let entity = props.data.entity;

    let color: string | null = null;
    if (entity.brief && props.data.keywordColors && props.data.keywordColors.length > 0) {
        for (let keywordColor of props.data.keywordColors) {
            if (entity.brief.value.includes(keywordColor.keyword)) {
                color = keywordColor.color;
                break;
            }
        }
    }

    if (color == null) {
        switch (entity.entityType) {
            case EntityType.Ref:
                color = '#237804';
                break;
            case EntityType.Ref2:
                color = '#006d75';
                break;
            case EntityType.RefIn:
                color = '#003eb3';
                break;
            default:
                color = '#1677ff';
                break;
        }
    }

    return <MyNode styles={() => css`background: ${color as string}`} {...props}  />;
}


// eslint-disable-next-line max-statements
function MyNode(props: { data: EntityNode, emit: RenderEmit<any>, styles?: () => any }) {
    const inputs = Object.entries(props.data.inputs)
    const outputs = Object.entries(props.data.outputs)
    const controls = Object.entries(props.data.controls)
    const selected = props.data.selected || false
    const { id, label, width, height } = props.data

    let title;
    if (label.indexOf("_")!=-1){
        title =  <Typography.Text className="title" data-testid="title" copyable>{label}</Typography.Text>;
    }else {
        title = <div className="title" data-testid="title">{label}</div>
    }
    return (
        <NodeStyles
            selected={selected}
            width={width}
            height={height}
            styles={props.styles}
            data-testid="node"
        >
            {title}
            {/* Outputs */}
            {outputs.map(([key, output]) => (
                output && <div className="output" key={key} data-testid={`output-${key}`}>
                    <div className="output-title" data-testid="output-title">{output?.label}</div>
                    <RefSocket
                        name="output-socket"
                        side="output"
                        socketKey={key}
                        nodeId={id}
                        emit={props.emit}
                        payload={output.socket}
                        data-testid="output-socket"
                    />
                </div>
            ))}
            {/* Controls */}
            {controls.map(([key, control]) => {
                return control ? <RefControl
                    key={key}
                    name="control"
                    emit={props.emit}
                    payload={control}
                    data-testid={`control-${key}`}
                /> : null
            })}
            {/* Inputs */}
            {inputs.map(([key, input]) => (
                input && <div className="input" key={key} data-testid={`input-${key}`}>
                    <RefSocket
                        name="input-socket"
                        side="input"
                        socketKey={key}
                        nodeId={id}
                        emit={props.emit}
                        payload={input.socket}
                        data-testid="input-socket"
                    />
                    {input && (!input.control || !input.showControl) && (
                        <div className="input-title" data-testid="input-title">{input?.label}</div>
                    )}
                    {input?.control && input?.showControl && (
                        <RefControl
                            key={key}
                            name="input-control"
                            emit={props.emit}
                            payload={input.control}
                            data-testid="input-control"
                        />
                    )
                    }
                </div>
            ))}
        </NodeStyles>
    )
}