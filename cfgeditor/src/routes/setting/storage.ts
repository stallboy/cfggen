import {path} from "@tauri-apps/api";
import {readTextFile, writeTextFile} from "@tauri-apps/api/fs";
import {parse, stringify} from "yaml";

export function getInt(key: string, def: number): number {
    let v = localStorage.getItem(key);
    if (v) {
        let n = parseInt(v);
        if (!isNaN(n)) {
            return n;
        }
    }
    return def;
}

export function getBool(key: string, def: boolean): boolean {
    let v = localStorage.getItem(key);
    if (v) {
        return v == 'true';
    }
    return def;
}

export function getStr(key: string, def: string): string {
    let v = localStorage.getItem(key);
    if (v) {
        return v;
    }
    return def;
}

export function getEnumStr<T>(key: string, enums: string[], def: T): T {
    let v = localStorage.getItem(key);
    if (v && enums.includes(v)) {
        return v as T;
    }
    return def;
}


export function getJsonNullable<T>(key: string, parser: (jsonStr: string) => T): T | null {
    let v = localStorage.getItem(key);
    if (v) {
        try {
            return parser(v);
        } catch (e) {
            console.log(e);
        }
    }
    return null;
}

export function getJson<T>(key: string, parser: (jsonStr: string) => T, def: T): T {
    let v = localStorage.getItem(key);
    if (v) {
        try {
            return parser(v);
        } catch (e) {
            console.log(e);
        }
    }
    return def;
}

let conf: string | undefined = undefined;

async function getConf() {
    if (!conf) {
        conf = await path.resolveResource("cfgeditor.yml")
    }
    return conf;
}

export async function readCfgAsync() {
    console.log('read cfg file')
    const conf = await getConf();
    const content = await readTextFile(conf);
    const settings = parse(content);
    // console.log(conf, content, settings);
    if (typeof settings == "object") {
        for (const key in settings) {
            const value = settings[key];
            localStorage.setItem(key, value);
            // console.log(key, value);
        }
    }
    return true;
}

async function saveCfgAsync() {
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

function dummy(_reason: any) {
}

export function setCfg(key: string, value: string) {
    localStorage.setItem(key, value);
    saveCfgAsync().catch(dummy);
}

export function removeCfg(key: string) {
    localStorage.removeItem(key);
    saveCfgAsync().catch(dummy);
}