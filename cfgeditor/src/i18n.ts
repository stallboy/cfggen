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

                    themeSetting: 'Theme Setting',
                    themeFile: 'Theme File',
                    themeFileHelp: 'Theme file name, e.g. colourpurple.json',
                    themeFileNotFound: 'Theme file not found',
                    themeFileExists: 'Theme file exists',
                    themeFileValid: 'Theme file is valid',
                    themeFileInvalid: 'Theme file is invalid',
                    themeSettingSaved: 'Theme setting saved',
                    themeSettingFailed: 'Theme setting failed',
                    themeTestFailed: 'Theme test failed',
                    themeChangeHint: 'Theme updated, may need to refresh page',
                    themeSettingNote: 'Theme Setting Note',
                    themeSettingDescription: 'Set theme file name, theme files should be placed in the same directory as cfgeditor.yml',
                    pleaseSetThemeFile: 'Please set theme file first',
                    testTheme: 'Test Theme',
                    save: 'Save',

                    // Flow Visualization Settings
                    nodeDimensions: 'Node Dimensions',
                    nodeWidth: 'Node Width',
                    editNodeWidth: 'Edit Node Width',
                    edgeStyling: 'Edge Styling',
                    edgeColor: 'Edge Color',
                    edgeStrokeWidth: 'Edge Stroke Width',
                    layoutSpacing: 'Layout Spacing',
                    mrtreeSpacing: 'Tree Layout Spacing',
                    mrtreeSpacingTooltip: 'Spacing between nodes in tree layout',
                    layeredSpacing: 'Nodes Spacing In Same Layer ',
                    layeredSpacingTooltip: 'Spacing between nodes in layered layout',
                    layeredNodeSpacing: 'Layers Spacing',
                    layeredNodeSpacingTooltip: 'Spacing between nodes in different layers'
                }
            },
            zh: {
                translation: {
                    nextSlot: "空位:",
                    table: "表",
                    tableRef: "表关系",
                    record: "记录",
                    recordRef: "关系",
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
                    refIdsInDepth: 'Id入层',
                    refIdsOutDepth: 'Id出层',
                    refIdsMaxNode: 'Id总数',
                    refIdList: '关联数据',

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
                    fieldColorsByName: '字段色<名',
                    editFoldColor: '折叠背景色',

                    refIsShowCopyable: 'ref标题复制',
                    refShowDescription: 'ref内容',
                    refContainEnum: 'ref显示<枚举',
                    refTableHides: 'ref隐藏<表名',
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
                    addOrUpdateTooltip: '增加或更新记录,alt+s',
                    setDefaultValue: '清空',

                    addListItemBefore: '前插入',
                    structCopy: '拷贝',
                    structPaste: '粘贴',

                    lastAccessed: '访问历史',
                    lastModified: '修改历史',

                    themeSetting: '主题设置',
                    themeFile: '主题文件',
                    themeFileHelp: '主题文件名，如 colourpurple.json',
                    themeFileNotFound: '主题文件不存在',
                    themeFileExists: '主题文件存在',
                    themeFileValid: '主题文件有效',
                    themeFileInvalid: '主题文件无效',
                    themeSettingSaved: '主题设置已保存',
                    themeSettingFailed: '主题设置失败',
                    themeTestFailed: '主题测试失败',
                    themeChangeHint: '主题已更新，可能需要刷新页面',
                    themeSettingNote: '主题设置说明',
                    themeSettingDescription: '设置主题文件名称，主题文件应放在与 cfgeditor.yml 相同的目录中',
                    pleaseSetThemeFile: '请先设置主题文件',
                    testTheme: '测试主题',
                    save: '保存',

                    // Flow Visualization Settings
                    nodeDimensions: '节点尺寸',
                    nodeWidth: '节点宽度',
                    editNodeWidth: '编辑节点宽度',
                    edgeStyling: '边样式',
                    edgeColor: '边颜色',
                    edgeStrokeWidth: '边粗细',
                    layoutSpacing: '布局间距',
                    mrtreeSpacing: '树状布局间距',
                    mrtreeSpacingTooltip: '树状布局中节点之间的间距',
                    layeredSpacing: '分层上下间距',
                    layeredSpacingTooltip: '分层布局中同一层节点之间的间距',
                    layeredNodeSpacing: '分层左右间距',
                    layeredNodeSpacingTooltip: '不同层之间节点的间距'
                }
            }
        }
    });
