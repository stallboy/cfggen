/**
 * Utility functions for color handling in settings
 */
import {NODE_SHOW_DEFAULTS} from "@/flow/colors.ts";

type ColorValue = string | { toHexString: () => string } | null | undefined;

export function fixColor(color: ColorValue, defaultColor: string = NODE_SHOW_DEFAULTS.nodeColor): string {
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

export function fixColors(keywordColors: KeywordColorInput[], defaultColor: string = NODE_SHOW_DEFAULTS.nodeColor): Array<{keyword: string, color: string}> {
    const colors = [];
    for (const {keyword, color} of keywordColors) {
        colors.push({keyword: keyword, color: fixColor(color, defaultColor)})
    }
    return colors;
}
