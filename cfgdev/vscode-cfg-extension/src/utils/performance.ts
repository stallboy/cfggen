export interface PerformanceMetrics {
    operation: string;
    duration: number;  // in milliseconds
    timestamp: number;
    metadata?: Record<string, any>;
}

export class PerformanceMonitor {
    private metrics: PerformanceMetrics[] = [];
    private maxMetrics = 1000; // 最多保存1000条记录

    // 记录操作性能
    record(operation: string, duration: number, metadata?: Record<string, any>): void {
        const metric: PerformanceMetrics = {
            operation,
            duration,
            timestamp: Date.now(),
            metadata
        };

        this.metrics.push(metric);

        // 限制记录数量
        if (this.metrics.length > this.maxMetrics) {
            this.metrics.shift();
        }
    }

    // 测量操作执行时间
    async measure<T>(operation: string, fn: () => T | Promise<T>, metadata?: Record<string, any>): Promise<T> {
        const start = performance.now();
        try {
            const result = await fn();
            const duration = performance.now() - start;
            this.record(operation, duration, metadata);
            return result;
        } catch (error) {
            const duration = performance.now() - start;
            this.record(operation, duration, { ...metadata, error: true });
            throw error;
        }
    }

    // 获取性能统计
    getStats(operation?: string): {
        count: number;
        avg: number;
        min: number;
        max: number;
        total: number;
    } {
        const relevantMetrics = operation
            ? this.metrics.filter(m => m.operation === operation)
            : this.metrics;

        if (relevantMetrics.length === 0) {
            return { count: 0, avg: 0, min: 0, max: 0, total: 0 };
        }

        const durations = relevantMetrics.map(m => m.duration);
        const count = durations.length;
        const total = durations.reduce((a, b) => a + b, 0);
        const avg = total / count;
        const min = Math.min(...durations);
        const max = Math.max(...durations);

        return { count, avg, min, max, total };
    }

    // 获取最近的操作
    getRecentOperations(limit: number = 10): PerformanceMetrics[] {
        return this.metrics.slice(-limit);
    }

    // 清除所有记录
    clear(): void {
        this.metrics = [];
    }

    // 检查性能阈值
    checkThreshold(operation: string, threshold: number): boolean {
        const stats = this.getStats(operation);
        return stats.avg > threshold;
    }

    // 导出性能数据
    export(): PerformanceMetrics[] {
        return [...this.metrics];
    }

    // 打印性能报告
    printReport(): void {
        console.log('\n=== Performance Report ===');
        const operations = new Set(this.metrics.map(m => m.operation));
        for (const operation of operations) {
            const stats = this.getStats(operation);
            console.log(`\n${operation}:`);
            console.log(`  Count: ${stats.count}`);
            console.log(`  Average: ${stats.avg.toFixed(2)}ms`);
            console.log(`  Min: ${stats.min.toFixed(2)}ms`);
            console.log(`  Max: ${stats.max.toFixed(2)}ms`);
            console.log(`  Total: ${stats.total.toFixed(2)}ms`);
        }
        console.log('========================\n');
    }
}

// 全局性能监控器实例
export const globalPerformanceMonitor = new PerformanceMonitor();
