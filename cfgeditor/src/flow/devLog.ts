// 生产路径上诊断日志的统一收口：仅 DEV 构建生效，生产包被 Vite 静态消除，
// 避免 fillHandles / 布局对账等数据异常诊断在生产里打日志。
// 用法与 console 一致：devLog(...)/devWarn(...)/devError(...)。

export function devLog(...args: unknown[]): void {
    if (import.meta.env.DEV) {
        console.log(...args);
    }
}

export function devWarn(...args: unknown[]): void {
    if (import.meta.env.DEV) {
        console.warn(...args);
    }
}

export function devError(...args: unknown[]): void {
    if (import.meta.env.DEV) {
        console.error(...args);
    }
}
