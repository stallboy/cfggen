import {readStoreStateOnce, ResAudioTrack, ResInfo, ResSubtitlesTrack, ResType, store} from "./store.ts";
import {FileEntry, readDir, writeTextFile} from "@tauri-apps/api/fs";
import {path} from "@tauri-apps/api";
import {Schema} from "../table/schemaUtil.ts";
import {getResBrief} from "../../flow/ResPopover.tsx";

function findKeyEndIndex(name: string) {
    let foundFirst = false;
    for (let i = 0; i < name.length; i++) {
        const c = name[i];
        if (c == '_') {
            if (foundFirst) {
                return i;
            }
            foundFirst = true;
        } else if (c == '.') {
            if (foundFirst) {
                return i;
            }
        }
    }
    return -1;
}

function parentDir(dir: string): [boolean, string] {
    let idx = -1;
    for (let i = dir.length - 1; i >= 0; i--) {
        const c = dir[i];
        if (c == '/' || c == '\\') {
            idx = i;
            break;
        }
    }
    if (idx != -1) {
        return [true, dir.substring(0, idx)];

    }
    return [false, dir];
}

function joinPath(_baseDir: string, _path: string): [boolean, string] {
    let path = _path;
    let baseDir = _baseDir;
    if (baseDir.startsWith('\\\\?\\')) {
        baseDir = baseDir.substring(4);
    }
    if (baseDir.length > 0) {
        let c = baseDir[baseDir.length - 1];
        if (c == '/' || c == '\\') {
            baseDir = baseDir.substring(0, baseDir.length - 1);
        }
    }

    while (path.startsWith('../') || path.startsWith('..\\')) {
        path = path.substring(3);
        let [ok, pd] = parentDir(baseDir);
        if (ok) {
            baseDir = pd;
        } else {
            return [false, path];
        }
    }
    return [true, baseDir + '/' + path]
}

const ext2type: Record<string, ResType> = {
    ['.mp4']: 'video',
    ['.mp3']: 'audio',
    ['.wav']: 'audio',
    ['.jpg']: 'image',
    ['.png']: 'image',
    ['.jpeg']: 'image',
};

function processEntries(entries: FileEntry[], txtAsSrt: boolean, lang: string | undefined,
                        result: Map<string, ResInfo[]>, stat: Map<string, number>) {
    for (const {path, name, children} of entries) {
        if (children) {
            processEntries(children, txtAsSrt, lang, result, stat);
        } else if (name && !name.endsWith(".meta")) {
            const idx = findKeyEndIndex(name);
            if (idx == -1) {
                console.log(`ignore ${name} ${path}`);
            } else {
                const extIdx = name.lastIndexOf('.');
                if (extIdx == -1) {
                    console.log(`ignore ${name} ${path}`);
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
        let idx = v.name.lastIndexOf('.');
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
    for (let a of audios) {
        if (!a._picked) {
            result.push(a);
        }
    }
    for (let s of subtitles) {
        if (!s._picked) {
            result.push(s);
        }
    }
    result.push(...imageAndOthers);
    return result;
}

function packAllTracks(raws: Map<string, ResInfo[]>) {
    const packed = new Map<string, ResInfo[]>();
    for (let [key, resInfos] of raws.entries()) {
        packed.set(key, packTracks(resInfos));
    }
    return packed;
}

let alreadyRead = false;

export async function readResInfosAsync() {
    if (alreadyRead) {
        return true;
    }
    alreadyRead = true;
    readStoreStateOnce();
    const {tauriConf} = store;
    const result = new Map<string, ResInfo[]>();
    const stat = new Map<string, number>();
    for (let resDir of tauriConf.resDirs) {
        let dir = resDir.dir;
        if (dir.startsWith('.')) {
            const baseDir = await path.resourceDir();
            let [ok, fullDir] = joinPath(baseDir, dir);
            if (ok) {
                dir = fullDir;
            } else {
                console.log('not ok, ignore', dir);
                continue;
            }
        }
        try {
            const entries = await readDir(dir, {recursive: true});
            processEntries(entries, !!resDir.txtAsSrt, resDir.lang, result, stat);

        } catch (reason: any) {
            console.error(reason);
        }
    }

    const packed = packAllTracks(result);
    store.resMap = packed;
    console.log(`read res file for ${packed.size} node`, packed, stat);
    return true;
}

interface ResEntry {
    id: string;
    infos?: ResInfo[];
    brief: string;
}

export async function summarizeResAsync(schema: Schema) {
    const {resMap} = store;
    const table2entries = new Map<string, ResEntry[]>();
    for (let [key, infos] of resMap.entries()) {
        const i = key.indexOf('_');
        if (i != -1) {
            let tableLabel = key.substring(0, i);
            let id = key.substring(i + 1);
            let entries = table2entries.get(tableLabel);
            let e = {id, infos, brief: getResBrief(infos)};
            if (entries != undefined) {
                entries.push(e);
            } else {
                table2entries.set(tableLabel, [e]);
            }
        }
    }

    for (let [tableLabel, entries] of table2entries.entries()) {
        if (entries.length > 8) {
            const sTable = schema.getSTableByLastName(tableLabel);
            if (sTable && sTable.idSet) {

                const resIdSet = new Set<string>();
                for (let entry of entries) {
                    resIdSet.add(entry.id);
                    if (!sTable.idSet.has(entry.id)) {
                        entry.brief = 'noCfg-' + entry.brief;
                    }
                }

                for (let id of sTable.idSet) {
                    if (!resIdSet.has(id)) {
                        entries.push({id, brief: 'noRes'});
                    }
                }
            }
        }
    }


    const lines = [];
    for (let [tableLabel, entries] of table2entries.entries()) {
        for (let {id, brief} of entries) {
            lines.push(`${tableLabel},${id},${brief}`);
        }
    }

    const baseDir = await path.resourceDir();
    let [ok, fullPath] = joinPath(baseDir, '../res.csv');
    if (ok) {
        await writeTextFile(fullPath, lines.join("\r\n"));
    }
    return fullPath;
}


