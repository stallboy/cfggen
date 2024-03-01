import {ResInfo, ResType} from "./resInfo.ts";
import {ext2type, joinPath, sepParentDirAndFilename} from "./resUtils.ts";
import {Refs} from "../routes/record/recordModel.ts";
import {TauriConf} from "../routes/setting/storageJson.ts";


export interface FindResInfosParameter {
    label: string;
    refs: Refs;
    resMap: Map<string, ResInfo[]>,
    tauriConf: TauriConf;
    resourceDir: string;
}

export function findAllResInfos(param: FindResInfosParameter): ResInfo[] | undefined {
    let res: ResInfo[] | undefined;
    if (param.label.includes('_')) {
        res = param.resMap.get(param.label);
    }

    const assets = refsToResInfos(param);

    let finalRes: ResInfo[] | undefined;
    if (res) {
        if (assets) {
            finalRes = [...res, ...assets];
        } else {
            finalRes = res;
        }
    } else if (assets) {
        finalRes = assets;
    }

    return finalRes;
}

function refsToResInfos(param: FindResInfosParameter): ResInfo[] | undefined {
    const {refs, tauriConf, resourceDir} = param;
    if (!refs.$refs || refs.$refs.length == 0) {
        return;
    }

    const {assetDir, assetRefTable} = tauriConf;
    if (assetRefTable.length == 0) {
        return;
    }

    let baseDir = assetDir;
    if (assetDir.startsWith('.')) {
        let [ok, _baseDir] = joinPath(resourceDir, assetDir);
        baseDir = _baseDir;
        if (!ok) {
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

