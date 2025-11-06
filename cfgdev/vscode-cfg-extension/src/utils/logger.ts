import * as vscode from 'vscode';

export enum LogLevel {
  ERROR = 0,
  WARN = 1,
  INFO = 2,
  DEBUG = 3,
  VERBOSE = 4
}

export class Logger {
  private static instance: Logger;
  private logLevel: LogLevel = LogLevel.INFO;
  private outputChannel: vscode.OutputChannel;

  private constructor() {
    this.outputChannel = vscode.window.createOutputChannel('CFG Editor');
    this.updateLogLevel();
  }

  public static getInstance(): Logger {
    if (!Logger.instance) {
      Logger.instance = new Logger();
    }
    return Logger.instance;
  }

  private updateLogLevel(): void {
    const config = vscode.workspace.getConfiguration('cfg');
    const level = config.get<string>('logLevel', 'info');

    switch (level) {
      case 'error':
        this.logLevel = LogLevel.ERROR;
        break;
      case 'warn':
        this.logLevel = LogLevel.WARN;
        break;
      case 'info':
        this.logLevel = LogLevel.INFO;
        break;
      case 'debug':
        this.logLevel = LogLevel.DEBUG;
        break;
      case 'verbose':
        this.logLevel = LogLevel.VERBOSE;
        break;
      default:
        this.logLevel = LogLevel.INFO;
    }
  }

  public error(message: string, ...args: any[]): void {
    this.log(LogLevel.ERROR, `[ERROR] ${message}`, args);
  }

  public warn(message: string, ...args: any[]): void {
    this.log(LogLevel.WARN, `[WARN] ${message}`, args);
  }

  public info(message: string, ...args: any[]): void {
    this.log(LogLevel.INFO, `[INFO] ${message}`, args);
  }

  public debug(message: string, ...args: any[]): void {
    this.log(LogLevel.DEBUG, `[DEBUG] ${message}`, args);
  }

  public verbose(message: string, ...args: any[]): void {
    this.log(LogLevel.VERBOSE, `[VERBOSE] ${message}`, args);
  }

  private log(level: LogLevel, message: string, args: any[]): void {
    if (level <= this.logLevel) {
      const timestamp = new Date().toISOString();
      const formattedMessage = args.length > 0
        ? `${timestamp} ${message} ${args.map(arg => JSON.stringify(arg)).join(' ')}`
        : `${timestamp} ${message}`;

      this.outputChannel.appendLine(formattedMessage);

      // Also log to console in debug mode
      const config = vscode.workspace.getConfiguration('cfg');
      if (config.get<boolean>('debugMode', false)) {
        console.log(formattedMessage);
      }
    }
  }

  public showOutputChannel(): void {
    this.outputChannel.show();
  }

  public dispose(): void {
    this.outputChannel.dispose();
  }
}