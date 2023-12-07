import {useEffect, useState} from "react";
import {TableList} from "./TableList.tsx";
import {getFirstSTable, getSTable, resolveSchema, Schema, STable} from "./schemaModel.ts";
import {Space} from "antd";
import {IdList} from "./IdList.tsx";
import {TableSchema} from "./TableSchema.tsx";


export default function App() {
    const [schema, setSchema] = useState<Schema | null>(null);
    const [curTable, setCurTable] = useState<STable | null>(null);
    const [curId, setCurId] = useState<string | null>(null);
    const [settingMaxImplSchema, setSettingMaxImplSchema] = useState<number>(10);

    useEffect(() => {
        const fetchData = async () => {
            const response = await fetch('http://localhost:3456/schemas');
            const rawSchema = await response.json();
            const schema = resolveSchema(rawSchema);

            setSchema(schema);
            selectCurTableFromSchema(schema);
            setSettingMaxImplSchema(10);
        }

        fetchData().catch(console.error);
    }, []);


    function selectCurTableFromSchema(schema: Schema | null, cur: string | null = null) {
        if (schema == null) {
            return;
        }
        let table;
        if (cur) {
            table = getSTable(schema, cur);
        } else {
            table = getFirstSTable(schema);
        }
        if (table) {
            setCurTable(table);
            if (table.recordIds.length > 0) {
                setCurId(table.recordIds[0].id)
            }
        }
    }

    function selectCurTable(cur: string) {
        selectCurTableFromSchema(schema, cur);
    }


    return <div className="App"><Space>
        <TableList schema={schema} curTable={curTable} setCurTable={selectCurTable}/>
        <IdList curTable={curTable} curId={curId} setCurId={setCurId}/>
    </Space>
        <TableSchema schema={schema} curTable={curTable} inDepth={0} outDepth={0}
                     settingMaxImplSchema={settingMaxImplSchema}/>
    </div>;

}

