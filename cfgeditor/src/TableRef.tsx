import {Schema, SItem, STable} from "./model/schemaModel.ts";
import {useRete} from "rete-react-plugin";
import {createEditor} from "./editor.tsx";
import {useCallback} from "react";
import {Entity, EntityConnectionType, EntityNodeType} from "./model/graphModel.ts";
import {Item} from "rete-context-menu-plugin/_types/types";


function createNode(item: SItem, id: string, nodeType: EntityNodeType = EntityNodeType.Normal): Entity {
    return {
        id: id,
        label: item.name,
        fields: [],
        inputs: [{key: "input"}],
        outputs: [],

        fieldsShow: 'direct',
        nodeType,
        userData: item,
    };
}

function includeRefTables(entityMap: Map<string, Entity>, curTable: STable, schema: Schema,
                          refIn: boolean, maxOutDepth: number, maxNode: number) {

    let curNode = createNode(curTable, curTable.name);
    entityMap.set(curNode.id, curNode);


    if (refIn && curTable.refInTables) {

        for (let ref of curTable.refInTables) {
            let refInNode = entityMap.get(ref);
            if (refInNode) {
                continue;
            }

            let refInTable = schema.getSTable(ref);
            if (!refInTable) {
                console.log(ref + " not found!")
                continue; // 不该发生
            }

            refInNode = createNode(refInTable, ref, EntityNodeType.RefIn);
            entityMap.set(ref, refInNode);

            refInNode.outputs.push({
                output: {key: 'refIn', label: 'refIn'},
                connectToSockets: [{
                    nodeId: curTable.name,
                    inputKey: "input",
                    connectionType: EntityConnectionType.Ref
                }],
            });

            if (entityMap.size > maxNode / 2) {
                break;
            }
        }
    }


    let frontier: SItem[] = [curTable];
    let entityFrontier: Entity[] = [curNode];
    let depth = 1;
    while (depth <= maxOutDepth) {

        let newFrontier: SItem[] = [];
        let newEntityFrontier: Entity[] = [];

        let refTableNames = schema.getAllRefTablesByItems(frontier);
        for (let ref of refTableNames) {
            let refNode = entityMap.get(ref);
            if (refNode) {
                continue;
            }

            let refTable = schema.getSTable(ref);
            if (!refTable) {
                console.log(ref + " not found!")
                continue; // 不该发生
            }

            let nodeType = depth == 1 ? EntityNodeType.Ref : EntityNodeType.Ref2;
            refNode = createNode(refTable, ref, nodeType);
            entityMap.set(ref, refNode);

            newFrontier.push(refTable);
            newEntityFrontier.push(refNode);

            if (entityMap.size > maxNode) {
                break;
            }
        }

        for (let oldNode of entityFrontier) {
            let item = oldNode.userData as SItem;

            let directRefs = schema.getAllRefTablesByItem(item);
            let connectToSockets = []
            for (let ref of directRefs) {
                if (entityMap.has(ref)) {
                    connectToSockets.push({
                        nodeId: ref,
                        inputKey: "input",
                        connectionType: EntityConnectionType.Ref
                    });
                }
            }

            if (connectToSockets.length > 0) {
                oldNode.outputs.push({
                    output: {key: 'ref', label: 'ref'},
                    connectToSockets
                });
            }
        }

        frontier = newFrontier;
        entityFrontier = newEntityFrontier;
        depth++;

        if (entityMap.size > maxNode) {
            break;
        }
    }
}


export function TableRef({schema, curTable, setCurTable, refIn, refOutDepth, maxNode}: {
    schema: Schema;
    curTable: STable;
    setCurTable: (cur: string) => void;
    refIn: boolean;
    refOutDepth: number;
    maxNode: number;
}) {
    const entityMap = new Map<string, Entity>();
    includeRefTables(entityMap, curTable, schema, refIn, refOutDepth, maxNode);

    const menu: Item[] = [];

    const nodeMenuFunc = (node: Entity): Item[] => {
        let sItem = node.userData as SItem;
        if (sItem.type == 'table' && sItem.name != curTable.name) {
            return [{
                label: `表关系`,
                key: `${sItem.name}表关系`,
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
        [schema, curTable, refIn, refOutDepth, maxNode]
    );
    const [ref] = useRete(create);


    return <div ref={ref} style={{height: "100vh", width: "100vw"}}></div>
}