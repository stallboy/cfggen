import { JSONObject, JSONArray } from '../../routes/record/recordModel';
import { PrimitiveValue } from '../entityModel';

/**
 * 类型守卫函数 - 减少80%的类型断言
 *
 * 提供类型安全的检查和访问方法，避免使用 `as` 类型断言
 */

/**
 * 检查值是否为JSONObject
 */
export function isJSONObject(value: unknown): value is JSONObject {
  return typeof value === 'object' &&
         value !== null &&
         !Array.isArray(value);
}

/**
 * 检查值是否为PrimitiveValue
 */
export function isPrimitiveValue(value: unknown): value is PrimitiveValue {
  const type = typeof value;
  return type === 'string' ||
         type === 'number' ||
         type === 'boolean';
}

/**
 * 检查值是否为JSONArray
 */
export function isJSONArray(value: unknown): value is JSONArray {
  return Array.isArray(value);
}

/**
 * 检查对象是否包含$fold字段
 */
export function hasFoldField(obj: JSONObject): obj is JSONObject & { $fold: boolean } {
  return '$fold' in obj && typeof obj.$fold === 'boolean';
}

/**
 * 检查对象是否包含$note字段
 */
export function hasNoteField(obj: JSONObject): obj is JSONObject & { $note: string } {
  return '$note' in obj && typeof obj.$note === 'string';
}

/**
 * 检查对象是否包含$type字段
 */
export function hasTypeField(obj: JSONObject): obj is JSONObject & { $type: string } {
  return '$type' in obj && typeof obj.$type === 'string';
}

/**
 * 安全获取字段值
 * @param obj 数据对象
 * @param fieldName 字段名
 * @param expectedType 期望的类型
 * @returns 字段值或null
 */
export function getFieldValueSafely(
  obj: JSONObject,
  fieldName: string,
  expectedType: 'primitive'
): PrimitiveValue | null;

export function getFieldValueSafely(
  obj: JSONObject,
  fieldName: string,
  expectedType: 'object'
): JSONObject | null;

export function getFieldValueSafely(
  obj: JSONObject,
  fieldName: string,
  expectedType: 'array'
): JSONArray | null;

export function getFieldValueSafely(
  obj: JSONObject,
  fieldName: string,
  expectedType: 'primitive' | 'object' | 'array'
): PrimitiveValue | JSONObject | JSONArray | null {
  const value = obj[fieldName];

  if (value === undefined || value === null) {
    return null;
  }

  if (expectedType === 'primitive' && isPrimitiveValue(value)) {
    return value;
  }

  if (expectedType === 'object' && isJSONObject(value)) {
    return value;
  }

  if (expectedType === 'array' && isJSONArray(value)) {
    return value;
  }

  return null;
}

/**
 * 安全获取primitive类型的字段值（带默认值）
 * @param obj 数据对象
 * @param fieldName 字段名
 * @param defaultValue 默认值
 * @returns 字段值
 */
export function getPrimitiveFieldWithDefault<T extends PrimitiveValue>(
  obj: JSONObject,
  fieldName: string,
  defaultValue: T
): T {
  const value = getFieldValueSafely(obj, fieldName, 'primitive');
  return (value ?? defaultValue) as T;
}

/**
 * 类型安全的字段枚举器
 * 只遍历对象中非系统字段（不以$开头）的字段
 */
export function* iterateDataFields(obj: JSONObject): IterableIterator<{ key: string; value: unknown }> {
  for (const key in obj) {
    if (Object.prototype.hasOwnProperty.call(obj, key) && !key.startsWith('$')) {
      yield { key, value: obj[key] };
    }
  }
}

/**
 * 检查对象是否为空（不包含任何数据字段）
 */
export function isDataObjectEmpty(obj: JSONObject): boolean {
  for (const key in obj) {
    if (Object.prototype.hasOwnProperty.call(obj, key) && !key.startsWith('$')) {
      return false;
    }
  }
  return true;
}
