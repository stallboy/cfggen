import {Entity, isEditableEntity, EditingObjectRes, EFitView} from "@/domain/entityModel";
import {JSONObject, RecordEditResult, RecordResult, RefId} from "@/api/recordModel";
import {App, Result} from "antd";
import {createRefEntities, getId, getLabel} from "./recordRefUtils.ts";
import {RecordEntityCreator} from "./recordEntityCreator.ts";
import {RecordEditEntityCreator} from "./recordEditEntityCreator.ts";
import {Folds} from "@/domain/folds";
import {EditingSession, getCurrentEditingSession, setCurrentEditingSession} from "@/services/editingSession";
import {isCopiedFitAllowedType, structCopy} from "@/services/clipboard";
import {useTranslation} from "react-i18next";
import {invalidateAllQueries, navTo, setIsEditMode, useMyStore, useLocationData} from "@/store/store";
import {useNavigate, useOutletContext} from "react-router";
import {useMutation, useQuery} from "@tanstack/react-query";
import {addOrUpdateRecord, fetchRecord} from "@/api/api";
import {MenuItem} from "@/flow/FlowContextMenu";
import {SchemaTableType} from "@/CfgEditorApp";
import {fillHandles} from "@/flow/entityToNodeAndEdge";
import {memo, useCallback, useEffect, useMemo, useRef, useState, useSyncExternalStore} from "react";


import {useEntityToGraph} from "@/flow/useEntityToGraph";
import {SInterface, SStruct} from "@/api/schemaModel";
import {queryClient} from "@/queryClient";
import {EntityNode} from "@/flow/FlowGraph";
import {NEW_RECORD_ID} from "@/domain/schema";


function RecordWithResult({recordResult}: { recordResult: RecordResult }) {
    const {schema, notes, curTable} = useOutletContext<SchemaTableType>();
    const {server, tauriConf, resourceDir, resMap} = useMyStore();
    const {notification} = App.useApp();
    const {curTableId, curId, edit, pathname} = useLocationData();
    const navigate = useNavigate();
    const {t} = useTranslation();

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

    const isEditable = schema.isEditable;
    const isEditing = isEditable && edit;

    // useMutation 返回的 mutate 是稳定引用；构造 session 时直接捕获。
    const mutateRecord = addOrUpdateRecordMutation.mutate;

    // 编辑会话 store（每条记录编辑态一个实例，取代旧的模块级 editState 单例）。
    // lazy ref：首次渲染构造，构造函数纯（仅初始化字段，不 notify）。recordResult 变化由下方 effect 的 maybeReset 同步。
    // pathname 走 ref：edit↔view 切换会改 pathname 但 key 不变、session 不重建，闭包需读到最新 pathname。
    const pathnameRef = useRef(pathname);
    pathnameRef.current = pathname;
    const sessionRef = useRef<EditingSession | null>(null);
    if (sessionRef.current === null) {
        sessionRef.current = new EditingSession(recordResult, {
            // 结构变更时同步删 layout 缓存：用 removeQueries（非 invalidate）——remove 不主动 fetch，
            // 等重渲后 useQuery 用新 queryFn 闭包（新 nodes）重取；invalidate 会立即用重渲前的旧闭包 refetch
            // → 旧布局。不能挪 effect：effect 晚于 render，重渲那一帧 useQuery 会读到还没被删的旧缓存 →
            // 旧布局多一帧。状态管理文档 9.1c。
            onStructureChange: () => queryClient.removeQueries({queryKey: ['layout', pathnameRef.current, 'e']}),
            mutate: mutateRecord,
        });
    }
    const session = sessionRef.current;
    // 只订阅结构版本号（number）：值类编辑不 bump → 不重渲（性能契约1）；结构类编辑 bump → 重渲重算 entityMap。
    const structureVersion = useSyncExternalStore(session.subscribe, session.getStructureVersion);

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
            // editingObject 是 session 的就地变异对象（共享引用）。structureVersion 是 entityMap 重算的显式触发器：
            // in-place 变异下 editingObject 顶层引用不变，无法靠引用变化驱动重算；值类编辑不 bump structureVersion
            // （性能契约1：几十表单输入零重渲），结构类编辑 bump → 此处重算 → 闭包拿到最新子对象引用（契约2）。
            void structureVersion;
            const editingObject = session.getEditingObject();
            const creator = new RecordEditEntityCreator(entityMap, schema, curTable, curId, folds, setFolds, session, editingObject);
            creator.createThis();
            editingObjectRes = session.getEditingObjectRes();
        }
        fillHandles(entityMap);
        return {entityMap, editingObjectRes}
    }, [isEditing, curId, schema, recordResult, tauriConf, resourceDir, resMap, curTable,
        folds, setFolds, session, structureVersion]);

    useEffect(() => {
        setIsEditMode(edit);
    }, [edit]);

    // recordResult 变化（切记录 / 后台推新数据）→ maybeReset（幂等：同表同id且内容相等则早退，保留编辑态）。
    // effect 期可合法 notifyEditingState（render 期不行），故原 render 后补通知的补偿 effect 合并到此。
    useEffect(() => {
        if (isEditing) {
            session.maybeReset(recordResult);
            session.notifyEditingState();
        }
    }, [isEditing, recordResult, session]);

    // 注册为当前活动编辑会话，供 Chat / AddJson（Splitter 兄弟，非本路由子树）寻址写入。
    useEffect(() => {
        setCurrentEditingSession(session);
        return () => {
            if (getCurrentEditingSession() === session) {
                setCurrentEditingSession(null);
            }
        };
    }, [session]);


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
                            session.addArrayItemAtIndex(defaultValue, index,
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
                        structCopy(editObj)
                    }
                });
            }

            if (editFieldChain && editAllowObjType && isCopiedFitAllowedType(editAllowObjType)) {
                mm.push({
                    label: t('structPaste'),
                    key: 'structPaste',
                    handler() {
                        session.pasteStruct(editFieldChain, {
                            id: entity.id,
                            x: entityNode.position.x,
                            y: entityNode.position.y
                        })
                    }
                });
            }
        }
        return mm;
    }, [curTable.name, curId, t, isEditing, isEditable, getEditMenu, edit, schema, navigate, session]);

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
}


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

