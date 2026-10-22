import {ReferencePath, SForeignKey, SInterface, SItem, SStruct, STable} from "@/api/schemaModel.ts";
import {ReadOnlyEntity, DisplayField, EntityEdgeType, EntityType} from "@/domain/entityModel.ts";
import {Schema} from "@/domain/schema.ts";
import {HANDLE_IN, HANDLE_OUT} from "@/domain/handleIds.ts";

export class UserData {
    constructor(public table: string, public item: SItem) {}
}

// table 页私有 id 前缀（handle 约定统一走 handleIds.ts）
const PREFIX = {
    TABLE: 't-',
} as const;

function eid(id: string): string {
    return PREFIX.TABLE + id;
}

function createEntity(
    item: SItem,
    id: string,
    table: string,
    entityType: EntityType = EntityType.Normal
): ReadOnlyEntity<UserData> {
    const fields: DisplayField[] = item.type === "interface" ? [] : item.fields.map(field => ({
        key: field.name,
        name: field.name,
        comment: field.comment,
        value: field.type + (field.index?.length ? ` [index: ${field.index.join(', ')}]` : ''),
        handleIn: false,
        handleOut: false,
    }));
    if (item.type !== 'interface') {
        for (const fk of item.foreignKeys ?? []) {
            if (!fk.pathReference) continue;
            fields.push({
                key: `$reference_${fk.name}`,
                name: `→${fk.name}`,
                comment: '',
                value: referenceDescription(fk),
                handleIn: false,
                handleOut: false,
            });
        }
    }

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

function referencePath(path: ReferencePath): string {
    const prefix = path.start === 'ROOT' ? '::' : path.start === 'ANCESTOR' ? `@${path.owner}::`
        : path.start === 'TABLE' ? `${path.owner}${path.fields.length ? '::' : ''}` : '';
    return prefix + path.fields.join('.');
}

function referenceDescription(fk: SForeignKey): string {
    const reference = fk.pathReference!;
    let target = referencePath(reference.target);
    if (reference.target.start === 'TABLE' && fk.refKeys?.length) {
        target = target.replace(reference.target.owner, `${reference.target.owner}[${fk.refKeys.join(',')}]`);
    }
    return `[${reference.sources.map(referencePath).join(', ')}] → ${target}${fk.resultType ? ` : ${fk.resultType}` : ''}`;
}

export class TableEntityCreator {
    constructor(
        public entityMap: Map<string, ReadOnlyEntity<UserData>>,
        public schema: Schema,
        public curTable: STable,
        public maxImpl: number
    ) {}

    private addEntityToMap(entity: ReadOnlyEntity<UserData>): void {
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

                const dep = this.schema.getItem(depName);
                if (!dep) continue;

                const depEntity = createEntity(dep, dep.name, this.curTable.name);
                this.addEntityToMap(depEntity);

                if (dep.type === 'interface') {
                    this.handleInterface(dep, depEntity, frontier);
                } else {
                    frontier.push(dep);
                }
            }

            // 处理旧的前沿节点
            this.processOldFrontier(oldFrontier);
        }
    }

    private handleInterface(depInterface: SInterface, depEntity: ReadOnlyEntity<UserData>, frontier: (STable | SStruct)[]): void {
        depInterface.impls
            .slice(0, this.maxImpl)
            .forEach(impl => {
                const implEntity = createEntity(impl, impl.id ?? impl.name, this.curTable.name);
                this.addEntityToMap(implEntity);
                frontier.push(impl);

                depEntity.sourceEdges.push({
                    sourceHandle: HANDLE_OUT,
                    target: implEntity.id,
                    targetHandle: HANDLE_IN,
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
                    targetHandle: HANDLE_IN,
                    type: EntityEdgeType.Normal,
                });
            }
        });
    }

    includeRefTables(): void {
        Array.from(this.entityMap.values()).forEach(oldEntity => {
            const item = oldEntity.userData;
            if (!item) return; // createEntity 必设 userData，守卫仅为类型收窄

            if (item.item.type === 'interface') {
                this.handleInterfaceRef(item.item, oldEntity);
            } else {
                this.handleStructOrTableRef(item.item, oldEntity);
            }
        });
    }

    private handleInterfaceRef(ii: SInterface, entity: ReadOnlyEntity<UserData>): void {
        if (ii.enumRef) {
            this.addRefToEntityMapIf(ii.enumRef);
            entity.sourceEdges.push({
                sourceHandle: HANDLE_OUT,
                target: eid(ii.enumRef),
                targetHandle: HANDLE_IN,
                type: EntityEdgeType.Ref,
            });
        }
    }

    private handleStructOrTableRef(si: SStruct | STable, entity: ReadOnlyEntity<UserData>): void {
        if (si.foreignKeys) {
            si.foreignKeys.forEach(fk => {
                const ancestor = fk.pathReference?.target.start === 'ANCESTOR' ? fk.pathReference.target.owner : undefined;
                const target = fk.refTable || (ancestor && this.entityMap.has(eid(ancestor)) ? ancestor : undefined);
                if (!target) return;
                if (fk.refTable) this.addRefToEntityMapIf(fk.refTable);
                const sourceField = fk.pathReference
                    ? fk.pathReference.sources.find(path => path.start === 'CURRENT')?.fields[0] ?? HANDLE_OUT
                    : fk.keys[0];
                entity.sourceEdges.push({
                    sourceHandle: sourceField,
                    target: eid(target),
                    targetHandle: fk.refTable ? this.schema.getFkTargetHandle(fk) : HANDLE_IN,
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
