import {STable} from "./model/schemaModel.ts";
import {useRete} from "rete-react-plugin";
import {createEditor} from "./editor.tsx";
import {useCallback, useEffect, useState} from "react";
import {Entity, EntityConnectionType, EntityNodeType, FieldsShowType, fillInputs,} from "./model/graphModel.ts";
import {Item} from "rete-context-menu-plugin/_types/types";
import {BriefRecord, RecordRefsResult, RefId, Refs, TableMap} from "./model/recordModel.ts";
import {App, Empty, Result, Spin} from "antd";


function getLabel(id: string): string {
    let seps = id.split('.');
    return seps[seps.length - 1];
}

function getId(table: string, id: string): string {
    return table + "-" + id;
}

function isRefIdInTableMap(refId: RefId, tableMap: TableMap): boolean {
    let recordMap = tableMap[refId.table];
    if (recordMap) {
        let briefRecord = recordMap[refId.id];
        if (briefRecord) {
            return true;
        }
    }
    return false;
}

function createRefs(node: Entity, refs: Refs, tableMap: TableMap) {
    let refIdMap = refs.$refs;
    if (refIdMap == null) {
        return;
    }
    for (let refName in refIdMap) {
        let refIds: RefId[] = refIdMap[refName];
        let connectToSockets = [];
        for (let refId of refIds) {
            if (isRefIdInTableMap(refId, tableMap)) {
                connectToSockets.push({
                    nodeId: getId(refId.table, refId.id),
                    inputKey: 'input',
                    connectionType: EntityConnectionType.Ref
                });
            }
        }
        if (connectToSockets.length > 0) {
            node.outputs.push({
                output: {key: refName, label: refName},
                connectToSockets: connectToSockets
            });
        }
    }
}

function createRefNodes(entityMap: Map<string, Entity>, tableMap: TableMap) {
    for (let table in tableMap) {
        let recordMap = tableMap[table]
        for (let id in recordMap) {
            let briefRecord: BriefRecord = recordMap[id];
            let refId: RefId = {table, id};
            let eid = getId(table, id);

            let nodeType;
            if (briefRecord.depth == 0) {
                nodeType = EntityNodeType.Normal;
            } else if (briefRecord.depth == 1) {
                nodeType = EntityNodeType.Ref;
            } else if (briefRecord.depth > 1) {
                nodeType = EntityNodeType.Ref2;
            } else {
                nodeType = EntityNodeType.RefIn;
            }

            let entity: Entity = {
                id: eid,
                label: getLabel(table),
                fields: [{key: 'id', name: 'id', value: id},
                    {key: 'value', name: 'value', value: briefRecord.value}
                ],
                inputs: [],
                outputs: [],

                fieldsShow: FieldsShowType.Direct,
                nodeType,
                userData: refId,
            };
            createRefs(entity, briefRecord, tableMap);
            entityMap.set(eid, entity);
        }
    }
}


export function TableRecordRefLoaded({curTable, curId, recordRefResult, setCurTableAndId}: {
    curTable: STable;
    curId: string;
    recordRefResult: RecordRefsResult;
    setCurTableAndId: (table: string, id: string) => void;
}) {

    const entityMap = new Map<string, Entity>();
    createRefNodes(entityMap, recordRefResult.refs);
    fillInputs(entityMap);

    const menu: Item[] = [];

    const nodeMenuFunc = (node: Entity): Item[] => {
        let refId = node.userData as RefId;
        if (refId.table != curTable.name || refId.id != curId) {

            return [{
                label: '数据关系',
                key: '数据关系',
                handler() {
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
        [recordRefResult]
    );
    const [ref] = useRete(create);


    return <div ref={ref} style={{height: "100vh", width: "100vw"}}></div>
}

export function TableRecordRef({curTable, curId, refIn, refOutDepth, maxNode, server, tryReconnect, setCurTableAndId}: {
    curTable: STable;
    curId: string;
    refIn: boolean;
    refOutDepth: number;
    maxNode: number;
    server: string;
    tryReconnect: () => void;
    setCurTableAndId: (table: string, id: string) => void;
}) {
    const [recordRefResult, setRecordRefResult] = useState<RecordRefsResult | null>(null);
    const {notification} = App.useApp();

    useEffect(() => {
        setRecordRefResult(null);
        let url = `http://${server}/record?table=${curTable.name}&id=${curId}&depth=${refOutDepth}&maxObjs=${maxNode}&refs${refIn ? '&in' : ''}`;
        const fetchData = async () => {
            const response = await fetch(url);
            const recordResult: RecordRefsResult = await response.json();
            setRecordRefResult(recordResult);
            // notification.info({message: `fetch ${url} ok`, placement: 'topRight', duration: 2});
        }

        fetchData().catch((err) => {
            notification.error({message: `fetch ${url} err: ${err.toString()}`, placement: 'topRight', duration: 4});
            tryReconnect();
        });
    }, [server, curTable, curId, refOutDepth, maxNode, refIn]);


    if (recordRefResult == null) {
        return <Empty> <Spin/> </Empty>
    }

    if (recordRefResult.resultCode != 'ok') {
        return <Result status={'error'} title={recordRefResult.resultCode}/>
    }

    return <TableRecordRefLoaded curTable={curTable} curId={curId}
                                 recordRefResult={recordRefResult}
                                 setCurTableAndId={setCurTableAndId}/>


}

