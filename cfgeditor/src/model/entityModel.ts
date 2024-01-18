import {Item} from "rete-context-menu-plugin/_types/types";
import {BriefDescription} from "./recordModel.ts";
import {NodeShowType} from "../func/localStoreJson.ts";

export interface EntityField {
    name: string;
    comment?: string;
    value: string;
    key: string;
    handleOut?: boolean; // <name>
    handleIn?: boolean;  // @in_<name>
}

export interface EntityEditFieldOption {
    value: string;
    label: string;
}

export interface EntityEditFieldOptions {
    options: EntityEditFieldOption[];
    isValueInteger: boolean;
}

export interface EntityEditField {
    name: string;
    comment?: string;
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

export interface EntitySourceEdge {
    sourceHandle: string;
    target: string;
    targetHandle: string;
    type: EntityEdgeType;
}

export interface EntityBrief {
    img?: string;
    title?: string;
    descriptions?: BriefDescription[];
    value: string;
}

export interface Entity {
    id: string;
    label: string;

    // table/record -> table
    fields?: EntityField[];

    // edit -> form
    editFields?: EntityEditField[];
    editOnUpdateValues?: (values: any) => void;

    // brief -> card
    brief?: EntityBrief;

    inputs: EntitySocket[];
    outputs: EntitySocketOutput[];

    sourceEdges: EntitySourceEdge[];
    handleIn?: boolean;  // @in
    handleOut?: boolean; // @out

    fieldsShow: FieldsShowType;
    entityType?: EntityType;
    userData?: any;
}

export interface EntityGraph {
    entityMap: Map<string, Entity>;
    menu: Item[];
    entityMenuFunc?: (entity: Entity) => Item[];

    query?: string;
    nodeShow?: NodeShowType;
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

export enum EntityEdgeType {
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

function findField(fields: EntityField[], key: string) {
    for (let field of fields) {
        if (field.key == key) {
            return field;
        }
    }
    return null;
}


export function fillHandles(entityMap: Map<string, Entity>) {
    for (let entity of entityMap.values()) {
        for (let {sourceHandle, target, targetHandle} of entity.sourceEdges) {
            if (sourceHandle == '@out') {
                entity.handleOut = true;
            } else {
                let field = findField(entity.fields!, sourceHandle);
                field!.handleOut = true;
            }

            let targetEntity = entityMap.get(target);
            if (targetEntity) {
                if (targetHandle == '@in') {
                    targetEntity.handleIn = true;
                } else if (targetHandle.startsWith('@in_')) {
                    let field = findField(targetEntity.fields!, targetHandle.substring(4));
                    field!.handleIn = true;
                }else{
                    console.error(targetHandle + ' not found');
                }
            }

        }
    }
}
