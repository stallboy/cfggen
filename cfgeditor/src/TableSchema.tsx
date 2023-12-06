import {getDepStructs, getDepStructs2, Schema, SInterface, SItem, SStruct, STable} from "./model.ts";
import {useRete} from "rete-react-plugin";
import {createEditor, ConnectToSocket, NamedNodeType} from "./whiteboard.tsx";
import {useCallback} from "react";


function createNode(item: SItem): NamedNodeType {
    const node: NamedNodeType = {
        name: item.name,
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


export function TableSchema({schema, curTable, inDepth, outDepth}: {
    schema: Schema | null;
    curTable: STable | null;
    inDepth: number;
    outDepth: number;
}) {
    if (schema == null || curTable == null) {
        return <div/>
    }

    const data = new Map<string, NamedNodeType>();

    let curNode = createNode(curTable);
    data.set(curNode.name, curNode);

    let frontier: (STable | SStruct)[] = [curTable];
    while (frontier.length > 0) {
        let oldFrontier = frontier;
        let depStructNames = getDepStructs2(frontier);
        frontier = [];
        for (let depName of depStructNames) {
            let depNode = data.get(depName);
            if (!depNode) {
                let dep = schema.items[depName];
                if (dep) {
                    let depNode = createNode(dep);
                    data.set(depNode.name, depNode);

                    if (dep.type == 'interface') {
                        let depInterface = dep as SInterface;
                        let connSockets: ConnectToSocket[] = [];
                        let cnt = 0;
                        for (let impl of depInterface.impls) {
                            let depNode2 = createNode(impl);
                            data.set(depNode2.name, depNode2);
                            frontier.push(impl);

                            connSockets.push({
                                node: depNode2.name,
                                inputKey: "input",
                            })
                            cnt++;
                            if (cnt >= 10) {
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
            let oldFNode = data.get(oldF.name);
            let deps = getDepStructs(oldF);

            let connSockets: ConnectToSocket[] = [];
            for (let dep of deps) {
                connSockets.push({
                    node: dep,
                    inputKey: "input",
                })
            }

            oldFNode?.outputs.push({
                output: {key: "output"},
                connectToSockets: connSockets
            })
        }
    }


    const create = useCallback(
        (el: HTMLElement) => {
            return createEditor(el, data);
        },
        [schema, curTable, inDepth, outDepth]
    );
    const [ref] = useRete(create);


    return <div ref={ref} style={{height: "100vh", width: "100vw"}}></div>
}