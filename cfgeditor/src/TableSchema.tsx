import {Schema, SInterface, SItem, SStruct, STable} from "./model/schemaModel.ts";
import {useRete} from "rete-react-plugin";
import {createEditor} from "./editor.tsx";
import {useCallback} from "react";
import {
    ConnectTo,
    Entity,
    EntityConnectionType, EntityGraph,
    EntityType,
    FieldsShowType,
    fillInputs
} from "./model/graphModel.ts";
import {Item} from "rete-context-menu-plugin/_types/types";


function createEntity(item: SItem, id: string, entityType: EntityType = EntityType.Normal): Entity {
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

    let fieldsShow = FieldsShowType.Direct;
    if (entityType == EntityType.Ref && fields.length > 5) {
        fieldsShow = FieldsShowType.Fold;
    }

    return {
        id: id,
        label: item.name,
        fields: fields,
        inputs: [],
        outputs: [],

        fieldsShow,
        entityType: entityType,
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
            let depEntity = entityMap.get(depName);
            if (depEntity) {
                continue;
            }
            let dep = schema.itemMap.get(depName);
            if (!dep) {
                continue; //不会发生
            }

            depEntity = createEntity(dep, dep.name);
            entityMap.set(depEntity.id, depEntity);

            if (dep.type == 'interface') {
                hasInterface = true;
                let depInterface = dep as SInterface;
                let connSockets: ConnectTo[] = [];
                let cnt = 0;
                for (let impl of depInterface.impls) {
                    let implEntity = createEntity(impl, depInterface.name + "." + impl.name);
                    entityMap.set(implEntity.id, implEntity);
                    frontier.push(impl);

                    connSockets.push({
                        entityId: implEntity.id,
                        inputKey: "input",
                    })
                    cnt++;
                    if (cnt >= maxImpl) {
                        break;
                    }
                }
                depEntity.outputs.push({
                    output: {key: "output", label: depInterface.impls.length.toString()},
                    connectToSockets: connSockets
                })

            } else {
                frontier.push(dep as SStruct);
            }
        }

        for (let oldF of oldFrontier) {
            let oldFEntity = entityMap.get(oldF.id ?? oldF.name);
            if (!oldFEntity) {
                console.log("old frontier " + (oldF.id ?? oldF.name) + " not found!");
                continue;
            }

            let deps = schema.getDirectDepStructsByItem(oldF);

            let connSockets: ConnectTo[] = [];
            for (let dep of deps) {
                connSockets.push({
                    entityId: dep,
                    inputKey: "input",
                })
            }
            if (connSockets.length > 0) {
                oldFEntity.outputs.push({
                    output: {key: "output"},
                    connectToSockets: connSockets
                })
            }
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
        let refEntity = entityMap.get(ref);
        if (refEntity) {
            continue;
        }

        let refTable = schema.getSTable(ref);
        if (!refTable) {
            console.log(ref + "not found!")
            continue; // 不该发生
        }

        refEntity = createEntity(refTable, ref, EntityType.Ref);
        entityMap.set(ref, refEntity);
    }

    for (let oldEntity of entityFrontier) {
        let item = oldEntity.userData as SItem;

        if (item.type == 'interface') {
            let ii = item as SInterface;
            oldEntity.outputs.push({
                output: {key: "enumRef", label: "enumRef"},
                connectToSockets: [{entityId: ii.enumRef, inputKey: "input", connectionType: EntityConnectionType.Ref}]
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
                    oldEntity.outputs.push({
                        output: {key: key, label: key},
                        connectToSockets: [{
                            entityId: fk.refTable,
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

export function TableSchema({schema, curTable, maxImpl, setCurTable}: {
    schema: Schema;
    curTable: STable;
    maxImpl: number;
    setCurTable: (cur: string) => void;
}) {

    function createGraph() : EntityGraph {
        const entityMap = new Map<string, Entity>();
        let curEntity = createEntity(curTable, curTable.name);
        entityMap.set(curEntity.id, curEntity);
        let frontier = [curTable];
        includeSubStructs(entityMap, frontier, schema, maxImpl);
        includeRefTables(entityMap, schema);
        fillInputs(entityMap);

        const menu: Item[] = [];

        const entityMenuFunc = (entity: Entity): Item[] => {
            let sItem = entity.userData as SItem;
            if (sItem.type == 'table' && sItem.name != curTable.name) {
                return [{
                    label: `表结构`,
                    key: `${sItem.name}表结构`,
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
        [schema, curTable, maxImpl]
    );
    const [ref] = useRete(create);

    return <div ref={ref} style={{height: "100vh", width: "100vw"}}></div>
}
