import {TauriConf} from "../routes/setting/storageJson.ts";
import {ResInfo, ResType} from "./resInfo.ts";
import {ext2type, joinPath, sepParentDirAndFilename} from "./resUtils.ts";
import {Refs} from "../routes/record/recordModel.ts";
import {store} from "../routes/setting/store.ts";


export function refsToResInfos(refs: Refs, conf: TauriConf): ResInfo[] | undefined {
    if (!refs.$refs || refs.$refs.length == 0) {
        return;
    }

    const {assetDir, assetRefTable} = conf;
    if (assetRefTable.length == 0) {
        return;
    }

    let baseDir = assetDir;
    if (assetDir.startsWith('.')) {
        const {resourceDir} = store;
        let [ok, _baseDir] = joinPath(resourceDir, assetDir);
        baseDir = _baseDir;
        if (!ok){
            return;
        }
    }


    const resInfos: ResInfo[] = [];
    for (const ref of refs.$refs) {
        if (ref.toTable == assetRefTable) {
            const [ok, fullPath] = joinPath(baseDir, ref.toId);
            if (!ok) {
                console.log('not ok, ignore', ref.toId);
                continue;
            }
            const [_ok, _dir, fileName] = sepParentDirAndFilename(fullPath);
            const extIdx = fileName.lastIndexOf('.');
            if (extIdx == -1) {
                console.log(`ignore ${fullPath}`);
            } else {
                const ext = fileName.substring(extIdx).toLowerCase();
                let type: ResType = 'other';
                if (ext in ext2type) {
                    type = ext2type[ext];
                }

                const resInfo: ResInfo = {type, name: fileName, path: fullPath};
                resInfos.push(resInfo);
            }
        }
    }
    if (resInfos.length > 0) {
        return resInfos;
    }
}