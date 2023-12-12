import {getField, Schema, SField, SStruct, STable} from "./model/schemaModel.ts";
import {useRete} from "rete-react-plugin";
import {createEditor} from "./editor.tsx";
import {useCallback, useEffect, useState} from "react";
import {
    Entity,
    EntityConnectionType,
    EntityField,
    EntityNodeType,
    EntitySocketOutput,
} from "./model/graphModel.ts";
import {Item} from "rete-context-menu-plugin/_types/types";
import {JSONArray, JSONObject, JSONValue, RecordResult, RefId, Refs, TableMap} from "./model/recordModel.ts";
import {Empty, Result, Spin} from "antd";


function getLabel(id: string): string {
    let seps = id.split('.');
    return seps[seps.length - 1];
}

function getId(table: string, id: string): string {
    return table + "-" + id;
}

function createNodes(entityMap: Map<string, Entity>, schema: Schema, refId: RefId, id: string, obj: JSONObject & Refs): Entity | null {
    let fields: EntityField[] = [];
    let type: string = obj['$type'] as string;
    if (type == null) {
        console.error('$type missing');
        return null;
    }

    let label = getLabel(type);

    let sItem: STable | SStruct | null = null;
    if (!type.startsWith("$")) {
        sItem = schema.itemIncludeImplMap.get(type) as STable | SStruct;
        if (sItem == null) {
            console.error(type + ' not found!');
            return null;
        }
    }

    let outputs: EntitySocketOutput[] = [];

    for (let fieldKey in obj) {
        if (fieldKey.startsWith("$")) {
            continue;
        }
        let fieldValue: JSONValue = obj[fieldKey];

        let sField: SField | null = null;
        if (sItem) {
            sField = getField(sItem, fieldKey);
        }
        let comment = sField?.comment ?? fieldKey;

        let ft = typeof fieldValue
        if (ft == 'object') {
            if (Array.isArray(fieldValue)) {
                let fArr: JSONArray = fieldValue as JSONArray;
                if (fArr.length == 0) {
                    fields.push({
                        key: fieldKey,
                        name: fieldKey,
                        comment: comment,
                        value: '[]'
                    });
                } else {
                    let ele = fArr[0];
                    if (typeof ele == 'object') {
                        let i = 0;
                        let connectToSockets = [];
                        for (let e of fArr) {
                            let fObj: JSONObject & Refs = e as JSONObject & Refs;
                            let childId: string = `${id}-${fieldKey}[${i}]`;
                            let childNode = createNodes(entityMap, schema, refId, childId, fObj);
                            i++;

                            if (childNode) {
                                connectToSockets.push({
                                    nodeId: childNode.id,
                                    inputKey: 'input',
                                    connectionType: EntityConnectionType.Normal
                                });
                            }
                        }

                        outputs.push({
                            output: {key: fieldKey, label: fieldKey},
                            connectToSockets: connectToSockets
                        });

                    } else {
                        let i = 0;
                        for (let e of fArr) {
                            fields.push({
                                key: `${fieldKey}[${i}]`,
                                name: i == 0 ? fieldKey : "",
                                comment: i == 0 ? comment : "",
                                value: e.toString(),
                            });
                            i++;
                        }

                    }
                }
            } else {
                let fObj: JSONObject & Refs = fieldValue as JSONObject & Refs;
                let childId: string = id + "-" + fieldKey;
                let childNode = createNodes(entityMap, schema, refId, childId, fObj);
                if (childNode) {
                    outputs.push({
                        output: {key: fieldKey, label: fieldKey},
                        connectToSockets: [{
                            nodeId: childNode.id,
                            inputKey: 'input',
                            connectionType: EntityConnectionType.Normal
                        }]
                    });
                }
            }
        } else {
            let valueStr: string = fieldValue.toString();
            if (ft == 'boolean') {
                let fb = fieldValue as boolean
                valueStr = fb ? '✔️' : '✘';
            }

            fields.push({
                key: fieldKey,
                name: fieldKey,
                comment: comment,
                value: valueStr
            });
        }
    }

    let entity: Entity = {
        id: id,
        label: label,
        fields: fields,
        inputs: [{key: 'input'}],
        outputs: outputs,

        fieldsShow: 'direct',
        nodeType: EntityNodeType.Normal,
        userData: refId,
    };

    entityMap.set(id, entity);
    createRefs(entity, obj);
    return entity;
}

function createRefs(node: Entity, refs: Refs) {
    let refIdMap = refs.$refs;
    if (refIdMap == null) {
        return;
    }
    for (let refName in refIdMap) {
        let refIds: RefId[] = refIdMap[refName];
        let connectToSockets = [];
        for (let refId of refIds) {
            connectToSockets.push({
                nodeId: getId(refId.table, refId.id),
                inputKey: 'input',
                connectionType: EntityConnectionType.Ref
            });
        }
        node.outputs.push({
            output: {key: refName, label: refName},
            connectToSockets: connectToSockets
        });
    }
}

function createRefNodes(entityMap: Map<string, Entity>, refs: TableMap) {
    for (let table in refs) {
        let recordMap = refs[table]
        for (let id in recordMap) {
            let refId: RefId = {table, id};
            let refRecord: Refs = recordMap[id];
            let eid = getId(table, id);
            let entity: Entity = {
                id: eid,
                label: getLabel(table),
                fields: [{key: "id", name: "id", value: id}],
                inputs: [{key: "input"}],
                outputs: [],

                fieldsShow: 'direct',
                nodeType: EntityNodeType.Ref,
                userData: refId,
            };

            entityMap.set(eid, entity);
            createRefs(entity, refRecord);
        }
    }
}


export function TableRecordLoaded({schema, curTable, curId, recordResult, setCurTableAndId}: {
    schema: Schema;
    curTable: STable;
    curId: string;
    recordResult: RecordResult;
    setCurTableAndId: (table: string, id: string) => void;
}) {

    const entityMap = new Map<string, Entity>();

    createRefNodes(entityMap, recordResult.refs);
    let refId = {table: curTable.name, id: curId};
    let entityId = getId(curTable.name, curId);
    createNodes(entityMap, schema, refId, entityId, recordResult.object);

    const menu: Item[] = [];

    const nodeMenuFunc = (node: Entity): Item[] => {
        let refId = node.userData as RefId;
        if (refId.table != curTable.name || refId.id != curId) {

            return [{
                label: '数据',
                key: '数据',
                handler: () => {
                    setCurTableAndId(refId.table, refId.id);
                }
            }];

        }
        return [];
    }

    const create = useCallback(
        (el: HTMLElement) => {
            return createEditor(el, {entityMap, menu, nodeMenuFunc});
        },
        [recordResult]
    );
    const [ref] = useRete(create);


    return <div ref={ref} style={{height: "100vh", width: "100vw"}}></div>
}

export function TableRecord({schema, curTable, curId, setCurTableAndId}: {
    schema: Schema;
    curTable: STable;
    curId: string;
    setCurTableAndId: (table: string, id: string) => void;
}) {
    const [recordResult, setRecordResult] = useState<RecordResult | null>(null);

    useEffect(() => {
        setRecordResult(null);
        const fetchData = async () => {
            const response = await fetch(`http://localhost:3456/record?table=${curTable.name}&id=${curId}&depth=1`);
            const recordResult: RecordResult = await response.json();
            setRecordResult(recordResult);
        }

        fetchData().catch(console.error);
    }, [schema, curTable, curId]);


    if (recordResult == null) {
        return <Empty> <Spin/> </Empty>
    }

    if (recordResult.resultCode != 'ok') {
        return <Result status={'error'} title={recordResult.resultCode}/>
    }

    return <TableRecordLoaded schema={schema} curTable={curTable} curId={curId}
                              recordResult={recordResult}
                              setCurTableAndId={setCurTableAndId}/>


}

