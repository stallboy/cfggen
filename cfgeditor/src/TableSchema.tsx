import {Schema, SInterface, SItem, SStruct, STable} from "./schemaModel.ts";
import {useRete} from "rete-react-plugin";
import {createEditor} from "./editor.tsx";
import {Dispatch, useCallback} from "react";
import {ConnectTo, Entity, EntityConnectionType, EntityNodeType, FieldsShow} from "./graphModel.ts";
import {Item} from "rete-context-menu-plugin/_types/types";


function createNode(item: SItem, id: string, nodeType: EntityNodeType = EntityNodeType.Normal): Entity {
    let fields = [];
    if (item.type != "interface") {
        let st = item as STable | SStruct;
        for (let field of st.fields) {
            fields.push({
                key: field.name,
                name: field.name,
                comment: field.comment,
                value: field.type
            });
        }
    }

    let fieldsShow: FieldsShow = 'direct';
    if (nodeType == EntityNodeType.Ref && fields.length > 5) {
        fieldsShow = 'fold';
    }

    return {
        id: id,
        label: item.name,
        fields: fields,
        inputs: [{key: "input"}],
        outputs: [],

        fieldsShow,
        nodeType,
        userData: item,
    };
}


function includeSubStructs(entityMap: Map<string, Entity>, frontier: (STable | SStruct)[],
                           schema: Schema, maxImpl: number): boolean {
    let hasInterface = false;
    while (frontier.length > 0) {
        let oldFrontier = frontier;
        let depStructNames = schema.getDirectDepStructsByItems(frontier);
        frontier = [];
        for (let depName of depStructNames) {
            let depNode = entityMap.get(depName);
            if (depNode) {
                continue;
            }
            let dep = schema.itemMap.get(depName);
            if (!dep) {
                continue; //不会发生
            }

            depNode = createNode(dep, dep.name);
            entityMap.set(depNode.id, depNode);

            if (dep.type == 'interface') {
                hasInterface = true;
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
                    if (cnt >= maxImpl) {
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

        for (let oldF of oldFrontier) {
            let oldFNode = entityMap.get(oldF.id ?? oldF.name);
            if (!oldFNode) {
                console.log("old frontier " + oldF.id ?? oldF.name + " not found!");
                continue;
            }

            let deps = schema.getDirectDepStructsByItem(oldF);

            let connSockets: ConnectTo[] = [];
            for (let dep of deps) {
                connSockets.push({
                    nodeId: dep,
                    inputKey: "input",
                })
            }

            oldFNode.outputs.push({
                output: {key: "output"},
                connectToSockets: connSockets
            })
        }
    }
    return hasInterface;
}

function includeRefTables(entityMap: Map<string, Entity>, schema: Schema) {
    let frontier: SItem[] = [];
    let entityFrontier: Entity[] = [];
    for (let e of entityMap.values()) {
        frontier.push(e.userData as SItem);
        entityFrontier.push(e);
    }

    let refTableNames = schema.getDirectRefTables(frontier);
    for (let ref of refTableNames) {
        let refNode = entityMap.get(ref);
        if (refNode) {
            continue;
        }

        let refTable = schema.getSTable(ref);
        if (!refTable) {
            console.log(ref + "not found!")
            continue; // 不该发生
        }

        refNode = createNode(refTable, ref, EntityNodeType.Ref);
        entityMap.set(ref, refNode);
    }

    for (let oldNode of entityFrontier) {
        let item = oldNode.userData as SItem;

        if (item.type == 'interface') {
            let ii = item as SInterface;
            oldNode.outputs.push({
                output: {key: "enumRef", label: "enumRef"},
                connectToSockets: [{nodeId: ii.enumRef, inputKey: "input", connectionType: EntityConnectionType.Ref}]
            })
        } else {
            let si = item as (SStruct | STable)
            if (si.foreignKeys) {
                for (let fk of si.foreignKeys) {
                    let prefix = 'ref';
                    if (fk.refType == 'rList') {
                        prefix = 'refList';
                    } else if (fk.refType.startsWith("rNullable")) {
                        prefix = 'nullableRef'
                    }
                    let key = prefix + upper1(fk.name);
                    oldNode.outputs.push({
                        output: {key: key, label: key},
                        connectToSockets: [{
                            nodeId: fk.refTable,
                            inputKey: "input",
                            connectionType: EntityConnectionType.Ref
                        }]
                    })
                }
            }
        }
    }
}

function upper1(str: string): string {
    if (str.length > 0) {
        return str.charAt(0).toUpperCase() + str.substring(1);
    }
    return str;
}

export function TableSchema({schema, curTable, maxImpl, setMaxImpl, setCurTable}: {
    schema: Schema | null;
    curTable: STable | null;
    maxImpl: number;
    setMaxImpl: Dispatch<number>;
    setCurTable: (cur: string) => void;
}) {
    if (schema == null || curTable == null) {
        return <div/>
    }

    const entityMap = new Map<string, Entity>();

    let curNode = createNode(curTable, curTable.name);
    entityMap.set(curNode.id, curNode);
    let frontier = [curTable];
    let hasInterface = includeSubStructs(entityMap, frontier, schema, maxImpl);
    includeRefTables(entityMap, schema);

    const menu: Item[] = [];
    if (hasInterface) {
        for (let n of [10, 1000]) {
            menu.push({
                label: `MaxImpl：${n}`,
                key: n.toString(),
                handler: () => {
                    setMaxImpl(n);
                }
            })
        }
    }

    const nodeMenuFunc = (node: Entity): Item[] => {
        let sItem = node.userData as SItem;
        if (sItem.type == 'table' && sItem.name != curTable.name) {
            return [{
                label: `表结构`,
                key: `${sItem.name}表结构`,
                handler: () => {
                    setCurTable(sItem.name);
                }
            }];

        }
        return [];
    }

    const create = useCallback(
        (el: HTMLElement) => {
            return createEditor(el, {entityMap, menu, nodeMenuFunc});
        },
        [schema, curTable, maxImpl]
    );
    const [ref] = useRete(create);


    return <div ref={ref} style={{height: "100vh", width: "100vw"}}></div>
}