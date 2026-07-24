import {JSONObject} from '@/api/recordModel';
import {isNumberType, isPrimitiveType, PrimitiveType, SField, SInterface, SStruct} from '@/api/schemaModel';
import {getImpl} from '@/domain/schema';
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
// 注意 SStruct/SInterface 未把 type 收窄为单字面量（见 schemaModel.ts Namable），故仍需 as 断言。
function resolveEmbedTarget(fieldType: SStruct | SInterface, fieldValue: JSONObject): ResolvedEmbed | null {
    if (fieldType.type === 'struct') {
        return {struct: fieldType as SStruct, structCfg: EMBEDDING_CONFIG.struct};
    }
    if (fieldType.type === 'interface') {
        const sInterface = fieldType as SInterface;
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

    // 默认值（集中管理）
    switch (field.type) {
        case 'bool':
            return false;
        case 'int':
        case 'long':
        case 'float':
            return 0;
        case 'str':
        case 'text':
            return '';
        default:
            return 0;
    }
}
