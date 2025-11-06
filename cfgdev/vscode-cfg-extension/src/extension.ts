import * as vscode from 'vscode';
import { CfgSyntaxHighlightingProvider, cfgTokenLegend, CfgCompletionProvider, CfgDefinitionProvider, CfgForeignKeyProvider } from './providers';
import { FileIndexService } from './services/fileIndexService';
import { Logger } from './utils/logger';
import { PerformanceMonitor } from './utils/performance';

/**
 * VSCode CFG扩展主入口
 */
export function activate(context: vscode.ExtensionContext) {
  const logger = Logger.getInstance();
  const performance = PerformanceMonitor.getInstance();
  const fileIndexService = FileIndexService.getInstance();

  logger.info('CFG Extension activated');

  // 注册语法高亮提供者
  const syntaxHighlightingProvider = new CfgSyntaxHighlightingProvider();
  const syntaxHighlightingDisposable = vscode.languages.registerDocumentSemanticTokensProvider(
    { language: 'cfg', scheme: 'file' },
    syntaxHighlightingProvider,
    cfgTokenLegend
  );

  // 注册智能提示提供者
  const completionProvider = new CfgCompletionProvider();
  const completionDisposable = vscode.languages.registerCompletionItemProvider(
    { language: 'cfg', scheme: 'file' },
    completionProvider,
    '.', ':', '<' // 触发字符
  );

  // 注册定义跳转提供者
  const definitionProvider = new CfgDefinitionProvider();
  const definitionDisposable = vscode.languages.registerDefinitionProvider(
    { language: 'cfg', scheme: 'file' },
    definitionProvider
  );

  // 注册外键跳转提供者
  const foreignKeyProvider = new CfgForeignKeyProvider();
  const foreignKeyDefinitionDisposable = vscode.languages.registerDefinitionProvider(
    { language: 'cfg', scheme: 'file' },
    foreignKeyProvider
  );

  // 注册外键引用提供者
  const foreignKeyReferenceDisposable = vscode.languages.registerReferenceProvider(
    { language: 'cfg', scheme: 'file' },
    foreignKeyProvider
  );

  // 注册外键悬停提供者
  const foreignKeyHoverDisposable = vscode.languages.registerHoverProvider(
    { language: 'cfg', scheme: 'file' },
    foreignKeyProvider
  );

  // 注册文件监听器
  const fileWatcher = vscode.workspace.createFileSystemWatcher('**/*.cfg');

  // 监听文件创建
  const fileCreateDisposable = fileWatcher.onDidCreate(async (uri) => {
    try {
      const document = await vscode.workspace.openTextDocument(uri);
      await fileIndexService.addFile(uri, document.getText());
      logger.debug(`File created and indexed: ${uri.toString()}`);
    } catch (error) {
      logger.error(`Failed to index created file ${uri.toString()}:`, error);
    }
  });

  // 监听文件修改
  const fileChangeDisposable = fileWatcher.onDidChange(async (uri) => {
    try {
      const document = await vscode.workspace.openTextDocument(uri);
      await fileIndexService.addFile(uri, document.getText());
      logger.debug(`File updated and re-indexed: ${uri.toString()}`);
    } catch (error) {
      logger.error(`Failed to re-index changed file ${uri.toString()}:`, error);
    }
  });

  // 监听文件删除
  const fileDeleteDisposable = fileWatcher.onDidDelete((uri) => {
    fileIndexService.removeFile(uri);
    logger.debug(`File removed from index: ${uri.toString()}`);
  });

  // 初始索引所有已打开的CFG文件
  const initialIndexDisposable = vscode.workspace.onDidOpenTextDocument(async (document) => {
    if (document.languageId === 'cfg' && document.uri.scheme === 'file') {
      try {
        await fileIndexService.addFile(document.uri, document.getText());
        logger.debug(`Initial file indexed: ${document.uri.toString()}`);
      } catch (error) {
        logger.error(`Failed to index initial file ${document.uri.toString()}:`, error);
      }
    }
  });

  // 注册命令
  const commands = [
    // 调试命令：显示索引统计
    vscode.commands.registerCommand('cfg.showIndexStats', () => {
      const stats = fileIndexService.getStats();
      vscode.window.showInformationMessage(
        `CFG Index Stats: Files=${stats.fileCount}, Structs=${stats.structCount}, ` +
        `Interfaces=${stats.interfaceCount}, Tables=${stats.tableCount}`
      );
    }),

    // 调试命令：清空索引
    vscode.commands.registerCommand('cfg.clearIndex', () => {
      fileIndexService.clear();
      vscode.window.showInformationMessage('CFG index cleared');
    }),

    // 调试命令：显示性能报告
    vscode.commands.registerCommand('cfg.showPerformance', () => {
      performance.reportPerformance();
    })
  ];

  // 将所有可释放资源添加到上下文中
  context.subscriptions.push(
    syntaxHighlightingDisposable,
    completionDisposable,
    definitionDisposable,
    foreignKeyDefinitionDisposable,
    foreignKeyReferenceDisposable,
    foreignKeyHoverDisposable,
    fileCreateDisposable,
    fileChangeDisposable,
    fileDeleteDisposable,
    initialIndexDisposable,
    fileWatcher,
    ...commands
  );

  logger.info('CFG Extension fully activated with all providers');
}

/**
 * 扩展停用时调用
 */
export function deactivate() {
  const logger = Logger.getInstance();
  logger.info('CFG Extension deactivated');

  // 清理资源
  const fileIndexService = FileIndexService.getInstance();
  fileIndexService.clear();
}