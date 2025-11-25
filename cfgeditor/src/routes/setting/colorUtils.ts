/**
 * Utility functions for color handling in settings
 */
import {getCurrentWebviewWindow} from "@tauri-apps/api/webviewWindow";

type ColorValue = string | { toHexString: () => string } | null | undefined;

export function fixColor(color: ColorValue, defaultColor: string = '#0898b5'): string {
    let c;
    if (color && typeof color === 'object' && 'toHexString' in color) {
        c = color.toHexString();
    } else if (typeof color === 'string') {
        c = color;
    } else {
        c = defaultColor;
    }
    return c;
}

interface KeywordColorInput {
    keyword: string;
    color: ColorValue;
}

export function fixColors(keywordColors: KeywordColorInput[], defaultColor: string = '#0898b5'): Array<{keyword: string, color: string}> {
    const colors = [];
    for (const {keyword, color} of keywordColors) {
        colors.push({keyword: keyword, color: fixColor(color, defaultColor)})
    }
    return colors;
}

export async function toggleFullScreen() {
    const appWindow = getCurrentWebviewWindow()
    const isFullScreen = await appWindow.isFullscreen();
    await appWindow.setFullscreen(!isFullScreen);
}
