import {defineConfig} from 'vitest/config'
import react from '@vitejs/plugin-react'
import {fileURLToPath, URL} from 'node:url'

// 仅用于单元测试的配置，与生产构建 (vite.config.ts) 解耦。
// - react 插件：编译被测的 .tsx（如 domain/schema.tsx）
// - jsdom：提供 window/document，让 antd / resso / @tauri-apps/api 等可在测试环境导入
// - setupFiles：补上 Tauri 运行时 shim（见 src/test/setup.ts）
export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url)),
        },
    },
    test: {
        environment: 'jsdom',
        setupFiles: ['./src/test/setup.ts'],
        include: ['src/**/*.{test,spec}.{ts,tsx}'],
        // 测试用显式 import { describe, it, expect } from 'vitest'，不开启 globals
    },
})
