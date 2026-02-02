import {Entity, isEditableEntity} from "../../flow/entityModel.ts";
import {JSONObject, RecordEditResult, RecordResult, RefId} from "../../api/recordModel.ts";
import {App, Result} from "antd";
import {createRefEntities, getId, getLabel} from "./recordRefEntity.ts";
import {RecordEntityCreator} from "./recordEntityCreator.ts";
import {Folds, RecordEditEntityCreator} from "./recordEditEntityCreator.ts";
import {
    EditingObjectRes,
    editState,
    EFitView,
    isCopiedFitAllowedType, notifyEditingState,
    onAddItemToArrayIndex,
    onStructCopy,
    onStructPaste,
    startEditingObject,
} from "./editingObject.ts";
import {useTranslation} from "react-i18next";
import {invalidateAllQueries, navTo, setIsEditMode, useMyStore, useLocationData} from "../../store/store.ts";
import {useNavigate, useOutletContext} from "react-router-dom";
import {useMutation, useQuery} from "@tanstack/react-query";
import {addOrUpdateRecord, fetchRecord} from "../../api/api.ts";
import {MenuItem} from "../../flow/FlowContextMenu.tsx";
import {SchemaTableType} from "../../CfgEditorApp.tsx";
import {fillHandles} from "../../flow/entityToNodeAndEdge.ts";
import {memo, useCallback, useEffect, useMemo, useState} from "react";


import {useEntityToGraph} from "../../flow/useEntityToGraph.tsx";
import {SInterface, SStruct} from "../../api/schemaModel.ts";
import {queryClient} from "../../main.tsx";
import {EntityNode} from "../../flow/FlowGraph.tsx";
import {NEW_RECORD_ID} from "../table/schemaUtil.tsx";


const RecordWithResult = memo(function RecordWithResult({recordResult}: { recordResult: RecordResult }) {
    const {schema, notes, curTable} = useOutletContext<SchemaTableType>();
    const {server, tauriConf, resourceDir, resMap} = useMyStore();
    const {notification} = App.useApp();
    const {curTableId, curId, edit, pathname} = useLocationData();
    const navigate = useNavigate();
    const {t} = useTranslation();
    const [updateVersion, setUpdateVersion] = useState(0);

    const addOrUpdateRecordMutation = useMutation<RecordEditResult, Error, JSONObject>({
        mutationFn: (jsonObject: JSONObject) => addOrUpdateRecord(server, curTableId, jsonObject),

        onError: (error) => {
            notification.error({
                title: `addOrUpdateRecord  ${curTableId}  err: ${error.toString()}`,
                placement: 'topRight', duration: 4
            });
        },
        onSuccess: (editResult) => {
            if (editResult.resultCode == 'updateOk' || editResult.resultCode == 'addOk') {
                notification.info({
                    title: `addOrUpdateRecord  ${curTableId} ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 3
                });

                invalidateAllQueries();
                // navigate(0);
            } else {
                notification.warning({
                    title: `addOrUpdateRecord ${curTableId} ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 4
                });
            }
        },
    });


    // folds 信息跟notes信息一样都是临时存下来，而不是直接通知server存成json。
    const [folds, setFolds] = useState<Folds>(new Folds([]));
    const update = useCallback(() => {
        // 让其重新layout，因为可能已经经过编辑，缺少了某些节点的位置信息
        queryClient.removeQueries({queryKey: ['layout', pathname, 'e']});
        setUpdateVersion(updateVersion + 1);
    }, [pathname, updateVersion, setUpdateVersion])

    const isEditable = schema.isEditable;
    const isEditing = isEditable && edit;

    const {entityMap, editingObjectRes} = useMemo(() => {
        const entityMap = new Map<string, Entity>();
        let editingObjectRes: EditingObjectRes;

        if (!isEditing) {
            const refId = {table: curTable.name, id: curId};
            const creator = new RecordEntityCreator(entityMap, schema, refId, recordResult.refs, tauriConf, resourceDir, resMap);
            creator.createRecordEntity(getId(curTable.name, curId), recordResult.object, getId(getLabel(curTable.name), curId));
            createRefEntities({
                entityMap,
                schema,
                briefRecordRefs: recordResult.refs,
                isCreateRefs: false,
                tauriConf,
                resourceDir,
                resMap
            });
            editingObjectRes = {
                fitView: EFitView.FitFull,
                isEdited: false
            }

        } else {
            const submitEditingObject = () => {
                addOrUpdateRecordMutation.mutate(editState.editingObject);
            };

            // 这是非纯函数，escape hatch，用useRef也能做，这里用全局变量
            editingObjectRes = startEditingObject(recordResult, update, submitEditingObject);
            const creator = new RecordEditEntityCreator(entityMap, schema, curTable, curId, folds, setFolds);
            creator.createThis();
        }
        fillHandles(entityMap);
        return {entityMap, editingObjectRes}
    }, [isEditing, curId, schema, recordResult, tauriConf, resourceDir, resMap, curTable,
        addOrUpdateRecordMutation, update, folds, setFolds]);

    useEffect(() => {
        setIsEditMode(edit);
    }, [edit]);

    useEffect(() => {
        if (isEditing) {
            notifyEditingState()
        }
    }, [isEditing]); // editState.table, editState.id, editState.editingObject


    const getEditMenu = useCallback(function (table: string, id: string, edit: boolean) {
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
    }, [t, navigate]);

    const paneMenu = useMemo(() => {
        const menu: MenuItem[] = [];
        if (isEditable) {
            menu.push(getEditMenu(curTable.name, curId, !edit));
        }
        menu.push({
            label: t('recordRef') + curId,
            key: 'recordRef',
            handler() {
                navigate(navTo('recordRef', curTable.name, curId));
            }
        });
        return menu;
    }, [isEditable, getEditMenu, curTable, curId, edit, t, navigate]);


    const nodeMenuFunc = useCallback(function (entityNode: EntityNode): MenuItem[] {
        const entity = entityNode.data.entity;
        const refId = entity.userData as RefId;

        const mm = [];

        const isCurrentEntity = (refId.table == curTable.name && refId.id == curId);
        if (isCurrentEntity) {
            if (isEditable) {
                mm.push(getEditMenu(curTable.name, curId, !edit));
            }
        } else {
            const isEntityEditable = schema.isEditable;
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
        if (isEditing && isEditableEntity(entity)) {
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
                            onAddItemToArrayIndex(defaultValue, index,
                                editFieldChain.slice(0, editFieldChain.length - 1),
                                {id: entity.id, x: entityNode.position.x, y: entityNode.position.y}
                            )
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
                        onStructPaste(editFieldChain, {
                            id: entity.id,
                            x: entityNode.position.x,
                            y: entityNode.position.y
                        })
                    }
                });
            }
        }
        return mm;
    }, [curTable.name, curId, t, isEditing, isEditable, getEditMenu, edit, schema, navigate]);

    // const ep = pathname + (isEditing ? ',' + editSeq : ''); // 用 editSeq触发layout
    useEntityToGraph({
        type: isEditing ? 'edit' : 'record',
        pathname,
        entityMap,
        notes,
        nodeMenuFunc,
        paneMenu,
        editingObjectRes
    });

    return null;
});


export const Record = memo(function () {
    const {server} = useMyStore();
    const {curTableId, curId} = useLocationData();
    const {schema, curTable} = useOutletContext<SchemaTableType>();

    // 只要recordIds大于0就没有 + new 的选项
    const isNewRecord = curTable.recordIds.length == 0;
    // 对于现有记录，使用API获取数据
    const {isLoading, isError, error, data: recordResult} = useQuery({
        queryKey: ['table', curTableId, curId],
        queryFn: ({signal}) => fetchRecord(server, curTableId, curId, signal),
        enabled: !isNewRecord,
    })

    // 如果是新记录，使用默认数据
    if (isNewRecord) {
        const defaultData = schema.defaultValueOfStructural(curTable);
        const mockRecordResult: RecordResult = {
            resultCode: 'ok',
            table: curTableId,
            id: NEW_RECORD_ID,
            maxObjs: 0,
            object: defaultData,
            refs: []
        };
        return <RecordWithResult key={`${curTableId}-${NEW_RECORD_ID}`} recordResult={mockRecordResult}/>;
    }

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

    // 需要key，让不同key的RecordWithResult里folds不会互相影响
    return <RecordWithResult key={`${curTableId}-${curId}`} recordResult={recordResult}/>;
});

