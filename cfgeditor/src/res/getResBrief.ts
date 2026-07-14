import {ResInfo} from "@/domain/resInfo";

export function getResBrief(res: ResInfo[]) {
    let v = 0;
    let a = 0;
    let i = 0;
    let o = 0;
    for (const {type, audioTracks, subtitlesTracks} of res) {
        switch (type) {
            case "video":
                v++;
                break;
            case "audio":
                a++;
                break;
            case "image":
                i++;
                break;
            default:
                o++;
                break;
        }
        if (audioTracks) {
            a += audioTracks.length;
        }
        if (subtitlesTracks) {
            o += subtitlesTracks.length;
        }
    }

    let info = '';
    if (v > 0) {
        info += v + 'v';
    }
    if (a > 0) {
        info += a + 'a';
    }
    if (i > 0) {
        info += i + 'i';
    }
    if (o > 0) {
        info += o + 'o';
    }
    return info;
}

// 节点标题栏资源摘要按钮用：图标+数字（🎬视频 🔊音频 🖼图片 📎其它，含音轨/字幕）。
// 跨语言直观，替代原 "2v3a1i" 字母缩写（无图例时费解）。
// getResBrief（"2v3a1i" 紧凑串）仍被 summarizeResAsync 写进摘要文件，保持不变。
export function getResBriefEmoji(res: ResInfo[]): string {
    let v = 0;
    let a = 0;
    let i = 0;
    let o = 0;
    for (const {type, audioTracks, subtitlesTracks} of res) {
        switch (type) {
            case "video":
                v++;
                break;
            case "audio":
                a++;
                break;
            case "image":
                i++;
                break;
            default:
                o++;
                break;
        }
        if (audioTracks) {
            a += audioTracks.length;
        }
        if (subtitlesTracks) {
            o += subtitlesTracks.length;
        }
    }

    const parts: string[] = [];
    if (v > 0) parts.push(`🎬${v}`);
    if (a > 0) parts.push(`🔊${a}`);
    if (i > 0) parts.push(`🖼${i}`);
    if (o > 0) parts.push(`📎${o}`);
    return parts.join(' ');
}