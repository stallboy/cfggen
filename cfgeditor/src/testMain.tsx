import React from 'react'
import ReactDOM from 'react-dom/client'
import './style.css'
import {App, ConfigProvider} from "antd";
import './i18n.js'
import {Test} from "./Test.tsx";
import {produce} from 'immer';


const initialUser = {
    "id": 1,
    "name": "Leanne Graham",
    "username": "Bret",
    "email": "Sincere@april.biz",
    "address": {
        "street": "Kulas Light",
        "suite": "Apt. 556",
        "city": "Gwenborough",
        "zipcode": "92998-3874",
        "geo": {
            "lat": "-37.3159",
            "lng": "81.1496"
        }
    },
    "phone": "1-770-736-8031 x56442",
    "website": "hildegard.org",
    "company": {
        "name": "Romaguera-Crona",
        "catchPhrase": "Multi-layered client-server neural-net",
        "bs": "harness real-time e-markets"
    }
};


const copy2 = produce(initialUser, (draft: any) => {
    draft.address.geo.lat = 'lat';
});

console.log("company", copy2.company === initialUser.company); // true
console.log("address", copy2.address === initialUser.address); // false
console.log("address street", copy2.address.street === initialUser.address.street); // true
console.log("address geo lng", copy2.address.geo.lng === initialUser.address.geo.lng); // true
console.log("address geo lat", copy2.address.geo.lat, copy2.address.geo.lat === initialUser.address.geo.lat); // false


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
