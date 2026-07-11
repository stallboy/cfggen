// 测试 fixture 工厂：集中构造冗长的类型（NodeShowType / Entity / RawSchema），
// 让各测试用例聚焦于被测行为本身（DAMP），而非重复拼装数据结构。
// 这里只构造数据，不含任何被测逻辑。
import {
    SField, SForeignKey, SInterface, SItem, SStruct, STable, RawSchema,
} from '@/api/schemaModel'
import {
    CardEntity, EditableEntity, EntityEdit, EntityEditField,
    ReadOnlyEntity,
} from '@/domain/entityModel'
import {NodeShowType} from '@/domain/storageJson'
import {NODE_SHOW_DEFAULTS} from '@/flow/colors.ts'

// ---------------------------------------------------------------------------
// NodeShowType
// ---------------------------------------------------------------------------

/** 构造合法 NodeShowType，颜色默认值取自 colors.ts 的 NODE_SHOW_DEFAULTS，便于断言解耦 hex。 */
export function makeNodeShow(overrides: Partial<NodeShowType> = {}): NodeShowType {
    return {
        edgeColor: NODE_SHOW_DEFAULTS.edgeColor,
        edgeStrokeWidth: 1.5,
        editFoldColor: '#888',
        editLayout: 'SIMPLE',
        editNodeWidth: 280,
        fieldColorsByName: [],
        layeredNodeSpacing: 40,
        layeredSpacing: 40,
        mrtreeSpacing: 40,
        nodeColor: NODE_SHOW_DEFAULTS.nodeColor,
        nodeColorsByLabel: [],
        nodeColorsByValue: [],
        nodeRef2Color: NODE_SHOW_DEFAULTS.nodeRef2Color,
        nodeRefColor: NODE_SHOW_DEFAULTS.nodeRefColor,
        nodeRefInColor: NODE_SHOW_DEFAULTS.nodeRefInColor,
        nodeWidth: 240,
        recordLayout: 'SIMPLE',
        refContainEnum: false,
        refIsShowCopyable: false,
        refLayout: 'SIMPLE',
        refShowDescription: 'none',
        refTableHides: [],
        tableLayout: 'SIMPLE',
        tableRefLayout: 'SIMPLE',
        ...overrides,
    }
}

// ---------------------------------------------------------------------------
// Entity 构造（最小可用，按需覆盖）
// ---------------------------------------------------------------------------

export function makeReadOnly(over: Partial<ReadOnlyEntity> & Pick<ReadOnlyEntity, 'id' | 'label'>): ReadOnlyEntity {
    return {
        type: 'readonly',
        sourceEdges: [],
        fields: [],
        ...over,
    } as ReadOnlyEntity
}

export function makeCard(over: Partial<CardEntity> & Pick<CardEntity, 'id' | 'label'>): CardEntity {
    return {
        type: 'card',
        sourceEdges: [],
        brief: {value: ''},
        ...over,
    } as CardEntity
}

export function makeEditable(over: Partial<EditableEntity> & Pick<EditableEntity, 'id' | 'label'>): EditableEntity {
    return {
        type: 'editable',
        sourceEdges: [],
        edit: {
            fields: [],
            editOnUpdateValues: () => {},
            editOnUpdateNote: () => {},
            editOnUpdateFold: () => {},
            hasChild: false,
        },
        ...over,
    } as EditableEntity
}

/** 构造完整 EntityEdit（calcWidthHeight 等仅读 fields/fold，其余给 noop 占位）。 */
export function editWith(fields: EntityEditField[], over: Partial<EntityEdit> = {}): EntityEdit {
    return {
        fields,
        editOnUpdateValues: () => {},
        editOnUpdateNote: () => {},
        editOnUpdateFold: () => {},
        hasChild: false,
        ...over,
    }
}

// ---------------------------------------------------------------------------
// Schema (RawSchema) 构造
// ---------------------------------------------------------------------------

export function field(name: string, type: string, comment = ''): SField {
    return {name, type, comment}
}

export function makeStruct(name: string, fields: SField[], over: Partial<SStruct> = {}): SStruct {
    return {name, type: 'struct', comment: '', fields, ...over}
}

export function makeInterface(name: string, impls: SStruct[], over: Partial<SInterface> = {}): SInterface {
    return {name, type: 'interface', comment: '', impls, ...over}
}

export function makeTable(name: string, fields: SField[], over: Partial<STable> = {}): STable {
    return {
        name, type: 'table', comment: '',
        pk: ['id'],
        uks: [],
        entryType: 'eNo',
        fields,
        recordIds: [],
        ...over,
    }
}

export function fk(name: string, keys: string[], refTable: string,
                   over: Partial<SForeignKey> = {}): SForeignKey {
    return {name, keys, refTable, refType: 'rPrimary', ...over}
}

/** 构造 RawSchema（lastModifiedMap 可省略）。 */
export function makeRawSchema(items: SItem[], over: Partial<RawSchema> = {}): RawSchema {
    return {
        isEditable: true,
        items,
        lastModifiedMap: new Map(),
        ...over,
    }
}
