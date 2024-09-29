import {path} from "@tauri-apps/api";
import {readTextFile, writeTextFile} from "@tauri-apps/api/fs";
import {parse, stringify} from "yaml";
import {getPrefKeySet, getPrefSelfKeySet} from "./store.ts";

export function getPrefInt(key: string, def: number): number {
    const v = localStorage.getItem(key);
    if (v) {
        const n = parseInt(v);
        if (!isNaN(n)) {
            return n;
        }
    }
    return def;
}

export function getPrefBool(key: string, def: boolean): boolean {
    const v = localStorage.getItem(key);
    if (v) {
        return v == 'true';
    }
    return def;
}

export function getPrefStr(key: string, def: string): string {
    const v = localStorage.getItem(key);
    if (v) {
        return v;
    }
    return def;
}

export function getPrefEnumStr<T>(key: string, enums: string[]): T | undefined {
    const v = localStorage.getItem(key);
    if (v && enums.includes(v)) {
        return v as T;
    }
}


export function getPrefJson<T>(key: string, parser: (jsonStr: string) => T): T | undefined {
    const v = localStorage.getItem(key);
    if (v) {
        try {
            return parser(v);
        } catch (e) {
            console.log(e);
        }
    }
}


let conf: string | undefined = undefined;
let selfConf: string | undefined = undefined;

async function getConf() {
    if (!conf) {
        conf = await path.resolveResource("cfgeditor.yml");
    }
    return conf;
}

async function getSelfConf() {
    if (!selfConf) {
        selfConf = await path.resolveResource("cfgeditorSelf.yml");
    }
    return selfConf;
}


let alreadyRead = false;

export async function readPrefAsyncOnce() {
    if (alreadyRead) {
        return true;
    }
    alreadyRead = true;
    if (!window.__TAURI__) {
        return true;
    }
    console.log('read yml file')
    localStorage.clear();
    await readConf(await getConf());
    await readConf(await getSelfConf());
    return true;
}

async function readConf(conf: string) {
    console.log("read", conf);
    const settings = parse(await readTextFile(conf));
    if (typeof settings == "object") {
        for (const key in settings) {
            const value = settings[key];
            localStorage.setItem(key, value);
            console.log(key, value);
        }
    }
}

async function savePrefAsync() {
    const settings: Record<string, any> = {};
    const selfSettings: Record<string, any> = {};
    const prefKeySet = getPrefKeySet();
    const prefSelfKeySet = getPrefSelfKeySet();
    for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (key === null) {
            continue;
        }
        if (prefKeySet.has(key)) {
            settings[key] = localStorage.getItem(key);
        } else if (prefSelfKeySet.has(key)) {
            selfSettings[key] = localStorage.getItem(key);
        }
    }
    const conf = await getConf();
    await writeTextFile(conf, stringify(settings));
    const selfConf = await getSelfConf();
    await writeTextFile(selfConf, stringify(selfSettings));
}

function log(reason: any) {
    console.log(reason)
}

export function setPref(key: string, value: string) {
    localStorage.setItem(key, value);
    if (window.__TAURI__) {
        savePrefAsync().catch(log);
    }
}

export function removePref(key: string) {
    localStorage.removeItem(key);
    if (window.__TAURI__) {
        savePrefAsync().catch(log);
    }
}
