import React from 'react'
import ReactDOM from 'react-dom/client'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'

import {CfgEditorApp} from './CfgEditorApp.tsx'
import 'reactflow/dist/style.css';
import './style.css'
import {App, ConfigProvider} from "antd";
import './i18n.js'
import {createBrowserRouter, RouterProvider} from "react-router-dom";
import {Table} from "./routes/table/Table.tsx";
import {TableRef} from "./routes/table/TableRef.tsx";
import {Record} from "./routes/record/Record.tsx";
import {RecordRefRoute} from "./routes/record/RecordRef.tsx";

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
