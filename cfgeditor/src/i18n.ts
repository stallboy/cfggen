import i18n from 'i18next';
import {initReactI18next} from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

i18n.use(LanguageDetector)
    .use(initReactI18next)
    .init({
        // debug: true,
        // lng: 'en',
        fallbackLng: 'en',
        interpolation: {
            escapeValue: false,
        },
        resources: {
            en: {
                translation: {
                    table: "table",
                    tableRef: "table relation",
                    record: "record",
                    recordRef: "relation",

                    serverConnectFail: 'connect failed',
                    netErrFixTip: "run cfgeditor_server.bat to view your own data！ " +
                        "or use another server address to view other's data！",
                    curServer: 'current server',
                    newServer: 'new server',
                    connectNewServer: 'link',
                    connect: 'link',
                    reconnectCurServer: 'reconnect',

                    deleteCurRecord: 'delete cur record',

                    search: 'search',
                    edit: 'edit',
                    view: 'view',
                    addOrUpdate: 'add or update',
                }
            },
            zh: {
                translation: {
                    table: "表结构",
                    tableRef: "表关系",
                    record: "数据",
                    recordRef: "关系",

                    serverConnectFail: '服务器连接失败',
                    netErrFixTip: '请 启动 cfgeditor服务器.bat，查看自己的配表！ 或 更改服务器地址，查看别人的配表！',
                    curServer: '当前服务器',
                    newServer: '新服务器',
                    connectNewServer: '连接新服',
                    connect: '连接',
                    reconnectCurServer: '重连当前服务器',

                    implsShowCnt: '接口实现数',
                    refIn: '入层',
                    refOutDepth: '出层',
                    maxNode: '节点数',
                    recordRefIn: '数据入层',
                    recordRefOutDepth: '数据出层',
                    recordMaxNode: '数据节点数',
                    searchMaxReturn: '搜索返回数',
                    deleteCurRecord: '删除当前数据',

                    search: '搜索',
                    edit: '编辑',
                    view: '浏览',
                    addOrUpdate: '更新',


                }
            }
        }
    });

export default i18n;
