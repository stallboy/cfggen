import React from 'react'
import ReactDOM from 'react-dom/client'
import {CfgEditorApp} from './CfgEditorApp.tsx'
import './style.css'
import {App} from "antd";

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <App>
            <CfgEditorApp/>
        </App>
    </React.StrictMode>,
)
