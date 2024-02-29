import {ResType} from "./resInfo.ts";
import {path} from "@tauri-apps/api";

export function findKeyEndIndex(name: string) {
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

export function sepParentDirAndFilename(dir: string): [boolean, string, string] {
    let idx = -1;
    for (let i = dir.length - 1; i >= 0; i--) {
        const c = dir[i];
        if (c == '/' || c == '\\') {
            idx = i;
            break;
        }
    }
    if (idx != -1) {
        return [true, dir.substring(0, idx), dir.substring(idx + 1)];

    }
    return [false, '', dir];
}

export async function getResourceDirAsync() {
    let baseDir = await path.resourceDir();
    if (baseDir.startsWith('\\\\?\\')) {
        baseDir = baseDir.substring(4);
    }
    return baseDir;
}

export function joinPath(_baseDir: string, _path: string): [boolean, string] {
    let path = _path;
    let baseDir = _baseDir;
    if (baseDir.length > 0) {
        let c = baseDir[baseDir.length - 1];
        if (c == '/' || c == '\\') {
            baseDir = baseDir.substring(0, baseDir.length - 1);
        }
    }

    while (path.startsWith('../') || path.startsWith('..\\')) {
        path = path.substring(3);
        let [ok, pd, _] = sepParentDirAndFilename(baseDir);
        if (ok) {
            baseDir = pd;
        } else {
            return [false, path];
        }
    }
    return [true, baseDir + '/' + path]
}

export const ext2type: Record<string, ResType> = {
    ['.mp4']: 'video',

    ['.mp3']: 'audio',
    ['.wav']: 'audio',
    ['.ogg']: 'audio',

    ['.jpg']: 'image',
    ['.png']: 'image',
    ['.jpeg']: 'image',
};