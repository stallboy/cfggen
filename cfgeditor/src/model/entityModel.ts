import {Item} from "rete-context-menu-plugin/_types/types";

export interface EntityField {
    name: string;
    comment?: string;
    value: string;
    key: string;
}

export interface EntityEditField {
    name: string;
    comment?: string;
    type: EditFieldType;
    eleType: string;
    value: EditFieldValueType;
    autoCompleteOptions?: string[]; // use Select if it has autoCompleteOptions
    implFields?: EntityEditField[];
    interfaceOnChangeImpl?: ((impl: string) => void)
}

export type EditFieldType = 'arrayOfPrimitive' | 'primitive' | 'funcAdd' | 'interface' | 'funcSubmit' | 'funcDelete'; // interface: value:string
export type PrimitiveType = 'bool' | 'int' | 'long' | 'float' | 'str' | 'text';
export type EditFieldValueType = string | number | boolean | string[] | number[] | boolean[] | ((value: any) => void);

export interface ConnectTo {
    entityId: string;
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
    editFields?: EntityEditField[];
    editOnUpdateValues?: (values: any) => void;

    inputs: EntitySocket[];
    outputs: EntitySocketOutput[];

    fieldsShow: FieldsShowType;
    entityType?: EntityType;
    userData?: any;
}

export interface EntityGraph {
    entityMap: Map<string, Entity>;
    menu: Item[];
    entityMenuFunc?: (entity: Entity) => Item[];
}

export enum FieldsShowType {
    Direct,
    Expand,
    Fold,
    Edit,
}

export enum EntityType {
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
    let entityIdSet = new Set<string>();
    for (let entity of entityMap.values()) {
        for (let output of entity.outputs) {
            for (let connectToSocket of output.connectToSockets) {
                entityIdSet.add(connectToSocket.entityId);
            }
        }
    }

    for (let id of entityIdSet) {
        let entity = entityMap.get(id);
        if (entity) {
            entity.inputs = [{key: 'input'}];
        }
    }
}
