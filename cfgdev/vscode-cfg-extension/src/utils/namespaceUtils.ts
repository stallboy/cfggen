/**
 * 解析命名空间标识符
 * 例如: "module.submodule.TypeName" -> ["module", "submodule", "TypeName"]
 */
export function parseNamespaceIdentifier(identifier: string): string[] {
    return identifier.split('.');
}

/**
 * 获取命名空间的短名称
 * 例如: "module.submodule.TypeName" -> "TypeName"
 */
export function getShortName(identifier: string): string {
    const parts = parseNamespaceIdentifier(identifier);
    return parts[parts.length - 1];
}

/**
 * 获取命名空间的父命名空间
 * 例如: "module.submodule.TypeName" -> "module.submodule"
 */
export function getParentNamespace(identifier: string): string | null {
    const parts = parseNamespaceIdentifier(identifier);
    if (parts.length <= 1) {
        return null;
    }
    return parts.slice(0, -1).join('.');
}

/**
 * 检查是否为完整命名空间
 */
export function isFullyQualified(identifier: string): boolean {
    return identifier.includes('.');
}

/**
 * 构建完整命名空间
 */
export function buildNamespace(module: string, name: string): string {
    return `${module}.${name}`;
}

/**
 * 解析类型引用
 */
export function parseTypeReference(ref: string): { module?: string; name: string } {
    const parts = ref.split('.');
    if (parts.length === 1) {
        return { name: parts[0] };
    }
    return {
        module: parts[0],
        name: parts[parts.length - 1]
    };
}
