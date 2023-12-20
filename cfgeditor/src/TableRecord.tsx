import {Schema, STable} from "./model/schemaModel.ts";
import {useRete} from "rete-react-plugin";
import {createEditor} from "./editor.tsx";
import {useCallback, useEffect, useReducer, useState} from "react";
import {Entity, EntityGraph, fillInputs} from "./model/entityModel.ts";
import {Item} from "rete-context-menu-plugin/_types/types";
import {RecordResult, RefId} from "./model/recordModel.ts";
import {App, Empty, Result, Spin} from "antd";
import {createRefEntities, getId} from "./func/recordRefEntity.ts";
import {RecordEntityCreator} from "./func/RecordEntityCreator.ts";
import {RecordEditEntityCreator} from "./func/RecordEditEntityCreator.ts";
import {editingState} from "./func/editingRecord.ts";


export function TableRecordLoaded({schema, curTable, curId, recordResult, setCurTableAndId}: {
    schema: Schema;
    curTable: STable;
    curId: string;
    recordResult: RecordResult;
    setCurTableAndId: (table: string, id: string) => void;
}) {

    const [editMode, setEditMode] = useState<boolean>(false);
    const [forceUpdate, setForceUpdate] = useReducer(x => x + 1, 0);

    function createGraph(): EntityGraph {
        const entityMap = new Map<string, Entity>();
        let refId = {table: curTable.name, id: curId};
        let entityId = getId(curTable.name, curId);
        let isEditable = schema.isEditable && curTable.isEditable;
        let isEditing = isEditable && editMode;
        if (!isEditing) {
            createRefEntities(entityMap, recordResult.refs, false);
            let creator = new RecordEntityCreator(entityMap, schema, refId, recordResult.refs);
            creator.createEntity(entityId, recordResult.object);
        } else {

            function OnSubmit() {
                console.log(editingState.editingObject);

            }
            editingState.startEditingObject(recordResult, setForceUpdate);
            let creator = new RecordEditEntityCreator(entityMap, schema, curTable, curId, OnSubmit);
            creator.createThis();
        }
        fillInputs(entityMap);

        const menu: Item[] = [];

        const entityMenuFunc = (entity: Entity): Item[] => {
            let refId = entity.userData as RefId;
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
        return {entityMap, menu, entityMenuFunc}
    }


    const create = useCallback(
        (el: HTMLElement) => {
            return createEditor(el, createGraph());
        },
        [recordResult, editMode, forceUpdate]
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

