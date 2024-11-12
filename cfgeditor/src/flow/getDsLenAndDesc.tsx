import {EntityBrief} from "./entityModel.ts";
import {NodeShowType} from "../routes/setting/storageJson.ts";

export function getDsLenAndDesc(brief: EntityBrief, nodeShow?: NodeShowType): [number, string | null] {
    const ds = brief.descriptions;
    let desc: string | null = null;
    let showDsLen: number = 0;
    if (nodeShow) {
        switch (nodeShow.refShowDescription) {
            case "show":
                if (ds && ds.length > 0) {
                    desc = ds[ds.length - 1].value;
                    showDsLen = ds.length - 1;
                }
                break;
            case "showFallbackValue":
                if (ds && ds.length > 0) {
                    desc = ds[ds.length - 1].value;
                    showDsLen = ds.length - 1;
                } else {
                    desc = brief.value;
                }
                break;
            case "showValue":
                desc = brief.value;
                if (ds) {
                    showDsLen = ds.length;
                }
                break;
            case "none":
                break;
        }
    }

    return [showDsLen, desc];
}