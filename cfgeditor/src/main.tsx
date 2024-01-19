import React from 'react'
import ReactDOM from 'react-dom/client'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'

import {CfgEditorApp} from './CfgEditorApp.tsx'
import './style.css'
import {App, ConfigProvider} from "antd";
import './i18n.js'
import {createBrowserRouter, RouterProvider} from "react-router-dom";
import {Index} from './routes/Index.tsx';
import {TableSchema} from "./routes/TableSchema.tsx";
import {TableRef} from "./routes/TableRef.tsx";
import {TableRecord} from "./routes/TableRecord.tsx";
import {TableRecordRefRoute} from "./routes/TableRecordRef.tsx";

const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 1000 * 10,
        },
    },
})


const router = createBrowserRouter([
    {
        path: "/",
        element: <CfgEditorApp/>,
        children: [
            {
                index: true,
                element: <Index/>
            },
            {
                path: "table/:table/:id?",
                Component: TableSchema
            },
            {
                path: "tableRef/:table/:id?",
                Component: TableRef,
            },
            {
                path: "record/:table/:id",
                Component: TableRecord,
                errorElement: <div>Oops! There was an error.</div>,
            },
            {
                path: "recordRef/:table/:id",
                Component: TableRecordRefRoute,
                errorElement: <div>Oops! There was an error.</div>,
            },
        ]
    }
]);


ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <ConfigProvider theme={{
            components: {
                Tabs: {
                    horizontalMargin: '0,0,0,0'
                },
            },
        }}>
            <App>
                <QueryClientProvider client={queryClient}>
                    <RouterProvider router={router}/>
                </QueryClientProvider>
            </App>
        </ConfigProvider>
    </React.StrictMode>
);