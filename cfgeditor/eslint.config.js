import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
    globalIgnores(['dist', 'src-tauri', 'src/store/storageJson.ts']),
    {
        files: ['**/*.{ts,tsx}'],
        extends: [
            js.configs.recommended,
            tseslint.configs.recommended,
            reactHooks.configs.flat.recommended,
            reactRefresh.configs.vite,
        ],
        languageOptions: {
            ecmaVersion: 2020,
            globals: globals.browser,
        },
    },
    {
        // resso 是 vendored 第三方状态库，其核心设计在 Proxy getter 中调用 React hook，无法满足 rules-of-hooks
        files: ['src/store/resso.ts'],
        rules: {
            'react-hooks/rules-of-hooks': 'off',
        },
    },
    {
        // main.tsx 是应用入口（ReactDOM.render），不参与 react fast refresh 的组件导出约定
        files: ['src/main.tsx'],
        rules: {
            'react-refresh/only-export-components': 'off',
        },
    },
])
