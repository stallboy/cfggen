import {Item} from "rete-context-menu-plugin/_types/types";

export interface EntityField {
    name: string;
    comment?: string;
    value: string | number | boolean;
    key: string;
}

export interface ConnectTo {
    nodeId: string;
    inputKey: string;

    connectionType?: EntityConnectionType;
}

export interface EntitySocket {
    key: string;
    label?: string;
}

export interface EntitySocketOutput {
    output: EntitySocket;
    connectToSockets: ConnectTo[];
}

export interface Entity {
    id: string;
    label: string;
    fields: EntityField[];
    inputs: EntitySocket[];
    outputs: EntitySocketOutput[];

    fieldsShow: FieldsShow;
    nodeType?: EntityNodeType;
    userData?: any;
}

export interface EntityGraph {
    entityMap: Map<string, Entity>;
    menu: Item[];
    nodeMenuFunc?: (node: Entity) => Item[];
}

export type FieldsShow = 'direct' | 'expand' | 'fold';

export enum EntityNodeType {
    Normal,
    Ref,
    Ref2,
}

export enum EntityConnectionType {
    Normal,
    Ref = 1,
}