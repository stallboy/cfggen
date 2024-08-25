import {BriefDescription, JSONObject} from "../routes/record/recordModel.ts";
import {NodeShowType} from "../routes/setting/storageJson.ts";

import {ResInfo} from "../res/resInfo.ts";

export interface EntityBaseField {
    name: string;
    comment?: string;
    handleOut?: boolean; // <name>
    handleIn?: boolean;  // @in_<name>
}

export interface EntityField extends EntityBaseField {
    value: string;
    key: string;
}


export interface EntityEditField extends EntityBaseField {
    type: EditFieldType;
    eleType: string;
    value: EditFieldValueType;
    autoCompleteOptions?: EntityEditFieldOptions; // use AutoComplete if it has autoCompleteOptions
    implFields?: EntityEditField[];
    interfaceOnChangeImpl?: ((impl: string) => void)
}


export type EditFieldType = 'arrayOfPrimitive' | 'primitive' | 'structRef' |
    'funcAdd' | 'interface' | 'funcSubmit'; // | 'funcDelete'; // interface: value:string
export type PrimitiveType = 'bool' | 'int' | 'long' | 'float' | 'str' | 'text';
export type EditFieldValueType =
    string
    | number
    | boolean
    | string[]
    | number[]
    | boolean[]
    | FuncType
    | FuncSubmitType;

export type FuncType = () => void;

export interface FuncSubmitType {
    funcSubmit: FuncType;
    funcClear: FuncType;
}

export interface EntityEditFieldOptions {
    options: EntityEditFieldOption[];
    isValueInteger: boolean;
    isEnum: boolean;
}

export interface EntityEditFieldOption {
    value: string | number;
    label: string;
}

export interface EntityEdit {
    editFields: EntityEditField[];
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    editOnUpdateValues: (values: any) => void;
    editOnUpdateNote: (note?:string) => void;
    editOnDelete?: () => void;
    editOnMoveUp?: () => void;
    editOnMoveDown?: () => void;

    editFieldChain?: (string | number)[];
    editObj?: JSONObject;
    editAllowObjType? : string;
}

export interface EntityBrief {
    title?: string;
    descriptions?: BriefDescription[];
    value: string;
}

export interface EntitySourceEdge {
    sourceHandle: string;
    target: string;
    targetHandle: string;
    type: EntityEdgeType;
    label?: string;
}

export interface Entity {
    id: string;
    label: string;

    // table/record -> table
    fields?: EntityField[];
    // edit -> form
    edit?: EntityEdit;
    // brief -> card
    brief?: EntityBrief;

    sourceEdges: EntitySourceEdge[];
    handleIn?: boolean;  // @in
    handleOut?: boolean; // @out
    entityType?: EntityType;

    note?:string; // 注释
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    userData?: any;
    sharedSetting?: EntitySharedSetting;
    assets?: ResInfo[];
}

export interface EntityGraph {
    entityMap: Map<string, Entity>;
    sharedSetting?: EntitySharedSetting;
}

export interface EntitySharedSetting {
    notes?: Map<string, string>;
    query?: string;
    nodeShow?: NodeShowType;
}

export enum EntityType {
    Normal,
    Ref,
    Ref2,
    RefIn,
}

export enum EntityEdgeType {
    Normal,
    Ref = 1,
}
