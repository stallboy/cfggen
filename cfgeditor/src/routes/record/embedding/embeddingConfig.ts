/**
 * 内嵌显示配置 - 统一管理所有内嵌相关的阈值和规则
 *
 * 将分散在代码中的魔数集中管理，便于调整内嵌规则
 */
export const EMBEDDING_CONFIG = {
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

/**
 * 原始类型集合
 */
export const PRIMITIVE_TYPES = new Set<string>(['bool', 'int', 'long', 'float', 'str', 'text']);

/**
 * 数字类型集合
 */
export const NUMBER_TYPES = new Set<string>(['int', 'long', 'float']);

/**
 * 判断是否为原始类型
 */
export function isPrimitiveType(type: string): boolean {
  return PRIMITIVE_TYPES.has(type);
}

/**
 * 判断是否为数字类型
 */
export function isNumberType(type: string): boolean {
  return NUMBER_TYPES.has(type);
}
