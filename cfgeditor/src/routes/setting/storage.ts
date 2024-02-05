import {path} from "@tauri-apps/api";
import {readTextFile, writeTextFile} from "@tauri-apps/api/fs";
import {parse, stringify} from "yaml";

export function getPrefInt(key: string, def: number): number {
    let v = localStorage.getItem(key);
    if (v) {
        let n = parseInt(v);
        if (!isNaN(n)) {
            return n;
        }
    }
    return def;
}

export function getPrefBool(key: string, def: boolean): boolean {
    let v = localStorage.getItem(key);
    if (v) {
        return v == 'true';
    }
    return def;
}

export function getPrefStr(key: string, def: string): string {
    let v = localStorage.getItem(key);
    if (v) {
        return v;
    }
    return def;
}

export function getPrefEnumStr<T>(key: string, enums: string[]): T | undefined {
    let v = localStorage.getItem(key);
    if (v && enums.includes(v)) {
        return v as T;
    }
}


export function getPrefJson<T>(key: string, parser: (jsonStr: string) => T): T | undefined {
    let v = localStorage.getItem(key);
    if (v) {
        try {
            return parser(v);
        } catch (e) {
            console.log(e);
        }
    }
}


let conf: string | undefined = undefined;

async function getConf() {
    if (!conf) {
        conf = await path.resolveResource("cfgeditor.yml")
    }
    return conf;
}

let alreadyRead = false;

export async function readPrefAsyncOnce() {
    if (alreadyRead) {
        return true;
    }
    alreadyRead = true;
    console.log('read yml file')
    const conf = await getConf();
    const content = await readTextFile(conf);
    const settings = parse(content);
    if (typeof settings == "object") {
        for (const key in settings) {
            const value = settings[key];
            localStorage.setItem(key, value);
            // console.log(key, value);
        }
    }
    return true;
}

async function savePrefAsync() {
    const settings: Record<string, any> = {};
    for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (key) {
            settings[key] = localStorage.getItem(key);
        }
    }
    const conf = await getConf();
    const content = stringify(settings);
    await writeTextFile(conf, content);
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
