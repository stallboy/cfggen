/**
 * 主题配置
 */
export interface ThemeConfig {
    name: 'default' | 'chineseClassical';
    colors: {
        // 1. 结构定义
        structureDefinition: string;    // struct/interface/table + 名称

        // 2. 复杂结构类型声明
        complexType: string;            // 自定义类型（Position等）

        // 3. 主键字段名称
        primaryKey: string;             // PK字段

        // 4. 唯一键字段名称
        uniqueKey: string;              // UK字段

        // 5. 外键引用
        foreignKey: string;             // -> tt, -> tt[kk], => tt[kk]

        // 6. 注释
        comment: string;                // 注释

        // 7. 特定元数据
        metadata: string;               // nullable, mustFill等
    };
}

/**
 * 主题配置预设
 */
export const THEMES: Record<'default' | 'chineseClassical', ThemeConfig> = {
    default: {
        name: 'default',
        colors: {
            // 1. 结构定义
            structureDefinition: '#0000FF',    // 蓝色

            // 2. 复杂结构类型声明
            complexType: '#267F99',            // 深蓝青

            // 3. 主键字段名称
            primaryKey: '#C586C0',             // 紫色

            // 4. 唯一键字段名称
            uniqueKey: '#C586C0',              // 紫色

            // 5. 外键引用
            foreignKey: '#AF00DB',             // 紫红

            // 6. 注释
            comment: '#008000',                // 绿色

            // 7. 特定元数据
            metadata: '#808080'                // 灰色
        }
    },

    chineseClassical: {
        name: 'chineseClassical',
        colors: {
            // 1. 结构定义
            structureDefinition: '#1E3A8A',    // 黛青 - struct/interface/table + 名称

            // 2. 复杂结构类型声明
            complexType: '#0F766E',            // 苍青 - 自定义类型

            // 3. 主键字段名称
            primaryKey: '#7E22CE',             // 紫棠 - PK字段

            // 4. 唯一键字段名称
            uniqueKey: '#7E22CE',              // 紫棠 - UK字段

            // 5. 外键引用
            foreignKey: '#BE185D',             // 桃红 - 外键引用

            // 6. 注释
            comment: '#166534',                // 竹青 - 注释

            // 7. 特定元数据
            metadata: '#6B7280'                // 墨灰 - 元数据
        }
    }
};

/**
 * 获取主题配置
 * @param themeName 主题名称
 * @returns 主题配置
 */
export function getThemeConfig(themeName: 'default' | 'chineseClassical' = 'chineseClassical'): ThemeConfig {
    return THEMES[themeName];
}
