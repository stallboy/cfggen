export class NamespaceUtils {
  /**
   * 解析命名空间标识符
   * @param nsIdentifier 命名空间标识符，如 "task.completecondition"
   * @returns 包含命名空间和名称的对象
   */
  public static parseNamespace(nsIdentifier: string): { namespace: string | null; name: string } {
    const parts = nsIdentifier.split('.');

    if (parts.length === 1) {
      return { namespace: null, name: parts[0] };
    }

    const name = parts.pop()!;
    const namespace = parts.join('.');

    return { namespace, name };
  }

  /**
   * 构建完整的命名空间标识符
   * @param namespace 命名空间
   * @param name 名称
   * @returns 完整的命名空间标识符
   */
  public static buildNamespace(namespace: string | null, name: string): string {
    if (!namespace) {
      return name;
    }
    return `${namespace}.${name}`;
  }

  /**
   * 检查两个命名空间是否匹配
   * @param ns1 第一个命名空间
   * @param ns2 第二个命名空间
   * @returns 是否匹配
   */
  public static namespaceMatches(ns1: string | null, ns2: string | null): boolean {
    if (ns1 === null && ns2 === null) {
      return true;
    }
    if (ns1 === null || ns2 === null) {
      return false;
    }
    return ns1 === ns2;
  }

  /**
   * 从当前命名空间解析相对引用
   * @param currentNamespace 当前命名空间
   * @param reference 引用标识符
   * @returns 解析后的完整命名空间标识符
   */
  public static resolveRelativeReference(currentNamespace: string | null, reference: string): string {
    // 如果引用已经是绝对路径（包含点），直接返回
    if (reference.includes('.')) {
      return reference;
    }

    // 如果没有当前命名空间，返回引用本身
    if (!currentNamespace) {
      return reference;
    }

    // 在当前命名空间下解析相对引用
    return `${currentNamespace}.${reference}`;
  }

  /**
   * 获取命名空间的父级
   * @param namespace 命名空间
   * @returns 父级命名空间，如果没有父级则返回null
   */
  public static getParentNamespace(namespace: string): string | null {
    const parts = namespace.split('.');
    if (parts.length <= 1) {
      return null;
    }
    parts.pop();
    return parts.join('.');
  }

  /**
   * 检查命名空间是否包含另一个命名空间
   * @param parent 父命名空间
   * @param child 子命名空间
   * @returns 是否包含
   */
  public static namespaceContains(parent: string, child: string): boolean {
    if (parent === child) {
      return true;
    }
    return child.startsWith(parent + '.');
  }

  /**
   * 获取命名空间的所有层级
   * @param namespace 命名空间
   * @returns 所有层级的数组
   */
  public static getNamespaceLevels(namespace: string): string[] {
    const parts = namespace.split('.');
    const levels: string[] = [];

    for (let i = 1; i <= parts.length; i++) {
      levels.push(parts.slice(0, i).join('.'));
    }

    return levels;
  }
}