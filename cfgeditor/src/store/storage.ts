import {BaseDirectory, readFile, writeTextFile} from "@tauri-apps/plugin-fs";
import {parse, stringify} from "yaml";
import {getPrefKeySet, getPrefSelfKeySet} from "./store.ts";
import {isTauri} from "@tauri-apps/api/core";

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


let alreadyRead = false;

export async function readPrefAsyncOnce() {
    if (alreadyRead) {
        return true;
    }
    alreadyRead = true;
    if (!isTauri()) {
        return true;
    }
    localStorage.clear();
    await readConf("cfgeditor.yml");
    await readConf("cfgeditorSelf.yml");
    return true;
}

async function readConf(conf: string) {
    // console.log("read", conf);
    // tauri fs 2.0.1之后的版本，readTextFile出来的是乱码
    // const txt = await readTextFile(conf, {baseDir: BaseDirectory.Resource});
    const contentBytes = await readFile(conf, {baseDir: BaseDirectory.Resource});
    const txt = new TextDecoder().decode(contentBytes);
    const settings = parse(txt);
    // console.log("settings", typeof settings, settings)
    if (typeof settings == "object") {
        for (const key in settings) {
            const value = settings[key];
            localStorage.setItem(key, value);
            // console.log(key, value);
        }
    }
}

async function saveKeySetPrefAsync(keySet: Set<string>, fn: string) {
    const settings: Record<string, any> = {};
    for (const key of keySet) {
        const value = localStorage.getItem(key);
        if (value) {
            settings[key] = value;
        }
    }
    await writeTextFile(fn, stringify(settings, {sortMapEntries: true}), {baseDir: BaseDirectory.Resource});
}

async function savePrefAsyncIf(changedKey: string) {
    const prefKeySet = getPrefKeySet();
    if (prefKeySet.has(changedKey)) {
        await saveKeySetPrefAsync(prefKeySet, "cfgeditor.yml");
    }
}

function log(reason: any) {
    console.log(reason)
}

export function saveSelfPrefAsync() {
    const prefSelfKeySet = getPrefSelfKeySet();
    saveKeySetPrefAsync(prefSelfKeySet, "cfgeditorSelf.yml").catch(log);
}

export function setPref(key: string, value: string) {
    localStorage.setItem(key, value);
    if (isTauri()) {
        savePrefAsyncIf(key).catch(log);
    }
}

export function removePref(key: string) {
    localStorage.removeItem(key);
    if (isTauri()) {
        savePrefAsyncIf(key).catch(log);
    }
}
