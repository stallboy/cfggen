export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

export class Logger {
    private context: string;
    private logLevel: LogLevel;

    constructor(context: string, logLevel: LogLevel = 'info') {
        this.context = context;
        this.logLevel = logLevel;
    }

    debug(message: string, ...args: any[]): void {
        if (this.shouldLog('debug')) {
            console.debug(`[${this.context}] DEBUG: ${message}`, ...args);
        }
    }

    info(message: string, ...args: any[]): void {
        if (this.shouldLog('info')) {
            console.info(`[${this.context}] INFO: ${message}`, ...args);
        }
    }

    warn(message: string, ...args: any[]): void {
        if (this.shouldLog('warn')) {
            console.warn(`[${this.context}] WARN: ${message}`, ...args);
        }
    }

    error(message: string, error?: Error | any, ...args: any[]): void {
        if (this.shouldLog('error')) {
            if (error instanceof Error) {
                console.error(`[${this.context}] ERROR: ${message}`, error, ...args);
            } else {
                console.error(`[${this.context}] ERROR: ${message}`, error, ...args);
            }
        }
    }

    private shouldLog(level: LogLevel): boolean {
        const levels: LogLevel[] = ['debug', 'info', 'warn', 'error'];
        const currentIndex = levels.indexOf(this.logLevel);
        const messageIndex = levels.indexOf(level);
        return messageIndex >= currentIndex;
    }

    setLogLevel(level: LogLevel): void {
        this.logLevel = level;
    }

    getLogLevel(): LogLevel {
        return this.logLevel;
    }

    // 性能计时器
    startTimer(label: string): void {
        console.time(`[${this.context}] ${label}`);
    }

    endTimer(label: string): void {
        console.timeEnd(`[${this.context}] ${label}`);
    }
}
