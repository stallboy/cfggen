import * as vscode from 'vscode';
import { Logger } from './logger';

export class PerformanceMonitor {
  private static instance: PerformanceMonitor;
  private measurements: Map<string, number[]> = new Map();
  private logger = Logger.getInstance();

  private constructor() {}

  public static getInstance(): PerformanceMonitor {
    if (!PerformanceMonitor.instance) {
      PerformanceMonitor.instance = new PerformanceMonitor();
    }
    return PerformanceMonitor.instance;
  }

  public startMeasurement(name: string): () => number {
    const startTime = performance.now();

    return () => {
      const endTime = performance.now();
      const duration = endTime - startTime;

      this.recordMeasurement(name, duration);
      return duration;
    };
  }

  public async measureAsync<T>(name: string, operation: () => Promise<T>): Promise<T> {
    const endMeasurement = this.startMeasurement(name);
    try {
      const result = await operation();
      endMeasurement();
      return result;
    } catch (error) {
      endMeasurement();
      throw error;
    }
  }

  public measureSync<T>(name: string, operation: () => T): T {
    const endMeasurement = this.startMeasurement(name);
    try {
      const result = operation();
      endMeasurement();
      return result;
    } catch (error) {
      endMeasurement();
      throw error;
    }
  }

  private recordMeasurement(name: string, duration: number): void {
    if (!this.measurements.has(name)) {
      this.measurements.set(name, []);
    }

    const measurements = this.measurements.get(name)!;
    measurements.push(duration);

    // Keep only last 100 measurements
    if (measurements.length > 100) {
      measurements.shift();
    }

    // Log if performance is slow
    const config = vscode.workspace.getConfiguration('cfg');
    if (config.get<boolean>('debugMode', false) && duration > 100) {
      this.logger.warn(`Slow operation detected: ${name} took ${duration.toFixed(2)}ms`);
    }
  }

  public getStats(name: string): {
    count: number;
    average: number;
    min: number;
    max: number;
    p95: number;
  } | null {
    const measurements = this.measurements.get(name);
    if (!measurements || measurements.length === 0) {
      return null;
    }

    const sorted = [...measurements].sort((a, b) => a - b);
    const sum = sorted.reduce((a, b) => a + b, 0);
    const average = sum / sorted.length;
    const min = sorted[0];
    const max = sorted[sorted.length - 1];
    const p95Index = Math.floor(sorted.length * 0.95);
    const p95 = sorted[p95Index];

    return {
      count: sorted.length,
      average,
      min,
      max,
      p95
    };
  }

  public reportPerformance(): void {
    const config = vscode.workspace.getConfiguration('cfg');
    if (!config.get<boolean>('debugMode', false)) {
      return;
    }

    this.logger.info('=== Performance Report ===');
    for (const [name] of this.measurements) {
      const stats = this.getStats(name);
      if (stats) {
        this.logger.info(
          `${name}: count=${stats.count}, avg=${stats.average.toFixed(2)}ms, ` +
          `min=${stats.min.toFixed(2)}ms, max=${stats.max.toFixed(2)}ms, p95=${stats.p95.toFixed(2)}ms`
        );
      }
    }
  }

  public reset(): void {
    this.measurements.clear();
  }
}