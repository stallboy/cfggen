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

    // Semantic layer colors (must match SemanticTokensProvider.TOKEN_TYPES)
    semanticTokens: {
        structureDefinition: string;  // 0: struct/interface/table names
        typeIdentifier: string;       // 1: custom types (non-basic)
        foreignKey: string;           // 2: foreign key references
        comment: string;              // 3: comments
        metadata: string;             // 4: metadata keywords
        primaryKey: string;           // 5: primary key fields
        uniqueKey: string;            // 6: unique key fields
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
                    foreignKey: '#AF00DB',             // purple - foreign keys
                    comment: '#008000',                // green - comments
                    metadata: '#808080',               // gray - metadata
                    primaryKey: '#C586C0',             // purple - primary keys
                    uniqueKey: '#C586C0'               // purple - unique keys
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
                    foreignKey: '#BE185D',           // 桃红 (pink) - foreign keys
                    comment: '#166534',              // 竹青 - comments
                    metadata: '#6B7280',             // 墨灰 - metadata
                    primaryKey: '#7E22CE',           // 紫棠 - primary keys
                    uniqueKey: '#7E22CE'             // 紫棠 - unique keys
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
     * Apply theme to VSCode (TextMate + Semantic layers)
     * Updates both TextMate scopes and semantic token colors
     */
    public applyTheme(themeName: ThemeName): void {
        this.currentTheme = themeName;
        const theme = this.themes[themeName];

        // Apply TextMate layer colors
        const tokenColors = this.generateTokenColorCustomizations(theme);
        const configuration = vscode.workspace.getConfiguration();

        configuration.update(
            'editor.tokenColorCustomizations',
            tokenColors,
            vscode.ConfigurationTarget.Global
        );

        // Apply semantic token colors
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
     * Applies semantic token color customizations to VSCode settings
     */
    private updateSemanticTokenColors(theme: ThemeConfig): void {
        const semanticColors = theme.colors.semanticTokens;

        // Configure semantic token color customizations
        const semanticTokenColorCustomizations = {
            '[*]': {
                'semanticTokenColorCustomizations': {
                    '[cfg]': {
                        'rules': {
                            'structureDefinition': {
                                'foreground': semanticColors.structureDefinition
                            },
                            'typeIdentifier': {
                                'foreground': semanticColors.typeIdentifier
                            },
                            'foreignKey': {
                                'foreground': semanticColors.foreignKey
                            },
                            'comment': {
                                'foreground': semanticColors.comment
                            },
                            'metadata': {
                                'foreground': semanticColors.metadata
                            },
                            'primaryKey': {
                                'foreground': semanticColors.primaryKey
                            },
                            'uniqueKey': {
                                'foreground': semanticColors.uniqueKey
                            }
                        }
                    }
                }
            }
        };

        const configuration = vscode.workspace.getConfiguration();
        configuration.update(
            'editor.semanticTokenColorCustomizations',
            semanticTokenColorCustomizations,
            vscode.ConfigurationTarget.Global
        );
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
     * tokenType index must match SemanticTokensProvider.TOKEN_TYPES
     */
    public getColorForTokenType(tokenType: number, themeName?: ThemeName): string {
        const colors = this.getSemanticTokenColors(themeName);
        // Color map matches SemanticTokensProvider.TOKEN_TYPES index
        const colorMap: string[] = [
            colors.structureDefinition,  // 0: structureDefinition
            colors.typeIdentifier,        // 1: typeIdentifier
            colors.foreignKey,            // 2: foreignKey
            colors.comment,               // 3: comment
            colors.metadata,              // 4: metadata
            colors.primaryKey,            // 5: primaryKey
            colors.uniqueKey              // 6: uniqueKey
        ];
        return colorMap[tokenType] || '#000000';
    }
}
