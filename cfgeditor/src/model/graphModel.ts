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

    fieldsShow: FieldsShowType;
    nodeType?: EntityNodeType;
    userData?: any;
}

export interface EntityGraph {
    entityMap: Map<string, Entity>;
    menu: Item[];
    nodeMenuFunc?: (node: Entity) => Item[];
}

export enum FieldsShowType {
    Direct,
    Expand,
    Fold,
    Edit,
}

export enum EntityNodeType {
    Normal,
    Ref,
    Ref2,
    RefIn,
}

export enum EntityConnectionType {
    Normal,
    Ref = 1,
}

export function fillInputs(entityMap: Map<string, Entity>) {
    let nodeIdSet = new Set<string>();
    for (let entity of entityMap.values()) {
        for (let output of entity.outputs) {
            for (let connectToSocket of output.connectToSockets) {
                nodeIdSet.add(connectToSocket.nodeId);
            }
        }
    }

    for (let id of nodeIdSet) {
        let entity = entityMap.get(id);
        if (entity) {
            entity.inputs = [{key: 'input'}];
        }
    }
}
