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

/**
 * 主题服务类，负责主题文件的读取和管理
 */
export class ThemeService {
    private static instance: ThemeService;
    private cache = new Map<string, AntdThemeConfig>();

    private constructor() {}

    public static getInstance(): ThemeService {
        if (!ThemeService.instance) {
            ThemeService.instance = new ThemeService();
        }
        return ThemeService.instance;
    }

    /**
     * 获取主题文件路径
     */
    private getThemePath(themeFile: string): string {
        return themeFile;
    }

    /**
     * 检查主题文件是否存在
     */
    async themeExists(themeFile: string): Promise<boolean> {
        if (!isTauri()) {
            // Web 环境暂不支持文件系统操作
            return false;
        }

        try {
            const themePath = this.getThemePath(themeFile);
            return await exists(themePath, { baseDir: BaseDirectory.Resource });
        } catch (error) {
            console.error('检查主题文件失败:', error);
            return false;
        }
    }

    /**
     * 读取主题文件内容
     */
    async loadTheme(themeFile: string): Promise<AntdThemeConfig | null> {
        if (!themeFile) {
            return null;
        }

        // 检查缓存
        if (this.cache.has(themeFile)) {
            return this.cache.get(themeFile) || null;
        }

        if (!isTauri()) {
            // Web 环境暂不支持文件系统操作
            console.warn('Web 环境暂不支持主题文件加载');
            return null;
        }

        try {
            const themePath = this.getThemePath(themeFile);
            const contentBytes = await readFile(themePath, { baseDir: BaseDirectory.Resource });
            const content = new TextDecoder().decode(contentBytes);

            const themeConfig = JSON.parse(content) as AntdThemeConfig;

            // 验证主题配置格式
            if (this.validateThemeConfig(themeConfig)) {
                this.cache.set(themeFile, themeConfig);
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
    private validateThemeConfig(config: AntdThemeConfig): config is AntdThemeConfig {
        return (
            typeof config === 'object' &&
            config !== null &&
            (config.token === undefined || typeof config.token === 'object') &&
            (config.components === undefined || typeof config.components === 'object')
        );
    }
}

// 导出单例实例
export const themeService = ThemeService.getInstance();