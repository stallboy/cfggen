import {getCurrentWebviewWindow} from "@tauri-apps/api/webviewWindow";

export async function toggleFullScreen() {
    const appWindow = getCurrentWebviewWindow()
    const isFullScreen = await appWindow.isFullscreen();
    await appWindow.setFullscreen(!isFullScreen);
}
