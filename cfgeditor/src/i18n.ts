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
                    nextSlot: "next slot:",
                    table: "table",
                    tableRef: "table relation",
                    record: "record",
                    recordRef: "relation",
                    fix: "fix",


                    serverConnectFail: 'connect failed',
                    netErrFixTip: "run cfgeditor_server.bat to view your own data！ " +
                        "or use another server address to view other's data！",
                    curServer: 'current server',
                    newServer: 'new server',
                    connectNewServer: 'link',
                    connect: 'link',
                    reconnectCurServer: 'reconnect',

                    deleteCurRecord: 'delete cur record',
                    addFix: 'add fix',
                    removeFix: 'remove fix',
                    toPng: 'save relation png',

                    addKeywordColor: 'add keyword color',
                    setNodeShow: 'set node show',

                    search: 'search',
                    edit: 'edit',
                    view: 'view',
                    addOrUpdate: 'add or update',
                    setDefaultValue: 'clear',
                }
            },
            zh: {
                translation: {
                    nextSlot: "空位:",
                    table: "表",
                    tableRef: "表关系",
                    record: "数据",
                    recordRef: "关系",
                    fix: "固定",

                    serverConnectFail: '服务器连接失败',
                    netErrFixTip: '请 启动 cfgeditor服务器.bat，查看自己的配表！ 或 更改服务器地址，查看别人的配表！',
                    curServer: '当前服务器',
                    newServer: '新服务器',
                    connectNewServer: '连接新服',
                    connect: '连接',
                    reconnectCurServer: '重连当前服务器',

                    tableSetting: '表显示',
                    fixedPagesSetting: '固定页面',
                    recordSetting: '关系显示',
                    operations: '操作',
                    keySetting: '快捷键',
                    appSetting: '资源',
                    implsShowCnt: '接口实现数',
                    refIn: '入层',
                    refOutDepth: '出层',
                    maxNode: '节点数',
                    recordRefIn: '数据入层',
                    recordRefOutDepth: '数据出层',
                    recordMaxNode: '数据节点数',

                    SIMPLE: '中间',
                    LINEAR_SEGMENTS: '中间起伏',
                    BRANDES_KOEPF: '默认',

                    searchMaxReturn: '搜索返回数',
                    imageSizeScale: '图片缩放倍数',
                    dragPanel: '可拖拽面板',
                    pages: '页面',
                    setFixedPagesConf: '设置固定页面',
                    fixCurrentPage: '固定当前页面',
                    none: '不要',
                    toPng: '保存图片',

                    deleteCurRecord: '删除当前数据',

                    nodeShowSetting: '节点显示设置',
                    showHead: '节点标头',
                    showCopyable: '显示并可复制',
                    containEnum: '包含枚举',
                    showDescription: '描述',
                    show: '显示',
                    showFallbackValue: '无则显示值',
                    showValue: '全值',
                    nodePlacementStrategy: '节点布局',
                    keywordColors: '关键词颜色',
                    addKeywordColor: '增加',
                    tableHideAndColors: '表显示',
                    addTableHideAndColor: '增加',
                    setNodeShow: '提交',
                    toggleFullScreen: '切换全屏',
                    tauriConf: 'App设置',
                    resDirs: '资源目录',
                    addResDir: '增加',
                    setTauriConf: '提交',
                    summarizeRes: '资源分析',
                    cancelUpdateNote: '取消',
                    updateNote: '更新',

                    search: '搜索',
                    edit: '编辑',
                    view: '浏览',
                    addOrUpdate: '更新',
                    setDefaultValue: '清空',
                }
            }
        }
    });

export default i18n;
