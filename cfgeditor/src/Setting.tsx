import {Button, Descriptions, Divider, Flex, Form, Input, InputNumber, Select, Space, Switch, Tabs} from "antd";
import {CloseOutlined, LeftOutlined, RightOutlined, SearchOutlined} from "@ant-design/icons";
import {NodeShowSetting} from "./NodeShowSetting.tsx";
import {useTranslation} from "react-i18next";
import {pageRecordRef} from "./CfgEditorApp.tsx";
import {STable} from "./model/schemaModel.ts";
import {Schema} from "./model/schemaUtil.ts";
import {
    setDragPanel, setFix, setFixNull,
    setImageSizeScale,
    setMaxImpl,
    setMaxNode, setRecordMaxNode,
    setRecordRefIn,
    setRecordRefOutDepth,
    setRefIn,
    setRefOutDepth, setSearchMax,
    store
} from "./model/store.ts";


export function Setting({
                            schema, curPage,
                            curTable, hasFix, onDeleteRecord,
                            onConnectServer,
                            onToPng,


                        }: {
    schema: Schema | null;
    curTableId: string;
    curId: string;
    curPage: string;
    curTable: STable | null;
    hasFix: boolean;
    onDeleteRecord: () => void;

    onConnectServer: (v: string) => void;


    onToPng: () => void;

}) {

    const {t} = useTranslation();

    const {
        server, dragPanel,
        maxImpl,
        refIn, refOutDepth, maxNode,
        recordRefIn, recordRefOutDepth, recordMaxNode,
        searchMax, imageSizeScale
    } = store;

    let deleteRecordButton;
    if (schema && curTable && schema.isEditable && curTable.isEditable) {
        deleteRecordButton = <Button type="primary" danger onClick={onDeleteRecord}>
            <CloseOutlined/>{t('deleteCurRecord')}
        </Button>

    }


    let addFixButton;
    let removeFixButton;
    if (schema && curTable && curPage == pageRecordRef) {


        addFixButton = <Form.Item wrapperCol={{offset: 10}}>
            <Button type="primary" onClick={setFix}>
                {t('addFix')}
            </Button>
        </Form.Item>
    }
    if (hasFix) {
        removeFixButton = <Form.Item wrapperCol={{offset: 10}}>
            <Button type="primary" onClick={setFixNull}>
                {t('removeFix')}
            </Button>
        </Form.Item>
    }

    let tableSetting =
        <Form labelCol={{span: 10}} wrapperCol={{span: 14}} layout={'horizontal'}
              initialValues={{maxImpl, refIn, refOutDepth, maxNode}}>
            <Form.Item label={t('implsShowCnt')} name='maxImpl'>
                <InputNumber min={1} max={500} onChange={setMaxImpl}/>
            </Form.Item>

            <Form.Item name='refIn' label={t('refIn')} valuePropName="checked">
                <Switch onChange={setRefIn}/>
            </Form.Item>

            <Form.Item name='refOutDepth' label={t('refOutDepth')}>
                <InputNumber min={1} max={500} onChange={setRefOutDepth}/>
            </Form.Item>

            <Form.Item name='maxNode' label={t('maxNode')}>
                <InputNumber min={1} max={500} onChange={setMaxNode}/>
            </Form.Item>
        </Form>;

    let recordSetting = <>
        <Form labelCol={{span: 10}} wrapperCol={{span: 14}} layout={'horizontal'}
              initialValues={{recordRefIn, recordRefOutDepth, recordMaxNode}}>
            <Form.Item name='recordRefIn' label={t('recordRefIn')} valuePropName="checked">
                <Switch onChange={setRecordRefIn}/>
            </Form.Item>

            <Form.Item name='recordRefOutDepth' label={t('recordRefOutDepth')}>
                <InputNumber min={1} max={500} onChange={setRecordRefOutDepth}/>
            </Form.Item>

            <Form.Item name='recordMaxNode' label={t('recordMaxNode')}>
                <InputNumber min={1} max={500} onChange={setRecordMaxNode}/>
            </Form.Item>
        </Form>
        <Divider/>
        <NodeShowSetting/>
    </>;

    let otherSetting =
        <Form labelCol={{span: 10}} wrapperCol={{span: 14}} layout={'horizontal'}
              initialValues={{searchMax, imageSizeScale, server}}>
            <Form.Item name='searchMax' label={t('searchMaxReturn')}>
                <InputNumber min={1} max={500} onChange={setSearchMax}/>
            </Form.Item>

            <Form.Item name='imageSizeScale' label={t('imageSizeScale')}>
                <InputNumber min={1} max={256} onChange={setImageSizeScale}/>
            </Form.Item>

            <Form.Item wrapperCol={{offset: 10}}>
                <Button type="primary" onClick={onToPng}>
                    {t('toPng')}
                </Button>
            </Form.Item>

            <Form.Item name='dragePanel' initialValue={dragPanel} label={t('dragPanel')}>
                <Select onChange={setDragPanel} options={[
                    {label: t('recordRef'), value: 'recordRef'},
                    {label: t('fix'), value: 'fix'},
                    {label: t('none'), value: 'none'}]}/>
            </Form.Item>

            {addFixButton}
            {removeFixButton}


            <Form.Item label={t('curServer')}>
                {server}
            </Form.Item>
            <Form.Item name='server' label={t('newServer')}>
                <Input.Search enterButton={t('connect')} onSearch={onConnectServer}/>
            </Form.Item>
        </Form>;


    let keySetting = <Flex gap={"middle"} vertical>
        <Descriptions title="Key Shortcut" bordered column={2} items={[
            {
                key: '1',
                label: <LeftOutlined/>,
                children: 'alt+x',
            },
            {
                key: '2',
                label: <RightOutlined/>,
                children: 'alt+c',
            },
            {
                key: '3',
                label: t('table'),
                children: 'alt+1',
            },
            {
                key: '4',
                label: t('tableRef'),
                children: 'alt+2',
            },
            {
                key: '5',
                label: t('record'),
                children: 'alt+3',
            },
            {
                key: '6',
                label: t('recordRef'),
                children: 'alt+4',
            },

            {
                key: '7',
                label: <SearchOutlined/>,
                children: 'alt+q',
            },
        ]}/>

        <Space>
            {deleteRecordButton}
        </Space>
    </Flex>;


    return <Tabs items={[
        {key: 'recordSetting', label: t('recordSetting'), children: recordSetting,},
        {key: 'tableSetting', label: t('tableSetting'), children: tableSetting,},
        {key: 'otherSetting', label: t('otherSetting'), children: otherSetting,},
        {key: 'keySetting', label: t('keySetting'), children: keySetting,},
    ]} tabPosition='left'/>;
}
