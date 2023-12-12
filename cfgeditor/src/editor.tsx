import {createRoot} from "react-dom/client";
import {ClassicPreset, GetSchemes, NodeEditor} from "rete";
import {AreaExtensions, AreaPlugin} from "rete-area-plugin";
import {ConnectionPlugin, Presets as ConnectionPresets} from "rete-connection-plugin";
import {Presets, ReactArea2D, ReactPlugin} from "rete-react-plugin";

import {AutoArrangePlugin, Presets as ArrangePresets} from "rete-auto-arrange-plugin";
import {EntityConnectionType, EntityGraph, FieldsShowType} from "./model/graphModel.ts";
import {TableControl, TableControlComponent} from "./ui/TableControl.tsx";
import {EntityNode, EntityNodeComponent} from "./ui/EntityNode.tsx";
import {EntityConnection, EntityConnectionComponent} from "./ui/EntityConnection.tsx";
import {ContextMenuExtra, ContextMenuPlugin} from "rete-context-menu-plugin";


type Schemes = GetSchemes<EntityNode, EntityConnection<EntityNode, EntityNode>>;
type AreaExtra = ReactArea2D<Schemes> | ContextMenuExtra;

export async function createEditor(container: HTMLElement, graph: EntityGraph) {
    const editor = new NodeEditor<Schemes>();
    const area = new AreaPlugin<Schemes, AreaExtra>(container);
    const connection = new ConnectionPlugin<Schemes, AreaExtra>();
    const render = new ReactPlugin<Schemes, AreaExtra>({createRoot});

    const arrange = new AutoArrangePlugin<Schemes>();
    arrange.addPreset(ArrangePresets.classic.setup());
    area.use(arrange);

    AreaExtensions.selectableNodes(area, AreaExtensions.selector(), {
        accumulating: AreaExtensions.accumulateOnCtrl()
    });
    // AreaExtensions.simpleNodesOrder(area);


    const contextMenu = new ContextMenuPlugin<Schemes>({
        items: (context: 'root' | Schemes['Node'], _plugin: ContextMenuPlugin<Schemes>) => {
            if (context == 'root') {
                return {list: graph.menu};
            } else if (graph.nodeMenuFunc) {
                let en = editor.getNode(context.id) as EntityNode;
                if (en.entity) {
                    return {list: graph.nodeMenuFunc(en.entity)};
                }
            }
            return {list: []}
        }
    });
    area.use(contextMenu);
    render.addPreset(Presets.contextMenu.setup());

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
    // area.use(connection); // Disable connection manipulation
    area.use(render);

    const socket = new ClassicPreset.Socket("socket");

    let id2node = new Map<string, EntityNode>();
    for (let entity of graph.entityMap.values()) {
        const node = new EntityNode(entity.label);
        node.entity = entity;
        id2node.set(entity.id, node);

        let hasCtrl = true;
        let ch;
        let fc;
        switch (entity.fieldsShow) {
            case FieldsShowType.Direct:
                ch = 0;
                fc = entity.fields.length;
                hasCtrl = false;
                break;
            case FieldsShowType.Expand:
                ch = 80;
                fc = entity.fields.length;
                break;
            case FieldsShowType.Fold:
                ch = 60;
                fc = 0;
                break;
        }
        node.height = 60 + ch + fc * 40 +
            entity.inputs.length * 40 +
            entity.outputs.length * 40;

        const fieldsControl = new TableControl(entity.fields, entity.fieldsShow);
        if (hasCtrl) {
            fieldsControl.onChange = (key: string | string[]) => {
                let ch;
                let fc;
                if (key.length == 0) {
                    ch = 60;
                    fc = 0;
                } else {
                    ch = 80;
                    fc = entity.fields.length;
                }
                node.height = 60 + ch + fc * 40 +
                    entity.inputs.length * 40 +
                    entity.outputs.length * 40;

                area.update('node', node.id);
                // 没办法让connection刷新
                // area.update('control', fieldsControl.id);
                // area.update('socket', node.inputs[0]?.socket.name as string);
                // const connections = editor.getConnections();
                // const incomingConnections = connections.filter(connection => connection.target === node.id);
                // for (let ic of incomingConnections) {
                //     area.update('connection', ic.id);
                // }

            };
        }
        node.addControl("value", fieldsControl);

        for (let inputSocket of entity.inputs) {
            let input = new ClassicPreset.Input(socket, inputSocket.label);
            node.addInput(inputSocket.key, input);
        }
        for (let outputInfo of entity.outputs) {
            let output = new ClassicPreset.Output(socket, outputInfo.output.label);
            node.addOutput(outputInfo.output.key, output);
        }

        await editor.addNode(node);
    }

    for (let entity of graph.entityMap.values()) {
        let fromNode = id2node.get(entity.id) as EntityNode;
        for (let output of entity.outputs) {
            for (let connSocket of output.connectToSockets) {
                let toNode = id2node.get(connSocket.nodeId) as EntityNode;
                if (toNode) { // 可能会没有
                    let conn = new EntityConnection(fromNode, output.output.key, toNode, connSocket.inputKey);
                    conn.connectionType = connSocket.connectionType ?? EntityConnectionType.Normal;
                    await editor.addConnection(conn);
                }

            }
        }
    }

    await arrange.layout();
    await AreaExtensions.zoomAt(area, editor.getNodes());

    return {
        destroy: () => area.destroy()
    };
}
