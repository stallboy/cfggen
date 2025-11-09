export class PerformanceMonitor {
    private static measurements: Map<string, number[]> = new Map();

    /**
     * 记录性能测量
     */
    static record(operation: string, time: number): void {
        if (!this.measurements.has(operation)) {
            this.measurements.set(operation, []);
        }
        this.measurements.get(operation)!.push(time);
    }

    /**
     * 测量操作执行时间
     */
    static measure<T>(operation: string, fn: () => T): T {
        const start = performance.now();
        const result = fn();
        const end = performance.now();
        this.record(operation, end - start);
        return result;
    }

    /**
     * 异步测量
     */
    static async measureAsync<T>(operation: string, fn: () => Promise<T>): Promise<T> {
        const start = performance.now();
        const result = await fn();
        const end = performance.now();
        this.record(operation, end - start);
        return result;
    }

    /**
     * 获取统计信息
     */
    static getStats(operation: string): { avg: number; min: number; max: number; count: number } | null {
        const measurements = this.measurements.get(operation);
        if (!measurements || measurements.length === 0) {
            return null;
        }

        const sum = measurements.reduce((a, b) => a + b, 0);
        return {
            avg: sum / measurements.length,
            min: Math.min(...measurements),
            max: Math.max(...measurements),
            count: measurements.length
        };
    }

    /**
     * 打印所有统计信息
     */
    static printStats(): void {
        console.log('\n=== Performance Statistics ===');
        for (const [operation, _measurements] of this.measurements) {
            const stats = this.getStats(operation);
            if (stats) {
                console.log(`${operation}:`);
                console.log(`  Avg: ${stats.avg.toFixed(2)}ms`);
                console.log(`  Min: ${stats.min.toFixed(2)}ms`);
                console.log(`  Max: ${stats.max.toFixed(2)}ms`);
                console.log(`  Count: ${stats.count}`);
            }
        }
        console.log('==============================\n');
    }

    /**
     * 清除所有测量数据
     */
    static clear(): void {
        this.measurements.clear();
    }
}
