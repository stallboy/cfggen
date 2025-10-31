import {BriefDescription, JSONObject} from "../routes/record/recordModel.ts";
import {NodeShowType} from "../store/storageJson.ts";

import {ResInfo} from "../res/resInfo.ts";
import * as React from "react";

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
    interfaceOnChangeImpl?: ((impl: string, position: EntityPosition) => void)
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
    | FuncAddType
    | FuncSubmitType;

export type FuncType = () => void;
export type FuncAddType = (position: EntityPosition) => void;

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
    label?: React.ReactNode;
    labelStr: string;
    title: string;
}

export interface EntityPosition {
    id: string;
    x: number;
    y: number;
}

export interface EntityEdit {
    editFields: EntityEditField[];
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    editOnUpdateValues: (values: any) => void;
    editOnUpdateNote: (note?: string) => void;
    editOnUpdateFold: (fold: boolean, position: EntityPosition) => void;
    editOnDelete?: (position: EntityPosition) => void;
    editOnMoveUp?: (position: EntityPosition) => void;
    editOnMoveDown?: (position: EntityPosition) => void;

    editFieldChain?: (string | number)[];
    editObj?: JSONObject;
    editAllowObjType?: string;
    fold?: boolean;
    hasChild: boolean; // 是否有子节点
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

    note?: string; // 注释
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
