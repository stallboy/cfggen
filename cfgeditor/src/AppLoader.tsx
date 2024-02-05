import {useQuery} from "@tanstack/react-query";
import {readPrefAsyncOnce} from "./routes/setting/storage.ts";
import {CfgEditorApp} from "./CfgEditorApp.tsx";
import {readStoreStateOnce, store} from "./routes/setting/store.ts";
import {FileEntry, readDir} from "@tauri-apps/api/fs";
import {path} from "@tauri-apps/api";

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

function processEntries(entries: FileEntry[], result: Map<string, string[]>, stat: Map<string, number>) {
    for (const {path, name, children} of entries) {
        if (children) {
            processEntries(children, result, stat);
        } else if (name && !name.endsWith(".meta")) {
            const idx = findKeyEndIndex(name);
            if (idx == -1) {
                console.log(`ignore ${name} ${path}`);
            } else {
                const extIdx = name.lastIndexOf('.');
                if (extIdx == -1){
                    console.log(`ignore ${name} ${path}`);
                }else{
                    const key = name.substring(0, idx);
                    const value = result.get(key);
                    if (value) {
                        value.push(path);
                    } else {
                        result.set(key, [path]);
                    }

                    const ext = name.substring(extIdx).toLowerCase();
                    const extCnt = stat.get(ext);
                    let cnt = 1;
                    if (extCnt){
                        cnt += extCnt;
                    }
                    stat.set(ext, cnt);
                }


            }
        }
    }
}

let alreadyRead = false;

async function readResFileInfoAsync() {
    if (alreadyRead) {
        return true;
    }
    alreadyRead = true;
    readStoreStateOnce();
    const {tauriConf} = store;
    const result = new Map<string, string[]>();
    const stat = new Map<string, number>();
    for (let resDir of tauriConf.resDirs) {
        let dir = resDir.dir;
        if (dir.startsWith('..')) {
            dir = await path.resolveResource(dir);
        }
        try {
            const entries = await readDir(dir, {recursive: true});
            processEntries(entries, result, stat);

        } catch (reason: any) {
            console.error(reason);
        }
    }
    store.resMap = result;
    console.log(`read res file for ${result.size} node`, stat);
    return true;
}

export function AppLoader() {
    const {isError, error: _error, data} = useQuery({
        queryKey: ['setting'],
        queryFn: readPrefAsyncOnce,
        staleTime: Infinity,
        retry: 0,
    })
    useQuery({
        queryKey: ['setting', 'resInfo'],
        queryFn: readResFileInfoAsync,
        // The query will not execute until the userId exists
        enabled: !!data,
    })

    // console.log(isError, _error, data);

    if (isError || data) {
        return <CfgEditorApp/>
    }
}
