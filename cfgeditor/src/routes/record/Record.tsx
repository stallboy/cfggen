import {Entity} from "../../flow/entityModel.ts";
import {JSONObject, RecordEditResult, RecordResult, RefId} from "./recordModel.ts";
import {App, Result} from "antd";
import {createRefEntities, getId, getLabel} from "./recordRefEntity.ts";
import {RecordEntityCreator} from "./recordEntityCreator.ts";
import {RecordEditEntityCreator} from "./recordEditEntityCreator.ts";
import {
    editState,
    isCopiedFitAllowedType, onAddItemToArrayIndex,
    onStructCopy, onStructPaste,
    startEditingObject
} from "./editingObject.ts";
import {useTranslation} from "react-i18next";
import {
    invalidateAllQueries,
    navTo,
    setIsEditMode,
    store,
    useLocationData
} from "../setting/store.ts";
import {useNavigate, useOutletContext} from "react-router-dom";
import {useMutation, useQuery} from "@tanstack/react-query";
import {addOrUpdateRecord, fetchRecord} from "../api.ts";
import {MenuItem} from "../../flow/FlowContextMenu.tsx";
import {SchemaTableType} from "../../CfgEditorApp.tsx";
import {fillHandles} from "../../flow/entityToNodeAndEdge.ts";
import {useEffect, useReducer} from "react";


import {useEntityToGraph} from "../../flow/useEntityToGraph.tsx";
import {SInterface, SStruct} from "../table/schemaModel.ts";


function RecordWithResult({recordResult}: { recordResult: RecordResult }) {
    const {schema, notes, curTable} = useOutletContext<SchemaTableType>();
    const {server, tauriConf, resourceDir, resMap} = store;
    const {notification} = App.useApp();
    const {curTableId, curId} = useLocationData();
    const navigate = useNavigate();
    const [t] = useTranslation();
    const {edit, pathname} = useLocationData();
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const [_, forceUpdate] = useReducer(v => v++, 0);

    useEffect(() => {
        setIsEditMode(edit);
    }, [edit]);

    const addOrUpdateRecordMutation = useMutation<RecordEditResult, Error, JSONObject>({
        mutationFn: (jsonObject: JSONObject) => addOrUpdateRecord(server, curTableId, jsonObject),

        onError: (error) => {
            notification.error({
                message: `addOrUpdateRecord  ${curTableId}  err: ${error.toString()}`,
                placement: 'topRight', duration: 4
            });
        },
        onSuccess: (editResult) => {
            if (editResult.resultCode == 'updateOk' || editResult.resultCode == 'addOk') {
                notification.info({
                    message: `addOrUpdateRecord  ${curTableId} ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 3
                });

                invalidateAllQueries();
                // navigate(0);
            } else {
                notification.warning({
                    message: `addOrUpdateRecord ${curTableId} ${editResult.resultCode}`,
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
    let editSeq: number = 0;
    let fitView: boolean = true;
    if (!isEditing) {
        const creator = new RecordEntityCreator(entityMap, schema, refId, recordResult.refs, tauriConf, resourceDir, resMap);
        creator.createRecordEntity(entityId, recordResult.object, getId(getLabel(curTable.name), curId));
        createRefEntities({
            entityMap,
            schema,
            briefRecordRefs: recordResult.refs,
            isCreateRefs: false,
            tauriConf,
            resourceDir,
            resMap
        });
    } else {

        const submitEditingObject = () => {
            addOrUpdateRecordMutation.mutate(editState.editingObject);
        };

        const afterEditStateChanged = () => {
            forceUpdate();  // 触发更新
        };

        //这是非纯函数，escape hatch，用useRef也能做，这里用全局变量
        [editSeq, fitView] = startEditingObject(recordResult, afterEditStateChanged, submitEditingObject);
        const creator = new RecordEditEntityCreator(entityMap, schema, curTable, curId);
        creator.createThis();
    }
    fillHandles(entityMap);

    function getEditMenu(table: string, id: string, edit: boolean) {
        if (edit) {
            return {
                label: t('edit') + id,
                key: 'edit',
                handler() {
                    navigate(navTo('record', table, id, true));
                }
            };
        } else {
            return {
                label: t('view') + id,
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
        label: t('recordRef') + curId,
        key: 'recordRef',
        handler() {
            navigate(navTo('recordRef', curTable.name, curId));
        }
    })

    const nodeMenuFunc = (entity: Entity): MenuItem[] => {
        const refId = entity.userData as RefId;

        const mm = [];

        const isCurrentEntity = (refId.table == curTable.name && refId.id == curId);
        if (isCurrentEntity) {
            if (isEditable) {
                mm.push(getEditMenu(curTable.name, curId, !edit));
            }
        } else {
            const isEntityEditable = schema.isEditable && !!(schema.getSTable(refId.table)?.isEditable);
            if (isEntityEditable) {
                mm.push(getEditMenu(refId.table, refId.id, false));
                mm.push(getEditMenu(refId.table, refId.id, true));
            } else {
                mm.push({
                    label: t('record') + refId.id,
                    key: 'entityRecord',
                    handler() {
                        navigate(navTo('record', refId.table, refId.id));
                    }
                });
            }
        }
        mm.push({
            label: t('recordRef') + refId.id,
            key: 'entityRecordRef',
            handler() {
                navigate(navTo('recordRef', refId.table, refId.id));
            }
        })
        if (isEditing && entity.edit) {
            const {editObj, editFieldChain, editAllowObjType} = entity.edit;
            if (editObj) {
                if (editFieldChain && editAllowObjType && entity.edit.editOnDelete) { // 有editOnDelete表明是list的成员
                    const index = editFieldChain[editFieldChain.length - 1] as number;
                    mm.push({
                        label: t('addListItemBefore'),
                        key: 'addListItemBefore',
                        handler() {
                            const sFieldable = schema.itemIncludeImplMap.get(editAllowObjType) as SStruct | SInterface;
                            const defaultValue = schema.defaultValue(sFieldable);
                            onAddItemToArrayIndex(defaultValue, index, editFieldChain.slice(0, editFieldChain.length - 1))
                        }
                    });
                }
                mm.push({
                    label: t('structCopy'),
                    key: 'structCopy',
                    handler() {
                        onStructCopy(editObj)
                    }
                });
            }

            if (editFieldChain && editAllowObjType && isCopiedFitAllowedType(editAllowObjType)) {
                mm.push({
                    label: t('structPaste'),
                    key: 'structPaste',
                    handler() {
                        onStructPaste(editFieldChain)
                    }
                });
            }
        }
        return mm;
    }

    const ep = pathname + (isEditing ? ',' + editSeq : '');
    useEntityToGraph({pathname: ep, entityMap, notes, nodeMenuFunc, paneMenu, fitView});

    return <></>;
}


export function Record() {
    const {server} = store;
    const {curTableId, curId} = useLocationData();
    const {isLoading, isError, error, data: recordResult} = useQuery({
        queryKey: ['table', curTableId, curId],
        queryFn: ({signal}) => fetchRecord(server, curTableId, curId, signal),
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

