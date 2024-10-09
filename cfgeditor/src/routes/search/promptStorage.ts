import {path} from "@tauri-apps/api";
import {writeTextFile} from "@tauri-apps/plugin-fs";
import {isTauri} from "@tauri-apps/api/core";

const table2fn = new Map<string, string>();

async function getFn(table: string) {
    let fn = table2fn.get(table);
    if (fn == undefined) {
        fn = await path.resolveResource(`${table}.prompt.md`);
        table2fn.set(table, fn);
    }
    return fn;
}

export async function savePromptAsync(table: string, prompt: string) {
    if (isTauri()) {
        const fn = await getFn(table);
        await writeTextFile(fn, prompt);
    }
}