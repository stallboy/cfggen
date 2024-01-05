import {Schema, SInterface, SItem, SStruct, STable} from "../model/schemaModel.ts";
import {ConnectTo, Entity, EntityConnectionType, EntityType, FieldsShowType} from "../model/entityModel.ts";

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
                    let connSockets: ConnectTo[] = [];
                    let cnt = 0;
                    for (let impl of depInterface.impls) {
                        let implEntity = createEntity(impl, depInterface.name + "." + impl.name, this.curTable.name);
                        this.entityMap.set(implEntity.id, implEntity);
                        frontier.push(impl);

                        connSockets.push({
                            entityId: implEntity.id,
                            inputKey: "input",
                        })
                        cnt++;
                        if (cnt >= this.maxImpl) {
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
                let oldFEntity = this.entityMap.get(oldF.id ?? oldF.name);
                if (!oldFEntity) {
                    console.log("old frontier " + (oldF.id ?? oldF.name) + " not found!");
                    continue;
                }

                let deps = this.schema.getDirectDepStructsByItem(oldF);

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
    }


    includeRefTables() {
        let frontier: SItem[] = [];
        let entityFrontier: Entity[] = [];
        for (let e of this.entityMap.values()) {
            frontier.push((e.userData as UserData).item);
            entityFrontier.push(e);
        }

        let refTableNames = this.schema.getDirectRefTables(frontier);
        for (let ref of refTableNames) {
            let refEntity = this.entityMap.get(ref);
            if (refEntity) {
                continue;
            }

            let refTable = this.schema.getSTable(ref);
            if (!refTable) {
                console.log(ref + "not found!")
                continue; // 不该发生
            }

            refEntity = createEntity(refTable, ref, refTable.name, EntityType.Ref);
            this.entityMap.set(ref, refEntity);
        }

        for (let oldEntity of entityFrontier) {
            let item = (oldEntity.userData as UserData).item;

            if (item.type == 'interface') {
                let ii = item as SInterface;
                if (ii.enumRef) {
                    oldEntity.outputs.push({
                        output: {key: "enumRef", label: "enumRef"},
                        connectToSockets: [{
                            entityId: ii.enumRef,
                            inputKey: "input",
                            connectionType: EntityConnectionType.Ref
                        }]
                    })
                }

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
}

function upper1(str: string): string {
    if (str.length > 0) {
        return str.charAt(0).toUpperCase() + str.substring(1);
    }
    return str;
}