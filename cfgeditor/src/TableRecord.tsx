import {Schema, STable} from "./model/schemaModel.ts";
import {useRete} from "rete-react-plugin";
import {createEditor} from "./editor.tsx";
import {useCallback, useEffect, useState} from "react";
import {Entity, fillInputs} from "./model/graphModel.ts";
import {Item} from "rete-context-menu-plugin/_types/types";
import {RecordResult, RefId} from "./model/recordModel.ts";
import {App, Empty, Result, Spin} from "antd";
import {createRefNodes, getId} from "./model/recordRefGraph.ts";
import {RecordNodeCreator} from "./model/recordGraph.ts";


export function TableRecordLoaded({schema, curTable, curId, recordResult, setCurTableAndId}: {
    schema: Schema;
    curTable: STable;
    curId: string;
    recordResult: RecordResult;
    setCurTableAndId: (table: string, id: string) => void;
}) {

    const [editMode, setEditMode] = useState<boolean>(false);

    const entityMap = new Map<string, Entity>();
    createRefNodes(entityMap, recordResult.refs, false);
    let refId = {table: curTable.name, id: curId};
    let entityId = getId(curTable.name, curId);
    let isEditable = schema.isEditable && curTable.isEditable;
    let isEditing = isEditable && editMode;
    let recordNodeCreator = new RecordNodeCreator(entityMap, schema, refId, recordResult.refs, isEditing);
    recordNodeCreator.createNodes(entityId, recordResult.object);
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

    const create = useCallback(
        (el: HTMLElement) => {
            return createEditor(el, {entityMap, menu, nodeMenuFunc});
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
                              recordResult={recordResult}
                              setCurTableAndId={setCurTableAndId}/>

}

