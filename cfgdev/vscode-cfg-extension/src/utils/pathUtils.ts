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
     */
    static getBasename(filePath: string): string {
        const parts = filePath.split(/[\\/]/);
        return parts[parts.length - 1];
    }

    /**
     * 拼接多个路径部分
     */
    static joinPath(...paths: string[]): string {
        return paths.join('/');
    }

}
