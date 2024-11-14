import {
    readStoreStateOnce,
    store
} from "../routes/setting/store.ts";
import {readDir} from "@tauri-apps/plugin-fs";
import {queryClient} from "../main.tsx";
import {ext2type, findKeyEndIndex} from "./resUtils.ts";
import {ResAudioTrack, ResInfo, ResSubtitlesTrack, ResType} from "./resInfo.ts";
import {isTauri} from "@tauri-apps/api/core";
import {join} from "@tauri-apps/api/path";
import {path} from "@tauri-apps/api";

async function processDirRecursively(dir: string,
                                     txtAsSrt: boolean,
                                     lang: string | undefined,
                                     result: Map<string, ResInfo[]>,
                                     stat: Map<string, number>) {
    const entries = await readDir(dir);

    for (const {name, isFile, isDirectory} of entries) {
        if (isDirectory) {
            const subDir = await join(dir, name);
            await processDirRecursively(subDir, txtAsSrt, lang, result, stat);
        } else if (isFile && !name.endsWith(".meta")) {
            const path = await join(dir, name);

            const idx = findKeyEndIndex(name);
            if (idx == -1) {
                console.log(`ignore  ${path}`);
            } else {
                const extIdx = name.lastIndexOf('.');
                if (extIdx == -1) {
                    console.log(`ignore ${path}`);
                } else {
                    const ext = name.substring(extIdx).toLowerCase();
                    let type: ResType = 'other';
                    let thisLang;
                    if (ext in ext2type) {
                        type = ext2type[ext];
                    } else if (txtAsSrt && ext == '.txt') {
                        type = 'subtitles';
                        thisLang = lang
                    }

                    const key = name.substring(0, idx);
                    const value = result.get(key);
                    const resInfo: ResInfo = {type, name, path, lang: thisLang};
                    if (value) {
                        value.push(resInfo);
                    } else {
                        result.set(key, [resInfo]);
                    }

                    const extCnt = stat.get(ext);
                    let cnt = 1;
                    if (extCnt) {
                        cnt += extCnt;
                    }
                    stat.set(ext, cnt);
                }
            }
        }
    }
}

function packTracks(resInfos: ResInfo[]): ResInfo[] {
    if (resInfos.length == 1) {
        return resInfos;
    }
    const videos = [];
    const audios: (ResInfo & { _picked?: boolean }) [] = [];
    const subtitles: (ResInfo & { _picked?: boolean }) [] = [];
    const imageAndOthers = [];
    for (const r of resInfos) {
        switch (r.type) {
            case "video":
                videos.push(r);
                break;
            case "audio":
                audios.push(r);
                break;
            case "subtitles":
                subtitles.push(r);
                break;
            default:
                imageAndOthers.push(r);
                break;
        }
    }
    if (videos.length == 0 || (audios.length == 0 && subtitles.length == 0)) {
        return resInfos;
    }

    videos.sort((a, b) => b.name.length - a.name.length);
    let picked = 0;
    for (const v of videos) {
        const idx = v.name.lastIndexOf('.');
        if (idx != -1) {
            const noExtName = v.name.substring(0, idx);
            for (const a of audios) {
                if (!a._picked && a.name.startsWith(noExtName)) {
                    const at: ResAudioTrack = {name: a.name, path: a.path};
                    if (v.audioTracks) {
                        v.audioTracks.push(at);
                    } else {
                        v.audioTracks = [at];
                    }
                    a._picked = true;
                    picked++;
                }
            }
            for (const s of subtitles) {
                if (!s._picked && s.name.startsWith(noExtName)) {
                    const st: ResSubtitlesTrack = {name: s.name, path: s.path, lang: s.lang ?? 'zh'};
                    if (v.subtitlesTracks) {
                        v.subtitlesTracks.push(st);
                    } else {
                        v.subtitlesTracks = [st];
                    }
                    s._picked = true;
                    picked++;
                }
            }
        }
    }
    if (picked == 0) {
        return resInfos;
    }

    const result: ResInfo[] = videos.reverse();
    for (const a of audios) {
        if (!a._picked) {
            result.push(a);
        }
    }
    for (const s of subtitles) {
        if (!s._picked) {
            result.push(s);
        }
    }
    result.push(...imageAndOthers);
    return result;
}

function packAllTracks(raws: Map<string, ResInfo[]>) {
    const packed = new Map<string, ResInfo[]>();
    for (const [key, resInfos] of raws.entries()) {
        packed.set(key, packTracks(resInfos));
    }
    return packed;
}

let alreadyRead = false;

export function invalidateResInfos() {
    queryClient.invalidateQueries({queryKey: ['setting', 'resInfo'], refetchType: 'all'}).catch((reason: any) => {
        console.log(reason);
    });
    alreadyRead = false;
}

export async function readResInfosAsync() {
    if (alreadyRead) {
        return true;
    }
    alreadyRead = true;
    readStoreStateOnce();
    if (!isTauri()) {
        return true;
    }

    const {tauriConf} = store;
    const result = new Map<string, ResInfo[]>();
    const stat = new Map<string, number>();
    const baseDir = await path.resourceDir();
    store.resourceDir = baseDir;

    for (const resDir of tauriConf.resDirs) {
        let dir = resDir.dir;
        if (dir.startsWith('.')) {
            dir = await path.join(baseDir, dir);
        }
        try {
            await processDirRecursively(dir, !!resDir.txtAsSrt, resDir.lang, result, stat);

        } catch (reason: any) {
            console.error(reason);
        }
    }

    const packed = packAllTracks(result);
    store.resMap = packed;
    // console.log(`read res file for ${packed.size} node`, packed, stat);
    return true;
}

