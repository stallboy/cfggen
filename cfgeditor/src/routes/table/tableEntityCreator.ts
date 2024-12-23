import {SInterface, SItem, SStruct, STable} from "./schemaModel.ts";
import {Entity, EntityEdgeType, EntityType} from "../../flow/entityModel.ts";
import {Schema} from "./schemaUtil.tsx";

export class UserData {
    constructor(public table: string, public item: SItem) {
    }
}

function eid(id: string) {
    return 't-' + id;
}

function createEntity(item: SItem, id: string, table: string, entityType: EntityType = EntityType.Normal): Entity {
    const fields = [];
    if (item.type != "interface") {
        const st = item as STable | SStruct;
        for (const field of st.fields) {
            fields.push({
                key: field.name,
                name: field.name,
                comment: field.comment,
                value: field.type
            });
        }
    }


    return {
        id: eid(id),
        label: item.name,
        fields: fields,
        sourceEdges: [],
        entityType: entityType,
        userData: new UserData(table, item),
    };
}

export class TableEntityCreator {

    constructor(public entityMap: Map<string, Entity>,
                public schema: Schema,
                public curTable: STable,
                public maxImpl: number
    ) {
    }

    includeSubStructs() {
        let frontier: (STable | SStruct)[] = [this.curTable];
        const curEntity = createEntity(this.curTable, this.curTable.name, this.curTable.name);
        this.entityMap.set(curEntity.id, curEntity);

        while (frontier.length > 0) {
            const oldFrontier = frontier;
            const depStructNames = this.schema.getDirectDepStructsByItems(frontier);
            frontier = [];
            for (const depName of depStructNames) {
                let depEntity = this.entityMap.get(eid(depName));
                if (depEntity) {
                    continue;
                }
                const dep = this.schema.itemMap.get(depName);
                if (!dep) {
                    continue; //不会发生
                }

                depEntity = createEntity(dep, dep.name, this.curTable.name);
                this.entityMap.set(depEntity.id, depEntity);

                if (dep.type == 'interface') {

                    const depInterface = dep as SInterface;

                    let cnt = 0;
                    for (const impl of depInterface.impls) {
                        const implEntity = createEntity(impl, impl.id ?? impl.name, this.curTable.name);
                        this.entityMap.set(implEntity.id, implEntity);
                        frontier.push(impl);

                        depEntity.sourceEdges.push({
                            sourceHandle: '@out',
                            target: implEntity.id,
                            targetHandle: '@in',
                            type: EntityEdgeType.Normal,
                        })

                        cnt++;
                        if (cnt >= this.maxImpl) {
                            break;
                        }
                    }

                } else {
                    frontier.push(dep as SStruct);
                }
            }

            for (const oldF of oldFrontier) {
                const oldFEntity = this.entityMap.get(eid(oldF.id ?? oldF.name));
                if (!oldFEntity) {
                    console.log("old frontier " + (oldF.id ?? oldF.name) + " not found!");
                    continue;
                }
                const deps = this.schema.getDirectDepStructsMapByItem(oldF);
                for (const [type, name] of deps) {
                    oldFEntity.sourceEdges.push({
                        sourceHandle: name,
                        target: eid(type),
                        targetHandle: '@in',
                        type: EntityEdgeType.Normal,
                    })
                }
            }
        }
    }


    includeRefTables() {
        const entityFrontier = [];
        for (const e of this.entityMap.values()) {
            entityFrontier.push(e);
        }

        for (const oldEntity of entityFrontier) {
            const item = (oldEntity.userData as UserData).item;

            if (item.type == 'interface') {
                const ii = item as SInterface;
                if (ii.enumRef) {
                    this.addRefToEntityMapIf(ii.enumRef);
                    oldEntity.sourceEdges.push({
                        sourceHandle: '@out',
                        target: eid(ii.enumRef),
                        targetHandle: "@in",
                        type: EntityEdgeType.Ref,
                    });
                }

            } else {
                const si = item as (SStruct | STable)
                if (si.foreignKeys) {
                    for (const fk of si.foreignKeys) {
                        this.addRefToEntityMapIf(fk.refTable);
                        oldEntity.sourceEdges.push({
                            sourceHandle: fk.keys[0],
                            target: eid(fk.refTable),
                            targetHandle: this.schema.getFkTargetHandle(fk),
                            type: EntityEdgeType.Ref,
                        });
                    }
                }
            }
        }
    }

    addRefToEntityMapIf(tableName: string) {
        let entity = this.entityMap.get(eid(tableName));
        if (!entity) {
            const sTable = this.schema.getSTable(tableName);
            if (sTable) {
                entity = createEntity(sTable, sTable.name, sTable.name, EntityType.Ref);
                this.entityMap.set(entity.id, entity);
            }
        }
    }

}
