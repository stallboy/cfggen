import {getDepStructs, getDepStructs2, Schema, SInterface, SItem, SStruct, STable} from "./schemaModel.ts";
import {useRete} from "rete-react-plugin";
import {createEditor} from "./editor.tsx";
import {useCallback} from "react";
import {ConnectTo, Entity} from "./graphModel.ts";


function createNode(item: SItem, id: string): Entity {
    const node: Entity = {
        id: id,
        label: item.name,
        fields: [],
        inputs: [],
        outputs: [],
    };

    if (item.type == "interface") {
        node.inputs.push({key: "input"});
        return node;
    }

    let st = item as STable | SStruct;

    if (item.type == "struct") {
        node.inputs.push({key: "input"});
    }

    for (let field of st.fields) {
        node.fields.push({
            key: field.name,
            name: field.name,
            value: field.type
        });
    }
    return node;
}

type FrontierType = (STable | SStruct)[];

function includeSubStructs(entityMap: Map<string, Entity>, frontier: FrontierType, schema: Schema, settingMaxImplSchema: number) {
    while (frontier.length > 0) {
        let oldFrontier = frontier;
        let depStructNames = getDepStructs2(frontier, schema);
        frontier = [];
        for (let depName of depStructNames) {
            let depNode = entityMap.get(depName);
            if (!depNode) {
                let dep = schema.itemMap.get(depName);
                if (dep) {
                    let depNode = createNode(dep, dep.name);
                    entityMap.set(depNode.id, depNode);

                    if (dep.type == 'interface') {
                        let depInterface = dep as SInterface;
                        let connSockets: ConnectTo[] = [];
                        let cnt = 0;
                        for (let impl of depInterface.impls) {
                            let implNode = createNode(impl, depInterface.name + "." + impl.name);
                            entityMap.set(implNode.id, implNode);
                            frontier.push(impl);

                            connSockets.push({
                                nodeId: implNode.id,
                                inputKey: "input",
                            })
                            cnt++;
                            if (cnt >= settingMaxImplSchema) {
                                break;
                            }
                        }
                        depNode.outputs.push({
                            output: {key: "output", label: depInterface.impls.length.toString()},
                            connectToSockets: connSockets
                        })

                    } else {
                        frontier.push(dep as SStruct);
                    }
                }
            }
        }

        for (let oldF of oldFrontier) {
            let oldFNode = entityMap.get(oldF.id ?? oldF.name);
            if (!oldFNode) {
                console.log("old frontier " + oldF.id ?? oldF.name + " not found!");
                continue;
            }

            let deps = getDepStructs(oldF, schema);

            let connSockets: ConnectTo[] = [];
            for (let dep of deps) {
                connSockets.push({
                    nodeId: dep,
                    inputKey: "input",
                })
            }

            if (oldFNode.outputs.length > 0) {
                console.log(oldF.name);
            }

            oldFNode.outputs.push({
                output: {key: "output"},
                connectToSockets: connSockets
            })
        }
    }
}

export function TableSchema({schema, curTable, inDepth, outDepth, settingMaxImplSchema}: {
    schema: Schema | null;
    curTable: STable | null;
    inDepth: number;
    outDepth: number;
    settingMaxImplSchema: number;
}) {
    if (schema == null || curTable == null) {
        return <div/>
    }

    const entityMap = new Map<string, Entity>();

    let curNode = createNode(curTable, curTable.name);
    entityMap.set(curNode.id, curNode);
    let frontier: FrontierType = [curTable];
    includeSubStructs(entityMap, frontier, schema, settingMaxImplSchema);

    const create = useCallback(
        (el: HTMLElement) => {
            return createEditor(el, {entityMap});
        },
        [schema, curTable, inDepth, outDepth]
    );
    const [ref] = useRete(create);


    return <div ref={ref} style={{height: "100vh", width: "100vw"}}></div>
}