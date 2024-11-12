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

export function joinPath(_baseDir: string, _path: string): [boolean, string] {
    let selfPath = _path;
    let baseDir = _baseDir;
    if (baseDir.length > 0) {
        const c = baseDir[baseDir.length - 1];
        if (c == '/' || c == '\\') {
            baseDir = baseDir.substring(0, baseDir.length - 1);
        }
    }

    while (selfPath.startsWith('../') || selfPath.startsWith('..\\')) {
        selfPath = selfPath.substring(3);
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const [ok, pd, _] = sepParentDirAndFilename(baseDir);
        if (ok) {
            baseDir = pd;
        } else {
            return [false, selfPath];
        }
    }
    return [true, baseDir + path.sep() + selfPath]
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