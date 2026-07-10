import { EmbeddedFieldValue } from './types';
import { EMBEDDING_CONFIG } from './embeddingConfig';
import { SStruct, SInterface, SField } from '../../../api/schemaModel';
import { JSONObject } from '../../../api/recordModel';
import { PrimitiveValue, PrimitiveType } from '../../../flow/entityModel';
import { FieldTypeAnalyzer, EmptyListFieldFilter, EmbeddingConditionChecker, resolveImpl } from './embeddingChecker';

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
    const resolved = resolveImpl(iface, obj);
    if (!resolved) return null;
    const { impl, implName } = resolved;

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
      // 复用 checker 的单一内嵌判定，避免 5 条条件复制漂移（A3）
      const canEmbedAsStruct = EmbeddingConditionChecker.matchesAnyCondition(analysis, EMBEDDING_CONFIG.struct);
      const canEmbedAsInterface = EmbeddingConditionChecker.matchesAnyCondition(analysis, EMBEDDING_CONFIG.interface);

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
