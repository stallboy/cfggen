import React from 'react'
import ReactDOM from 'react-dom/client'
import './style.css'
import {App, ConfigProvider} from "antd";
import './i18n.js'
import {Test} from "./Test.tsx";

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
                <Test/>
            </App>
        </ConfigProvider>
    </React.StrictMode>,
)
