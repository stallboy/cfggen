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

}