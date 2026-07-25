import {getCurrentWebviewWindow} from "@tauri-apps/api/webviewWindow";
import {isTauri} from "@tauri-apps/api/core";

export async function toggleFullScreen() {
    // 浏览器 dev 环境下 IPC 不可用，getCurrentWebviewWindow 会 reject（unhandled rejection），直接跳过
    if (!isTauri()) {
        return;
    }
    const appWindow = getCurrentWebviewWindow()
    const isFullScreen = await appWindow.isFullscreen();
    await appWindow.setFullscreen(!isFullScreen);
}
