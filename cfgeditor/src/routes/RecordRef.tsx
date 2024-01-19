import {STable} from "../model/schemaModel.ts";

import {Entity} from "../model/entityModel.ts";

import {RefId} from "../model/recordModel.ts";
import {Result, Spin} from "antd";
import {createRefEntities, getId} from "../func/recordRefEntity.ts";
import {useTranslation} from "react-i18next";
import {Schema} from "../model/schemaUtil.ts";
import {NodeShowType} from "../func/localStoreJson.ts";
import {navTo, setEditMode, store, useLocationData} from "../model/store.ts";
import {useNavigate, useOutletContext} from "react-router-dom";
import {useQuery} from "@tanstack/react-query";
import {fetchRecordRefs} from "../func/api.ts";
import {MenuItem} from "../ui/FlowContextMenu.tsx";
import {FlowGraph} from "../ui/FlowGraph.tsx";
import {ReactFlowProvider} from "reactflow";
import {SchemaTableType} from "../CfgEditorApp.tsx";
import {convertNodeAndEdges, fillHandles} from "../ui/entityToFlow.ts";


export function RecordRef({schema, curTable, curId, refIn, refOutDepth, maxNode, nodeShow}: {
    schema: Schema;
    curTable: STable;
    curId: string;
    refIn: boolean;
    refOutDepth: number;
    maxNode: number;
    nodeShow: NodeShowType;
}) {
    const {server, query} = store;
    const [t] = useTranslation();
    const navigate = useNavigate();
    const {pathname} = useLocationData();

    const {isLoading, isError, error, data: recordRefResult} = useQuery({
        queryKey: ['tableRef', curTable.id, curId, refOutDepth, maxNode, refIn],
        queryFn: () => fetchRecordRefs(server, curTable.name, curId, refOutDepth, maxNode, refIn),
        staleTime: 1000 * 10,
    })


    if (isLoading) {
        return <Spin/>;
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
                setEditMode(false);
            }
        });

        let isEntityEditable = schema.isEditable && !!(schema.getSTable(refId.table)?.isEditable);
        if (isEntityEditable) {
            mm.push({
                label: id + "\n" + t('edit'),
                key: 'entityEdit',
                handler() {
                    navigate(navTo('record', refId.table, refId.id));
                    setEditMode(true);
                }
            });
        }
        return mm;
    }

    const {nodes, edges} = convertNodeAndEdges({entityMap, nodeShow, query});
    return <ReactFlowProvider>
        <FlowGraph key={pathname}
                   initialNodes={nodes}
                   initialEdges={edges}
                   paneMenu={paneMenu}
                   nodeMenuFunc={nodeMenuFunc}
        />
    </ReactFlowProvider>
}

export function RecordRefRoute() {
    const {schema, curTable} = useOutletContext<SchemaTableType>();
    const {curId} = useLocationData();
    const {recordRefIn, recordRefOutDepth, recordMaxNode, nodeShow} = store;

    return <RecordRef schema={schema} curTable={curTable} curId={curId}
                      refIn={recordRefIn} refOutDepth={recordRefOutDepth} maxNode={recordMaxNode}
                      nodeShow={nodeShow}/>
}

