import {STable} from "../table/schemaModel.ts";
import {Entity} from "../../flow/entityModel.ts";
import {RecordRefsResult, RefId} from "./recordModel.ts";
import {Result} from "antd";
import {createRefEntities} from "./recordRefEntity.ts";
import {useTranslation} from "react-i18next";
import {Schema} from "../table/schemaUtil.tsx";
import {NodeShowType} from "../setting/storageJson.ts";
import {navTo, useMyStore, useLocationData} from "../setting/store.ts";
import {useNavigate, useOutletContext} from "react-router-dom";
import {useQuery} from "@tanstack/react-query";
import {fetchRecordRefs} from "../api.ts";
import {MenuItem} from "../../flow/FlowContextMenu.tsx";
import {SchemaTableType} from "../../CfgEditorApp.tsx";
import {fillHandles} from "../../flow/entityToNodeAndEdge.ts";

import {useCallback, useRef} from "react";
import {useEntityToGraph} from "../../flow/useEntityToGraph.tsx";
import {EditingObjectRes, EFitView} from "./editingObject.ts";
import {EntityNode} from "../../flow/FlowGraph.tsx";


export function RecordRefWithResult({schema, notes, curTable, curId, nodeShow, recordRefResult, inDragPanelAndFix}: {
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
    const {recordRefInShowLinkMaxNode, tauriConf, resourceDir, resMap} = useMyStore();

    const entityMap = new Map<string, Entity>();
    const hasContainEnum = nodeShow.refContainEnum || curTable.entryType == 'eEnum';

    let checkTable;
    if (!hasContainEnum || nodeShow.refTableHides.length > 0) {
        checkTable = (tableName: string) => {
            if (!hasContainEnum) {
                const sT = schema.getSTable(tableName);
                if (sT == null) {
                    return false;
                }
                if (sT.entryType == 'eEnum') {
                    return false;
                }
            }

            for (const t of nodeShow.refTableHides) {
                if (tableName.includes(t)) {
                    return false;
                }
            }
            return true;
        }
    }

    createRefEntities({
        entityMap, schema, briefRecordRefs: recordRefResult.refs, isCreateRefs: true,
        checkTable, recordRefInShowLinkMaxNode, tauriConf, resourceDir, resMap
    });
    fillHandles(entityMap);

    const paneMenu: MenuItem[] = [{
        label: t('record') + curId,
        key: 'record',
        handler() {
            navigate(navTo('record', curTable.name, curId));
        }
    }];

    const nodeDoubleClickFunc = (entityNode: EntityNode): void => {
        const {isEditMode} = useMyStore();
        const refId = entityNode.data.entity.userData as RefId;
        navigate(navTo('record', refId.table, refId.id, isEditMode));
    };

    const nodeMenuFunc = (entityNode: EntityNode): MenuItem[] => {
        const refId = entityNode.data.entity.userData as RefId;
        const mm = [];
        mm.push({
            label: t('record') + refId.id,
            key: 'entityRecord',
            handler() {
                navigate(navTo('record', refId.table, refId.id));
            }
        });

        const isEntityEditable = schema.isEditable && !!(schema.getSTable(refId.table)?.isEditable);
        if (isEntityEditable) {
            mm.push({
                label: t('edit') + refId.id,
                key: 'entityEdit',
                handler() {
                    navigate(navTo('record', refId.table, refId.id, true));
                }
            });
        }
        if (refId.table != recordRefResult.table || refId.id != recordRefResult.id) {
            mm.push({
                label: t('recordRef') + refId.id,
                key: 'entityRecordRef',
                handler() {
                    navigate(navTo('recordRef', refId.table, refId.id));
                }
            });
        }
        return mm;
    };

    const lastFitViewForFix = useRef<string | undefined>();
    let pathname = `/recordRef/${curTable.name}/${curId}`;
    let editingObjectRes; // EFitView.FitFull;
    if (inDragPanelAndFix) {
        pathname += '/fix';
        if (lastFitViewForFix.current && lastFitViewForFix.current == pathname) {
            editingObjectRes = fitNone;
        }
    }

    const setFitViewForPathname = useCallback((pathname: string) => {
        lastFitViewForFix.current = pathname;
    }, [lastFitViewForFix]);


    useEntityToGraph({
        type: 'ref',
        pathname, entityMap, notes, nodeMenuFunc, paneMenu, nodeDoubleClickFunc, editingObjectRes,
        setFitViewForPathname: (inDragPanelAndFix ? setFitViewForPathname : undefined),
        nodeShow,
    });

    return <></>;
}

const fitNone: EditingObjectRes = {fitView: EFitView.FitNone, isEdited: false}

export function RecordRef({schema, notes, curTable, curId, refIn, refOutDepth, maxNode, nodeShow, inDragPanelAndFix}: {
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
    const {server} = useMyStore();
    const {isLoading, isError, error, data: recordRefResult} = useQuery({
        queryKey: ['tableRef', curTable.name, curId, refOutDepth, maxNode, refIn],
        queryFn: ({signal}) => fetchRecordRefs(server, curTable.name, curId, refOutDepth, maxNode, refIn, signal),
        staleTime: 1000 * 10,
    })


    if (isLoading) {
        return;
    }

    if (isError) {
        return <Result status={'error'} title={error.message}/>;
    }

    if (!recordRefResult) {
        return <Result title={'recordRef result empty'}/>;
    }

    if (recordRefResult.resultCode != 'ok') {
        return <Result status={'error'} title={recordRefResult.resultCode}/>;
    }

    return <RecordRefWithResult schema={schema} notes={notes} curTable={curTable} curId={curId}
                                nodeShow={nodeShow} recordRefResult={recordRefResult}
                                inDragPanelAndFix={inDragPanelAndFix}/>

}

export function RecordRefRoute() {
    const {schema, notes, curTable} = useOutletContext<SchemaTableType>();
    const {curId} = useLocationData();
    const {recordRefIn, recordRefOutDepth, recordMaxNode, nodeShow} = useMyStore();

    return <RecordRef schema={schema} notes={notes} curTable={curTable} curId={curId}
                      refIn={recordRefIn} refOutDepth={recordRefOutDepth} maxNode={recordMaxNode}
                      nodeShow={nodeShow}
                      inDragPanelAndFix={false}/>
}

