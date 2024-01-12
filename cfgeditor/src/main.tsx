import React from 'react'
import ReactDOM from 'react-dom/client'
import {CfgEditorApp} from './CfgEditorApp.tsx'
import './style.css'
import {App, ConfigProvider} from "antd";
import './i18n.js'

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
                <CfgEditorApp/>
            </App>
        </ConfigProvider>
    </React.StrictMode>,
)
