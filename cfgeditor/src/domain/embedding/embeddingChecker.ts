import {EmbeddableStructConfig, EMBEDDING_CONFIG, isNumberType, isPrimitiveType} from './embeddingConfig';

import {JSONObject} from '../../api/recordModel';
import {SField, SInterface, SStruct} from '../../api/schemaModel';
import {getImpl} from '../schema.tsx';
import {PrimitiveType, PrimitiveValue} from "../entityModel.ts";

export interface EmbeddingSchema {
    itemIncludeImplMap: Map<string, SStruct | SInterface>;
}

/**
 * 内嵌检查器（统一入口）
 * 检查字段是否可以内嵌显示
 */
export function canBeEmbeddedCheck(fieldValue: JSONObject, sField: SField, schema: EmbeddingSchema): boolean {
    const fieldType = schema.itemIncludeImplMap.get(sField.type);
    if (!fieldType) return false;

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
