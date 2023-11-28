import { createRoot } from "react-dom/client";
import { NodeEditor, GetSchemes, ClassicPreset } from "rete";
import { AreaPlugin, AreaExtensions } from "rete-area-plugin";
import {
    ConnectionPlugin,
    Presets as ConnectionPresets
} from "rete-connection-plugin";
import { ReactPlugin, Presets, ReactArea2D } from "rete-react-plugin";
import { Button, Progress } from "antd";
import "antd/dist/reset.css";

class Node extends ClassicPreset.Node<
    { [key in string]: ClassicPreset.Socket },
    { [key in string]: ClassicPreset.Socket },
    {
        [key in string]:
        | ButtonControl
        | ProgressControl
        | ClassicPreset.Control
        | ClassicPreset.InputControl<"number">
        | ClassicPreset.InputControl<"text">;
    }
> {}
class Connection<
    A extends Node,
    B extends Node
> extends ClassicPreset.Connection<A, B> {}

type Schemes = GetSchemes<Node, Connection<Node, Node>>;
type AreaExtra = ReactArea2D<any>;

class ButtonControl extends ClassicPreset.Control {
    constructor(public label: string, public onClick: () => void) {
        super();
    }
}

class ProgressControl extends ClassicPreset.Control {
    constructor(public percent: number) {
        super();
    }
}

function CustomButton(props: { data: ButtonControl }) {
    return (
        <Button
            onPointerDown={(e) => e.stopPropagation()}
    onDoubleClick={(e) => e.stopPropagation()}
    onClick={props.data.onClick}
        >
        {props.data.label}
        </Button>
);
}

function CustomProgress(props: { data: ProgressControl }) {
    return <Progress type="circle" percent={props.data.percent} />;
}

export async function createEditor(container: HTMLElement) {
    const socket = new ClassicPreset.Socket("socket");

    const editor = new NodeEditor<Schemes>();
    const area = new AreaPlugin<Schemes, AreaExtra>(container);
    const connection = new ConnectionPlugin<Schemes, AreaExtra>();
    const render = new ReactPlugin<Schemes, AreaExtra>({ createRoot });

    render.addPreset(
        Presets.classic.setup({
            customize: {
                control(data) {
                    if (data.payload instanceof ButtonControl) {
                        return CustomButton;
                    }
                    if (data.payload instanceof ProgressControl) {
                        return CustomProgress;
                    }
                    if (data.payload instanceof ClassicPreset.InputControl) {
                        return Presets.classic.Control;
                    }
                    return null;
                }
            }
        })
    );
    connection.addPreset(ConnectionPresets.classic.setup());

    editor.use(area);
    area.use(connection);
    area.use(render);

    const a = new Node("A");
    a.addOutput("a", new ClassicPreset.Output(socket));

    const progressControl = new ProgressControl(0);
    const inputControl = new ClassicPreset.InputControl("number", {
        initial: 0,
        change(value) {
            progressControl.percent = value;
            area.update("control", progressControl.id);
        }
    });

    a.addControl("input", inputControl);
    a.addControl("progress", progressControl);
    a.addControl(
        "button",
        new ButtonControl("Randomize", () => {
            const percent = Math.round(Math.random() * 100);

            inputControl.setValue(percent);
            area.update("control", inputControl.id);

            progressControl.percent = percent;
            area.update("control", progressControl.id);
        })
    );
    await editor.addNode(a);

    AreaExtensions.zoomAt(area, editor.getNodes());

    return {
        destroy: () => area.destroy()
    };
}
