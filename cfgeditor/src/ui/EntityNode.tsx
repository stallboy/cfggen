import {ClassicPreset} from "rete";
import {EntityControl} from "./EntityControl.tsx";
import {Entity} from "../model/entityModel.ts";
import {Presets, RenderEmit} from "rete-react-plugin";
import {Typography} from "antd";
import {css} from "styled-components";
import {NodeShowType} from "../func/localStoreJson.ts";
import {getNodeBackgroundColor} from "./colors.ts";

const {NodeStyles, RefControl, RefSocket} = Presets.classic;

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
                public nodeShow?: NodeShowType,) {
        super(entity.label);
    }
}


export function EntityNodeComponent(props: { data: EntityNode, emit: RenderEmit<any> }) {
    let entity = props.data.entity;
    entity.nodeShow = props.data.nodeShow;
    let color: string = getNodeBackgroundColor(entity);


    const styles = css<{}>`background: ${color}`;
    return <MyNode styles={() => styles} {...props} />;
}


// eslint-disable-next-line max-statements
function MyNode(props: { data: EntityNode, emit: RenderEmit<any>, styles?: () => any }) {
    const inputs = Object.entries(props.data.inputs)
    const outputs = Object.entries(props.data.outputs)
    const controls = Object.entries(props.data.controls)
    const selected = props.data.selected || false
    const {id, label, width, height} = props.data

    let title;
    const nodeShow = props.data.nodeShow;
    if (label.indexOf("_") != -1 && nodeShow && nodeShow.showHead == 'showCopyable') {
        title = <Typography.Text className="title" data-testid="title" copyable>{label}</Typography.Text>;
    } else {
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
