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
  common: {
    filterEmptyLists: boolean
  }
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
  common: {
    filterEmptyLists: true,         // 是否过滤空list字段
  },
} as const;

// ============================================================================
// 内嵌判定规则（原 embeddingChecker.ts）—— 检查/提取可内嵌字段
// ============================================================================

/**
 * 内嵌检查器（统一入口）
 * 检查字段是否可以内嵌显示
 */
export function canBeEmbeddedCheck(fieldValue: JSONObject, fieldType: SStruct | SInterface): boolean {
    let struct: SStruct;
    let structCfg: EmbeddableStructConfig;
    if (fieldType.type === 'struct') {
        struct = fieldType as SStruct;
        structCfg = EMBEDDING_CONFIG.struct;
    } else if (fieldType.type === 'interface') {
        const sInterface = fieldType as SInterface;
        const resolved = resolveImpl(sInterface, fieldValue);
        if (!resolved) return false;
        struct = resolved.impl;
        structCfg = EMBEDDING_CONFIG.interface;
    } else {
        return false;
    }

    const filteredFields = filterEmptyListFields(struct.fields, fieldValue);
    const analysis = analyzeFieldTypes(filteredFields);

    return matchEmbeddingConfig(analysis, structCfg);
}

/**
 * 手工新增 list 元素（"添加" / "前插入"）时调用：若该元素类型满足内嵌条件，显式置 $fold=false，
 * 使其作为可编辑节点展开，而非内嵌压缩态——否则新元素因无 $fold（→ undefined）被 shouldEmbed 视为
 * 内嵌，渲染成 EmbeddedSimpleStructuralItem，用户需再点一次展开按钮才能编辑，与"添加后立即编辑"的
 * 意图相悖。与 interfaceOnChangeImpl 切换 impl 后置 $fold=false 同理。
 *
 * 仅对可内嵌元素写入 $fold：永不内嵌的对象（canBeEmbeddedCheck=false）不需要此 UI 标记，避免提交载荷
 * 残留无意义字段。就地变异入参（与 addArrayItem 的 push 语义对齐：调用方 fresh 构造，无共享引用）。
 */
export function markNewItemExpanded(obj: JSONObject, fieldType: SStruct | SInterface): void {
    if (canBeEmbeddedCheck(obj, fieldType)) {
        obj['$fold'] = false;
    }
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
    let struct: SStruct;
    let structCfg: EmbeddableStructConfig;
    let implNameToDisplay: string | undefined = undefined;
    if (fieldType.type === 'struct') {
        struct = fieldType as SStruct;
        structCfg = EMBEDDING_CONFIG.struct;
    } else if (fieldType.type === 'interface') {
        const sInterface = fieldType as SInterface;
        const resolved = resolveImpl(sInterface, fieldValue);
        if (!resolved) return null;
        struct = resolved.impl;
        structCfg = EMBEDDING_CONFIG.interface;
        // 记录implName（非defaultImpl时需要显示）
        implNameToDisplay = resolved.implName !== sInterface.defaultImpl ? resolved.implName : undefined;
    } else {
        return null;
    }

    const filteredFields = filterEmptyListFields(struct.fields, fieldValue);

    if (filteredFields.length === 0) {
        // 没有字段，返回空数组
        return {embeddedFields: []};
    }

    const analysis = analyzeFieldTypes(filteredFields);
    const canEmbedding = matchEmbeddingConfig(analysis, structCfg);

    if (canEmbedding) {
        return {
            embeddedFields: filteredFields.map(field => ({
                value: getFieldValue(fieldValue, field),
                type: field.type as PrimitiveType,
                name: field.name,
                comment: field.comment,
            })),
            implNameToDisplay: implNameToDisplay
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
 * 空list字段过滤器
 * 负责过滤掉值为空数组的list类型字段
 */
function filterEmptyListFields(fields: SField[], obj: JSONObject): SField[] {
    if (!EMBEDDING_CONFIG.common.filterEmptyLists) {
        return fields;
    }

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
