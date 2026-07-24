import {JSONArray, JSONObject} from '@/api/recordModel';
import {isNumberType, isPrimitiveType, PrimitiveType, SField, SInterface, SStruct} from '@/api/schemaModel';
import {defaultValueOfPrimitive, getImpl} from '@/domain/schema';
import {PrimitiveValue} from "@/domain/entityModel";

// ============================================================================
// 内嵌配置（原 embeddingConfig.ts）—— 内嵌规则的阈值与类型判断
// ============================================================================

export interface EmbeddableStructConfig {
  maxFieldsForEmpty: number,
  maxFieldsForSinglePrimitive: number,
  maxNumberFields: number,
  maxBoolFields: number,
  boolAndNumberCombination: {
    boolCount: number,
    numberCount: number,
    totalFields: number,
  },
}

export interface EmbeddingConfig {
  struct : EmbeddableStructConfig,
  interface: EmbeddableStructConfig,
}

/**
 * 内嵌显示配置 - 统一管理所有内嵌相关的阈值和规则
 * 将分散在代码中的魔数集中管理，便于调整内嵌规则
 */
export const EMBEDDING_CONFIG : EmbeddingConfig = {
  struct: {
    maxFieldsForEmpty: 0,           // 条件1a: 没有字段
    maxFieldsForSinglePrimitive: 1, // 条件1b: 只有1个primitive
    maxNumberFields: 3,             // 条件1c: ≤3个number
    maxBoolFields: 4,               // 条件1d: ≤4个bool
    boolAndNumberCombination: {     // 条件1e: 1个bool + 1个number
      boolCount: 1,
      numberCount: 1,
      totalFields: 2,
    },
  },
  interface: {
    maxFieldsForEmpty: 0,           // 条件2a: 没有字段
    maxFieldsForSinglePrimitive: 1, // 条件2b: 只有1个primitive
    maxNumberFields: 2,             // 条件2c: ≤2个number (比struct更严格)
    maxBoolFields: 3,               // 条件2d: ≤3个bool
    boolAndNumberCombination: {     // 条件2e: 1个bool + 1个number
      boolCount: 1,
      numberCount: 1,
      totalFields: 2,
    },
  },
} as const;

// ============================================================================
// 内嵌判定规则（原 embeddingChecker.ts）—— 检查/提取可内嵌字段
// ============================================================================

interface ResolvedEmbed {
    struct: SStruct;
    structCfg: EmbeddableStructConfig;
    implNameToDisplay?: string;
}

// struct/interface 解析（canBeEmbeddedCheck 与 extractEmbeddingFields 共用）：
// 选 struct + 对应 structCfg；interface 解析失败返回 null。
function resolveEmbedTarget(fieldType: SStruct | SInterface, fieldValue: JSONObject): ResolvedEmbed | null {
    if (fieldType.type === 'struct') {
        return {struct: fieldType, structCfg: EMBEDDING_CONFIG.struct};
    }
    if (fieldType.type === 'interface') {
        const sInterface = fieldType;
        const resolved = resolveImpl(sInterface, fieldValue);
        if (!resolved) return null;
        const implNameToDisplay = resolved.implName !== sInterface.defaultImpl ? resolved.implName : undefined;
        return {struct: resolved.impl, structCfg: EMBEDDING_CONFIG.interface, implNameToDisplay};
    }
    return null;
}

/**
 * 内嵌检查器（统一入口）
 * 检查字段是否可以内嵌显示
 */
export function canBeEmbeddedCheck(fieldValue: JSONObject, fieldType: SStruct | SInterface): boolean {
    const target = resolveEmbedTarget(fieldType, fieldValue);
    if (!target) return false;

    const filteredFields = filterEmptyListFields(target.struct.fields, fieldValue);
    return matchEmbeddingConfig(analyzeFieldTypes(filteredFields), target.structCfg);
}

export interface EmbeddingFieldValues {
    embeddedFields: {
        value: PrimitiveValue;
        type: PrimitiveType;
        name: string;
        comment?: string;
    }[];
    implNameToDisplay?: string;
}


export function extractEmbeddingFields(fieldType: SStruct | SInterface, fieldValue: JSONObject): EmbeddingFieldValues | null {
    const target = resolveEmbedTarget(fieldType, fieldValue);
    if (!target) return null;
    const {struct, structCfg, implNameToDisplay} = target;

    const filteredFields = filterEmptyListFields(struct.fields, fieldValue);

    if (filteredFields.length === 0) {
        // 没有字段，返回空数组
        return {embeddedFields: [], implNameToDisplay};
    }

    if (matchEmbeddingConfig(analyzeFieldTypes(filteredFields), structCfg)) {
        return {
            embeddedFields: filteredFields.map(field => ({
                value: getFieldValue(fieldValue, field),
                type: field.type as PrimitiveType,
                name: field.name,
                comment: field.comment,
            })),
            implNameToDisplay
        };
    }

    return null;
}


interface FieldTypeAnalysis {
    totalFields: number;
    boolCount: number;
    numberCount: number;
    primitiveCount: number;
    allPrimitive: boolean;
}

function analyzeFieldTypes(fields: SField[]): FieldTypeAnalysis {
    let boolCount = 0, numberCount = 0, primitiveCount = 0;

    for (const field of fields) {
        if (isPrimitiveType(field.type)) {
            primitiveCount++;
            if (field.type === 'bool') boolCount++;
            if (isNumberType(field.type)) numberCount++;
        }
    }

    return {
        totalFields: fields.length,
        boolCount,
        numberCount,
        primitiveCount,
        allPrimitive: primitiveCount === fields.length,
    };
}


/**
 * 内嵌条件检查器
 * 负责检查给定的字段类型分析是否满足内嵌条件
 */
function matchEmbeddingConfig(analysis: FieldTypeAnalysis, config: EmbeddableStructConfig): boolean {
    // 条件a: 没有字段
    if (analysis.totalFields === config.maxFieldsForEmpty) return true;

    // 条件b: 只有1个primitive
    if (analysis.totalFields === config.maxFieldsForSinglePrimitive &&
        analysis.allPrimitive) return true;

    // 条件c: 只有≤N个number
    if (analysis.totalFields <= config.maxNumberFields &&
        analysis.totalFields === analysis.numberCount &&
        analysis.allPrimitive) return true;

    // 条件d: 只有≤M个bool
    if (analysis.totalFields <= config.maxBoolFields &&
        analysis.totalFields === analysis.boolCount &&
        analysis.allPrimitive) return true;

    // 条件e: 1个bool + 1个number
    const {numberCount, totalFields, boolCount} = config.boolAndNumberCombination;
    // noinspection RedundantIfStatementJS
    if (analysis.totalFields === totalFields &&
        analysis.boolCount === boolCount &&
        analysis.numberCount === numberCount &&
        analysis.allPrimitive) return true;

    return false;
}


/**
 * 空list字段过滤器：计数前固定过滤值为空数组的 list 类型字段（无开关）——
 * 避免一个空 list 把字段数顶过阈值。
 */
function filterEmptyListFields(fields: SField[], obj: JSONObject): SField[] {
    return fields.filter(field => {
        // 检查是否为list类型
        if (!field.type.startsWith('list<')) {
            return true; // 非list类型，保留
        }

        // list类型，检查实际值是否为空数组
        const fieldValue = obj[field.name];
        if (!Array.isArray(fieldValue)) {
            return true; // 不是数组，保留
        }

        // 是空数组，过滤掉
        if (fieldValue.length === 0) {
            return false;
        }

        // 非空数组，保留
        return true;
    });
}


/**
 * 解析 interface 的 $type → impl（checker 与 extractor 共用，避免解析逻辑漂移）
 */
function resolveImpl(iface: SInterface, obj: JSONObject): { impl: SStruct; implName: string } | null {
    const type = obj['$type'];
    if (typeof type !== 'string') return null;  // 后端脏数据/新旧 schema 不一致时 $type 可能缺失
    const implName = type.split('.').pop() || type;
    const impl = getImpl(iface, implName);
    if (!impl) return null;
    return {impl, implName};
}

/**
 * 获取字段值（带默认值处理）
 */
function getFieldValue(obj: JSONObject, field: SField): PrimitiveValue {
    const value = obj[field.name];

    if (value !== undefined && value !== null) {
        return value as PrimitiveValue;
    }

    // 默认值（走 schemaModel 单一来源 defaultValueOfPrimitive；非原始类型兜底 0）
    return isPrimitiveType(field.type) ? defaultValueOfPrimitive(field.type) : 0;
}

// ============================================================================
// embed 状态机（读侧分类 + 写侧归一化政策）
// ----------------------------------------------------------------------------
// 给「`$embed_<fieldName>` 键该怎么读、操作后该怎么写」一个家。读侧 classifyListField 是
// doc 07 §五-b 的 4 稳态机本体；写侧 normalizeOnX 是纯决策（返回 KeyOp），由 EditingSession
// 在 undo 括号内 apply——session 仍是唯一 mutator、creator 仍经参数注入可内嵌判定。
// 不变式：键只存「当前类」的非默认值（类 1 默认收起 / 类 2 默认展开）。归一化只写 false（保持
// 展开意图）或删键，永不写 true（true 仅来自用户显式收起 updateEmbed(true)）。
// ============================================================================

/** 字段级 embed 状态键：状态寄存在父对象的 `$embed_<fieldName>` 上（数组挂不了属性）。 */
export function embedKey(fieldName: string): string {
    return `$embed_${fieldName}`;
}

/** 读父对象上的 `$embed_<fieldName>`：true=收起 / false=展开 / undefined=当前类默认态。 */
export function getEmbedState(obj: JSONObject | undefined, fieldName: string): boolean | undefined {
    return obj?.[embedKey(fieldName)] as boolean | undefined;
}

/** 写侧归一化决策：操作后该对 `$embed_<fieldName>` 键做什么。三种都不带 payload——归一化只写
 *  字面 false（永不写 true；true 仅来自用户显式收起 updateEmbed(true)），故用字符串联合而非
 *  {kind} 判别联合（无 payload 时判别联合是纯仪式）。 */
export type KeyOp = 'writeFalse' | 'delete' | 'noop';

/**
 * list 字段的 embed 分类——**类由数据决定，键在类语义内解读**（doc 07 §五-b 4 稳态机读侧）：
 * - 'embedTag'：类 1（恰 1 元素、元素可内嵌、`embedState !== false`）→ 内嵌 Tag，不建子节点
 * - 'summary'：类 2 嵌入态（`embedState === true`）→ 摘要行，不建子节点
 * - 'nodes'：展开 → 建子 entity / funcAdd 非嵌入态
 *
 * embedState 由调用方经 getEmbedState 算好传入；可内嵌判定内部自调 canBeEmbeddedCheck。
 */
export function classifyListField(
    fArr: JSONArray,
    itemType: SStruct | SInterface,
    embedState: boolean | undefined,
): 'embedTag' | 'summary' | 'nodes' {
    if (fArr.length === 1 && embedState !== false
        && canBeEmbeddedCheck(fArr[0] as JSONObject, itemType)) {
        return 'embedTag';   // true 键在类 1 视同默认（收起）
    }
    if (embedState === true) {
        return 'summary';
    }
    return 'nodes';
}

/** add 后归一化决策：0→1 且新元素可内嵌 → 写 false（默认展开、立即可编辑）；
 *  其余（≥2 或单元素不可内嵌）→ 删键。可内嵌判定内部自调 canBeEmbeddedCheck（itemType 由调用方传入）。 */
export function normalizeOnAdd(arr: JSONArray, itemType: SStruct | SInterface): KeyOp {
    if (arr.length === 1 && canBeEmbeddedCheck(arr[0] as JSONObject, itemType)) {
        return 'writeFalse';
    }
    return 'delete';
}

/** delete 后归一化决策（与用户操作同一步 undo）：
 *  删到空 → 删键；恰剩 1 且可内嵌 →（true→删键延续收起意图 / 否则写 false 保持展开）；
 *  恰剩 1 且不可内嵌 → 删键（默认展开）；≥2 → 不动（类 2 收起态 true 仍合法）。
 *  传删后的 arr——幸存元素即 arr[0]，判定内部自调 canBeEmbeddedCheck。 */
export function normalizeOnDelete(arr: JSONArray, itemType: SStruct | SInterface, currentKey?: boolean): KeyOp {
    if (arr.length === 0) {
        return 'delete';
    }
    if (arr.length === 1) {
        if (canBeEmbeddedCheck(arr[0] as JSONObject, itemType)) {
            return currentKey === true ? 'delete' : 'writeFalse';
        }
        return 'delete';
    }
    return 'noop';
}

/** impl 切换后归一化决策（双向，切换入口只在展开态）：
 *  新 impl 可内嵌 → 写 false（保持展开）；不可内嵌 → 删残留键。判定内部自调 canBeEmbeddedCheck。
 *  arrLen 非 undefined（list 元素换 impl）：仅恰剩 1 元素（类 1 候选）才写 false，多元素删键。
 *  arrLen undefined（struct 字段换 impl）：直接按可内嵌决定。 */
export function normalizeOnImplSwitch(newImpl: JSONObject, itemType: SStruct | SInterface, arrLen?: number): KeyOp {
    const embeddable = canBeEmbeddedCheck(newImpl, itemType);
    if (arrLen !== undefined) {
        return arrLen === 1 && embeddable ? 'writeFalse' : 'delete';
    }
    return embeddable ? 'writeFalse' : 'delete';
}
