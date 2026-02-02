import { PrimitiveValue, PrimitiveType } from '../../../flow/entityModel';
import { SStruct, SInterface } from '../../../api/schemaModel';

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
 * 内嵌检查上下文
 */
export interface EmbeddingCheckContext {
  fieldType: 'struct' | 'interface';
  struct?: SStruct;
  iface?: SInterface;
  impl?: SStruct;
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

/**
 * 内嵌检查结果
 */
export interface EmbeddingCheckResult {
  canEmbed: boolean;
  reason?: string;
}

/**
 * 字段类型
 */
export type FieldDataType = 'struct' | 'interface';
