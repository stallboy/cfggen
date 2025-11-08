/**
 * 命名空间工具函数
 */

/**
 * 解析命名空间
 * @param namespace 完整命名空间（如 "task.player.Position"）
 * @returns 解析结果
 */
export function parseNamespace(namespace: string): {
    module: string;
    qualifier?: string;
    name: string;
} {
    const parts = namespace.split('.');
    const module = parts[0];
    const name = parts[parts.length - 1];
    const qualifier = parts.length > 2 ? parts.slice(1, -1).join('.') : undefined;

    return { module, qualifier, name };
}

/**
 * 构建完整命名空间
 * @param module 模块名
 * @param name 名称
 * @param qualifier 限定符（可选）
 * @returns 完整命名空间
 */
export function buildNamespace(module: string, name: string, qualifier?: string): string {
    if (qualifier) {
        return `${module}.${qualifier}.${name}`;
    }
    return `${module}.${name}`;
}

/**
 * 提取模块名
 * @param namespace 命名空间
 * @returns 模块名
 */
export function getModuleName(namespace: string): string {
    const parts = namespace.split('.');
    return parts[0] || namespace;
}

/**
 * 提取短名称（不包含模块前缀）
 * @param namespace 命名空间
 * @returns 短名称
 */
export function getShortName(namespace: string): string {
    const parts = namespace.split('.');
    return parts[parts.length - 1] || namespace;
}

/**
 * 检查命名空间是否匹配
 * @param namespace1 命名空间1
 * @param namespace2 命名空间2
 * @param matchModule 是否需要匹配模块
 * @returns 是否匹配
 */
export function matchNamespace(
    namespace1: string,
    namespace2: string,
    matchModule: boolean = true
): boolean {
    if (matchModule) {
        return namespace1 === namespace2;
    }

    // 只匹配短名称
    return getShortName(namespace1) === getShortName(namespace2);
}

/**
 * 获取命名空间层级
 * @param namespace 命名空间
 * @returns 层级数
 */
export function getNamespaceDepth(namespace: string): number {
    return namespace.split('.').length;
}

/**
 * 验证命名空间格式
 * @param namespace 命名空间
 * @returns 是否有效
 */
export function isValidNamespace(namespace: string): boolean {
    if (!namespace || namespace.trim() === '') {
        return false;
    }

    // 不能以点开始或结束
    if (namespace.startsWith('.') || namespace.endsWith('.')) {
        return false;
    }

    // 不能有连续的点
    if (namespace.includes('..')) {
        return false;
    }

    // 不能包含空白字符
    if (/\s/.test(namespace)) {
        return false;
    }

    return true;
}

/**
 * 获取命名空间的所有可能匹配
 * 用于模糊匹配
 * @param namespace 命名空间
 * @returns 可能的匹配模式
 */
export function getNamespacePatterns(namespace: string): string[] {
    const patterns: string[] = [];
    const parts = namespace.split('.');

    // 完整命名空间
    patterns.push(namespace);

    // 短名称
    patterns.push(getShortName(namespace));

    // 模块.短名称
    patterns.push(`${getModuleName(namespace)}.${getShortName(namespace)}`);

    return patterns;
}
