import {SInterface, SItem, SStruct, STable} from "../model/schemaModel.ts";
import {Entity, EntityEdgeType, EntityType, FieldsShowType} from "../model/entityModel.ts";
import {Schema} from "../model/schemaUtil.ts";

export class UserData {
    constructor(public table: string, public item: SItem) {
    }
}

function createEntity(item: SItem, id: string, table: string, entityType: EntityType = EntityType.Normal): Entity {
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
        sourceEdges: [],

        fieldsShow,
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
        let curEntity = createEntity(this.curTable, this.curTable.name, this.curTable.name);
        this.entityMap.set(curEntity.id, curEntity);

        while (frontier.length > 0) {
            let oldFrontier = frontier;
            let depStructNames = this.schema.getDirectDepStructsByItems(frontier);
            frontier = [];
            for (let depName of depStructNames) {
                let depEntity = this.entityMap.get(depName);
                if (depEntity) {
                    continue;
                }
                let dep = this.schema.itemMap.get(depName);
                if (!dep) {
                    continue; //不会发生
                }

                depEntity = createEntity(dep, dep.name, this.curTable.name);
                this.entityMap.set(depEntity.id, depEntity);

                if (dep.type == 'interface') {

                    let depInterface = dep as SInterface;

                    let cnt = 0;
                    for (let impl of depInterface.impls) {
                        let implEntity = createEntity(impl, impl.id ?? impl.name, this.curTable.name);
                        implEntity.parentId = depEntity.id;
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

            for (let oldF of oldFrontier) {
                let oldFEntity = this.entityMap.get(oldF.id ?? oldF.name);
                if (!oldFEntity) {
                    console.log("old frontier " + (oldF.id ?? oldF.name) + " not found!");
                    continue;
                }
                let deps = this.schema.getDirectDepStructsMapByItem(oldF);
                for (let [type, name] of deps) {
                    oldFEntity.sourceEdges.push({
                        sourceHandle: name,
                        target: type,
                        targetHandle: '@in',
                        type: EntityEdgeType.Normal,
                    })
                }
            }
        }
    }


    includeRefTables() {
        let entityFrontier = [];
        for (let e of this.entityMap.values()) {
            entityFrontier.push(e);
        }

        for (let oldEntity of entityFrontier) {
            let item = (oldEntity.userData as UserData).item;

            if (item.type == 'interface') {
                let ii = item as SInterface;
                if (ii.enumRef) {
                    this.addRefToEntityMapIf(ii.enumRef);
                    oldEntity.sourceEdges.push({
                        sourceHandle: '@out',
                        target: ii.enumRef,
                        targetHandle: "@in",
                        type: EntityEdgeType.Ref,
                    });
                }

            } else {
                let si = item as (SStruct | STable)
                if (si.foreignKeys) {
                    for (let fk of si.foreignKeys) {
                        this.addRefToEntityMapIf(fk.refTable);
                        oldEntity.sourceEdges.push({
                            sourceHandle: fk.keys[0],
                            target: fk.refTable,
                            targetHandle: this.schema.getFkTargetHandle(fk),
                            type: EntityEdgeType.Ref,
                        });
                    }
                }
            }
        }
    }

    addRefToEntityMapIf(tableName: string) {
        let entity = this.entityMap.get(tableName);
        if (!entity) {
            let sTable = this.schema.getSTable(tableName);
            if (sTable) {
                entity = createEntity(sTable, sTable.name, sTable.name, EntityType.Ref);
                this.entityMap.set(sTable.name, entity);
            }
        }
    }

}
