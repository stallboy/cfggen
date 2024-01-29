import {Entity} from "../../flow/entityModel.ts";
import {JSONObject, RecordEditResult, RecordResult, RefId} from "./recordModel.ts";
import {App, Result} from "antd";
import {createRefEntities, getId} from "./recordRefEntity.ts";
import {RecordEntityCreator} from "./recordEntityCreator.ts";
import {EditEntityCreator} from "./editEntityCreator.ts";
import {editState, startEditingObject} from "./editingObject.ts";
import {useTranslation} from "react-i18next";
import {navTo, setIsEditMode, store, useLocationData} from "../setting/store.ts";
import {useNavigate, useOutletContext} from "react-router-dom";
import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {addOrUpdateRecord, fetchRecord} from "../../io/api.ts";
import {MenuItem} from "../../flow/FlowContextMenu.tsx";
import {SchemaTableType} from "../../CfgEditorApp.tsx";
import {fillHandles} from "../../flow/entityToNodeAndEdge.ts";
import {useEffect, useState} from "react";

import {useEntityToGraph} from "../../flow/FlowGraph.tsx";


function RecordWithResult({recordResult}: { recordResult: RecordResult }) {
    const {schema, curTable} = useOutletContext<SchemaTableType>();
    const {server} = store;
    const {notification} = App.useApp();
    const {curTableId, curId} = useLocationData();
    const navigate = useNavigate();
    const queryClient = useQueryClient()
    const [t] = useTranslation();
    const {edit, pathname} = useLocationData();
    const [editSeq, setEditSeq] = useState<number>(0);

    useEffect(() => {
        setIsEditMode(edit);
    }, [edit]);

    const addOrUpdateRecordMutation = useMutation<RecordEditResult, Error, JSONObject>({
        mutationFn: (jsonObject: JSONObject) => addOrUpdateRecord(server, curTableId, jsonObject),

        onError: (error, _variables, _context) => {
            notification.error({
                message: `addOrUpdateRecord  ${curTableId}/${curId}  err: ${error.toString()}`,
                placement: 'topRight', duration: 4
            });
            queryClient.clear();
        },
        onSuccess: (editResult, _variables, _context) => {
            if (editResult.resultCode == 'updateOk' || editResult.resultCode == 'addOk') {
                console.log(editResult);

                notification.info({
                    message: `addOrUpdateRecord  ${curTableId}/${curId}  ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 3
                });
                queryClient.clear();
                // navigate(navTo(curPage, curTableId, curId));
            } else {
                notification.warning({
                    message: `addOrUpdateRecord ${curTableId}/${curId} ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 4
                });
            }
        },
    });

    const entityMap = new Map<string, Entity>();
    const refId = {table: curTable.name, id: curId};
    const entityId = getId(curTable.name, curId);
    const isEditable = schema.isEditable && curTable.isEditable;
    const isEditing = isEditable && edit;
    if (!isEditing) {
        let creator = new RecordEntityCreator(entityMap, schema, refId, recordResult.refs);
        creator.createRecordEntity(entityId, recordResult.object);
        createRefEntities(entityMap, schema, recordResult.refs, false, true);
    } else {

        function submitEditingObject() {
            addOrUpdateRecordMutation.mutate(editState.editingObject);
        }

        function afterEditStateChanged() {
            setEditSeq(editSeq + 1);
            queryClient.removeQueries({queryKey: ['layout', pathname]});
            queryClient.removeQueries({queryKey: ['viewport', pathname]});
        }

        //这是非纯函数，escape hatch，用useRef也能做，这里用全局变量
        startEditingObject(recordResult, afterEditStateChanged, submitEditingObject);
        let creator = new EditEntityCreator(entityMap, schema, curTable, curId);
        creator.createThis();
    }
    fillHandles(entityMap);

    function getEditMenu(table: string, id: string, edit: boolean) {
        if (edit) {
            return {
                label: getId(table, id) + ' ' + t('edit'),
                key: 'edit',
                handler() {
                    navigate(navTo('record', table, id, true));
                }
            };
        } else {
            return {
                label: getId(table, id) + ' ' + t('view'),
                key: 'view',
                handler() {
                    navigate(navTo('record', table, id));
                }
            };

        }
    }

    const paneMenu: MenuItem[] = [];
    if (isEditable) {
        paneMenu.push(getEditMenu(curTable.name, curId, !edit));
    }
    paneMenu.push({
        label: entityId + " " + t('recordRef'),
        key: 'recordRef',
        handler() {
            navigate(navTo('recordRef', curTable.name, curId));
        }
    })

    const nodeMenuFunc = (entity: Entity): MenuItem[] => {
        let refId = entity.userData as RefId;
        let id = getId(refId.table, refId.id);

        let mm = [];

        let isCurrentEntity = (refId.table == curTable.name && refId.id == curId);
        if (isCurrentEntity) {
            if (isEditable) {
                mm.push(getEditMenu(curTable.name, curId, !edit));
            }
        } else {
            let isEntityEditable = schema.isEditable && !!(schema.getSTable(refId.table)?.isEditable);
            if (isEntityEditable) {
                mm.push(getEditMenu(refId.table, refId.id, false));
                mm.push(getEditMenu(refId.table, refId.id, true));
            } else {
                mm.push({
                    label: id + " " + t('record'),
                    key: 'entityRecord',
                    handler() {
                        navigate(navTo('record', refId.table, refId.id));
                    }
                });
            }
        }
        mm.push({
            label: id + " " + t('recordRef'),
            key: 'entityRecordRef',
            handler() {
                navigate(navTo('recordRef', refId.table, refId.id));
            }
        })
        return mm;
    }

    useEntityToGraph(pathname, entityMap, nodeMenuFunc, paneMenu);

    return <></>
}


export function Record() {
    const {server} = store;
    const {curTableId, curId} = useLocationData();
    const {isLoading, isError, error, data: recordResult} = useQuery({
        queryKey: ['table', curTableId, curId],
        queryFn: () => fetchRecord(server, curTableId, curId),
    })

    if (isLoading) {
        return;
    }

    if (isError) {
        return <Result status={'error'} title={error.message}/>;
    }

    if (!recordResult) {
        return <Result title={'record result empty'}/>;
    }

    if (recordResult.resultCode != 'ok') {
        return <Result status={'error'} title={recordResult.resultCode}/>;
    }

    return <RecordWithResult recordResult={recordResult}/>;
}

