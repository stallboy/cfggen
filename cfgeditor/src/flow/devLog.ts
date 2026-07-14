// 生产路径上诊断日志的统一收口：仅 DEV 构建生效——import.meta.env.DEV 是 Vite 编译时常量，
// 生产构建里 if (false) {...} 死分支（含 console 调用本身）被 esbuild 消除，不打日志。
// 注意：调用点的实参仍会被求值，故只传廉价参数（已有变量引用），勿在此构造昂贵实参（如 JSON.stringify）。
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
