import {STable} from "../table/schemaModel.ts";
import {Entity} from "../../flow/entityModel.ts";
import {RecordRefsResult, RefId} from "./recordModel.ts";
import {Result} from "antd";
import {createRefEntities, getId} from "./recordRefEntity.ts";
import {useTranslation} from "react-i18next";
import {Schema} from "../table/schemaUtil.ts";
import {NodeShowType} from "../../io/localStoreJson.ts";
import {navTo, store, useLocationData} from "../setting/store.ts";
import {useNavigate, useOutletContext} from "react-router-dom";
import {useQuery} from "@tanstack/react-query";
import {fetchRecordRefs} from "../../io/api.ts";
import {MenuItem} from "../../flow/FlowContextMenu.tsx";
import {SchemaTableType} from "../../CfgEditorApp.tsx";
import {fillHandles} from "../../flow/entityToNodeAndEdge.ts";

import {useEntityToGraph} from "../../flow/FlowGraph.tsx";


export function RecordRefWithResult({schema, curTable, curId, nodeShow, recordRefResult}: {
    schema: Schema;
    curTable: STable;
    curId: string;
    nodeShow: NodeShowType;
    recordRefResult: RecordRefsResult;
}) {
    const [t] = useTranslation();
    const navigate = useNavigate();

    const entityMap = new Map<string, Entity>();
    const hasContainEnum = nodeShow.containEnum || curTable.entryType == 'eEnum';
    createRefEntities(entityMap, schema, recordRefResult.refs, true, hasContainEnum);
    fillHandles(entityMap);

    const paneMenu: MenuItem[] = [{
        label: recordRefResult.table + "\n" + t('record'),
        key: 'record',
        handler() {
            navigate(navTo('record', curTable.name, curId));
        }
    }];

    const nodeMenuFunc = (entity: Entity): MenuItem[] => {
        let refId = entity.userData as RefId;
        let id = getId(refId.table, refId.id);
        let mm = [];
        if (refId.table != recordRefResult.table || refId.id != recordRefResult.id) {
            mm.push({
                label: id + "\n" + t('recordRef'),
                key: 'entityRecordRef',
                handler() {
                    navigate(navTo('recordRef', refId.table, refId.id));
                }
            });
        }
        mm.push({
            label: id + "\n" + t('record'),
            key: 'entityRecord',
            handler() {
                navigate(navTo('record', refId.table, refId.id));
            }
        });

        let isEntityEditable = schema.isEditable && !!(schema.getSTable(refId.table)?.isEditable);
        if (isEntityEditable) {
            mm.push({
                label: id + "\n" + t('edit'),
                key: 'entityEdit',
                handler() {
                    navigate(navTo('record', refId.table, refId.id, true));
                }
            });
        }
        return mm;
    }

    const pathname = `/tableRef/${curTable.name}/${curId}`;
    useEntityToGraph(pathname, entityMap, nodeMenuFunc, paneMenu);

    return <></>;
}


export function RecordRef({schema, curTable, curId, refIn, refOutDepth, maxNode, nodeShow}: {
    schema: Schema;
    curTable: STable;
    curId: string;
    refIn: boolean;
    refOutDepth: number;
    maxNode: number;
    nodeShow: NodeShowType;
}) {
    const {server} = store;
    const {isLoading, isError, error, data: recordRefResult} = useQuery({
        queryKey: ['tableRef', curTable.name, curId, refOutDepth, maxNode, refIn],
        queryFn: () => fetchRecordRefs(server, curTable.name, curId, refOutDepth, maxNode, refIn),
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

    return <RecordRefWithResult schema={schema} curTable={curTable} curId={curId}
                                nodeShow={nodeShow} recordRefResult={recordRefResult}/>

}

export function RecordRefRoute() {
    const {schema, curTable} = useOutletContext<SchemaTableType>();
    const {curId} = useLocationData();
    const {recordRefIn, recordRefOutDepth, recordMaxNode, nodeShow} = store;

    return <RecordRef schema={schema} curTable={curTable} curId={curId}
                      refIn={recordRefIn} refOutDepth={recordRefOutDepth} maxNode={recordMaxNode}
                      nodeShow={nodeShow}/>
}

