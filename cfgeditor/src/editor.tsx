import { createRoot } from "react-dom/client";
import { NodeEditor, GetSchemes, ClassicPreset } from "rete";
import { AreaPlugin, AreaExtensions } from "rete-area-plugin";
import {
    ConnectionPlugin,
    Presets as ConnectionPresets
} from "rete-connection-plugin";
import { ReactPlugin, Presets, ReactArea2D } from "rete-react-plugin";

import { AutoArrangePlugin, Presets as ArrangePresets } from "rete-auto-arrange-plugin";

class Node extends ClassicPreset.Node {
    width = 180;
    height = 220;
}
class Connection<N extends Node> extends ClassicPreset.Connection<N, N> {}


type Schemes = GetSchemes<
    Node,
    Connection<Node>
>;
type AreaExtra = ReactArea2D<Schemes>;



export async function createEditor(container: HTMLElement) {


    const editor = new NodeEditor<Schemes>();
    const area = new AreaPlugin<Schemes, AreaExtra>(container);
    const connection = new ConnectionPlugin<Schemes, AreaExtra>();
    const render = new ReactPlugin<Schemes, AreaExtra>({ createRoot });

    AreaExtensions.selectableNodes(area, AreaExtensions.selector(), {
        accumulating: AreaExtensions.accumulateOnCtrl()
    });

    const arrange = new AutoArrangePlugin<Schemes>();
    arrange.addPreset(ArrangePresets.classic.setup());
    area.use(arrange);

    render.addPreset(Presets.classic.setup());
    connection.addPreset(ConnectionPresets.classic.setup());

    editor.use(area);
    area.use(connection);
    area.use(render);

    AreaExtensions.simpleNodesOrder(area);

    const socket = new ClassicPreset.Socket("socket");

    const a = new Node("A");
    a.addControl("a", new ClassicPreset.InputControl("text", { initial: "a\nb\nc" , readonly: true}));
    a.addOutput("a", new ClassicPreset.Output(socket, "aa"));
    a.addOutput("a2", new ClassicPreset.Output(socket, "bbbb"));
    await editor.addNode(a);

    const b = new Node("B");
    b.addControl("b", new ClassicPreset.InputControl("text", { initial: "b" }));
    b.addInput("b", new ClassicPreset.Input(socket, "b port"));
    await editor.addNode(b);

    const c = new Node("C");
    c.addInput("c", new ClassicPreset.Input(socket, "c port"));
    c.addControl("c1", new ClassicPreset.InputControl("text", { initial: "c" }));
    c.addControl("c2", new ClassicPreset.InputControl("text", { initial: "c2" }));

    await editor.addNode(c);

    await editor.addConnection(new Connection(a, "a", b, "b"));
    await editor.addConnection(new Connection(a, "a", c, "c"));

    await arrange.layout();
    // await area.translate(a.id, { x: 0, y: 0 });
    // await area.translate(b.id, { x: 270, y: 0 });

    setTimeout(() => {
        // wait until nodes rendered because they dont have predefined width and height
        AreaExtensions.zoomAt(area, editor.getNodes());
    }, 10);
    return {
        destroy: () => area.destroy()
    };
}
