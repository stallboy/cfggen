import {Schema, STable} from "./model/schemaModel.ts";
import {useRete} from "rete-react-plugin";
import {createEditor} from "./editor.tsx";
import {useCallback, useEffect, useState} from "react";
import {Entity, fillInputs} from "./model/graphModel.ts";
import {Item} from "rete-context-menu-plugin/_types/types";
import {JSONObject, RecordResult, RefId, Refs} from "./model/recordModel.ts";
import {App, Empty, Result, Spin} from "antd";
import {createRefNodes, getId} from "./model/recordRefNode.ts";
import {RecordNodeCreator} from "./model/RecordNodeCreator.ts";
import {RecordEditNodeCreator} from "./model/RecordEditNodeCreator.ts";


export function TableRecordLoaded({schema, curTable, curId, recordResult, setCurTableAndId}: {
    schema: Schema;
    curTable: STable;
    curId: string;
    recordResult: RecordResult;
    setCurTableAndId: (table: string, id: string) => void;
}) {

    const [editMode, setEditMode] = useState<boolean>(false);
    const [editingRecord, setEditingRecord] = useState<(JSONObject & Refs) | null>(null);

    function createGraph() {
        const entityMap = new Map<string, Entity>();
        let refId = {table: curTable.name, id: curId};
        let entityId = getId(curTable.name, curId);
        let isEditable = schema.isEditable && curTable.isEditable;
        let isEditing = isEditable && editMode;
        if (!isEditing) {
            createRefNodes(entityMap, recordResult.refs, false);
            let recordNodeCreator = new RecordNodeCreator(entityMap, schema, refId, recordResult.refs);
            recordNodeCreator.createNodes(entityId, recordResult.object);
        } else {
            let recordEditNodeCreator = new RecordEditNodeCreator(entityMap, schema, refId, setEditingRecord);
            recordEditNodeCreator.createNodes(entityId, curTable, editingRecord ?? recordResult.object);
        }
        fillInputs(entityMap);

        const menu: Item[] = [];

        const nodeMenuFunc = (node: Entity): Item[] => {
            let refId = node.userData as RefId;
            if (refId.table != curTable.name || refId.id != curId) {  // ref节点
                return [
                    {
                        label: '数据',
                        key: '数据',
                        handler() {
                            setCurTableAndId(refId.table, refId.id);
                        }
                    },
                ];
            } else if (isEditable) { // 本节点
                if (editMode) {
                    return [
                        {
                            label: '只读',
                            key: '只读',
                            handler() {
                                setEditMode(false);
                            }
                        },
                    ];
                } else {
                    return [
                        {
                            label: '编辑',
                            key: '编辑',
                            handler() {
                                setEditMode(true);
                            }
                        },
                    ];
                }

            }
            return [];
        }
        return {entityMap, menu, nodeMenuFunc}
    }


    const create = useCallback(
        (el: HTMLElement) => {
            return createEditor(el, createGraph());
        },
        [recordResult, editMode]
    );
    const [ref] = useRete(create);

    return <div ref={ref} style={{height: "100vh", width: "100vw"}}></div>
}

export function TableRecord({schema, curTable, curId, server, tryReconnect, setCurTableAndId}: {
    schema: Schema;
    curTable: STable;
    curId: string;
    server: string;
    tryReconnect: () => void;
    setCurTableAndId: (table: string, id: string) => void;
}) {
    const [recordResult, setRecordResult] = useState<RecordResult | null>(null);
    const {notification} = App.useApp();

    useEffect(() => {
        setRecordResult(null);
        let url = `http://${server}/record?table=${curTable.name}&id=${curId}&depth=1`;
        const fetchData = async () => {
            const response = await fetch(url);
            const recordResult: RecordResult = await response.json();
            setRecordResult(recordResult);
            // notification.info({message: `fetch ${url} ok`, placement: 'topRight', duration: 2});
        }

        fetchData().catch((err) => {
            notification.error({message: `fetch ${url} err: ${err.toString()}`, placement: 'topRight', duration: 4});
            tryReconnect();
        });
    }, [schema, server, curTable, curId]);


    if (recordResult == null) {
        return <Empty> <Spin/> </Empty>
    }

    if (recordResult.resultCode != 'ok') {
        return <Result status={'error'} title={recordResult.resultCode}/>
    }

    return <TableRecordLoaded schema={schema} curTable={curTable} curId={curId}
                              key={`${curTable.name}-${curId}`}
                              recordResult={recordResult}
                              setCurTableAndId={setCurTableAndId}/>

}

