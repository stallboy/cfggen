import {Entity} from "../model/entityModel.ts";
import {JSONObject, RecordEditResult, RefId} from "../model/recordModel.ts";
import {App, Empty, Result, Spin} from "antd";
import {createRefEntities, getId} from "../func/recordRefEntity.ts";
import {RecordEntityCreator} from "../func/RecordEntityCreator.ts";
import {RecordEditEntityCreator} from "../func/RecordEditEntityCreator.ts";
import {editingState} from "../model/editingState.ts";
import {useTranslation} from "react-i18next";
import {navTo, setEditMode, store, useLocationData} from "../model/store.ts";
import {useNavigate, useOutletContext} from "react-router-dom";
import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {addOrUpdateRecord, fetchRecord} from "../func/api.ts";
import {useReducer} from "react";
import {FlowGraph} from "../ui/FlowGraph.tsx";
import {ReactFlowProvider} from "reactflow";
import {MenuItem} from "../ui/FlowContextMenu.tsx";
import {SchemaTableType} from "../CfgEditorApp.tsx";
import {convertNodeAndEdges, fillHandles} from "../ui/entityToFlow.ts";


export function Record() {
    const {schema, curTable} = useOutletContext<SchemaTableType>();
    const {server, editMode, nodeShow} = store;
    const {notification} = App.useApp();
    const {curPage, curTableId, curId} = useLocationData();
    const navigate = useNavigate();
    const queryClient = useQueryClient()
    const [t] = useTranslation();
    const [_forceUpdate, setForceUpdate] = useReducer(x => x + 1, 0);
    const {pathname} = useLocationData();

    const {isLoading, isError, error, data: recordResult} = useQuery({
        queryKey: ['table', curTableId, curId],
        queryFn: () => fetchRecord(server, curTableId, curId),
        staleTime: 1000 * 10,
    })

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
                navigate(navTo(curPage, curTableId, curId));
                setEditMode(false);
            } else {
                notification.warning({
                    message: `addOrUpdateRecord ${curTableId}/${curId} ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 4
                });
            }

        },
    });

    function onSubmit() {
        addOrUpdateRecordMutation.mutate(editingState.editingObject);
    }

    if (isLoading) {
        return <Empty> <Spin/> </Empty>;
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

    const entityMap = new Map<string, Entity>();
    let refId = {table: curTable.name, id: curId};
    let entityId = getId(curTable.name, curId);
    let isEditable = schema.isEditable && curTable.isEditable;
    let isEditing = isEditable && editMode;
    if (!isEditing) {
        let creator = new RecordEntityCreator(entityMap, schema, refId, recordResult.refs);
        creator.createRecordEntity(entityId, recordResult.object);
        createRefEntities(entityMap, schema, recordResult.refs, false, true);
    } else {
        editingState.startEditingObject(schema, recordResult, setForceUpdate);
        let creator = new RecordEditEntityCreator(entityMap, schema, curTable, curId, onSubmit);
        creator.createThis();
    }
    fillHandles(entityMap);

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

    const paneMenu: MenuItem[] = [];
    if (isEditable) {
        paneMenu.push(getEditMenu(curTable.name, curId, !editMode));
    }
    paneMenu.push({
        label: entityId + "\n" + t('recordRef'),
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
    const {nodes, edges} = convertNodeAndEdges({entityMap, nodeShow});

    return <ReactFlowProvider>
        <FlowGraph key={pathname}
                   initialNodes={nodes}
                   initialEdges={edges}
                   paneMenu={paneMenu}
                   nodeMenuFunc={nodeMenuFunc}
        />
    </ReactFlowProvider>


}

