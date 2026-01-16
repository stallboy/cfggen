import { EmbeddedFieldValue, FieldTypeAnalysis, EmbeddingCheckContext } from './types';
import { EMBEDDING_CONFIG } from './embeddingConfig';
import { SStruct, SInterface, SField } from '../../table/schemaModel';
import { JSONObject } from '../recordModel';
import { PrimitiveValue, PrimitiveType } from '../../../flow/entityModel';
import { FieldTypeAnalyzer, EmptyListFieldFilter } from './embeddingChecker';
import { getImpl } from '../../table/schemaUtil';

/**
 * 内嵌字段提取器
 *
 * 负责从struct或interface中提取内嵌字段的值
 */
export class EmbeddingFieldExtractor {
  /**
   * 从struct中提取内嵌字段的值
   */
  static extractFromStruct(
    struct: SStruct,
    obj: JSONObject
  ): EmbeddedFieldValue[] | null {
    return this.extractFields(struct.fields, obj);
  }

  /**
   * 从interface的impl中提取内嵌字段的值
   */
  static extractFromInterface(
    iface: SInterface,
    obj: JSONObject
  ): { fields: EmbeddedFieldValue[]; implName?: string } | null {
    const type = obj['$type'] as string;
    const implName = type.split('.').pop() || type;
    const impl = getImpl(iface, implName);

    if (!impl) return null;

    const fields = this.extractFields(impl.fields, obj);

    if (!fields) return null;

    return {
      fields,
      // 记录implName（非defaultImpl时需要显示）
      implName: implName !== iface.defaultImpl ? implName : undefined,
    };
  }

  /**
   * 提取字段的通用方法
   */
  private static extractFields(
    fields: SField[],
    obj: JSONObject
  ): EmbeddedFieldValue[] | null {
    // 先过滤空list字段
    const filteredFields = EmptyListFieldFilter.filter(fields, obj);

    if (filteredFields.length === 0) {
      // 没有字段，返回空数组
      return [];
    }

    const analysis = FieldTypeAnalyzer.analyze(filteredFields);

    // 单字段情况
    if (filteredFields.length === 1 && analysis.allPrimitive) {
      const onlyField = filteredFields[0];
      return [{
        value: this.getFieldValue(obj, onlyField),
        type: onlyField.type as PrimitiveType,
        name: onlyField.name,
        comment: onlyField.comment,
      }];
    }

    // 多字段情况
    if (analysis.allPrimitive) {
      // 检查是否满足内嵌条件
      const structContext: EmbeddingCheckContext = { fieldType: 'struct' };
      const interfaceContext: EmbeddingCheckContext = { fieldType: 'interface' };

      const canEmbedAsStruct = this.canEmbedWithConfig(analysis, structContext, EMBEDDING_CONFIG.struct);
      const canEmbedAsInterface = this.canEmbedWithConfig(analysis, interfaceContext, EMBEDDING_CONFIG.interface);

      if (canEmbedAsStruct || canEmbedAsInterface) {
        return filteredFields.map(field => ({
          value: this.getFieldValue(obj, field),
          type: field.type as PrimitiveType,
          name: field.name,
          comment: field.comment,
        }));
      }
    }

    return null;
  }

  /**
   * 检查是否可以使用指定配置内嵌
   */
  private static canEmbedWithConfig(
    analysis: FieldTypeAnalysis,
    _context: EmbeddingCheckContext,
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

  /**
   * 获取字段值（带默认值处理）
   */
  private static getFieldValue(obj: JSONObject, field: SField): PrimitiveValue {
    const value = obj[field.name];

    if (value !== undefined && value !== null) {
      return value as PrimitiveValue;
    }

    // 默认值（集中管理）
    switch (field.type) {
      case 'bool': return false;
      case 'int':
      case 'long':
      case 'float': return 0;
      case 'str':
      case 'text': return '';
      default: return 0;
    }
  }
}
