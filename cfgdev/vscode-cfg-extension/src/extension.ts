import * as vscode from 'vscode';
import { LanguageClient, LanguageClientOptions, ServerOptions, TransportKind } from 'vscode-languageclient/node';

let client: LanguageClient;

/**
 * 激活扩展
 * @param context 扩展上下文
 */
export async function activate(context: vscode.ExtensionContext): Promise<void> {
    console.log('[CFG] Extension is now active');

    // 注册语言标识符 'cfg'
    const cfgLanguageId = 'cfg';

    // 配置语言客户端
    const clientOptions: LanguageClientOptions = {
        // 注册的文档类型
        documentSelector: [
            {
                scheme: 'file',
                language: cfgLanguageId
            }
        ],
        // 同步配置
        synchronize: {
            // 文件更改时通知服务器
            fileEvents: vscode.workspace.createFileSystemWatcher('**/*.cfg')
        }
    };

    // 配置服务器选项
    const serverOptions: ServerOptions = {
        run: {
            module: context.asAbsolutePath('./out/server/cfgLanguageServer.js'),
            transport: TransportKind.ipc,
            args: []
        },
        debug: {
            module: context.asAbsolutePath('./out/server/cfgLanguageServer.js'),
            transport: TransportKind.ipc,
            args: ['--debug']
        }
    };

    // 创建语言客户端
    client = new LanguageClient(
        'cfgLanguageServer',
        'CFG Language Server',
        serverOptions,
        clientOptions
    );

    // 启动客户端
    try {
        await client.start();
        console.log('[CFG] Language client started successfully');
    } catch (error) {
        console.error('[CFG] Failed to start language client:', error);
        vscode.window.showErrorMessage('CFG Language Server 启动失败: ' + (error as Error).message);
        throw error;
    }

    // 注册命令：重新加载当前文件的符号表
    const reloadCommand = vscode.commands.registerCommand('cfg.reload', async () => {
        const editor = vscode.window.activeTextEditor;
        if (editor && editor.document.languageId === 'cfg') {
            // 发送重新加载请求到服务器
            await client.sendRequest('workspace/executeCommand', {
                command: 'cfg/reload',
                arguments: [editor.document.uri.toString()]
            });
            vscode.window.showInformationMessage('CFG 文件已重新加载');
        } else {
            vscode.window.showWarningMessage('当前没有打开的 CFG 文件');
        }
    });

    // 注册命令：显示引用
    const showReferencesCommand = vscode.commands.registerCommand('cfg.showReferences', async () => {
        const editor = vscode.window.activeTextEditor;
        if (editor) {
            const position = editor.selection.active;
            await client.sendRequest('workspace/executeCommand', {
                command: 'cfg/showReferences',
                arguments: [editor.document.uri.toString(), position.line, position.character]
            });
        }
    });

    // 添加到订阅列表
    context.subscriptions.push(reloadCommand);
    context.subscriptions.push(showReferencesCommand);
    context.subscriptions.push(client);

    // 显示激活信息
    vscode.window.showInformationMessage('CFG Language Support 扩展已激活');
}

/**
 * 停用扩展
 */
export function deactivate(): Thenable<void> | undefined {
    console.log('[CFG] Extension is being deactivated');
    if (!client) {
        return undefined;
    }
    return client.stop();
}
