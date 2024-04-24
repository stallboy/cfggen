import {ResInfo} from "../res/resInfo.ts";

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