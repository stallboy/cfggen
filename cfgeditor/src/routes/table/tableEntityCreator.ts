import {SInterface, SItem, SStruct, STable} from "./schemaModel.ts";
import {ReadOnlyEntity, DisplayField, EntityEdgeType, EntityType} from "../../flow/entityModel.ts";
import {Schema} from "./schemaUtil.tsx";

export class UserData {
    constructor(public table: string, public item: SItem) {}
}

// 统一 ID 前缀处理
const PREFIX = {
    TABLE: 't-',
    IN: '@in',
    OUT: '@out'
} as const;

function eid(id: string): string {
    return PREFIX.TABLE + id;
}

function createEntity(
    item: SItem,
    id: string,
    table: string,
    entityType: EntityType = EntityType.Normal
): ReadOnlyEntity {
    const fields: DisplayField[] = item.type === "interface" ? [] : (item as STable | SStruct).fields.map(field => ({
        key: field.name,
        name: field.name,
        comment: field.comment,
        value: field.type,
        handleIn: false,
        handleOut: false,
    }));

    return {
        id: eid(id),
        label: item.name,
        type: 'readonly',
        fields,
        sourceEdges: [],
        entityType,
        userData: new UserData(table, item),
    };
}

export class TableEntityCreator {
    constructor(
        public entityMap: Map<string, ReadOnlyEntity>,
        public schema: Schema,
        public curTable: STable,
        public maxImpl: number
    ) {}

    private addEntityToMap(entity: ReadOnlyEntity): void {
        this.entityMap.set(entity.id, entity);
    }

    includeSubStructs(): void {
        const frontier: (STable | SStruct)[] = [this.curTable];
        const curEntity = createEntity(this.curTable, this.curTable.name, this.curTable.name);
        this.addEntityToMap(curEntity);

        while (frontier.length > 0) {
            const oldFrontier = frontier.slice();
            const depStructNames = this.schema.getDirectDepStructsByItems(frontier);
            frontier.length = 0;

            // 处理依赖结构
            for (const depName of depStructNames) {
                const entityId = eid(depName);
                if (this.entityMap.has(entityId)) continue;

                const dep = this.schema.itemMap.get(depName);
                if (!dep) continue;

                const depEntity = createEntity(dep, dep.name, this.curTable.name);
                this.addEntityToMap(depEntity);

                if (dep.type === 'interface') {
                    this.handleInterface(dep as SInterface, depEntity, frontier);
                } else {
                    frontier.push(dep as SStruct);
                }
            }

            // 处理旧的前沿节点
            this.processOldFrontier(oldFrontier);
        }
    }

    private handleInterface(depInterface: SInterface, depEntity: ReadOnlyEntity, frontier: (STable | SStruct)[]): void {
        depInterface.impls
            .slice(0, this.maxImpl)
            .forEach(impl => {
                const implEntity = createEntity(impl, impl.id ?? impl.name, this.curTable.name);
                this.addEntityToMap(implEntity);
                frontier.push(impl);

                depEntity.sourceEdges.push({
                    sourceHandle: PREFIX.OUT,
                    target: implEntity.id,
                    targetHandle: PREFIX.IN,
                    type: EntityEdgeType.Normal,
                });
            });
    }

    private processOldFrontier(oldFrontier: (STable | SStruct)[]): void {
        oldFrontier.forEach(oldF => {
            const entityId = eid(oldF.id ?? oldF.name);
            const oldFEntity = this.entityMap.get(entityId);
            
            if (!oldFEntity) {
                console.warn(`Old frontier ${oldF.id ?? oldF.name} not found!`);
                return;
            }

            const deps = this.schema.getDirectDepStructsMapByItem(oldF);
            for (const [type, name] of deps) {
                oldFEntity.sourceEdges.push({
                    sourceHandle: name,
                    target: eid(type),
                    targetHandle: PREFIX.IN,
                    type: EntityEdgeType.Normal,
                });
            }
        });
    }

    includeRefTables(): void {
        Array.from(this.entityMap.values()).forEach(oldEntity => {
            const item = oldEntity.userData as UserData;
            
            if (item.item.type === 'interface') {
                this.handleInterfaceRef(item.item as SInterface, oldEntity);
            } else {
                this.handleStructOrTableRef(item.item as (SStruct | STable), oldEntity);
            }
        });
    }

    private handleInterfaceRef(ii: SInterface, entity: ReadOnlyEntity): void {
        if (ii.enumRef) {
            this.addRefToEntityMapIf(ii.enumRef);
            entity.sourceEdges.push({
                sourceHandle: PREFIX.OUT,
                target: eid(ii.enumRef),
                targetHandle: PREFIX.IN,
                type: EntityEdgeType.Ref,
            });
        }
    }

    private handleStructOrTableRef(si: SStruct | STable, entity: ReadOnlyEntity): void {
        if (si.foreignKeys) {
            si.foreignKeys.forEach(fk => {
                this.addRefToEntityMapIf(fk.refTable);
                entity.sourceEdges.push({
                    sourceHandle: fk.keys[0],
                    target: eid(fk.refTable),
                    targetHandle: this.schema.getFkTargetHandle(fk),
                    type: EntityEdgeType.Ref,
                });
            });
        }
    }

    private addRefToEntityMapIf(tableName: string): void {
        if (!this.entityMap.has(eid(tableName))) {
            const sTable = this.schema.getSTable(tableName);
            if (sTable) {
                const entity = createEntity(sTable, sTable.name, sTable.name, EntityType.Ref);
                this.addEntityToMap(entity);
            }
        }
    }
}
