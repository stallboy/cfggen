import {BaseDirectory, readFile, writeTextFile} from "@tauri-apps/plugin-fs";
import {parse, stringify} from "yaml";
import {isTauri} from "@tauri-apps/api/core";

// 持久化键集由 store 在初始化时注册，避免 storage 反向依赖 store（消除 store↔storage 循环）
let prefKeySet: Set<string> = new Set();
let prefSelfKeySet: Set<string> = new Set();
export function registerPrefKeySet(keySet: Set<string>, selfKeySet: Set<string>) {
    prefKeySet = keySet;
    prefSelfKeySet = selfKeySet;
}

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
            return undefined;
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
    const settings: Record<string, string> = {};
    for (const key of keySet) {
        const value = localStorage.getItem(key);
        if (value) {
            settings[key] = value;
        }
    }
    await writeTextFile(fn, stringify(settings, {sortMapEntries: true}), {baseDir: BaseDirectory.Resource});
}

function log(reason: unknown) {
    console.log(reason)
}

// 写入串行化：同一时刻只有一个 writeTextFile 在执行，避免并发写同一文件导致损坏/丢字段
let writeChain: Promise<void> = Promise.resolve();
function writeFileSerialized(fn: string, keySet: Set<string>): Promise<void> {
    writeChain = writeChain
        .then(() => saveKeySetPrefAsync(keySet, fn))
        .catch(log);
    return writeChain;
}

// debounce：合并同文件短时间内的多次写入（如 navTo 一次触发 3 次 setPref），减少全量重写次数
const WRITE_DEBOUNCE_MS = 300;
const writeTimers = new Map<string, ReturnType<typeof setTimeout>>();

function scheduleWrite(fn: string, keySet: Set<string>) {
    const existing = writeTimers.get(fn);
    if (existing) {
        clearTimeout(existing);
    }
    const timer = setTimeout(() => {
        writeTimers.delete(fn);
        writeFileSerialized(fn, keySet);
    }, WRITE_DEBOUNCE_MS);
    writeTimers.set(fn, timer);
}

function savePrefAsyncIf(changedKey: string) {
    if (prefKeySet.has(changedKey)) {
        scheduleWrite("cfgeditor.yml", prefKeySet);
    }
}

export async function saveSelfPrefAsync() {
    // 关窗等需立即落盘的场景：绕过 debounce 直接串行写，返回 promise 供调用方 await
    await writeFileSerialized("cfgeditorSelf.yml", prefSelfKeySet);
}

export function setPref(key: string, value: string) {
    localStorage.setItem(key, value);
    if (isTauri()) {
        savePrefAsyncIf(key);
    }
}

// export function removePref(key: string) {
//     localStorage.removeItem(key);
//     if (isTauri()) {
//         savePrefAsyncIf(key);
//     }
// }
