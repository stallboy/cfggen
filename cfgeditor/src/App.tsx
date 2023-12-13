import {useEffect, useState} from "react";
import {TableList} from "./TableList.tsx";
import {Schema, STable} from "./model/schemaModel.ts";
import {Button, Drawer, Form, InputNumber, Space, Switch, Tabs} from "antd";
import {IdList} from "./IdList.tsx";
import {TableSchema} from "./TableSchema.tsx";
import {TableRef} from "./TableRef.tsx";
import {LeftOutlined, RightOutlined, SearchOutlined, SettingOutlined} from "@ant-design/icons";
import {TableRecord} from "./TableRecord.tsx";
import {TableRecordRef} from "./TableRecordRef.tsx";
import {History, HistoryItem} from "./model/historyModel.ts";
import {SearchValue} from "./SearchValue.tsx";

export default function App() {
    const [schema, setSchema] = useState<Schema | null>(null);
    const [curTable, setCurTable] = useState<STable | null>(null);
    const [curId, setCurId] = useState<string | null>(null);

    const [history, setHistory] = useState<History>(new History());

    const [settingOpen, setSettingOpen] = useState(false);
    const [maxImpl, setMaxImpl] = useState<number>(10);
    const [refIn, setRefIn] = useState<boolean>(true);
    const [refOutDepth, setRefOutDepth] = useState<number>(3);
    const [maxNode, setMaxNode] = useState<number>(30);
    const [recordRefIn, setRecordRefIn] = useState<boolean>(true);
    const [recordRefOutDepth, setRecordRefOutDepth] = useState<number>(3);
    const [recordMaxNode, setRecordMaxNode] = useState<number>(30);
    const [searchMax, setSearchMax] = useState<number>(30);

    const [searchOpen, setSearchOpen] = useState(false);

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


    function selectCurTableFromSchema(schema: Schema,
                                      curTableName: string | null = null,
                                      curIdStr: string | null = null,
                                      fromOp: boolean = true) {
        if (schema == null) {
            return;
        }
        let table;
        if (curTableName) {
            table = schema.getSTable(curTableName);
        } else {
            table = schema.getFirstSTable();
        }
        if (table) {
            setCurTable(table);
            if (curIdStr == null) {
                if (table.recordIds.length > 0) {
                    curIdStr = table.recordIds[0].id;
                }
            }
            setCurId(curIdStr);
            if (fromOp) { // 如果是从prev，next中来的，就不要再设置history了
                setHistory(history.addItem(table.name, curIdStr));
            }
        }
    }

    function selectCurTable(curTableName: string) {
        if (schema) {
            selectCurTableFromSchema(schema, curTableName);
        }
    }

    function selectCurTableAndId(curTableName: string, curId: string) {
        if (schema) {
            selectCurTableFromSchema(schema, curTableName, curId);
        }
    }

    function selectHistoryCur(item: HistoryItem | null) {
        if (item && schema) {
            selectCurTableFromSchema(schema, item.table, item.id, false);
        }
    }

    function prev() {
        let newHistory = history.prev();
        setHistory(newHistory);
        selectHistoryCur(newHistory.cur());
    }

    function next() {
        let newHistory = history.next();
        setHistory(newHistory);
        selectHistoryCur(newHistory.cur());
    }


    const showSetting = () => {
        setSettingOpen(true);
    };

    const onSettingClose = () => {
        setSettingOpen(false);
    };


    let leftOp = <Space>
        <Button onClick={showSetting}>
            <SettingOutlined/>
        </Button>
        <TableList schema={schema} curTable={curTable} setCurTable={selectCurTable}/>
        <IdList curTable={curTable} curId={curId} setCurId={setCurId}/>

        <Button onClick={prev} disabled={!history.canPrev()}>
            <LeftOutlined/>
        </Button>

        <Button onClick={next} disabled={!history.canNext()}>
            <RightOutlined/>
        </Button>
    </Space>;

    const showSearch = () => {
        setSearchOpen(true);
    };

    const onSearchClose = () => {
        setSearchOpen(false);
    };

    let rightOp = <Space>
        <Button onClick={showSearch}>
            <SearchOutlined/>
        </Button>
    </Space>

    let tableSchema = <div/>;
    let tableRef = <div/>;
    let tableRecord = <div/>;
    let tableRecordRef = <div/>;
    if (schema != null && curTable != null) {
        tableSchema = <TableSchema schema={schema}
                                   curTable={curTable}
                                   maxImpl={maxImpl}
                                   setCurTable={selectCurTable}/>;
        tableRef = <TableRef schema={schema}
                             curTable={curTable}
                             setCurTable={selectCurTable}
                             refIn={refIn}
                             refOutDepth={refOutDepth}
                             maxNode={maxNode}/>;

        if (curId != null) {
            tableRecord = <TableRecord schema={schema}
                                       curTable={curTable}
                                       curId={curId}
                                       setCurTableAndId={selectCurTableAndId}/>;

            tableRecordRef = <TableRecordRef curTable={curTable}
                                             curId={curId}
                                             refIn={recordRefIn}
                                             refOutDepth={recordRefOutDepth}
                                             maxNode={recordMaxNode}
                                             setCurTableAndId={selectCurTableAndId}/>;
        }
    }

    let items = [{key: "表结构", label: "表结构", children: tableSchema,},
        {key: "表关系", label: "表关系", children: tableRef,},
        {key: "数据", label: "数据", children: tableRecord,},
        {key: "数据关系", label: "数据关系", children: tableRecordRef,},
    ]


    function onChangeMaxImpl(value: number | null) {
        if (value) {
            setMaxImpl(value);
        }
    }

    function onChangeRefIn(checked: boolean) {
        setRefIn(checked);
    }

    function onChangeRefOutDepth(value: number | null) {
        if (value) {
            setRefOutDepth(value);
        }
    }

    function onChangeMaxNode(value: number | null) {
        if (value) {
            setMaxNode(value);
        }
    }

    function onChangeRecordRefIn(checked: boolean) {
        setRecordRefIn(checked);
    }

    function onChangeRecordRefOutDepth(value: number | null) {
        if (value) {
            setRecordRefOutDepth(value);
        }
    }

    function onChangeRecordMaxNode(value: number | null) {
        if (value) {
            setRecordMaxNode(value);
        }
    }

    function onChangeSearchMax(value: number | null) {
        if (value) {
            setSearchMax(value);
        }
    }

    return <div className="App">
        <Tabs tabBarExtraContent={{'left': leftOp, 'right': rightOp}} items={items} type="card"/>
        <Drawer title="setting" placement="left" onClose={onSettingClose} open={settingOpen}>
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

                <Form.Item label='数据入层：'>
                    <Switch checked={recordRefIn} onChange={onChangeRecordRefIn}/>
                </Form.Item>

                <Form.Item label='数据出层：'>
                    <InputNumber value={recordRefOutDepth} min={1} max={500} onChange={onChangeRecordRefOutDepth}/>
                </Form.Item>

                <Form.Item label='数据节点数：'>
                    <InputNumber value={recordMaxNode} min={1} max={500} onChange={onChangeRecordMaxNode}/>
                </Form.Item>

                <Form.Item label='搜索返回数：'>
                    <InputNumber value={searchMax} min={1} max={500} onChange={onChangeSearchMax}/>
                </Form.Item>
            </Form>
        </Drawer>

        <Drawer title="search" placement="right" onClose={onSearchClose} open={searchOpen} size={'large'}>
            <SearchValue searchMax={searchMax} setCurTableAndId={selectCurTableAndId}/>
        </Drawer>

    </div>;

}

