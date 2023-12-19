import {STable} from "./model/schemaModel.ts";
import {useRete} from "rete-react-plugin";
import {createEditor} from "./editor.tsx";
import {useCallback, useEffect, useState} from "react";
import {Entity, fillInputs,} from "./model/graphModel.ts";
import {Item} from "rete-context-menu-plugin/_types/types";
import {RecordRefsResult, RefId} from "./model/recordModel.ts";
import {App, Empty, Result, Spin} from "antd";
import {createRefNodes} from "./func/recordRefNode.ts";


export function TableRecordRefLoaded({curTable, curId, recordRefResult, setCurTableAndId}: {
    curTable: STable;
    curId: string;
    recordRefResult: RecordRefsResult;
    setCurTableAndId: (table: string, id: string) => void;
}) {

    function createGraph() {
        const entityMap = new Map<string, Entity>();
        createRefNodes(entityMap, recordRefResult.refs);
        fillInputs(entityMap);

        const menu: Item[] = [];

        const nodeMenuFunc = (node: Entity): Item[] => {
            let refId = node.userData as RefId;
            if (refId.table != curTable.name || refId.id != curId) {

                return [{
                    label: '数据关系',
                    key: '数据关系',
                    handler() {
                        setCurTableAndId(refId.table, refId.id);
                    }
                }];

            }
            return [];
        }
        return {entityMap, menu, nodeMenuFunc};
    }

    const create = useCallback(
        (el: HTMLElement) => {
            return createEditor(el, createGraph());
        },
        [recordRefResult]
    );
    const [ref] = useRete(create);


    return <div ref={ref} style={{height: "100vh", width: "100vw"}}></div>
}

export function TableRecordRef({curTable, curId, refIn, refOutDepth, maxNode, server, tryReconnect, setCurTableAndId}: {
    curTable: STable;
    curId: string;
    refIn: boolean;
    refOutDepth: number;
    maxNode: number;
    server: string;
    tryReconnect: () => void;
    setCurTableAndId: (table: string, id: string) => void;
}) {
    const [recordRefResult, setRecordRefResult] = useState<RecordRefsResult | null>(null);
    const {notification} = App.useApp();

    useEffect(() => {
        setRecordRefResult(null);
        let url = `http://${server}/record?table=${curTable.name}&id=${curId}&depth=${refOutDepth}&maxObjs=${maxNode}&refs${refIn ? '&in' : ''}`;
        const fetchData = async () => {
            const response = await fetch(url);
            const recordResult: RecordRefsResult = await response.json();
            setRecordRefResult(recordResult);
            // notification.info({message: `fetch ${url} ok`, placement: 'topRight', duration: 2});
        }

        fetchData().catch((err) => {
            notification.error({message: `fetch ${url} err: ${err.toString()}`, placement: 'topRight', duration: 4});
            tryReconnect();
        });
    }, [server, curTable, curId, refOutDepth, maxNode, refIn]);


    if (recordRefResult == null) {
        return <Empty> <Spin/> </Empty>
    }

    if (recordRefResult.resultCode != 'ok') {
        return <Result status={'error'} title={recordRefResult.resultCode}/>
    }

    return <TableRecordRefLoaded curTable={curTable} curId={curId}
                                 recordRefResult={recordRefResult}
                                 setCurTableAndId={setCurTableAndId}/>


}

