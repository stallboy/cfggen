// 测试环境准备（非业务逻辑 mock）。
//
// res/resUtils.ts 的 joinPath 末尾调用 @tauri-apps/api 的 path.sep()：
//   return [true, baseDir + path.sep() + selfPath]
// 而 path.sep() 的实现是 `window.__TAURI_INTERNALS__.plugins.path.sep`，
// 仅在真实 Tauri 运行时存在。这里在 jsdom 的 window 上补一个固定分隔符 '/'，
// 使 joinPath 这类纯逻辑可在测试中直接运行。
//
// 分隔符取固定 '/'：joinPath 的核心逻辑是「剥离尾部分隔符 + 逐层消解 ../ 」，
// 分隔符只是最后拼接用的字面量，取定值便于断言，不影响对逻辑正确性的覆盖。
const win = globalThis as unknown as { window: Record<string, unknown> }
if (win.window && !(win.window as { __TAURI_INTERNALS__?: unknown }).__TAURI_INTERNALS__) {
    (win.window as { __TAURI_INTERNALS__: unknown }).__TAURI_INTERNALS__ = {
        plugins: {
            path: {sep: '/', delimiter: ':'},
        },
    }
}
