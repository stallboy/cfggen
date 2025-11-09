export enum LogLevel {
    DEBUG = 0,
    INFO = 1,
    WARN = 2,
    ERROR = 3
}

export class Logger {
    private static instance: Logger;
    private logLevel: LogLevel = LogLevel.INFO;

    private constructor() {}

    public static getInstance(): Logger {
        if (!Logger.instance) {
            Logger.instance = new Logger();
        }
        return Logger.instance;
    }

    public setLogLevel(level: LogLevel): void {
        this.logLevel = level;
    }

    public debug(message: string, ...args: unknown[]): void {
        if (this.logLevel <= LogLevel.DEBUG) {
            console.log(`[DEBUG] ${message}`, ...args);
        }
    }

    public info(message: string, ...args: unknown[]): void {
        if (this.logLevel <= LogLevel.INFO) {
            console.log(`[INFO] ${message}`, ...args);
        }
    }

    public warn(message: string, ...args: unknown[]): void {
        if (this.logLevel <= LogLevel.WARN) {
            console.warn(`[WARN] ${message}`, ...args);
        }
    }

    public error(message: string, ...args: unknown[]): void {
        if (this.logLevel <= LogLevel.ERROR) {
            console.error(`[ERROR] ${message}`, ...args);
        }
    }
}
