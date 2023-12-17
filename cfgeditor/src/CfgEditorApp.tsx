import {useEffect, useState} from "react";
import {Alert, Button, Drawer, Flex, Form, Input, InputNumber, Modal, Space, Switch, Tabs} from "antd";
import {LeftOutlined, RightOutlined, SearchOutlined, SettingOutlined} from "@ant-design/icons";
import {Schema, STable} from "./model/schemaModel.ts";
import {History, HistoryItem} from "./model/historyModel.ts";
import {TableList} from "./TableList.tsx";
import {IdList} from "./IdList.tsx";
import {TableSchema} from "./TableSchema.tsx";
import {TableRef} from "./TableRef.tsx";
import {TableRecord} from "./TableRecord.tsx";
import {TableRecordRef} from "./TableRecordRef.tsx";
import {SearchValue} from "./SearchValue.tsx";
import {useHotkeys} from "react-hotkeys-hook";
import {getInt, getBool, getStr} from "./model/localStore.ts";

const key1 = 'table'
const key2 = 'tableRef'
const key3 = 'record'
const key4 = 'recordRef'

export default function CfgEditorApp() {
    const [server, setServer] = useState<string>(getStr('server', 'localhost:3456'));
    const [schema, setSchema] = useState<Schema | null>(null);
    const [curTableId, setCurTableId] = useState<string>(getStr('curTableId', ''));
    const [curTable, setCurTable] = useState<STable | null>(null);
    const [curId, setCurId] = useState<string>(getStr('curId', ''));
    const [curPage, setCurPage] = useState<string>(getStr('curPage', key3));

    const [maxImpl, setMaxImpl] = useState<number>(getInt('maxImpl', 10));
    const [refIn, setRefIn] = useState<boolean>(getBool('refIn', true));
    const [refOutDepth, setRefOutDepth] = useState<number>(getInt('refOutDepth', 5));
    const [maxNode, setMaxNode] = useState<number>(getInt('maxNode', 30));
    const [recordRefIn, setRecordRefIn] = useState<boolean>(getBool('recordRefIn', true));
    const [recordRefOutDepth, setRecordRefOutDepth] = useState<number>(getInt('recordRefOutDepth', 5));
    const [recordMaxNode, setRecordMaxNode] = useState<number>(getInt('recordMaxNode', 30));
    const [searchMax, setSearchMax] = useState<number>(getInt('searchMax', 50));

    const [history, setHistory] = useState<History>(new History());
    const [settingOpen, setSettingOpen] = useState<boolean>(false);
    const [searchOpen, setSearchOpen] = useState<boolean>(false);

    const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
    const [isFetching, setIsFetching] = useState<boolean>(false);
    const [fetchErr, setFetchErr] = useState<string>('');

    useHotkeys('alt+1', () => selectCurPage(key1));
    useHotkeys('alt+2', () => selectCurPage(key2));
    useHotkeys('alt+3', () => selectCurPage(key3));
    useHotkeys('alt+4', () => selectCurPage(key4));
    useHotkeys('alt+x', () => showSearch());
    useHotkeys('alt+c', () => prev());
    useHotkeys('alt+v', () => next());

    useEffect(() => {
        tryConnect(server);
    }, []);

    function tryConnect(server: string) {
        setSchema(null);
        setIsFetching(true);

        const fetchData = async () => {
            const response = await fetch(`http://${server}/schemas`);
            const rawSchema = await response.json();
            const schema = new Schema(rawSchema);

            setSchema(schema);
            selectCurTableFromSchema(schema, curTableId, curId);
            setIsFetching(false);
            setIsModalOpen(false);
        }

        fetchData().catch((err) => {
            setIsFetching(false);
            setFetchErr(err.toString());
            setIsModalOpen(true);
        });
    }


    function selectCurTableFromSchema(schema: Schema,
                                      curTableName: string = curTableId,
                                      curIdStr: string = curId,
                                      fromOp: boolean = true) {
        if (schema == null) {
            return;
        }

        let curTab;
        if (curTableName.length > 0) {
            curTab = schema.getSTable(curTableName);
        }
        if (curTab == null) {
            curTab = schema.getFirstSTable();
        }

        if (curTab) {
            setCurTableId(curTab.name);
            setCurTable(curTab);

            let id = '';
            if (curIdStr.length > 0 && schema.hasId(curTab, curIdStr)) {
                id = curIdStr;
            } else if (curTab.recordIds.length > 0) {
                id = curTab.recordIds[0].id;
            }

            setCurId(id);
            if (fromOp) { // 如果是从prev，next中来的，就不要再设置history了
                setHistory(history.addItem(curTab.name, id));
            }
            localStorage.setItem('curTableId', curTab.name);
            localStorage.setItem('curId', id);
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

    function selectCurId(curId: string) {
        if (schema && curTable) {
            selectCurTableFromSchema(schema, curTable.name, curId);
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

    function selectCurPage(page: string) {
        setCurPage(page);
        localStorage.setItem('curPage', page);
    }

    const leftOp = <Space>
        <TableList schema={schema} curTable={curTable} setCurTable={selectCurTable}/>
        <IdList curTable={curTable} curId={curId} setCurId={selectCurId}/>

        <Button onClick={prev} disabled={!history.canPrev()}>
            <LeftOutlined/>
        </Button>

        <Button onClick={next} disabled={!history.canNext()}>
            <RightOutlined/>
        </Button>
    </Space>;

    const showSetting = () => {
        setSettingOpen(true);
    };

    const onSettingClose = () => {
        setSettingOpen(false);
    };

    const showSearch = () => {
        setSearchOpen(true);
    };

    const onSearchClose = () => {
        setSearchOpen(false);
    };

    const rightOp = <Space>
        <Button onClick={showSearch}>
            <SearchOutlined/>
        </Button>
        <Button onClick={showSetting}>
            <SettingOutlined/>
        </Button>
    </Space>

    function tryReconnect() {
        tryConnect(server);
    }

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
                                       server={server}
                                       tryReconnect={tryReconnect}
                                       setCurTableAndId={selectCurTableAndId}/>;

            tableRecordRef = <TableRecordRef curTable={curTable}
                                             curId={curId}
                                             refIn={recordRefIn}
                                             refOutDepth={recordRefOutDepth}
                                             maxNode={recordMaxNode}
                                             server={server}
                                             tryReconnect={tryReconnect}
                                             setCurTableAndId={selectCurTableAndId}/>;
        }
    }

    let items = [
        {key: key1, label: <Space>表结构</Space>, children: tableSchema,},
        {key: key2, label: <Space>表关系</Space>, children: tableRef,},
        {key: key3, label: <Space>数据</Space>, children: tableRecord,},
        {key: key4, label: <Space>数据关系</Space>, children: tableRecordRef,},
    ]

    function onTabChange(activeKey: string) {
        selectCurPage(activeKey);
    }


    function onChangeMaxImpl(value: number | null) {
        if (value) {
            setMaxImpl(value);
            localStorage.setItem('maxImpl', value.toString());
        }
    }

    function onChangeRefIn(checked: boolean) {
        setRefIn(checked);
        localStorage.setItem('refIn', checked ? 'true' : 'false');
    }

    function onChangeRefOutDepth(value: number | null) {
        if (value) {
            setRefOutDepth(value);
            localStorage.setItem('refOutDepth', value.toString());
        }
    }

    function onChangeMaxNode(value: number | null) {
        if (value) {
            setMaxNode(value);
            localStorage.setItem('maxNode', value.toString());
        }
    }

    function onChangeRecordRefIn(checked: boolean) {
        setRecordRefIn(checked);
        localStorage.setItem('recordRefIn', checked ? 'true' : 'false');
    }

    function onChangeRecordRefOutDepth(value: number | null) {
        if (value) {
            setRecordRefOutDepth(value);
            localStorage.setItem('recordRefOutDepth', value.toString());
        }
    }

    function onChangeRecordMaxNode(value: number | null) {
        if (value) {
            setRecordMaxNode(value);
            localStorage.setItem('recordMaxNode', value.toString());
        }
    }

    function onChangeSearchMax(value: number | null) {
        if (value) {
            setSearchMax(value);
            localStorage.setItem('searchMax', value.toString());
        }
    }

    function onConnectServer(value: string) {
        setServer(value);
        localStorage.setItem('server', value);
        tryConnect(value);
    }


    function handleModalOk() {
        onConnectServer(server);
    }


    return <Space>
        <Modal title="服务器连接失败" open={isModalOpen}
               cancelButtonProps={{disabled: true}}
               closable={false}
               confirmLoading={isFetching}
               okText='重连当前服务器'
               onOk={handleModalOk}>

            <Flex vertical>
                <Alert message={fetchErr} type='error'/>
                <p> 请 启动'cfgeditor服务器.bat'，查看自己的配表！ 或 更改服务器地址，查看别人的配表！</p>
                <p> 当前服务器: {server}</p>
                <Form.Item label='新服务器:'>
                    <Input.Search defaultValue={server} enterButton='连接新服' onSearch={onConnectServer}/>
                </Form.Item>
            </Flex>
        </Modal>

        <Tabs tabBarExtraContent={{'left': leftOp, 'right': rightOp}}
              items={items}
              activeKey={curPage}
              onChange={onTabChange}
              type="card"/>
        <Drawer title="setting" placement="right" onClose={onSettingClose} open={settingOpen}>
            <Form labelCol={{span: 10}} wrapperCol={{span: 14}} layout={'horizontal'}>
                <Form.Item label='接口实现数:'>
                    <InputNumber value={maxImpl} min={1} max={500} onChange={onChangeMaxImpl}/>
                </Form.Item>

                <Form.Item label='入层：'>
                    <Switch checked={refIn} onChange={onChangeRefIn}/>
                </Form.Item>

                <Form.Item label='出层：'>
                    <InputNumber value={refOutDepth} min={1} max={500} onChange={onChangeRefOutDepth}/>
                </Form.Item>

                <Form.Item label='节点数'>
                    <InputNumber value={maxNode} min={1} max={500} onChange={onChangeMaxNode}/>
                </Form.Item>

                <Form.Item label='数据入层'>
                    <Switch checked={recordRefIn} onChange={onChangeRecordRefIn}/>
                </Form.Item>

                <Form.Item label='数据出层'>
                    <InputNumber value={recordRefOutDepth} min={1} max={500} onChange={onChangeRecordRefOutDepth}/>
                </Form.Item>

                <Form.Item label='数据节点数'>
                    <InputNumber value={recordMaxNode} min={1} max={500} onChange={onChangeRecordMaxNode}/>
                </Form.Item>

                <Form.Item label='搜索返回数'>
                    <InputNumber value={searchMax} min={1} max={500} onChange={onChangeSearchMax}/>
                </Form.Item>

                <Form.Item label='当前服务器'>
                    {server}
                </Form.Item>
                <Form.Item label='服务器'>
                    <Input.Search defaultValue={server} enterButton='连接' onSearch={onConnectServer}/>
                </Form.Item>

                <Form.Item label=<LeftOutlined/>>
                    alt+x
                </Form.Item>
                <Form.Item label=<RightOutlined/>>
                    alt+c
                </Form.Item>
                <Form.Item label='表结构'>
                    alt+1
                </Form.Item>
                <Form.Item label='表关系'>
                    alt+2
                </Form.Item>
                <Form.Item label='数据'>
                    alt+3
                </Form.Item>
                <Form.Item label='数据关系'>
                    alt+4
                </Form.Item>
                <Form.Item label=<SearchOutlined/>>
                    alt+q
                </Form.Item>


            </Form>
        </Drawer>

        <Drawer title="search" placement="right" onClose={onSearchClose} open={searchOpen} size='large'>
            <SearchValue searchMax={searchMax}
                         server={server}
                         tryReconnect={tryReconnect}
                         setCurTableAndId={selectCurTableAndId}/>
        </Drawer>

    </Space>;

}

