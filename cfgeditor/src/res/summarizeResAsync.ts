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
    for (let [key, infos] of resMap.entries()) {
        const i = key.indexOf('_');
        if (i != -1) {
            let tableLabel = key.substring(0, i);
            let id = key.substring(i + 1);
            let entries = table2entries.get(tableLabel);
            let e = {id, infos, brief: getResBrief(infos)};
            if (entries != undefined) {
                entries.push(e);
            } else {
                table2entries.set(tableLabel, [e]);
            }
        }
    }

    for (let [tableLabel, entries] of table2entries.entries()) {
        if (entries.length > 8) {
            const sTable = schema.getSTableByLastName(tableLabel);
            if (sTable && sTable.idSet) {

                const resIdSet = new Set<string>();
                for (let entry of entries) {
                    resIdSet.add(entry.id);
                    if (!sTable.idSet.has(entry.id)) {
                        entry.brief = 'noCfg-' + entry.brief;
                    }
                }

                for (let id of sTable.idSet) {
                    if (!resIdSet.has(id)) {
                        entries.push({id, brief: 'noRes'});
                    }
                }
            }
        }
    }


    const lines = [];
    for (let [tableLabel, entries] of table2entries.entries()) {
        for (let {id, brief} of entries) {
            lines.push(`${tableLabel},${id},${brief}`);
        }
    }

    const baseDir = await getResourceDirAsync();
    let [ok, fullPath] = joinPath(baseDir, '_res.csv');
    if (ok) {
        await writeTextFile(fullPath, lines.join("\r\n"));
    }
    return fullPath;
}