import {Schema} from "../routes/table/schemaUtil.tsx";
import {BaseDirectory, writeTextFile} from "@tauri-apps/plugin-fs";
import {ResInfo} from "./resInfo.ts";
import {getResBrief} from "../flow/getResBrief.tsx";
import {path} from "@tauri-apps/api";

interface ResEntry {
    id: string;
    infos?: ResInfo[];
    brief: string;
}


export async function summarizeResAsync(schema: Schema, resMap: Map<string, ResInfo[]>) {
    const table2entries = new Map<string, ResEntry[]>();
    for (const [key, infos] of resMap.entries()) {
        const i = key.indexOf('_');
        if (i != -1) {
            const tableLabel = key.substring(0, i);
            const id = key.substring(i + 1);
            const entries = table2entries.get(tableLabel);
            const e = {id, infos, brief: getResBrief(infos)};
            if (entries != undefined) {
                entries.push(e);
            } else {
                table2entries.set(tableLabel, [e]);
            }
        }
    }

    for (const [tableLabel, entries] of table2entries.entries()) {
        if (entries.length > 8) {
            const sTable = schema.getSTableByLastName(tableLabel);
            if (sTable && sTable.idMap) {

                const resIdSet = new Set<string>();
                for (const entry of entries) {
                    resIdSet.add(entry.id);
                    if (!sTable.idMap.has(entry.id)) {
                        entry.brief = 'noCfg-' + entry.brief;
                    }
                }

                for (const id of sTable.idMap.keys()) {
                    if (!resIdSet.has(id)) {
                        entries.push({id, brief: 'noRes'});
                    }
                }
            }
        }
    }


    const lines = [];
    for (const [tableLabel, entries] of table2entries.entries()) {
        for (const {id, brief} of entries) {
            lines.push(`${tableLabel},${id},${brief}`);
        }
    }
    const fn = '_res.csv';
    await writeTextFile(fn, lines.join("\r\n"), {baseDir: BaseDirectory.Resource});
    return await path.join(await path.resourceDir(), fn);
}