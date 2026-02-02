import { EMBEDDING_CONFIG, isPrimitiveType } from './embeddingConfig';
import { EmbeddingCheckContext, FieldTypeAnalysis } from './types';
import { JSONObject } from '../../../api/recordModel';
import { SField, SStruct, SInterface } from '../../../api/schemaModel';
import { getImpl } from '../../table/schemaUtil';

/**
 * 字段类型分析器
 *
 * 负责分析结构体或接口的字段类型分布
 */
export class FieldTypeAnalyzer {
  /**
   * 分析字段类型
   */
  static analyze(fields: SField[]): FieldTypeAnalysis {
    let boolCount = 0, numberCount = 0, primitiveCount = 0;

    for (const field of fields) {
      if (isPrimitiveType(field.type)) {
        primitiveCount++;
        if (field.type === 'bool') boolCount++;
        if (EMBEDDING_CONFIG.struct && // 使用配置判断
            ['int', 'long', 'float'].includes(field.type)) numberCount++;
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
}

/**
 * 内嵌条件检查器
 *
 * 负责检查给定的字段类型分析是否满足内嵌条件
 */
export class EmbeddingConditionChecker {
  /**
   * 检查是否满足内嵌条件
   */
  static check(
    analysis: FieldTypeAnalysis,
    context: EmbeddingCheckContext
  ): boolean {
    const config = context.fieldType === 'struct'
      ? EMBEDDING_CONFIG.struct
      : EMBEDDING_CONFIG.interface;

    return this.matchesAnyCondition(analysis, config);
  }

  /**
   * 检查是否匹配任一内嵌条件
   */
  private static matchesAnyCondition(
    analysis: FieldTypeAnalysis,
    config: typeof EMBEDDING_CONFIG.struct | typeof EMBEDDING_CONFIG.interface
  ): boolean {
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
    if (analysis.totalFields === config.boolAndNumberCombination.totalFields &&
        analysis.boolCount === config.boolAndNumberCombination.boolCount &&
        analysis.numberCount === config.boolAndNumberCombination.numberCount &&
        analysis.allPrimitive) return true;

    return false;
  }
}

/**
 * 空list字段过滤器
 *
 * 负责过滤掉值为空数组的list类型字段
 */
export class EmptyListFieldFilter {
  /**
   * 过滤空list字段
   */
  static filter(fields: SField[], obj: JSONObject): SField[] {
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
}

/**
 * 内嵌检查器（统一入口）
 *
 * 检查字段是否可以内嵌显示
 */
export function canBeEmbeddedCheck(
  fieldValue: JSONObject,
  sField: SField,
  schema: { itemIncludeImplMap: Map<string, SStruct | SInterface> }
): boolean {
  const fieldType = schema.itemIncludeImplMap.get(sField.type);
  if (!fieldType) return false;

  if (fieldType.type === 'struct') {
    const struct = fieldType as SStruct;
    return checkStructEmbeddable(struct, fieldValue);
  }

  if (fieldType.type === 'interface') {
    const iface = fieldType as SInterface;
    return checkInterfaceEmbeddable(iface, fieldValue);
  }

  return false;
}

/**
 * 检查struct是否可以内嵌
 */
function checkStructEmbeddable(struct: SStruct, fieldValue: JSONObject): boolean {
  // 先过滤空list字段
  const filteredFields = EmptyListFieldFilter.filter(struct.fields, fieldValue);
  const analysis = FieldTypeAnalyzer.analyze(filteredFields);

  const context: EmbeddingCheckContext = {
    fieldType: 'struct',
    struct,
  };

  return EmbeddingConditionChecker.check(analysis, context);
}

/**
 * 检查interface是否可以内嵌
 */
function checkInterfaceEmbeddable(iface: SInterface, fieldValue: JSONObject): boolean {
  const type = fieldValue['$type'] as string;
  const implName = type.split('.').pop() || type;
  const impl = getImpl(iface, implName);
  if (!impl) return false;

  // 先过滤空list字段
  const filteredFields = EmptyListFieldFilter.filter(impl.fields, fieldValue);
  const analysis = FieldTypeAnalyzer.analyze(filteredFields);

  const context: EmbeddingCheckContext = {
    fieldType: 'interface',
    iface,
    impl,
  };

  return EmbeddingConditionChecker.check(analysis, context);
}
