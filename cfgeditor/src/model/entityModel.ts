import {BriefDescription} from "./recordModel.ts";
import {NodeShowType} from "../func/localStoreJson.ts";

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


export type EditFieldType = 'arrayOfPrimitive' | 'primitive' | 'funcAdd' | 'interface' | 'funcSubmit' | 'funcDelete'; // interface: value:string
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
}

export interface EntityEditFieldOption {
    value: string;
    label: string;
}

export interface EntityEdit {
    editFields: EntityEditField[];
    editOnUpdateValues: (values: any) => void;
}

export interface EntityBrief {
    img?: string;
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

    userData?: any;
    query?: string;
    nodeShow?: NodeShowType;
}

export interface EntityGraph {
    entityMap: Map<string, Entity>;
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
