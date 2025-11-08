import {
    InitializeParams,
    InitializeResult,
    ServerCapabilities,
    TextDocumentSyncKind,
    DocumentHighlightParams,
    DocumentHighlight,
    CompletionParams,
    CompletionItem,
    CompletionItemKind,
    DefinitionParams,
    Location,
    HoverParams,
    Hover,
    ReferenceParams,
} from 'vscode-languageserver-protocol';
import { TextDocument } from 'vscode-languageserver-textdocument';
import {
    createConnection,
    ProposedFeatures,
    TextDocuments,
} from 'vscode-languageserver/node';
import { CfgParserService } from '../services/cfgParserService';
import { SymbolTableService } from '../services/symbolTableService';
import { CacheService } from '../services/cacheService';
import { ModuleResolverService } from '../services/moduleResolverService';
import { SyntaxHighlightingProvider } from '../providers/syntaxHighlightingProvider';
import { CompletionProvider } from '../providers/completionProvider';
import { DefinitionProvider } from '../providers/definitionProvider';
import { HoverProvider } from '../providers/hoverProvider';
import { ReferenceProvider } from '../providers/referenceProvider';
import { Logger } from '../utils/logger';

// 创建服务器连接
const connection = createConnection(ProposedFeatures.all);

// 创建文档集合
const documents: TextDocuments<TextDocument> = new TextDocuments(TextDocument);

// 全局变量
let parserService: CfgParserService;
let symbolTableService: SymbolTableService;
let cacheService: CacheService;
let moduleResolverService: ModuleResolverService;
let syntaxHighlightingProvider: SyntaxHighlightingProvider;
let completionProvider: CompletionProvider;
let definitionProvider: DefinitionProvider;
let hoverProvider: HoverProvider;
let referenceProvider: ReferenceProvider;
let logger: Logger;

// 服务器初始化
connection.onInitialize((params: InitializeParams): InitializeResult => {
    console.log('[CFG Server] Initializing CFG Language Server...');

    // 初始化日志
    logger = new Logger('CFGServer');

    // 初始化服务
    cacheService = new CacheService();
    moduleResolverService = new ModuleResolverService(params.rootUri || '');
    parserService = new CfgParserService(logger);
    symbolTableService = new SymbolTableService(parserService, moduleResolverService, cacheService, logger);

    // 初始化提供器
    syntaxHighlightingProvider = new SyntaxHighlightingProvider(parserService, logger);
    completionProvider = new CompletionProvider(parserService, symbolTableService, logger);
    definitionProvider = new DefinitionProvider(parserService, symbolTableService, moduleResolverService, logger);
    hoverProvider = new HoverProvider(parserService, symbolTableService, logger);
    referenceProvider = new ReferenceProvider(parserService, symbolTableService, logger);

    console.log('[CFG Server] Services initialized successfully');

    // 定义服务器能力
    const capabilities: ServerCapabilities = {
        // 文本同步
        textDocumentSync: TextDocumentSyncKind.Full,

        // 语法高亮
        documentHighlightProvider: true,

        // 自动补全
        completionProvider: {
            resolveProvider: true,
            triggerCharacters: [':', '>', '(', '.']
        },

        // 跳转到定义
        definitionProvider: true,

        // 悬停提示
        hoverProvider: true,

        // 引用查找
        referencesProvider: true,

        // 工作区管理
        workspace: {
            workspaceFolders: {
                supported: true
            }
        }
    };

    console.log('[CFG Server] Initialization complete');

    return {
        capabilities
    };
});

// 服务器启动
connection.onInitialized(() => {
    console.log('[CFG Server] Server initialized');
    logger.info('CFG Language Server 已启动');
});

// 文档内容变更时的处理
documents.onDidChangeContent(async (change: any) => {
    const document = change.document;
    logger.debug(`Document content changed: ${document.uri}`);

    try {
        // 重新解析文档
        await parserService.parseDocument(document);

        // 更新符号表
        await symbolTableService.updateSymbolTable(document.uri);

        // 清空诊断信息
        connection.sendDiagnostics({
            uri: document.uri,
            diagnostics: []
        });
    } catch (error) {
        logger.error(`Failed to process document change: ${error}`);
    }
});

// 文档打开时的处理
documents.onDidOpen(async (event: any) => {
    const document = event.document;
    logger.info(`Document opened: ${document.uri}`);

    try {
        // 解析文档
        await parserService.parseDocument(document);
        await symbolTableService.updateSymbolTable(document.uri);
    } catch (error) {
        logger.error(`Failed to open document: ${error}`);
    }
});

// 文档关闭时的处理
documents.onDidClose((event: any) => {
    const document = event.document;
    logger.debug(`Document closed: ${document.uri}`);
    // 清理缓存
    cacheService.invalidate(document.uri);
});

// 语法高亮
connection.onDocumentHighlight((params: DocumentHighlightParams): DocumentHighlight[] => {
    logger.debug(`Document highlight request: ${params.textDocument.uri} at line ${params.position.line}`);
    try {
        const document = documents.get(params.textDocument.uri);
        if (!document) {
            return [];
        }
        return syntaxHighlightingProvider.provideHighlights(document, params.position);
    } catch (error) {
        logger.error(`Document highlight error: ${error}`);
        return [];
    }
});

// 自动补全
connection.onCompletion(async (params: CompletionParams): Promise<CompletionItem[] | null> => {
    logger.debug(`Completion request: ${params.textDocument.uri} at line ${params.position.line}`);
    try {
        const document = documents.get(params.textDocument.uri);
        if (!document) {
            return null;
        }
        return await completionProvider.provideCompletions(document, params.position);
    } catch (error) {
        logger.error(`Completion error: ${error}`);
        return null;
    }
});

// 补全项解析
connection.onCompletionResolve((item: CompletionItem): CompletionItem => {
    return completionProvider.resolveCompletion(item);
});

// 跳转到定义
connection.onDefinition((params: DefinitionParams): Location[] | null => {
    logger.debug(`Definition request: ${params.textDocument.uri} at line ${params.position.line}`);
    try {
        const document = documents.get(params.textDocument.uri);
        if (!document) {
            return null;
        }
        return definitionProvider.provideDefinitions(document, params.position);
    } catch (error) {
        logger.error(`Definition error: ${error}`);
        return null;
    }
});

// 悬停提示
connection.onHover((params: HoverParams): Hover | null => {
    logger.debug(`Hover request: ${params.textDocument.uri} at line ${params.position.line}`);
    try {
        const document = documents.get(params.textDocument.uri);
        if (!document) {
            return null;
        }
        return hoverProvider.provideHover(document, params.position);
    } catch (error) {
        logger.error(`Hover error: ${error}`);
        return null;
    }
});

// 引用查找
connection.onReferences((params: ReferenceParams): Location[] | null => {
    logger.debug(`References request: ${params.textDocument.uri} at line ${params.position.line}`);
    try {
        const document = documents.get(params.textDocument.uri);
        if (!document) {
            return null;
        }
        return referenceProvider.provideReferences(document, params.position, params.context);
    } catch (error) {
        logger.error(`References error: ${error}`);
        return null;
    }
});

// 监听文档
documents.listen(connection);

// 开始监听连接
connection.listen();

console.log('[CFG Server] CFG Language Server listening on connection');
