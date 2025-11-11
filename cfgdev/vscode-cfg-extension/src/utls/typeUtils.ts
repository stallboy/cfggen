/**
 * 类型工具类 - 提供类型相关的工具方法
 */
export class TypeUtils {
    /**
     * 基础类型列表
     */
    private static readonly BASE_TYPES = ['int', 'float', 'long', 'bool', 'str', 'text'];

    public static isCustomType(typeText: string): boolean {
        return !TypeUtils.BASE_TYPES.includes(typeText);
    }

}