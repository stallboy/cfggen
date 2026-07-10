import { PrimitiveValue, PrimitiveType } from '../../../flow/entityModel';

/**
 * 字段类型分析结果
 */
export interface FieldTypeAnalysis {
  totalFields: number;
  boolCount: number;
  numberCount: number;
  primitiveCount: number;
  allPrimitive: boolean;
}

/**
 * 内嵌字段值
 */
export interface EmbeddedFieldValue {
  value: PrimitiveValue;
  type: PrimitiveType;
  name: string;
  comment?: string;
}
