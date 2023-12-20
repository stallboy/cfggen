import {Schema, SItem, STable} from "./model/schemaModel.ts";
import {useRete} from "rete-react-plugin";
import {createEditor} from "./editor.tsx";
import {useCallback} from "react";
import {Entity, EntityConnectionType, EntityGraph, EntityType, FieldsShowType, fillInputs} from "./model/entityModel.ts";
import {Item} from "rete-context-menu-plugin/_types/types";


function createEntity(item: SItem, id: string, entityType: EntityType = EntityType.Normal): Entity {
    return {
        id: id,
        label: item.name,
        fields: [],
        inputs: [],
        outputs: [],

        fieldsShow: FieldsShowType.Direct,
        entityType: entityType,
        userData: item,
    };
}

function includeRefTables(entityMap: Map<string, Entity>, curTable: STable, schema: Schema,
                          refIn: boolean, maxOutDepth: number, maxNode: number) {

    let curEntity = createEntity(curTable, curTable.name);
    entityMap.set(curEntity.id, curEntity);


    if (refIn && curTable.refInTables) {

        for (let ref of curTable.refInTables) {
            let refInEntity = entityMap.get(ref);
            if (refInEntity) {
                continue;
            }

            let refInTable = schema.getSTable(ref);
            if (!refInTable) {
                console.log(ref + " not found!")
                continue; // 不该发生
            }

            refInEntity = createEntity(refInTable, ref, EntityType.RefIn);
            entityMap.set(ref, refInEntity);

            refInEntity.outputs.push({
                output: {key: 'refIn', label: 'refIn'},
                connectToSockets: [{
                    entityId: curTable.name,
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
    let entityFrontier: Entity[] = [curEntity];
    let depth = 1;
    while (depth <= maxOutDepth) {

        let newFrontier: SItem[] = [];
        let newEntityFrontier: Entity[] = [];

        let refTableNames = schema.getAllRefTablesByItems(frontier);
        for (let ref of refTableNames) {
            let refEntity = entityMap.get(ref);
            if (refEntity) {
                continue;
            }

            let refTable = schema.getSTable(ref);
            if (!refTable) {
                console.log(ref + " not found!")
                continue; // 不该发生
            }

            let entityType = depth == 1 ? EntityType.Ref : EntityType.Ref2;
            refEntity = createEntity(refTable, ref, entityType);
            entityMap.set(ref, refEntity);

            newFrontier.push(refTable);
            newEntityFrontier.push(refEntity);

            if (entityMap.size > maxNode) {
                break;
            }
        }

        for (let oldEntity of entityFrontier) {
            let item = oldEntity.userData as SItem;

            let directRefs = schema.getAllRefTablesByItem(item);
            let connectToSockets = []
            for (let ref of directRefs) {
                if (entityMap.has(ref)) {
                    connectToSockets.push({
                        entityId: ref,
                        inputKey: "input",
                        connectionType: EntityConnectionType.Ref
                    });
                }
            }

            if (connectToSockets.length > 0) {
                oldEntity.outputs.push({
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

    function createGraph(): EntityGraph {
        const entityMap = new Map<string, Entity>();
        includeRefTables(entityMap, curTable, schema, refIn, refOutDepth, maxNode);
        fillInputs(entityMap);

        const menu: Item[] = [];

        const entityMenuFunc = (entity: Entity): Item[] => {
            let sItem = entity.userData as SItem;
            if (sItem.type == 'table' && sItem.name != curTable.name) {
                return [{
                    label: `表关系`,
                    key: `${sItem.name}表关系`,
                    handler() {
                        setCurTable(sItem.name);
                    }
                }];

            }
            return [];
        }
        return {entityMap, menu, entityMenuFunc};
    }

    const create = useCallback(
        (el: HTMLElement) => {
            return createEditor(el, createGraph());
        },
        [schema, curTable, refIn, refOutDepth, maxNode]
    );
    const [ref] = useRete(create);


    return <div ref={ref} style={{height: "100vh", width: "100vw"}}></div>
}
