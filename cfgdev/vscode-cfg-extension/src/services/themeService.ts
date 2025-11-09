/**
 * Theme Service
 * Manages theme colors for two-layer highlighting (TextMate + Semantic)
 * Supports two palettes: default and chineseClassical
 */

import * as vscode from 'vscode';
import { TEXTMATE_SCOPES } from '../providers/textmateGrammar';

export type ThemeName = 'default' | 'chineseClassical';

export interface ThemeColors {
    // TextMate layer colors
    scopes: {
        keywords: string;
        strings: string;
        numbers: string;
        comments: string;
        operators: string;
        punctuation: string;
    };

    // Semantic layer colors
    semanticTokens: {
        structureDefinition: string;  // struct/interface/table names
        typeIdentifier: string;       // custom types (non-basic)
        fieldName: string;            // field names
        foreignKey: string;           // foreign key references
        primaryKey: string;           // primary key fields
        uniqueKey: string;            // unique key fields
        metadata: string;             // metadata keywords
    };
}

export interface ThemeConfig {
    name: ThemeName;
    colors: ThemeColors;
    metadata: {
        displayName: string;
        description: string;
        isDefault: boolean;
    };
}

export class ThemeService {
    private currentTheme: ThemeName = 'chineseClassical';

    // Theme color palettes
    private themes: Record<ThemeName, ThemeConfig> = {
        default: {
            name: 'default',
            metadata: {
                displayName: 'Default',
                description: 'VSCode standard color scheme',
                isDefault: false
            },
            colors: {
                scopes: {
                    keywords: '#0000FF',        // blue keywords
                    strings: '#A31515',         // red strings
                    numbers: '#098658',         // green numbers
                    comments: '#008000',        // green comments
                    operators: '#795E26',       // brown operators
                    punctuation: '#000000'      // black punctuation
                },
                semanticTokens: {
                    structureDefinition: '#0000FF',    // blue - struct/interface/table
                    typeIdentifier: '#267F99',         // teal - custom types
                    fieldName: '#001080',              // dark blue - field names
                    foreignKey: '#AF00DB',             // purple - foreign keys
                    primaryKey: '#C586C0',             // purple - primary keys
                    uniqueKey: '#C586C0',              // purple - unique keys
                    metadata: '#808080'                // gray - metadata
                }
            }
        },

        chineseClassical: {
            name: 'chineseClassical',
            metadata: {
                displayName: '中国古典色',
                description: 'Traditional Chinese color palette with cultural aesthetics',
                isDefault: true
            },
            colors: {
                scopes: {
                    keywords: '#1E3A8A',        // 黛青 (dark cyan-blue) - keywords
                    strings: '#7C2D12',         // 赭石 (ochre) - strings
                    numbers: '#0F766E',         // 苍青 (sky blue) - numbers
                    comments: '#166534',        // 竹青 (bamboo green) - comments
                    operators: '#7E22CE',       // 紫棠 (purple) - operators
                    punctuation: '#6B7280'      // 墨灰 (ink gray) - punctuation
                },
                semanticTokens: {
                    structureDefinition: '#1E3A8A',  // 黛青 - struct/interface/table
                    typeIdentifier: '#0F766E',       // 苍青 - custom types
                    fieldName: '#0369A1',            // 天蓝 (sky blue) - field names
                    foreignKey: '#BE185D',           // 桃红 (pink) - foreign keys
                    primaryKey: '#7E22CE',           // 紫棠 - primary keys
                    uniqueKey: '#7E22CE',            // 紫棠 - unique keys
                    metadata: '#6B7280'              // 墨灰 - metadata
                }
            }
        }
    };

    constructor() {
        this.loadCurrentTheme();
    }

    /**
     * Load current theme from VSCode configuration
     */
    private loadCurrentTheme(): void {
        const config = vscode.workspace.getConfiguration('cfg');
        const theme = config.get<ThemeName>('theme', 'chineseClassical');
        this.currentTheme = theme;
    }

    /**
     * Get current theme name
     */
    public getCurrentTheme(): ThemeName {
        return this.currentTheme;
    }

    /**
     * Get theme colors by name
     */
    public getThemeColors(themeName?: ThemeName): ThemeConfig {
        if (themeName) {
            return this.themes[themeName];
        }
        return this.themes[this.currentTheme];
    }

    /**
     * Apply theme to VSCode (TextMate layer)
     * Uses tokenColorCustomizations to apply colors to TextMate scopes
     */
    public applyTheme(themeName: ThemeName): void {
        this.currentTheme = themeName;
        const theme = this.themes[themeName];

        const tokenColors = this.generateTokenColorCustomizations(theme);
        const configuration = vscode.workspace.getConfiguration();

        configuration.update(
            'editor.tokenColorCustomizations',
            tokenColors,
            vscode.ConfigurationTarget.Global
        );

        // Also update semantic token colors if supported
        this.updateSemanticTokenColors(theme);
    }

    /**
     * Generate tokenColorCustomizations for TextMate layer
     */
    private generateTokenColorCustomizations(theme: ThemeConfig): unknown {
        return {
            '[*]': {
                'textMateRules': [
                    {
                        'name': 'CFG Keywords',
                        'scope': TEXTMATE_SCOPES.keywords,
                        'settings': {
                            'foreground': theme.colors.scopes.keywords
                        }
                    },
                    {
                        'name': 'CFG Strings',
                        'scope': TEXTMATE_SCOPES.strings,
                        'settings': {
                            'foreground': theme.colors.scopes.strings
                        }
                    },
                    {
                        'name': 'CFG Numbers',
                        'scope': TEXTMATE_SCOPES.numbers,
                        'settings': {
                            'foreground': theme.colors.scopes.numbers
                        }
                    },
                    {
                        'name': 'CFG Comments',
                        'scope': TEXTMATE_SCOPES.comments,
                        'settings': {
                            'foreground': theme.colors.scopes.comments
                        }
                    },
                    {
                        'name': 'CFG Operators',
                        'scope': TEXTMATE_SCOPES.operators,
                        'settings': {
                            'foreground': theme.colors.scopes.operators
                        }
                    },
                    {
                        'name': 'CFG Punctuation',
                        'scope': TEXTMATE_SCOPES.punctuation,
                        'settings': {
                            'foreground': theme.colors.scopes.punctuation
                        }
                    }
                ]
            }
        };
    }

    /**
     * Update semantic token colors
     */
    private updateSemanticTokenColors(_theme: ThemeConfig): void {
        // Note: Semantic token colors are applied directly in the provider
        // This method can be used for future extensions or for theme preview
    }

    /**
     * Get semantic token colors
     */
    public getSemanticTokenColors(themeName?: ThemeName): ThemeColors['semanticTokens'] {
        const theme = this.getThemeColors(themeName);
        return theme.colors.semanticTokens;
    }

    /**
     * List available themes
     */
    public listThemes(): ThemeConfig[] {
        return Object.values(this.themes);
    }

    /**
     * Get color for a specific semantic token type
     */
    public getColorForTokenType(tokenType: number, themeName?: ThemeName): string {
        const colors = this.getSemanticTokenColors(themeName);
        const colorMap: string[] = [
            colors.structureDefinition,  // 0
            colors.typeIdentifier,        // 1
            colors.fieldName,             // 2
            colors.foreignKey,            // 3
            colors.metadata,              // 4 (used as comment color - comments share with metadata)
            colors.metadata,              // 5
            colors.primaryKey,            // 6
            colors.uniqueKey              // 7
        ];
        return colorMap[tokenType] || '#000000';
    }
}
