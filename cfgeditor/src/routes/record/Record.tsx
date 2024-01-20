import {Entity} from "../../flow/entityModel.ts";
import {JSONObject, RecordEditResult, RefId} from "./recordModel.ts";
import {App, Empty, Result, Spin} from "antd";
import {createRefEntities, getId} from "./recordRefEntity.ts";
import {RecordEntityCreator} from "./recordEntityCreator.ts";
import {EditEntityCreator} from "./editEntityCreator.ts";
import {EditingObject, startEditingObject} from "./editingObject.ts";
import {useTranslation} from "react-i18next";
import {navTo, store, useLocationData} from "../setting/store.ts";
import {useNavigate, useOutletContext} from "react-router-dom";
import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {addOrUpdateRecord, fetchRecord} from "../../io/api.ts";
import {FlowGraph} from "../../flow/FlowGraph.tsx";
import {ReactFlowProvider} from "reactflow";
import {MenuItem} from "../../flow/FlowContextMenu.tsx";
import {SchemaTableType} from "../../CfgEditorApp.tsx";
import {convertNodeAndEdges, fillHandles} from "../../flow/entityToFlow.ts";
import {useState} from "react";


export function Record() {
    const {schema, curTable} = useOutletContext<SchemaTableType>();
    const {server, nodeShow} = store;
    const {notification} = App.useApp();
    const {curPage, curTableId, curId} = useLocationData();
    const navigate = useNavigate();
    const queryClient = useQueryClient()
    const [t] = useTranslation();
    const {edit, pathname} = useLocationData();
    const [editingObject, setEditingObject] = useState<EditingObject | undefined>();

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
            } else {
                notification.warning({
                    message: `addOrUpdateRecord ${curTableId}/${curId} ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 4
                });
            }
        },
    });


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
    let isEditing = isEditable && edit;
    if (!isEditing) {
        let creator = new RecordEntityCreator(entityMap, schema, refId, recordResult.refs);
        creator.createRecordEntity(entityId, recordResult.object);
        createRefEntities(entityMap, schema, recordResult.refs, false, true);
    } else {

        const thisEditingObject = startEditingObject(recordResult, editingObject);

        function onSubmit() {
            addOrUpdateRecordMutation.mutate(thisEditingObject.editingJson);
        }

        let creator = new EditEntityCreator(entityMap, schema, curTable, curId,
            {editingObject:thisEditingObject, setEditingObject}, onSubmit);
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
    const {nodes, edges} = convertNodeAndEdges({entityMap, nodeShow});

    console.log("nodes", nodes.length);


    return <ReactFlowProvider>
        <FlowGraph
            key={pathname}
                   initialNodes={nodes}
                   initialEdges={edges}
                   paneMenu={paneMenu}
                   nodeMenuFunc={nodeMenuFunc}
        />
    </ReactFlowProvider>


}

