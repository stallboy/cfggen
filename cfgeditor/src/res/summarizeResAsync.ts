import {Schema} from "../routes/table/schemaUtil.ts";
import {store} from "../routes/setting/store.ts";
import {writeTextFile} from "@tauri-apps/api/fs";
import {getResourceDirAsync, joinPath} from "./resUtils.ts";
import {ResInfo} from "./resInfo.ts";
import {getResBrief} from "../flow/getResBrief.tsx";

interface ResEntry {
    id: string;
    infos?: ResInfo[];
    brief: string;
}


export async function summarizeResAsync(schema: Schema) {
    const {resMap} = store;
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
            if (sTable && sTable.idSet) {

                const resIdSet = new Set<string>();
                for (const entry of entries) {
                    resIdSet.add(entry.id);
                    if (!sTable.idSet.has(entry.id)) {
                        entry.brief = 'noCfg-' + entry.brief;
                    }
                }

                for (const id of sTable.idSet) {
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

    const baseDir = await getResourceDirAsync();
    const [ok, fullPath] = joinPath(baseDir, '_res.csv');
    if (ok) {
        await writeTextFile(fullPath, lines.join("\r\n"));
    }
    return fullPath;
}