import {STable} from "../model/schemaModel.ts";
import {useRete} from "rete-react-plugin";
import {createEditor} from "../editor.tsx";
import {useCallback, useEffect, useReducer, useState} from "react";
import {Entity, EntityGraph, fillInputs} from "../model/entityModel.ts";
import {Item} from "rete-context-menu-plugin/_types/types";
import {RecordEditResult, RecordResult, RefId} from "../model/recordModel.ts";
import {App, Empty, Result, Spin} from "antd";
import {createRefEntities, getId} from "../func/recordRefEntity.ts";
import {RecordEntityCreator} from "../func/RecordEntityCreator.ts";
import {RecordEditEntityCreator} from "../func/RecordEditEntityCreator.ts";
import {editingState} from "../func/editingState.ts";
import {useTranslation} from "react-i18next";
import {newSchema, Schema} from "../model/schemaUtil.ts";
import {navTo, setEditMode, setSchema, store, useLocationData} from "../model/store.ts";
import {useNavigate} from "react-router-dom";
import {useQueryClient} from "@tanstack/react-query";


export function TableRecordLoaded({schema, curTable, recordResult, onSubmit}: {
    schema: Schema;
    curTable: STable;
    recordResult: RecordResult;
    onSubmit: () => void;
}) {
    const {editMode, nodeShow} = store;
    const [forceUpdate, setForceUpdate] = useReducer(x => x + 1, 0);
    const [t] = useTranslation();
    const navigate = useNavigate();
    const {curId} = useLocationData();


    useEffect(() => {
        editingState.clear();
    }, [schema, curTable, curId, editMode]);

    function createGraph(): EntityGraph {
        const entityMap = new Map<string, Entity>();
        let refId = {table: curTable.name, id: curId};
        let entityId = getId(curTable.name, curId);
        let isEditable = schema.isEditable && curTable.isEditable;
        let isEditing = isEditable && editMode;
        if (!isEditing) {
            let creator = new RecordEntityCreator(entityMap, schema, refId, recordResult.refs);
            creator.createEntity(entityId, recordResult.object);
            createRefEntities(entityMap, schema, recordResult.refs, false, true);
        } else {
            editingState.startEditingObject(schema, recordResult, setForceUpdate);
            let creator = new RecordEditEntityCreator(entityMap, schema, curTable, curId, onSubmit);
            creator.createThis();
        }
        fillInputs(entityMap);

        function getEditMenu(table: string, id: string, edit: boolean) {
            if (edit) {
                return {
                    label: getId(table, id) + '\n' + t('edit'),
                    key: 'edit',
                    handler() {
                        if (table != curTable.name || id != curId) {
                            navigate(navTo('record', table, id));
                        }
                        setEditMode(true);
                    }
                };
            } else {
                return {
                    label: getId(table, id) + '\n' + t('view'),
                    key: 'view',
                    handler() {
                        if (table != curTable.name || id != curId) {
                            navigate(navTo('record', table, id));
                        }
                        setEditMode(false);
                    }
                };

            }
        }

        const menu: Item[] = [];
        if (isEditable) {
            menu.push(getEditMenu(curTable.name, curId, !editMode));
        }
        menu.push({
            label: entityId + "\n" + t('recordRef'),
            key: 'recordRef',
            handler() {
                navigate(navTo('recordRef', curTable.name, curId));
            }
        })

        const entityMenuFunc = (entity: Entity): Item[] => {
            let refId = entity.userData as RefId;
            let id = getId(refId.table, refId.id);

            let mm = [];

            let isCurrentEntity = (refId.table == curTable.name && refId.id == curId);
            if (isCurrentEntity) {
                if (isEditable) {
                    mm.push(getEditMenu(curTable.name, curId, !editMode));
                }
            } else {
                let isEntityEditable = schema.isEditable && !!(schema.getSTable(refId.table)?.isEditable);
                if (isEntityEditable) {
                    mm.push(getEditMenu(refId.table, refId.id, false));
                    mm.push(getEditMenu(refId.table, refId.id, true));
                } else {
                    mm.push({
                        label: id + "\n" + t('record'),
                        key: 'entityRecord',
                        handler() {
                            navigate(navTo('record', refId.table, refId.id));
                        }
                    });
                }
            }
            mm.push({
                label: id + "\n" + t('recordRef'),
                key: 'entityRecordRef',
                handler() {
                    navigate(navTo('recordRef', refId.table, refId.id));
                }
            })
            return mm;
        }
        return {entityMap, menu, entityMenuFunc, nodeShow}
    }

    const create = useCallback(
        (el: HTMLElement) => {
            return createEditor(el, createGraph());
        },
        [recordResult, editMode, forceUpdate, nodeShow]
    );
    const [ref] = useRete(create);

    return <div ref={ref} style={{height: "100vh", width: "100%"}}></div>
}

export function TableRecord({schema, curTable}: {
    schema: Schema;
    curTable: STable;
}) {
    const {server} = store;
    const [recordResult, setRecordResult] = useState<RecordResult | null>(null);
    const {notification} = App.useApp();
    const {curPage, curTableId, curId} = useLocationData();
    const navigate = useNavigate();
    const queryClient = useQueryClient()

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
            notification.error({
                message: `fetch ${url} err: ${err.toString()}`,
                placement: 'topRight',
                duration: 4
            });
            queryClient.clear();
        });
    }, [schema, server, curTable, curId]);


    function onSubmit() {
        // console.log(editingState.editingObject);
        let editingObject = editingState.editingObject;

        let url = `http://${server}/recordAddOrUpdate?table=${curTable.name}`;
        const postData = async () => {
            const response = await fetch(url, {
                method: 'POST',
                cache: "no-cache",
                mode: "cors",
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/json",
                },
                redirect: "follow",
                referrerPolicy: "no-referrer",
                body: JSON.stringify(editingObject)
            });
            const editResult: RecordEditResult = await response.json();
            if (editResult.resultCode == 'updateOk' || editResult.resultCode == 'addOk') {
                console.log(editResult);

                notification.info({
                    message: `post ${url} ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 3
                });
                let schemaNew = newSchema(schema, editResult.table, editResult.recordIds);
                const [tableId, id] = setSchema(schemaNew, curTableId, curId);
                navigate(navTo(curPage, tableId, id));
            } else {
                notification.warning({
                    message: `post ${url} ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 4
                });
            }
        }

        postData().catch((err) => {
            notification.error({
                message: `post ${url} err: ${err.toString()}`,
                placement: 'topRight', duration: 4
            });
            queryClient.clear();
        });
    }

    if (recordResult == null) {
        return <Empty> <Spin/> </Empty>
    }

    if (recordResult.resultCode != 'ok') {
        return <Result status={'error'} title={recordResult.resultCode}/>
    }

    return <TableRecordLoaded key={`${curTable.name}-${curId}`}
                              {...{
                                  schema, curTable,
                                  recordResult,
                                  onSubmit
                              }}
    />

}

