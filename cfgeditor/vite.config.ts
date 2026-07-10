import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'
import {fileURLToPath, URL} from 'node:url'


// https://vitejs.dev/config/
export default defineConfig({
    plugins: [react({
        babel: {
            plugins: [
                ["babel-plugin-react-compiler"],
            ],
        },
    })],

    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url)),
        },
    },

    // prevent vite from obscuring rust errors
    clearScreen: false,
    // Tauri expects a fixed port, fail if that port is not available
    server: {
        // 5173 落在 Windows Hyper-V/WSL2 动态保留端口范围 (5041–5240) 内，绑定会 EACCES；
        // 改用 Tauri 官方模板默认端口 1420，远离所有保留区间
        port: 1420,
        strictPort: true,
    },
    // to access the Tauri environment variables set by the CLI with information about the current target
    envPrefix: ['VITE_', 'TAURI_PLATFORM', 'TAURI_ARCH', 'TAURI_FAMILY', 'TAURI_PLATFORM_VERSION', 'TAURI_PLATFORM_TYPE', 'TAURI_DEBUG'],
    build: {
        // Tauri uses Chromium on Windows and WebKit on macOS and Linux
        target: process.env.TAURI_PLATFORM == 'windows' ? 'chrome105' : 'safari13',
        // don't minify for debug builds
        minify: !process.env.TAURI_DEBUG ? 'oxc' : false,
        // produce sourcemaps for debug builds
        sourcemap: !!process.env.TAURI_DEBUG,
        chunkSizeWarningLimit: 7000,
        modulePreload: {
            polyfill: false
        },

    },

})
