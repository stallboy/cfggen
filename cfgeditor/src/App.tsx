import {useEffect, useState} from "react";
import {TableList} from "./TableList.tsx";
import {Schema, STable} from "./schemaModel.ts";
import {Space, Tabs} from "antd";
import {IdList} from "./IdList.tsx";
import {TableSchema} from "./TableSchema.tsx";
import {TableRef} from "./TableRef.tsx";


export default function App() {
    const [schema, setSchema] = useState<Schema | null>(null);
    const [curTable, setCurTable] = useState<STable | null>(null);
    const [curId, setCurId] = useState<string | null>(null);
    const [maxImpl, setMaxImpl] = useState<number>(10);

    useEffect(() => {
        const fetchData = async () => {
            const response = await fetch('http://localhost:3456/schemas');
            const rawSchema = await response.json();
            const schema = new Schema(rawSchema);

            setSchema(schema);
            selectCurTableFromSchema(schema);
        }

        fetchData().catch(console.error);
    }, []);


    function selectCurTableFromSchema(schema: Schema | null, cur: string | null = null) {
        if (schema == null) {
            return;
        }
        let table;
        if (cur) {
            table = schema.getSTable(cur);
        } else {
            table = schema.getFirstSTable();
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

    let operation = <Space>
        <TableList schema={schema} curTable={curTable} setCurTable={selectCurTable}/>
        <IdList curTable={curTable} curId={curId} setCurId={setCurId}/>
    </Space>;


    let items = [
        {
            key: "表结构",
            label: "表结构",
            children: <TableSchema schema={schema} curTable={curTable}
                                   maxImpl={maxImpl}
                                   setMaxImpl={setMaxImpl}
                                   setCurTable={selectCurTable}/>

        },
        {
            key: "表关系",
            label: "表关系",
            children: <TableRef schema={schema} curTable={curTable}
                                setCurTable={selectCurTable}/>
        },
        {
            key: "数据",
            label: "数据",
            children: <TableSchema schema={schema} curTable={curTable}
                                   maxImpl={maxImpl}
                                   setMaxImpl={setMaxImpl}
                                   setCurTable={selectCurTable}/>
        },
    ]


    return <div className="App">
        <Tabs tabBarExtraContent={{'left': operation}} items={items} type="card"/>
    </div>;

}

