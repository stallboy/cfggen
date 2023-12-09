export interface Namable {
    name: string;
    type: 'struct' | 'interface' | 'table';
    id?: string;
}

export interface SField {
    name: string;
    type: string;
    comment: string;
}

export interface SForeignKey {
    name: string;
    keys: string[];
    refTable: string;
    refType: 'rPrimary' | 'rUniq' | 'rList' | 'rNullablePrimary' | 'rNullableUniq';
    refKeys?: string[];
}

export interface SStruct extends Namable {
    fields: SField[];
    foreignKeys?: SForeignKey[];
    extends?: SInterface;
}

export interface SInterface extends Namable {
    enumRef: string;
    defaultImpl?: string;
    impls: SStruct[];
}

export interface RecordId {
    id: string;
    desc?: string;
}

export interface STable extends Namable {
    pk: string[];
    uks: string[][];
    entryType: 'eNo' | 'eEnum' | 'eEntry';
    entryField?: string;
    fields: SField[];
    foreignKeys?: SForeignKey[];
    recordCount: number;
    recordIds: RecordId[];
}

export type SItem = SStruct | SInterface | STable;

export interface RawSchema {
    items: SItem[];
}

export class Schema {
    itemMap: Map<string, SItem> = new Map<string, SItem>();

    constructor(rawSchema: RawSchema) {
        for (let item of rawSchema.items) {
            if (item.type == 'interface') {
                let ii = item as SInterface;
                for (let impl of ii.impls) {
                    impl.extends = ii;
                    impl.id = ii.name + "." + impl.name;
                }
            }
            this.itemMap.set(item.name, item);
        }
    }

    getFirstSTable(): STable | null {
        for (let item of this.itemMap.values()) {
            if (item.type == 'table') {
                return item as STable;
            }
        }
        return null;
    }

    getSTable(name: string): STable | null {
        let item = this.itemMap.get(name);
        if (item && item.type == 'table') {
            return item as STable;
        }
        return null;
    }

    getDepStructsByItem(item: STable | SStruct): Set<string> {
        const primitiveTypeSet = new Set<string>(['bool', 'int', 'long', 'float', 'str', 'text']);
        let depNameSet = new Set<string>();
        for (let field of item.fields) {
            let type = field.type;
            if (primitiveTypeSet.has(type)) {
                continue;
            }
            if (type.startsWith("list")) {
                let itemType = type.slice(5, type.length - 1);
                if (!primitiveTypeSet.has(itemType)) {
                    depNameSet.add(itemType);
                }
            } else if (type.startsWith("map")) {
                let item = type.slice(4, type.length - 1);
                let sp = item.split(",");
                let keyType = sp[0].trim();
                let valueType = sp[1].trim();
                if (!primitiveTypeSet.has(keyType)) {
                    depNameSet.add(keyType);
                }
                if (!primitiveTypeSet.has(valueType)) {
                    depNameSet.add(valueType);
                }
            } else {
                depNameSet.add(type);
            }
        }

        // 把局部空间的名字，全转换成全局空间的名字
        if (depNameSet.size > 0) {
            let interfaceNamespace;
            let implNameSet = new Set<string>();
            let pitem: SItem = item;
            if (item.type == 'struct') {
                let si = item as SStruct;
                if (si.extends) {
                    pitem = si.extends;
                    interfaceNamespace = `${si.extends.name}.`;
                    for (let impl of si.extends.impls) {
                        implNameSet.add(impl.name);
                    }
                }
            }

            let moduleNamespace;
            let lastIdx = pitem.name.lastIndexOf(".");
            if (lastIdx != -1) {
                moduleNamespace = pitem.name.substring(0, lastIdx + 1);
            }

            let depNameSetGlobal = new Set<string>();
            for (let n of depNameSet) {
                if (interfaceNamespace) {
                    if (implNameSet.has(n)) {
                        depNameSetGlobal.add(interfaceNamespace + n);
                        continue;
                    }
                }

                if (moduleNamespace) {
                    let fn = moduleNamespace + n;
                    if (this.itemMap.has(fn)) {
                        depNameSetGlobal.add(fn);
                        continue;
                    }
                }

                if (this.itemMap.has(n)) {
                    depNameSetGlobal.add(n);
                    continue;
                }

                console.log(`getDepStructs ${item.name}, ${n} not found!`);
            }
            return depNameSetGlobal;
        }

        return depNameSet;
    }

    getDepStructsByItems(items: (STable | SStruct)[]): Set<string> {
        let res = new Set<string>();
        for (let item of items) {
            let r = this.getDepStructsByItem(item);
            for (let i of r) {
                res.add(i);
            }
        }
        return res;
    }

    getRefTables(items: SItem[]): Set<string> {
        let res = new Set<string>();
        for (let item of items) {
            if (item.type == 'interface') {
                let ii = item as SInterface;
                res.add(ii.enumRef);  // 这里不再遍历impls，因为假设impl被包含在参数items里
            } else {
                let si = item as (SStruct | STable)
                if (si.foreignKeys) {
                    for (let fk of si.foreignKeys) {
                        res.add(fk.refTable);
                    }
                }
            }
        }
        return res;
    }

}
