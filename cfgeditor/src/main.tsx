import React from 'react'
import ReactDOM from 'react-dom/client'
import {CfgEditorApp} from './CfgEditorApp.tsx'
import './style.css'
import {App, ConfigProvider} from "antd";
import './i18n.js'
import {createBrowserRouter, RouterProvider} from "react-router-dom";
import {Index} from './routes/Index.tsx';


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
                path: "table/:table",
                element: <Index/>
            },
            {
                path: "tableRef/:table",
                element: <Index/>,
            },
            {
                path: "record/:table/:id",
                element: <Index/>,
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
                <RouterProvider router={router}/>
            </App>
        </ConfigProvider>
    </React.StrictMode>,
)
