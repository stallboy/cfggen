import { STable } from "@/api/schemaModel";
import { Entity, EditingObjectRes, EFitView } from "@/domain/entityModel";
import { RecordRefsResult, RefId, UnreferencedRecordsResult } from "@/api/recordModel";
import { Result } from "antd";
import { createRefEntities } from "./recordRefUtils.ts";
import { useTranslation } from "react-i18next";
import { Schema } from "@/domain/schema";
import { NodeShowType } from "@/domain/storageJson";
import { navTo, useMyStore } from "@/store/store";
import { Navigate, useNavigate, useOutletContext } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { fetchRecordRefs, fetchUnreferencedRecords } from "@/api/api";
import { MenuItem } from "@/flow/FlowContextMenu";
import { SchemaTableType } from "@/CfgEditorApp";
import { fillHandles } from "@/flow/entityToNodeAndEdge";

import { useCallback, useMemo, useState } from "react";
import { useEntityToGraph } from "@/flow/useEntityToGraph";
import { EntityNode } from "@/flow/FlowGraph";
import { useParams } from "react-router";


export function RecordRefWithResult({ schema, notes, curTable, curId, nodeShow, recordRefResult, inDragPanelAndFix, isUnrefMode }: {
    schema: Schema;
    notes: Map<string, string> | undefined;
    curTable: STable;
    curId?: string;
    nodeShow: NodeShowType;
    recordRefResult: RecordRefsResult | UnreferencedRecordsResult;
    inDragPanelAndFix: boolean;
    isUnrefMode: boolean;
}) {
    const [t] = useTranslation();
    const navigate = useNavigate();
    const { recordRefInShowLinkMaxNode, tauriConf, resourceDir, resMap, isEditMode } = useMyStore();

    // Memoize checkTable function
    const checkTable = useMemo(() => {
        if (!nodeShow.refContainEnum && !nodeShow.refTableHides.length) return undefined;

        return (tableName: string) => {
            if (!nodeShow.refContainEnum) {
                const sT = schema.getSTable(tableName);
                if (!sT || sT.entryType === 'eEnum') return false;
            }
            return !nodeShow.refTableHides.some(t => tableName.includes(t));
        };
    }, [nodeShow.refContainEnum, nodeShow.refTableHides, schema]);

    // Memoize entityMap creation
    const entityMap = useMemo(() => {
        const map = new Map<string, Entity>();
        createRefEntities({
            entityMap: map,
            schema,
            briefRecordRefs: recordRefResult.refs,
            isCreateRefs: !isUnrefMode,  // 未引用模式不创建引用关系
            checkTable,
            recordRefInShowLinkMaxNode,
            tauriConf,
            resourceDir,
            resMap
        });
        fillHandles(map);
        return map;
    }, [schema, recordRefResult.refs, checkTable, recordRefInShowLinkMaxNode, tauriConf, resourceDir, resMap, isUnrefMode]);

    // Extract menu creation functions
    const createPaneMenu = useCallback((): MenuItem[] => {
        const menuItem: MenuItem = {
            label: isUnrefMode
                ? `${t('unreferencedRecords')}: ${curTable.name}`
                : t('record') + curId,
            key: 'pane',
            handler: isUnrefMode || !curId
                ? () => {
                    // 未引用模式或没有id时不做任何操作
                }
                : () => navigate(navTo('record', curTable.name, curId))
        };

        return [menuItem];
    }, [t, curId, curTable.name, navigate, isUnrefMode]);

    const createNodeMenu = useCallback((entityNode: EntityNode): MenuItem[] => {
        const refId = entityNode.data.entity.userData as RefId;
        const isEntityEditable = schema.isEditable;

        const menuItems: MenuItem[] = [{
            label: t('record') + refId.id,
            key: 'entityRecord',
            handler: () => navigate(navTo('record', refId.table, refId.id))
        }];

        if (isEntityEditable) {
            menuItems.push({
                label: t('edit') + refId.id,
                key: 'entityEdit',
                handler: () => navigate(navTo('record', refId.table, refId.id, true))
            });
        }

        // 对于未引用模式，或者在单个record模式下的其他record，显示recordRef选项
        if (isUnrefMode || (refId.table !== recordRefResult.table || refId.id !== (recordRefResult as RecordRefsResult).id)) {
            menuItems.push({
                label: t('recordRef') + refId.id,
                key: 'entityRecordRef',
                handler: () => navigate(navTo('recordRef', refId.table, refId.id))
            });
        }

        return menuItems;
    }, [t, schema, navigate, recordRefResult, isUnrefMode]);

    const nodeDoubleClickFunc = useCallback((entityNode: EntityNode): void => {
        const refId = entityNode.data.entity.userData as RefId;
        navigate(navTo('record', refId.table, refId.id, isEditMode));
    }, [navigate, isEditMode]);

    const [lastFitViewPath, setLastFitViewPath] = useState<string | undefined>(undefined);
    const pathname = isUnrefMode
        ? `/recordUnref/${curTable.name}`
        : `/recordRef/${curTable.name}/${curId}`;
    let editingObjectRes;
    if (inDragPanelAndFix) {
        const pathnameWithFix = pathname + '/fix';
        if (lastFitViewPath && lastFitViewPath === pathnameWithFix) {
            editingObjectRes = fitNone;
        }
    }

    const setFitViewForPathname = useCallback((pathname: string) => {
        setLastFitViewPath(pathname);
    }, []);


    const paneMenu = useMemo(() => createPaneMenu(), [createPaneMenu]);

    useEntityToGraph({
        type: 'ref',
        pathname, entityMap, notes, nodeMenuFunc: createNodeMenu, paneMenu, nodeDoubleClickFunc, editingObjectRes,
        setFitViewForPathname: (inDragPanelAndFix ? setFitViewForPathname : undefined),
        nodeShow,
    });

    return null;
}

const fitNone: EditingObjectRes = { fitView: EFitView.FitNone, isEdited: false }

export function RecordRef({ schema, notes, curTable, curId, refIn, refOutDepth, maxNode, nodeShow, inDragPanelAndFix }: {
    schema: Schema;
    notes: Map<string, string> | undefined;
    curTable: STable;
    curId?: string;  // 改为可选，支持未引用模式
    refIn: boolean;
    refOutDepth: number;
    maxNode: number;
    nodeShow: NodeShowType;
    inDragPanelAndFix: boolean;
}) {
    const { server } = useMyStore();

    // 判断当前是哪种模式
    const isUnrefMode = curId === undefined || curId === '';

    // 根据模式选择不同的API
    const { isLoading, isError, error, data: recordRefResult } = useQuery({
        queryKey: isUnrefMode
            ? ['unreferenced', curTable.name, maxNode]
            : ['recordRef', curTable.name, curId, refOutDepth, maxNode, refIn],
        queryFn: ({ signal }) => {
            if (isUnrefMode) {
                return fetchUnreferencedRecords(server, curTable.name, maxNode, signal);
            } else {
                return fetchRecordRefs(server, curTable.name, curId!, refOutDepth, maxNode, refIn, signal);
            }
        },
        staleTime: 1000 * 10,
        enabled: !!curId || isUnrefMode,
    })


    if (isLoading) {
        return null;
    }

    if (isError) {
        return <Result status={'error'} title={error.message} />;
    }

    if (!recordRefResult) {
        return <Result title={'recordRef result empty'} />;
    }

    if (recordRefResult.resultCode != 'ok') {
        return <Result status={'error'} title={recordRefResult.resultCode} />;
    }

    return <RecordRefWithResult schema={schema} notes={notes} curTable={curTable} curId={curId}
        nodeShow={nodeShow} recordRefResult={recordRefResult}
        inDragPanelAndFix={inDragPanelAndFix}
        isUnrefMode={isUnrefMode} />

}

export function RecordRefRoute() {
    const { schema, notes } = useOutletContext<SchemaTableType>();
    const { table, id } = useParams<{ table: string; id?: string }>();
    const { recordRefIn, recordRefOutDepth, recordMaxNode, nodeShow } = useMyStore();

    const curTable = schema ? schema.getSTable(table || '') : null;

    // table 不存在时声明式跳转（render 期 navigate 是副作用，StrictMode 下会触发两次）
    if (!curTable) {
        return <Navigate to="/PathNotFound" replace />;
    }

    // id可能为undefined（未引用模式）或字符串（单个record模式）
    return <RecordRef schema={schema} notes={notes} curTable={curTable} curId={id}
        refIn={recordRefIn} refOutDepth={recordRefOutDepth} maxNode={recordMaxNode}
        nodeShow={nodeShow}
        inDragPanelAndFix={false} />
}
