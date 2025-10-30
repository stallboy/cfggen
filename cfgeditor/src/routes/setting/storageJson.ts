// To parse this data:
//
//   import { Convert, NodeShowType, KeywordColor, ShowDescriptionType, NodePlacementStrategyType, FixedPage, FixedPagesConf, ResDir, TauriConf, AIConf, ThemeConfig } from "./file";
//
//   const nodeShowType = Convert.toNodeShowType(json);
//   const keywordColor = Convert.toKeywordColor(json);
//   const showDescriptionType = Convert.toShowDescriptionType(json);
//   const nodePlacementStrategyType = Convert.toNodePlacementStrategyType(json);
//   const fixedPage = Convert.toFixedPage(json);
//   const fixedPagesConf = Convert.toFixedPagesConf(json);
//   const resDir = Convert.toResDir(json);
//   const tauriConf = Convert.toTauriConf(json);
//   const aIConf = Convert.toAIConf(json);
//   const themeConfig = Convert.toThemeConfig(json);
//
// These functions will throw an error if the JSON doesn't
// match the expected interface, even if the JSON is valid.

export interface FixedPagesConf {
    pages: FixedPage[];
}

export interface FixedPage {
    id:          string;
    label:       string;
    maxNode:     number;
    nodeShow:    NodeShowType;
    refIn:       boolean;
    refOutDepth: number;
    table:       string;
}

export interface NodeShowType {
    edgeColor:          string;
    edgeStrokeWidth:    number;
    editFoldColor:      string;
    editLayout:         NodePlacementStrategyType;
    editNodeWidth:      number;
    fieldColorsByName:  KeywordColor[];
    layeredNodeSpacing: number;
    layeredSpacing:     number;
    mrtreeSpacing:      number;
    nodeColorsByLabel:  KeywordColor[];
    nodeColorsByValue:  KeywordColor[];
    nodeWidth:          number;
    recordLayout:       NodePlacementStrategyType;
    refContainEnum:     boolean;
    refIsShowCopyable:  boolean;
    refLayout:          NodePlacementStrategyType;
    refShowDescription: ShowDescriptionType;
    refTableHides:      string[];
    tableLayout:        NodePlacementStrategyType;
    tableRefLayout:     NodePlacementStrategyType;
}

export type NodePlacementStrategyType = "BRANDES_KOEPF" | "LINEAR_SEGMENTS" | "SIMPLE" | "mrtree";

export interface KeywordColor {
    color:   string;
    keyword: string;
}

export type ShowDescriptionType = "none" | "show" | "showFallbackValue" | "showValue";

export interface TauriConf {
    assetDir:      string;
    assetRefTable: string;
    resDirs:       ResDir[];
}

export interface ResDir {
    dir:       string;
    lang?:     string;
    txtAsSrt?: boolean;
}

export interface AIConf {
    apiKey:  string;
    baseUrl: string;
    model:   string;
}

export interface ThemeConfig {
    themeFile?: string;
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

    public static toFixedPagesConf(json: string): FixedPagesConf {
        return cast(JSON.parse(json), r("FixedPagesConf"));
    }

    public static fixedPagesConfToJson(value: FixedPagesConf): string {
        return JSON.stringify(uncast(value, r("FixedPagesConf")), null, 2);
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

    public static toAIConf(json: string): AIConf {
        return cast(JSON.parse(json), r("AIConf"));
    }

    public static aIConfToJson(value: AIConf): string {
        return JSON.stringify(uncast(value, r("AIConf")), null, 2);
    }

    public static toThemeConfig(json: string): ThemeConfig {
        return cast(JSON.parse(json), r("ThemeConfig"));
    }

    public static themeConfigToJson(value: ThemeConfig): string {
        return JSON.stringify(uncast(value, r("ThemeConfig")), null, 2);
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

function u(...typs: any[]) {
    return { unionMembers: typs };
}

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
    "FixedPagesConf": o([
        { json: "pages", js: "pages", typ: a(r("FixedPage")) },
    ], false),
    "FixedPage": o([
        { json: "id", js: "id", typ: "" },
        { json: "label", js: "label", typ: "" },
        { json: "maxNode", js: "maxNode", typ: 3.14 },
        { json: "nodeShow", js: "nodeShow", typ: r("NodeShowType") },
        { json: "refIn", js: "refIn", typ: true },
        { json: "refOutDepth", js: "refOutDepth", typ: 3.14 },
        { json: "table", js: "table", typ: "" },
    ], false),
    "NodeShowType": o([
        { json: "edgeColor", js: "edgeColor", typ: "" },
        { json: "edgeStrokeWidth", js: "edgeStrokeWidth", typ: 3.14 },
        { json: "editFoldColor", js: "editFoldColor", typ: "" },
        { json: "editLayout", js: "editLayout", typ: r("NodePlacementStrategyType") },
        { json: "editNodeWidth", js: "editNodeWidth", typ: 3.14 },
        { json: "fieldColorsByName", js: "fieldColorsByName", typ: a(r("KeywordColor")) },
        { json: "layeredNodeSpacing", js: "layeredNodeSpacing", typ: 3.14 },
        { json: "layeredSpacing", js: "layeredSpacing", typ: 3.14 },
        { json: "mrtreeSpacing", js: "mrtreeSpacing", typ: 3.14 },
        { json: "nodeColorsByLabel", js: "nodeColorsByLabel", typ: a(r("KeywordColor")) },
        { json: "nodeColorsByValue", js: "nodeColorsByValue", typ: a(r("KeywordColor")) },
        { json: "nodeWidth", js: "nodeWidth", typ: 3.14 },
        { json: "recordLayout", js: "recordLayout", typ: r("NodePlacementStrategyType") },
        { json: "refContainEnum", js: "refContainEnum", typ: true },
        { json: "refIsShowCopyable", js: "refIsShowCopyable", typ: true },
        { json: "refLayout", js: "refLayout", typ: r("NodePlacementStrategyType") },
        { json: "refShowDescription", js: "refShowDescription", typ: r("ShowDescriptionType") },
        { json: "refTableHides", js: "refTableHides", typ: a("") },
        { json: "tableLayout", js: "tableLayout", typ: r("NodePlacementStrategyType") },
        { json: "tableRefLayout", js: "tableRefLayout", typ: r("NodePlacementStrategyType") },
    ], false),
    "KeywordColor": o([
        { json: "color", js: "color", typ: "" },
        { json: "keyword", js: "keyword", typ: "" },
    ], false),
    "TauriConf": o([
        { json: "assetDir", js: "assetDir", typ: "" },
        { json: "assetRefTable", js: "assetRefTable", typ: "" },
        { json: "resDirs", js: "resDirs", typ: a(r("ResDir")) },
    ], false),
    "ResDir": o([
        { json: "dir", js: "dir", typ: "" },
        { json: "lang", js: "lang", typ: u(undefined, "") },
        { json: "txtAsSrt", js: "txtAsSrt", typ: u(undefined, true) },
    ], false),
    "AIConf": o([
        { json: "apiKey", js: "apiKey", typ: "" },
        { json: "baseUrl", js: "baseUrl", typ: "" },
        { json: "model", js: "model", typ: "" },
    ], false),
    "ThemeConfig": o([
        { json: "themeFile", js: "themeFile", typ: u(undefined, "") },
    ], false),
    "NodePlacementStrategyType": [
        "BRANDES_KOEPF",
        "LINEAR_SEGMENTS",
        "mrtree",
        "SIMPLE",
    ],
    "ShowDescriptionType": [
        "none",
        "show",
        "showFallbackValue",
        "showValue",
    ],
};
