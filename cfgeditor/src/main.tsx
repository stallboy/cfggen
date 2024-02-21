import React from 'react'
import ReactDOM from 'react-dom/client'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'

import '@xyflow/react/dist/style.css';
import './style.css'
import {App, ConfigProvider} from "antd";
import './i18n.js'
import {createBrowserRouter, RouterProvider} from "react-router-dom";
import {Table} from "./routes/table/Table.tsx";
import {Record} from "./routes/record/Record.tsx";
import {RecordRefRoute} from "./routes/record/RecordRef.tsx";
import {PathNotFound} from "./routes/PathNotFound.tsx";
import {TableRef} from "./routes/table/TableRef.tsx";
import {AppLoader} from "./AppLoader.tsx";
import {CfgEditorApp} from "./CfgEditorApp.tsx";
// import {ReactQueryDevtools} from "@tanstack/react-query-devtools";

export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 1000 * 10,
        },
    },
})

const router = createBrowserRouter([
    {
        path: "/",
        Component: window.__TAURI__ ? AppLoader : CfgEditorApp,
        children: [
            {
                path: "table/:table/:id?",
                Component: Table,
            },
            {
                path: "tableRef/:table/:id?",
                Component: TableRef,
            },
            {
                path: "record/:table/:id/edit?",
                Component: Record,
            },
            {
                path: "recordRef/:table/:id",
                Component: RecordRefRoute,
            },
            {
                path: "*",
                Component: PathNotFound,
            }
        ]
    }
]);

const theme = {
    components: {
        Tabs: {
            horizontalMargin: '0,0,0,0'
        },
    },
}

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <ConfigProvider theme={theme}>
            <App>
                <QueryClientProvider client={queryClient}>
                    <RouterProvider router={router}/>
                    {/*<ReactQueryDevtools initialIsOpen={false} />*/}
                </QueryClientProvider>
            </App>
        </ConfigProvider>
    </React.StrictMode>
);
