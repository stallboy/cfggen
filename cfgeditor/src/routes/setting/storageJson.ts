// To parse this data:
//
//   import { Convert, NodeShowType, KeywordColor, TableHideAndColor, ShowHeadType, ShowDescriptionType, NodePlacementStrategyType, FixedPage, ResDir, TauriConf } from "./file";
//
//   const nodeShowType = Convert.toNodeShowType(json);
//   const keywordColor = Convert.toKeywordColor(json);
//   const tableHideAndColor = Convert.toTableHideAndColor(json);
//   const showHeadType = Convert.toShowHeadType(json);
//   const showDescriptionType = Convert.toShowDescriptionType(json);
//   const nodePlacementStrategyType = Convert.toNodePlacementStrategyType(json);
//   const fixedPage = Convert.toFixedPage(json);
//   const resDir = Convert.toResDir(json);
//   const tauriConf = Convert.toTauriConf(json);
//
// These functions will throw an error if the JSON doesn't
// match the expected interface, even if the JSON is valid.

export interface FixedPage {
    id:          string;
    maxNode:     number;
    nodeShow:    NodeShowType;
    refIn:       boolean;
    refOutDepth: number;
    table:       string;
}

export interface NodeShowType {
    containEnum:           boolean;
    keywordColors:         KeywordColor[];
    nodePlacementStrategy: NodePlacementStrategyType;
    showDescription:       ShowDescriptionType;
    showHead:              ShowHeadType;
    tableHideAndColors:    TableHideAndColor[];
}

export interface KeywordColor {
    color:   string;
    keyword: string;
}

export type NodePlacementStrategyType = "BRANDES_KOEPF" | "LINEAR_SEGMENTS" | "SIMPLE";

export type ShowDescriptionType = "none" | "show" | "showFallbackValue" | "showValue";

export type ShowHeadType = "show" | "showCopyable";

export interface TableHideAndColor {
    color:   string;
    hide:    boolean;
    keyword: string;
}

export interface TauriConf {
    resDirs: ResDir[];
}

export interface ResDir {
    dir: string;
}

// Converts JSON strings to/from your types
// and asserts the results of JSON.parse at runtime
export class Convert {
    public static toNodeShowType(json: string): NodeShowType {
        return cast(JSON.parse(json), r("NodeShowType"));
    }

    public static nodeShowTypeToJson(value: NodeShowType): string {
        return JSON.stringify(uncast(value, r("NodeShowType")), null, 2);
    }

    public static toKeywordColor(json: string): KeywordColor {
        return cast(JSON.parse(json), r("KeywordColor"));
    }

    public static keywordColorToJson(value: KeywordColor): string {
        return JSON.stringify(uncast(value, r("KeywordColor")), null, 2);
    }

    public static toTableHideAndColor(json: string): TableHideAndColor {
        return cast(JSON.parse(json), r("TableHideAndColor"));
    }

    public static tableHideAndColorToJson(value: TableHideAndColor): string {
        return JSON.stringify(uncast(value, r("TableHideAndColor")), null, 2);
    }

    public static toShowHeadType(json: string): ShowHeadType {
        return cast(JSON.parse(json), r("ShowHeadType"));
    }

    public static showHeadTypeToJson(value: ShowHeadType): string {
        return JSON.stringify(uncast(value, r("ShowHeadType")), null, 2);
    }

    public static toShowDescriptionType(json: string): ShowDescriptionType {
        return cast(JSON.parse(json), r("ShowDescriptionType"));
    }

    public static showDescriptionTypeToJson(value: ShowDescriptionType): string {
        return JSON.stringify(uncast(value, r("ShowDescriptionType")), null, 2);
    }

    public static toNodePlacementStrategyType(json: string): NodePlacementStrategyType {
        return cast(JSON.parse(json), r("NodePlacementStrategyType"));
    }

    public static nodePlacementStrategyTypeToJson(value: NodePlacementStrategyType): string {
        return JSON.stringify(uncast(value, r("NodePlacementStrategyType")), null, 2);
    }

    public static toFixedPage(json: string): FixedPage {
        return cast(JSON.parse(json), r("FixedPage"));
    }

    public static fixedPageToJson(value: FixedPage): string {
        return JSON.stringify(uncast(value, r("FixedPage")), null, 2);
    }

    public static toResDir(json: string): ResDir {
        return cast(JSON.parse(json), r("ResDir"));
    }

    public static resDirToJson(value: ResDir): string {
        return JSON.stringify(uncast(value, r("ResDir")), null, 2);
    }

    public static toTauriConf(json: string): TauriConf {
        return cast(JSON.parse(json), r("TauriConf"));
    }

    public static tauriConfToJson(value: TauriConf): string {
        return JSON.stringify(uncast(value, r("TauriConf")), null, 2);
    }
}

function invalidValue(typ: any, val: any, key: any, parent: any = ''): never {
    const prettyTyp = prettyTypeName(typ);
    const parentText = parent ? ` on ${parent}` : '';
    const keyText = key ? ` for key "${key}"` : '';
    throw Error(`Invalid value${keyText}${parentText}. Expected ${prettyTyp} but got ${JSON.stringify(val)}`);
}

function prettyTypeName(typ: any): string {
    if (Array.isArray(typ)) {
        if (typ.length === 2 && typ[0] === undefined) {
            return `an optional ${prettyTypeName(typ[1])}`;
        } else {
            return `one of [${typ.map(a => { return prettyTypeName(a); }).join(", ")}]`;
        }
    } else if (typeof typ === "object" && typ.literal !== undefined) {
        return typ.literal;
    } else {
        return typeof typ;
    }
}

function jsonToJSProps(typ: any): any {
    if (typ.jsonToJS === undefined) {
        const map: any = {};
        typ.props.forEach((p: any) => map[p.json] = { key: p.js, typ: p.typ });
        typ.jsonToJS = map;
    }
    return typ.jsonToJS;
}

function jsToJSONProps(typ: any): any {
    if (typ.jsToJSON === undefined) {
        const map: any = {};
        typ.props.forEach((p: any) => map[p.js] = { key: p.json, typ: p.typ });
        typ.jsToJSON = map;
    }
    return typ.jsToJSON;
}

function transform(val: any, typ: any, getProps: any, key: any = '', parent: any = ''): any {
    function transformPrimitive(typ: string, val: any): any {
        if (typeof typ === typeof val) return val;
        return invalidValue(typ, val, key, parent);
    }

    function transformUnion(typs: any[], val: any): any {
        // val must validate against one typ in typs
        const l = typs.length;
        for (let i = 0; i < l; i++) {
            const typ = typs[i];
            try {
                return transform(val, typ, getProps);
            } catch (_) {}
        }
        return invalidValue(typs, val, key, parent);
    }

    function transformEnum(cases: string[], val: any): any {
        if (cases.indexOf(val) !== -1) return val;
        return invalidValue(cases.map(a => { return l(a); }), val, key, parent);
    }

    function transformArray(typ: any, val: any): any {
        // val must be an array with no invalid elements
        if (!Array.isArray(val)) return invalidValue(l("array"), val, key, parent);
        return val.map(el => transform(el, typ, getProps));
    }

    function transformDate(val: any): any {
        if (val === null) {
            return null;
        }
        const d = new Date(val);
        if (isNaN(d.valueOf())) {
            return invalidValue(l("Date"), val, key, parent);
        }
        return d;
    }

    function transformObject(props: { [k: string]: any }, additional: any, val: any): any {
        if (val === null || typeof val !== "object" || Array.isArray(val)) {
            return invalidValue(l(ref || "object"), val, key, parent);
        }
        const result: any = {};
        Object.getOwnPropertyNames(props).forEach(key => {
            const prop = props[key];
            const v = Object.prototype.hasOwnProperty.call(val, key) ? val[key] : undefined;
            result[prop.key] = transform(v, prop.typ, getProps, key, ref);
        });
        Object.getOwnPropertyNames(val).forEach(key => {
            if (!Object.prototype.hasOwnProperty.call(props, key)) {
                result[key] = transform(val[key], additional, getProps, key, ref);
            }
        });
        return result;
    }

    if (typ === "any") return val;
    if (typ === null) {
        if (val === null) return val;
        return invalidValue(typ, val, key, parent);
    }
    if (typ === false) return invalidValue(typ, val, key, parent);
    let ref: any = undefined;
    while (typeof typ === "object" && typ.ref !== undefined) {
        ref = typ.ref;
        typ = typeMap[typ.ref];
    }
    if (Array.isArray(typ)) return transformEnum(typ, val);
    if (typeof typ === "object") {
        return typ.hasOwnProperty("unionMembers") ? transformUnion(typ.unionMembers, val)
            : typ.hasOwnProperty("arrayItems")    ? transformArray(typ.arrayItems, val)
            : typ.hasOwnProperty("props")         ? transformObject(getProps(typ), typ.additional, val)
            : invalidValue(typ, val, key, parent);
    }
    // Numbers can be parsed by Date but shouldn't be.
    if (typ === Date && typeof val !== "number") return transformDate(val);
    return transformPrimitive(typ, val);
}

function cast<T>(val: any, typ: any): T {
    return transform(val, typ, jsonToJSProps);
}

function uncast<T>(val: T, typ: any): any {
    return transform(val, typ, jsToJSONProps);
}

function l(typ: any) {
    return { literal: typ };
}

function a(typ: any) {
    return { arrayItems: typ };
}
//
// function u(...typs: any[]) {
//     return { unionMembers: typs };
// }

function o(props: any[], additional: any) {
    return { props, additional };
}

// function m(additional: any) {
//     return { props: [], additional };
// }

function r(name: string) {
    return { ref: name };
}

const typeMap: any = {
    "FixedPage": o([
        { json: "id", js: "id", typ: "" },
        { json: "maxNode", js: "maxNode", typ: 3.14 },
        { json: "nodeShow", js: "nodeShow", typ: r("NodeShowType") },
        { json: "refIn", js: "refIn", typ: true },
        { json: "refOutDepth", js: "refOutDepth", typ: 3.14 },
        { json: "table", js: "table", typ: "" },
    ], false),
    "NodeShowType": o([
        { json: "containEnum", js: "containEnum", typ: true },
        { json: "keywordColors", js: "keywordColors", typ: a(r("KeywordColor")) },
        { json: "nodePlacementStrategy", js: "nodePlacementStrategy", typ: r("NodePlacementStrategyType") },
        { json: "showDescription", js: "showDescription", typ: r("ShowDescriptionType") },
        { json: "showHead", js: "showHead", typ: r("ShowHeadType") },
        { json: "tableHideAndColors", js: "tableHideAndColors", typ: a(r("TableHideAndColor")) },
    ], false),
    "KeywordColor": o([
        { json: "color", js: "color", typ: "" },
        { json: "keyword", js: "keyword", typ: "" },
    ], false),
    "TableHideAndColor": o([
        { json: "color", js: "color", typ: "" },
        { json: "hide", js: "hide", typ: true },
        { json: "keyword", js: "keyword", typ: "" },
    ], false),
    "TauriConf": o([
        { json: "resDirs", js: "resDirs", typ: a(r("ResDir")) },
    ], false),
    "ResDir": o([
        { json: "dir", js: "dir", typ: "" },
    ], false),
    "NodePlacementStrategyType": [
        "BRANDES_KOEPF",
        "LINEAR_SEGMENTS",
        "SIMPLE",
    ],
    "ShowDescriptionType": [
        "none",
        "show",
        "showFallbackValue",
        "showValue",
    ],
    "ShowHeadType": [
        "show",
        "showCopyable",
    ],
};
