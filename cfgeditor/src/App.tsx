import {useRete} from "rete-react-plugin";
import {createEditor} from "./whiteboard";
import {useEffect, useState} from "react";
import {TableList} from "./TableList.tsx";
import {getSTable, Schema, STable} from "./model.ts";
import {Space} from "antd";
import {IdList} from "./IdList.tsx";


export default function App() {
    const [ref] = useRete(createEditor);

    const [schema, setSchema] = useState<Schema | null>(null);
    const [curTable, setCurTable] = useState<STable | null>(null);
    const [curId, setCurId] = useState<string | null>(null);

    useEffect(() => {
        const fetchData = async () => {
            const response = await fetch('http://localhost:3456/schemas');
            const schema = await response.json();

            setSchema(schema);
            selectCurTable2(schema) // 这时schema还没传过来
        }

        fetchData().catch(console.error);
    }, []);


    function selectCurTable2(schema: Schema | null, cur: string | null = null) {
        if (schema == null) {
            return;
        }
        let table = getSTable(schema, cur);
        if (table) {
            setCurTable(table);
            if (table.recordIds.length > 0) {
                setCurId(table.recordIds[0].id)
            }
        }
    }

    function selectCurTable(cur: string) {
        selectCurTable2(schema, cur);
    }


    return <div className="App"><Space>
        <TableList schema={schema} curTable={curTable} setCurTable={selectCurTable}/>
        <IdList curTable={curTable} curId={curId} setCurId={setCurId}/>
    </Space>
        <div ref={ref} style={{height: "100vh", width: "100vw"}}></div>
    </div>;

}
