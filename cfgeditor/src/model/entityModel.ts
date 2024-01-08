import {Item} from "rete-context-menu-plugin/_types/types";

export interface EntityField {
    name: string;
    comment?: string;
    value: string;
    key: string;
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

export interface EntityBrief {
    img?: string;
    title?: string;
    description?: string;
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

export interface NodeShowType {
    showHead: ShowHeadType;
    showDescription: ShowDescriptionType;
    containEnum: boolean;
    nodePlacementStrategy: NodePlacementStrategyType;
    keywordColors: KeywordColor[];
}

export interface KeywordColor {
    keyword: string;
    color: string;
}

export type ShowHeadType = 'show' | 'showCopyable';
export type ShowDescriptionType = 'show' | 'showFallbackValue' | 'showValue' | 'none';
export type NodePlacementStrategyType = 'SIMPLE' | 'LINEAR_SEGMENTS' | 'BRANDES_KOEPF';


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

