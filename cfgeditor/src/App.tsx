import {useEffect, useState} from "react";
import {TableList} from "./TableList.tsx";
import {Schema, STable} from "./schemaModel.ts";
import {Button, Drawer, Form, InputNumber, Space, Switch, Tabs} from "antd";
import {IdList} from "./IdList.tsx";
import {TableSchema} from "./TableSchema.tsx";
import {TableRef} from "./TableRef.tsx";
import {SettingOutlined} from "@ant-design/icons";


export default function App() {
    const [schema, setSchema] = useState<Schema | null>(null);
    const [curTable, setCurTable] = useState<STable | null>(null);
    const [curId, setCurId] = useState<string | null>(null);

    const [settingOpen, setSettingOpen] = useState(false);
    const [maxImpl, setMaxImpl] = useState<number>(10);
    const [refIn, setRefIn] = useState<boolean>(true);
    const [refOutDepth, setRefOutDepth] = useState<number>(3);
    const [maxNode, setMaxNode] = useState<number>(30);


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


    const showDrawer = () => {
        setSettingOpen(true);
    };

    const onClose = () => {
        setSettingOpen(false);
    };


    let operation = <Space>
        <Button type="default" onClick={showDrawer}>
            <SettingOutlined/>
        </Button>
        <TableList schema={schema} curTable={curTable} setCurTable={selectCurTable}/>
        <IdList curTable={curTable} curId={curId} setCurId={setCurId}/>
    </Space>;


    let items = [
        {
            key: "表结构",
            label: "表结构",
            children: <TableSchema schema={schema} curTable={curTable}
                                   maxImpl={maxImpl}
                                   setCurTable={selectCurTable}/>

        },
        {
            key: "表关系",
            label: "表关系",
            children: <TableRef schema={schema}
                                curTable={curTable}
                                setCurTable={selectCurTable}
                                refIn={refIn}
                                refOutDepth={refOutDepth}
                                maxNode={maxNode}/>
        },
        {
            key: "数据",
            label: "数据",
            children: <TableSchema schema={schema} curTable={curTable}
                                   maxImpl={maxImpl}
                                   setCurTable={selectCurTable}/>
        },
    ]

    function onChangeRefOutDepth(value: number | null) {
        if (value) {
            setRefOutDepth(value);
        }
    }

    function onChangeMaxImpl(value: number | null) {
        if (value) {
            setMaxImpl(value);
        }
    }

    function onChangeMaxNode(value: number | null) {
        if (value) {
            setMaxNode(value);
        }
    }

    function onChangeRefIn(checked: boolean) {
        setRefIn(checked);
    }

    return <div className="App">
        <Tabs tabBarExtraContent={{'left': operation}} items={items} type="card"/>
        <Drawer title="setting" placement="left" onClose={onClose} open={settingOpen}>
            <Form labelCol={{span: 6}} wrapperCol={{span: 14}} layout={'horizontal'}>
                <Form.Item label='接口实现数:'>
                    <InputNumber value={maxImpl} min={1} max={500} onChange={onChangeMaxImpl}/>
                </Form.Item>

                <Form.Item label='入层：'>
                    <Switch checked={refIn} onChange={onChangeRefIn}/>
                </Form.Item>

                <Form.Item label='出层：'>
                    <InputNumber value={refOutDepth} min={1} max={500} onChange={onChangeRefOutDepth}/>
                </Form.Item>

                <Form.Item label='节点数：'>
                    <InputNumber value={maxNode} min={1} max={500} onChange={onChangeMaxNode}/>
                </Form.Item>
            </Form>
        </Drawer>
    </div>;

}

