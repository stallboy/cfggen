/**
 * 错误处理工具类 - 提供统一的错误处理机制
 */
export class ErrorHandler {
    /**
     * 记录错误日志
     * @param context 错误发生的上下文
     * @param error 错误对象
     */
    static logError(context: string, error: unknown): void {
        console.error(`[${context}] Error:`, error);
        // 可以添加更多错误处理逻辑，如发送到监控系统
    }

    /**
     * 处理异步操作的错误
     * @param promise 异步操作
     * @param context 操作上下文
     */
    static handleAsyncError(promise: Promise<unknown>, context: string): void {
        promise.catch(error => this.logError(context, error));
    }

    /**
     * 安全执行函数，捕获并记录错误
     * @param fn 要执行的函数
     * @param context 执行上下文
     * @param defaultValue 出错时的默认返回值
     */
    static safeExecute<T>(fn: () => T, context: string, defaultValue?: T): T {
        try {
            return fn();
        } catch (error) {
            this.logError(context, error);
            return defaultValue as T;
        }
    }

    /**
     * 安全执行异步函数，捕获并记录错误
     * @param fn 要执行的异步函数
     * @param context 执行上下文
     * @param defaultValue 出错时的默认返回值
     */
    static async safeExecuteAsync<T>(
        fn: () => Promise<T>,
        context: string,
        defaultValue?: T
    ): Promise<T> {
        try {
            return await fn();
        } catch (error) {
            this.logError(context, error);
            return defaultValue as T;
        }
    }
}