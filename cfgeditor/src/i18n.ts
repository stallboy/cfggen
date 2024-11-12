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

                    addColor: 'add color',
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
                    record: "记录",
                    recordRef: "记录关系",
                    fix: "固定",

                    serverConnectFail: '服务器连接失败',
                    netErrFixTip: '请 启动 cfgeditor服务器.bat，查看自己的配表！ 或 更改服务器地址，查看别人的配表！',
                    curServer: '当前服务器',
                    newServer: '新服务器',
                    connectNewServer: '连接新服',
                    connect: '连接',
                    reconnectCurServer: '重连当前服务器',

                    basicSetting: '基础',
                    recordShowSetting: '显示',
                    serverAndAi: 'ai',
                    operations: '操作',
                    keySetting: '快捷键',
                    appSetting: '资源',
                    implsShowCnt: '接口实现数',
                    refIn: '入层',
                    recordRefInShowLinkMaxNode: '入层显示链接的最大节点数',
                    refOutDepth: '出层',
                    maxNode: '节点数',
                    recordRefIn: '数据入层',
                    recordRefOutDepth: '数据出层',
                    recordMaxNode: '数据节点数',
                    isNextIdShow:'显示下一个空Id',

                    SIMPLE: '中间',
                    LINEAR_SEGMENTS: '中间起伏',
                    BRANDES_KOEPF: '默认',
                    mrtree:'树状',

                    searchMaxReturn: '搜索返回数',
                    imageSizeScale: '图片缩放倍数',
                    dragPanel: '可拖拽面板',
                    pages: '页面',
                    setFixedPagesConf: '设置固定页面',
                    fixCurrentPage: '固定当前页面',
                    none: '空',
                    toPng: '保存图片',

                    deleteCurRecord: '删除当前数据',

                    refIsShowCopyable: '标题复制',
                    refContainEnum: '包含枚举',

                    refShowDescription: '关系内容',
                    showDescription: '描述',
                    show: '显示',
                    showFallbackValue: '无则显示值',
                    showValue: '全值',

                    recordLayout: '记录布局',
                    editLayout: '编辑布局',
                    refLayout: '关系布局',
                    tableLayout: '表布局',
                    tableRefLayout: '表关系布局',

                    nodeColorsByLabel: '节点色<标题',
                    nodeColorsByValue: '节点色<值',
                    editFoldColor: '折叠背景色',
                    refTableHides: '表隐藏',
                    fieldColorsByName: '字段色<名',
                    addColor: '增加',
                    addTableHide: '增加',


                    setNodeShow: '提交',
                    toggleFullScreen: '切换全屏',
                    tauriConf: 'App设置',
                    resDirs: '资源目录',
                    addResDir: '增加',
                    setTauriConf: '提交',
                    summarizeRes: '资源分析',
                    reloadRes: '重新加载资源',
                    cancelUpdateNote: '取消',
                    updateNote: '更新',

                    search: '搜索',
                    edit: '编辑',
                    view: '浏览',
                    addOrUpdate: '更新',
                    setDefaultValue: '清空',

                    addListItemBefore: '前插入',
                    structCopy: '拷贝',
                    structPaste: '粘贴',

                    lastAccessed: '访问历史',
                    lastModified: '修改历史'
                }
            }
        }
    });

export default i18n;
