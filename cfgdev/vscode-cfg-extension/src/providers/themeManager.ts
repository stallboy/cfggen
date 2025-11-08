/**
 * Theme Manager
 * Singleton that manages theme switching and notifies subscribers
 * Works with ThemeService to handle both TextMate and Semantic token themes
 */

import * as vscode from 'vscode';
import { ThemeName, ThemeService } from '../services/themeService';

export type ThemeChangeCallback = (theme: ThemeName) => void;

export class ThemeManager {
    private static instance: ThemeManager;
    private currentTheme: ThemeName = 'chineseClassical';
    private subscribers: ThemeChangeCallback[] = [];
    private themeService: ThemeService;
    private isListening = false;

    private constructor() {
        this.themeService = new ThemeService();
        this.loadCurrentTheme();
    }

    /**
     * Get singleton instance
     */
    public static getInstance(): ThemeManager {
        if (!ThemeManager.instance) {
            ThemeManager.instance = new ThemeManager();
        }
        return ThemeManager.instance;
    }

    /**
     * Initialize the theme manager
     * Must be called before using the instance
     */
    public initialize(): void {
        if (this.isListening) {
            return;
        }

        // Listen for configuration changes
        vscode.workspace.onDidChangeConfiguration((e) => {
            if (e.affectsConfiguration('cfg.theme')) {
                this.handleThemeChange();
            }
        });

        this.isListening = true;
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
     * Handle theme configuration change
     */
    private handleThemeChange(): void {
        const config = vscode.workspace.getConfiguration('cfg');
        const newTheme = config.get<ThemeName>('theme', 'chineseClassical');

        if (newTheme !== this.currentTheme) {
            const oldTheme = this.currentTheme;
            this.currentTheme = newTheme;

            // Apply the new theme
            this.themeService.applyTheme(newTheme);

            // Notify all subscribers
            this.notifySubscribers(newTheme);

            console.log(`Theme changed from ${oldTheme} to ${newTheme}`);
        }
    }

    /**
     * Notify all subscribers of theme change
     */
    private notifySubscribers(theme: ThemeName): void {
        this.subscribers.forEach(callback => {
            try {
                callback(theme);
            } catch (error) {
                console.error('Error in theme change callback:', error);
            }
        });
    }

    /**
     * Subscribe to theme changes
     * @param callback Function to call when theme changes
     * @returns Disposable to unsubscribe
     */
    public subscribe(callback: ThemeChangeCallback): vscode.Disposable {
        this.subscribers.push(callback);

        // Return disposable for cleanup
        return {
            dispose: () => {
                this.unsubscribe(callback);
            }
        };
    }

    /**
     * Unsubscribe from theme changes
     */
    public unsubscribe(callback: ThemeChangeCallback): void {
        this.subscribers = this.subscribers.filter(cb => cb !== callback);
    }

    /**
     * Get current theme
     */
    public getCurrentTheme(): ThemeName {
        return this.currentTheme;
    }

    /**
     * Get theme service instance
     */
    public getThemeService(): ThemeService {
        return this.themeService;
    }

    /**
     * Refresh all active CFG documents when theme changes
     */
    public refreshActiveDocuments(): void {
        vscode.workspace.textDocuments.forEach(doc => {
            if (doc.languageId === 'cfg') {
                // Trigger semantic tokens refresh
                vscode.commands.executeCommand(
                    'vscode.refreshSemanticTokens',
                    doc.uri
                );
            }
        });
    }

    /**
     * Set theme programmatically
     * @param theme Theme name to set
     */
    public async setTheme(theme: ThemeName): Promise<void> {
        const config = vscode.workspace.getConfiguration('cfg');
        await config.update(
            'cfg.theme',
            theme,
            vscode.ConfigurationTarget.Global
        );
    }

    /**
     * Get all available themes
     */
    public getAvailableThemes(): ThemeName[] {
        return ['default', 'chineseClassical'];
    }

    /**
     * Check if a theme name is valid
     */
    public isValidTheme(theme: string): theme is ThemeName {
        return theme === 'default' || theme === 'chineseClassical';
    }
}
