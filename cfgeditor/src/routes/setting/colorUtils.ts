/**
 * Utility functions for color handling in settings
 */

export function fixColor(color: any, defaultColor: string = '#0898b5'): string {
    let c;
    if (typeof color == 'object') {
        c = color.toHexString();
    } else if (typeof color == 'string') {
        c = color;
    } else {
        c = defaultColor;
    }
    return c;
}

export function fixColors(keywordColors: any[], defaultColor: string = '#0898b5'): Array<{keyword: string, color: string}> {
    const colors = [];
    for (const {keyword, color} of keywordColors) {
        colors.push({keyword: keyword, color: fixColor(color, defaultColor)})
    }
    return colors;
}