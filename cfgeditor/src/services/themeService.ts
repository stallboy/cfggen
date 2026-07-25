import { BaseDirectory, readFile, exists } from "@tauri-apps/plugin-fs";
import { isTauri } from "@tauri-apps/api/core";

/**
 * 主题配置接口，符合 Antd 主题配置规范
 */
export interface AntdThemeConfig {
    token?: {
        colorPrimary?: string;
        colorBgBase?: string;
        colorTextBase?: string;
        // 更多 token 配置...
    };
    components?: {
        [key: string]: never;
    };
}

// 主题文件读取缓存（模块级，与 clipboard.ts 同写法）：key 为主题文件名
const cache = new Map<string, AntdThemeConfig>();

/**
 * 检查主题文件是否存在
 */
export async function themeExists(themeFile: string): Promise<boolean> {
    if (!isTauri()) {
        // Web 环境暂不支持文件系统操作
        return false;
    }

    try {
        return await exists(themeFile, { baseDir: BaseDirectory.Resource });
    } catch (error) {
        console.error('检查主题文件失败:', error);
        return false;
    }
}

/**
 * 读取主题文件内容
 */
export async function loadTheme(themeFile: string): Promise<AntdThemeConfig | null> {
    if (!themeFile) {
        return null;
    }

    // 检查缓存
    if (cache.has(themeFile)) {
        return cache.get(themeFile) || null;
    }

    if (!isTauri()) {
        // Web 环境暂不支持文件系统操作
        console.warn('Web 环境暂不支持主题文件加载');
        return null;
    }

    try {
        const contentBytes = await readFile(themeFile, { baseDir: BaseDirectory.Resource });
        const content = new TextDecoder().decode(contentBytes);

        const themeConfig = JSON.parse(content) as AntdThemeConfig;

        // 验证主题配置格式
        if (validateThemeConfig(themeConfig)) {
            cache.set(themeFile, themeConfig);
            return themeConfig;
        } else {
            console.error('主题配置格式无效:', themeFile);
            return null;
        }
    } catch (error) {
        console.error('读取主题文件失败:', themeFile, error);
        return null;
    }
}

/**
 * 验证主题配置格式
 */
function validateThemeConfig(config: AntdThemeConfig): config is AntdThemeConfig {
    return (
        typeof config === 'object' &&
        config !== null &&
        (config.token === undefined || typeof config.token === 'object') &&
        (config.components === undefined || typeof config.components === 'object')
    );
}
