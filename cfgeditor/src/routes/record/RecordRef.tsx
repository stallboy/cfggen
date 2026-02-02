import { STable } from "../../api/schemaModel.ts";
import { Entity } from "../../flow/entityModel.ts";
import { RecordRefsResult, RefId, UnreferencedRecordsResult } from "../../api/recordModel.ts";
import { Result } from "antd";
import { createRefEntities } from "./recordRefEntity.ts";
import { useTranslation } from "react-i18next";
import { Schema } from "../table/schemaUtil.tsx";
import { NodeShowType } from "../../store/storageJson.ts";
import { navTo, useMyStore, useLocationData } from "../../store/store.ts";
import { useNavigate, useOutletContext } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { fetchRecordRefs, fetchUnreferencedRecords } from "../../api/api.ts";
import { MenuItem } from "../../flow/FlowContextMenu.tsx";
import { SchemaTableType } from "../../CfgEditorApp.tsx";
import { fillHandles } from "../../flow/entityToNodeAndEdge.ts";

import { useCallback, useMemo, useState } from "react";
import { useEntityToGraph } from "../../flow/useEntityToGraph.tsx";
import { EditingObjectRes, EFitView } from "./editingObject.ts";
import { EntityNode } from "../../flow/FlowGraph.tsx";
import { useParams } from "react-router-dom";


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
            isCreateRefs: !isUnrefMode,  // 未引用模式不创建引用关系边
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

    const nodeDoubleClickFunc = (entityNode: EntityNode): void => {
        const refId = entityNode.data.entity.userData as RefId;
        navigate(navTo('record', refId.table, refId.id, isEditMode));
    };

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


    useEntityToGraph({
        type: 'ref',
        pathname, entityMap, notes, nodeMenuFunc: createNodeMenu, paneMenu: createPaneMenu(), nodeDoubleClickFunc, editingObjectRes,
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

    // 根据模式选择不同的API和数据获取
    const { isLoading, isError, error, data: recordRefResult } = useQuery({
        queryKey: isUnrefMode
            ? ['unreferenced', curTable.name, refOutDepth, maxNode]
            : ['recordRef', curTable.name, curId, refOutDepth, maxNode, refIn],
        queryFn: ({ signal }) => {
            if (isUnrefMode) {
                return fetchUnreferencedRecords(server, curTable.name, refOutDepth, maxNode, signal);
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
    const navigate = useNavigate();

    const curTable = schema ? schema.getSTable(table || '') : null;

    // 如果table不存在，导航到404
    if (!curTable) {
        navigate('/PathNotFound');
        return null;
    }

    // id可能为undefined（未引用模式）或字符串（单个record模式）
    return <RecordRef schema={schema} notes={notes} curTable={curTable} curId={id}
        refIn={recordRefIn} refOutDepth={recordRefOutDepth} maxNode={recordMaxNode}
        nodeShow={nodeShow}
        inDragPanelAndFix={false} />
}
