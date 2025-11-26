import { STable } from "../table/schemaModel.ts";
import { Entity } from "../../flow/entityModel.ts";
import { RecordRefsResult, RefId } from "./recordModel.ts";
import { Result } from "antd";
import { createRefEntities } from "./recordRefEntity.ts";
import { useTranslation } from "react-i18next";
import { Schema } from "../table/schemaUtil.tsx";
import { NodeShowType } from "../../store/storageJson.ts";
import { navTo, useMyStore, useLocationData } from "../../store/store.ts";
import { useNavigate, useOutletContext } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { fetchRecordRefs } from "../api.ts";
import { MenuItem } from "../../flow/FlowContextMenu.tsx";
import { SchemaTableType } from "../../CfgEditorApp.tsx";
import { fillHandles } from "../../flow/entityToNodeAndEdge.ts";

import { useCallback, useMemo, useState } from "react";
import { useEntityToGraph } from "../../flow/useEntityToGraph.tsx";
import { EditingObjectRes, EFitView } from "./editingObject.ts";
import { EntityNode } from "../../flow/FlowGraph.tsx";


export function RecordRefWithResult({ schema, notes, curTable, curId, nodeShow, recordRefResult, inDragPanelAndFix }: {
    schema: Schema;
    notes: Map<string, string> | undefined;
    curTable: STable;
    curId: string;
    nodeShow: NodeShowType;
    recordRefResult: RecordRefsResult;
    inDragPanelAndFix: boolean;
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
            isCreateRefs: true,
            checkTable,
            recordRefInShowLinkMaxNode,
            tauriConf,
            resourceDir,
            resMap
        });
        fillHandles(map);
        return map;
    }, [schema, recordRefResult.refs, checkTable, recordRefInShowLinkMaxNode, tauriConf, resourceDir, resMap]);

    // Extract menu creation functions
    const createPaneMenu = useCallback((): MenuItem[] => [{
        label: t('record') + curId,
        key: 'record',
        handler: () => navigate(navTo('record', curTable.name, curId))
    }], [t, curId, curTable.name, navigate]);

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

        if (refId.table !== recordRefResult.table || refId.id !== recordRefResult.id) {
            menuItems.push({
                label: t('recordRef') + refId.id,
                key: 'entityRecordRef',
                handler: () => navigate(navTo('recordRef', refId.table, refId.id))
            });
        }

        return menuItems;
    }, [t, schema, navigate, recordRefResult]);

    const nodeDoubleClickFunc = (entityNode: EntityNode): void => {
        const refId = entityNode.data.entity.userData as RefId;
        navigate(navTo('record', refId.table, refId.id, isEditMode));
    };

    const [lastFitViewPath, setLastFitViewPath] = useState<string | undefined>(undefined);
    let pathname = `/recordRef/${curTable.name}/${curId}`;
    let editingObjectRes; // EFitView.FitFull;
    if (inDragPanelAndFix) {
        pathname += '/fix';
        if (lastFitViewPath && lastFitViewPath === pathname) {
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
    curId: string;
    refIn: boolean;
    refOutDepth: number;
    maxNode: number;
    nodeShow: NodeShowType;
    inDragPanelAndFix: boolean;
}) {
    const { server } = useMyStore();
    const { isLoading, isError, error, data: recordRefResult } = useQuery({
        queryKey: ['tableRef', curTable.name, curId, refOutDepth, maxNode, refIn],
        queryFn: ({ signal }) => fetchRecordRefs(server, curTable.name, curId, refOutDepth, maxNode, refIn, signal),
        staleTime: 1000 * 10,
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
        inDragPanelAndFix={inDragPanelAndFix} />

}

export function RecordRefRoute() {
    const { schema, notes, curTable } = useOutletContext<SchemaTableType>();
    const { curId } = useLocationData();
    const { recordRefIn, recordRefOutDepth, recordMaxNode, nodeShow } = useMyStore();

    return <RecordRef schema={schema} notes={notes} curTable={curTable} curId={curId}
        refIn={recordRefIn} refOutDepth={recordRefOutDepth} maxNode={recordMaxNode}
        nodeShow={nodeShow}
        inDragPanelAndFix={false} />
}
