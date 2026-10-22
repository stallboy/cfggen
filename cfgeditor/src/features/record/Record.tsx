import {Entity, isEditableEntity, EditingObjectRes, EFitView} from "@/domain/entityModel.ts";
import {FieldRef, JSONObject, RecordEditResult, RecordResult, RefId} from "@/api/recordModel.ts";
import {App} from "antd";
import {createEditingRefs, createRefEntities, getId, getLabel, incomingObjectReferences, locateRecordRefs} from "./recordRefUtils.ts";
import {RecordEntityCreator} from "./recordEntityCreator.ts";
import {RecordEditEntityCreator} from "./recordEditEntityCreator.ts";
import {EditingSession, getCurrentEditingSession, setCurrentEditingSession} from "@/services/editingSession.ts";
import {isCopiedFitAllowedType, structCopy} from "@/services/clipboard.ts";
import {useTranslation} from "react-i18next";
import {navTo, setIsEditMode, setEditingState, useMyStore, useLocationData} from "@/store/store.ts";
import {useLocation, useNavigate, useOutletContext} from "react-router";
import {useMutation, useQuery} from "@tanstack/react-query";
import {addOrUpdateRecord, fetchRecord} from "@/api/apiClient.ts";
import {MenuItem} from "@/flow/FlowContextMenu.tsx";
import {fillHandles} from "@/flow/layout/entityToNodeAndEdge.ts";
import {memo, useCallback, useEffect, useMemo, useRef, useState, useSyncExternalStore} from "react";
import {useHotkeys} from "react-hotkeys-hook";


import {useEntityToGraph} from "@/flow/useEntityToGraph.ts";
import {invalidateAllExceptLayout} from "@/services/queryClient.ts";
import {queryKeys, removeEditLayoutCache} from "@/services/queryKeys.ts";
import {EntityEdge, EntityNode} from "@/flow/FlowGraph.tsx";
import {NEW_RECORD_ID, SchemaTableType} from "@/domain/schema.ts";
import {QueryGate} from "@/app/QueryGate.tsx";
import {encodeEditorRecord} from '@/domain/editorValueEncoding';
import {referencePathLabel, resolveReferenceTarget} from '@/domain/referenceTarget';
import {prepareEditingObject, RecordResponseMarker, referencesNeedValidation} from '@/services/editingObjectUtils';


function RecordWithResult({recordResult, responseVersion}: { recordResult: RecordResult; responseVersion: number }) {
    const {schema, notes, curTable} = useOutletContext<SchemaTableType>();
    const {server, tauriConf, resourceDir, resMap, editingIsEdited} = useMyStore();
    const {notification} = App.useApp();
    const {curTableId, curId, edit, pathname} = useLocationData();
    const navigate = useNavigate();
    const location = useLocation();
    const {t} = useTranslation();
    const navigationReference = (location.state as {referenceTarget?: FieldRef} | null)?.referenceTarget;
    const [focusReference, setFocusReference] = useState<FieldRef | undefined>(navigationReference);
    const [savedFromResponse, setSavedFromResponse] = useState<RecordResponseMarker | undefined>(undefined);
    const referencesPendingRef = useRef(false);
    const validatedObject = useMemo(() => prepareEditingObject(recordResult.object), [recordResult.object]);
    useEffect(() => {
        if (navigationReference) setFocusReference(navigationReference);
    }, [navigationReference]);

    const addOrUpdateRecordMutation = useMutation<RecordEditResult, Error, JSONObject, RecordResponseMarker>({
        mutationFn: async (jsonObject: JSONObject) => addOrUpdateRecord(server, curTableId, encodeEditorRecord(schema, curTable, jsonObject)),
        onMutate: () => ({updatedAt: responseVersion, revision: recordResult.revision}),

        onError: (error) => {
            session.onCommitFailure();
            notification.error({
                title: `addOrUpdateRecord  ${curTableId}  err: ${error.toString()}`,
                placement: 'topRight', duration: 4
            });
        },
        onSuccess: (editResult, _variables, submittedFrom) => {
            if (editResult.resultCode == 'updateOk' || editResult.resultCode == 'addOk') {
                referencesPendingRef.current = true;
                setSavedFromResponse(submittedFrom);
                notification.info({
                    title: `addOrUpdateRecord  ${curTableId} ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 3
                });

                session.onCommitSuccess();
                invalidateAllExceptLayout();   // 排除 layout：避免用重渲前旧闭包 refetch 出陈旧布局服到展开后的新节点集（见 helper 注释）
                if (curId === NEW_RECORD_ID) {
                    // 新记录创建后跳真实 id：key 变 → 旧 session unmount（栈随实例销毁）→ 新 session 用真实 id 构造
                    navigate(navTo('record', curTableId, editResult.id, true));
                }
            } else {
                session.onCommitFailure();
                notification.warning({
                    title: `addOrUpdateRecord ${curTableId} ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 4
                });
            }
        },
    });


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
            // 结构变更时事件期同步清编辑态 layout 缓存（remove 非 invalidate、不可挪 effect，理由见 helper JSDoc）
            onStructureChange: () => removeEditLayoutCache(pathnameRef.current),
            mutate: mutateRecord,
            onEditingStateChange: (table, id, isEdited) => setEditingState(table, id, isEdited),
        });
    }
    const session = sessionRef.current;
    // 只订阅结构版本号（number）：值类编辑不 bump → 不重渲（性能契约1）；结构类编辑 bump → 重渲重算 entityMap。
    const structureVersion = useSyncExternalStore(session.subscribe, session.getStructureVersion);
    // A dirty transition must remove validated paths immediately; later keystrokes keep the existing no-render contract.
    void editingIsEdited;
    const referencesPending = isEditing && referencesNeedValidation(session.getEditingObject(), validatedObject,
        session.getIsEdited(), {updatedAt: responseVersion, revision: recordResult.revision}, savedFromResponse);
    referencesPendingRef.current = referencesPending;
    // ctrl+z/y per-session。回调实时判 canUndo/canRedo：栈空/非编辑态直接放行（不 preventDefault）→
    // input 原生 undo 不被误杀；不订阅 canUndo → 其翻转不触发重渲（性能契约1）。
    // react-hotkeys-hook 内部用 ref 存最新回调，本组件因他因重渲（edit 切换/结构编辑）时刷新 isEditing 闭包。
    useHotkeys('ctrl+z, cmd+z', (e) => {
        if (!isEditing || !session.canUndo()) return;
        e.preventDefault();
        session.undo();
    }, {enableOnFormTags: true});
    useHotkeys('ctrl+y, ctrl+shift+z, cmd+y, cmd+shift+z', (e) => {
        if (!isEditing || !session.canRedo()) return;
        e.preventDefault();
        session.redo();
    }, {enableOnFormTags: true});

    // Value edits may hide validated edges, but they must not reissue a viewport instruction.
    const editingObjectRes = useMemo<EditingObjectRes>(() => {
        void structureVersion;
        void recordResult;
        return isEditing ? session.getEditingObjectRes() : {fitView: EFitView.FitFull, isEdited: false};
    }, [isEditing, session, structureVersion, recordResult]);
    const entityMap = useMemo(() => {
        const entityMap = new Map<string, Entity>();
        const refId = {table: curTable.name, id: curId};

        if (!isEditing) {
            const creator = new RecordEntityCreator(entityMap, schema, refId, recordResult.refs, tauriConf, resourceDir, resMap);
            creator.createRecordEntity(getId(curTable.name, curId), recordResult.object, getId(getLabel(curTable.name), curId));
        } else {
            // 结构版本更新节点；dirty 翻转时重投影引用，后续键入复用同一编辑对象。
            void structureVersion;
            const editingObject = session.getEditingObject();
            const creator = new RecordEditEntityCreator(entityMap, schema, curTable, curId, session, editingObject);
            creator.createThis();
        }
        createRefEntities({entityMap, schema, briefRecordRefs: recordResult.refs, isCreateRefs: false,
            tauriConf, resourceDir, resMap});
        if (isEditing) {
            createEditingRefs(entityMap, recordResult.object, refId, recordResult.refs, referencesPending);
        }
        locateRecordRefs(entityMap, recordResult.revision, t('referencesPending'));
        fillHandles(entityMap);
        return entityMap;
    }, [isEditing, curId, schema, recordResult, tauriConf, resourceDir, resMap, curTable,
        session, structureVersion, referencesPending, t]);

    const focusRequest = useMemo(() => ({reference: focusReference, revision: recordResult.revision}), [focusReference, recordResult.revision]);
    const focusTarget = useMemo(() => {
        if (!focusReference) return undefined;
        const target = resolveReferenceTarget(focusReference, entityMap, recordResult.revision, referencesPending);
        return target && {nodeId: target.nodeId, request: focusRequest};
    }, [focusReference, entityMap, recordResult.revision, referencesPending, focusRequest]);
    const refreshedReference = useRef<FieldRef | undefined>(undefined);
    useEffect(() => {
        if (!focusReference || refreshedReference.current === focusReference) return;
        const target = resolveReferenceTarget(focusReference, entityMap, recordResult.revision, referencesPending);
        if (target?.stale && !referencesPending) {
            refreshedReference.current = focusReference;
            notification.info({title: t('referenceSnapshotChanged'), placement: 'topRight', duration: 4});
            invalidateAllExceptLayout();
        }
    }, [focusReference, entityMap, recordResult.revision, referencesPending, notification, t]);

    const openReferenceTarget = useCallback((reference: FieldRef) => {
        if (isEditing && (session.getIsEdited() || referencesPendingRef.current)) return;
        if (reference.toTable === curTable.name && reference.toId === curId) {
            setFocusReference({...reference});
        } else {
            navigate(navTo('record', reference.toTable, reference.toId, isEditing), {state: {referenceTarget: reference}});
        }
    }, [isEditing, session, curTable.name, curId, navigate]);
    const edgeDoubleClickFunc = useCallback((edge: EntityEdge) => {
        if (edge.data?.reference) openReferenceTarget(edge.data.reference);
    }, [openReferenceTarget]);
    const nodeDoubleClickFunc = useCallback((node: EntityNode) => {
        if (node.data.entity.type !== 'card') return;
        const references = incomingObjectReferences(entityMap, node.id);
        if (references.length === 1) openReferenceTarget(references[0]);
        else {
            const record = node.data.entity.userData as RefId;
            navigate(navTo('record', record.table, record.id, isEditing));
        }
    }, [entityMap, openReferenceTarget, navigate, isEditing]);

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

    // undo 基准初始化（mount）+ unmount 清理。构造函数在 render 期，structuredClone 是副作用，挪到 effect；
    // unmount 显式 dispose 清 listeners + coalesce timer，防 session 被 setTimeout 闭包持住泄漏。
    useEffect(() => {
        session.initUndoBaseline();
        return () => session.dispose();
    }, [session]);

    // 注册为当前活动编辑会话，供 Chat / AddJson（Splitter 兄弟，非本路由子树）寻址写入。
    useEffect(() => {
        setCurrentEditingSession(session);
        return () => {
            if (getCurrentEditingSession() === session) {
                setCurrentEditingSession(null);
            }
        };
    }, [session]);


    const getEditMenu = useCallback(function (table: string, id: string, edit: boolean, reference?: FieldRef) {
        const state = () => reference && !(isEditing && (session.getIsEdited() || referencesPendingRef.current)) ? {referenceTarget: reference} : null;
        const action = edit ? 'edit' : 'view';
        return {
            label: t(action) + id,
            key: action,
            handler() {
                navigate(navTo('record', table, id, edit), {state: state()});
            }
        };
    }, [t, navigate, isEditing, session]);

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
        if (isEditing) {
            menu.push({label: t('undo'), key: 'undo', handler: () => session.undo(), disabled: () => !session.canUndo()});
            menu.push({label: t('redo'), key: 'redo', handler: () => session.redo(), disabled: () => !session.canRedo()});
        }
        return menu;
    }, [isEditing, isEditable, getEditMenu, curTable, curId, edit, t, navigate, session]);


    const nodeMenuFunc = useCallback(function (entityNode: EntityNode): MenuItem[] {
        const entity = entityNode.data.entity;
        const refId = entity.userData as RefId;
        const incoming = entity.type === 'card' ? incomingObjectReferences(entityMap, entity.id) : [];
        const preferredReference = incoming.length === 1 ? incoming[0] : undefined;

        const mm = [];

        const isCurrentEntity = (refId.table == curTable.name && refId.id == curId);
        if (isCurrentEntity) {
            if (isEditable) {
                mm.push(getEditMenu(curTable.name, curId, !edit));
            }
        } else {
            const isEntityEditable = schema.isEditable;
            if (isEntityEditable) {
                mm.push(getEditMenu(refId.table, refId.id, false, preferredReference));
                mm.push(getEditMenu(refId.table, refId.id, true, preferredReference));
            } else {
                mm.push({
                    label: t('record') + refId.id,
                    key: 'entityRecord',
                    handler() {
                        navigate(navTo('record', refId.table, refId.id), {state: preferredReference ? {referenceTarget: preferredReference} : null});
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
        for (const edge of entity.sourceEdges) {
            const reference = edge.reference;
            if (!reference) continue;
            mm.push({
                label: `${t('openReferenceTarget')} · ${reference.label ?? ''} · ${reference.toTable}/${reference.toId} · ${referencePathLabel(reference.toPath ?? [])}`,
                key: `reference-${mm.length}`,
                handler: () => openReferenceTarget(reference),
            });
        }
        if (entity.type === 'card') {
            for (const reference of incoming) mm.push({
                label: `${t('openReferenceTarget')} · ${referencePathLabel(reference.toPath ?? [])}`,
                key: `reference-${mm.length}`,
                handler: () => openReferenceTarget(reference),
            });
        }
        if (isEditing && isEditableEntity(entity)) {
            const {editObj, editFieldChain, editAllowObjType} = entity.edit;
            if (editObj) {
                if (editFieldChain && editAllowObjType && entity.edit.editOnDelete) { // 有editOnDelete表明是list的成员
                    const index = editFieldChain[editFieldChain.length - 1] as number;
                    mm.push({
                        label: t('addListItemBefore'),
                        key: 'addListItemBefore',
                        handler() {
                            const sFieldable = schema.getStructOrInterface(editAllowObjType);
                            if (!sFieldable) return;
                            const defaultValue = schema.defaultValue(sFieldable);
                            // 0→1 且可内嵌时 session 写 $embed=false（原 markNewItemExpanded 语义）：
                            // 新元素默认展开成节点，立即可编辑。判定由 normalizeOnAdd 自调 canBeEmbeddedCheck
                            session.addArrayItemAtIndex(defaultValue, index,
                                editFieldChain.slice(0, editFieldChain.length - 1),
                                {id: entity.id, x: entityNode.position.x, y: entityNode.position.y},
                                sFieldable
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
    }, [curTable.name, curId, t, isEditing, isEditable, getEditMenu, edit, schema, navigate, session, entityMap, openReferenceTarget]);

    useEntityToGraph({
        type: isEditing ? 'edit' : 'record',
        pathname,
        entityMap,
        notes,
        nodeMenuFunc,
        paneMenu,
        editingObjectRes,
        edgeDoubleClickFunc,
        nodeDoubleClickFunc,
        focusTarget,
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
    const recordQuery = useQuery({
        queryKey: queryKeys.record(curTableId, curId),
        queryFn: ({signal}) => fetchRecord(server, curTableId, curId, signal),
        // curId 可能为空串（路由 * 可匹配空 id、pref 恢复的 navTo 缺 id），空 id 不发请求
        enabled: !isNewRecord && curId.length > 0,
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
        return <RecordWithResult key={`${curTableId}-${NEW_RECORD_ID}`} recordResult={mockRecordResult} responseVersion={0}/>;
    }

    return <QueryGate query={recordQuery} loading={null} emptyTitle={'record result empty'}>
        {/* key 让切 record 时 unmount 旧 RecordWithResult（session 随之销毁），编辑态/undo 栈 per-record 隔离 */}
        {(recordResult) => <RecordWithResult key={`${curTableId}-${curId}`} recordResult={recordResult} responseVersion={recordQuery.dataUpdatedAt}/>}
    </QueryGate>;
});

