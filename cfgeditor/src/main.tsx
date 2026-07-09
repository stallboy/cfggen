import React, { useState, useEffect } from 'react'
import ReactDOM from 'react-dom/client'
import {QueryClientProvider} from '@tanstack/react-query'
import {queryClient} from "./queryClient.ts";

import '@xyflow/react/dist/style.css';
import './style.css'
import {App, ConfigProvider} from "antd";
import './i18n.js'
import {createBrowserRouter} from "react-router";
import {RouterProvider} from "react-router/dom";
import {Table} from "./routes/table/Table.tsx";
import {Record} from "./routes/record/Record.tsx";
import {RecordRefRoute} from "./routes/record/RecordRef.tsx";
import {PathNotFound} from "./routes/PathNotFound.tsx";
import {TableRef} from "./routes/table/TableRef.tsx";
import {AppLoader} from "./AppLoader.tsx";
import {isTauri} from "@tauri-apps/api/core";
import {saveSelfPrefAsync} from "./store/storage.ts";
import {Window} from "@tauri-apps/api/window";
import {useMyStore} from "./store/store.ts";
import {themeService} from "./services/themeService.ts";
// import {ReactQueryDevtools} from "@tanstack/react-query-devtools";


const router = createBrowserRouter([
    {
        path: "/",
        Component: AppLoader,
        children: [
            {
                path: "table/:table/*",
                Component: Table,
            },
            {
                path: "tableRef/:table/*",
                Component: TableRef,
            },
            {
                path: "edit?/record/:table/*",
                Component: Record,
            },
            {
                path: "recordRef/:table/:id",
                Component: RecordRefRoute,
            },
            {
                path: "recordUnref/:table",  // 新增：未引用记录页面路由
                Component: RecordRefRoute,   // 复用RecordRefRoute组件
            },
            {
                path: "*",
                Component: PathNotFound,
            }
        ]
    }
]);

// 默认主题配置
const defaultTheme = {
    components: {
        Tabs: {
            horizontalMargin: '0,0,0,0'
        },
    },
}
if (isTauri()) {
    Window.getCurrent().onCloseRequested(async (event) => {
        // preventDefault 后等自身偏好写盘完成再销毁窗口，避免 fire-and-forget 在写入完成前关窗丢失会话态
        event.preventDefault();
        try {
            await saveSelfPrefAsync();
        } catch {
            // 写盘失败也不能阻止用户关窗
        }
        await Window.getCurrent().destroy();
    });
}


// 动态主题提供者组件
function ThemeProvider({ children }: { children: React.ReactNode }) {
    const { themeConfig } = useMyStore();
    const [currentTheme, setCurrentTheme] = useState(defaultTheme);

    useEffect(() => {
        const loadTheme = async () => {
            if (themeConfig.themeFile) {
                try {
                    const theme = await themeService.loadTheme(themeConfig.themeFile);
                    if (theme) {
                        // 合并自定义主题和默认主题
                        setCurrentTheme({
                            ...defaultTheme,
                            ...theme,
                            components: {
                                ...defaultTheme.components,
                                ...(theme.components || {})
                            }
                        });
                    } else {
                        // 主题文件加载失败，使用默认主题
                        setCurrentTheme(defaultTheme);
                    }
                } catch (error) {
                    console.error('加载主题失败:', error);
                    setCurrentTheme(defaultTheme);
                }
            } else {
                // 没有设置主题文件，使用默认主题
                setCurrentTheme(defaultTheme);
            }
        };

        loadTheme();
    }, [themeConfig.themeFile]);

    return (
        <ConfigProvider theme={currentTheme}>
            {children}
        </ConfigProvider>
    );
}

function MyApp() {

    return (
        <App>
            <QueryClientProvider client={queryClient}>
                <RouterProvider router={router}/>
                {/*<ReactQueryDevtools initialIsOpen={false} />*/}
            </QueryClientProvider>
        </App>
    );
}

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <ThemeProvider>
            <MyApp/>
        </ThemeProvider>
    </React.StrictMode>
);
