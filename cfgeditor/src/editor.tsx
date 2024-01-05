import {createRoot} from "react-dom/client";
import {ClassicPreset, GetSchemes, NodeEditor} from "rete";
import {AreaExtensions, AreaPlugin} from "rete-area-plugin";
import {ConnectionPlugin, Presets as ConnectionPresets} from "rete-connection-plugin";
import {Presets, ReactArea2D, ReactPlugin} from "rete-react-plugin";

import {AutoArrangePlugin, Presets as ArrangePresets} from "rete-auto-arrange-plugin";
import {Entity, EntityConnectionType, EntityGraph, FieldsShowType} from "./model/entityModel.ts";
import {EntityControl, EntityControlComponent} from "./ui/EntityControl.tsx";
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
            } else if (graph.entityMenuFunc) {
                let en = editor.getNode(context.id) as EntityNode;
                return {list: graph.entityMenuFunc(en.entity)};
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
                    if (data.payload instanceof EntityControl) {
                        return EntityControlComponent;
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
    let heightCollected = 0;

    // https://eclipse.dev/elk/reference/algorithms/org-eclipse-elk-layered.html
    let options = {
        'elk.layered.spacing.nodeNodeBetweenLayers': '80',
        'elk.spacing.nodeNode': '60',
        'elk.layered.nodePlacement.strategy': graph.nodePlacementStrategy ?? 'LINEAR_SEGMENTS',
        'elk.layered.considerModelOrder.strategy': 'NODES_AND_EDGES',
        'elk.layered.crossingMinimization.forceNodeModelOrder': 'true',
    };


    async function onHeightCallback() {
        heightCollected++;
        if (heightCollected == graph.entityMap.size) {
            // console.log("layout" + heightCollected);
            heightCollected = 0;
            await arrange.layout({options});
            await AreaExtensions.zoomAt(area, editor.getNodes());
        }
    }

    for (let entity of graph.entityMap.values()) {
        const node = new EntityNode(entity, graph.keywordColors);

        id2node.set(entity.id, node);

        node.height = calcHeight(entity);
        node.width = entity.fieldsShow == FieldsShowType.Edit ? 360 : 280;

        async function changeHeightCallback(height: number) {
            // console.log(entity.id + ", " + new Date() + ", " + height);
            node.height = calcKnownHeight(entity) + height;
            onHeightCallback();
            await area.update('node', node.id);
            setTimeout(async () => {
                await area.update('node', node.id);
            }, 200)

        }

        const fieldsControl = new EntityControl(entity, changeHeightCallback, graph.query, graph.showDescription);
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
                let toNode = id2node.get(connSocket.entityId) as EntityNode;
                if (toNode) { // 可能会没有
                    let conn = new EntityConnection(fromNode, output.output.key, toNode, connSocket.inputKey);
                    conn.connectionType = connSocket.connectionType ?? EntityConnectionType.Normal;
                    await editor.addConnection(conn);
                }
            }
        }
    }

    await arrange.layout({options});
    await AreaExtensions.zoomAt(area, editor.getNodes());

    return {
        destroy: () => area.destroy()
    };
}

function calcHeight(entity: Entity): number {
    let ch;
    let fc = 0;
    let fh = 40;

    if (entity.editFields) {
        fc = entity.editFields.length;
        if (fc > 0 && entity.editFields[0].implFields) {
            fc += entity.editFields[0].implFields.length;
        }
    } else if (entity.fields) {
        fc = entity.fields.length;
    } else if (entity.brief) {
        fc = 5;
    }
    switch (entity.fieldsShow) {
        case FieldsShowType.Direct:
            ch = 0;
            break;
        case FieldsShowType.Expand:
            ch = 80;
            break;
        case FieldsShowType.Fold:
            ch = 60;
            fc = 0;
            break;
        case FieldsShowType.Edit:
            ch = 0;
            fh = 60;
            break;
    }
    return calcKnownHeight(entity) + ch + fc * fh;
}


function calcKnownHeight(entity: Entity): number {
    return 60 + entity.inputs.length * 40 + entity.outputs.length * 40;
}
