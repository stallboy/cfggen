import {useRete} from "rete-react-plugin";
import {createEditor} from "./whiteboard";
import {useEffect, useState} from "react";
import {getTableNames, TableList} from "./TableList.tsx";
import {getSTable, RecordId, Schema} from "./model.ts";
import {Space} from "antd";
import {IdList} from "./IdList.tsx";

// import {TableList, CurSelect, getDefaultSelect, schemaToTree, TableTreeNode} from "./TableListByBreadcrumb.tsx";


export default function App() {
    const [ref] = useRete(createEditor);


    const [schema, setSchema] = useState<Schema | null>(null);
    const [tableNames, setTableNames] = useState<string[] | null>(null);
    const [curTable, setCurTable] = useState<string | null>(null);
    const [curId, setCurId] = useState<string | null>(null);

    const [recordCount, setRecordCount] = useState<number>(0);
    const [recordIds, setRecordIds] = useState<RecordId[] | null>(null);

    // const [tree, setTree] = useState<TableTreeNode | null>(null);
    // const [curSelect, setCurSelect] = useState<CurSelect | null>(null);

    useEffect(() => {
        const fetchData = async () => {
            const response = await fetch('http://localhost:3456/schemas');
            const schema = await response.json();

            setSchema(schema);

            const names = getTableNames(schema);
            setTableNames(names);

            if (names.length > 0) {
                selectCurTable2(schema, names[0]) // 这时schema还没传过来
            }

            // const tree = schemaToTree(schema);
            // setTree(tree);
            // const cur = getDefaultSelect(tree);
            // setCurSelect(cur);
        }

        fetchData().catch(console.error);
    }, []);

    // <TableListByBreadcrumb tree={tree} curSelect={curSelect} setCurSelect={setCurSelect}/>

    function selectCurTable2(schema: Schema | null, cur: string) {
        setCurTable(cur);
        if (schema == null) {
            return;
        }

        let table = getSTable(schema, cur);
        if (table) {
            setRecordCount(table.recordCount);
            setRecordIds(table.recordIds);

            if (table.recordIds.length > 0) {
                selectCurId(table.recordIds[0].id)
            }
        }
    }

    function selectCurTable(cur: string) {
        selectCurTable2(schema, cur);
    }

    function selectCurId(cur: string) {
        setCurId(cur);
    }


    return <div className="App"><Space>
        <TableList tables={tableNames} curTable={curTable} setCurTable={selectCurTable}/>
        <IdList recordIds={recordIds} recordCount={recordCount} curId={curId} setCurId={selectCurId}/>
    </Space>
        <div ref={ref} style={{height: "100vh", width: "100vw"}}></div>
    </div>;

}
