import {STable} from "../model/schemaModel.ts";
import {useRete} from "rete-react-plugin";
import {createEditor} from "../editor.tsx";
import {useCallback, useEffect, useState} from "react";
import {Entity, EntityGraph, fillInputs} from "../model/entityModel.ts";
import {Item} from "rete-context-menu-plugin/_types/types";
import {RecordRefsResult, RefId} from "../model/recordModel.ts";
import {App, Empty, Result, Spin} from "antd";
import {createRefEntities, getId} from "../func/recordRefEntity.ts";
import {useTranslation} from "react-i18next";
import {Schema} from "../model/schemaUtil.ts";
import {NodeShowType} from "../func/localStoreJson.ts";
import {navTo, setEditMode, store, useLocationData} from "../model/store.ts";
import {useNavigate} from "react-router-dom";
import {useQueryClient} from "@tanstack/react-query";


export function TableRecordRefLoaded({schema, curTable, recordRefResult, nodeShow}: {
    schema: Schema;
    curTable: STable;
    recordRefResult: RecordRefsResult;
    nodeShow: NodeShowType;
}) {

    const {query} = store;
    const {curId} = useLocationData();
    const [t] = useTranslation();
    const navigate = useNavigate();

    function createGraph(): EntityGraph {
        const entityMap = new Map<string, Entity>();
        const hasContainEnum = nodeShow.containEnum || curTable.entryType == 'eEnum';
        createRefEntities(entityMap, schema, recordRefResult.refs, true, hasContainEnum);
        fillInputs(entityMap);

        const menu: Item[] = [{
            label: recordRefResult.table + "\n" + t('record'),
            key: 'record',
            handler() {
                navigate(navTo('record', curTable.name, curId));
            }
        }];

        const entityMenuFunc = (entity: Entity): Item[] => {
            let refId = entity.userData as RefId;
            let id = getId(refId.table, refId.id);
            let mm = [];
            if (refId.table != recordRefResult.table || refId.id != recordRefResult.id) {
                mm.push({
                    label: id + "\n" + t('recordRef'),
                    key: 'entityRecordRef',
                    handler() {
                        navigate(navTo('recordRef', refId.table, refId.id));
                    }
                });
            }
            mm.push({
                label: id + "\n" + t('record'),
                key: 'entityRecord',
                handler() {
                    navigate(navTo('record', refId.table, refId.id));
                    setEditMode(false);
                }
            });

            let isEntityEditable = schema.isEditable && !!(schema.getSTable(refId.table)?.isEditable);
            if (isEntityEditable) {
                mm.push({
                    label: id + "\n" + t('edit'),
                    key: 'entityEdit',
                    handler() {
                        navigate(navTo('record', refId.table, refId.id));
                        setEditMode(true);
                    }
                });
            }
            return mm;
        }
        return {entityMap, menu, entityMenuFunc, query, nodeShow};
    }

    const create = useCallback(
        (el: HTMLElement) => {
            return createEditor(el, createGraph());
        },
        [recordRefResult, query, nodeShow]
    );
    const [ref] = useRete(create);


    return <div ref={ref} style={{height: "100vh", width: "100%"}}></div>
}

export function TableRecordRef({
                                   schema, curTable, curId,
                                   refIn, refOutDepth, maxNode,
                                   nodeShow
                               }: {
    schema: Schema;
    curTable: STable;
    curId: string;
    refIn: boolean;
    refOutDepth: number;
    maxNode: number;
    nodeShow: NodeShowType;
}) {
    const {server} = store;
    const [recordRefResult, setRecordRefResult] = useState<RecordRefsResult | null>(null);
    const {notification} = App.useApp();
    const queryClient = useQueryClient();

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
            queryClient.clear();
        });
    }, [schema, server, curTable, curId, refOutDepth, maxNode, refIn]);


    if (recordRefResult == null) {
        return <Empty> <Spin/> </Empty>
    }

    if (recordRefResult.resultCode != 'ok') {
        return <Result status={'error'} title={recordRefResult.resultCode}/>
    }

    return <TableRecordRefLoaded {...{schema, curTable, recordRefResult, nodeShow}}/>


}
