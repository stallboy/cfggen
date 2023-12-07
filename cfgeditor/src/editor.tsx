import {createRoot} from "react-dom/client";
import {ClassicPreset, GetSchemes, NodeEditor} from "rete";
import {AreaExtensions, AreaPlugin} from "rete-area-plugin";
import {ConnectionPlugin, Presets as ConnectionPresets} from "rete-connection-plugin";
import {Presets, ReactArea2D, ReactPlugin} from "rete-react-plugin";

import {AutoArrangePlugin, Presets as ArrangePresets} from "rete-auto-arrange-plugin";
import {EntityConnectionType, EntityGraph, EntityNodeType} from "./graphModel.ts";
import {TableControl, TableControlComponent} from "./ui/TableControl.tsx";
import {EntityNode, EntityNodeComponent} from "./ui/EntityNode.tsx";
import {EntityConnection, EntityConnectionComponent} from "./ui/EntityConnection.tsx";


type Schemes = GetSchemes<EntityNode, EntityConnection<EntityNode, EntityNode>>;
type AreaExtra = ReactArea2D<never>;

export async function createEditor(container: HTMLElement, graph: EntityGraph) {
    const editor = new NodeEditor<Schemes>();
    const area = new AreaPlugin<Schemes, AreaExtra>(container);
    const connection = new ConnectionPlugin<Schemes, AreaExtra>();
    const render = new ReactPlugin<Schemes, AreaExtra>({createRoot});

    const arrange = new AutoArrangePlugin<Schemes>();
    arrange.addPreset(ArrangePresets.classic.setup());
    area.use(arrange);

    render.addPreset(
        Presets.classic.setup({
            customize: {
                control(data) {
                    if (data.payload instanceof TableControl) {
                        return TableControlComponent;
                    }

                    if (data.payload instanceof ClassicPreset.InputControl) {
                        return Presets.classic.Control;
                    }
                    return null;
                },
                connection() {
                    return EntityConnectionComponent;
                },
                node() {
                    return EntityNodeComponent;
                }
            }
        })
    );
    connection.addPreset(ConnectionPresets.classic.setup());
    editor.use(area);
    area.use(connection);
    area.use(render);

    const socket = new ClassicPreset.Socket("socket");

    let id2node = new Map<string, EntityNode>();
    for (let nodeData of graph.entityMap.values()) {
        const node = new EntityNode(nodeData.label);
        node.nodeType = nodeData.nodeType ?? EntityNodeType.Normal;
        id2node.set(nodeData.id, node);
        node.height = 40 * nodeData.fields.length + nodeData.inputs.length * 60 + nodeData.outputs.length * 60 + 60;

        const fields = new TableControl(nodeData.fields);
        node.addControl("value", fields);

        for (let inputSocket of nodeData.inputs) {
            let input = new ClassicPreset.Input(socket, inputSocket.label);
            node.addInput(inputSocket.key, input);
        }
        for (let outputInfo of nodeData.outputs) {
            let output = new ClassicPreset.Output(socket, outputInfo.output.label);
            node.addOutput(outputInfo.output.key, output);
        }

        await editor.addNode(node);
    }

    for (let nodeData of graph.entityMap.values()) {
        let fromNode = id2node.get(nodeData.id) as EntityNode;
        for (let output of nodeData.outputs) {
            for (let connSocket of output.connectToSockets) {
                let toNode = id2node.get(connSocket.nodeId) as EntityNode;
                let conn = new EntityConnection(fromNode, output.output.key, toNode, connSocket.inputKey);
                conn.connectionType = connSocket.connectionType ?? EntityConnectionType.Normal;
                await editor.addConnection(conn);
            }
        }
    }

    await arrange.layout();
    await AreaExtensions.zoomAt(area, editor.getNodes());

    return {
        destroy: () => area.destroy()
    };
}
