import {Schema, STable} from "./model/schemaModel.ts";
import {useRete} from "rete-react-plugin";
import {createEditor} from "./editor.tsx";
import {useCallback, useEffect, useState} from "react";
import {Entity, EntityGraph, fillInputs,} from "./model/entityModel.ts";
import {Item} from "rete-context-menu-plugin/_types/types";
import {RecordRefsResult, RefId} from "./model/recordModel.ts";
import {App, Empty, Result, Spin} from "antd";
import {createRefEntities, getId} from "./func/recordRefEntity.ts";
import {pageRecord} from "./CfgEditorApp.tsx";
import {useTranslation} from "react-i18next";


export function TableRecordRefLoaded({schema, recordRefResult, setCurTableAndId, setCurPage, setEditMode}: {
    schema: Schema;
    recordRefResult: RecordRefsResult;
    setCurTableAndId: (table: string, id: string) => void;
    setCurPage: (page: string) => void;
    setEditMode: (edit: boolean) => void;
}) {

    const [t] = useTranslation();

    function createGraph(): EntityGraph {
        const entityMap = new Map<string, Entity>();
        createRefEntities(entityMap, recordRefResult.refs);
        fillInputs(entityMap);

        const menu: Item[] = [{
            label: recordRefResult.table + "\n" + t('record'),
            key: 'record',
            handler() {
                setCurPage(pageRecord);
            }
        }];

        const entityMenuFunc = (entity: Entity): Item[] => {
            let refId = entity.userData as RefId;
            let id = getId(refId.table, refId.id);
            let mm = [];
            if (refId.table != recordRefResult.table && refId.id != recordRefResult.id) {
                mm.push({
                    label: id + "\n" + t('recordRef'),
                    key: 'entityRecordRef',
                    handler() {
                        setCurTableAndId(refId.table, refId.id);
                    }
                });
            }
            mm.push({
                label: id + "\n" + t('record'),
                key: 'entityRecord',
                handler() {
                    setCurTableAndId(refId.table, refId.id);
                    setCurPage(pageRecord);
                    setEditMode(false);
                }
            });

            let isEntityEditable = schema.isEditable && !!(schema.getSTable(refId.table)?.isEditable);
            if (isEntityEditable) {
                mm.push({
                    label: id + "\n" + t('edit'),
                    key: 'entityEdit',
                    handler() {
                        setCurTableAndId(refId.table, refId.id);
                        setCurPage(pageRecord);
                        setEditMode(true);
                    }
                });
            }
            return mm;
        }
        return {entityMap, menu, entityMenuFunc};
    }

    const create = useCallback(
        (el: HTMLElement) => {
            return createEditor(el, createGraph());
        },
        [recordRefResult]
    );
    const [ref] = useRete(create);


    return <div ref={ref} style={{height: "100vh", width: "100vw"}}></div>
}

export function TableRecordRef({
                                   schema, curTable, curId,
                                   refIn, refOutDepth, maxNode,
                                   server, tryReconnect,
                                   setCurTableAndId, setCurPage, setEditMode
                               }: {
    schema: Schema;
    curTable: STable;
    curId: string;
    refIn: boolean;
    refOutDepth: number;
    maxNode: number;
    server: string;
    tryReconnect: () => void;
    setCurTableAndId: (table: string, id: string) => void;
    setCurPage: (page: string) => void;
    setEditMode: (edit: boolean) => void;
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

    return <TableRecordRefLoaded
        schema={schema}
        recordRefResult={recordRefResult}
        setCurTableAndId={setCurTableAndId}
        setCurPage={setCurPage}
        setEditMode={setEditMode}/>


}

