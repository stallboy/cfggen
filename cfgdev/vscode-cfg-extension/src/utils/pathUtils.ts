/**
 * 路径工具类 - 提供统一的路径处理功能
 */
export class PathUtils {
    /**
     * 获取文件路径的目录部分
     */
    static getDirname(filePath: string): string {
        const parts = filePath.split(/[\\/]/);
        parts.pop();
        return parts.join('/');
    }

    /**
     * 获取文件路径的基本名称
     * @param ext 可选的文件扩展名，如果提供会从结果中移除
     */
    static getBasename(filePath: string, ext?: string): string {
        const parts = filePath.split(/[\\/]/);
        let basename = parts[parts.length - 1];
        if (ext && basename.endsWith(ext)) {
            basename = basename.substring(0, basename.length - ext.length);
        }
        return basename;
    }

    /**
     * 拼接多个路径部分
     */
    static joinPath(...paths: string[]): string {
        return paths.join('/');
    }

    /**
     * 计算两个路径之间的相对路径
     */
    static getRelativePath(fromDir: string, toDir: string): string {
        const fromParts = fromDir.split(/[\\/]/).filter(part => part !== '');
        const toParts = toDir.split(/[\\/]/).filter(part => part !== '');

        let i = 0;
        while (i < fromParts.length && i < toParts.length && fromParts[i] === toParts[i]) {
            i++;
        }

        const relativeParts: string[] = [];
        for (let j = i; j < fromParts.length; j++) {
            relativeParts.push('..');
        }

        for (let j = i; j < toParts.length; j++) {
            relativeParts.push(toParts[j]);
        }

        return relativeParts.join('/') || '.';
    }
}