import {Item} from "rete-context-menu-plugin/_types/types";

export interface EntityField {
    name: string;
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

    nodeType?: EntityNodeType;
    userData? : any;
}

export interface EntityGraph {
    entityMap: Map<string, Entity>;
    menu : Item[];
}

export enum EntityNodeType {
    Normal,
    Ref,
}

export enum EntityConnectionType {
    Normal,
    Ref = 1,
}